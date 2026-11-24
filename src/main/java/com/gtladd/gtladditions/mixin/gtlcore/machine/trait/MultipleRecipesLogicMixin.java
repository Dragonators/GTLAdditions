package com.gtladd.gtladditions.mixin.gtlcore.machine.trait;

import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine;
import org.gtlcore.gtlcore.api.machine.trait.IRecipeStatus;
import org.gtlcore.gtlcore.api.recipe.IGTRecipe;
import org.gtlcore.gtlcore.api.recipe.IParallelLogic;
import org.gtlcore.gtlcore.api.recipe.RecipeExtensionCopier;
import org.gtlcore.gtlcore.api.recipe.RecipeResult;
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper;
import org.gtlcore.gtlcore.common.machine.trait.MultipleRecipesLogic;

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import net.minecraft.nbt.ListTag;

import com.google.common.primitives.Ints;
import com.gtladd.gtladditions.api.machine.IThreadModifierMachine;
import com.gtladd.gtladditions.api.machine.IWirelessElectricMultiblockMachine;
import com.gtladd.gtladditions.api.machine.logic.IWirelessRecipeLogic;
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler;
import com.gtladd.gtladditions.api.recipe.IWirelessGTRecipe;
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe;
import com.gtladd.gtladditions.integration.gtlcore.monitor.BatchMonitorInputs;
import com.gtladd.gtladditions.utils.recipe.RecipeCalculationHelper;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;

@Mixin(MultipleRecipesLogic.class)
public abstract class MultipleRecipesLogicMixin extends RecipeLogic implements IWirelessRecipeLogic, IRecipeStatus {

    @Shadow(remap = false)
    @Final
    private static int MAX_THREADS;
    @Shadow(remap = false)
    @Final
    private ParallelMachine parallel;
    @Unique
    @Nullable
    private BatchMonitorInputs gtladditions$batchInputs;

    public MultipleRecipesLogicMixin(IRecipeLogicMachine machine) {
        super(machine);
    }

    @Shadow(remap = false)
    protected double getEuMultiplier() {
        throw new AssertionError();
    }

    @Shadow(remap = false)
    private Iterator<GTRecipe> lookupRecipeIterator() {
        throw new AssertionError();
    }

    @Inject(method = "getRecipe",
            at = @At(value = "INVOKE",
                     target = "Lcom/gregtechceu/gtceu/api/machine/multiblock/WorkableElectricMultiblockMachine;getOverclockVoltage()J"),
            cancellable = true,
            remap = false,
            require = 1)
    private void gtladditions$findWirelessBatch(CallbackInfoReturnable<GTRecipe> cir) {
        if (machine instanceof IWirelessElectricMultiblockMachine wireless) {
            var handler = wireless.getWirelessNetworkEnergyHandler();
            if (handler != null) cir.setReturnValue(gtladditions$getWirelessRecipe(handler));
        }
    }

    @ModifyConstant(method = "getRecipe", constant = @Constant(longValue = 64L), remap = false, require = 1)
    private long gtladditions$threadCount(long baseThreads) {
        if (machine instanceof IThreadModifierMachine modifier) {
            return Ints.saturatedCast(baseThreads + modifier.getAdditionalThread());
        }
        return baseThreads;
    }

    // Core reuses the match local for both the parallel copy and the rolled outputs.
    @ModifyArg(method = "getRecipe",
               at = @At(value = "INVOKE",
                        target = "Lorg/gtlcore/gtlcore/api/recipe/IParallelLogic;getMaxParallel(Lcom/gregtechceu/gtceu/api/capability/recipe/IRecipeCapabilityHolder;Lcom/gregtechceu/gtceu/api/recipe/GTRecipe;J)J"),
               index = 1,
               remap = false,
               require = 1)
    private GTRecipe gtladditions$rememberSource(GTRecipe recipe, @Share("batchSource") LocalRef<GTRecipe> source) {
        source.set(recipe);
        return recipe;
    }

    @ModifyExpressionValue(method = "getRecipe",
                           at = @At(value = "INVOKE",
                                    target = "Lcom/gregtechceu/gtceu/api/recipe/GTRecipe;copy(Lcom/gregtechceu/gtceu/api/recipe/content/ContentModifier;Z)Lcom/gregtechceu/gtceu/api/recipe/GTRecipe;"),
                           remap = false,
                           require = 1)
    private GTRecipe gtladditions$copyExtensions(GTRecipe copy, @Share("batchSource") LocalRef<GTRecipe> source) {
        return RecipeExtensionCopier.copy(source.get(), copy);
    }

