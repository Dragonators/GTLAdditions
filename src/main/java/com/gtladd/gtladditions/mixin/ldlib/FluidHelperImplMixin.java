package com.gtladd.gtladditions.mixin.ldlib;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.side.fluid.forge.FluidHelperImpl;

import com.gtladd.gtladditions.integration.xei.LongFluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FluidHelperImpl.class)
public abstract class FluidHelperImplMixin {

    @Shadow(remap = false)
    public static FluidStack toFluidStack(net.minecraftforge.fluids.FluidStack fluidStack) {
        throw new AssertionError();
    }

    /**
     * @author Dragons
     * @reason fix
     */
    @Overwrite(remap = false)
    public static FluidStack fromRealFluidStack(Object fluidStack) {
        if (fluidStack instanceof LongFluidStack longFluidStack) {
            return FluidStack.create(longFluidStack.getFluid(), longFluidStack.getLongAmount(), longFluidStack.getTag());
        } else if (fluidStack instanceof net.minecraftforge.fluids.FluidStack stack) {
            return toFluidStack(stack);
        }

        return FluidStack.empty();
    }
}
