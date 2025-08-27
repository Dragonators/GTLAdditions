package com.gtladd.gtladditions.api.machine

import com.gregtechceu.gtceu.api.block.ICoilType
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.common.block.CoilBlock
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import java.util.function.BiPredicate
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

open class GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(holder: IMachineBlockEntity) :
    GTLAddWorkableElectricMultipleRecipesMachine(holder) {
        var coilType: ICoilType? = CoilBlock.CoilType.CUPRONICKEL

    companion object {
        private val EBF_CHECK: BiPredicate<GTRecipe?, IRecipeLogicMachine?>? =
            BiPredicate { recipe: GTRecipe?, machine: IRecipeLogicMachine? ->
                val tm = machine as GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine
                val temp = tm.coilType!!.coilTemperature + 100L * max(0, tm.getTier() - 2)
                if (temp < recipe!!.data.getInt("ebf_temp")) {
                    RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_TEMPERATURE)
                    return@BiPredicate false
                }
                return@BiPredicate true
            }
    }

    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return CoilMachineLogic(this)
    }

    override fun getRecipeLogic(): CoilMachineLogic {
        return super.getRecipeLogic() as CoilMachineLogic
    }

    override fun getMaxParallel(): Int {
        return min(Int.Companion.MAX_VALUE, 2.0.pow(this.coilType!!.coilTemperature.toDouble() / 900.0).toInt())
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        val type = multiblockState.matchContext.get<Any?>("CoilType")
        if (type is ICoilType) this.coilType = type
    }

    fun getCoilTier(): Int {
        return coilType!!.tier
    }

    class CoilMachineLogic(machine: GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine) :
        GTLAddMultipleRecipesLogic((machine as ParallelMachine?) !!) {

        override fun checkRecipe(recipe: GTRecipe?): Boolean {
            return super.checkRecipe(recipe) && EBF_CHECK!!.test(recipe, machine)
        }
    }
}
