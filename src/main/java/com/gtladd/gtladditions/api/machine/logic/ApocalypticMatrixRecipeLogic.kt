package com.gtladd.gtladditions.api.machine.logic

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler
import com.gtladd.gtladditions.api.recipe.ChanceParallelLogic
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.common.machine.multiblock.controller.ApocalypticWarpQuantumMatrix
import com.gtladd.gtladditions.utils.recipe.RecipeCalculationHelper
import com.gtladd.gtladditions.utils.wideBuffer.longValue
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
import net.minecraft.nbt.CompoundTag
import org.gtlcore.gtlcore.api.recipe.*
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.matchRecipe
import org.gtlcore.gtlcore.utils.datastructure.Int128

class ApocalypticMatrixRecipeLogic(parallel: ApocalypticWarpQuantumMatrix) : GTLAddMultipleTypeWirelessRecipesLogic(parallel) {
    init {
        this.setReduction(0.2, 1.0)
    }

    override fun getMachine(): ApocalypticWarpQuantumMatrix = super.getMachine() as ApocalypticWarpQuantumMatrix

    override fun getEuMultiplier(): Double =
        (if (getMachine().quantumWarpTrait.hasInfiniteParallel()) 4 else 1) * super.getEuMultiplier()

    override fun getMultipleThreads(): Int = Ints.saturatedCast(
        getMachine().quantumWarpTrait.finiteThreads()
    )

    override fun checkRecipe(recipe: GTRecipe): Boolean =
        matchRecipe(machine, recipe) && (recipeCheck == null || recipeCheck.test(recipe, machine))

    override fun getMaxParallel(recipe: GTRecipe, limit: Long): Long = ChanceParallelLogic.getMaxParallel(
        getMachine(),
        recipe,
        limit,
        chanceCaches,
        recipe.type.chanceFunction,
        IGTRecipe.of(recipe).euTier,
        getMachine().tier
    )

    override fun calculateParallels(): ParallelData? {
        val matrix = getMachine()
        val warp = matrix.quantumWarpTrait
        val distorter = matrix.resolveTSD()?.takeIf { it.isReady() }

        val infinite = warp.hasInfiniteParallel(distorter != null)
        val folded = infinite && warp.enableDimensionFold
        val coefficient = warp.auxiliaryCoefficient()

        val remain = Int128(matrix.maxParallel.toLong()).multiply(warp.finiteThreads())
        val consumedParallels = Int128.ZERO()
        val recipeList = ObjectArrayList<GTRecipe>()
        val processedRecipeList = ObjectArrayList<GTRecipe>()
        val parallelsList = LongArrayList()

        for (match in lookupRecipeIterator()) {
            if (!infinite && !remain.isPositive) break
            val modified = modifyChance(match, chanceDivisor(warp.recipeSecs(match.id)))
            val p = getMaxParallel(modified, if (infinite) Long.MAX_VALUE else remain.longValue())
            if (p <= 0) continue

            val paralleledRecipe = IParallelLogic.getRecipeOutputChance(
                machine,
                RecipeCalculationHelper.multipleRecipe(modified, p) { recipe ->
                    RecipeCalculationHelper.copyFixRecipe(recipe, IAdvancedContentModifier.preciseMultiplier(p))
                }
            )

            if (RecipeRunnerHelper.handleRecipeInput(matrix, paralleledRecipe)) {
                consumedParallels.add(p)
                if (!infinite) remain.add(-p)
                recipeList.add(match)
                processedRecipeList.add(paralleledRecipe)
                parallelsList.add(p)
            }
        }

        if (recipeList.isEmpty) return null

        val outputMultiplier = distorter?.tryConsumeAndGetOutputMultiplier(consumedParallels, coefficient, folded) ?: 1.0

        if (outputMultiplier > 1.0) {
            applyOutputMultiplier(processedRecipeList, outputMultiplier)
        }

        return ParallelData(recipeList, parallelsList.toLongArray(), false, processedRecipeList)
    }

    override fun buildFinalWirelessRecipe(
        parallelData: ParallelData,
        wirelessTrait: IWirelessNetworkEnergyHandler
    ): WirelessGTRecipe? {
        val recipe = super.buildFinalWirelessRecipe(parallelData, wirelessTrait) ?: return null
        recipe.data.put(
            "quantumWarpRecipes",
            CompoundTag().apply {
                parallelData.originRecipeList.forEach { putBoolean(it.id.toString(), true) }
            }
        )
        return recipe
    }

    companion object {
        const val RECIPE_SECONDS_MAX = 3600L
        private const val INPUT_CHANCE_RATIO = 10.0

        fun chanceDivisor(seconds: Long): Double =
            INPUT_CHANCE_RATIO + INPUT_CHANCE_RATIO * seconds.coerceIn(0, RECIPE_SECONDS_MAX) / RECIPE_SECONDS_MAX

        private fun applyOutputMultiplier(recipes: Iterable<GTRecipe>, multiplier: Double) {
            val modifier = ContentModifier.multiplier(multiplier)

            for (recipe in recipes) {
                for ((capability, contents) in recipe.outputs) {
                    for (i in contents.indices) {
                        contents[i] = RecipeCalculationHelper.copyFixBoost(contents[i], capability, modifier)
                    }
                }
            }
        }

        private fun modifyContents(
            before: Map<RecipeCapability<*>, MutableList<Content>>,
            isInput: Boolean,
            maxChanceBonus: Double
        ): Reference2ReferenceOpenHashMap<RecipeCapability<*>, MutableList<Content>> {
            val after = Reference2ReferenceOpenHashMap<RecipeCapability<*>, MutableList<Content>>()

            for ((cap, value) in before) {
                val contentList = after.computeIfAbsent(cap) { ObjectArrayList() }

                for (cont in value) {
                    if (cont.chance >= cont.maxChance || isInput && cont.chance == 0) {
                        contentList.add(cont)
                    } else if (cont.chance != 0) {
                        val copy = RecipeCalculationHelper.copyFixBoost(cont, cap, null)
                        if (isInput) {
                            copy.maxChance = (cont.maxChance * maxChanceBonus).toInt()
                        } else {
                            copy.chance = cont.maxChance
                        }

                        contentList.add(copy)
                    }
                }

                if (contentList.isEmpty()) after.remove(cap)
            }

            return after
        }

        private fun modifyChance(recipe: GTRecipe, maxChanceBonus: Double = INPUT_CHANCE_RATIO): GTRecipe {
            val copy = GTRecipe(
                recipe.recipeType,
                recipe.id,
                modifyContents(recipe.inputs, true, maxChanceBonus),
                modifyContents(recipe.outputs, false, INPUT_CHANCE_RATIO),
                recipe.tickInputs,
                recipe.tickOutputs,
                recipe.inputChanceLogics,
                recipe.outputChanceLogics,
                recipe.tickInputChanceLogics,
                recipe.tickOutputChanceLogics,
                recipe.conditions,
                recipe.ingredientActions,
                recipe.data,
                recipe.duration,
                recipe.isFuel
            )
            RecipeExtensionCopier.copy(recipe, copy)
            return copy
        }
    }
}
