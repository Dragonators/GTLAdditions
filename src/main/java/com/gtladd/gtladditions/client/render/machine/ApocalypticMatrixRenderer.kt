package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.client.renderer.machine.WorkableCasingMachineRenderer
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.machine.matrix.MatrixEffectRenderer
import com.gtladd.gtladditions.client.render.machine.matrix.MatrixEyeRenderer
import com.gtladd.gtladditions.client.render.machine.matrix.MatrixFrameTerrainRenderer
import com.gtladd.gtladditions.common.machine.multiblock.controller.ApocalypticWarpQuantumMatrix
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.api.machine.IGTLRenderDisabled

class ApocalypticMatrixRenderer(
    baseCasing: ResourceLocation,
    workableModel: ResourceLocation
) : WorkableCasingMachineRenderer(baseCasing, workableModel) {

    @OnlyIn(Dist.CLIENT)
    override fun render(
        blockEntity: BlockEntity,
        partialTicks: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        combinedLight: Int,
        combinedOverlay: Int
    ) {
        val machine = (blockEntity as? IMachineBlockEntity)?.metaMachine as? ApocalypticWarpQuantumMatrix ?: return
        if (!machine.isFormed) {
            MatrixEyeRenderer.forget(blockEntity)
            return
        }

        if (IGTLRenderDisabled.isRenderDisabled(machine)) {
            MatrixEyeRenderer.forget(blockEntity)
            MatrixFrameTerrainRenderer.enqueueStatic(blockEntity, machine)
            return
        }

        val eyeSeconds = RenderUtils.getSmoothTickDouble(machine, partialTicks) / 20.0
        MatrixEffectRenderer.enqueue(blockEntity, machine, machine.matrixEffects.animationTicks(partialTicks), eyeSeconds)
    }

    @OnlyIn(Dist.CLIENT)
    override fun hasTESR(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun isGlobalRenderer(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun getViewDistance(): Int = 384
}
