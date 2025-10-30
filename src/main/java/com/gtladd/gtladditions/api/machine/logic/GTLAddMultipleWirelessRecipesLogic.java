package com.gtladd.gtladditions.api.machine.logic;

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleRecipesMachine;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.matchRecipe;

public class GTLAddMultipleWirelessRecipesLogic extends GTLAddMultipleRecipesLogic implements IWirelessRecipeLogic {

    public GTLAddMultipleWirelessRecipesLogic(GTLAddWirelessWorkableElectricMultipleRecipesMachine parallel) {
        super(parallel);
    }

    public GTLAddMultipleWirelessRecipesLogic(GTLAddWirelessWorkableElectricMultipleRecipesMachine parallel, BiPredicate<GTRecipe, IRecipeLogicMachine> recipeCheck) {
        super(parallel, recipeCheck);
    }

    public GTLAddMultipleWirelessRecipesLogic(GTLAddWirelessWorkableElectricMultipleRecipesMachine parallel, Predicate<IRecipeLogicMachine> beforeWorking) {
        super(parallel, beforeWorking);
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

    // Disable
    @Override
    protected double getTotalEuOfRecipe(GTRecipe recipe) {
        return 0;
    }
}
