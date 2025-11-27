package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.utils.antichrist.ClientAnimationHelper.getClientRenderColor
import com.gtladd.gtladditions.utils.antichrist.ClientAnimationHelper.getClientRenderRadius
import com.gtladd.gtladditions.client.RenderMode
import com.gtladd.gtladditions.common.data.RotationParams
import com.gtladd.gtladditions.common.machine.muiltblock.controller.ForgeOfTheAntichrist
import com.gtladd.gtladditions.utils.CommonUtils.getRotatedRenderPosition
import com.gtladd.gtladditions.utils.RenderUtils
import com.gtladd.gtladditions.utils.antichrist.RingStructureVertexBuffer
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.math.Axis
import com.tterrag.registrate.util.entry.BlockEntry
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.RandomSource
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.utils.RenderUtil
import org.joml.Quaternionf
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

class ForgeOfAntichristRenderer(
    baseCasing: ResourceLocation,
    workableModel: ResourceLocation,
    partEntry: BlockEntry<Block>,
    partCasing: ResourceLocation
) : PartWorkableCasingMachineRenderer(baseCasing, workableModel, partEntry, partCasing) {

    @OnlyIn(Dist.CLIENT)
    override fun render(
        blockEntity: BlockEntity,
        partialTicks: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        combinedLight: Int,
        combinedOverlay: Int
    ) {
        if (blockEntity is IMachineBlockEntity) {
            val machine = blockEntity.metaMachine as? ForgeOfTheAntichrist ?: return

            if (machine.isFormed) {
                renderAllRings(machine, poseStack, partialTicks, machine.recipeLogic.isWorking)
            }

            if (machine.recipeLogic.isWorking) {
                val tick = RenderUtil.getSmoothTick(machine, partialTicks)
                val seed = blockEntity.blockPos.asLong()
                val starPos = getRotatedRenderPosition(BASE_DIRECTION, machine.frontFacing, -122.0, 0.0, 0.0)
                val renderMode = machine.starRitual.renderMode
                val (argb32, radiusMultiplier) = getRenderColorAndRadius(renderMode, machine, tick)

                val baseRadius = 0.175f * radiusMultiplier
                val middleRadius = baseRadius + minOf(0.0055f, baseRadius * 0.02f)
                val outerRadius = middleRadius * 1.02f

                renderBeaconToStar(poseStack, buffer, starPos, argb32, tick, blockEntity, outerRadius, renderMode)

                renderMultiLayerStar(
                    tick, poseStack, buffer, baseRadius, middleRadius, outerRadius,
                    getOrCreateCache(seed), argb32, starPos.x, starPos.y, starPos.z, renderMode
                )
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    override fun onAdditionalModel(registry: Consumer<ResourceLocation>) {
        super.onAdditionalModel(registry)
        registry.accept(STAR_LAYER_0)
        registry.accept(STAR_LAYER_2)
    }

    @OnlyIn(Dist.CLIENT)
    override fun hasTESR(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun isGlobalRenderer(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun getViewDistance(): Int = 384

    @OnlyIn(Dist.CLIENT)
    private data class RenderCache(val seed: Long) {
        val rotation0: RotationParams
        val rotation1: RotationParams
        val rotation2: RotationParams

        init {
            val random = RandomSource.create(seed)
            rotation0 = RenderUtils.createRandomRotation(random, 2.0f, 3.0f)
            rotation1 = RenderUtils.createRandomRotation(random, 0.9f, 1.5f)
            rotation2 = RenderUtils.createRandomRotation(random, 0.9f, 1.5f)
        }
    }

    @OnlyIn(Dist.CLIENT)
    companion object {
        const val STAR_OFFSET_X = -122.0
        val BASE_DIRECTION = Direction.EAST
        private val STAR_LAYER_0 = GTLAdditions.id("obj/star_layer_0")
        private val STAR_LAYER_2 = GTLAdditions.id("obj/star_layer_2")
        private val HALO_TEX = GTLAdditions.id("textures/block/obj/halo_tex2.png")
        private val CACHE_MAP = ConcurrentHashMap<Long, RenderCache>()

        private fun getRenderColorAndRadius(
            renderMode: RenderMode,
            machine: ForgeOfTheAntichrist,
            tick: Float
        ): Pair<Int, Float> {
            return when (renderMode) {
                RenderMode.NORMAL -> {
                    Pair(machine.rgbFromTime, machine.radiusMultiplier)
                }

                RenderMode.RAINBOW -> {
                    val rainbowColor = getRainbowColor(tick)
                    Pair(rainbowColor, machine.radiusMultiplier)
                }

                RenderMode.COLLAPSING -> {
                    val rainbowColor = getRainbowColor(tick)
                    val darkenedRainbow = getClientRenderColor(machine, rainbowColor)
                    val clientRadius = getClientRenderRadius(machine, machine.radiusMultiplier)
                    Pair(darkenedRainbow, clientRadius)
                }

                RenderMode.RECOVERING -> {
                    val clientColor = getClientRenderColor(machine, machine.rgbFromTime)
                    val clientRadius = getClientRenderRadius(machine, machine.radiusMultiplier)
                    Pair(clientColor, clientRadius)
                }
            }
        }

        private fun renderBeaconToStar(
            poseStack: PoseStack,
            buffer: MultiBufferSource,
            starPos: Vec3,
            argb32: Int,
            tick: Float,
            blockEntity: BlockEntity,
            outerRadius: Float,
            renderMode: RenderMode
        ) {
            if (renderMode == RenderMode.NORMAL ||
                renderMode == RenderMode.RAINBOW) {
                RenderUtils.drawBeaconToStar(
                    poseStack, buffer, starPos.x, starPos.y, starPos.z,
                    argb32, tick, blockEntity, outerRadius
                )
            }
        }

        private fun renderMultiLayerStar(
            tick: Float,
            poseStack: PoseStack,
            buffer: MultiBufferSource,
            baseRadius: Float,
            middleRadius: Float,
            outerRadius: Float,
            cache: RenderCache,
            argb32: Int,
            x: Double,
            y: Double,
            z: Double,
            renderMode: RenderMode
        ) {
            poseStack.pushPose()
            poseStack.translate(x, y, z)

            val rotationSpeedMultiplier = when (renderMode) {
                RenderMode.RAINBOW -> 1.7f
                RenderMode.COLLAPSING -> 2.6f
                else -> 1.0f
            }

            RenderUtils.renderStarLayer(
                poseStack, buffer, STAR_LAYER_2, middleRadius,
                cache.rotation2.axis, cache.rotation1.getAngle(tick * rotationSpeedMultiplier),
                argb32, RenderType.translucent()
            )

            RenderUtils.renderStarLayer(
                poseStack, buffer, STAR_LAYER_0, baseRadius,
                cache.rotation1.axis, cache.rotation0.getAngle(tick * rotationSpeedMultiplier),
                argb32, RenderType.solid()
            )

            RenderUtils.renderHaloLayer(
                poseStack, buffer, outerRadius,
                cache.rotation0.axis, cache.rotation0.getAngle(tick * rotationSpeedMultiplier),
                HALO_TEX, STAR_LAYER_2,
                1.0f, true
            )

            poseStack.popPose()
        }

        private fun renderAllRings(
            machine: ForgeOfTheAntichrist,
            poseStack: PoseStack,
            partialTicks: Float,
            isWorking: Boolean
        ) {
            val tick = if (isWorking) RenderUtil.getSmoothTick(machine, partialTicks) else 0f
            val ringPos = getRotatedRenderPosition(Direction.EAST, machine.frontFacing, -122.0, 0.0, 0.0)

            RenderSystem.enableBlend()
            RenderSystem.enableDepthTest()
            RenderSystem.blendFunc(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
            )
            RenderSystem.setShader { GameRenderer.getRendertypeSolidShader() }
            RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS)
            Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer()

            RingStructureVertexBuffer.ringBuffers.forEachIndexed { index, buffer ->
                poseStack.pushPose()
                poseStack.translate(ringPos.x, ringPos.y, ringPos.z)

                when (machine.frontFacing) {
                    Direction.NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270f))
                    Direction.SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90f))
                    Direction.WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(0f))
                    Direction.EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(180f))
                    else -> {}
                }

                if (isWorking) {
                    val direction = if (index == 1) -1.0f else 1.0f
                    val speedMultiplier = 0.4f + (index * 0.4f)
                    val angleOffset = index * 120f
                    val rotationAngle = (tick * speedMultiplier * 2.0f * direction + angleOffset) % 360.0f

                    poseStack.mulPose(Quaternionf().fromAxisAngleDeg(1.0f, 0.0f, 0.0f, rotationAngle))
                }

                buffer.bind()
                buffer.drawWithShader(
                    poseStack.last().pose(),
                    RenderSystem.getProjectionMatrix(),
                    RenderSystem.getShader()!!
                )
                VertexBuffer.unbind()
                poseStack.popPose()
            }

            RenderSystem.disableDepthTest()
            RenderSystem.disableBlend()
        }

        private fun getOrCreateCache(seed: Long): RenderCache =
            CACHE_MAP.computeIfAbsent(seed) { RenderCache(it) }

        private fun getRainbowColor(tick: Float): Int {
            val hue = (tick % 60) / 60.0f
            val color = Color.getHSBColor(hue, 1.0f, 1.0f)
            return (0xFF shl 24) or (color.red shl 16) or (color.green shl 8) or color.blue
        }
    }
}