@file:JvmName("MEBufferPatternHelperExtensions")

package com.gtladd.gtladditions.integration.ae2

import appeng.api.crafting.IPatternDetails
import appeng.api.crafting.PatternDetailsHelper.decodePattern
import appeng.api.crafting.PatternDetailsHelper.encodeProcessingPattern
import appeng.api.stacks.AEFluidKey
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.AEKey
import appeng.api.stacks.GenericStack
import appeng.crafting.pattern.AEProcessingPattern
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient
import com.gtladd.gtladditions.common.machine.multiblock.part.MESuperPatternBufferPartMachine
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient
import org.gtlcore.gtlcore.integration.ae2.handler.MEBufferPatternHelper
import org.gtlcore.gtlcore.utils.NumberUtils
import org.gtlcore.gtlcore.utils.Registries
import java.util.function.Consumer

fun MEBufferPatternHelper.processForgeOfTheAntichristPattern(
    stack: ItemStack,
    circuitCacheSetter: Consumer<Int>,
    level: Level?,
    keepByProduct: Boolean,
    multiplier: Int
): IPatternDetails? {
    val pattern = processPatternWithCircuit(stack, circuitCacheSetter, level, keepByProduct)
    return rewriteForgeOfTheAntichristPattern(pattern, level, multiplier)
}

private fun rewriteForgeOfTheAntichristPattern(
    pattern: IPatternDetails?,
    level: Level?,
    multiplier: Int
): IPatternDetails? {
    val processingPattern = pattern as? AEProcessingPattern ?: return pattern
    val clampedMultiplier = multiplier.coerceIn(
        MESuperPatternBufferPartMachine.MIN_MULTIPLIER,
        MESuperPatternBufferPartMachine.MAX_MULTIPLIER
    )
    if (clampedMultiplier <= 1) return processingPattern

    val sparseOutputs = createForgeOutputs(processingPattern.sparseOutputs, clampedMultiplier)
    val extraInput = tryGetSpecialInput(processingPattern.sparseInputs)
    val sparseInputs = if (extraInput != null) {
        appendMatchingSpecialInput(
            processingPattern.sparseInputs,
            extraInput,
            clampedMultiplier
        )
    } else {
        createForgeInputs(processingPattern.sparseInputs, clampedMultiplier)
    }

    return decodePattern(encodeProcessingPattern(sparseInputs, sparseOutputs), level) ?: processingPattern
}

private fun createForgeInputs(stacks: Array<GenericStack?>, multiplier: Int): Array<GenericStack?> {
    val filteredInputs = mutableListOf<GenericStack?>()
    for (input in stacks) {
        if (input == null) continue
        filteredInputs.add(
            if (isRecipeCycleContainerStack(input)) {
                GenericStack(input.what(), NumberUtils.saturatedMultiply(input.amount(), multiplier.toLong()))
            } else {
                input
            }
        )
    }
    return filteredInputs.toTypedArray()
}

private fun createForgeOutputs(stacks: Array<GenericStack?>, multiplier: Int): Array<GenericStack?> {
    val filteredOutputs = mutableListOf<GenericStack?>()
    for (output in stacks) {
        if (output == null) continue
        filteredOutputs.add(
            if (isRecipeCycleContainerStack(output)) {
                output
            } else {
                GenericStack(output.what(), NumberUtils.saturatedMultiply(output.amount(), multiplier.toLong()))
            }
        )
    }
    return filteredOutputs.toTypedArray()
}

private fun tryGetSpecialInput(stacks: Array<GenericStack?>): GenericStack? {
    val originalInputKeys = stacks.mapNotNullTo(mutableSetOf()) { stack -> stack?.what() }
    return specialInputMap[originalInputKeys]
}

private fun appendMatchingSpecialInput(
    stacks: Array<GenericStack?>,
    extraInput: GenericStack,
    multiplier: Int
): Array<GenericStack?> {
    val extraAmount = NumberUtils.saturatedMultiply(extraInput.amount(), (multiplier - 1).toLong())
    if (extraAmount <= 0) return stacks

    val filteredInputs = mutableListOf<GenericStack?>()
    for (input in stacks) {
        if (input != null) {
            filteredInputs.add(input)
        }
    }
    if (filteredInputs.size >= AEProcessingPattern.MAX_INPUT_SLOTS) return stacks
    filteredInputs.add(GenericStack(extraInput.what, extraAmount))
    return filteredInputs.toTypedArray()
}

private fun isRecipeCycleContainerStack(stack: GenericStack): Boolean {
    val key = stack.what()
    return key is AEItemKey && RecipeCalculationHelper.isRecipeCycleContainerItem(key.item)
}

// ========================================
// GTRecipe
// ========================================

private val specialInputMap: Object2ObjectMap<Set<AEKey>, GenericStack> by lazy {
    val rules = Object2ObjectOpenHashMap<Set<AEKey>, GenericStack>()
    for ((recipeId, extraInputContent) in RecipeCalculationHelper.FORGE_OF_THE_ANTICHRIST_SPECIAL_INPUT_RULES) {
        val recipe = Registries.getRecipeManager().byKey(recipeId).orElse(null) as? GTRecipe ?: continue
        val extraInput = contentToGenericStack(extraInputContent) ?: continue
        rules[getRecipeInputKeySet(recipe)] = extraInput
    }
    return@lazy rules
}

private fun getRecipeInputKeySet(recipe: GTRecipe): Set<AEKey> = buildSet {
    recipe.inputs[ItemRecipeCapability.CAP]?.forEach { content ->
        val ingredient = ItemRecipeCapability.CAP.of(content.content)
        ingredient.items.forEach { stack ->
            if (!stack.isEmpty) AEItemKey.of(stack)?.let(::add)
        }
    }

    recipe.inputs[FluidRecipeCapability.CAP]?.forEach { content ->
        val ingredient = FluidRecipeCapability.CAP.of(content.content)
        ingredient.stacks.forEach { stack ->
            if (stack != null && !stack.isEmpty) {
                add(if (stack.hasTag()) AEFluidKey.of(stack.fluid, stack.tag) else AEFluidKey.of(stack.fluid))
            }
        }
    }
}

private fun contentToGenericStack(content: Content): GenericStack? {
    val ingredient = ItemRecipeCapability.CAP.of(content.content)
    val stack = ingredient.items.firstOrNull { !it.isEmpty } ?: return null
    val amount = when (ingredient) {
        is LongIngredient -> ingredient.actualAmount
        is SizedIngredient -> ingredient.amount.toLong()
        else -> stack.count.toLong()
    }
    val key = AEItemKey.of(stack) ?: return null
    return GenericStack(key, amount)
}