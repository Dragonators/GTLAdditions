package com.gtladd.gtladditions.api.machine.logic;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.content.Content;

import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleRecipesMachine;
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe;
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipeBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
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
    protected @NotNull WirelessGTRecipe buildWirelessRecipe(@NotNull List<Content> item, @NotNull List<Content> fluid, int duration, BigInteger eut) {
        return WirelessGTRecipeBuilder
                .ofRaw(getMachine().getRecipeType())
                .output(ItemRecipeCapability.CAP, item)
                .output(FluidRecipeCapability.CAP, fluid)
                .duration(duration)
                .setWirelessEut(eut)
                .buildRawRecipe();
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
