package com.gtladd.gtladditions.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.block.ModelBlockRenderer
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.BlockAndTintGetter
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState
import net.minecraftforge.client.model.BakedModelWrapper
import net.minecraftforge.client.model.data.ModelData
import org.joml.Matrix3fc
import kotlin.math.sqrt

object StructureBlockLighting {
    enum class FaceGroup(val direction: Direction?) {
        UNSHADED(null),
        DOWN(Direction.DOWN),
        UP(Direction.UP),
        NORTH(Direction.NORTH),
        SOUTH(Direction.SOUTH),
        WEST(Direction.WEST),
        EAST(Direction.EAST);

        fun shade(level: BlockAndTintGetter, modelNormal: Matrix3fc): Float {
            val face = direction ?: return level.getShade(Direction.UP, false)
            return shade(
                level,
                modelNormal.m00() * face.stepX + modelNormal.m10() * face.stepY + modelNormal.m20() * face.stepZ,
                modelNormal.m01() * face.stepX + modelNormal.m11() * face.stepY + modelNormal.m21() * face.stepZ,
                modelNormal.m02() * face.stepX + modelNormal.m12() * face.stepY + modelNormal.m22() * face.stepZ
            )
        }

        companion object {
            fun of(quad: BakedQuad): FaceGroup = if (quad.isShade) of(quad.direction) else UNSHADED

            fun of(direction: Direction): FaceGroup = entries[direction.ordinal + 1]
        }
    }

    /** Forge's continuous diffuse function for a world-space normal, using this dimension/shaderpack's cardinal values. */
    fun shade(level: BlockAndTintGetter, x: Float, y: Float, z: Float): Float {
        val lengthSquared = x * x + y * y + z * z
        val up = level.getShade(Direction.UP, true)
        val down = level.getShade(Direction.DOWN, true)
        val yFactor = (up + down) * 0.5f + (up - down) * 0.5f * y / sqrt(lengthSquared)

        return (
            (x * x * level.getShade(Direction.EAST, true) + y * y * yFactor + z * z * level.getShade(Direction.SOUTH, true)) /
                lengthSquared
            ).coerceIn(0f, 1f)
    }

    class CacheState {
        private var ao: Boolean? = null
        private var packAo = Float.NaN

        fun changed(): Boolean {
            val currentAo = Minecraft.useAmbientOcclusion()
            val currentPackAo = WorldRenderingSettings.INSTANCE.ambientOcclusionLevel
            if (ao == currentAo && packAo == currentPackAo) return false
            ao = currentAo
            packAo = currentPackAo

            return true
        }
    }

    /** Local positions must never sample unrelated live-world lighting. */
    open class LocalView(private val blocks: Map<BlockPos, BlockState>, lighting: BlockAndTintGetter) : BlockAndTintGetter by lighting {
        override fun getBlockState(pos: BlockPos): BlockState = blocks[pos] ?: Blocks.AIR.defaultBlockState()

        override fun getBlockEntity(pos: BlockPos): BlockEntity? = null

        override fun getFluidState(pos: BlockPos): FluidState = getBlockState(pos).fluidState

        override fun getShade(direction: Direction, shade: Boolean): Float = 1f

        override fun getShade(normalX: Float, normalY: Float, normalZ: Float, shade: Boolean): Float = 1f

        override fun getBrightness(layer: LightLayer, pos: BlockPos): Int = if (layer == LightLayer.SKY) 13 else 0

        override fun getRawBrightness(pos: BlockPos, skyDarken: Int): Int =
            maxOf(13 - skyDarken, getBlockState(pos).getLightEmission(this, pos))
    }

