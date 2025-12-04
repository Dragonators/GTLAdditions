package com.gtladd.gtladditions.utils

import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gregtechceu.gtceu.common.data.GTRecipeTypes
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipeBuilder
import com.gtladd.gtladditions.common.data.ParallelData
import it.unimi.dsi.fastutil.ints.IntList
import it.unimi.dsi.fastutil.longs.LongList
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import org.gtlcore.gtlcore.api.recipe.IParallelLogic
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.min

object RecipeCalculationHelper {

    // ===================================================
    // Process Recipe
    // ===================================================

    fun multipleRecipe(
        recipe: GTRecipe,
        parallel: Long
    ): GTRecipe {
        val processed = if (parallel > 1) recipe.copy(ContentModifier.multiplier(parallel.toDouble()), false) else recipe
        IGTRecipe.of(processed).realParallels = parallel
        return processed
    }

    inline fun processParallelDataNormal(
        parallelData: ParallelData,
        machine: IRecipeLogicMachine,
        maxEUt: Long,
        euMultiplier: Double,
        getTotalRecipeEu: (GTRecipe) -> Double,
        crossinline shouldBreak: (Double) -> Boolean = { totalEu -> totalEu / maxEUt > 20 * 500 }
    ): Triple<ObjectArrayList<Content>, ObjectArrayList<Content>, Double> {
        val itemOutputs = ObjectArrayList<Content>()
        val fluidOutputs = ObjectArrayList<Content>()
        var totalEu = 0.0
        var index = 0

        for (r in parallelData.recipeList) {
            val p = parallelData.parallels[index++]
            var paralleledRecipe = multipleRecipe(r, p)
            paralleledRecipe = IParallelLogic.getRecipeOutputChance(machine, paralleledRecipe)

            if (RecipeRunnerHelper.matchRecipeInput(machine, paralleledRecipe) && RecipeRunnerHelper.handleRecipeInput(
                    machine,
                    paralleledRecipe
                )
            ) {
                totalEu += getTotalRecipeEu(r) * p * euMultiplier
                collectOutputs(paralleledRecipe, itemOutputs, fluidOutputs)
            }
            if (shouldBreak(totalEu)) break
        }

        return Triple(itemOutputs, fluidOutputs, totalEu)
    }

    inline fun processParallelDataWireless(
        parallelData: ParallelData,
        machine: IRecipeLogicMachine,
        maxTotalEu: BigInteger,
        euMultiplier: Double,
        getRecipeEut: (GTRecipe) -> Long
    ): Triple<ObjectArrayList<Content>, ObjectArrayList<Content>, BigInteger> {
        val itemOutputs = ObjectArrayList<Content>()
        val fluidOutputs = ObjectArrayList<Content>()
        var totalEu = BigInteger.ZERO
        var index = 0

        for (r in parallelData.recipeList) {
            val p = parallelData.parallels[index++]
            var paralleledRecipe = multipleRecipe(r, p)
            var parallelEUt = BigInteger.valueOf(getRecipeEut(r))
            if (p > 1) parallelEUt = parallelEUt.multiply(BigInteger.valueOf(p))

            val tempTotalEu = totalEu.add(
                BigDecimal.valueOf(paralleledRecipe.duration * euMultiplier)
                    .multiply(BigDecimal(parallelEUt)).toBigInteger()
            )

            if (tempTotalEu > maxTotalEu) {
                if (totalEu.signum() == 0) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_EU_IN)
                break
            }

            paralleledRecipe = IParallelLogic.getRecipeOutputChance(machine, paralleledRecipe)
            if (RecipeRunnerHelper.matchRecipeInput(machine, paralleledRecipe) && RecipeRunnerHelper.handleRecipeInput(
                    machine,
                    paralleledRecipe
                )
            ) {
                totalEu = tempTotalEu
                collectOutputs(paralleledRecipe, itemOutputs, fluidOutputs)
            }
        }

