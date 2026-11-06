package com.gtladd.gtladditions.mixin.gtlcore.recipe;

import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine;
import org.gtlcore.gtlcore.api.machine.trait.MEPatternRecipeHandlePart;
import org.gtlcore.gtlcore.api.machine.trait.RecipeHandlePart;
import org.gtlcore.gtlcore.api.recipe.RecipeCacheStrategy;
import org.gtlcore.gtlcore.api.recipe.RecipeRunner;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import com.gtladd.gtladditions.common.machine.multiblock.part.MESuperPatternBufferPartMachine;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Collections;
import java.util.List;

@Mixin(value = RecipeRunner.class, remap = false)
public abstract class RecipeRunnerMixin {

    /**
     * @author Codex
     * @reason Prefer super pattern inputs using the original distinct input rules.
     */
    @Overwrite
    @SuppressWarnings("DuplicatedCode")
    private static boolean handleInputDistinct(IRecipeCapabilityMachine machine, GTRecipe recipe,
                                               Reference2ObjectOpenHashMap<RecipeCapability<?>, List<Object>> recipeContent,
                                               boolean simulated, RecipeCacheStrategy strategy) {
        // Prefer super pattern inputs before Core's original source passes.
        boolean triedSuper = false;
        if (simulated && strategy.cacheToHandlePartMap && gtladditions$onlyPatternInputs(recipeContent)) {
            var parts = machine.getMEPatternRecipeHandleParts();
            for (MEPatternRecipeHandlePart part : parts) {
                if (!gtladditions$isSuperPattern(part)) continue;
                triedSuper = true;
                var slot = part.handleRecipe(recipe, recipeContent, simulated, strategy.cacheToMEInternal);
                if (slot != -1) {
                    if (slot == -2) machine.tryAddAndActiveRhp(recipe, part);
                    else machine.tryAddAndActiveMERhp(part, recipe, slot);
                    return true;
                }
            }
        }

        // Priority 1: Try all cached handlers (active is first in iterator)
        for (var it = machine.getAllCachedRecipeHandlesIter(recipe); it.hasNext();) {
            var handler = it.next();

            if (handler instanceof MEPatternRecipeHandlePart cachedMEPart) {
                if (triedSuper && gtladditions$isSuperPattern(cachedMEPart)) continue;
                var slot = cachedMEPart.handleRecipe(recipe, recipeContent, simulated, strategy.cacheToMEInternal);
                if (slot != -1) {
                    if (simulated && strategy.cacheToHandlePartMap) {
                        if (slot == -2) machine.tryAddAndActiveRhp(recipe, cachedMEPart);
                        else machine.tryAddAndActiveMERhp(cachedMEPart, recipe, slot);
                    }
                    return true;
                }
            } else if (handler instanceof RecipeHandlePart cachedNormalPart) {
                var result = cachedNormalPart.handleRecipe(IO.IN, recipe, recipeContent, simulated);
                if (result.isEmpty()) {
                    if (simulated && strategy.cacheToHandlePartMap) {
                        machine.tryAddAndActiveRhp(recipe, cachedNormalPart);
                    }
                    return true;
                }
            }
        }

        var cachedHandlers = machine.getAllCachedRecipeHandles(recipe);

        // Priority 2: Try uncached ME Pattern parts
        for (var part : machine.getMEPatternRecipeHandleParts()) {
            if (cachedHandlers.contains(part) || triedSuper && gtladditions$isSuperPattern(part)) continue;
            var slot = part.handleRecipe(recipe, recipeContent, simulated, strategy.cacheToMEInternal);
            if (slot >= 0) {
                if (simulated && strategy.cacheToHandlePartMap) {
                    machine.tryAddAndActiveMERhp(part, recipe, slot);
                }
                return true;
            }
        }

        // Priority 3: Try uncached normal parts
        for (var part : machine.getNormalRecipeHandlePart(IO.IN)) {
            if (cachedHandlers.contains(part)) continue;
            var result = part.handleRecipe(IO.IN, recipe, recipeContent, simulated);
            if (result.isEmpty()) {
                if (simulated && strategy.cacheToHandlePartMap) {
                    machine.tryAddAndActiveRhp(recipe, part);
                }
                return true;
            }
        }

        return false;
    }

