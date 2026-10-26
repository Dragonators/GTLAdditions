package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.client.renderer.machine.WorkableCasingMachineRenderer
import com.gtladd.gtladditions.client.render.machine.lighthunter.LightHunterVfxRenderer
import com.gtladd.gtladditions.common.machine.multiblock.controller.LightHunterSpaceStation
import com.mojang.blaze3d.vertex.PoseStack
import net.irisshaders.iris.shadows.ShadowRenderingState
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.config.ConfigHolder
import org.gtlcore.gtlcore.utils.RenderUtil

class LightHunterSpaceStationRenderer :
    WorkableCasingMachineRenderer(
        GTCEu.id("block/casings/hpca/high_power_casing"),
        GTCEu.id("block/multiblock/data_bank")
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
        val machine = (blockEntity as? IMachineBlockEntity)?.metaMachine as? LightHunterSpaceStation ?: return
        if ((!machine.isFormed && !machine.recipeLogic.isWorking) || ShadowRenderingState.areShadowsCurrentlyBeingRendered()) return
        val tick = LightHunterVfxRenderer.sampleAnimationTick(ConfigHolder.INSTANCE.graphics.enableSmoothAnimations) {
            RenderUtil.getSmoothTick(machine, partialTicks)
        }

        LightHunterVfxRenderer.enqueue(blockEntity, machine, tick)
    }

    @OnlyIn(Dist.CLIENT)
    override fun hasTESR(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun isGlobalRenderer(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun getViewDistance(): Int = 384
}