package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.client.renderer.machine.IControllerRenderer
import com.gregtechceu.gtceu.common.data.GTBlocks.HIGH_POWER_CASING
import com.gtladd.gtladditions.api.client.scene.SceneRendering
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.BlockEntityDispatch
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.common.machine.multiblock.controller.ArcanicAstrograph
import com.mojang.blaze3d.vertex.PoseStack
import net.irisshaders.iris.shadows.ShadowRenderingState
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.GTLCore
import org.gtlcore.gtlcore.api.machine.IGTLRenderDisabled
import org.gtlcore.gtlcore.client.ClientUtil
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
        if (IGTLRenderDisabled.isRenderDisabled(machine) || !machine.isActive) return

        val tick = RenderUtils.getSmoothTickDouble(machine, partialTicks)
        val facing = machine.frontFacing
        // The level's own source, wrapped by Iris under a shaderpack; shadows, breaking overlays and custom sources are not.
        val mainView = !ShadowRenderingState.areShadowsCurrentlyBeingRendered() && BlockEntityDispatch.isMainView(blockEntity)
        val center = center(facing)
        poseStack.withPose {
            translate(center.x, center.y, center.z)
            if (mainView) {
                renderOrbitObjects(tick, buffer, machine.parallelAmount, blockEntity.blockPos.asLong())
                SceneRendering.frame()?.let { ArcanicAstrographStarRenderer.submit(it, blockEntity, facing, tick) }
            } else {
                ArcanicAstrographStarRenderer.renderSpaceShell(this, buffer)
                renderOrbitObjects(tick, buffer, machine.parallelAmount, blockEntity.blockPos.asLong())
                ArcanicAstrographStarRenderer.render(this, buffer, tick)
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private fun PoseStack.renderOrbitObjects(tick: Double, buffer: MultiBufferSource, parallelAmount: Int, seed: Long) {
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

    @OnlyIn(Dist.CLIENT)
    /** Measured from the sky shell, not from the controller in front of it. */
    override fun shouldRender(blockEntity: BlockEntity, cameraPos: Vec3): Boolean {
        val facing = (blockEntity as IMachineBlockEntity).metaMachine.frontFacing
        return center(facing).add(Vec3.atLowerCornerOf(blockEntity.blockPos)).closerThan(cameraPos, viewDistance.toDouble())
    }

    /** The sky shell's centre relative to the controller block's corner. */
    private fun center(facing: Direction): Vec3 = Vec3(0.5 - facing.stepX * 16.0, 0.5, 0.5 - facing.stepZ * 16.0)
}
