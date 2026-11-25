package com.gtladd.gtladditions.api.machine.logic

import com.gregtechceu.gtceu.api.machine.feature.IOverclockMachine
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import org.gtlcore.gtlcore.api.recipe.RecipeResult

class UpkeepRecipeLogic(machine: IRecipeLogicMachine, private val upkeep: GTRecipe) : RecipeLogic(machine) {
    private val tier = RecipeHelper.getRecipeEUtTier(upkeep)

    override fun findAndHandleRecipe() {
        lastRecipe = null
        RecipeResult.of(machine, null)
        // The matched-recipe path, not a recipe-map search, keeps modifiers and the continuation between cycles native.
        if (checkMatchedRecipeAvailable(upkeep)) return
        if (machine is IOverclockMachine && tier > (machine as IOverclockMachine).maxOverclockTier) {
            RecipeResult.of(machine, RecipeResult.FAIL_VOLTAGE_TIER)
        }
    }

    companion object {
        const val CYCLE_TICKS = 1200
    }
}
