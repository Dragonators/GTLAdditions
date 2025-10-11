package com.gtladd.gtladditions.api.recipe;

import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine;
import org.gtlcore.gtlcore.api.machine.trait.MEPatternRecipeHandlePart;
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.chance.boost.ChanceBoostFunction;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.utils.IngredientEquality;
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import com.hepdd.gtmthings.common.block.machine.trait.CatalystFluidStackHandler;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;

public class ChanceParallelLogic {

    private static class ContentAmountPair {

        long amount = 0;
        final int chance;
        final int maxChance;

        private ContentAmountPair(int chance, int maxChance) {
            this.chance = chance;
            this.maxChance = maxChance;
        }

        public void addTo(long amount) {
            this.amount += amount;
        }
    }

    public static long getMaxParallel(
                                      IRecipeCapabilityHolder holder,
                                      GTRecipe recipe,
                                      long parallelAmount,
                                      Map<RecipeCapability<?>, Object2IntMap<?>> chanceCaches,
                                      @NotNull ChanceBoostFunction boostFunction,
                                      int baseTier,
                                      int machineTier) {
        for (var cap : recipe.inputs.keySet()) {
            if (cap == ItemRecipeCapability.CAP) {
                parallelAmount = Math.min(parallelAmount, getInputItemParallel(holder, recipe, parallelAmount, chanceCaches.get(ItemRecipeCapability.CAP), boostFunction, baseTier, machineTier));
                if (parallelAmount == 0L) break;
            } else if (cap == FluidRecipeCapability.CAP) {
                parallelAmount = Math.min(parallelAmount, getInputFluidParallel(holder, recipe, parallelAmount, chanceCaches.get(FluidRecipeCapability.CAP), boostFunction, baseTier, machineTier));
                if (parallelAmount == 0L) break;
            }
        }
        return parallelAmount;
    }

    @SuppressWarnings("unchecked")
    static long getInputItemParallel(
                                     IRecipeCapabilityHolder holder,
                                     GTRecipe recipe,
                                     long parallelAmount,
                                     Object2IntMap<?> cache,
                                     @NotNull ChanceBoostFunction boostFunction,
                                     int baseTier,
                                     int machineTier) {
        if (parallelAmount <= 1) return parallelAmount;
        if (!(holder instanceof IRecipeCapabilityMachine machine)) return 1;
        if (machine.getRecipeHandleParts().isEmpty() && machine.getMEPatternRecipeHandleParts().isEmpty()) return 0;

        Object2LongOpenCustomHashMap<Ingredient> guaranteedMap = new Object2LongOpenCustomHashMap<>(IngredientEquality.IngredientHashStrategy.INSTANCE);
        Object2ReferenceOpenCustomHashMap<Ingredient, ContentAmountPair> chanceMap = new Object2ReferenceOpenCustomHashMap<>(IngredientEquality.IngredientHashStrategy.INSTANCE);

        for (Content content : recipe.getInputContents(ItemRecipeCapability.CAP)) {
            if (content.chance <= 0) continue;
            Ingredient ingredient = ItemRecipeCapability.CAP.of(content.content);
            long ingredientCount;
            if (ingredient instanceof LongIngredient longIngredient) {
                ingredientCount = longIngredient.getAmount();
            } else if (ingredient instanceof SizedIngredient sizedIngredient) {
                ingredientCount = sizedIngredient.getAmount();
            } else ingredientCount = 1;

            if (content.chance >= content.maxChance) guaranteedMap.addTo(ingredient, ingredientCount);
            else {
                // Separate guaranteed (chance >= maxChance) from chanced inputs
                // Apply boost function to get the actual chance used in roll
                int boostedChance = boostFunction.getBoostedChance(content, baseTier, machineTier);
                if (boostedChance >= content.maxChance) {
                    guaranteedMap.addTo(ingredient, ingredientCount);
                } else {
                    chanceMap.computeIfAbsent(ingredient, ignored -> new ContentAmountPair(boostedChance, content.maxChance)).addTo(ingredientCount);
                }
            }
        }

        if (guaranteedMap.isEmpty() && chanceMap.isEmpty()) return parallelAmount;

        Object2LongOpenCustomHashMap<ItemStack> ingredientStacks = new Object2LongOpenCustomHashMap<>(ItemStackHashStrategy.comparingAllButCount());

        var handle = machine.getRecipeHandleMap().get(recipe);
        if (handle instanceof MEPatternRecipeHandlePart mePatternRecipeHandlePart) {
            // ME handler
            for (var entry : Object2LongMaps.fastIterable(mePatternRecipeHandlePart.getFirstAvailableMEContentOrEmpty(ItemRecipeCapability.CAP, mePatternRecipeHandlePart.getRecipes2SlotsMap().getValues(recipe)))) {
                ingredientStacks.addTo(entry.getKey(), entry.getLongValue());
            }
        } else if (handle != null) {
            for (var entry : Object2LongMaps.fastIterable(handle.getContent(ItemRecipeCapability.CAP))) {
                ingredientStacks.addTo(entry.getKey(), entry.getLongValue());
            }
        } else {
            // ME handlers, All Active Slots
            for (MEPatternRecipeHandlePart part : machine.getMEPatternRecipeHandleParts()) {
                for (var entry : Object2LongMaps.fastIterable(part.getMEContent(ItemRecipeCapability.CAP))) {
                    ingredientStacks.addTo(entry.getKey(), entry.getLongValue());
                }
            }
            // other handlers
            for (var it : machine.getCapabilities().getOrDefault(IO.IN, Collections.emptyList())) {
                for (var entry : Object2LongMaps.fastIterable(it.getContent(ItemRecipeCapability.CAP))) {
                    ingredientStacks.addTo(entry.getKey(), entry.getLongValue());
                }
            }
        }
        return calculateWithChance(parallelAmount, guaranteedMap, chanceMap, ingredientStacks, (Object2IntMap<Ingredient>) cache);
    }

