package com.gtladd.gtladditions.api.machine.logic;

import org.gtlcore.gtlcore.api.recipe.IGTRecipe;
import org.gtlcore.gtlcore.api.recipe.IParallelLogic;
import org.gtlcore.gtlcore.api.recipe.RecipeResult;
import org.gtlcore.gtlcore.common.machine.trait.MultipleRecipesLogic;

import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.google.common.primitives.Ints;
import com.gtladd.gtladditions.api.machine.IGTLAddMultiRecipeMachine;
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine;
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler;
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe;
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipeBuilder;
import com.gtladd.gtladditions.common.record.ParallelData;
import com.gtladd.gtladditions.utils.CommonUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.*;

public class GTLAddMultipleRecipesLogic extends MultipleRecipesLogic {

    private static final long MAX_THREADS = 128;
    protected final IGTLAddMultiRecipeMachine limited;
    protected final BiPredicate<GTRecipe, IRecipeLogicMachine> recipeCheck;
    protected final Predicate<IRecipeLogicMachine> beforeWorking;

    public GTLAddMultipleRecipesLogic(GTLAddWorkableElectricMultipleRecipesMachine parallel) {
        this(parallel, null, null);
    }

    public GTLAddMultipleRecipesLogic(GTLAddWorkableElectricMultipleRecipesMachine parallel, BiPredicate<GTRecipe, IRecipeLogicMachine> recipeCheck) {
        this(parallel, recipeCheck, null);
    }

    public GTLAddMultipleRecipesLogic(GTLAddWorkableElectricMultipleRecipesMachine parallel, Predicate<IRecipeLogicMachine> beforeWorking) {
        this(parallel, null, beforeWorking);
    }

    public GTLAddMultipleRecipesLogic(GTLAddWorkableElectricMultipleRecipesMachine parallel, BiPredicate<GTRecipe, IRecipeLogicMachine> recipeCheck, Predicate<IRecipeLogicMachine> beforeWorking) {
        super(parallel);
        this.limited = parallel;
        this.recipeCheck = recipeCheck;
        this.beforeWorking = beforeWorking;
    }

