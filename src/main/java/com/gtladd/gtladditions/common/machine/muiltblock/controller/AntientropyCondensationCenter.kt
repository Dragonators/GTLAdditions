package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.api.machine.GTLAddWorkableElectricParallelHatchMultipleRecipesMachine
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import org.gtlcore.gtlcore.utils.MachineIO
import org.gtlcore.gtlcore.utils.Registries
import java.util.function.BiPredicate

class AntientropyCondensationCenter(holder: IMachineBlockEntity, vararg args: Any?) :
    GTLAddWorkableElectricParallelHatchMultipleRecipesMachine(holder, *args) {

    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return GTLAddMultipleRecipesLogic(this, BEFORE_RECIPE)
    }

    override fun getFieldHolder(): ManagedFieldHolder {
        return MANAGED_FIELD_HOLDER
    }

    companion object {
        private val BEFORE_RECIPE = BiPredicate { recipe: GTRecipe?, machine: IRecipeLogicMachine? ->
            if (machine is AntientropyCondensationCenter) return@BiPredicate MachineIO.inputItem(machine,
                Registries.getItemStack("kubejs:dust_cryotheum", 1 shl (14 - machine.getTier()))
            )
            false
        }
        val MANAGED_FIELD_HOLDER: ManagedFieldHolder =
            ManagedFieldHolder(AntientropyCondensationCenter::class.java, WorkableMultiblockMachine.MANAGED_FIELD_HOLDER)
    }
}
