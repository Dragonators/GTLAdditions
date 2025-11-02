package com.gtladd.gtladditions.common.machine.muiltblock.controller.module

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.CHAOTIC_ALCHEMY

class HeliofluixMeltingCore(holder: IMachineBlockEntity, vararg args: Any?) :
    ForgeOfTheAntichristModuleBase(
        holder,
        *args
    ) {

    override fun createRecipeLogic(vararg args: Any): RecipeLogic = HeliofluixMeltingCoreLogic(this)

    override fun getRecipeLogic(): HeliofluixMeltingCoreLogic = super.getRecipeLogic() as HeliofluixMeltingCoreLogic

    companion object {
        class HeliofluixMeltingCoreLogic(
            parallel: HeliofluixMeltingCore
        ) : ForgeOfTheAntichristModuleBase.Companion.ForgeOfTheAntichristModuleBaseLogic(parallel) {
            init {
                this.setReduction(0.2, 1.0)
            }

            override fun getMachine(): HeliofluixMeltingCore = machine as HeliofluixMeltingCore

            override fun enableModify(recipe: GTRecipe): Boolean {
                return recipe.recipeType == CHAOTIC_ALCHEMY
            }
        }
    }
}