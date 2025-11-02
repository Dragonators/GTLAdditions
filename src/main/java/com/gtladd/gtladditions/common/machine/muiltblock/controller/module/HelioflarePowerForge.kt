package com.gtladd.gtladditions.common.machine.muiltblock.controller.module

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.common.data.GCyMRecipeTypes.ALLOY_BLAST_RECIPES

class HelioflarePowerForge(holder: IMachineBlockEntity, vararg args: Any?) :
    ForgeOfTheAntichristModuleBase(
        holder,
        *args
    ) {

    override fun createRecipeLogic(vararg args: Any): RecipeLogic = HelioflarePowerForgeLogic(this)

    override fun getRecipeLogic(): HelioflarePowerForgeLogic = super.getRecipeLogic() as HelioflarePowerForgeLogic

    companion object {
        class HelioflarePowerForgeLogic(
            parallel: HelioflarePowerForge
        ) : ForgeOfTheAntichristModuleBase.Companion.ForgeOfTheAntichristModuleBaseLogic(parallel) {
            init {
                this.setReduction(0.2, 1.0)
            }

            override fun getMachine(): HelioflarePowerForge = machine as HelioflarePowerForge

            override fun enableModify(recipe: GTRecipe): Boolean {
                return recipe.recipeType == ALLOY_BLAST_RECIPES
            }
        }
    }
}