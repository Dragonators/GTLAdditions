package com.gtladd.gtladditions.api.recipe;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class WirelessGTRecipe extends GTRecipe implements IWirelessGTRecipe {

    @Nullable
    protected final BigInteger wirelessEuTickInputs;

    public WirelessGTRecipe(GTRecipeType recipeType, Map<RecipeCapability<?>, List<Content>> inputs, Map<RecipeCapability<?>, List<Content>> outputs, Map<RecipeCapability<?>, List<Content>> tickInputs, Map<RecipeCapability<?>, List<Content>> tickOutputs, Map<RecipeCapability<?>, ChanceLogic> inputChanceLogics, Map<RecipeCapability<?>, ChanceLogic> outputChanceLogics, Map<RecipeCapability<?>, ChanceLogic> tickInputChanceLogics, Map<RecipeCapability<?>, ChanceLogic> tickOutputChanceLogics, List<RecipeCondition> conditions, List<?> ingredientActions, @NotNull CompoundTag data, int duration, boolean isFuel) {
        this(recipeType, null, inputs, outputs, tickInputs, tickOutputs, inputChanceLogics, outputChanceLogics, tickInputChanceLogics, tickOutputChanceLogics, conditions, ingredientActions, data, duration, isFuel, null);
    }

    public WirelessGTRecipe(GTRecipeType recipeType, @Nullable ResourceLocation id, Map<RecipeCapability<?>, List<Content>> inputs, Map<RecipeCapability<?>, List<Content>> outputs, Map<RecipeCapability<?>, List<Content>> tickInputs, Map<RecipeCapability<?>, List<Content>> tickOutputs, Map<RecipeCapability<?>, ChanceLogic> inputChanceLogics, Map<RecipeCapability<?>, ChanceLogic> outputChanceLogics, Map<RecipeCapability<?>, ChanceLogic> tickInputChanceLogics, Map<RecipeCapability<?>, ChanceLogic> tickOutputChanceLogics, List<RecipeCondition> conditions, List<?> ingredientActions, @NotNull CompoundTag data, int duration, boolean isFuel) {
        this(recipeType, id, inputs, outputs, tickInputs, tickOutputs, inputChanceLogics, outputChanceLogics, tickInputChanceLogics, tickOutputChanceLogics, conditions, ingredientActions, data, duration, isFuel, null);
    }

    public WirelessGTRecipe(GTRecipeType recipeType, @Nullable ResourceLocation id, Map<RecipeCapability<?>, List<Content>> inputs, Map<RecipeCapability<?>, List<Content>> outputs, Map<RecipeCapability<?>, List<Content>> tickInputs, Map<RecipeCapability<?>, List<Content>> tickOutputs, Map<RecipeCapability<?>, ChanceLogic> inputChanceLogics, Map<RecipeCapability<?>, ChanceLogic> outputChanceLogics, Map<RecipeCapability<?>, ChanceLogic> tickInputChanceLogics, Map<RecipeCapability<?>, ChanceLogic> tickOutputChanceLogics, List<RecipeCondition> conditions, List<?> ingredientActions, @NotNull CompoundTag data, int duration, boolean isFuel, @Nullable BigInteger wirelessEuTickInputs) {
        super(recipeType, id, inputs, outputs, tickInputs, tickOutputs, inputChanceLogics, outputChanceLogics, tickInputChanceLogics, tickOutputChanceLogics, conditions, ingredientActions, data, duration, isFuel);
        this.wirelessEuTickInputs = wirelessEuTickInputs;
    }

    @Override
    @Nullable
    public BigInteger getWirelessEuTickInputs() {
        return wirelessEuTickInputs;
    }
}
