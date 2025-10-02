package com.gtladd.gtladditions.api.machine.logic;

import org.gtlcore.gtlcore.api.recipe.IGTRecipe;
import org.gtlcore.gtlcore.api.recipe.RecipeResult;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleRecipesMachine;
import com.gtladd.gtladditions.api.recipe.IWirelessGTRecipe;
import com.hepdd.gtmthings.api.misc.WirelessEnergyManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import static org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.*;

public class GTLAddMultipleWirelessRecipesLogic extends GTLAddMultipleRecipesLogic {

    public static final BigInteger MAX_EU_RATIO = BigInteger.valueOf(10);

    public GTLAddMultipleWirelessRecipesLogic(GTLAddWirelessWorkableElectricMultipleRecipesMachine parallel) {
        super(parallel);
    }

    public GTLAddMultipleWirelessRecipesLogic(GTLAddWirelessWorkableElectricMultipleRecipesMachine parallel, BiPredicate<GTRecipe, IRecipeLogicMachine> recipeCheck) {
        super(parallel, recipeCheck);
    }

    @Override
    public GTLAddWirelessWorkableElectricMultipleRecipesMachine getMachine() {
        return (GTLAddWirelessWorkableElectricMultipleRecipesMachine) super.getMachine();
    }

    @Override
    protected double getTotalEuOfRecipe(GTRecipe recipe) {
        // only duration
        return recipe.duration;
    }

    @Override
    protected boolean checkBeforeWorking() {
        if (!machine.hasProxies()) return false;

        final UUID uuid = getMachine().getUuid();
        if (uuid == null || WirelessEnergyManager.getUserEU(uuid).signum() <= 0) return false;

        return this.beforeWorking == null || this.beforeWorking.test(machine);
    }

    @Nullable
    @Override
    protected GTRecipe buildFinalRecipe(ParallelData parallelData) {
        final UUID uuid = getMachine().getUuid();
        assert uuid != null;
        BigInteger maxTotalEu = WirelessEnergyManager.getUserEU(uuid).divide(MAX_EU_RATIO);

        GTRecipe recipe = GTRecipeBuilder.ofRaw().buildRawRecipe();
        recipe.outputs.put(ItemRecipeCapability.CAP, new ObjectArrayList<>());
        recipe.outputs.put(FluidRecipeCapability.CAP, new ObjectArrayList<>());

        double euMultiplier = this.getEuMultiplier();
        BigInteger totalEu = BigInteger.ZERO;
        int index = 0;

        for (var r : parallelData.recipeList()) {
            BigInteger parallelEUt = BigInteger.valueOf(RecipeHelper.getInputEUt(r));

            final long p = parallelData.parallels()[index++];
            if (p > 1) {
                r = r.copy(ContentModifier.multiplier(p), false);
                parallelEUt = parallelEUt.multiply(BigInteger.valueOf(p));
            }
            ((IGTRecipe) r).setRealParallels(p);

            var tempTotalEu = totalEu.add(BigDecimal.valueOf(getTotalEuOfRecipe(r) * euMultiplier).multiply(new BigDecimal(parallelEUt)).toBigInteger());
            if (tempTotalEu.compareTo(maxTotalEu) > 0) break;
            else totalEu = tempTotalEu;

            r = modifyInputAndOutput(r);
            if (matchRecipeInput(machine, r) && handleRecipeInput(machine, r)) {
                var item = r.outputs.get(ItemRecipeCapability.CAP);
                if (item != null) recipe.outputs.get(ItemRecipeCapability.CAP).addAll(item);
                var fluid = r.outputs.get(FluidRecipeCapability.CAP);
                if (fluid != null) recipe.outputs.get(FluidRecipeCapability.CAP).addAll(fluid);
            }
        }

        if (recipe.outputs.get(ItemRecipeCapability.CAP).isEmpty() && recipe.outputs.get(FluidRecipeCapability.CAP).isEmpty()) {
            if (totalEu.signum() == 0) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_EU_IN);
            return null;
        }

        int minDuration = limited.getLimitedDuration();
        BigInteger eut = totalEu.divide(BigInteger.valueOf(minDuration)).negate();
        ((IWirelessGTRecipe) recipe).setEuTickInputs(eut);
        recipe.duration = minDuration;
        return recipe;
    }

    protected boolean handleWirelessTickInput(IWirelessGTRecipe recipe) {
        final BigInteger euTickInputs = recipe.getEuTickInputs();
        if (euTickInputs == null) return true;

        final UUID uuid = getMachine().getUuid();
        if (uuid == null) return false;
        return WirelessEnergyManager.addEUToGlobalEnergyMap(uuid, euTickInputs, getMachine());
    }

    @Override
    protected boolean checkRecipe(GTRecipe recipe) {
        return matchRecipe(machine, recipe) && recipe.checkConditions(machine.getRecipeLogic()).isSuccess() &&
                (recipeCheck == null || recipeCheck.test(recipe, machine));
    }

    @Override
    public void handleRecipeWorking() {
        assert this.lastRecipe != null;

        if (this.handleWirelessTickInput((IWirelessGTRecipe) this.lastRecipe)) {
            this.setStatus(Status.WORKING);
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
