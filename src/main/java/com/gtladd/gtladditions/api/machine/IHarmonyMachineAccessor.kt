package com.gtladd.gtladditions.api.machine

import com.gregtechceu.gtceu.api.recipe.GTRecipe
import org.gtlcore.gtlcore.api.recipe.RecipeExtensionCopier

interface IHarmonyMachineAccessor {

    fun consumeCosmosStartup(): Boolean = false

    fun consumeAstralStartup(): Boolean = false

    fun getHarmonyDuration(): Int = 0

    fun applyCreateDataOutput(recipe: GTRecipe): GTRecipe = recipe.copy().also {
        RecipeExtensionCopier.copy(recipe, it)
    }
}