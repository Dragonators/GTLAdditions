package com.gtladd.gtladditions.mixin.gtceu.recipe;

import org.gtlcore.gtlcore.api.recipe.IRecipeIterator;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.lookup.AbstractMapIngredient;
import com.gregtechceu.gtceu.api.recipe.lookup.Branch;
import com.gregtechceu.gtceu.api.recipe.lookup.GTRecipeLookup;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;
import java.util.Set;

@Mixin(GTRecipeLookup.class)
public abstract class GTRecipeLookupMixin {

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    private @Nullable GTRecipe recurseIngredientTreeFindRecipeCollisions(@NotNull List<List<AbstractMapIngredient>> ingredients,
                                                                         @NotNull Branch branchMap, int index, int count, long skip,
                                                                         @NotNull Set<GTRecipe> collidingRecipes) {
        if (count == ingredients.size()) return null;
        else {
            List<AbstractMapIngredient> ingredient = new ObjectArrayList<>(ingredients.get(index));
            IRecipeIterator.diveIngredientTreeFindRecipeCollection(ingredient, branchMap, (r) -> true, collidingRecipes);
            return null;
        }
    }
}
