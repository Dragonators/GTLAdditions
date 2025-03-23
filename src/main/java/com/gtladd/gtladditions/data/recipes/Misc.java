package com.gtladd.gtladditions.data.recipes;

import org.gtlcore.gtlcore.common.data.GTLMaterials;
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes;

import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.api.registry.GTLAddRecipeBuilder;

import java.util.function.Consumer;

public class Misc {

    public Misc() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        new GTLAddRecipeBuilder("tiranium50", GTLRecipeTypes.DECAY_HASTENER_RECIPES)
                .inputFluids(GTMaterials.Titanium.getFluid(144))
                .outputFluids(GTLMaterials.Titanium50.getFluid(144))
                .TierEUtVA(13).duration(100).save(provider);
    }
}
