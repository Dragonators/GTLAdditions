package com.gtladd.gtladditions.api.machine.mutable

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gtladd.gtladditions.api.machine.IWirelessThreadModifierParallelMachine
import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart
import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic

open class MutableElectricMultiblockMachine(holder: IMachineBlockEntity, vararg args: Any?) :
    WorkableElectricMultiblockMachine(holder, *args), IWirelessThreadModifierParallelMachine {
    private var threadPartMachine: IThreadModifierPart? = null

    override fun createRecipeLogic(vararg args: Any?): RecipeLogic {
        return MutableRecipesLogic(this)
    }

    @Suppress("UNCHECKED_CAST")
    override fun getRecipeLogic(): MutableRecipesLogic<MutableElectricMultiblockMachine> {
        return super.getRecipeLogic() as MutableRecipesLogic<MutableElectricMultiblockMachine>
    }

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        threadPartMachine = null
        getRecipeLogic().setUseMultipleRecipes(false)
    }

    override fun getMaxParallel(): Int {
        return Int.MAX_VALUE
    }

    override fun setThreadPartMachine(threadModifierPart: IThreadModifierPart) {
        this.threadPartMachine = threadModifierPart
    }

    override fun getThreadPartMachine(): IThreadModifierPart? = this.threadPartMachine
}
