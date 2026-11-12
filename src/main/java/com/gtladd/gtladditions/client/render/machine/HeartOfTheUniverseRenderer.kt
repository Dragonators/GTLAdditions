package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.client.renderer.machine.WorkableCasingMachineRenderer
import com.gtladd.gtladditions.api.client.scene.SceneRendering
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleRenderProfile
import com.gtladd.gtladditions.client.render.scene.SceneVolumes
import com.gtladd.gtladditions.common.machine.multiblock.controller.HeartOfTheUniverse
import com.gtladd.gtladditions.utils.CommonUtils.getRotatedRenderPosition
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.Direction
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

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
        val machine = (blockEntity as? IMachineBlockEntity)?.metaMachine as? HeartOfTheUniverse ?: return
        if (!machine.recipeLogic.isWorking) return
        val frame = SceneRendering.frame() ?: return

        val tick = RenderUtils.getSmoothTickDouble(machine, partialTicks)
        val starPos = getRotatedRenderPosition(Direction.SOUTH, machine.frontFacing, 0.0, 36.0, -39.0)
        val profile = HeartBlackHoleRenderProfile.create(tick, machine.frontFacing, starPos)
        SceneVolumes.submit(frame, blockEntity, profile)
    }

    @OnlyIn(Dist.CLIENT)
    override fun hasTESR(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun isGlobalRenderer(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun getViewDistance(): Int = 384
}