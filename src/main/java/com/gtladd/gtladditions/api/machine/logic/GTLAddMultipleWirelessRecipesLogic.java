package com.gtladd.gtladditions.api.machine.logic;

import org.gtlcore.gtlcore.api.recipe.RecipeResult;

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleRecipesMachine;
import com.gtladd.gtladditions.api.recipe.IWirelessGTRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;

import static org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.matchRecipe;

public class GTLAddMultipleWirelessRecipesLogic extends GTLAddMultipleRecipesLogic implements IWirelessRecipeLogic {

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

    @Nullable
    @Override
    protected GTRecipe getGTRecipe() {
        if (!checkBeforeWorking()) return null;

        final var parallelData = calculateParallels();
        if (parallelData == null) return null;

        return buildFinalWirelessRecipe(parallelData, getMachine().getWirelessNetworkEnergyHandler());
    }

    @Override
    protected boolean checkBeforeWorking() {
        if (!machine.hasProxies()) return false;
        if (!getMachine().getWirelessNetworkEnergyHandler().isOnline()) return false;
        return this.beforeWorking == null || this.beforeWorking.test(machine);
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

    // Disable
    @Override
    protected double getTotalEuOfRecipe(GTRecipe recipe) {
        return 0;
    }
}
