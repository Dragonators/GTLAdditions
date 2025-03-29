package com.gtladd.gtladditions.data.recipes.newmachinerecipe;

import org.gtlcore.gtlcore.common.data.GTLMaterials;
import org.gtlcore.gtlcore.utils.Registries;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;

import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.api.recipe.GTLAddRecipesTypes;

import java.util.function.Consumer;

public class TitansCripEarthbore {

    public TitansCripEarthbore() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        GTLAddRecipesTypes.TECTONIC_FAULT_GENERATOR.recipeBuilder("bedrock_dust")
                .chancedInput(Registries.getItemStack("kubejs:bedrock_drill"), 100, 0).circuitMeta(1)
                .outputItems(TagPrefix.dust, GTLMaterials.Bedrock, 64)
                .EUt(GTValues.VA[11]).duration(1200).save(provider);
    }
}
