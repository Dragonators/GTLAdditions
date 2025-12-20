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
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntList
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.longs.LongList
import it.unimi.dsi.fastutil.longs.LongLongPair
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
import kotlin.math.roundToInt

object RecipeCalculationHelper {

    // ===================================================
    // Process Recipe
    // ===================================================

    inline fun multipleRecipe(
        recipe: GTRecipe,
        parallel: Long,
        crossinline copyRecipe: (GTRecipe) -> GTRecipe
    ): GTRecipe {
        val processed = if (parallel > 1) copyRecipe(recipe) else recipe
        IGTRecipe.of(processed).realParallels = parallel
        return processed
    }

    fun multipleRecipe(
        recipe: GTRecipe,
        parallel: Long,
    ): GTRecipe {
        return multipleRecipe(recipe, parallel) { recipe -> recipe.copy(ContentModifier.multiplier(parallel.toDouble()), false) }
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

        for (i in parallelData.originRecipeList.indices) {
            val r = parallelData.originRecipeList[i]
            val p = parallelData.parallels[i]
            if (parallelData.shouldProcess) {
                val processedRecipe = IParallelLogic.getRecipeOutputChance(machine, multipleRecipe(r, p))

                if (RecipeRunnerHelper.matchRecipeInput(
                        machine, processedRecipe
                    ) && RecipeRunnerHelper.handleRecipeInput(
                        machine, processedRecipe
                    )
                ) {
                    totalEu += getTotalRecipeEu(r) * p * euMultiplier
                    collectOutputs(processedRecipe, itemOutputs, fluidOutputs)
                }

                if (shouldBreak(totalEu)) break
            } else {
                totalEu += getTotalRecipeEu(r) * p * euMultiplier
                collectOutputs(parallelData.processedRecipeList!![i], itemOutputs, fluidOutputs)
            }
        }

        return Triple(itemOutputs, fluidOutputs, totalEu)
    }

    inline fun processParallelDataWireless(
        parallelData: ParallelData,
        machine: IRecipeLogicMachine,
        maxTotalEu: BigInteger,
        euMultiplier: Double,
        getRecipeEut: (GTRecipe) -> Long,
        isEnergyConsumer: Boolean = true
    ): Triple<ObjectArrayList<Content>, ObjectArrayList<Content>, BigInteger> {
        val itemOutputs = ObjectArrayList<Content>()
        val fluidOutputs = ObjectArrayList<Content>()
        var accumulatedEu = BigInteger.ZERO

        for (i in parallelData.originRecipeList.indices) {
            val r = parallelData.originRecipeList[i]
            val p = parallelData.parallels[i]

            var parallelEUt = BigInteger.valueOf(getRecipeEut(r))
            if (p > 1) parallelEUt = parallelEUt.multiply(BigInteger.valueOf(p))

            val tempAccumulatedEu = accumulatedEu.add(
                BigDecimal.valueOf(r.duration * euMultiplier)
                    .multiply(BigDecimal(parallelEUt)).toBigInteger()
            )

            if (parallelData.shouldProcess) {
                if (tempAccumulatedEu > maxTotalEu) {
                    if (accumulatedEu.signum() == 0) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_EU_IN)
                    break
                }

                val paralleledRecipe = IParallelLogic.getRecipeOutputChance(machine, multipleRecipe(r, p))

                if (RecipeRunnerHelper.matchRecipeInput(
                        machine, paralleledRecipe
                    ) && RecipeRunnerHelper.handleRecipeInput(
                        machine, paralleledRecipe
                    )
                ) {
                    accumulatedEu = tempAccumulatedEu
                    collectOutputs(paralleledRecipe, itemOutputs, fluidOutputs)
                }
            } else {
                accumulatedEu = tempAccumulatedEu
                collectOutputs(parallelData.processedRecipeList!![i], itemOutputs, fluidOutputs)
            }
        }

        val totalEu = if (isEnergyConsumer) accumulatedEu else accumulatedEu.negate()
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
        recipe.duration = maxOf(d, minDuration.toDouble()).roundToInt()
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

