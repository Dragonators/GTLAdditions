package com.gtladd.gtladditions.api.recipe

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.chance.boost.ChanceBoostFunction
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient
import com.gregtechceu.gtceu.utils.FluidStackHashStrategy
import com.gregtechceu.gtceu.utils.IngredientEquality
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy
import com.hepdd.gtmthings.common.block.machine.trait.CatalystFluidStackHandler
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import it.unimi.dsi.fastutil.objects.*
import net.minecraft.world.item.crafting.Ingredient
import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine
import org.gtlcore.gtlcore.api.machine.trait.MEPatternRecipeHandlePart
import org.gtlcore.gtlcore.api.machine.trait.RecipeHandlePart
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient
import org.gtlcore.gtlcore.utils.NumberUtils
import org.gtlcore.gtlcore.utils.NumberUtils.saturatedAdd
import org.gtlcore.gtlcore.utils.datastructure.Int128
import java.math.BigInteger
import java.util.function.Predicate

object ChanceParallelLogic {

    private class ContentAmountPair(
        val chance: Int,
        val maxChance: Int
    ) {
        var amount: Long = 0
            private set

        fun addTo(amount: Long) {
            this.amount += amount
        }
    }

    fun getMaxParallel(
        holder: ParallelMachine,
        recipe: GTRecipe,
        parallelAmount: Long,
        chanceCaches: Map<RecipeCapability<*>, Object2IntMap<*>>,
        boostFunction: ChanceBoostFunction,
        baseTier: Int,
        machineTier: Int
    ): Long {
        var parallel = parallelAmount
        for (cap in recipe.inputs.keys) {
            if (cap == ItemRecipeCapability.CAP) {
                parallel = minOf(
                    parallel,
                    getInputItemParallel(
                        holder,
                        recipe,
                        parallel,
                        chanceCaches[ItemRecipeCapability.CAP],
                        boostFunction,
                        baseTier,
                        machineTier
                    )
                )
                if (parallel == 0L) break
            } else if (cap == FluidRecipeCapability.CAP) {
                parallel = minOf(
                    parallel,
                    getInputFluidParallel(
                        holder,
                        recipe,
                        parallel,
                        chanceCaches[FluidRecipeCapability.CAP],
                        boostFunction,
                        baseTier,
                        machineTier
                    )
                )
                if (parallel == 0L) break
            }
        }
        return parallel
    }

