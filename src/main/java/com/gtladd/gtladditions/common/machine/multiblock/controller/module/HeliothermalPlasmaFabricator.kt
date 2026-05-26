package com.gtladd.gtladditions.common.machine.multiblock.controller.module

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.STELLAR_LGNITION

class HeliothermalPlasmaFabricator(holder: IMachineBlockEntity, vararg args: Any?) :
    ForgeOfTheAntichristModuleBase(
        holder,
        *args
    ) {
    override fun createRecipeLogic(vararg args: Any): HeliothermalPlasmaFabricatorLogic = HeliothermalPlasmaFabricatorLogic(this)

    override fun getRecipeLogic(): HeliothermalPlasmaFabricatorLogic = recipeLogic as HeliothermalPlasmaFabricatorLogic

    companion object {
        class HeliothermalPlasmaFabricatorLogic(
            parallel: HeliothermalPlasmaFabricator
        ) : ForgeOfTheAntichristModuleBase.Companion.ForgeOfTheAntichristModuleBaseLogic(parallel, BEFORE_WORKING) {
            init {
                this.setReduction(0.2, 1.0)
            }

            override fun getMachine(): HeliothermalPlasmaFabricator = machine as HeliothermalPlasmaFabricator

            override fun enableModify(recipeType: GTRecipeType): Boolean = recipeType != STELLAR_LGNITION
        }
    }
}