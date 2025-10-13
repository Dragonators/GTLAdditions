package com.gtladd.gtladditions.api.machine.wireless

import com.gregtechceu.gtceu.api.block.ICoilType
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.common.block.CoilBlock
import com.gtladd.gtladditions.api.machine.EBFChecks
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleWirelessRecipesLogic
import kotlin.math.min
import kotlin.math.pow

open class GTLAddWirelessCoilWorkableElectricMultipleRecipesMultiblockMachine(
    holder: IMachineBlockEntity,
    vararg args: Any?
) : GTLAddWirelessWorkableElectricMultipleRecipesMachine(holder, *args) {
    var coilType: ICoilType = CoilBlock.CoilType.CUPRONICKEL

    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return GTLAddMultipleWirelessRecipesLogic(this, EBFChecks.WIRELESS_EBF_CHECK)
    }

    override fun getMaxParallel(): Int {
        return min(Int.MAX_VALUE, 2.0.pow(this.coilType.coilTemperature / 900.0).toInt())
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        (this.multiblockState.matchContext.get<Any?>("CoilType") as? ICoilType)?.let { coilType = it }
    }

    fun getCoilTier(): Int {
        return coilType.tier
    }
}
