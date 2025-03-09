package com.gtladd.gtladditions.api.recipeslogic;

import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.lookup.AbstractMapIngredient;
import com.gregtechceu.gtceu.api.recipe.lookup.GTRecipeLookup;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;

public class GTLAddRecipeLookup extends GTRecipeLookup {

    private GTRecipeType recipeType;

    public GTLAddRecipeLookup(GTRecipeType recipeType) {
        super(recipeType);
        this.recipeType = recipeType;
    }

    private AdvancedRecipeIterator GetRecipeIterator(@NotNull IRecipeCapabilityHolder holder, @NotNull Predicate<GTRecipe> canHandle) {
        List<List<AbstractMapIngredient>> list = prepareRecipeFind(holder);
        return new AdvancedRecipeIterator(recipeType, list, canHandle);
    }

    /**
     * 查找所有匹配的配方（去重）
     */
    @NotNull
    public GTRecipe[] findAllRecipes(@NotNull IRecipeCapabilityHolder holder) {
        List<GTRecipe> recipes = new ArrayList<>();
        AdvancedRecipeIterator iterator = GetRecipeIterator(holder, recipe -> recipe.matchRecipe(holder).isSuccess());
        while (iterator.hasNext()) {
            GTRecipe recipe = iterator.next();
            if (recipe != null && !recipes.contains(recipe)) {
                recipes.add(recipe);
            }
        }
        return recipes.toArray(new GTRecipe[0]);
    }

    public GTRecipe findOneRecipe(@NotNull IRecipeCapabilityHolder holder) {
        GTRecipe[] recipe = findAllRecipes(holder);
        return recipe.length > 0 ? recipe[0] : null;
    }

    static class AdvancedRecipeIterator implements Iterator<GTRecipe> {

        private final GTRecipeLookup lookup;
        private final List<List<AbstractMapIngredient>> ingredients;
        private final Predicate<GTRecipe> canHandle;
        private final Set<GTRecipe> visitedRecipes = new HashSet<>();
        private int currentIndex = 0;
        private GTRecipe nextRecipe = null;

        public AdvancedRecipeIterator(@NotNull GTRecipeType recipeType,
                                      List<List<AbstractMapIngredient>> ingredients,
                                      @NotNull Predicate<GTRecipe> canHandle) {
            this.lookup = recipeType.getLookup();
            this.ingredients = ingredients != null ? ingredients : Collections.emptyList();
            this.canHandle = canHandle;
            findNextRecipe();
        }

        @Override
        public boolean hasNext() {
            return nextRecipe != null;
        }

        @Override
        public GTRecipe next() {
            if (nextRecipe == null) throw new NoSuchElementException();
            GTRecipe result = nextRecipe;
            visitedRecipes.add(result);
            findNextRecipe();
            return result;
        }

        private void findNextRecipe() {
            nextRecipe = null;
            // 遍历所有可能的起始点
            while (currentIndex < ingredients.size()) {
                // 递归查找未访问的配方
                GTRecipe candidate = lookup.recurseIngredientTreeFindRecipe(
                        ingredients,
                        lookup.getLookup(),
                        recipe -> canHandle.test(recipe) && !visitedRecipes.contains(recipe),
                        currentIndex,
                        0,
                        (1L << currentIndex));
                if (candidate != null) {
                    nextRecipe = candidate;
                    return;
                }
                // 当前起始点无更多配方，尝试下一个起始点
                currentIndex++;
            }
        }

        public void reset() {
            currentIndex = 0;
            visitedRecipes.clear();
            findNextRecipe();
        }
    }
}
