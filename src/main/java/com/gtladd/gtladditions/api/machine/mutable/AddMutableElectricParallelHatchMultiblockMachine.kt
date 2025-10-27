package com.gtladd.gtladditions.api.machine.mutable

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gtladd.gtladditions.api.machine.logic.AddMutableRecipesLogic

class AddMutableElectricParallelHatchMultiblockMachine (holder: IMachineBlockEntity, vararg args: Any?) :
    MutableElectricParallelHatchMultiblockMachine(holder, *args) {
    override fun createRecipeLogic(vararg args: Any?): RecipeLogic {
        return AddMutableRecipesLogic(this)
    }

    @Suppress("UNCHECKED_CAST")
    override fun getRecipeLogic(): AddMutableRecipesLogic<MutableElectricMultiblockMachine> {
        return super.getRecipeLogic() as AddMutableRecipesLogic<MutableElectricMultiblockMachine>
    }
}