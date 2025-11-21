package com.gtladd.gtladditions.mixin.gtlcore.machine.trait;

import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine;
import org.gtlcore.gtlcore.api.machine.trait.IRecipeStatus;
import org.gtlcore.gtlcore.api.recipe.IParallelLogic;
import org.gtlcore.gtlcore.api.recipe.RecipeResult;
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper;
import org.gtlcore.gtlcore.common.machine.trait.MultipleRecipesLogic;

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import net.minecraft.nbt.CompoundTag;

import com.google.common.primitives.Ints;
import com.gtladd.gtladditions.api.machine.IWirelessElectricMultiblockMachine;
import com.gtladd.gtladditions.api.machine.IWirelessThreadModifierParallelMachine;
import com.gtladd.gtladditions.api.machine.logic.IWirelessRecipeLogic;
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler;
import com.gtladd.gtladditions.api.recipe.IWirelessGTRecipe;
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe;
import com.gtladd.gtladditions.utils.RecipeCalculationHelper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.function.BiPredicate;

import static org.gtlcore.gtlcore.api.recipe.IParallelLogic.getMaxParallel;
import static org.gtlcore.gtlcore.api.recipe.IParallelLogic.getRecipeOutputChance;
import static org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.handleRecipeInput;

@Mixin(MultipleRecipesLogic.class)
public abstract class MultipleRecipesLogicMixin extends RecipeLogic implements IWirelessRecipeLogic, IRecipeStatus {

    @Shadow(remap = false)
    @Final
    private static int MAX_THREADS;
    @Unique
    private IWirelessThreadModifierParallelMachine gTLAdditions$machine;

    public MultipleRecipesLogicMixin(IRecipeLogicMachine machine) {
        super(machine);
    }

    @Inject(method = "<init>(Lorg/gtlcore/gtlcore/api/machine/multiblock/ParallelMachine;Ljava/util/function/BiPredicate;DD)V", at = @At("TAIL"), remap = false)
    private void onInit(ParallelMachine ignore1, BiPredicate<CompoundTag, IRecipeLogicMachine> ignore2, double ignore3, double ignore4, CallbackInfo ci) {
        gTLAdditions$machine = ((IWirelessThreadModifierParallelMachine) getMachine());
    }

    @Shadow(remap = false)
    protected double getTotalEuOfRecipe(GTRecipe recipe) {
        throw new AssertionError();
    }

    @Shadow(remap = false)
    protected double getEuMultiplier() {
        throw new AssertionError();
    }

    @Shadow(remap = false)
    private Iterator<GTRecipe> lookupRecipeIterator() {
        throw new AssertionError();
    }

    @Shadow(remap = false)
    public WorkableElectricMultiblockMachine getMachine() {
        throw new AssertionError();
    }

    /**
     * @author Dragons
     * @reason Wireless and thread modify
     */
    @Overwrite(remap = false)
    private GTRecipe getRecipe() {
        if (!machine.hasProxies()) return null;

        final var wirelessTrait = gTLAdditions$machine.getWirelessNetworkEnergyHandler();
        if (wirelessTrait != null) return gTLAdditions$getWirelessRecipe(wirelessTrait);

        long maxEUt = getMachine().getOverclockVoltage();
        if (maxEUt <= 0) return null;
        var iterator = lookupRecipeIterator();
        double euMultiplier = getEuMultiplier();
        var itemOutputs = new ObjectArrayList<Content>();
        var fluidOutputs = new ObjectArrayList<Content>();

        double totalEu = 0;
        long remain = (long) this.gTLAdditions$machine.getMaxParallel() * Ints.saturatedCast(MAX_THREADS + (long) gTLAdditions$machine.getAdditionalThread());

        while (remain > 0 && iterator.hasNext()) {
            var match = iterator.next();
            if (match == null) continue;
            var p = getMaxParallel(machine, match, remain);
            if (p <= 0) continue;

            GTRecipe paralleledRecipe = RecipeCalculationHelper.INSTANCE.multipleRecipe(match, p);
            paralleledRecipe = getRecipeOutputChance(machine, paralleledRecipe);

            if (handleRecipeInput(machine, paralleledRecipe)) {
                remain -= p;
                totalEu += getTotalEuOfRecipe(paralleledRecipe) * euMultiplier;
                RecipeCalculationHelper.INSTANCE.collectOutputs(paralleledRecipe, itemOutputs, fluidOutputs);
            }
            if (totalEu / maxEUt > 20 * 500) break;
        }

        if (!RecipeCalculationHelper.INSTANCE.hasOutputs(itemOutputs, fluidOutputs)) {
            if (getRecipeStatus() == null || getRecipeStatus().isSuccess()) RecipeResult.of(this.machine, RecipeResult.FAIL_FIND);
            return null;
        }

        return RecipeCalculationHelper.INSTANCE.buildNormalRecipe(itemOutputs, fluidOutputs, totalEu, maxEUt, 20);
    }

