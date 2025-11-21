package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.machine.muiltblock.controller.ForgeOfTheAntichrist
import com.gtladd.gtladditions.common.data.RotationParams
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.vertex.PoseStack
import com.tterrag.registrate.util.entry.BlockEntry
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
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
            if (machine.isActive) {
                val tick = machine.offsetTimer + partialTicks
                val seed = blockEntity.blockPos.asLong()
                val starPos = RenderUtils.getRotatedRenderPosition(Direction.EAST, machine.frontFacing, -121.5, 0.0, 0.0)

                val argb32 = machine.rgbFromTime
                val baseRadius = 0.175f * machine.radiusMultiplier
                val middleRadius = baseRadius + minOf(0.0055f, baseRadius * 0.02f)
                val outerRadius = middleRadius * 1.02f

                RenderUtils.drawBeaconToStar(poseStack, buffer, starPos.x, starPos.y, starPos.z, argb32, tick, blockEntity, outerRadius)

                renderMultiLayerStar(tick, poseStack, buffer, baseRadius, middleRadius, outerRadius, getOrCreateCache(seed), argb32, starPos.x, starPos.y, starPos.z)
            }
        }
    }

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

    companion object {
        private val STAR_LAYER_0 = GTLAdditions.id("obj/star_layer_0")
        private val STAR_LAYER_2 = GTLAdditions.id("obj/star_layer_2")
        private val HALO_TEX = GTLAdditions.id("textures/block/obj/halo_tex2.png")

        @OnlyIn(Dist.CLIENT)
        private val CACHE_MAP = ConcurrentHashMap<Long, RenderCache>()

        @OnlyIn(Dist.CLIENT)
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
            z: Double
        ) {
            poseStack.pushPose()
            poseStack.translate(x, y, z)

            RenderUtils.renderStarLayer(
                poseStack, buffer, STAR_LAYER_2, middleRadius,
                cache.rotation2.axis, cache.rotation1.getAngle(tick),
                argb32, RenderType.translucent()
            )

            RenderUtils.renderStarLayer(
                poseStack, buffer, STAR_LAYER_0, baseRadius,
                cache.rotation1.axis, cache.rotation0.getAngle(tick),
                argb32, RenderType.solid()
            )

            RenderUtils.renderHaloLayer(
                poseStack, buffer, outerRadius,
                cache.rotation0.axis, cache.rotation0.getAngle(tick),
                HALO_TEX, STAR_LAYER_2,
                1.0f, true
            )

            poseStack.popPose()
        }

        @OnlyIn(Dist.CLIENT)
        private fun getOrCreateCache(seed: Long): RenderCache =
            CACHE_MAP.computeIfAbsent(seed) { RenderCache(it) }
    }
}