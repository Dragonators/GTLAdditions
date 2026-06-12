package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.client.renderer.machine.WorkableCasingMachineRenderer
import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleRenderProfile
import com.gtladd.gtladditions.client.render.machine.heart.HeartDeferredRenderer
import com.gtladd.gtladditions.common.machine.multiblock.controller.HeartOfTheUniverse
import com.gtladd.gtladditions.utils.CommonUtils.getRotatedRenderPosition
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.Direction
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.utils.RenderUtil

class HeartOfTheUniverseRenderer :
    WorkableCasingMachineRenderer(
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
                val tick = RenderUtil.getSmoothTick(machine, partialTicks)
                val starPos = getRotatedRenderPosition(Direction.SOUTH, machine.frontFacing, 0.0, 36.0, -39.0)
                val profile = HeartBlackHoleRenderProfile.create(tick, machine.frontFacing, starPos)

                HeartDeferredRenderer.enqueue(blockEntity, profile)
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    override fun hasTESR(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun isGlobalRenderer(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun getViewDistance(): Int = 384
}