    @SuppressWarnings("unchecked")
    static long getInputFluidParallel(
                                      IRecipeCapabilityHolder holder,
                                      GTRecipe recipe,
                                      long parallelAmount,
                                      Object2IntMap<?> cache,
                                      @NotNull ChanceBoostFunction boostFunction,
                                      int baseTier,
                                      int machineTier) {
        if (parallelAmount <= 1) return parallelAmount;
        if (!(holder instanceof IRecipeCapabilityMachine machine)) return 1;
        if (machine.getRecipeHandleParts().isEmpty() && machine.getMEPatternRecipeHandleParts().isEmpty()) return 0;

        Object2LongOpenHashMap<FluidIngredient> guaranteedFluidMap = new Object2LongOpenHashMap<>();
        Object2ReferenceOpenHashMap<FluidIngredient, ContentAmountPair> chanceFluidMap = new Object2ReferenceOpenHashMap<>();

        for (Content content : recipe.getInputContents(FluidRecipeCapability.CAP)) {
            if (content.chance <= 0) continue;
            FluidIngredient fluidInput = FluidRecipeCapability.CAP.of(content.content);

            if (content.chance >= content.maxChance) guaranteedFluidMap.addTo(fluidInput, fluidInput.getAmount());
            else {
                // Separate guaranteed from chanced fluid inputs
                // Apply boost function to get the actual chance used in roll
                int boostedChance = boostFunction.getBoostedChance(content, baseTier, machineTier);
                if (boostedChance >= content.maxChance) {
                    guaranteedFluidMap.addTo(fluidInput, fluidInput.getAmount());
                } else {
                    chanceFluidMap.computeIfAbsent(fluidInput, ignored -> new ContentAmountPair(boostedChance, content.maxChance)).addTo(fluidInput.getAmount());
                }
            }
        }

        if (guaranteedFluidMap.isEmpty() && chanceFluidMap.isEmpty()) return parallelAmount;

        Object2LongOpenHashMap<FluidStack> ingredientStacks = new Object2LongOpenHashMap<>();

        var recipeHandle = machine.getRecipeHandleMap().get(recipe);
        if (recipeHandle instanceof MEPatternRecipeHandlePart mePatternRecipeHandlePart) {
            // ME handler
            for (var entry : Object2LongMaps.fastIterable(mePatternRecipeHandlePart.getFirstAvailableMEContentOrEmpty(FluidRecipeCapability.CAP, mePatternRecipeHandlePart.getRecipes2SlotsMap().getValues(recipe)))) {
                ingredientStacks.addTo(entry.getKey(), entry.getLongValue());
            }
        } else if (recipeHandle != null && machine.isDistinct()) {
            for (var entry : Object2LongMaps.fastIterable(recipeHandle.getContent(FluidRecipeCapability.CAP))) {
                ingredientStacks.addTo(entry.getKey(), entry.getLongValue());
            }
        } else {
            for (var container : machine.getCapabilitiesFlat(IO.IN, FluidRecipeCapability.CAP)) {
                if (container instanceof CatalystFluidStackHandler) continue;
                for (var object : container.getContents()) {
                    if (object instanceof FluidStack fs) {
                        ingredientStacks.addTo(fs, fs.getAmount());
                    }
                }
            }
        }
        return calculateWithChance(parallelAmount, guaranteedFluidMap, chanceFluidMap, ingredientStacks, (Object2IntMap<FluidIngredient>) cache);
    }

