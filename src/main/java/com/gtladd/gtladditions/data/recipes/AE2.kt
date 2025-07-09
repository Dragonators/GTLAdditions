package com.gtladd.gtladditions.data.recipes

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.dust
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.plate
import com.gregtechceu.gtceu.api.data.tag.TagUtil
import com.gregtechceu.gtceu.api.data.tag.TagUtil.createItemTag
import com.gregtechceu.gtceu.common.data.GTMaterials.*
import com.gregtechceu.gtceu.common.data.GTRecipeTypes.*
import com.gtladd.gtladditions.GTLAdditions
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes.MATTER_FABRICATOR_RECIPES
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import java.util.function.Consumer

object AE2 {
    @JvmStatic
    fun init(provider : Consumer<FinishedRecipe?>) {
        FORMING_PRESS_RECIPES.recipeBuilder(GTLAdditions.id("cell_component_1k"))
            .inputItems(plate, CertusQuartz)
            .inputItems(plate, Redstone)
            .inputItems(getItemStack("ae2:logic_processor"))
            .outputItems(getItemStack("ae2:cell_component_1k"))
            .EUt(1).duration(100).save(provider)
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
        ASSEMBLER_RECIPES.recipeBuilder(GTLAdditions.id("energy_cell"))
            .inputItems(createItemTag("gems/certus_quartz"), 4)
            .inputItems(getItemStack("ae2:fluix_dust", 4))
            .inputItems(getItemStack("ae2:quartz_glass"))
            .outputItems(getItemStack("ae2:energy_cell"))
            .EUt(32).duration(10).save(provider)
        ASSEMBLER_RECIPES.recipeBuilder(GTLAdditions.id("dense_energy_cell"))
            .inputItems(getItemStack("ae2:energy_cell", 8))
            .inputItems(getItemStack("ae2:calculation_processor"))
            .outputItems(getItemStack("ae2:dense_energy_cell"))
            .EUt(32).duration(10).save(provider)
    }
}
