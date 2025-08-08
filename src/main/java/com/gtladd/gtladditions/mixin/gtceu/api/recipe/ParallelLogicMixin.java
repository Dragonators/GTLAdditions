package com.gtladd.gtladditions.mixin.gtceu.api.recipe;

import org.gtlcore.gtlcore.api.recipe.IParallelLogic;

import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;

import com.google.common.primitives.Ints;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ParallelLogic.class)
public abstract class ParallelLogicMixin {

    /**
     * @author Dragons
     * @reason 解决超级并行仓丢失并行的问题
     */
    @Overwrite(remap = false)
    public static int getMaxRecipeMultiplier(@NotNull GTRecipe recipe, @NotNull IRecipeCapabilityHolder holder, int parallelAmount) {
        int minimum = Integer.MAX_VALUE;
        minimum = Ints.saturatedCast(Math.min(minimum, IParallelLogic.getMaxParallel(holder, recipe, parallelAmount)));
        for (RecipeCapability<?> cap : recipe.tickInputs.keySet()) {
            if (cap.doMatchInRecipe()) {
                minimum = Math.min(minimum, cap.getMaxParallelRatio(holder, recipe, parallelAmount));
            }
        }
        return minimum;
    }
}
