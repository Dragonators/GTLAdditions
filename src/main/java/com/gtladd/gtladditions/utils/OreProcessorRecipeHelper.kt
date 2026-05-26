package com.gtladd.gtladditions.utils

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import kotlin.math.max

object OreProcessorRecipeHelper {
    fun copyForOreProcessor(
        recipe: GTRecipe,
        itemChanceBoost: Int = 1,
        outputMultiplier: Double = 1.0,
        inputFluidMultiplier: Double = 1.0,
        durationMultiplier: Double = 1.0
    ): GTRecipe {
        if (itemChanceBoost == 1 && outputMultiplier == 1.0 && inputFluidMultiplier == 1.0 && durationMultiplier == 1.0) {
            return recipe
        }

        val copy = GTRecipe(
            recipe.recipeType,
            recipe.id,
            copyContents(recipe.inputs, inputFluidMultiplier = inputFluidMultiplier),
            copyContents(recipe.outputs, itemChanceBoost = itemChanceBoost, outputMultiplier = outputMultiplier),
            recipe.tickInputs,
            copyContents(recipe.tickOutputs, itemChanceBoost = itemChanceBoost, outputMultiplier = outputMultiplier),
            recipe.inputChanceLogics,
            recipe.outputChanceLogics,
            recipe.tickInputChanceLogics,
            recipe.tickOutputChanceLogics,
            recipe.conditions,
            recipe.ingredientActions,
            recipe.data,
            max((recipe.duration * durationMultiplier).toInt(), 1),
            recipe.isFuel
        )
        IGTRecipe.of(copy).realParallels = IGTRecipe.of(recipe).realParallels
        copy.ocTier = recipe.ocTier
        return copy
    }

    private fun copyContents(
        contents: Map<RecipeCapability<*>, List<Content>>,
        itemChanceBoost: Int = 1,
        outputMultiplier: Double = 1.0,
        inputFluidMultiplier: Double = 1.0
    ): Map<RecipeCapability<*>, List<Content>> {
        if (contents.isEmpty()) return contents

        val copied = Reference2ReferenceOpenHashMap<RecipeCapability<*>, List<Content>>()
        for ((capability, contentList) in contents) {
            if (contentList.isEmpty()) continue
            val copyList = ObjectArrayList<Content>(contentList.size)
            for (content in contentList) {
                copyList.add(copyContent(content, capability, itemChanceBoost, outputMultiplier, inputFluidMultiplier))
            }
            copied[capability] = copyList
        }
        return copied
    }

    private fun copyContent(
        content: Content,
        capability: RecipeCapability<*>,
        itemChanceBoost: Int,
        outputMultiplier: Double,
        inputFluidMultiplier: Double
    ): Content {
        val modifier = when {
            outputMultiplier != 1.0 -> ContentModifier.multiplier(outputMultiplier)
            capability == FluidRecipeCapability.CAP && inputFluidMultiplier != 1.0 -> ContentModifier.multiplier(inputFluidMultiplier)
            else -> null
        }
        val copiedContent = if (modifier == null) {
            capability.copyContent(content.content)
        } else {
            capability.copyContent(content.content, modifier)
        }
        return Content(
            copiedContent,
            content.chance,
            content.maxChance,
            if (capability == ItemRecipeCapability.CAP) content.tierChanceBoost * itemChanceBoost else content.tierChanceBoost,
            content.slotName,
            content.uiName
        )
    }
}