    inline fun calculateParallelsWithFairAllocation(
        recipes: Collection<GTRecipe>,
        totalParallel: Long,
        crossinline getParallelForRecipe: (GTRecipe) -> Long,
        crossinline consumeParallel: (GTRecipe) -> Boolean = { true },
    ): ParallelData? {
        val length = recipes.size
        if (length == 0) return null

        var remaining = totalParallel
        val parallels = LongArray(length)
        var index = 0
        val recipeList = ObjectArrayList<GTRecipe>(length)
        val remainingWants = LongArrayList(length)
        val remainingIndices = IntArrayList(length)

        for (r in recipes) {
            val p = getParallelForRecipe(r)
            if (p <= 0) continue
            recipeList.add(r)

            if (!consumeParallel(r)) {
                parallels[index] = p
                index++
                continue
            }

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

        return getFinalParallelData(remaining, parallels, remainingWants, remainingIndices, recipeList)
    }

    inline fun calculateParallelsWithGreedyAllocation(
        recipes: Collection<GTRecipe>,
        totalParallel: Long,
        machine: IRecipeLogicMachine,
        crossinline modifyRecipe: (GTRecipe) -> GTRecipe = { it },
        crossinline createParalleledRecipe: (GTRecipe, Long) -> GTRecipe = { r, p -> multipleRecipe(r, p) },
        crossinline getParallelAndConsumption: (GTRecipe, Long) -> LongLongPair
    ): ParallelData? {
        var remain = totalParallel

        val recipeList = ObjectArrayList<GTRecipe>()
        val processedRecipeList = ObjectArrayList<GTRecipe>()
        val parallelsList = LongArrayList()

        for (match in recipes) {
            if (remain <= 0) break
            val modified = modifyRecipe(match)
            val pair = getParallelAndConsumption(modified, remain)
            val p = pair.firstLong()
            if (p <= 0) continue

            val paralleledRecipe = IParallelLogic.getRecipeOutputChance(
                machine,
                createParalleledRecipe(modified, p)
            )
            if (RecipeRunnerHelper.handleRecipeInput(machine, paralleledRecipe)) {
                remain -= pair.secondLong()
                recipeList.add(match)
                processedRecipeList.add(paralleledRecipe)
                parallelsList.add(p)
            }
        }

        return if (recipeList.isEmpty()) null
        else ParallelData(recipeList, parallelsList.toLongArray(), false, processedRecipeList)
    }

    inline fun calculateParallelsWithProcessing(
        recipes: Collection<GTRecipe>,
        machine: IRecipeLogicMachine,
        crossinline getParallelLimitForRecipe: (GTRecipe) -> Long,
        crossinline getMaxParallelForRecipe: (GTRecipe, Long) -> Long,
        crossinline modifyRecipe: (GTRecipe) -> GTRecipe = { it },
        crossinline createParalleledRecipe: (GTRecipe, Long) -> GTRecipe = { r, p -> multipleRecipe(r, p) },
        useModifiedRecipe: Boolean = false,
        preProcessRecipes: Boolean = true
    ): ParallelData? {
        val length = recipes.size
        if (length == 0) return null

        val recipeList = ObjectArrayList<GTRecipe>(length)
        val processedRecipeList = if (preProcessRecipes) ObjectArrayList<GTRecipe>(length) else null
        val parallelsList = LongArrayList(length)

        for (recipe in recipes) {
            val modified = modifyRecipe(recipe)
            val limit = getParallelLimitForRecipe(modified)
            val parallel = getMaxParallelForRecipe(modified, limit)

            if (parallel > 0) {
                if (preProcessRecipes) {
                    val paralleledRecipe = IParallelLogic.getRecipeOutputChance(
                        machine,
                        createParalleledRecipe(modified, parallel)
                    )
                    if (RecipeRunnerHelper.handleRecipeInput(machine, paralleledRecipe)) {
                        recipeList.add(if (useModifiedRecipe) modified else recipe)
                        processedRecipeList!!.add(paralleledRecipe)
                        parallelsList.add(parallel)
                    }
                } else {
                    recipeList.add(if (useModifiedRecipe) modified else recipe)
                    parallelsList.add(parallel)
                }
            }
        }

        return if (recipeList.isEmpty()) null
        else ParallelData(recipeList, parallelsList.toLongArray(), !preProcessRecipes, processedRecipeList)
    }

    fun getFinalParallelData(
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