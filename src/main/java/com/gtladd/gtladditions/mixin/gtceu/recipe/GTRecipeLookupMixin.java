package com.gtladd.gtladditions.mixin.gtceu.recipe;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.lookup.AbstractMapIngredient;
import com.gregtechceu.gtceu.api.recipe.lookup.Branch;
import com.gregtechceu.gtceu.api.recipe.lookup.GTRecipeLookup;

import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(GTRecipeLookup.class)
public abstract class GTRecipeLookupMixin {

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    private @Nullable GTRecipe recurseIngredientTreeFindRecipeCollisions(@NotNull List<List<AbstractMapIngredient>> ingredients, @NotNull Branch branchMap, int index, int count, long skip, @NotNull Set<GTRecipe> collidingRecipes) {
        if (count == ingredients.size()) {
            return null;
        } else {
            List<AbstractMapIngredient> ingredient = new ObjectArrayList<>(ingredients.get(index));
            this.gtladditions$diveIngredientTreeFindRecipeCollisions(ingredient, branchMap, collidingRecipes);
            return null;
        }
    }

    @Unique
    private @Nullable GTRecipe gtladditions$diveIngredientTreeFindRecipeCollisions(@NotNull List<AbstractMapIngredient> ingredients, @NotNull Branch map, @NotNull Set<GTRecipe> collidingRecipes) {
        if (ingredients.isEmpty()) {
            return null;
        } else {
            for (AbstractMapIngredient o : ingredients) {
                Map<AbstractMapIngredient, Either<GTRecipe, Branch>> targetMap = determineRootNodes(o, map);
                Either<GTRecipe, Branch> result = targetMap.get(o);
                if (result != null) {
                    GTRecipe r = result.map((recipe) -> recipe,
                            (right) -> this.gtladditions$diveIngredientTreeFindRecipeCollisions(ingredients, right, collidingRecipes));
                    if (r != null) {
                        collidingRecipes.add(r);
                    }
                }
            }
            return null;
        }
    }

    @Shadow(remap = false)
    protected static @NotNull Map<AbstractMapIngredient, Either<GTRecipe, Branch>> determineRootNodes(@NotNull AbstractMapIngredient ingredient, @NotNull Branch branchMap) {
        throw new RuntimeException();
    }
}
