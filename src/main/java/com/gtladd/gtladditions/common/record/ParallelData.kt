package com.gtladd.gtladditions.common.record

import com.gregtechceu.gtceu.api.recipe.GTRecipe

/**
 * Data structure for parallel recipe processing.
 * Contains a list of recipes and their corresponding parallel counts.
 *
 * @param recipeList The list of recipes to be processed in parallel
 * @param parallels  Array of parallel counts for each recipe, matching the order in recipeList
 */
@Suppress("ArrayInDataClass")
@JvmRecord
data class ParallelData(@JvmField val recipeList: List<GTRecipe>, @JvmField val parallels: LongArray)
