package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.google.common.math.IntMath
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.common.machine.hatch.OreProcessorHatch
import com.gtladd.gtladditions.utils.OreProcessorRecipeHelper
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import it.unimi.dsi.fastutil.longs.LongLongPair
import org.gtlcore.gtlcore.utils.NumberUtils

class OreProcessorMachine(holder: IMachineBlockEntity) : GTLAddWorkableElectricMultipleRecipesMachine(holder) {

    private var opHatch: OreProcessorHatch? = null

    override fun onStructureFormed() {
        super.onStructureFormed()
        parts.forEach {
            if (it is OreProcessorHatch) {
                opHatch = it
                return
            }
        }
    }

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        opHatch = null
    }

    override fun createRecipeLogic(vararg args: Any): RecipeLogic = OreProcessorRecipeLogic(this)

    override fun getRecipeLogic(): OreProcessorRecipeLogic = super.getRecipeLogic() as OreProcessorRecipeLogic

    override fun getMaxParallel(): Int = Int.MAX_VALUE

    private fun modifyRecipe(recipe: GTRecipe): GTRecipe = OreProcessorRecipeHelper.copyForOreProcessor(
        recipe,
        itemChanceBoost = opHatch?.itemChanceBoost ?: 1,
        inputFluidMultiplier = opHatch?.fluidMultiplier ?: 1.0
    )

    companion object {
        class OreProcessorRecipeLogic(oreProcessor: OreProcessorMachine) : GTLAddMultipleRecipesLogic(oreProcessor) {
            override fun getMachine(): OreProcessorMachine = super.getMachine() as OreProcessorMachine

            @Suppress("UnstableApiUsage")
            override fun getMultipleThreads(): Int = IntMath.saturatedAdd(super.getMultipleThreads(), getMachine().opHatch?.advancedThreadBonus ?: 0)

            override fun calculateParallels(): ParallelData? {
                val hatch = getMachine().opHatch
                val totalParallel = NumberUtils.saturatedMultiply(
                    parallel.maxParallel.toLong(),
                    getMultipleThreads().toLong()
                ).let { if (hatch?.matchAll == true) NumberUtils.saturatedMultiply(it, 2) else it }

                val recipes = lookupRecipeIterator().map { getMachine().modifyRecipe(it) }
                return RecipeCalculationHelper.calculateParallelsWithGreedyAllocation(
                    recipes,
                    totalParallel,
                    machine,
                    getParallelAndConsumption = { recipe, remaining ->
                        val p = getMaxParallel(recipe, remaining)
                        LongLongPair.of(p, p)
                    }
                )
            }
        }
    }
}