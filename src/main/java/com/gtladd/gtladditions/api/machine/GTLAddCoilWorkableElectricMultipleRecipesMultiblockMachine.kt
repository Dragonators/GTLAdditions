package com.gtladd.gtladditions.api.machine

import com.gregtechceu.gtceu.api.block.ICoilType
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.common.block.CoilBlock
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import kotlin.math.min
import kotlin.math.pow

open class GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(holder: IMachineBlockEntity) :
    GTLAddWorkableElectricMultipleRecipesMachine(holder) {
        var coilType: ICoilType = CoilBlock.CoilType.CUPRONICKEL

    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return GTLAddMultipleRecipesLogic(this, EBFChecks.EBF_CHECK)
    }

    override fun getMaxParallel(): Int {
        return min(Int.Companion.MAX_VALUE, 2.0.pow(this.coilType.coilTemperature.toDouble() / 900.0).toInt())
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        val type = multiblockState.matchContext.get<Any?>("CoilType")
        if (type is ICoilType) this.coilType = type
    }

    fun getCoilTier(): Int {
        return coilType.tier
    }
}