    private static <I extends Predicate<S>, S> long calculateWithChance(
                                                                        long parallelLimit,
                                                                        Object2LongMap<I> guaranteedMap,
                                                                        Object2ReferenceMap<I, ContentAmountPair> chanceMap,
                                                                        Object2LongMap<S> ingredientStacks,
                                                                        Object2IntMap<I> cache) {
        if (ingredientStacks.isEmpty()) return 0;

        // First, calculate parallel limit for guaranteed inputs (traditional method)
        for (var entry : Object2LongMaps.fastIterable(guaranteedMap)) {
            long needed = entry.getLongValue();

            long available = findAvailable(entry.getKey(), ingredientStacks);
            if (available < needed) return 0;

            parallelLimit = Math.min(parallelLimit, available / needed);
        }

        // For chanced inputs, calculate maximum parallel based on roll simulation
        for (var entry : Object2ReferenceMaps.fastIterable(chanceMap)) {
            ContentAmountPair chanceNeeded = entry.getValue();
            I ingredient = entry.getKey();

            long available = findAvailable(ingredient, ingredientStacks);
            if (available < chanceNeeded.amount) return 0;

            // Get or generate cached chance value
            int cached = getGuaranteedCachedChanceValue(ingredient, chanceNeeded.maxChance, cache);

            // parallel <= ((maxOutputs + 1) * maxChance - 1 - cached) / chance
            // This ensures: floor((parallel * chance + cached) / maxChance) <= maxOutputs
            long maxOutputs = available / chanceNeeded.amount;
            long maxParallelForThis = ((maxOutputs + 1) * chanceNeeded.maxChance - 1 - cached) / chanceNeeded.chance;

            // Ensure non-negative
            if (maxParallelForThis < 0) maxParallelForThis = 0;

            parallelLimit = Math.min(parallelLimit, maxParallelForThis);
        }

        return parallelLimit;
    }

    private static <I extends Predicate<S>, S> long findAvailable(I ingredient, Object2LongMap<S> stacks) {
        for (var it = Object2LongMaps.fastIterator(stacks); it.hasNext();) {
            var input = it.next();
            if (ingredient.test(input.getKey())) {
                it.remove();
                return input.getLongValue();
            }
        }
        return 0;
    }

    private static <I> int getGuaranteedCachedChanceValue(I content, int maxChance, @Nullable Object2IntMap<I> cache) {
        if (cache == null) {
            return GTValues.RNG.nextInt(maxChance);
        }

        if (cache.containsKey(content)) {
            return cache.getInt(content);
        } else {
            // Generate random and save to cache for consistency
            int randomValue = GTValues.RNG.nextInt(maxChance);
            cache.put(content, randomValue);
            return randomValue;
        }
    }
}
