package com.gtladd.gtladditions.common.machine.multiblock.controller.module

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.CHAOTIC_ALCHEMY

class HeliofluixMeltingCore(holder: IMachineBlockEntity, vararg args: Any?) :
    ForgeOfTheAntichristModuleBase(
        holder,
        *args
    ) {

    override fun createRecipeLogic(vararg args: Any): HeliofluixMeltingCoreLogic = HeliofluixMeltingCoreLogic(this)

    override fun getRecipeLogic(): HeliofluixMeltingCoreLogic = recipeLogic as HeliofluixMeltingCoreLogic

    companion object {
        class HeliofluixMeltingCoreLogic(
            parallel: HeliofluixMeltingCore
        ) : ForgeOfTheAntichristModuleBase.Companion.ForgeOfTheAntichristModuleBaseLogic(parallel, BEFORE_WORKING) {
            init {
                this.setReduction(0.2, 1.0)
            }

            override fun getMachine(): HeliofluixMeltingCore = machine as HeliofluixMeltingCore

            override fun enableModify(recipeType: GTRecipeType): Boolean = recipeType == CHAOTIC_ALCHEMY
        }
    }
}