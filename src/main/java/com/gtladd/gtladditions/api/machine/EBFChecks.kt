package com.gtladd.gtladditions.api.machine

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessCoilWorkableElectricMultipleRecipesMultiblockMachine
import net.minecraft.nbt.CompoundTag
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import java.util.function.BiPredicate
import kotlin.math.max

@Suppress("DuplicatedCode")
class EBFChecks {
    companion object {
        @JvmField
        val EBF_CHECK: BiPredicate<GTRecipe, IRecipeLogicMachine> =
            BiPredicate { recipe, machine ->
                val tm = machine as GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine
                val temp = tm.coilType.coilTemperature + 100 * max(0, tm.getTier() - 2)
                (temp >= recipe.data.getInt("ebf_temp")).also {
                    if (!it) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_TEMPERATURE)
                }
            }

        @JvmField
        val WIRELESS_EBF_CHECK: BiPredicate<GTRecipe, IRecipeLogicMachine> =
            BiPredicate { recipe, machine ->
                val tm = machine as GTLAddWirelessCoilWorkableElectricMultipleRecipesMultiblockMachine
                val temp = tm.coilType.coilTemperature + 100 * max(0, tm.getTier() - 2)
                (temp >= recipe.data.getInt("ebf_temp")).also {
                    if (!it) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_TEMPERATURE)
                }
            }

        @JvmField
        val DIMENSIONALLY__WIRELESS_EBF_CHECK: BiPredicate<GTRecipe, IRecipeLogicMachine> =
            BiPredicate { recipe, machine ->
                val tm = machine as GTLAddWirelessCoilWorkableElectricMultipleRecipesMultiblockMachine
                val temp = (if (tm.coilType.coilTemperature == 273) 32000 else tm.coilType.coilTemperature).toLong()
                (temp >= recipe.data.getInt("ebf_temp")).also {
                    if (!it) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_TEMPERATURE)
                }
            }
    }
}
