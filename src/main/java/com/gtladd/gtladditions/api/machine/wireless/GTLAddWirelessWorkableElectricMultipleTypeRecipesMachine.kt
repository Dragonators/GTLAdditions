package com.gtladd.gtladditions.api.machine.wireless

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleTypeWirelessRecipesLogic
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder

open class GTLAddWirelessWorkableElectricMultipleTypeRecipesMachine(
    holder: IMachineBlockEntity,
    @field:Persisted protected val dummyRecipeType: GTRecipeType,
    vararg args: Any?
) : GTLAddWirelessWorkableElectricMultipleRecipesMachine(holder, *args) {

    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return GTLAddMultipleTypeWirelessRecipesLogic(this)
    }

    override fun getRecipeLogic(): GTLAddMultipleTypeWirelessRecipesLogic {
        return super.getRecipeLogic() as GTLAddMultipleTypeWirelessRecipesLogic
    }

    override fun getRecipeType(): GTRecipeType {
        return dummyRecipeType
    }

    override fun getFieldHolder(): ManagedFieldHolder {
        return MANAGED_FIELD_HOLDER
    }

    companion object {
        @JvmStatic
        protected val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            GTLAddWirelessWorkableElectricMultipleTypeRecipesMachine::class.java,
            GTLAddWirelessWorkableElectricMultipleRecipesMachine.MANAGED_FIELD_HOLDER
        )
    }
}
