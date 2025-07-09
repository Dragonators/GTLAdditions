package com.gtladd.gtladditions.common.data

import com.gregtechceu.gtceu.common.data.GTRecipeTypes
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.common.data.GTLMaterials
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes
import java.util.function.Consumer
import kotlin.math.log10

object RecipesModify {
    @JvmStatic
    fun init() {
        GTLRecipeTypes.DOOR_OF_CREATE_RECIPES.setMaxIOSize(1, 1, 0, 0)
        GTLRecipeTypes.CREATE_AGGREGATION_RECIPES.setMaxIOSize(2, 1, 0, 0)
        GTRecipeTypes.LASER_ENGRAVER_RECIPES.onRecipeBuild { recipeBuilder: GTRecipeBuilder?, provider: Consumer<FinishedRecipe?>? ->
            val recipe = GTLAddRecipesTypes.PHOTON_MATRIX_ETCH.copyFrom(recipeBuilder)
                .duration((recipeBuilder!!.duration * 0.2).toInt()).EUt(recipeBuilder.EUt())
            recipe.save(provider)
            val recipe1 = GTLRecipeTypes.DIMENSIONAL_FOCUS_ENGRAVING_ARRAY_RECIPES.copyFrom(recipeBuilder)
                .duration((recipeBuilder.duration.toDouble() * 0.2).toInt()).EUt(recipeBuilder.EUt() * 4L)
            val value = log10(recipeBuilder.EUt().toDouble()) / log10(4.0)
            if (value > 10.0) {
                recipe1.inputFluids(GTLMaterials.EuvPhotoresist.getFluid((value / 2.0).toLong()))
            } else {
                recipe1.inputFluids(GTLMaterials.Photoresist.getFluid(value.toLong()))
            }
            recipe1.save(provider)
        }
    }
}
