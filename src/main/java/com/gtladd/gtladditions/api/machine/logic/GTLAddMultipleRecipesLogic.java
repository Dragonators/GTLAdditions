package com.gtladd.gtladditions.api.machine.logic;

import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine;
import org.gtlcore.gtlcore.api.machine.trait.ILockRecipe;
import org.gtlcore.gtlcore.api.recipe.*;
import org.gtlcore.gtlcore.common.machine.trait.MultipleRecipesLogic;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.google.common.primitives.Ints;
import com.gtladd.gtladditions.api.machine.IGTLAddMultiRecipe;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import static org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.*;

public class GTLAddMultipleRecipesLogic extends MultipleRecipesLogic implements ILockRecipe {

    private final IGTLAddMultiRecipe limited;

    private static final int MAX_THREADS = 128;

    protected BiPredicate<GTRecipe, IRecipeLogicMachine> beforeWorking;

    public GTLAddMultipleRecipesLogic(ParallelMachine parallel) {
        super(parallel);
        this.limited = (IGTLAddMultiRecipe) parallel;
    }

    public GTLAddMultipleRecipesLogic(ParallelMachine parallel, BiPredicate<GTRecipe, IRecipeLogicMachine> beforeWorking) {
        super(parallel);
        this.limited = (IGTLAddMultiRecipe) parallel;
        this.beforeWorking = beforeWorking;
    }

    @Override
    public void findAndHandleRecipe() {
        lastRecipe = null;
        var match = getGTRecipe();
        if (match != null && matchRecipeOutput(machine, match)) {
            setupRecipe(match);
        }
    }

    @Nullable
    protected GTRecipe getGTRecipe() {
        if (!machine.hasProxies()) return null;
        long maxEUt = getMachine().getOverclockVoltage();
        if (maxEUt <= 0) return null;
        var recipes = this.lookupRecipeIterator();
        int length = recipes.size();
        if (length == 0) return null;
        long parallel = this.getParallel().getMaxParallel();
        long[] parallels = new long[length];
        int index = 0;
        long remaining = parallel * MAX_THREADS;
        var queue = new ObjectArrayFIFOQueue<RecipeData>(length);
        var recipeList = new ObjectArrayList<GTRecipe>(length);
        for (var r : recipes) {
            if (r == null) continue;
            long p = IParallelLogic.getMaxParallel(this.machine, r, parallel * MAX_THREADS);
            if (p <= 0) continue;
            recipeList.add(r);
            parallels[index] = Math.min(p, parallel * MAX_THREADS / length);
            if (p > parallels[index]) queue.enqueue(new RecipeData(index, p - parallels[index]));
            remaining -= parallels[index++];
        }
        if (recipeList.isEmpty()) return null;
        while (remaining > 0 && !queue.isEmpty()) {
            var recipeData = queue.dequeue();
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
        if (this.beforeWorking != null && !this.beforeWorking.test(null, machine)) return null;
        GTRecipe recipe = GTRecipeBuilder.ofRaw().buildRawRecipe();
        recipe.outputs.put(ItemRecipeCapability.CAP, new ArrayList<>());
        recipe.outputs.put(FluidRecipeCapability.CAP, new ArrayList<>());
        long totalEu = 0;
        for (var r : recipeList) {
            if (parallels[index] > 1) r = r.copy(ContentModifier.multiplier(parallels[index]), false);
            r.parallels = Ints.saturatedCast(parallels[index++]);
            r = IParallelLogic.getRecipeOutputChance(machine, r);
            if (handleRecipeInput(machine, r)) {
                totalEu += RecipeHelper.getInputEUt(r) * r.duration;
                var item = r.outputs.get(ItemRecipeCapability.CAP);
                if (item != null) recipe.outputs.get(ItemRecipeCapability.CAP).addAll(item);
                var fluid = r.outputs.get(FluidRecipeCapability.CAP);
                if (fluid != null) recipe.outputs.get(FluidRecipeCapability.CAP).addAll(fluid);
            }
            if (totalEu / maxEUt > 20 * 500) break;
        }
        if (recipe.outputs.get(ItemRecipeCapability.CAP).isEmpty() && recipe.outputs.get(FluidRecipeCapability.CAP).isEmpty()) {
            if (totalEu / maxEUt > 20 * 500) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_EU_IN);
            return null;
        }
        int minDuration = limited.getLimitedDuration();
        double d = (double) totalEu / maxEUt;
        long eut = d > minDuration ? maxEUt : (long) (maxEUt * d / minDuration);
        recipe.tickInputs.put(EURecipeCapability.CAP, List.of(
                new Content(eut, 10000, 10000, 0, null, null)));
        recipe.duration = (int) Math.max(d, minDuration);
        IGTRecipe.of(recipe).setHasTick(true);
        return recipe;
    }

    private @NotNull Set<GTRecipe> lookupRecipeIterator() {
        if (this.isLock()) {
            if (this.getLockRecipe() == null) this.setLockRecipe(machine.getRecipeType().getLookup()
                    .find(machine, this::checkRecipe));
            else if (!checkRecipe(this.getLockRecipe())) return Collections.emptySet();
            return Collections.singleton(this.getLockRecipe());
        } else {
            var iterator = machine.getRecipeType().getLookup().getRecipeIterator(machine, this::checkRecipe);
            var recipeSet = new ObjectOpenHashSet<GTRecipe>();
            while (iterator.hasNext()) recipeSet.add(iterator.next());
            recipeSet.remove(null);
            return recipeSet;
        }
    }

    @Override
    public void onRecipeFinish() {
        machine.afterWorking();
        if (lastRecipe != null) {
            handleRecipeOutput(machine, lastRecipe);
        }
        var match = getGTRecipe();
        if (match != null && matchRecipeOutput(machine, match)) {
            setupRecipe(match);
            return;
        }
        setStatus(Status.IDLE);
        progress = 0;
        duration = 0;
    }

    protected boolean checkRecipe(GTRecipe recipe) {
        return matchRecipe(machine, recipe) &&
                IGTRecipe.of(recipe).getEuTier() <= getMachine().getTier() &&
                recipe.checkConditions(machine.getRecipeLogic()).isSuccess();
    }

    record RecipeData(int index, long remainingWant) {}
}
