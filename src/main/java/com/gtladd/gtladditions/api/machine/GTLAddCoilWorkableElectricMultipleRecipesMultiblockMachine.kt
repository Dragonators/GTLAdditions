package com.gtladd.gtladditions.api.machine

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gtladd.gtladditions.api.recipeslogic.GTLAddMultipleRecipesLogic
import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine
import kotlin.math.min
import kotlin.math.pow

class GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(holder: IMachineBlockEntity) :
    CoilWorkableElectricMultiblockMachine(holder), ParallelMachine {
    public override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return GTLAddMultipleRecipesLogic(this)
    }

    override fun getRecipeLogic(): GTLAddMultipleRecipesLogic {
        return super.getRecipeLogic() as GTLAddMultipleRecipesLogic
    }

    override fun getMaxParallel(): Int {
        return min(Int.Companion.MAX_VALUE, 2.0.pow(this.coilType.coilTemperature.toDouble() / 900.0).toInt())
    }
}
