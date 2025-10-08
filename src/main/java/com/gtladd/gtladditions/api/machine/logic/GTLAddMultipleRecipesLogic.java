package com.gtladd.gtladditions.api.machine.logic;

import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine;
import org.gtlcore.gtlcore.api.recipe.IGTRecipe;
import org.gtlcore.gtlcore.api.recipe.IParallelLogic;
import org.gtlcore.gtlcore.api.recipe.RecipeResult;
import org.gtlcore.gtlcore.common.machine.trait.MultipleRecipesLogic;

import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.gtladd.gtladditions.api.machine.IGTLAddMultiRecipe;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import static org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.*;

public class GTLAddMultipleRecipesLogic extends MultipleRecipesLogic {

    protected final IGTLAddMultiRecipe limited;

    protected static final int MAX_THREADS = 128;

    protected BiPredicate<GTRecipe, IRecipeLogicMachine> recipeCheck;
    protected Predicate<IRecipeLogicMachine> beforeWorking;

    public GTLAddMultipleRecipesLogic(ParallelMachine parallel) {
        this(parallel, null, null);
    }

    public GTLAddMultipleRecipesLogic(ParallelMachine parallel, BiPredicate<GTRecipe, IRecipeLogicMachine> recipeCheck) {
        this(parallel, recipeCheck, null);
    }

    public GTLAddMultipleRecipesLogic(ParallelMachine parallel, Predicate<IRecipeLogicMachine> beforeWorking) {
        this(parallel, null, beforeWorking);
    }

    public GTLAddMultipleRecipesLogic(ParallelMachine parallel, BiPredicate<GTRecipe, IRecipeLogicMachine> recipeCheck, Predicate<IRecipeLogicMachine> beforeWorking) {
        super(parallel);
        this.limited = (IGTLAddMultiRecipe) parallel;
        this.recipeCheck = recipeCheck;
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

    public int getMultipleThreads() {
        return MAX_THREADS;
    }

    @Nullable
    protected GTRecipe getGTRecipe() {
        if (!checkBeforeWorking()) return null;

        var parallelData = calculateParallels();
        if (parallelData == null) return null;

        return buildFinalRecipe(parallelData);
    }

    @Nullable
    protected ParallelData calculateParallels() {
        var recipes = this.lookupRecipeIterator();
        int length = recipes.size();
        if (length == 0) return null;

        long totalParallel = (long) this.getParallel().getMaxParallel() * getMultipleThreads();
        long remaining = totalParallel;
        long[] parallels = new long[length];
        int index = 0;
        var queue = new ObjectArrayFIFOQueue<RecipeData>(length);
        var recipeList = new ObjectArrayList<GTRecipe>(length);

        for (var r : recipes) {
            if (r == null) continue;
            long p = getMaxParallel(r, totalParallel);
            if (p <= 0) continue;
            recipeList.add(r);
            parallels[index] = Math.min(p, totalParallel / length);
            if (p > parallels[index]) queue.enqueue(new RecipeData(index, p - parallels[index]));
            remaining -= parallels[index++];
        }

        if (recipeList.isEmpty()) return null;

        var remainingWants = new long[length];
        var activeIndices = new IntArrayList(queue.size());
        while (!queue.isEmpty()) {
            var data = queue.dequeue();
            remainingWants[data.index] = data.remainingWant;
            activeIndices.add(data.index);
        }

        while (remaining > 0 && !activeIndices.isEmpty()) {
            long perRecipe = remaining / activeIndices.size();
            if (perRecipe == 0) break;

            long distributed = 0;
            for (var it = activeIndices.iterator(); it.hasNext();) {
                int idx = it.nextInt();
                long give = Math.min(remainingWants[idx], perRecipe);
                parallels[idx] += give;
                distributed += give;
                remainingWants[idx] -= give;
                if (remainingWants[idx] == 0) {
                    it.remove();
                }
            }
            remaining -= distributed;
        }

        return new ParallelData(recipeList, parallels);
    }

    @Nullable
    protected GTRecipe buildFinalRecipe(ParallelData parallelData) {
        long maxEUt = getMachine().getOverclockVoltage();

        GTRecipe recipe = GTRecipeBuilder.ofRaw().buildRawRecipe();
        recipe.outputs.put(ItemRecipeCapability.CAP, new ObjectArrayList<>());
        recipe.outputs.put(FluidRecipeCapability.CAP, new ObjectArrayList<>());

        double euMultiplier = this.getEuMultiplier();
        long totalEu = 0;
        int index = 0;

        for (var r : parallelData.recipeList) {
            if (parallelData.parallels[index] > 1) r = r.copy(ContentModifier.multiplier(parallelData.parallels[index]), false);
            ((IGTRecipe) r).setRealParallels(parallelData.parallels[index++]);
            r = modifyInputAndOutput(r);
            if (matchRecipeInput(machine, r) && handleRecipeInput(machine, r)) {
                totalEu += (long) (getTotalEuOfRecipe(r) * euMultiplier);
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

    protected @NotNull Set<GTRecipe> lookupRecipeIterator() {
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

    protected boolean checkRecipe(GTRecipe recipe) {
        return matchRecipe(machine, recipe) &&
                IGTRecipe.of(recipe).getEuTier() <= getMachine().getTier() &&
                recipe.checkConditions(machine.getRecipeLogic()).isSuccess() &&
                (recipeCheck == null || recipeCheck.test(recipe, machine));
    }

    protected boolean checkBeforeWorking() {
        if (!machine.hasProxies()) return false;
        if (getMachine().getOverclockVoltage() <= 0) return false;
        return this.beforeWorking == null || this.beforeWorking.test(machine);
    }

    protected long getMaxParallel(GTRecipe recipe, long limit) {
        return IParallelLogic.getMaxParallel(this.machine, recipe, limit);
    }

    protected GTRecipe modifyInputAndOutput(GTRecipe recipe) {
        return IParallelLogic.getRecipeOutputChance(machine, recipe);
    }

    public record RecipeData(int index, long remainingWant) {}

    public record ParallelData(List<GTRecipe> recipeList, long[] parallels) {}
}
