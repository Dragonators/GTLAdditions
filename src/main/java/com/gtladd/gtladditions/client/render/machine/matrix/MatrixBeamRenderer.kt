package com.gtladd.gtladditions.client.render.machine.matrix

import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristBeamRenderer
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderContext
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredSceneRenderer
import com.gtladd.gtladditions.client.render.machine.heart.HeartSceneEffect
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.AABB
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Vector3f
import org.lwjgl.opengl.GL11C

/** Orders the six narrow beam volumes individually around black holes. */
@OnlyIn(Dist.CLIENT)
object MatrixBeamRenderer {
    private const val RADIUS = MatrixEffectGeometry.BEAM_RADIUS * MatrixIrisStarRenderer.BEAM_HALO_RADIUS_SCALE

    internal fun render(context: DeferredRenderContext, blockEntity: BlockEntity, frame: MatrixEyeAnimation.Frame, yaw: Float, flipped: Boolean) {
        if (MatrixShaders.irisBeamGlow == null) return
        val direct = ArrayList<Vector3f>(6)
        for (origin in MatrixIrisStarRenderer.beamOrigins(frame.time, yaw, flipped)) {
            if (!DeferredSceneRenderer.tryDeferEffect(Entry(blockEntity, frame, origin))) direct.add(origin)
        }
        if (direct.isNotEmpty()) {
            context.renderRelativeToCamera(blockEntity) { draw(blockEntity, this, frame, direct, 0) }
        }
    }

    internal class Entry(
        override val blockEntity: BlockEntity,
        private val frame: MatrixEyeAnimation.Frame,
        origin: Vector3f
    ) : HeartSceneEffect {
        private val origins = listOf(Vector3f(origin))
        override val bounds: AABB = blockEntity.blockPos.let { pos ->
            val x = pos.x + origin.x.toDouble()
            val y = pos.y + origin.y.toDouble()
            val z = pos.z + origin.z.toDouble()
            AABB(x - RADIUS, y, z - RADIUS, x + RADIUS, y + MatrixEffectGeometry.BEAM_HEIGHT, z + RADIUS)
        }
        override val supportsOrderedRendering: Boolean
            get() = MatrixShaders.irisBeamGlow != null

        // Screen blending has RGB transmission, which the scalar-alpha layer bins cannot represent.
        override val supportsLayeredRendering: Boolean
            get() = false
        override val depthCaptureKey: Any
            get() = MatrixBeamRenderer
        override val orderedNeedsSceneDepth: Boolean
            get() = false

        override fun captureSceneDepth(): Boolean = true

        override fun renderDirect(context: DeferredRenderContext) = renderOrdered(context, 0)

        override fun renderOrdered(context: DeferredRenderContext, handMask: Int) {
            context.renderRelativeToCamera(blockEntity) { draw(blockEntity, this, frame, origins, handMask) }
        }

        override fun renderLayered(context: DeferredRenderContext, depth: Int, count: Int, base: Int, rect: IntArray, local: Boolean, handMask: Int) {
            error("Matrix beams require an ordered scene")
        }
    }

    internal fun draw(blockEntity: BlockEntity, pose: PoseStack, frame: MatrixEyeAnimation.Frame, origins: List<Vector3f>, handMask: Int) {
        val shader = MatrixShaders.irisBeamGlow ?: return
        RenderUtils.withSavedRenderState {
            HeartSceneEffect.bindHandMask(shader, handMask)
            RenderSystem.enableDepthTest()
            RenderSystem.depthFunc(GL11C.GL_LEQUAL)
            RenderSystem.depthMask(false)
            RenderSystem.disableCull()
            RenderSystem.enableBlend()
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SourceFactor.ZERO,
                GlStateManager.DestFactor.ONE
            )
            MatrixIrisStarRenderer.configureBeamShader(shader)
            AntichristBeamRenderer.renderVerticalLayers(
                blockEntity,
                pose,
                frame.time * 20.0,
                origins,
                MatrixEffectGeometry.BEAM_HEIGHT,
                RADIUS,
                MatrixIrisStarRenderer.beamAlpha(frame),
                shader
            )
        }
    }
}