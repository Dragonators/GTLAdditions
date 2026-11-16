package com.gtladd.gtladditions.mixin.gtlcore.machine.trait;

import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MECraftHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MECraftHandler.class)
public abstract class MECraftHandlerMixin {

    @ModifyVariable(method = "extractGTRecipe", at = @At("STORE"), ordinal = 1, require = 2, allow = 2, remap = false)
    private long gtladditions$fullOutputBudget(long remain, long parallelAmount, int tickDuration) {
        // Refresh both the initial and decremented budget, even when the total exceeds Long.MAX_VALUE.
        return parallelAmount == Long.MIN_VALUE ? Long.MAX_VALUE : remain;
    }

    @ModifyVariable(method = "extractGTRecipe", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1, remap = false)
    private int gtladditions$fullOutputDuration(int duration, long parallelAmount, int tickDuration) {
        return parallelAmount == Long.MIN_VALUE ? 1 : duration;
    }
}