    @Unique
    @Nullable
    private WirelessGTRecipe gTLAdditions$getWirelessRecipe(@NotNull IWirelessNetworkEnergyHandler wirelessTrait) {
        if (!wirelessTrait.isOnline()) return null;

        final var iterator = lookupRecipeIterator();
        final var maxTotalEu = wirelessTrait.getMaxAvailableEnergy();
        final var euMultiplier = getEuMultiplier();
        final var itemOutputs = new ObjectArrayList<Content>();
        final var fluidOutputs = new ObjectArrayList<Content>();

        long remain = (long) this.gTLAdditions$machine.getMaxParallel() * (MAX_THREADS + gTLAdditions$machine.getAdditionalThread());
        BigInteger totalEu = BigInteger.ZERO;

        while (remain > 0 && iterator.hasNext()) {
            GTRecipe match = iterator.next();
            if (match == null) continue;
            long p = IParallelLogic.getMaxParallel(machine, match, remain);
            if (p <= 0) continue;

            var parallelEUt = BigInteger.valueOf(RecipeHelper.getInputEUt(match));
            GTRecipe paralleledRecipe = RecipeCalculationHelper.INSTANCE.multipleRecipe(match, p);
            if (p > 1) parallelEUt = parallelEUt.multiply(BigInteger.valueOf(p));

            var tempTotalEu = totalEu.add(BigDecimal.valueOf(paralleledRecipe.duration * euMultiplier).multiply(new BigDecimal(parallelEUt)).toBigInteger());
            if (tempTotalEu.compareTo(maxTotalEu) > 0) {
                if (totalEu.signum() == 0) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_EU_IN);
                break;
            }

            paralleledRecipe = IParallelLogic.getRecipeOutputChance(machine, paralleledRecipe);
            if (RecipeRunnerHelper.handleRecipeInput(machine, paralleledRecipe)) {
                remain -= p;
                totalEu = tempTotalEu;
                RecipeCalculationHelper.INSTANCE.collectOutputs(paralleledRecipe, itemOutputs, fluidOutputs);
            }
        }

        if (!RecipeCalculationHelper.INSTANCE.hasOutputs(itemOutputs, fluidOutputs)) {
            if (getRecipeStatus() == null || getRecipeStatus().isSuccess()) RecipeResult.of(this.machine, RecipeResult.FAIL_FIND);
            return null;
        }

        return RecipeCalculationHelper.INSTANCE.buildWirelessRecipe(itemOutputs, fluidOutputs, 20, totalEu, GTRecipeTypes.DUMMY_RECIPES);
    }

    @SuppressWarnings("all")
    @Override
    @NotNull
    public IWirelessElectricMultiblockMachine getWirelessMachine() {
        return gTLAdditions$machine;
    }

    @Override
    public void handleRecipeWorking() {
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
}
