package com.gtladd.gtladditions.data.recipes;

import org.gtlcore.gtlcore.common.data.GTLMaterials;
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes;

import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.api.registry.GTLAddRecipeBuilder;

import java.util.function.Consumer;

public class Qft {

    public Qft() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        new GTLAddRecipeBuilder("resonating_gem", GTLRecipeTypes.QFT_RECIPES)
                .notConsumable("kubejs:eternity_catalyst")
                .InputItems("64x gtceu:sapphire_dust")
                .inputFluids(GTLMaterials.Mana.getFluid(10000))
                .inputFluids(GTLMaterials.Starlight.getFluid(10000))
                .inputFluids(GTMaterials.Water.getFluid(10000000))
                .OutputItems("64x kubejs:resonating_gem")
                .TierEUtVA(14)
                .duration(1200)
                .save(provider);
    }
}
