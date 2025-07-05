package com.gtladd.gtladditions.api.machine

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gtladd.gtladditions.api.recipeslogic.GTLAddMultipleRecipesLogic
import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine
import org.gtlcore.gtlcore.common.data.GTLRecipeModifiers

class GTLAddCoilWorkableElectricParallelHatchMultipleRecipesMachine(holder: IMachineBlockEntity) :
    CoilWorkableElectricMultiblockMachine(holder), ParallelMachine {
    public override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return GTLAddMultipleRecipesLogic(this)
    }

    override fun getRecipeLogic(): GTLAddMultipleRecipesLogic {
        return super.getRecipeLogic() as GTLAddMultipleRecipesLogic
    }

    override fun getMaxParallel(): Int {
        return GTLRecipeModifiers.getHatchParallel(this)
    }
}
