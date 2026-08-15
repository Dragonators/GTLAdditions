package com.gtladd.gtladditions.mixin.gtlcore.recipe;

import org.gtlcore.gtlcore.api.recipe.BatchProcessing;

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;

import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BatchProcessing.class, remap = false)
public abstract class BatchProcessingMixin {

    @Inject(method = "isCrossRecipeParallel", at = @At("HEAD"), cancellable = true)
    private static void gtladditions$detectMutableCrossRecipeParallel(
                                                                      IRecipeLogicMachine machine,
                                                                      CallbackInfoReturnable<Boolean> cir) {
        if (machine.getRecipeLogic() instanceof MutableRecipesLogic<?> logic && logic.isMultipleRecipeMode()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "canConfigureBatchProcessing", at = @At("HEAD"), cancellable = true)
    private static void gtladditions$hideBatchConfiguratorForMutableCrossRecipeParallel(
                                                                                        IRecipeLogicMachine machine,
                                                                                        CallbackInfoReturnable<Boolean> cir) {
        if (machine.getRecipeLogic() instanceof MutableRecipesLogic<?> logic && logic.isMultipleRecipeMode()) {
            cir.setReturnValue(false);
        }
    }
}