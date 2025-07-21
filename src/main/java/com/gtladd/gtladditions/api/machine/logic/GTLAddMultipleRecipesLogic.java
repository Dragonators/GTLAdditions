package com.gtladd.gtladditions.api.machine.logic;

import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine;
import org.gtlcore.gtlcore.api.machine.trait.ILockRecipe;
import org.gtlcore.gtlcore.api.recipe.IParallelLogic;
import org.gtlcore.gtlcore.api.recipe.RecipeResult;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.google.common.primitives.Ints;
import com.gtladd.gtladditions.api.machine.IGTLAddMultiRecipe;
import it.unimi.dsi.fastutil.objects.*;

import java.util.*;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import static org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.*;

public class GTLAddMultipleRecipesLogic extends RecipeLogic implements ILockRecipe {

    protected final ParallelMachine parallel;

    private final IGTLAddMultiRecipe limited;

    private static final int MAX_THREADS = 128;

    protected BiPredicate<GTRecipe, IRecipeLogicMachine> beforeWorking;

    public GTLAddMultipleRecipesLogic(ParallelMachine parallel) {
        super((IRecipeLogicMachine) parallel);
        this.parallel = parallel;
        this.limited = (IGTLAddMultiRecipe) parallel;
    }

    public GTLAddMultipleRecipesLogic(ParallelMachine parallel, BiPredicate<GTRecipe, IRecipeLogicMachine> beforeWorking) {
        super((IRecipeLogicMachine) parallel);
        this.parallel = parallel;
        this.limited = (IGTLAddMultiRecipe) parallel;
        this.beforeWorking = beforeWorking;
    }

    @Override
    public WorkableElectricMultiblockMachine getMachine() {
        return (WorkableElectricMultiblockMachine) super.getMachine();
    }

    @Override
    public void findAndHandleRecipe() {
        lastRecipe = null;
        var match = this.isLock() ? getLockGTRecipe() : getGTRecipe();
        if (match != null && matchRecipeOutput(machine, match)) {
            setupRecipe(match);
        }
    }

