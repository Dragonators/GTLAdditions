package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.client.renderer.machine.WorkableCasingMachineRenderer
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.machine.elevator.SpaceElevatorCableRenderer
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
import org.gtlcore.gtlcore.common.machine.multiblock.electric.SpaceElevatorMachine
import java.util.function.Consumer
import kotlin.math.sin

/** [climberHeight] is the climber's mid height above the controller; it swings 140 blocks either way. */
class SpaceElevatorRenderer(
    private val climberHeight: Double,
    private val cableLength: Int,
    private val viewDistance: Int
) : WorkableCasingMachineRenderer(
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
        val machine = (blockEntity as? IMachineBlockEntity)?.metaMachine as? SpaceElevatorMachine ?: return
        if (!machine.isFormed) return

        val ticks = RenderUtils.getSmoothTickDouble(machine, partialTicks)
        // The shaft is the column three blocks behind the controller; the cable stands on the power core below it.
        val x = when (machine.frontFacing) {
            Direction.WEST -> 3.5
            Direction.EAST -> -2.5
            else -> 0.5
        }
        val z = when (machine.frontFacing) {
            Direction.NORTH -> 3.5
            Direction.SOUTH -> -2.5
            else -> 0.5
        }
        val pos = blockEntity.blockPos
        SpaceElevatorCableRenderer.enqueue(blockEntity, pos.x + x, pos.y - 1.0, pos.z + z, cableLength, ticks)

        poseStack.withPose {
            translate(x, 1.0 + climberHeight + 140.0 * sin(ticks / 160.0), z)
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
    override fun getViewDistance(): Int = viewDistance

    private companion object {
        val CLIMBER_MODEL: ResourceLocation = GTLCore.id("obj/climber")
    }
}