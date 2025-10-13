package com.gtladd.gtladditions.api.machine.mutable

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gtladd.gtladditions.api.machine.IWirelessThreadModifierParallelMachine
import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart
import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic
import kotlin.math.min
import kotlin.math.pow

open class MutableCoilElectricMultiblockMachine(holder: IMachineBlockEntity) :
    CoilWorkableElectricMultiblockMachine(holder), IWirelessThreadModifierParallelMachine {

    private var threadPartMachine: IThreadModifierPart? = null

    override fun createRecipeLogic(vararg args: Any?): RecipeLogic {
        return MutableRecipesLogic(this)
    }

    @Suppress("UNCHECKED_CAST")
    override fun getRecipeLogic(): MutableRecipesLogic<MutableCoilElectricMultiblockMachine> {
        return super.getRecipeLogic() as MutableRecipesLogic<MutableCoilElectricMultiblockMachine>
    }

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        threadPartMachine = null
        getRecipeLogic().setUseMultipleRecipes(false)
    }

    override fun onPartUnload() {
        super.onPartUnload()
        threadPartMachine = null
        getRecipeLogic().setUseMultipleRecipes(false)
    }

    override fun getMaxParallel(): Int {
        return min(Int.MAX_VALUE, 2.0.pow(this.coilType.coilTemperature / 900.0).toInt())
    }

    override fun setThreadPartMachine(threadModifierPart: IThreadModifierPart) {
        this.threadPartMachine = threadModifierPart
    }

    override fun getAdditionalThread(): Int {
        return threadPartMachine?.threadCount ?: 0
    }
}
