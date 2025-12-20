package com.gtladd.gtladditions.api.machine.logic

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gtladd.gtladditions.api.machine.IThreadModifierMachine
import com.gtladd.gtladditions.api.machine.IWirelessElectricMultiblockMachine
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import it.unimi.dsi.fastutil.longs.LongBooleanPair
import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine

open class AddMutableRecipesLogic<T>(machine: T) : MutableRecipesLogic<T>(machine)
        where T : WorkableElectricMultiblockMachine,
              T : IRecipeLogicMachine,
              T : IWirelessElectricMultiblockMachine,
              T : IThreadModifierMachine,
              T : ParallelMachine {

    override fun calculateParallels(): ParallelData? {
        val recipes = lookupRecipeSet()
        val totalParallel = getMachine().maxParallel.toLong() * getMultipleThreads()

        return RecipeCalculationHelper.calculateParallelsWithFairAllocation(
            recipes,
            totalParallel
        ) { recipe ->
            LongBooleanPair.of(calculateParallel(machine, recipe, totalParallel).firstLong(), true)
        }
    }
}