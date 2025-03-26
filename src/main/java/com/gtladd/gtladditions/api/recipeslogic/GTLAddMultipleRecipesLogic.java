package com.gtladd.gtladditions.api.recipeslogic;

import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class GTLAddMultipleRecipesLogic extends RecipeLogic {

    protected final ParallelMachine parallel;

    public GTLAddMultipleRecipesLogic(ParallelMachine parallel) {
        super((IRecipeLogicMachine) parallel);
        this.parallel = parallel;
    }

    @Override
    public WorkableElectricMultiblockMachine getMachine() {
        return (WorkableElectricMultiblockMachine) super.getMachine();
    }

    @Override
    public void findAndHandleRecipe() {
        lastRecipe = null;
        var match = getRecipe();
        if (match != null) {
            if (match.matchRecipe(this.machine).isSuccess()) {
                setupRecipe(match);
            }
        }
    }

    @Nullable
    protected GTRecipe getRecipe() {
        if (!machine.hasProxies()) return null;
        GTRecipe[] recipes = LookupRecipe();
        int length = recipes.length;
        if (length == 0) return null;
        GTRecipe match = recipes[0];
        if (match == null) return null;
        GTRecipe recipe = buildEmptyRecipe();
        recipe.outputs.put(ItemRecipeCapability.CAP, new ArrayList<>());
        recipe.outputs.put(FluidRecipeCapability.CAP, new ArrayList<>());
        long maxEUt = getMachine().getOverclockVoltage();
        long totalEu = 0;
        int parallel = this.parallel.getMaxParallel();
        for (int i = 0; i < 64; i++) {
            if (checkRecipe(match)) {
                match = recipes[(i + 1) % length];
                continue;
            }
            if (parallel > 1) {
                match = match.copy(ContentModifier.multiplier(parallel), false);
                match.parallels *= parallel;
            }
            GTRecipe input = buildEmptyRecipe();
            input.inputs.putAll(match.inputs);
            if (input.matchRecipe(machine).isSuccess() && input.handleRecipeIO(IO.IN, machine, getChanceCaches())) {
                totalEu += match.duration * RecipeHelper.getInputEUt(match);
                List<Content> item = match.outputs.get(ItemRecipeCapability.CAP);
                if (item != null) recipe.outputs.get(ItemRecipeCapability.CAP).addAll(item);
                List<Content> fluid = match.outputs.get(FluidRecipeCapability.CAP);
                if (fluid != null) recipe.outputs.get(FluidRecipeCapability.CAP).addAll(fluid);
            }
            match = recipes[(i + 1) % length];
            if (totalEu > maxEUt || match == null) break;
        }
        if (recipe.outputs.get(ItemRecipeCapability.CAP).equals(new ArrayList<>()) && recipe.outputs.get(FluidRecipeCapability.CAP).equals(new ArrayList<>())) return null;
        double d = (double) totalEu / maxEUt;
        long eut = d > 20 ? maxEUt : (long) (maxEUt * d / 20);
        recipe.tickInputs.put(EURecipeCapability.CAP, List.of(new Content(eut, ChanceLogic.getMaxChancedValue(), ChanceLogic.getMaxChancedValue(), 0, null, null)));
        recipe.duration = (int) Math.max(d, 20);
        return recipe;
    }

    protected GTRecipe[] LookupRecipe() {
        return new GTLAddRecipeLookup(machine.getRecipeType()).findAllRecipes(machine);
    }

    protected GTRecipe buildEmptyRecipe() {
        return GTRecipeBuilder.ofRaw().buildRawRecipe();
    }

    @Override
    public void onRecipeFinish() {
        machine.afterWorking();
        if (lastRecipe != null) {
            lastRecipe.postWorking(this.machine);
            lastRecipe.handleRecipeIO(IO.OUT, this.machine, this.chanceCaches);
        }
        GTRecipe match = getRecipe();
        if (match != null) if (match.matchRecipe(this.machine).isSuccess()) {
            setupRecipe(match);
            return;
        }
        setStatus(Status.IDLE);
        progress = 0;
        duration = 0;
    }

    private boolean checkRecipe(GTRecipe recipe) {
        boolean eut = RecipeHelper.getRecipeEUtTier(recipe) > getMachine().getTier();
        boolean ebf_temp = machine instanceof CoilWorkableElectricMultiblockMachine coilMachine &&
                coilMachine.getCoilType().getCoilTemperature() < recipe.data.getInt("ebf_temp");
        return eut || ebf_temp;
    }
}
