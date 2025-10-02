package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gtladd.gtladditions.api.machine.GTLAddWorkableElectricParallelHatchMultipleRecipesMachine
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import org.gtlcore.gtlcore.utils.MachineIO
import org.gtlcore.gtlcore.utils.Registries
import java.util.function.Predicate

class AntientropyCondensationCenter(holder: IMachineBlockEntity, vararg args: Any?) :
    GTLAddWorkableElectricParallelHatchMultipleRecipesMachine(holder, *args) {

    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return GTLAddMultipleRecipesLogic(this, BEFORE_RECIPE)
    }

    companion object {
        private val BEFORE_RECIPE = Predicate { machine: IRecipeLogicMachine? ->
            if (machine is AntientropyCondensationCenter) return@Predicate MachineIO.inputItem(
                machine,
                Registries.getItemStack("kubejs:dust_cryotheum", 1 shl (14 - machine.getTier()))
            )
            false
        }
    }
}
