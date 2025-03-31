package com.gtladd.gtladditions.common.data;

import org.gtlcore.gtlcore.common.data.GTLMaterials;
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes;

import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.gtladd.gtladditions.api.recipe.GTLAddRecipesTypes;

public class RecipesModify {

    public RecipesModify() {}

    public static void init() {
        GTLRecipeTypes.DOOR_OF_CREATE_RECIPES.setMaxIOSize(1, 1, 0, 0);
        GTLRecipeTypes.CREATE_AGGREGATION_RECIPES.setMaxIOSize(2, 1, 0, 0);
        GTRecipeTypes.LASER_ENGRAVER_RECIPES.onRecipeBuild((recipeBuilder, provider) -> {
            GTRecipeBuilder recipe = GTLAddRecipesTypes.PHOTON_MATRIX_ETCH.copyFrom(recipeBuilder)
                    .duration((int) ((double) recipeBuilder.duration * 0.2)).EUt(recipeBuilder.EUt());
            recipe.save(provider);
            GTRecipeBuilder recipe1 = GTLRecipeTypes.DIMENSIONAL_FOCUS_ENGRAVING_ARRAY_RECIPES.copyFrom(recipeBuilder)
                    .duration((int) ((double) recipeBuilder.duration * 0.2)).EUt(recipeBuilder.EUt() * 4L);
            double value = Math.log10((double) recipeBuilder.EUt()) / Math.log10(4.0);
            if (value > 10.0) {
                recipe1.inputFluids(GTLMaterials.EuvPhotoresist.getFluid((long) (value / 2.0)));
            } else {
                recipe1.inputFluids(GTLMaterials.Photoresist.getFluid((long) value));
            }
            recipe1.save(provider);
        });
    }
}
