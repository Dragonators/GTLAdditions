package com.gtladd.gtladditions.api.machine.logic

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import java.math.BigInteger
import java.util.*
import java.util.function.BiPredicate

open class GTLAddMultipleTypeWirelessRecipesLogic : GTLAddMultipleWirelessRecipesLogic {
    constructor(parallel: GTLAddWirelessWorkableElectricMultipleRecipesMachine) : super(parallel)

    constructor(
        parallel: GTLAddWirelessWorkableElectricMultipleRecipesMachine,
        beforeWorking: BiPredicate<GTRecipe, IRecipeLogicMachine>?
    ) : super(parallel, beforeWorking)

    override fun buildWirelessRecipe(
        itemOutputs: ObjectArrayList<Content>,
        fluidOutputs: ObjectArrayList<Content>,
        totalEu: BigInteger
    ): WirelessGTRecipe {
        return RecipeCalculationHelper.buildWirelessRecipe(
            itemOutputs, fluidOutputs, limited.getLimitedDuration(), totalEu, getMachine().recipeType
        )
    }

    override fun lookupRecipeIterator(): Set<GTRecipe> {
        return if (isLock) {
            when {
                lockRecipe == null -> {
                    lockRecipe = machine.recipeTypes.asSequence()
                        .mapNotNull { it.lookup.find(machine, this::checkRecipe) }
                        .firstOrNull()
                    lockRecipe?.let { Collections.singleton(it) } ?: emptySet()
                }
                checkRecipe(lockRecipe) -> Collections.singleton(lockRecipe)
                else -> emptySet()
            }
        } else {
            machine.recipeTypes.asSequence()
                .flatMap { it.lookup.getRecipeIterator(machine, this::checkRecipe).asSequence() }
                .toCollection(ObjectOpenHashSet())
        }
    }
}