    @Suppress("UNCHECKED_CAST")
    fun getInputItemParallel(
        holder: ParallelMachine,
        recipe: GTRecipe,
        parallelAmount: Long,
        cache: Object2IntMap<*>?,
        boostFunction: ChanceBoostFunction,
        baseTier: Int,
        machineTier: Int
    ): Long {
        if (parallelAmount <= 1) return parallelAmount
        if (holder !is IRecipeCapabilityMachine) return 1
        if (holder.emptyRecipeHandlePart()) return 0

        val guaranteedMap = Object2LongOpenCustomHashMap(
            IngredientEquality.IngredientHashStrategy.INSTANCE
        )
        val chanceMap = Object2ReferenceOpenCustomHashMap<Ingredient, ContentAmountPair>(
            IngredientEquality.IngredientHashStrategy.INSTANCE
        )
        val confirmMEStock = holder.needConfirmMEStock()

        for (content in recipe.getInputContents(ItemRecipeCapability.CAP)) {
            if (content.chance <= 0) continue
            val ingredient = ItemRecipeCapability.CAP.of(content.content)
            val ingredientCount = when (ingredient) {
                is LongIngredient -> ingredient.actualAmount
                is SizedIngredient -> ingredient.amount.toLong()
                else -> 1L
            }

            if (content.chance >= content.maxChance) {
                guaranteedMap.addTo(ingredient, ingredientCount)
            } else {
                val boostedChance = boostFunction.getBoostedChance(content, baseTier, machineTier)
                if (boostedChance >= content.maxChance) {
                    guaranteedMap.addTo(ingredient, ingredientCount)
                } else {
                    chanceMap.computeIfAbsent(ingredient) {
                        ContentAmountPair(boostedChance, content.maxChance)
                    }.addTo(ingredientCount)
                }
            }
        }

        if (guaranteedMap.isEmpty() && chanceMap.isEmpty()) return parallelAmount

        val ingredientStacks = Object2LongOpenCustomHashMap(
            ItemStackHashStrategy.comparingAllButCount()
        )

        val handle = holder.getActiveRecipeHandle(recipe)
        when (handle) {
            is MEPatternRecipeHandlePart -> {
                for (entry in Object2LongMaps.fastIterable(handle.getMEContent(ItemRecipeCapability.CAP, recipe))) {
                    ingredientStacks.mergeLong(entry.key, entry.longValue, NumberUtils::saturatedAdd)
                }
            }

            is RecipeHandlePart -> {
                for (entry in Object2LongMaps.fastIterable(handle.getSelfContent(ItemRecipeCapability.CAP, confirmMEStock))) {
                    ingredientStacks.mergeLong(entry.key, entry.longValue, NumberUtils::saturatedAdd)
                }
            }

            else -> {
                val sharedRecipeHandlePart = holder.sharedRecipeHandlePart
                if (sharedRecipeHandlePart != null) {
                    for (entry in Object2LongMaps.fastIterable(
                        sharedRecipeHandlePart.getSelfContent(ItemRecipeCapability.CAP, confirmMEStock)
                    )) {
                        ingredientStacks.mergeLong(entry.key, entry.longValue, NumberUtils::saturatedAdd)
                    }
                }
            }
        }

        return calculateWithChance(
            parallelAmount,
            guaranteedMap,
            chanceMap,
            ingredientStacks,
            cache as? Object2IntMap<Ingredient>
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun getInputFluidParallel(
        holder: ParallelMachine,
        recipe: GTRecipe,
        parallelAmount: Long,
        cache: Object2IntMap<*>?,
        boostFunction: ChanceBoostFunction,
        baseTier: Int,
        machineTier: Int
    ): Long {
        if (parallelAmount <= 1) return parallelAmount
        if (holder !is IRecipeCapabilityMachine) return 1
        if (holder.emptyRecipeHandlePart()) return 0

        val guaranteedFluidMap = Object2LongOpenHashMap<FluidIngredient>()
        val chanceFluidMap = Object2ReferenceOpenHashMap<FluidIngredient, ContentAmountPair>()
        val confirmMEStock = holder.needConfirmMEStock()

        for (content in recipe.getInputContents(FluidRecipeCapability.CAP)) {
            if (content.chance <= 0) continue
            val fluidInput = FluidRecipeCapability.CAP.of(content.content)

            if (content.chance >= content.maxChance) {
                guaranteedFluidMap.addTo(fluidInput, fluidInput.amount)
            } else {
                val boostedChance = boostFunction.getBoostedChance(content, baseTier, machineTier)
                if (boostedChance >= content.maxChance) {
                    guaranteedFluidMap.addTo(fluidInput, fluidInput.amount)
                } else {
                    chanceFluidMap.computeIfAbsent(fluidInput) {
                        ContentAmountPair(boostedChance, content.maxChance)
                    }.addTo(fluidInput.amount)
                }
            }
        }

        if (guaranteedFluidMap.isEmpty() && chanceFluidMap.isEmpty()) return parallelAmount

        val ingredientStacks = Object2LongOpenCustomHashMap(FluidStackHashStrategy.comparingAllButAmount())

        val handle = holder.getActiveRecipeHandle(recipe)
        when (handle) {
            is MEPatternRecipeHandlePart -> {
                for (entry in Object2LongMaps.fastIterable(handle.getMEContent(FluidRecipeCapability.CAP, recipe))) {
                    ingredientStacks.mergeLong(entry.key, entry.longValue, NumberUtils::saturatedAdd)
                }
            }

            is RecipeHandlePart -> {
                val content = if (holder.isDistinct) {
                    handle.getSelfContent(FluidRecipeCapability.CAP, confirmMEStock)
                } else {
                    handle.getContentWithShared(FluidRecipeCapability.CAP, confirmMEStock)
                }
                for (entry in Object2LongMaps.fastIterable(content)) {
                    ingredientStacks.mergeLong(entry.key, entry.longValue, NumberUtils::saturatedAdd)
                }
            }

            else -> {
                val sharedRecipeHandlePart = holder.sharedRecipeHandlePart
                if (sharedRecipeHandlePart != null) {
                    for (handler in sharedRecipeHandlePart.getCapability(FluidRecipeCapability.CAP)) {
                        if (handler is CatalystFluidStackHandler) continue
                        for (obj in handler.contents) {
                            if (obj is FluidStack) {
                                ingredientStacks.mergeLong(obj, obj.amount, NumberUtils::saturatedAdd)
                            }
                        }
                    }
                }
            }
        }

        return calculateWithChance(
            parallelAmount,
            guaranteedFluidMap,
            chanceFluidMap,
            ingredientStacks,
            cache as? Object2IntMap<FluidIngredient>
        )
    }

    private fun <I, S> calculateWithChance(
        parallelLimit: Long,
        guaranteedMap: Object2LongMap<I>,
        chanceMap: Object2ReferenceMap<I, ContentAmountPair>,
        ingredientStacks: Object2LongMap<S>,
        cache: Object2IntMap<I>?
    ): Long where I : Predicate<S> {
        if (ingredientStacks.isEmpty()) return 0

        var parallel = parallelLimit

        // First, calculate parallel limit for guaranteed inputs (traditional method)
        for (entry in Object2LongMaps.fastIterable(guaranteedMap)) {
            val needed = entry.longValue
            val available = findAvailable(entry.key, ingredientStacks)
            if (available < needed) return 0

            parallel = minOf(parallel, available / needed)
        }

        // For chanced inputs, calculate maximum parallel based on roll simulation
        for (entry in Object2ReferenceMaps.fastIterable(chanceMap)) {
            val chanceNeeded = entry.value
            val ingredient = entry.key

            val available = findAvailable(ingredient, ingredientStacks)
            if (available < chanceNeeded.amount) return 0

            // Get or generate cached chance value
            val cached = getGuaranteedCachedChanceValue(ingredient, chanceNeeded.maxChance, cache)

            // parallel <= ((maxOutputs + 1) * maxChance - 1 - cached) / chance
            // This ensures: floor((parallel * chance + cached) / maxChance) <= maxOutputs
            val maxOutputs = available / chanceNeeded.amount
            var maxParallelForThis = (Int128(maxOutputs).add(1L)
                .multiply(Int128(chanceNeeded.maxChance.toLong()))
                .subtract(Int128((1 + cached).toLong()))
                .divide(chanceNeeded.chance.toLong()) as Number).toLong()

            // Ensure non-negative
            if (maxParallelForThis < 0) maxParallelForThis = 0

            parallel = minOf(parallel, maxParallelForThis)
        }

        return parallel
    }

    private fun <I, S> findAvailable(
        ingredient: I,
        stacks: Object2LongMap<S>
    ): Long where I : Predicate<S> {
        val it = Object2LongMaps.fastIterator(stacks)
        while (it.hasNext()) {
            val input = it.next()
            if (ingredient.test(input.key)) {
                val value = input.longValue
                it.remove()
                return value
            }
        }
        return 0
    }

    private fun <I> getGuaranteedCachedChanceValue(
        content: I,
        maxChance: Int,
        cache: Object2IntMap<I>?
    ): Int {
        if (cache == null) {
            return GTValues.RNG.nextInt(maxChance)
        }

        return if (cache.containsKey(content)) {
            cache.getInt(content)
        } else {
            // Generate random and save to cache for consistency
            val randomValue = GTValues.RNG.nextInt(maxChance)
            cache.put(content, randomValue)
            randomValue
        }
    }
}
