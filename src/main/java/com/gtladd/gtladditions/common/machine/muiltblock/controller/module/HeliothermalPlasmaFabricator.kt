package com.gtladd.gtladditions.common.machine.muiltblock.controller.module

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.STELLAR_LGNITION

class HeliothermalPlasmaFabricator(holder: IMachineBlockEntity, vararg args: Any?) :
    ForgeOfTheAntichristModuleBase(
        holder,
        *args
    ) {
    override fun createRecipeLogic(vararg args: Any): RecipeLogic = HeliothermalPlasmaFabricatorLogic(this)

    override fun getRecipeLogic(): HeliothermalPlasmaFabricatorLogic =
        super.getRecipeLogic() as HeliothermalPlasmaFabricatorLogic

    companion object {
        class HeliothermalPlasmaFabricatorLogic(
            parallel: HeliothermalPlasmaFabricator
        ) : ForgeOfTheAntichristModuleBase.Companion.ForgeOfTheAntichristModuleBaseLogic(parallel) {
            init {
                this.setReduction(0.2, 1.0)
            }

            override fun getMachine(): HeliothermalPlasmaFabricator = machine as HeliothermalPlasmaFabricator

            override fun enableModify(recipe: GTRecipe): Boolean {
                return recipe.recipeType != STELLAR_LGNITION
            }
        }
    }
}