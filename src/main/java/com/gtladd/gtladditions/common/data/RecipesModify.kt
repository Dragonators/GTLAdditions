package com.gtladd.gtladditions.common.data;

import org.gtlcore.gtlcore.common.data.GTLMaterials;
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes;

import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.lowdragmc.lowdraglib.utils.LocalizationUtils;

import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes;

import java.text.NumberFormat;

public class RecipesModify {

    public RecipesModify() {}

    public static void init() {
        GTLRecipeTypes.DOOR_OF_CREATE_RECIPES.setMaxIOSize(1, 1, 0, 0);
        GTLRecipeTypes.CREATE_AGGREGATION_RECIPES.setMaxIOSize(2, 1, 0, 0);
        GTRecipeTypes.FUSION_RECIPES.getDataInfos().clear();
        GTRecipeTypes.FUSION_RECIPES.addDataInfo((data) -> LocalizationUtils.format("gtceu.recipe.eu_to_starts",
                NumberFormat.getCompactNumberInstance().format(data.getLong("eu_to_start") / 1000000),
                getFusionTier(data.getLong("eu_to_start") / 1000000)));
        GTRecipeTypes.LASER_ENGRAVER_RECIPES.onRecipeBuild((recipeBuilder, provider) -> {
            GTRecipeBuilder recipe = GTLAddRecipesTypes.PHOTON_MATRIX_ETCH.copyFrom(recipeBuilder)
                    .duration((int) (recipeBuilder.duration * 0.2)).EUt(recipeBuilder.EUt());
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

    private static String getFusionTier(long startEu) {
        String tier = "I";
        if (startEu > 1280L) tier = "V";
        else if (startEu > 640L) tier = "IV";
        else if (startEu > 320L) tier = "III";
        else if (startEu > 160L) tier = "II";
        return tier;
    }
}
