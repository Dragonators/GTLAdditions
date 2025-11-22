package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.client.renderer.machine.WorkableCasingMachineRenderer
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.machine.muiltblock.controller.HeartOfTheUniverse
import com.gtladd.gtladditions.utils.CommonUtils.getRotatedRenderPosition
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FastColor
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import java.util.function.Consumer
import kotlin.math.sin

class HeartOfTheUniverseRenderer : WorkableCasingMachineRenderer(
    GTCEu.id("block/casings/hpca/high_power_casing"),
    GTCEu.id("block/multiblock/cosmos_simulation")
) {

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
            val machine = blockEntity.metaMachine as? HeartOfTheUniverse ?: return
            if (machine.recipeLogic.isWorking) {
                val tick = RenderUtils.getSmoothTick(machine, partialTicks)
                val starPos = getRotatedRenderPosition(Direction.SOUTH, machine.frontFacing, 0.0, 36.0, -39.0)
                val seed = blockEntity.blockPos.asLong()

                renderStar(tick, poseStack, buffer, seed, starPos.x, starPos.y, starPos.z)
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    override fun onAdditionalModel(registry: Consumer<ResourceLocation>) {
        super.onAdditionalModel(registry)
        registry.accept(SPACE_MODEL)
    }

    @OnlyIn(Dist.CLIENT)
    override fun hasTESR(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun isGlobalRenderer(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun getViewDistance(): Int = 384

    @OnlyIn(Dist.CLIENT)
    companion object {
        private val SPACE_MODEL = GTLAdditions.id("obj/heart_of_universe")
        private val HALO_TEX = GTLAdditions.id("textures/block/obj/halo_tex1.png")
        private const val HEARTBEAT_FREQUENCY = 0.14f
        private const val HEARTBEAT_AMPLITUDE = 0.22f

        private fun calculateHeartbeatScale(tick: Float): Float {
            val phase = sin(tick * HEARTBEAT_FREQUENCY)
            val pulse = phase * phase
            return 1.0f - pulse * HEARTBEAT_AMPLITUDE
        }

        private fun renderStar(
            tick: Float,
            poseStack: PoseStack,
            buffer: MultiBufferSource,
            randomSeed: Long,
            x: Double,
            y: Double,
            z: Double
        ) {
            poseStack.pushPose()
            poseStack.translate(x, y, z)

            val heartbeatScale = calculateHeartbeatScale(tick)
            poseStack.scale(heartbeatScale, heartbeatScale, heartbeatScale)

            val rotation = RenderUtils.createRandomRotation(RandomSource.create(randomSeed), 0.5f, 2.0f)

            RenderUtils.renderHaloLayer(
                poseStack, buffer, 0.45f * 1.02f,
                rotation.axis, rotation.getAngle(tick),
                HALO_TEX, SPACE_MODEL
            )

            RenderUtils.renderStarLayer(
                poseStack, buffer, SPACE_MODEL, 0.45f,
                rotation.axis, rotation.getAngle(tick),
                FastColor.ARGB32.color(255, 255, 255, 255),
                RenderType.solid()
            )

            poseStack.popPose()
        }
    }
}