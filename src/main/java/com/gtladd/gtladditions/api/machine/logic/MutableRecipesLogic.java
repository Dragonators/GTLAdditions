package com.gtladd.gtladditions.api.machine.logic;

import org.gtlcore.gtlcore.api.machine.trait.ILockRecipe;
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine;
import org.gtlcore.gtlcore.api.machine.trait.IRecipeStatus;
import org.gtlcore.gtlcore.api.recipe.IGTRecipe;
import org.gtlcore.gtlcore.api.recipe.IParallelLogic;
import org.gtlcore.gtlcore.api.recipe.RecipeResult;
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper;

import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.gtladd.gtladditions.api.machine.IWirelessElectricMultiblockMachine;
import com.gtladd.gtladditions.api.machine.IWirelessThreadModifierParallelMachine;
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler;
import com.gtladd.gtladditions.api.recipe.IWirelessGTRecipe;
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe;
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipeBuilder;
import it.unimi.dsi.fastutil.longs.LongLongPair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiPredicate;

public class MutableRecipesLogic<T extends WorkableElectricMultiblockMachine & IRecipeLogicMachine & IWirelessThreadModifierParallelMachine> extends RecipeLogic implements ILockRecipe, IWirelessRecipeLogic, IRecipeStatus {

    private boolean useMultipleRecipes;
    private final double reductionRatio;
    protected final @Nullable BiPredicate<GTRecipe, IRecipeLogicMachine> recipeCheck;

    public MutableRecipesLogic(T machine) {
        this(machine, null, 1.0);
    }

    public MutableRecipesLogic(T machine, double reductionRatio) {
        this(machine, null, reductionRatio);
    }

    public MutableRecipesLogic(T machine, @Nullable BiPredicate<GTRecipe, IRecipeLogicMachine> recipeCheck) {
        this(machine, recipeCheck, 1.0);
    }