    @Nullable
    protected GTRecipe getGTRecipe() {
        if (!machine.hasProxies()) return null;
        Set<GTRecipe> recipes = this.machine.getRecipeType().getLookup().findRecipeCollisions(machine);
        int length = 0;
        if (recipes != null) length = recipes.size();
        if (length == 0) return null;
        long parallel = this.parallel.getMaxParallel();
        long[] parallels = new long[length];
        int index = 0;
        int nullAmount = 0;
        long remaining = parallel * MAX_THREADS;
        ObjectArrayFIFOQueue<RecipeData> queue = new ObjectArrayFIFOQueue<>(length);
        List<GTRecipe> recipeList = new ObjectArrayList<>(length);
        for (var r : recipes) {
            if (matchRecipe(machine, r) && checkRecipe(r)) {
                long p = IParallelLogic.getMaxParallel(this.machine, r, parallel * MAX_THREADS);
                recipeList.add(r);
                parallels[index] = Math.min(p, parallel * MAX_THREADS / length);
                if (p > parallels[index]) queue.enqueue(new RecipeData(index, p - parallels[index]));
                remaining -= parallels[index++];
            } else nullAmount++;
        }
        if (nullAmount == length) return null;
        if (this.beforeWorking != null && !this.beforeWorking.test(null, this.machine)) return null;
        while (remaining > 0 && !queue.isEmpty()) {
            RecipeData recipeData = queue.dequeue();
            long canGive = remaining / (queue.size() + 1);
            if (canGive > 0) {
                long give = Math.min(recipeData.remainingWant, canGive);
                parallels[recipeData.index] += give;
                remaining -= give;
                long newRemaining = recipeData.remainingWant - give;
                if (newRemaining > 0) queue.enqueue(new RecipeData(recipeData.index, newRemaining));
            } else break;
        }
        index = 0;
        GTRecipe recipe = GTRecipeBuilder.ofRaw().buildRawRecipe();
        recipe.outputs.put(ItemRecipeCapability.CAP, new ArrayList<>());
        recipe.outputs.put(FluidRecipeCapability.CAP, new ArrayList<>());
        long maxEUt = getMachine().getOverclockVoltage();
        long totalEu = 0;
        for (GTRecipe r : recipeList) {
            if (parallels[index] > 1) r = r.copy(ContentModifier.multiplier(parallels[index]), false);
            r.parallels = Ints.saturatedCast(parallels[index++]);
            IParallelLogic.getRecipeOutputChance(machine, r);
            if (handleRecipeInput(machine, r)) {
                totalEu += RecipeHelper.getInputEUt(r) * r.duration;
                List<Content> item = r.outputs.get(ItemRecipeCapability.CAP);
                if (item != null) recipe.outputs.get(ItemRecipeCapability.CAP).addAll(item);
                List<Content> fluid = r.outputs.get(FluidRecipeCapability.CAP);
                if (fluid != null) recipe.outputs.get(FluidRecipeCapability.CAP).addAll(fluid);
            }
            if (totalEu / maxEUt > 1200) break;
        }
        if (recipe.outputs.get(ItemRecipeCapability.CAP).isEmpty() && recipe.outputs.get(FluidRecipeCapability.CAP).isEmpty()) {
            if (totalEu / maxEUt > 1200) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_EU);
            return null;
        }
        int minDuration = limited.getLimitedDuration();
        double d = (double) totalEu / maxEUt;
        long eut = d > minDuration ? maxEUt : (long) (maxEUt * d / minDuration);
        recipe.tickInputs.put(EURecipeCapability.CAP, List.of(
                new Content(eut, 10000, 10000, 0, null, null)));
        recipe.duration = (int) Math.max(d, minDuration);
        return recipe;
    }

    private GTRecipe getLockGTRecipe() {
        if (!machine.hasProxies()) return null;
        if (this.beforeWorking != null && !this.beforeWorking.test(null, this.machine)) return null;
        if (this.getLockRecipe() == null) this.setLockRecipe(machine.getRecipeType().getLookup().find(machine,
                (r) -> matchRecipe(machine, r) && checkRecipe(r)));
        else if (!matchRecipe(machine, this.getLockRecipe()) && !checkRecipe(this.getLockRecipe())) return null;
        GTRecipe recipe = this.getLockRecipe();
        if (recipe == null) return null;
        long parallel = IParallelLogic.getMaxParallel(machine, recipe, (long) this.parallel.getMaxParallel() * MAX_THREADS);
        if (parallel > 1) recipe = recipe.copy(ContentModifier.multiplier(parallel), false);
        recipe.parallels = Ints.saturatedCast(parallel);
        int minDuration = limited.getLimitedDuration();
        long maxEUt = getMachine().getOverclockVoltage();
        double d = (double) RecipeHelper.getInputEUt(recipe) * recipe.duration / maxEUt;
        long eut = d > minDuration ? maxEUt : (long) (maxEUt * d / minDuration);
        RecipeHelper.setInputEUt(recipe, eut);
        recipe.duration = (int) Math.max(d, minDuration);
        return recipe;
    }

    @Override
    public void onRecipeFinish() {
        machine.afterWorking();
        if (lastRecipe != null) {
            handleRecipeOutput(machine, lastRecipe);
        }
        var match = this.isLock() ? getLockGTRecipe() : getGTRecipe();
        if (match != null && matchRecipeOutput(machine, match)) {
            setupRecipe(match);
            return;
        }
        setStatus(Status.IDLE);
        progress = 0;
        duration = 0;
    }

    protected boolean checkRecipe(GTRecipe recipe) {
        return recipe.data.getInt("euTier") <= getMachine().getTier() && recipe.checkConditions(machine.getRecipeLogic()).isSuccess();
    }

    record RecipeData(int index, long remainingWant) {}
}
