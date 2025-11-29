package com.gtladd.gtladditions.common.machine.trait

import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer
import com.lowdragmc.lowdraglib.side.fluid.forge.FluidHelperImpl
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.fluids.capability.IFluidHandler
import java.util.function.Predicate

open class SuperNotifiableFluidTank(
    machine: MetaMachine,
    slots: Int,
    capacity: Long,
    io: IO
) : NotifiableFluidTank(machine, slots, capacity, io) {

    override fun exportToNearby(vararg facings: Direction) {
        if (!isEmpty) {
            machine.level?.let { level ->
                for (facing in facings) {
                    exportToTarget(
                        this,
                        machine.getFluidCapFilter(facing),
                        level,
                        machine.pos.relative(facing),
                        facing.opposite
                    )
                }
            }
        }
    }

    companion object {
        fun exportToTarget(
            source: IFluidTransfer,
            filter: Predicate<FluidStack>,
            level: Level,
            pos: BlockPos,
            direction: Direction?
        ) {
            val state = level.getBlockState(pos)
            if (!state.hasBlockEntity()) return

            val blockEntity = level.getBlockEntity(pos) ?: return
            val cap = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, direction).resolve()
            if (!cap.isPresent) return

            val target = cap.get()
            for (srcIndex in 0 until source.tanks) {
                val currentFluid = source.getFluidInTank(srcIndex)
                if (currentFluid.isEmpty || !filter.test(currentFluid)) {
                    continue
                }

                val toDrain = currentFluid.copy()
                var remainAmount = currentFluid.amount
                do {
                    val filled = target.fill(
                        FluidHelperImpl.toFluidStack(source.drain(toDrain, true)),
                        IFluidHandler.FluidAction.SIMULATE
                    )
                    if (filled > 0) {
                        toDrain.amount = filled.toLong()
                        target.fill(
                            FluidHelperImpl.toFluidStack(source.drain(toDrain, false)),
                            IFluidHandler.FluidAction.EXECUTE
                        )
                        remainAmount -= filled
                    } else {
                        break
                    }
                    toDrain.amount = remainAmount
                } while (!toDrain.isEmpty)
            }
        }
    }
}