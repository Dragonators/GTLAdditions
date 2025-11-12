package com.gtladd.gtladditions.integration.xei;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.google.common.primitives.Ints;
import org.jetbrains.annotations.NotNull;

public class LongFluidStack extends FluidStack {

    private long longAmount;

    public LongFluidStack(Fluid fluid, long amount) {
        super(fluid, Ints.saturatedCast(amount));
        this.longAmount = amount;
    }

    public LongFluidStack(Fluid fluid, long amount, CompoundTag nbt) {
        super(fluid, Ints.saturatedCast(amount), nbt);
        this.longAmount = amount;
    }

    public LongFluidStack(FluidStack stack, long amount) {
        super(stack, Ints.saturatedCast(amount));
        this.longAmount = amount;
    }

    public long getLongAmount() {
        return isEmpty() ? 0 : longAmount;
    }

    public void setLongAmount(long longAmount) {
        this.setAmount(Ints.saturatedCast(longAmount));
        this.longAmount = longAmount;
    }

    @Override
    public LongFluidStack copy() {
        return new LongFluidStack(getFluid(), longAmount, getTag());
    }

    @Override
    public boolean containsFluid(@NotNull FluidStack other) {
        return isFluidEqual(other) && other instanceof LongFluidStack longFluidStack ? this.longAmount >= longFluidStack.longAmount : this.longAmount >= other.getAmount();
    }

    @Override
    public boolean isFluidStackIdentical(FluidStack other) {
        return isFluidEqual(other) && other instanceof LongFluidStack longFluidStack ? this.longAmount == longFluidStack.longAmount : this.longAmount == other.getAmount();
    }
}
