package com.gtladd.gtladditions.data.Recipes.NewMachineRecipe;

import org.gtlcore.gtlcore.common.data.GTLMaterials;

import com.gregtechceu.gtceu.api.data.tag.TagPrefix;

import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.api.Recipe.GTLAddRecipesTypes;
import com.gtladd.gtladditions.api.Registry.GTLAddRecipeBuilder;

import java.util.function.Consumer;

public class TitansCripEarthbore {

    public TitansCripEarthbore() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        new GTLAddRecipeBuilder("bedrock_dust", GTLAddRecipesTypes.TECTONIC_FAULT_GENERATOR)
                .chancedInputItems("kubejs:bedrock_drill", 1, 0)
                .circuitMeta(1)
                .outputItems(TagPrefix.dust, GTLMaterials.Bedrock, 64)
                .TierEUtVA(11).duration(1200).save(provider);
    }
}
