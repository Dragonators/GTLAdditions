package com.gtladd.gtladditions.common.machine.multiblock.controller.module

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.common.data.GCyMRecipeTypes.ALLOY_BLAST_RECIPES

class HelioflarePowerForge(holder: IMachineBlockEntity, vararg args: Any?) :
    ForgeOfTheAntichristModuleBase(
        holder,
        *args
    ) {

    override fun createRecipeLogic(vararg args: Any): HelioflarePowerForgeLogic = HelioflarePowerForgeLogic(this)

    override fun getRecipeLogic(): HelioflarePowerForgeLogic = recipeLogic as HelioflarePowerForgeLogic

    companion object {
        class HelioflarePowerForgeLogic(
            parallel: HelioflarePowerForge
        ) : ForgeOfTheAntichristModuleBase.Companion.ForgeOfTheAntichristModuleBaseLogic(parallel, BEFORE_WORKING) {
            init {
                this.setReduction(0.2, 1.0)
            }

            override fun getMachine(): HelioflarePowerForge = machine as HelioflarePowerForge

            override fun enableModify(recipeType: GTRecipeType): Boolean = recipeType == ALLOY_BLAST_RECIPES
        }
    }
}