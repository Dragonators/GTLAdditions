package com.gtladd.gtladditions.mixin.ldlib;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.side.fluid.forge.FluidHelperImpl;

import com.gtladd.gtladditions.integration.xei.LongFluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidHelperImpl.class)
public abstract class FluidHelperImplMixin {

    @Inject(method = "fromRealFluidStack", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private static void gtladditions$convertLongFluid(Object fluidStack, CallbackInfoReturnable<FluidStack> cir) {
        if (fluidStack instanceof LongFluidStack longFluidStack) {
            cir.setReturnValue(FluidStack.create(longFluidStack.getFluid(), longFluidStack.getLongAmount(), longFluidStack.getTag()));
        }
    }
}