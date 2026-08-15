package com.gtladd.gtladditions.api.machine.logic

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import java.util.Collections
import java.util.function.Predicate

open class GTLAddMultipleTypeRecipesLogic(
    machine: GTLAddWorkableElectricMultipleRecipesMachine,
    beforeWorking: Predicate<IRecipeLogicMachine>? = null
) : GTLAddMultipleRecipesLogic(machine, beforeWorking) {
    override fun lookupRecipeIterator(): Set<GTRecipe> = if (isLock) {
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