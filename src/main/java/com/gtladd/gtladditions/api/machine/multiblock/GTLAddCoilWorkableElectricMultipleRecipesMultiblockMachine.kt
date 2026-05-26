package com.gtladd.gtladditions.api.machine.multiblock

import com.gregtechceu.gtceu.api.block.ICoilType
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.common.block.CoilBlock
import com.gtladd.gtladditions.api.machine.EBFChecks
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import kotlin.math.min
import kotlin.math.pow

open class GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(holder: IMachineBlockEntity) : GTLAddWorkableElectricMultipleRecipesMachine(holder) {
    var coilType: ICoilType = CoilBlock.CoilType.CUPRONICKEL

    override fun createRecipeLogic(vararg args: Any): RecipeLogic = GTLAddMultipleRecipesLogic(this, EBFChecks.EBF_CHECK)

    override fun getMaxParallel(): Int = min(Int.MAX_VALUE, 2.0.pow(this.coilType.coilTemperature / 900.0).toInt())

    override fun onStructureFormed() {
        super.onStructureFormed()
        (this.multiblockState.matchContext["CoilType"] as? ICoilType)?.let { coilType = it }
    }

    fun getCoilTier(): Int = coilType.tier
}