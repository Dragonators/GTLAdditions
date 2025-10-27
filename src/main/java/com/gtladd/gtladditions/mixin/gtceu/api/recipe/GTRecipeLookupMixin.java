package com.gtladd.gtladditions.mixin.gtceu.api.recipe;

import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine;
import org.gtlcore.gtlcore.api.recipe.IAdditionalRecipeIterator;
import org.gtlcore.gtlcore.common.machine.trait.MultipleRecipesLogic;
import org.gtlcore.gtlcore.mixin.gtm.api.recipe.RecipeIteratorAccessor;

import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.lookup.AbstractMapIngredient;
import com.gregtechceu.gtceu.api.recipe.lookup.GTRecipeLookup;
import com.gregtechceu.gtceu.api.recipe.lookup.RecipeIterator;

import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.function.Predicate;

@Mixin(value = GTRecipeLookup.class, priority = 2000)
public abstract class GTRecipeLookupMixin {

    @Shadow(remap = false)
    @Final
    private GTRecipeType recipeType;

    @Shadow(remap = false)
    protected @Nullable List<List<AbstractMapIngredient>> prepareRecipeFind(@NotNull IRecipeCapabilityHolder holder) {
        throw new AssertionError();
    }

    /**
     * @author Dragons
     * @reason 为Mutable机器添加完整的Iterator
     */
    @Overwrite(remap = false)
    public @NotNull RecipeIterator getRecipeIterator(@NotNull IRecipeCapabilityHolder holder, @NotNull Predicate<GTRecipe> canHandle) {
        List<List<AbstractMapIngredient>> list = this.prepareRecipeFind(holder);
        RecipeIterator iterator = RecipeIteratorAccessor.newRecipeIterator(this.recipeType, list, canHandle);

        if (holder instanceof IRecipeCapabilityMachine rlm) {
            var parts = rlm.getMEPatternRecipeHandleParts();
            if (!parts.isEmpty()) {
                List<GTRecipe> meRecipes = new ObjectArrayList<>();
                for (var part : parts) {
                    meRecipes.addAll(part.getCachedGTRecipe());
                }
                meRecipes = meRecipes.stream().filter(r -> r.recipeType == recipeType).toList();
                if (!meRecipes.isEmpty()) {
                    ((IAdditionalRecipeIterator) iterator).setAdditionalRecipes(meRecipes);
                }
            }

            if (holder instanceof IRecipeLogicMachine machine) {
                final var logic = machine.getRecipeLogic();
                if (logic instanceof MultipleRecipesLogic ||
                        (logic instanceof MutableRecipesLogic<?> mutableRecipesLogic &&
                                mutableRecipesLogic.isMultipleRecipeMode())) {
                    ((IAdditionalRecipeIterator) iterator).setUseDiveIngredientTreeFind(true);
                }
            }
        }

        return iterator;
    }
}
