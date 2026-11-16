package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.client.renderer.GTRenderTypes
import com.gregtechceu.gtceu.client.renderer.machine.WorkableCasingMachineRenderer
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.withPose
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.GTLCore
import org.gtlcore.gtlcore.client.renderer.RenderBufferHelper
import org.gtlcore.gtlcore.common.machine.multiblock.electric.SpaceElevatorMachine
import java.util.function.Consumer
import kotlin.math.sin

class SpaceElevatorMKIIRenderer :
    WorkableCasingMachineRenderer(
        GTLCore.id("block/space_elevator_mechanical_casing"),
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
        if (blockEntity !is IMachineBlockEntity) return
        val machine = blockEntity.metaMachine as? SpaceElevatorMachine ?: return
        if (!machine.isFormed) return

        val tick = RenderUtils.getSmoothTickDouble(machine, partialTicks)
        var x = 0.5
        val y = 1.0
        var z = 0.5
        when (machine.frontFacing) {
            Direction.NORTH -> z = 3.5
            Direction.SOUTH -> z = -2.5
            Direction.WEST -> x = 3.5
            Direction.EAST -> x = -2.5
            else -> {}
        }

        poseStack.withPose {
            RenderBufferHelper.renderCylinder(
                this,
                buffer.getBuffer(GTRenderTypes.getLightRing()),
                x.toFloat(),
                (y - 2.0).toFloat(),
                z.toFloat(),
                0.3f,
                360.0f,
                10,
                0.0f,
                0.0f,
                0.0f,
                255.0f
            )
            translate(x, y + 230.0 + 140.0 * sin(tick / 160.0f), z)
            renderClimber(this, buffer)
        }
    }

    private fun renderClimber(poseStack: PoseStack, buffer: MultiBufferSource) {
        poseStack.withPose {
            scale(4.0f, 4.0f, 4.0f)
            RenderUtils.renderFullBrightModel(this, buffer, CLIMBER_MODEL, RenderType.solid())
        }
    }

    override fun onAdditionalModel(registry: Consumer<ResourceLocation>) {
        super.onAdditionalModel(registry)
        registry.accept(CLIMBER_MODEL)
    }

    @OnlyIn(Dist.CLIENT)
    override fun hasTESR(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun isGlobalRenderer(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun getViewDistance(): Int = 512

    private companion object {
        val CLIMBER_MODEL: ResourceLocation = GTLCore.id("obj/climber")
    }
}