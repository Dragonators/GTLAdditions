package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristDeferredRenderer
import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristRenderProfile
import com.gtladd.gtladditions.common.machine.multiblock.controller.ForgeOfTheAntichrist
import com.mojang.blaze3d.vertex.PoseStack
import com.tterrag.registrate.util.entry.BlockEntry
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.utils.RenderUtil

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
        if (blockEntity !is IMachineBlockEntity) return

        val machine = blockEntity.metaMachine as? ForgeOfTheAntichrist ?: return
        val isWorking = machine.isActiveOrStasisAnchored()
        if (!machine.isFormed && !isWorking) return

        val tick = if (isWorking) RenderUtil.getSmoothTick(machine, partialTicks) else 0f
        val profile = AntichristRenderProfile.create(machine, tick, isWorking)

        if (machine.isFormed) {
            AntichristDeferredRenderer.enqueueRing(blockEntity, profile)
        }

        if (isWorking) {
            AntichristDeferredRenderer.enqueue(blockEntity, profile)
        }
    }

    @OnlyIn(Dist.CLIENT)
    override fun hasTESR(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun isGlobalRenderer(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun getViewDistance(): Int = 384
}