package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.client.renderer.machine.IControllerRenderer
import com.gregtechceu.gtceu.common.data.GTBlocks.HIGH_POWER_CASING
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.common.machine.multiblock.controller.ArcanicAstrograph
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.GTLCore
import org.gtlcore.gtlcore.client.ClientUtil
import org.gtlcore.gtlcore.utils.RenderUtil
import org.joml.Quaternionf
import java.util.function.Consumer

class ArcanicAstrographRender :
    PartWorkableCasingMachineRenderer(GTLCore.id("block/create_casing"), GTCEu.id("block/multiblock/cosmos_simulation"), HIGH_POWER_CASING, GTCEu.id("block/casings/hpca/high_power_casing")),
    IControllerRenderer {

    private val orbitRotation = Quaternionf()

    @OnlyIn(Dist.CLIENT)
    override fun render(
        blockEntity: BlockEntity,
        partialTicks: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        combinedLight: Int,
        combinedOverlay: Int
    ) {
        val machine = (blockEntity as? IMachineBlockEntity)?.metaMachine as? ArcanicAstrograph ?: return
        if (!machine.isActive) return

        val tick = RenderUtil.getSmoothTick(machine, partialTicks)
        val facing = machine.frontFacing
        poseStack.withPose {
            translate(0.5 - facing.stepX * 16.0, 0.5, 0.5 - facing.stepZ * 16.0)
            if (buffer is MultiBufferSource.BufferSource) {
                renderOrbitObjects(tick, buffer, machine.parallelAmount, blockEntity.blockPos.asLong())
                // Establish opaque depth before the immediate sky/star draws and additive shells.
                buffer.endBatch(RenderType.solid())
                ArcanicAstrographStarRenderer.renderCached(this, tick)
            } else {
                ArcanicAstrographStarRenderer.renderSpaceShell(this, buffer)
                renderOrbitObjects(tick, buffer, machine.parallelAmount, blockEntity.blockPos.asLong())
                ArcanicAstrographStarRenderer.render(this, buffer, tick)
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private fun PoseStack.renderOrbitObjects(tick: Float, buffer: MultiBufferSource, parallelAmount: Int, seed: Long) {
        val orbits = ArcanicAstrographOrbits.get(seed)
        val count = ArcanicAstrographOrbits.visibleCount(parallelAmount)
        val renderType = RenderType.solid()
        val consumer = buffer.getBuffer(renderType)

        for (index in 0 until count) {
            val orbit = orbits[index]
            withPose {
                orbit.applyTransform(this, tick, orbitRotation)
                RenderUtils.renderFullBrightModel(last(), consumer, ClientUtil.getBakedModel(orbit.model), renderType)
            }
        }
    }

    override fun onAdditionalModel(registry: Consumer<ResourceLocation>) {
        super.onAdditionalModel(registry)
        ArcanicAstrographOrbits.registerModels(registry)
    }

    @OnlyIn(Dist.CLIENT)
    override fun hasTESR(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun isGlobalRenderer(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun getViewDistance(): Int = 128
}