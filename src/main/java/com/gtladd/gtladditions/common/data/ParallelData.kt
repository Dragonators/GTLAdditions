package com.gtladd.gtladditions.common.data

import com.gregtechceu.gtceu.api.recipe.GTRecipe

/**
 * Data structure for parallel recipe processing.
 * Contains a list of recipes and their corresponding parallel counts.
 *
 * @param originRecipeList The list of recipes to be processed in parallel
 * @param parallels  Array of parallel counts for each recipe, matching the order in recipeList
 */
@Suppress("ArrayInDataClass")
data class ParallelData(
    val originRecipeList: List<GTRecipe>,
    val parallels: LongArray,
    val shouldProcess: Boolean = true,
    val processedRecipeList: List<GTRecipe>? = null
)