    public MutableRecipesLogic(T machine, @Nullable BiPredicate<GTRecipe, IRecipeLogicMachine> recipeCheck, double reductionRatio) {
        super(machine);
        this.recipeCheck = recipeCheck;
        this.reductionRatio = reductionRatio;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getMachine() {
        return (T) super.getMachine();
    }

    @Override
    public void findAndHandleRecipe() {
        if (useMultipleRecipes) findAndHandleMultipleRecipe();
        else super.findAndHandleRecipe();
    }

    @Override
    public void onRecipeFinish() {
        if (useMultipleRecipes) onMultipleRecipeFinish();
        else super.onRecipeFinish();
    }

    @Override
    public void handleRecipeWorking() {
        if (useMultipleRecipes) handleMultipleRecipeWorking();
        else super.handleRecipeWorking();
    }

    // ========================================
    // Multiple Logic
    // ========================================

    protected void findAndHandleMultipleRecipe() {
        lastRecipe = null;
        setRecipeStatus(null);
        var match = getRecipe();
        if (match != null) {
            if (RecipeRunnerHelper.matchRecipeOutput(machine, match)) {
                setupRecipe(match);
            }
        }
    }

    protected void onMultipleRecipeFinish() {
        machine.afterWorking();
        if (lastRecipe != null) {
            RecipeRunnerHelper.handleRecipeOutput(this.machine, lastRecipe);
        }
        var match = getRecipe();
        if (match != null) {
            if (RecipeRunnerHelper.matchRecipeOutput(machine, match)) {
                setupRecipe(match);
                return;
            }
        }
        setStatus(Status.IDLE);
        progress = 0;
        duration = 0;
    }

    protected void handleMultipleRecipeWorking() {
        assert this.lastRecipe != null;

        boolean success = this.lastRecipe instanceof IWirelessGTRecipe wirelessGTRecipe ? handleWirelessTickInput(wirelessGTRecipe) : this.handleTickRecipe(this.lastRecipe).isSuccess();

        if (success) {
            this.setStatus(RecipeLogic.Status.WORKING);
            if (!this.machine.onWorking()) {
                this.interruptRecipe();
                return;
            }
            ++this.progress;
            ++this.totalContinuousRunningTime;
        } else {
            this.setWaiting(RecipeResult.FAIL_NO_ENOUGH_EU_IN.reason());
        }

        if (this.getStatus() == RecipeLogic.Status.WAITING) {
            this.doDamping();
        }
    }

    protected @Nullable GTRecipe getRecipe() {
        final var wirelessTrait = getMachine().getWirelessNetworkEnergyHandler();
        return wirelessTrait != null ? getWirelessRecipe(wirelessTrait) : getNormalRecipe();
    }

    private @Nullable GTRecipe getNormalRecipe() {
        if (!machine.hasProxies()) return null;

        final long maxEUt = getMachine().getOverclockVoltage();
        if (maxEUt <= 0) return null;

        final var iterator = lookupRecipeIterator();
        final var euMultiplier = getEuMultiplier();
        final var itemOutputs = new ObjectArrayList<Content>();
        final var fluidOutputs = new ObjectArrayList<Content>();
        GTRecipe output = GTRecipeBuilder.ofRaw().buildRawRecipe();
        output.outputs.put(ItemRecipeCapability.CAP, itemOutputs);
        output.outputs.put(FluidRecipeCapability.CAP, fluidOutputs);

        double totalEu = 0;
        long remain = (long) getMachine().getMaxParallel() * getMultipleThreads();
        while (remain > 0 && iterator.hasNext()) {
            GTRecipe match = iterator.next();
            if (match == null) continue;
            var pair = calculateParallel(machine, match, remain);
            long p = pair.firstLong();
            if (p <= 0) continue;
            else if (p > 1) match = match.copy(ContentModifier.multiplier(p), false);
            IGTRecipe.of(match).setRealParallels(p);
            match = IParallelLogic.getRecipeOutputChance(machine, match);
            if (RecipeRunnerHelper.handleRecipeInput(machine, match)) {
                remain -= pair.secondLong();
                totalEu += (long) (getRecipeEut(match) * match.duration * euMultiplier);
                var item = match.outputs.get(ItemRecipeCapability.CAP);
                if (item != null) itemOutputs.addAll(item);
                var fluid = match.outputs.get(FluidRecipeCapability.CAP);
                if (fluid != null) fluidOutputs.addAll(fluid);
            }
            if (totalEu / maxEUt > 20 * 500) break;
        }
        if (itemOutputs.isEmpty() && fluidOutputs.isEmpty()) {
            if (getRecipeStatus() == null || getRecipeStatus().isSuccess()) RecipeResult.of(this.machine, RecipeResult.FAIL_FIND);
            return null;
        }
        double d = totalEu / maxEUt;
        long eut = d > 20 ? maxEUt : (long) (maxEUt * d / 20);
        output.tickInputs.put(EURecipeCapability.CAP, List.of(new Content(eut, 10000, 10000, 0, null, null)));
        output.duration = (int) Math.max(d, 20);
        IGTRecipe.of(output).setHasTick(true);
        return output;
    }

    private @Nullable WirelessGTRecipe getWirelessRecipe(@NotNull IWirelessNetworkEnergyHandler wirelessTrait) {
        if (!wirelessTrait.isOnline()) return null;

        final var iterator = lookupRecipeIterator();
        final var maxTotalEu = wirelessTrait.getMaxAvailableEnergy();
        final var euMultiplier = getEuMultiplier();
        final var itemOutputs = new ObjectArrayList<Content>();
        final var fluidOutputs = new ObjectArrayList<Content>();

        long remain = (long) getMachine().getMaxParallel() * getMultipleThreads();
        BigInteger totalEu = BigInteger.ZERO;

        while (remain > 0 && iterator.hasNext()) {
            GTRecipe match = iterator.next();
            if (match == null) continue;
            var pair = calculateParallel(machine, match, remain);
            long p = pair.firstLong();
            if (p <= 0) continue;

            var parallelEUt = BigInteger.valueOf(getRecipeEut(match));
            if (p > 1) {
                match = match.copy(ContentModifier.multiplier(p), false);
                parallelEUt = parallelEUt.multiply(BigInteger.valueOf(p));
            }
            IGTRecipe.of(match).setRealParallels(p);

            var tempTotalEu = totalEu.add(BigDecimal.valueOf(match.duration * euMultiplier).multiply(new BigDecimal(parallelEUt)).toBigInteger());
            if (tempTotalEu.compareTo(maxTotalEu) > 0) {
                if (totalEu.signum() == 0) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_EU_IN);
                break;
            }

            match = IParallelLogic.getRecipeOutputChance(machine, match);
            if (RecipeRunnerHelper.handleRecipeInput(machine, match)) {
                remain -= pair.secondLong();
                totalEu = tempTotalEu;
                var item = match.outputs.get(ItemRecipeCapability.CAP);
                if (item != null) itemOutputs.addAll(item);
                var fluid = match.outputs.get(FluidRecipeCapability.CAP);
                if (fluid != null) fluidOutputs.addAll(fluid);
            }
        }

        if (itemOutputs.isEmpty() && fluidOutputs.isEmpty()) {
            if (getRecipeStatus() == null || getRecipeStatus().isSuccess()) RecipeResult.of(this.machine, RecipeResult.FAIL_FIND);
            return null;
        }

        var eut = totalEu.divide(BigInteger.valueOf(20)).negate();
        return WirelessGTRecipeBuilder
                .ofRaw()
                .output(ItemRecipeCapability.CAP, itemOutputs)
                .output(FluidRecipeCapability.CAP, fluidOutputs)
                .duration(20)
                .setWirelessEut(eut)
                .buildRawRecipe();
    }

