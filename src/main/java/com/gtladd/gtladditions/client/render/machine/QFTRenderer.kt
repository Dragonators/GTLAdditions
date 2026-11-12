package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine
import com.gregtechceu.gtceu.client.renderer.GTRendererProvider
import com.gregtechceu.gtceu.client.renderer.machine.WorkableCasingMachineRenderer
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.client.scene.SceneRendering
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.machine.qft.QFTForceFieldRenderer
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.EntityRenderersEvent
import net.minecraftforge.client.event.RegisterShadersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.gtlcore.gtlcore.GTLCore
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineA

@OnlyIn(Dist.CLIENT)
class QFTRenderer :
    WorkableCasingMachineRenderer(
        GTLCore.id("block/manipulator"),
        GTCEu.id("block/multiblock/fusion_reactor")
    ) {
    override fun render(
        blockEntity: BlockEntity,
        partialTicks: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        combinedLight: Int,
        combinedOverlay: Int
    ) {
        val machine = (blockEntity as? IMachineBlockEntity)?.metaMachine as? WorkableMultiblockMachine ?: return
        if (!machine.isFormed || !machine.isActive || !machine.isWorkingEnabled) return
        val frame = SceneRendering.frame() ?: return
        QFTForceFieldRenderer.submit(frame, blockEntity.blockPos, machine.frontFacing, machine.offsetTimer.toDouble() + partialTicks)
    }

    override fun hasTESR(blockEntity: BlockEntity): Boolean = true

    override fun isGlobalRenderer(blockEntity: BlockEntity): Boolean = true

    override fun getViewDistance(): Int = 128
}

@Suppress("unused")
@Mod.EventBusSubscriber(
    modid = GTLAdditions.MOD_ID,
    bus = Mod.EventBusSubscriber.Bus.MOD,
    value = [Dist.CLIENT]
)
internal object QFTRenderEvents {
    @SubscribeEvent
    @JvmStatic
    fun onRegisterRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        // GTLCore registered QFT without hasTESR; changing its model alone is insufficient.
        event.registerBlockEntityRenderer(MultiBlockMachineA.QFT.blockEntityType, GTRendererProvider::getOrCreate)
    }

    @SubscribeEvent
    @JvmStatic
    fun onRegisterShaders(event: RegisterShadersEvent) {
        RenderUtils.registerShader(
            event,
            GTLAdditions.id("gtladditions_qft_force_field"),
            DefaultVertexFormat.POSITION_TEX
        ) { QFTForceFieldRenderer.shader = it }
    }
}