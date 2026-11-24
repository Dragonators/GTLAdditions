package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.client.renderer.machine.WorkableCasingMachineRenderer
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.machine.dimensionfocus.DimensionFocusOrbitRenderer
import com.gtladd.gtladditions.client.render.machine.dimensionfocus.DimensionFocusPocketRenderer
import com.gtladd.gtladditions.common.machine.multiblock.controller.DimensionFocusInfinityCraftingArray
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.GTLCore
import org.gtlcore.gtlcore.api.machine.IGTLRenderDisabled

class DimensionFocusInfinityCraftingArrayRenderer :
    WorkableCasingMachineRenderer(
        GTLCore.id("block/casings/sps_casing"),
        GTCEu.id("block/multiblock/research_station")
    ) {

    companion object {
        const val ORBIT_PERIOD_SECONDS = 30.0
    }

    @OnlyIn(Dist.CLIENT)
    override fun render(
        blockEntity: BlockEntity,
        partialTicks: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        combinedLight: Int,
        combinedOverlay: Int
    ) {
        val machine = (blockEntity as? IMachineBlockEntity)?.metaMachine as? DimensionFocusInfinityCraftingArray ?: return
        if (!machine.isFormed) return
        if (IGTLRenderDisabled.isRenderDisabled(machine)) {
            DimensionFocusOrbitRenderer.enqueue(blockEntity, machine, 0.0)
            return
        }

        val ticks = RenderUtils.getSmoothTickDouble(machine, partialTicks)
        DimensionFocusOrbitRenderer.enqueue(blockEntity, machine, ticks)
        DimensionFocusPocketRenderer.enqueue(blockEntity, machine, ticks)
    }

    @OnlyIn(Dist.CLIENT)
    override fun hasTESR(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun isGlobalRenderer(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun getViewDistance(): Int = 256
}