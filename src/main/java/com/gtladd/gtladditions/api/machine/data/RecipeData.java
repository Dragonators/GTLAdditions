package com.gtladd.gtladditions.api.machine.data;

/**
 * Data structure for recipe parallel calculation.
 * Used during the process of distributing remaining parallels among recipes.
 *
 * @param index         The index of the recipe in the recipe list
 * @param remainingWant The additional parallel count this recipe can still accept
 */
public record RecipeData(int index, long remainingWant) {}