    protected LongLongPair calculateParallel(IRecipeLogicMachine machine, GTRecipe match, long remain) {
        long p = IParallelLogic.getMaxParallel(machine, match, remain);
        return LongLongPair.of(p, p);
    }

    public int getMultipleThreads() {
        return getMachine().getAdditionalThread() > 0 ? getMachine().getAdditionalThread() : 1;
    }

    protected double getEuMultiplier() {
        var maintenanceMachine = ((IRecipeCapabilityMachine) machine).getMaintenanceMachine();
        return maintenanceMachine != null ? maintenanceMachine.getDurationMultiplier() * this.reductionRatio : this.reductionRatio;
    }

    protected long getRecipeEut(GTRecipe recipe) {
        return RecipeHelper.getInputEUt(recipe);
    }

    protected Iterator<GTRecipe> lookupRecipeIterator() {
        if (this.isLock()) {
            if (this.getLockRecipe() == null) {
                this.setLockRecipe(machine.getRecipeType().getLookup()
                        .find(machine, this::checkRecipe));
            } else if (!checkRecipe(this.getLockRecipe())) return Collections.emptyIterator();
            return Collections.singleton(this.getLockRecipe()).iterator();
        } else return machine.getRecipeType().getLookup().getRecipeIterator(machine, this::checkRecipe);
    }

    protected boolean checkRecipe(GTRecipe recipe) {
        return RecipeRunnerHelper.matchRecipe(machine, recipe) &&
                IGTRecipe.of(recipe).getEuTier() <= getMachine().getTier() &&
                recipe.checkConditions(this).isSuccess() &&
                (recipeCheck == null || recipeCheck.test(recipe, machine));
    }

    @Override
    @NotNull
    public IWirelessElectricMultiblockMachine getWirelessMachine() {
        return (IWirelessElectricMultiblockMachine) machine;
    }

    @SuppressWarnings("all")
    public void setUseMultipleRecipes(boolean useMultipleRecipes) {
        this.useMultipleRecipes = useMultipleRecipes;
    }

    public boolean isMultipleRecipeMode() {
        return this.useMultipleRecipes;
    }
}
