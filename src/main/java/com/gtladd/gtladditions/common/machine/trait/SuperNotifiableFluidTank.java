package com.gtladd.gtladditions.common.machine.trait;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer;
import com.lowdragmc.lowdraglib.side.fluid.forge.FluidHelperImpl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;

import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class SuperNotifiableFluidTank extends NotifiableFluidTank {

    public SuperNotifiableFluidTank(MetaMachine machine, int slots, long capacity, IO io) {
        super(machine, slots, capacity, io);
    }

    @Override
    public void exportToNearby(Direction... facings) {
        if (!this.isEmpty()) {
            Level level = this.getMachine().getLevel();
            BlockPos pos = this.getMachine().getPos();

            for (Direction facing : facings) {
                exportToTarget(this, this.getMachine().getFluidCapFilter(facing), level, pos.relative(facing), facing.getOpposite());
            }

        }
    }

    public static void exportToTarget(IFluidTransfer source, Predicate<FluidStack> filter, Level level, BlockPos pos, @Nullable Direction direction) {
        BlockState state = level.getBlockState(pos);
        if (state.hasBlockEntity()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                var cap = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, direction).resolve();
                if (cap.isPresent()) {
                    var target = cap.get();
                    for (int srcIndex = 0; srcIndex < source.getTanks(); srcIndex++) {
                        var currentFluid = source.getFluidInTank(srcIndex);
                        if (currentFluid.isEmpty() || !filter.test(currentFluid)) {
                            continue;
                        }

                        var toDrain = currentFluid.copy();
                        var remainAmount = currentFluid.getAmount();
                        do {
                            var filled = target.fill(FluidHelperImpl.toFluidStack(source.drain(toDrain, true)), IFluidHandler.FluidAction.SIMULATE);
                            if (filled > 0) {
                                toDrain = currentFluid.copy();
                                toDrain.setAmount(filled);
                                target.fill(FluidHelperImpl.toFluidStack(source.drain(toDrain, false)), IFluidHandler.FluidAction.EXECUTE);
                                remainAmount -= filled;
                                if (remainAmount <= 0) break;
                            } else {
                                break;
                            }
                            toDrain = currentFluid.copy();
                            toDrain.setAmount(remainAmount);
                        } while (!toDrain.isEmpty());
                    }
                }
            }
        }
    }
}
