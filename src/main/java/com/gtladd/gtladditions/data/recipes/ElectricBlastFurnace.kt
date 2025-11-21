package com.gtladd.gtladditions.data.recipes

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.dust
import com.gregtechceu.gtceu.common.data.GTMaterials.*
import com.gregtechceu.gtceu.common.data.GTRecipeTypes.BLAST_RECIPES
import com.gtladd.gtladditions.GTLAdditions
import net.minecraft.data.recipes.FinishedRecipe
import java.util.function.Consumer

object ElectricBlastFurnace {
    fun init(provider : Consumer<FinishedRecipe?>) {
        BLAST_RECIPES.recipeBuilder(GTLAdditions.id("magnesium_chloride_dust"))
            .inputItems(dust, Magnesia)
            .inputItems(dust, Carbon)
            .inputFluids(Chlorine.getFluid(4000))
            .outputItems(dust, MagnesiumChloride)
            .outputFluids(CarbonMonoxide.getFluid(1000))
            .blastFurnaceTemp(2160).EUt(GTValues.VA[GTValues.HV].toLong())
            .duration(200).save(provider)
    }
}
