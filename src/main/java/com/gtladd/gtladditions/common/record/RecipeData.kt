package com.gtladd.gtladditions.common.record

/**
 * Data structure for recipe parallel calculation.
 * Used during the process of distributing remaining parallels among recipes.
 *
 * @param index         The index of the recipe in the recipe list
 * @param remainingWant The additional parallel count this recipe can still accept
 */
@JvmRecord
data class RecipeData(@JvmField val index: Int, @JvmField val remainingWant: Long)
