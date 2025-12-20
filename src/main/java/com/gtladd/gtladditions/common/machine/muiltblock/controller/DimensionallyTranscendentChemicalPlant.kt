package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.api.machine.EBFChecks
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import java.util.function.BiPredicate

class DimensionallyTranscendentChemicalPlant(holder: IMachineBlockEntity) :
    GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(holder) {
    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return DimensionallyTranscendentChemicalPlantLogic(this, EBFChecks.EBF_CHECK)
    }

    override fun getRecipeLogic(): DimensionallyTranscendentChemicalPlantLogic {
        return super.getRecipeLogic() as DimensionallyTranscendentChemicalPlantLogic
    }

    companion object {
        class DimensionallyTranscendentChemicalPlantLogic(
            parallel: DimensionallyTranscendentChemicalPlant, recipeCheck: BiPredicate<GTRecipe, IRecipeLogicMachine>
        ) : GTLAddMultipleRecipesLogic(parallel, recipeCheck) {
            override fun calculateParallels(): ParallelData? {
                val recipes = lookupRecipeIterator()
                val totalParallel = parallel.maxParallel.toLong() * getMultipleThreads()

                return RecipeCalculationHelper.calculateParallelsWithFairAllocation(
                    recipes,
                    totalParallel,
                    { recipe -> getMaxParallel(recipe, totalParallel) },
                    { recipe -> IGTRecipe.of(recipe).euTier <= GTValues.UV }
                )
            }
        }
    }
}