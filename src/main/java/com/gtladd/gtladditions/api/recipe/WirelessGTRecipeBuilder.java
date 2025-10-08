package com.gtladd.gtladditions.api.recipe;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.List;

public class WirelessGTRecipeBuilder extends GTRecipeBuilder {

    @Nullable
    public BigInteger wirelessEut;

    public WirelessGTRecipeBuilder(ResourceLocation id, GTRecipeType recipeType) {
        super(id, recipeType);
    }

    public WirelessGTRecipeBuilder(GTRecipe toCopy, GTRecipeType recipeType) {
        super(toCopy, recipeType);
    }

    public WirelessGTRecipeBuilder setWirelessEut(@Nullable BigInteger wirelessEut) {
        this.wirelessEut = wirelessEut;
        return this;
    }

    public static WirelessGTRecipeBuilder ofRaw() {
        return ofRaw(GTRecipeTypes.DUMMY_RECIPES);
    }

    public static WirelessGTRecipeBuilder ofRaw(GTRecipeType recipeType) {
        return new WirelessGTRecipeBuilder(GTCEu.id("raw"), recipeType);
    }

    @NotNull
    public WirelessGTRecipeBuilder output(RecipeCapability<?> cap, List<Content> contents) {
        this.output.put(cap, contents);
        return this;
    }

    @Override
    @NotNull
    public WirelessGTRecipeBuilder duration(int duration) {
        this.duration = duration;
        return this;
    }

    @Override
    @NotNull
    public WirelessGTRecipe buildRawRecipe() {
        return new WirelessGTRecipe(this.recipeType, this.id.withPrefix(this.recipeType.registryName.getPath() + "/"), this.input, this.output, this.tickInput, this.tickOutput, this.inputChanceLogic, this.outputChanceLogic, this.tickInputChanceLogic, this.tickOutputChanceLogic, this.conditions, List.of(), this.data, this.duration, this.isFuel, wirelessEut);
    }
}
