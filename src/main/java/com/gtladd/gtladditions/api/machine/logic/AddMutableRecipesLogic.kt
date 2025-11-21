package com.gtladd.gtladditions.api.machine.logic

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.api.machine.IThreadModifierMachine
import com.gtladd.gtladditions.api.machine.IWirelessElectricMultiblockMachine
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine
import org.gtlcore.gtlcore.api.recipe.RecipeResult

open class AddMutableRecipesLogic<T>(machine: T) : MutableRecipesLogic<T>(machine)
        where T : WorkableElectricMultiblockMachine,
              T : IRecipeLogicMachine,
              T : IWirelessElectricMultiblockMachine,
              T : IThreadModifierMachine,
              T : ParallelMachine {

    override fun getRecipe(): GTRecipe? {
        if (!machine.hasProxies()) return null
        if (getMachine().overclockVoltage <= 0) return null

        val parallelData = calculateParallels() ?: return null

        val wirelessTrait = getMachine().getWirelessNetworkEnergyHandler()
        return if (wirelessTrait != null) buildFinalWirelessRecipe(parallelData, wirelessTrait)
        else buildFinalNormalRecipe(parallelData)
    }

    protected open fun calculateParallels(): ParallelData? {
        val recipes = lookupRecipeSet()
        val length = recipes.size
        if (length == 0) return null

        val totalParallel = getMachine().maxParallel.toLong() * getMultipleThreads()
        var remaining = totalParallel
        val parallels = LongArray(length)
        var index = 0
        val recipeList = ObjectArrayList<GTRecipe>(length)
        val remainingWants = LongArrayList(length)
        val remainingIndices = IntArrayList(length)

        for (r in recipes) {
            val pair = calculateParallel(machine, r, totalParallel)
            val p = pair.firstLong()
            if (p <= 0) continue
            recipeList.add(r)
            val allocated = minOf(p, totalParallel / length)
            parallels[index] = allocated
            val want = p - allocated
            if (want > 0) {
                remainingWants.add(want)
                remainingIndices.add(index)
            }
            remaining -= allocated
            index++
        }

        if (recipeList.isEmpty()) return null

        return RecipeCalculationHelper.getParallelData(remaining, parallels, remainingWants, remainingIndices, recipeList)
    }

    protected open fun buildFinalNormalRecipe(parallelData: ParallelData): GTRecipe? {
        val maxEUt = getMachine().overclockVoltage
        val (itemOutputs, fluidOutputs, totalEu) = RecipeCalculationHelper.processParallelDataNormal(
            parallelData, machine, maxEUt, euMultiplier, { getRecipeEut(it) * it.duration.toDouble() }
        )

        if (!RecipeCalculationHelper.hasOutputs(itemOutputs, fluidOutputs)) {
            if (recipeStatus == null || recipeStatus.isSuccess) RecipeResult.of(this.machine, RecipeResult.FAIL_FIND)
            return null
        }

        return RecipeCalculationHelper.buildNormalRecipe(itemOutputs, fluidOutputs, totalEu, maxEUt, 20)
    }

    protected open fun buildFinalWirelessRecipe(
        parallelData: ParallelData,
        wirelessTrait: IWirelessNetworkEnergyHandler
    ): WirelessGTRecipe? {
        if (!wirelessTrait.isOnline) return null

        val (itemOutputs, fluidOutputs, totalEu) = RecipeCalculationHelper.processParallelDataWireless(
            parallelData, machine, wirelessTrait.maxAvailableEnergy, euMultiplier, this::getRecipeEut
        )

        if (!RecipeCalculationHelper.hasOutputs(itemOutputs, fluidOutputs)) {
            if (recipeStatus == null || recipeStatus.isSuccess) RecipeResult.of(this.machine, RecipeResult.FAIL_FIND)
            return null
        }

        return RecipeCalculationHelper.buildWirelessRecipe(itemOutputs, fluidOutputs, 20, totalEu)
    }

    protected fun lookupRecipeSet(): Set<GTRecipe> {
        val iter = lookupRecipeIterator()
        val recipeSet = ObjectOpenHashSet<GTRecipe>()
        while (iter.hasNext()) recipeSet.add(iter.next())
        return recipeSet
    }
}