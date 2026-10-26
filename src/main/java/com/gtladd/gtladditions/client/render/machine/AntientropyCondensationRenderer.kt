package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.client.renderer.machine.WorkableCasingMachineRenderer
import com.gtladd.gtladditions.client.render.machine.condensation.CondensationCoreMesh
import com.gtladd.gtladditions.client.render.machine.condensation.CondensationCoreRenderer
import com.gtladd.gtladditions.common.machine.multiblock.controller.AntientropyCondensationCenter
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.GTLCore
import org.gtlcore.gtlcore.utils.RenderUtil
import java.util.function.Consumer

@OnlyIn(Dist.CLIENT)
class AntientropyCondensationRenderer :
    WorkableCasingMachineRenderer(
        GTLCore.id("block/casings/antifreeze_heatproof_machine_casing"),
        GTCEu.id("block/multiblock/vacuum_freezer")
    ) {
    override fun onAdditionalModel(registry: Consumer<ResourceLocation>) {
        super.onAdditionalModel(registry)
        registry.accept(CondensationCoreMesh.CORE_MODEL)
        registry.accept(CondensationCoreMesh.SHELL_MODEL)
    }

    override fun render(
        blockEntity: BlockEntity,
        partialTicks: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        combinedLight: Int,
        combinedOverlay: Int
    ) {
        val machine = (blockEntity as? IMachineBlockEntity)?.metaMachine as? AntientropyCondensationCenter ?: return
        if (!machine.isFormed) return
        val flowPhase = (RenderUtil.getSmoothTick(machine, partialTicks) % 80f) / 20f
        CondensationCoreRenderer.enqueue(blockEntity, machine, machine.condensationEffects.animationTicks(partialTicks), flowPhase)
    }

    override fun hasTESR(blockEntity: BlockEntity): Boolean = true

    override fun isGlobalRenderer(blockEntity: BlockEntity): Boolean = true

    override fun getViewDistance(): Int = 128
}