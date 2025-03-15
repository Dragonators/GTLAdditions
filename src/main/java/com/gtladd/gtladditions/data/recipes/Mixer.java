package com.gtladd.gtladditions.data.recipes;

import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.api.registry.GTLAddRecipeBuilder;

import java.util.function.Consumer;

public class Mixer {

    public Mixer() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        new GTLAddRecipeBuilder("warped_ender_pearl", GTRecipeTypes.MIXER_RECIPES)
                .inputItems("minecraft:bone_meal", 4).inputItems("minecraft:blaze_powder", 4).inputItems("minecraft:ender_pearl")
                .outputItems("kubejs:warped_ender_pearl", 4)
                .TierEUtVA(4).duration(200).save(provider);
    }
}
