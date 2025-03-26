package com.gtladd.gtladditions.data.recipes.newmachinerecipe;

import org.gtlcore.gtlcore.common.data.GTLMaterials;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.api.recipe.GTLAddRecipesTypes;

import java.util.function.Consumer;

public class ChaoticAlchemy {

    public static void init(Consumer<FinishedRecipe> provider) {
        GTLAddRecipesTypes.CHAOTIC_ALCHEMY.recipeBuilder("carbon_disulfide")
                .circuitMeta(8).inputItems(TagPrefix.dust, GTMaterials.Carbon).inputItems(TagPrefix.dust, GTMaterials.Sulfur, 2)
                .outputFluids(GTLMaterials.CarbonDisulfide.getFluid(1000))
                .EUt(GTValues.VA[GTValues.LV]).duration(350).blastFurnaceTemp(1200).save(provider);
    }
}
