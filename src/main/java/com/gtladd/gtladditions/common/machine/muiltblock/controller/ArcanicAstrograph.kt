package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.logic.OCParams
import com.gregtechceu.gtceu.api.recipe.logic.OCResult
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers
import org.gtlcore.gtlcore.common.machine.multiblock.electric.HarmonyMachine

class ArcanicAstrograph(holder: IMachineBlockEntity, vararg args: Any?) : HarmonyMachine(holder, *args) {
    companion object {
        fun recipeModifier(machine: MetaMachine, recipe: GTRecipe, params: OCParams, result: OCResult): GTRecipe? {
            val recipe1 = HarmonyMachine.recipeModifier(machine, recipe, params, result)
            if (recipe1 != null) return GTRecipeModifiers.accurateParallel(machine, recipe1, 2048, false).getFirst()
            return null
        }
    }
}
