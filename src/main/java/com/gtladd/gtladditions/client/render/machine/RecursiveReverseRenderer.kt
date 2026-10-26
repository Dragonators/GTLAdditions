package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import com.gregtechceu.gtceu.client.renderer.machine.WorkableCasingMachineRenderer
import com.gtladd.gtladditions.client.render.machine.rrf.RrfEffectKind
import com.gtladd.gtladditions.client.render.machine.rrf.RrfEffectRenderer
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.GTLCore

@OnlyIn(Dist.CLIENT)
class RecursiveReverseRenderer(private val kind: RrfEffectKind) :
    WorkableCasingMachineRenderer(
        GTLCore.id(if (kind == RrfEffectKind.ARRAY) "block/create_casing" else "block/casings/dimension_injection_casing"),
        GTCEu.id(if (kind == RrfEffectKind.ARRAY) "block/multiblock/top/cosmos_simulation" else "block/multiblock/fusion_reactor")
    ) {
    override fun render(
        blockEntity: BlockEntity,
        partialTicks: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        combinedLight: Int,
        combinedOverlay: Int
    ) {
        val machine = (blockEntity as? IMachineBlockEntity)?.metaMachine as? MultiblockControllerMachine ?: return
        if (!RrfEffectRenderer.isActive(machine)) return
        RrfEffectRenderer.enqueue(blockEntity, machine, kind, partialTicks)
    }

    override fun hasTESR(blockEntity: BlockEntity): Boolean = true
    override fun isGlobalRenderer(blockEntity: BlockEntity): Boolean = true
    override fun getViewDistance(): Int = 128
}