        return Triple(itemOutputs, fluidOutputs, totalEu)
    }

    fun buildNormalRecipe(
        itemOutputs: List<Content>,
        fluidOutputs: List<Content>,
        totalEu: Double,
        maxEUt: Long,
        minDuration: Int
    ): GTRecipe {
        val recipe = GTRecipeBuilder.ofRaw().buildRawRecipe()
        recipe.outputs[ItemRecipeCapability.CAP] = itemOutputs
        recipe.outputs[FluidRecipeCapability.CAP] = fluidOutputs

        val d = totalEu / maxEUt
        val eut = if (d > minDuration) maxEUt else (totalEu / minDuration).toLong()
        recipe.tickInputs[EURecipeCapability.CAP] = listOf(Content(eut, 10000, 10000, 0, null, null))
        recipe.duration = maxOf(d, minDuration.toDouble()).toInt()
        IGTRecipe.of(recipe).setHasTick(true)
        return recipe
    }

    fun buildWirelessRecipe(
        itemOutputs: List<Content>,
        fluidOutputs: List<Content>,
        duration: Int,
        totalEu: BigInteger,
        recipeType: GTRecipeType = GTRecipeTypes.DUMMY_RECIPES
    ): WirelessGTRecipe {
        val eut = totalEu.divide(BigInteger.valueOf(duration.toLong())).negate()
        return WirelessGTRecipeBuilder
            .ofRaw(recipeType)
            .output(ItemRecipeCapability.CAP, itemOutputs)
            .output(FluidRecipeCapability.CAP, fluidOutputs)
            .duration(duration)
            .setWirelessEut(eut)
            .buildRawRecipe()
    }

    fun collectOutputs(
        recipe: GTRecipe,
        itemOutputs: MutableList<Content>,
        fluidOutputs: MutableList<Content>
    ) {
        recipe.outputs[ItemRecipeCapability.CAP]?.let { itemOutputs.addAll(it) }
        recipe.outputs[FluidRecipeCapability.CAP]?.let { fluidOutputs.addAll(it) }
    }

    fun hasOutputs(itemOutputs: List<Content>, fluidOutputs: List<Content>): Boolean {
        return itemOutputs.isNotEmpty() || fluidOutputs.isNotEmpty()
    }

    // ===================================================
    // ParallelData Calculation
    // ===================================================

    fun getParallelData(
        remaining: Long,
        parallels: LongArray,
        remainingWants: LongList,
        remainingIndices: IntList,
        recipeList: ObjectList<GTRecipe>
    ): ParallelData? {
        if (recipeList.isEmpty()) return null
        if (remaining <= 0 || remainingWants.isEmpty()) return ParallelData(recipeList, parallels)

        return if (remainingWants.size <= 64)
            getParallelDataBitmap(remaining, parallels, remainingWants, remainingIndices, recipeList)
        else
            getParallelDataIndexArray(remaining, parallels, remainingWants, remainingIndices, recipeList)
    }

    private fun getParallelDataBitmap(
        remaining: Long,
        parallels: LongArray,
        remainingWants: LongList,
        remainingIndices: IntList,
        recipeList: ObjectList<GTRecipe>
    ): ParallelData {
        val count = remainingWants.size
        var activeBits = (1L shl count) - 1
        var activeCount = count

        var remaining = remaining
        while (remaining > 0 && activeCount > 0) {
            val perRecipe = remaining / activeCount
            if (perRecipe <= 0L) break

            var distributed = 0L
            var newActiveBits = 0L
            var newActiveCount = 0

            var bits = activeBits
            while (bits != 0L) {
                val i = bits.countTrailingZeroBits()
                bits = bits and (bits - 1)

                val idx = remainingIndices.getInt(i)
                val want = remainingWants.getLong(i)
                val give = min(want, perRecipe)
                parallels[idx] += give
                distributed += give
                remainingWants.set(i, want - give)

                if (want - give > 0) {
                    newActiveBits = newActiveBits or (1L shl i)
                    newActiveCount++
                }
            }

            activeBits = newActiveBits
            activeCount = newActiveCount
            remaining -= distributed
        }

        return ParallelData(recipeList, parallels)
    }

    private fun getParallelDataIndexArray(
        remaining: Long,
        parallels: LongArray,
        remainingWants: LongList,
        remainingIndices: IntList,
        recipeList: ObjectList<GTRecipe>
    ): ParallelData {
        var activeCount = remainingWants.size
        var remaining = remaining

        while (remaining > 0 && activeCount > 0) {
            val perRecipe = remaining / activeCount
            if (perRecipe <= 0L) break

            var distributed = 0L
            var writePos = 0

            for (readPos in 0 until activeCount) {
                val idx = remainingIndices.getInt(readPos)
                val want = remainingWants.getLong(readPos)
                val give = min(want, perRecipe)
                parallels[idx] += give
                distributed += give

                val newWant = want - give
                if (newWant > 0) {
                    remainingWants.set(writePos, newWant)
                    remainingIndices.set(writePos, idx)
                    writePos++
                }
            }

            activeCount = writePos
            remaining -= distributed
        }

        return ParallelData(recipeList, parallels)
    }

    // ===================================================
    // Recipe Copy Boost Fix ( > 10000 max chance)
    // ===================================================

    fun copyFixRecipe(origin: GTRecipe, modifier: ContentModifier, fixMultiplier: Int) =
        GTRecipe(
            origin.recipeType,
            origin.id,
            copyFixContents(origin.inputs, modifier, fixMultiplier),
            copyFixContents(origin.outputs, modifier, fixMultiplier),
            copyFixContents(origin.tickInputs, modifier, fixMultiplier),
            copyFixContents(origin.tickOutputs, modifier, fixMultiplier),
            Reference2ReferenceArrayMap(origin.inputChanceLogics),
            Reference2ReferenceArrayMap(origin.outputChanceLogics),
            Reference2ReferenceArrayMap(origin.tickInputChanceLogics),
            Reference2ReferenceArrayMap(origin.tickOutputChanceLogics),
            ObjectArrayList(origin.conditions),
            ObjectArrayList(origin.ingredientActions),
            origin.data,
            origin.duration,
            origin.isFuel
        )

    fun copyFixContents(
        contents: Map<RecipeCapability<*>, List<Content>>,
        modifier: ContentModifier,
        fixMultiplier: Int
    ): Map<RecipeCapability<*>, List<Content>> =
        Reference2ReferenceArrayMap<RecipeCapability<*>, List<Content>>().apply {
            contents.forEach { (cap, contentList) ->
                if (contentList.isNotEmpty()) {
                    put(cap, ObjectArrayList(contentList.map { content ->
                        copyFixBoost(content, cap, modifier, fixMultiplier)
                    }))
                }
            }
        }

    fun copyFixBoost(
        content: Content,
        capability: RecipeCapability<*>,
        modifier: ContentModifier,
        fixMultiplier: Int
    ): Content {
        val newContent = if (content.chance != 0) {
            capability.copyContent(content.content, modifier)
        } else {
            capability.copyContent(content.content)
        }

        return Content(
            newContent,
            content.chance,
            content.maxChance,
            content.tierChanceBoost / fixMultiplier,
            content.slotName,
            content.uiName
        )
    }
}