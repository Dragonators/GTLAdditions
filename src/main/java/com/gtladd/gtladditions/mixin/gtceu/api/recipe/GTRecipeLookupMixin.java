package com.gtladd.gtladditions.mixin.gtceu.api.recipe;

import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine;
import org.gtlcore.gtlcore.api.recipe.IAdditionalRecipeIterator;
import org.gtlcore.gtlcore.common.machine.trait.MultipleRecipesLogic;

import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.lookup.GTRecipeLookup;
import com.gregtechceu.gtceu.api.recipe.lookup.RecipeIterator;

import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;

@Mixin(value = GTRecipeLookup.class, priority = 2000)
public abstract class GTRecipeLookupMixin {

    @ModifyReturnValue(method = "getRecipeIterator", at = @At("RETURN"), remap = false, require = 1)
    private RecipeIterator gtladditions$enableCompleteIteration(RecipeIterator iterator,
                                                                IRecipeCapabilityHolder holder,
                                                                Predicate<GTRecipe> canHandle) {
        if (holder instanceof IRecipeCapabilityMachine && holder instanceof IRecipeLogicMachine machine) {
            var logic = machine.getRecipeLogic();
            boolean useCompleteIteration = logic instanceof MultipleRecipesLogic ||
                    logic instanceof MutableRecipesLogic<?> mutable && mutable.isMultipleRecipeMode();
            ((IAdditionalRecipeIterator) iterator).setUseDiveIngredientTreeFind(useCompleteIteration);
        }
        return iterator;
    }
}
