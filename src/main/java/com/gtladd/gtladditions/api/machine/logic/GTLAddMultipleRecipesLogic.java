package com.gtladd.gtladditions.api.machine.logic;

import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine;
import org.gtlcore.gtlcore.api.machine.trait.ILockRecipe;
import org.gtlcore.gtlcore.api.recipe.IParallelLogic;
import org.gtlcore.gtlcore.api.recipe.IRecipeIterator;
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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.primitives.Ints;
import com.gtladd.gtladditions.api.machine.IGTLAddMultiRecipe;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import static org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.*;

public class GTLAddMultipleRecipesLogic extends RecipeLogic implements ILockRecipe {

    private static final Set<WeakReference<GTLAddMultipleRecipesLogic>> INSTANCES = ConcurrentHashMap.newKeySet();
    private static final int MAX_THREADS = 128;
    protected final ParallelMachine parallel;
    private final IGTLAddMultiRecipe limited;
    private final LoadingCache<Key, Data> cache = Caffeine.newBuilder()
            .maximumSize(5_000)
            .expireAfterAccess(Duration.ofMinutes(30))
            .build(key -> {
                GTRecipe temp = GetTempRecipe(key.base(), key.p());
                long eu = RecipeHelper.getInputEUt(temp) * temp.duration;
                return new Data(temp, eu);
            });
    protected BiPredicate<GTRecipe, IRecipeLogicMachine> beforeWorking;

    public GTLAddMultipleRecipesLogic(ParallelMachine parallel) {
        super((IRecipeLogicMachine) parallel);
        this.parallel = parallel;
        this.limited = (IGTLAddMultiRecipe) parallel;
        INSTANCES.add(new WeakReference<>(this));
    }

    public GTLAddMultipleRecipesLogic(ParallelMachine parallel, BiPredicate<GTRecipe, IRecipeLogicMachine> beforeWorking) {
        super((IRecipeLogicMachine) parallel);
        this.parallel = parallel;
        this.limited = (IGTLAddMultiRecipe) parallel;
        this.beforeWorking = beforeWorking;
    }

    public static void clearAllInstanceCaches() {
        Iterator<WeakReference<GTLAddMultipleRecipesLogic>> it = INSTANCES.iterator();
        while (it.hasNext()) {
            GTLAddMultipleRecipesLogic logic = it.next().get();
            if (logic == null) {
                it.remove();
            } else {
                logic.clearLocalCaches();
            }
        }
    }

    @Override
    public WorkableElectricMultiblockMachine getMachine() {
        return (WorkableElectricMultiblockMachine) super.getMachine();
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
        if (this.beforeWorking != null && !this.beforeWorking.test(null, machine)) return null;
        final long maxEUt = getMachine().getOverclockVoltage();
        if (maxEUt <= 0) return null;

        var recipeSet = lookupRecipeIterator();
        int size = recipeSet.size();
        if (size == 0) return null;
        var recipes = new GTRecipe[size];
        int length = 0;
        for (GTRecipe r : recipeSet) {
            if (r != null) {
                recipes[length++] = r;
            }
        }
        if (length == 0) return null;

        boolean[] active = new boolean[length];
        Arrays.fill(active, true);
        int activeCount = length;

        long maxParallelLimit = parallel.getMaxParallel();
        long remainingParallel = maxParallelLimit * MAX_THREADS;
        long totalEu = 0L;

        GTRecipe recipe = GTRecipeBuilder.ofRaw().buildRawRecipe();
        var aggregatedItems = new ObjectArrayList<Content>();
        var aggregatedFluids = new ObjectArrayList<Content>();
        recipe.outputs.put(ItemRecipeCapability.CAP, aggregatedItems);
        recipe.outputs.put(FluidRecipeCapability.CAP, aggregatedFluids);

        outer:
        while (remainingParallel > 0 && activeCount > 0) {
            long avgParallel = remainingParallel / activeCount;
            if (avgParallel < 1) break;

            for (int i = 0; i < length && remainingParallel > 0; i++) {
                if (!active[i]) continue;
                GTRecipe baseRecipe = recipes[i];

                long usedParallel = 0;
                long pMax = IParallelLogic.getMaxParallel(machine, baseRecipe, avgParallel);
                if (pMax < 1) {
                    active[i] = false;
                    activeCount--;
                    continue;
                }

                int runs = (int) Math.min((avgParallel + pMax - 1) / pMax, 128);
                for (int j = 0; j < runs; j++) {
                    long p = Math.min(pMax, avgParallel - usedParallel);
                    if (p <= 0) break;

                    Data d = cache.get(new Key(baseRecipe, p));
                    long eu = d.euCost();
                    GTRecipe tempR = d.temp();
                    if (!handleRecipeInput(machine, tempR)) {
                        active[i] = false;
                        activeCount--;
                        break;
                    }
                    totalEu += eu;
                    AddGTRecipe(aggregatedItems, aggregatedFluids, tempR);
                    usedParallel += p;
                    if (totalEu / maxEUt > 20 * 500) break outer;

                    pMax = IParallelLogic.getMaxParallel(this.machine, baseRecipe, avgParallel);
                    if (pMax <= 0) {
                        active[i] = false;
                        activeCount--;
                        break;
                    }
                }
                remainingParallel -= usedParallel;
            }
        }

        if (aggregatedItems.isEmpty() && aggregatedFluids.isEmpty()) {
            if (totalEu / maxEUt > 20 * 500) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_EU_IN);
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

    private @NotNull Set<GTRecipe> lookupRecipeIterator() {
        if (this.isLock()) {
            if (this.getLockRecipe() == null) this.setLockRecipe(machine.getRecipeType().getLookup()
                    .find(machine, this::checkRecipe));
            else if (!checkRecipe(this.getLockRecipe())) return Collections.emptySet();
            return Collections.singleton(this.getLockRecipe());
        } else
            return IRecipeIterator.findIteratorRecipeCollection(machine.getRecipeType().getLookup()
                    .getRecipeIterator(machine, this::checkRecipe));
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
                recipe.data.getInt("euTier") <= getMachine().getTier() &&
                recipe.checkConditions(machine.getRecipeLogic()).isSuccess();
    }

    private GTRecipe GetTempRecipe(GTRecipe base, long p) {
        GTRecipe result;
        if (p > 1) result = base.copy(ContentModifier.multiplier(p), false);
        else result = base.copy();
        result.parallels = Ints.saturatedCast(p);
        IParallelLogic.getRecipeOutputChance(machine, result);
        return result;
    }

    private void AddGTRecipe(List<Content> aggregatedItems, List<Content> aggregatedFluids, GTRecipe recipe) {
        List<Content> item = recipe.outputs.get(ItemRecipeCapability.CAP);
        if (item != null) aggregatedItems.addAll(item);
        List<Content> fluid = recipe.outputs.get(FluidRecipeCapability.CAP);
        if (fluid != null) aggregatedFluids.addAll(fluid);
    }

    public void clearLocalCaches() {
        cache.invalidateAll();
    }

    private record Key(GTRecipe base, long p) {}

    private record Data(GTRecipe temp, long euCost) {}
}