    @Override
    public void findAndHandleRecipe() {
        lastRecipe = null;
        setRecipeStatus(null);
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

    @Override
    public GTLAddWorkableElectricMultipleRecipesMachine getMachine() {
        return (GTLAddWorkableElectricMultipleRecipesMachine) super.getMachine();
    }

    public int getMultipleThreads() {
        return Ints.saturatedCast(MAX_THREADS + getMachine().getAdditionalThread());
    }

    @Nullable
    protected GTRecipe getGTRecipe() {
        if (!checkBeforeWorking()) return null;

        final var parallelData = calculateParallels();
        if (parallelData == null) return null;

        final var wirelessTrait = getMachine().getWirelessNetworkEnergyHandler();
        return wirelessTrait != null ? buildFinalWirelessRecipe(parallelData, wirelessTrait) : buildFinalNormalRecipe(parallelData);
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
        var recipeList = new ObjectArrayList<GTRecipe>(length);
        var remainingWants = new LongArrayList(length);
        var remainingIndices = new IntArrayList(length);

        for (var r : recipes) {
            if (r == null) continue;
            long p = getMaxParallel(r, totalParallel);
            if (p <= 0) continue;
            recipeList.add(r);
            long allocated = Math.min(p, totalParallel / length);
            parallels[index] = allocated;
            long want = p - allocated;
            if (want > 0) {
                remainingWants.add(want);
                remainingIndices.add(index);
            }
            remaining -= allocated;
            index++;
        }

        if (recipeList.isEmpty()) return null;

        return CommonUtils.getParallelData(remaining, parallels, remainingWants, remainingIndices, recipeList);
    }

    @Nullable
    protected GTRecipe buildFinalNormalRecipe(ParallelData parallelData) {
        long maxEUt = getMachine().getOverclockVoltage();

        final var itemOutputs = new ObjectArrayList<Content>();
        final var fluidOutputs = new ObjectArrayList<Content>();
        GTRecipe recipe = GTRecipeBuilder.ofRaw().buildRawRecipe();
        recipe.outputs.put(ItemRecipeCapability.CAP, itemOutputs);
        recipe.outputs.put(FluidRecipeCapability.CAP, fluidOutputs);

        double euMultiplier = this.getEuMultiplier();
        double totalEu = 0;
        int index = 0;

        for (var r : parallelData.recipeList) {
            if (parallelData.parallels[index] > 1) r = r.copy(ContentModifier.multiplier(parallelData.parallels[index]), false);
            IGTRecipe.of(r).setRealParallels(parallelData.parallels[index++]);
            r = IParallelLogic.getRecipeOutputChance(machine, r);
            if (matchRecipeInput(machine, r) && handleRecipeInput(machine, r)) {
                totalEu += getTotalEuOfRecipe(r) * euMultiplier;
                var item = r.outputs.get(ItemRecipeCapability.CAP);
                if (item != null) itemOutputs.addAll(item);
                var fluid = r.outputs.get(FluidRecipeCapability.CAP);
                if (fluid != null) fluidOutputs.addAll(fluid);
            }
            if (totalEu / maxEUt > 20 * 500) break;
        }

        if (itemOutputs.isEmpty() && fluidOutputs.isEmpty()) {
            if (getRecipeStatus() == null || getRecipeStatus().isSuccess()) RecipeResult.of(this.machine, RecipeResult.FAIL_FIND);
            return null;
        }

        int minDuration = limited.getLimitedDuration();
        double d = totalEu / maxEUt;
        long eut = d > minDuration ? maxEUt : (long) (maxEUt * d / minDuration);
        recipe.tickInputs.put(EURecipeCapability.CAP, List.of(
                new Content(eut, 10000, 10000, 0, null, null)));
        recipe.duration = (int) Math.max(d, minDuration);
        IGTRecipe.of(recipe).setHasTick(true);
        return recipe;
    }

    @Nullable
    protected WirelessGTRecipe buildFinalWirelessRecipe(ParallelData parallelData, @NotNull IWirelessNetworkEnergyHandler wirelessTrait) {
        if (!wirelessTrait.isOnline()) return null;

        final BigInteger maxTotalEu = wirelessTrait.getMaxAvailableEnergy();
        final double euMultiplier = this.getEuMultiplier();
        final var itemOutputs = new ObjectArrayList<Content>();
        final var fluidOutputs = new ObjectArrayList<Content>();

        BigInteger totalEu = BigInteger.ZERO;
        int index = 0;

        for (var r : parallelData.recipeList) {
            BigInteger parallelEUt = BigInteger.valueOf(RecipeHelper.getInputEUt(r));

            final long p = parallelData.parallels[index++];
            if (p > 1) {
                r = r.copy(ContentModifier.multiplier(p), false);
                parallelEUt = parallelEUt.multiply(BigInteger.valueOf(p));
            }
            IGTRecipe.of(r).setRealParallels(p);

            var tempTotalEu = totalEu.add(BigDecimal.valueOf(r.duration * euMultiplier).multiply(new BigDecimal(parallelEUt)).toBigInteger());
            if (tempTotalEu.compareTo(maxTotalEu) > 0) {
                if (totalEu.signum() == 0) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_EU_IN);
                break;
            }

            r = IParallelLogic.getRecipeOutputChance(machine, r);
            if (matchRecipeInput(machine, r) && handleRecipeInput(machine, r)) {
                totalEu = tempTotalEu;
                var item = r.outputs.get(ItemRecipeCapability.CAP);
                if (item != null) itemOutputs.addAll(item);
                var fluid = r.outputs.get(FluidRecipeCapability.CAP);
                if (fluid != null) fluidOutputs.addAll(fluid);
            }
        }

        if (itemOutputs.isEmpty() && fluidOutputs.isEmpty()) {
            if (getRecipeStatus() == null || getRecipeStatus().isSuccess()) RecipeResult.of(this.machine, RecipeResult.FAIL_FIND);
            return null;
        }

        int minDuration = limited.getLimitedDuration();
        BigInteger eut = totalEu.divide(BigInteger.valueOf(minDuration)).negate();
        return buildWirelessRecipe(itemOutputs, fluidOutputs, minDuration, eut);
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

    protected @NotNull WirelessGTRecipe buildWirelessRecipe(@NotNull List<Content> item, @NotNull List<Content> fluid, int duration, BigInteger eut) {
        return WirelessGTRecipeBuilder
                .ofRaw()
                .output(ItemRecipeCapability.CAP, item)
                .output(FluidRecipeCapability.CAP, fluid)
                .duration(duration)
                .setWirelessEut(eut)
                .buildRawRecipe();
    }
}