    @ModifyArg(method = "getRecipe",
               at = @At(value = "INVOKE",
                        target = "Lorg/gtlcore/gtlcore/common/machine/trait/MultipleRecipesLogic;getTotalEuOfRecipe(Lcom/gregtechceu/gtceu/api/recipe/GTRecipe;)D"),
               index = 0,
               remap = false,
               require = 1)
    private GTRecipe gtladditions$sourceForEnergy(GTRecipe processed, @Share("batchSource") LocalRef<GTRecipe> source) {
        return source.get();
    }

    @ModifyExpressionValue(method = "getRecipe",
                           at = @At(value = "INVOKE",
                                    target = "Lorg/gtlcore/gtlcore/common/machine/trait/MultipleRecipesLogic;getTotalEuOfRecipe(Lcom/gregtechceu/gtceu/api/recipe/GTRecipe;)D"),
                           remap = false,
                           require = 1)
    private double gtladditions$parallelEnergy(double unitEnergy, @Local(name = "p") long parallels) {
        return unitEnergy * parallels;
    }

    @ModifyExpressionValue(method = "getRecipe",
                           at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(DD)D"),
                           remap = false,
                           require = 1)
    private double gtladditions$finishBatch(double duration, @Local(name = "output") GTRecipe output) {
        IGTRecipe.of(output).setBatchProcessed(true);
        return Math.round(duration);
    }

    @Unique
    @Nullable
    private WirelessGTRecipe gtladditions$getWirelessRecipe(@NotNull IWirelessNetworkEnergyHandler wirelessTrait) {
        if (!wirelessTrait.isOnline()) return null;

        final var iterator = lookupRecipeIterator();
        final var maxTotalEu = wirelessTrait.getMaxAvailableEnergy();
        final var euMultiplier = getEuMultiplier();
        final var itemOutputs = new ObjectArrayList<Content>();
        final var fluidOutputs = new ObjectArrayList<Content>();
        final var monitorInputs = new BatchMonitorInputs();

        long remain = (long) parallel.getMaxParallel() * gtladditions$threadCount(MAX_THREADS);
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
                monitorInputs.addInputs(paralleledRecipe.inputs);
                RecipeCalculationHelper.INSTANCE.collectOutputs(paralleledRecipe, itemOutputs, fluidOutputs);
            }
        }

        if (!RecipeCalculationHelper.INSTANCE.hasOutputs(itemOutputs, fluidOutputs)) {
            if (getRecipeStatus() == null || getRecipeStatus().isSuccess()) RecipeResult.of(this.machine, RecipeResult.FAIL_FIND);
            return null;
        }

        var recipe = RecipeCalculationHelper.INSTANCE.buildWirelessRecipe(
                itemOutputs,
                fluidOutputs,
                20,
                totalEu,
                GTRecipeTypes.DUMMY_RECIPES);
        gtladditions$batchInputs = monitorInputs.bind(recipe);
        return recipe;
    }

    @Inject(method = "monitorInputs", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void gtladditions$monitorWirelessInputs(CallbackInfoReturnable<ListTag> cir) {
        var pending = gtladditions$batchInputs;
        if (pending == null) return;

        var recipe = getLastRecipe();
        if (recipe != null && pending.matches(recipe)) {
            cir.setReturnValue(pending.materializeCopy());
        } else {
            gtladditions$batchInputs = null;
        }
    }

    @Inject(method = { "findAndHandleRecipe", "onRecipeFinish" }, at = @At("RETURN"), remap = false, require = 1)
    private void gtladditions$pruneWirelessInputs(CallbackInfo ci) {
        var pending = gtladditions$batchInputs;
        if (pending != null && (duration <= 0 || isIdle() || lastRecipe == null || !pending.matches(lastRecipe))) {
            gtladditions$batchInputs = null;
        }
    }

    @Override
    public void interruptRecipe() {
        gtladditions$batchInputs = null;
        super.interruptRecipe();
    }

    @Override
    public void resetRecipeLogic() {
        gtladditions$batchInputs = null;
        super.resetRecipeLogic();
    }

    @Override
    public void inValid() {
        gtladditions$batchInputs = null;
        super.inValid();
    }

    @Override
    @NotNull
    public IWirelessElectricMultiblockMachine getWirelessMachine() {
        return (IWirelessElectricMultiblockMachine) machine;
    }

    @Override
    public GTRecipe.ActionResult handleTickRecipe(GTRecipe recipe) {
        if (recipe instanceof IWirelessGTRecipe wireless) {
            if (handleWirelessTickInput(wireless)) return GTRecipe.ActionResult.SUCCESS;
            return GTRecipe.ActionResult.fail(RecipeResult.FAIL_NO_ENOUGH_EU_IN::reason);
        }

        return super.handleTickRecipe(recipe);
    }
}