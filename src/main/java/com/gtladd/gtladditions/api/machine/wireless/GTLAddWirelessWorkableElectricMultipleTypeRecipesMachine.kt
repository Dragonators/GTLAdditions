package com.gtladd.gtladditions.api.machine.wireless

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleTypeWirelessRecipesLogic

open class GTLAddWirelessWorkableElectricMultipleTypeRecipesMachine(
    holder: IMachineBlockEntity,
    protected val dummyRecipeType: GTRecipeType,
    vararg args: Any?
) : GTLAddWirelessWorkableElectricMultipleRecipesMachine(holder, *args) {

    override fun createRecipeLogic(vararg args: Any): RecipeLogic = GTLAddMultipleTypeWirelessRecipesLogic(this)

    override fun getRecipeLogic(): GTLAddMultipleTypeWirelessRecipesLogic = super.getRecipeLogic() as GTLAddMultipleTypeWirelessRecipesLogic

    override fun getRecipeType(): GTRecipeType = dummyRecipeType
}