    class LitQuad(
        val quad: BakedQuad,
        private val brightness: FloatArray,
        private val red: Float,
        private val green: Float,
        private val blue: Float,
        private val lights: IntArray,
        private val overlay: Int,
        private val useQuadColor: Boolean
    ) {
        val group: FaceGroup = FaceGroup.of(quad)

        fun write(consumer: VertexConsumer, pose: PoseStack.Pose) {
            // Use the native AO overload so Iris can store AO separately in vertex alpha.
            consumer.putBulkData(pose, quad, brightness, red, green, blue, lights, overlay, useQuadColor)
        }
    }

    class Baker(
        private val renderer: ModelBlockRenderer = Minecraft.getInstance().blockRenderer.modelRenderer,
        private val useAo: Boolean = Minecraft.useAmbientOcclusion()
    ) {
        private val pose = PoseStack()
        private val random = RandomSource.create(42L)
        private val collector = Collector()

        fun bake(
            model: BakedModel,
            view: BlockAndTintGetter,
            pos: BlockPos,
            state: BlockState,
            type: RenderType,
            quads: List<BakedQuad>
        ): List<LitQuad> {
            if (quads.isEmpty()) return emptyList()
            return bakeModel(SelectedModel(model, quads), view, pos, state, type, ModelData.EMPTY, false)
        }

        fun bakeModel(
            model: BakedModel,
            view: BlockAndTintGetter,
            pos: BlockPos,
            state: BlockState,
            type: RenderType,
            data: ModelData,
            checkSides: Boolean
        ): List<LitQuad> {
            collector.quads.clear()
            pose.withPose {
                if (useAo && state.getLightEmission(view, pos) == 0 && model.useAmbientOcclusion(state, type)) {
                    renderer.tesselateWithAO(
                        view,
                        model,
                        state,
                        pos,
                        this,
                        collector,
                        checkSides,
                        random,
                        42L,
                        OverlayTexture.NO_OVERLAY,
                        data,
                        type
                    )
                } else {
                    renderer.tesselateWithoutAO(
                        view,
                        model,
                        state,
                        pos,
                        this,
                        collector,
                        checkSides,
                        random,
                        42L,
                        OverlayTexture.NO_OVERLAY,
                        data,
                        type
                    )
                }
            }

            return collector.quads.toList()
        }
    }

    private class SelectedModel(original: BakedModel, private val selected: List<BakedQuad>) : BakedModelWrapper<BakedModel>(original) {
        override fun getQuads(
            state: BlockState?,
            side: Direction?,
            random: RandomSource,
            data: ModelData,
            renderType: RenderType?
        ): List<BakedQuad> = if (side == null) selected else emptyList()
    }

    private class Collector : VertexConsumer {
        val quads = ArrayList<LitQuad>()

        override fun putBulkData(
            pose: PoseStack.Pose,
            quad: BakedQuad,
            brightness: FloatArray,
            red: Float,
            green: Float,
            blue: Float,
            lights: IntArray,
            overlay: Int,
            useQuadColor: Boolean
        ) {
            // Forge's lighter reuses its scratch arrays for the next quad.
            quads.add(LitQuad(quad, brightness.copyOf(), red, green, blue, lights.copyOf(), overlay, useQuadColor))
        }

        override fun vertex(x: Double, y: Double, z: Double): VertexConsumer = error("Expected native quad lighting")

        override fun color(red: Int, green: Int, blue: Int, alpha: Int): VertexConsumer = error("Expected native quad lighting")

        override fun uv(u: Float, v: Float): VertexConsumer = error("Expected native quad lighting")

        override fun overlayCoords(u: Int, v: Int): VertexConsumer = error("Expected native quad lighting")

        override fun uv2(u: Int, v: Int): VertexConsumer = error("Expected native quad lighting")

        override fun normal(x: Float, y: Float, z: Float): VertexConsumer = error("Expected native quad lighting")

        override fun endVertex(): Unit = error("Expected native quad lighting")

        override fun defaultColor(red: Int, green: Int, blue: Int, alpha: Int): Unit = error("Expected native quad lighting")

        override fun unsetDefaultColor(): Unit = error("Expected native quad lighting")
    }
}
