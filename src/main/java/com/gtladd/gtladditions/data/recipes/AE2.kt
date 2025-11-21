package com.gtladd.gtladditions.data.recipes

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.dust
import com.gregtechceu.gtceu.api.data.tag.TagUtil.createItemTag
import com.gregtechceu.gtceu.common.data.GTMaterials.CertusQuartz
import com.gregtechceu.gtceu.common.data.GTRecipeTypes.ALLOY_SMELTER_RECIPES
import com.gtladd.gtladditions.GTLAdditions
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes.MATTER_FABRICATOR_RECIPES
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import java.util.function.Consumer

object AE2 {
    fun init(provider : Consumer<FinishedRecipe?>) {
        MATTER_FABRICATOR_RECIPES.recipeBuilder(GTLAdditions.id("singularity_1"))
            .inputItems(getItemStack("kubejs:scrap", 4320))
            .circuitMeta(3)
            .outputItems(getItemStack("ae2:singularity"))
            .EUt(GTValues.VA[9].toLong()).duration(1).save(provider)
        MATTER_FABRICATOR_RECIPES.recipeBuilder(GTLAdditions.id("singularity_2"))
            .inputItems(getItemStack("kubejs:scrap_box", 480))
            .circuitMeta(3)
            .outputItems(getItemStack("ae2:singularity", 9))
            .EUt(GTValues.VA[10].toLong()).duration(1).save(provider)
        ALLOY_SMELTER_RECIPES.recipeBuilder(GTLAdditions.id("quartz_glassquartz_glass"))
            .inputItems(createItemTag("glass"))
            .inputItems(dust, CertusQuartz)
            .outputItems(getItemStack("ae2:quartz_glass"))
            .EUt(7).duration(150).save(provider)
    }
}
