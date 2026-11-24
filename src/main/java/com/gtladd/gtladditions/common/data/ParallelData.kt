package com.gtladd.gtladditions.common.data

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gtladd.gtladditions.integration.gtlcore.monitor.BatchMonitorInputs
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import net.minecraft.world.item.ItemStack

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
) {
    val monitorInputs = BatchMonitorInputs()

    fun addInputs(inputs: Map<RecipeCapability<*>, List<Content>>) = monitorInputs.addInputs(inputs)

    fun addInput(item: ItemStack) = monitorInputs.addInput(item)

    fun addInput(fluid: FluidStack) = monitorInputs.addInput(fluid)
}