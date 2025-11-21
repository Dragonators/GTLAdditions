package com.gtladd.gtladditions.api.machine.logic

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleRecipesMachine
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.matchRecipe
import java.util.function.BiPredicate
import java.util.function.Predicate

open class GTLAddMultipleWirelessRecipesLogic : GTLAddMultipleRecipesLogic, IWirelessRecipeLogic {
    constructor(parallel: GTLAddWirelessWorkableElectricMultipleRecipesMachine) : super(parallel)

    constructor(
        parallel: GTLAddWirelessWorkableElectricMultipleRecipesMachine,
        recipeCheck: BiPredicate<GTRecipe, IRecipeLogicMachine>?
    ) : super(parallel, recipeCheck)

    constructor(
        parallel: GTLAddWirelessWorkableElectricMultipleRecipesMachine,
        beforeWorking: Predicate<IRecipeLogicMachine>?
    ) : super(parallel, beforeWorking)

    override fun getMachine(): GTLAddWirelessWorkableElectricMultipleRecipesMachine {
        return super.getMachine() as GTLAddWirelessWorkableElectricMultipleRecipesMachine
    }

    override fun getGTRecipe(): GTRecipe? {
        if (!checkBeforeWorking()) return null

        val parallelData = calculateParallels() ?: return null

        return buildFinalWirelessRecipe(parallelData, getMachine().getWirelessNetworkEnergyHandler())
    }

    override fun checkBeforeWorking(): Boolean {
        if (!machine.hasProxies()) return false
        if (!getMachine().getWirelessNetworkEnergyHandler().isOnline) return false
        return beforeWorking == null || beforeWorking.test(machine)
    }

    override fun checkRecipe(recipe: GTRecipe): Boolean {
        return matchRecipe(machine, recipe) && recipe.checkConditions(machine.recipeLogic).isSuccess &&
                (recipeCheck == null || recipeCheck.test(recipe, machine))
    }

    // Disable
    override fun getTotalEuOfRecipe(recipe: GTRecipe): Double {
        return 0.0
    }
}