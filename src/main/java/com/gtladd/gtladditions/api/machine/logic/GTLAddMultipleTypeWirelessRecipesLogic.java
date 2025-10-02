package com.gtladd.gtladditions.api.machine.logic;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleRecipesMachine;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiPredicate;

public class GTLAddMultipleTypeWirelessRecipesLogic extends GTLAddMultipleWirelessRecipesLogic {

    public GTLAddMultipleTypeWirelessRecipesLogic(GTLAddWirelessWorkableElectricMultipleRecipesMachine parallel) {
        super(parallel);
    }

    public GTLAddMultipleTypeWirelessRecipesLogic(GTLAddWirelessWorkableElectricMultipleRecipesMachine parallel, BiPredicate<GTRecipe, IRecipeLogicMachine> beforeWorking) {
        super(parallel, beforeWorking);
    }

    @Override
    protected @NotNull GTRecipe buildRawRecipe() {
        GTRecipe recipe = new GTRecipeBuilder(GTCEu.id("raw"), getMachine().getRecipeType()).buildRawRecipe();
        recipe.outputs.put(ItemRecipeCapability.CAP, new ObjectArrayList<>());
        recipe.outputs.put(FluidRecipeCapability.CAP, new ObjectArrayList<>());
        return recipe;
    }

    @Override
    protected @NotNull Set<GTRecipe> lookupRecipeIterator() {
        if (this.isLock()) {
            if (this.getLockRecipe() == null) {
                GTRecipe recipe = null;
                for (GTRecipeType recipeType : machine.getRecipeTypes()) {
                    recipe = recipeType.getLookup().find(machine, this::checkRecipe);
                    if (recipe != null) break;
                }
                this.setLockRecipe(recipe);
            } else if (!checkRecipe(this.getLockRecipe())) return Collections.emptySet();
            return Collections.singleton(this.getLockRecipe());
        } else {
            var recipeSet = new ObjectOpenHashSet<GTRecipe>();
            for (GTRecipeType recipeType : machine.getRecipeTypes()) {
                var iterator = recipeType.getLookup().getRecipeIterator(machine, this::checkRecipe);
                while (iterator.hasNext()) recipeSet.add(iterator.next());
            }
            recipeSet.remove(null);
            return recipeSet;
        }
    }
}
