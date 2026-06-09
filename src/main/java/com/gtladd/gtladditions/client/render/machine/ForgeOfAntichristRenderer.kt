package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristBeamRenderer
import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristRenderProfile
import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristRingRenderer
import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristStarRenderer
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
        if (blockEntity !is IMachineBlockEntity) return

        val machine = blockEntity.metaMachine as? ForgeOfTheAntichrist ?: return
        val isWorking = machine.recipeLogic.isWorking
        val tick = if (isWorking) RenderUtil.getSmoothTick(machine, partialTicks) else 0f
        val profile = AntichristRenderProfile.create(machine, tick, isWorking)

        if (isWorking) {
            AntichristStarRenderer.renderOpaque(profile, poseStack)
        }

        if (machine.isFormed) {
            AntichristRingRenderer.render(profile, poseStack)
        }

        if (isWorking) {
            AntichristStarRenderer.renderTransparent(profile, poseStack)
            AntichristBeamRenderer.render(profile, poseStack, blockEntity)
        }
    }

    @OnlyIn(Dist.CLIENT)
    override fun onAdditionalModel(registry: Consumer<ResourceLocation>) {
        super.onAdditionalModel(registry)
    }

    @OnlyIn(Dist.CLIENT)
    override fun hasTESR(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun isGlobalRenderer(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun getViewDistance(): Int = 384
}