    /**
     * @author Codex
     * @reason Prefer super pattern inputs using the original shared-fluid input rules.
     */
    @Overwrite
    @SuppressWarnings("DuplicatedCode")
    private static boolean handleInputNotDistinct(IRecipeCapabilityMachine machine, GTRecipe recipe,
                                                  Reference2ObjectOpenHashMap<RecipeCapability<?>, List<Object>> recipeContent,
                                                  boolean simulated, RecipeCacheStrategy strategy) {
        // Prefer super pattern inputs before Core's original source passes.
        boolean triedSuper = false;
        if (simulated && strategy.cacheToHandlePartMap && gtladditions$onlyPatternInputs(recipeContent)) {
            var parts = machine.getMEPatternRecipeHandleParts();
            for (MEPatternRecipeHandlePart part : parts) {
                if (!gtladditions$isSuperPattern(part)) continue;
                triedSuper = true;
                var slot = part.handleRecipe(recipe, recipeContent, simulated, strategy.cacheToMEInternal);
                if (slot != -1) {
                    if (slot == -2) machine.tryAddAndActiveRhp(recipe, part);
                    else machine.tryAddAndActiveMERhp(part, recipe, slot);
                    return true;
                }
            }
        }

        boolean fluidHandleResult = false;
        boolean hasFluidTry = false;

        // Priority 1: Try all cached handlers (active is first in iterator)
        for (var it = machine.getAllCachedRecipeHandlesIter(recipe); it.hasNext();) {
            var handler = it.next();

            if (handler instanceof MEPatternRecipeHandlePart cachedMEPart) {
                if (triedSuper && gtladditions$isSuperPattern(cachedMEPart)) continue;
                var slot = cachedMEPart.handleRecipe(recipe, recipeContent, simulated, strategy.cacheToMEInternal);
                if (slot != -1) {
                    if (simulated && strategy.cacheToHandlePartMap) {
                        if (slot == -2) machine.tryAddAndActiveRhp(recipe, cachedMEPart);
                        else machine.tryAddAndActiveMERhp(cachedMEPart, recipe, slot);
                    }
                    return true;
                }
            } else if (handler instanceof RecipeHandlePart cachedNormalPart) {
                if (!hasFluidTry) {
                    fluidHandleResult = tryNotDistinctFluid(machine.getSharedRecipeHandlePart(), recipe, recipeContent, simulated);
                    hasFluidTry = true;
                }
                if (fluidHandleResult && tryNotDistinctItem(cachedNormalPart, recipe, recipeContent, simulated)) {
                    if (simulated && strategy.cacheToHandlePartMap) {
                        machine.tryAddAndActiveRhp(recipe, cachedNormalPart);
                    }
                    return true;
                }
            }
        }

        var cachedHandlers = machine.getAllCachedRecipeHandles(recipe);

        // Priority 2: Try uncached ME Pattern parts
        for (var part : machine.getMEPatternRecipeHandleParts()) {
            if (cachedHandlers.contains(part) || triedSuper && gtladditions$isSuperPattern(part)) continue;
            var slot = part.handleRecipe(recipe, recipeContent, simulated, strategy.cacheToMEInternal);
            if (slot >= 0) {
                if (simulated && strategy.cacheToHandlePartMap) {
                    machine.tryAddAndActiveMERhp(part, recipe, slot);
                }
                return true;
            }
        }

        // Priority 3: Try uncached normal parts
        RecipeHandlePart sharedPart = machine.getSharedRecipeHandlePart();

        List<?> fluidContent = recipeContent.getOrDefault(FluidRecipeCapability.CAP, Collections.emptyList());
        if (!fluidContent.isEmpty()) {
            if (sharedPart == null) return false;
            fluidContent = sharedPart.handleRecipe(IO.IN, recipe, FluidRecipeCapability.CAP, fluidContent, simulated);
            if (fluidContent != null && !fluidContent.isEmpty()) return false;
        }

        List<?> itemContent = recipeContent.getOrDefault(ItemRecipeCapability.CAP, Collections.emptyList());
        if (itemContent.isEmpty()) {
            if (simulated && strategy.cacheToHandlePartMap) {
                machine.tryAddAndActiveRhp(recipe, sharedPart);
            }
            return true;
        }

        for (var part : machine.getNormalRecipeHandlePart(IO.IN)) {
            if (cachedHandlers.contains(part)) continue;
            var result = part.handleRecipe(IO.IN, recipe, ItemRecipeCapability.CAP, itemContent, simulated);
            if (result == null || result.isEmpty()) {
                if (simulated && strategy.cacheToHandlePartMap) {
                    machine.tryAddAndActiveRhp(recipe, part);
                }
                return true;
            }
        }

        if (sharedPart != null && !cachedHandlers.contains(sharedPart)) {
            var result = sharedPart.handleRecipe(IO.IN, recipe, ItemRecipeCapability.CAP, itemContent, simulated);
            if (result == null || result.isEmpty()) {
                if (simulated && strategy.cacheToHandlePartMap) {
                    machine.tryAddAndActiveRhp(recipe, sharedPart);
                }
                return true;
            }
        }

        return false;
    }

    @Unique
    private static boolean gtladditions$onlyPatternInputs(Reference2ObjectOpenHashMap<RecipeCapability<?>, List<Object>> contents) {
        for (var capability : contents.keySet()) {
            if (capability != ItemRecipeCapability.CAP && capability != FluidRecipeCapability.CAP) return false;
        }
        return true;
    }

    @Unique
    private static boolean gtladditions$isSuperPattern(MEPatternRecipeHandlePart part) {
        return part.getMECapability(ItemRecipeCapability.CAP) instanceof MachineTrait trait &&
                trait.getMachine().self() instanceof MESuperPatternBufferPartMachine;
    }

    @Shadow
    private static boolean tryNotDistinctFluid(RecipeHandlePart sharedPart, GTRecipe recipe,
                                               Reference2ObjectOpenHashMap<RecipeCapability<?>, List<Object>> recipeContent,
                                               boolean simulated) {
        throw new AssertionError();
    }

    @Shadow
    private static boolean tryNotDistinctItem(RecipeHandlePart cachedPart, GTRecipe recipe,
                                              Reference2ObjectOpenHashMap<RecipeCapability<?>, List<Object>> recipeContent,
                                              boolean simulated) {
        throw new AssertionError();
    }
}