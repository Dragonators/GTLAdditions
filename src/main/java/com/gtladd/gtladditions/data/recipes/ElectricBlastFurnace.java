package com.gtladd.gtladditions.data.recipes;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

public class ElectricBlastFurnace {
    public ElectricBlastFurnace() {}
    public static void init(Consumer<FinishedRecipe> provider) {
        GTRecipeTypes.BLAST_RECIPES.recipeBuilder("magnesium_chloride_dust")
                .inputItems(TagPrefix.dust, GTMaterials.Magnesia).inputItems(TagPrefix.dust, GTMaterials.Carbon)
                .inputFluids(GTMaterials.Chlorine.getFluid(4000))
                .outputItems(TagPrefix.dust, GTMaterials.MagnesiumChloride).outputFluids(GTMaterials.CarbonMonoxide.getFluid(1000))
                .blastFurnaceTemp(2160).EUt(GTValues.VA[GTValues.HV]).duration(200).save(provider);
    }
}
