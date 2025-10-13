package com.gtladd.gtladditions.api.machine

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine
import com.gtladd.gtladditions.api.machine.mutable.MutableCoilElectricMultiblockMachine
import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessCoilWorkableElectricMultipleRecipesMultiblockMachine
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import java.util.function.BiPredicate
import kotlin.math.max

@Suppress("DuplicatedCode")
object EBFChecks {

    val EBF_CHECK: BiPredicate<GTRecipe, IRecipeLogicMachine> =
        BiPredicate { recipe, machine ->
            val tm = machine as GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine
            val temp = tm.coilType.coilTemperature + 100 * max(0, tm.getTier() - 2)
            (temp >= recipe.data.getInt("ebf_temp")).also {
                if (!it) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_TEMPERATURE)
            }
        }

    val WIRELESS_EBF_CHECK: BiPredicate<GTRecipe, IRecipeLogicMachine> =
        BiPredicate { recipe, machine ->
            val tm = machine as GTLAddWirelessCoilWorkableElectricMultipleRecipesMultiblockMachine
            val temp = tm.coilType.coilTemperature + 100 * max(0, tm.getTier() - 2)
            (temp >= recipe.data.getInt("ebf_temp")).also {
                if (!it) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_TEMPERATURE)
            }
        }

    val DIMENSIONALLY__WIRELESS_EBF_CHECK: BiPredicate<GTRecipe, IRecipeLogicMachine> =
        BiPredicate { recipe, machine ->
            val tm = machine as GTLAddWirelessCoilWorkableElectricMultipleRecipesMultiblockMachine
            val temp = (if (tm.coilType.coilTemperature == 273) 32000 else tm.coilType.coilTemperature).toLong()
            (temp >= recipe.data.getInt("ebf_temp")).also {
                if (!it) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_TEMPERATURE)
            }
        }

    val ATOMIC_ENERGY_EXCITATION_PLANT_CHECK: BiPredicate<GTRecipe, IRecipeLogicMachine> =
        BiPredicate { recipe, machine ->
            val tm = machine as MutableCoilElectricMultiblockMachine
            (tm.coilType.coilTemperature >= recipe.data.getInt("ebf_temp")).also {
                if (!it) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_TEMPERATURE)
            }
        }
}
