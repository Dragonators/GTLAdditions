package com.gtladd.gtladditions.api.machine.data;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import java.util.List;

/**
 * Data structure for parallel recipe processing.
 * Contains a list of recipes and their corresponding parallel counts.
 *
 * @param recipeList The list of recipes to be processed in parallel
 * @param parallels  Array of parallel counts for each recipe, matching the order in recipeList
 */
public record ParallelData(List<GTRecipe> recipeList, long[] parallels) {}
