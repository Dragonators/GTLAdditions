package com.gtladd.gtladditions.api.recipeslogic;

import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine;
import org.gtlcore.gtlcore.api.machine.trait.ILockRecipe;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.gtladd.gtladditions.api.machine.ILimitedDuration;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.*;

import javax.annotation.Nullable;

import static org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.*;

public class GTLAddMultipleRecipesLogic extends RecipeLogic implements ILockRecipe, ILimitedDuration {

    protected final ParallelMachine parallel;

    private final ILimitedDuration limited;

    public GTLAddMultipleRecipesLogic(ParallelMachine parallel) {
        super((IRecipeLogicMachine) parallel);
        this.parallel = parallel;
        limited = (ILimitedDuration) parallel;
    }

    @Override
    public WorkableElectricMultiblockMachine getMachine() {
        return (WorkableElectricMultiblockMachine) super.getMachine();
    }

    @Override
    public void findAndHandleRecipe() {
        lastRecipe = null;
        var match = this.isLock() ? getLockrecipe() : getRecipe();
        if (match != null && matchRecipeOutput(machine, match)) {
            setupRecipe(match);
        }
    }

    @Nullable
    protected GTRecipe getRecipe() {
        if (!machine.hasProxies()) return null;
        Set<GTRecipe> recipes = this.machine.getRecipeType().getLookup().findRecipeCollisions(machine);
        int length = 0;
        if (recipes != null) length = recipes.size();
        if (length == 0) return null;
        GTRecipe recipe = GTRecipeBuilder.ofRaw().buildRawRecipe();
        recipe.outputs.put(ItemRecipeCapability.CAP, new ArrayList<>());
        recipe.outputs.put(FluidRecipeCapability.CAP, new ArrayList<>());
        long maxEUt = getMachine().getOverclockVoltage();
        long totalEu = 0;
        int parallel = this.parallel.getMaxParallel();
        int[] parallels = new int[length];
        int index = 0;
        int nullAmount = 0;
        int remaining = parallel * 64;
        int average = parallel * 64 / length;
        ObjectArrayFIFOQueue<RecipeData> queue = new ObjectArrayFIFOQueue<>();
        List<GTRecipe> recipeList = new ObjectArrayList<>(length);
        for (var r : recipes) {
            if (matchRecipe(machine, r) && checkRecipe(r)) {
                int p = 1;
                for (var cap : r.inputs.keySet()) {
                    if (cap.doMatchInRecipe()) p = cap.getMaxParallelRatio(machine, r, parallel * 64);
                }
                recipeList.add(r);
                parallels[index] = Math.min(p, average);
                if (p > parallels[index]) queue.enqueue(new RecipeData(index, p - parallels[index]));
                remaining -= parallels[index++];
            } else nullAmount++;
        }
        if (nullAmount == length) return null;
        while (remaining > 0 && !queue.isEmpty()) {
            RecipeData recipeData = queue.dequeue();
            int canGive = remaining / queue.size();
            if (canGive > 0) {
                int give = Math.min(recipeData.remainingWant, canGive);
                parallels[recipeData.index] += give;
                remaining -= give;
                int newRemaining = recipeData.remainingWant - give;
                if (newRemaining > 0) queue.enqueue(new RecipeData(recipeData.index, newRemaining));
            } else break;
        }
        index = 0;
        for (GTRecipe r : recipeList) {
            if (parallels[index] > 1) r = r.copy(ContentModifier.multiplier(parallels[index++]), false);
            if (handleRecipeInput(machine, r)) {
                totalEu += RecipeHelper.getInputEUt(r) * r.duration;
                List<Content> item = r.outputs.get(ItemRecipeCapability.CAP);
                if (item != null) recipe.outputs.get(ItemRecipeCapability.CAP).addAll(item);
                List<Content> fluid = r.outputs.get(FluidRecipeCapability.CAP);
                if (fluid != null) recipe.outputs.get(FluidRecipeCapability.CAP).addAll(fluid);
            }
        }
        if (recipe.outputs.get(ItemRecipeCapability.CAP).equals(new ArrayList<>()) &&
                recipe.outputs.get(FluidRecipeCapability.CAP).equals(new ArrayList<>()))
            return null;
        int minDuration = limited.getLimitedDuration();
        double d = (double) totalEu / maxEUt;
        long eut = d > minDuration ? maxEUt : (long) (maxEUt * d / minDuration);
        recipe.tickInputs.put(EURecipeCapability.CAP, List.of(
                new Content(eut, 10000, 10000, 0, null, null)));
        recipe.duration = (int) Math.max(d, minDuration);
        return recipe;
    }

    private GTRecipe getLockrecipe() {
        if (!machine.hasProxies()) return null;
        if (this.getLockRecipe() == null) this.setLockRecipe(machine.getRecipeType().getLookup().findRecipe(machine));
        else if (!matchRecipe(machine, this.getLockRecipe()) && !checkRecipe(this.getLockRecipe())) return null;
        GTRecipe recipe = this.getLockRecipe();
        if (recipe == null) return null;
        recipe = parallelRecipe(recipe, this.parallel.getMaxParallel() * 64);
        int minDuration = limited.getLimitedDuration();
        long maxEUt = getMachine().getOverclockVoltage();
        double d = (double) RecipeHelper.getInputEUt(recipe) * recipe.duration / maxEUt;
        long eut = d > minDuration ? maxEUt : (long) (maxEUt * d / minDuration);
        RecipeHelper.setInputEUt(recipe, eut);
        recipe.duration = (int) Math.max(d, minDuration);
        return recipe;
    }

    protected GTRecipe parallelRecipe(GTRecipe recipe, int max) {
        int maxMultipliers = Integer.MAX_VALUE;
        for (RecipeCapability<?> cap : recipe.inputs.keySet()) {
            if (cap.doMatchInRecipe()) {
                int currentMultiplier = cap.getMaxParallelRatio(machine, recipe, max);
                if (currentMultiplier < maxMultipliers) maxMultipliers = currentMultiplier;
            }
        }
        if (maxMultipliers > 1) recipe = recipe.copy(ContentModifier.multiplier(maxMultipliers), false);
        return recipe;
    }

    @Override
    public void onRecipeFinish() {
        machine.afterWorking();
        if (lastRecipe != null) {
            lastRecipe.postWorking(this.machine);
            handleRecipeOutput(machine, lastRecipe);
        }
        var match = this.isLock() ? getLockrecipe() : getRecipe();
        if (match != null && matchRecipeOutput(machine, match)) {
            setupRecipe(match);
            return;
        }
        setStatus(Status.IDLE);
        progress = 0;
        duration = 0;
    }

    private boolean checkRecipe(GTRecipe recipe) {
        boolean eut = RecipeHelper.getRecipeEUtTier(recipe) < getMachine().getTier();
        boolean ebf_temp = recipe.data.getInt("ebf_temp") != 0 &&
                machine instanceof CoilWorkableElectricMultiblockMachine coilMachine &&
                coilMachine.getCoilType().getCoilTemperature() > recipe.data.getInt("ebf_temp");
        return eut || ebf_temp;
    }

    record RecipeData(int index, int remainingWant) {}
}
