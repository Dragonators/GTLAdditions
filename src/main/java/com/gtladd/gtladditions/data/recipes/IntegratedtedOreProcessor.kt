package com.gtladd.gtladditions.data.recipes

import com.gregtechceu.gtceu.api.data.tag.TagPrefix.*
import com.gregtechceu.gtceu.api.data.tag.TagUtil
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient
import com.gregtechceu.gtceu.common.data.GTMaterials.*
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.registry.GTLAddRecipeBuilder
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.world.item.crafting.Ingredient
import org.gtlcore.gtlcore.common.data.GTLMaterials
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes
import org.gtlcore.gtlcore.config.ConfigHolder
import java.util.function.Consumer

object IntegratedtedOreProcessor {
    val orenumber : Int = ConfigHolder.INSTANCE.oreMultiplier
    val orefluid : Int = 100 * orenumber

    @JvmStatic
    fun init(provider : Consumer<FinishedRecipe?>) {
        GTLRecipeTypes.INTEGRATED_ORE_PROCESSOR.recipeBuilder(GTLAdditions.id("jasper_ore_processed"))
            .circuitMeta(24)
            .inputItems(SizedIngredient.create(Ingredient.of(TagUtil.createItemTag("ores/jasper"))))
            .inputFluids(DistilledWater.getFluid(2L * orefluid))
            .outputItems(dust, GTLMaterials.Jasper, 2 * orenumber)
            .chancedOutput(dust, Talc, 1400, 850)
            .chancedOutput(dust, Talc, 2 * orenumber, 3300, 0)
            .outputItems(dust, Stone, 2 * orenumber)
            .chancedOutput(dust, Boron, 2 * orenumber, 1400, 850)
            .chancedOutput(dust, GTLMaterials.RawTengam, 2 * orenumber, 1000, 0)
            .chancedOutput(dust, GTLMaterials.RawTengam, 2 * orenumber, 500, 0)
            .EUt(30).duration(26 + 800 * 2 * orenumber).save(provider)
        val platinum_group_sludge_dust_list = arrayOf<Array<String?>?>(
            arrayOf("cooperite", "nickel", "palladium", "mercury"),
            arrayOf("bornite", "pyrite", "gold", "mercury"),
            arrayOf("tetrahedrite", "antimony", "cadmium", "sodium_persulfate"),
            arrayOf("chalcocite", "sulfur")
        )
        for (pure in platinum_group_sludge_dust_list) {
            GTLAddRecipeBuilder("purified_" + pure !![0] + "_ore_8", GTLRecipeTypes.INTEGRATED_ORE_PROCESSOR)
                .circuitMeta(8).inputItemsTag("ores/" + pure[0])
                .inputFluids(DistilledWater.getFluid(2L * orefluid))
                .outputItems("gtceu:purified_" + pure[0] + "_ore", 2 * orenumber)
                .chancedOutputItems("gtceu:" + pure[1] + "_dust", 14.0, 8.5)
                .chancedOutputItems("gtceu:" + pure[1] + "_dust", 2 * orenumber, 33.0, 0.0)
                .outputItems(dust, Stone, 2 * orenumber)
                .EUt(30).duration(26 + 200 * 2 * orenumber).save(provider)
            if (pure[0] == "chalcocite") return
            GTLAddRecipeBuilder("purified_" + pure[0] + "_ore_9", GTLRecipeTypes.INTEGRATED_ORE_PROCESSOR)
                .circuitMeta(9).inputItemsTag("ores/" + pure[0])
                .inputFluids("gtceu:" + pure[3], 2 * orefluid)
                .outputItems("gtceu:purified_" + pure[0] + "_ore", 2 * orenumber)
                .chancedOutputItems("gtceu:" + pure[1] + "_dust", 14.0, 8.5)
                .chancedOutputItems("gtceu:" + pure[1] + "_dust", 2 * orenumber, 33.0, 0.0)
                .outputItems(dust, Stone, 2 * orenumber)
                .EUt(30).duration(26 + 200 * 2 * orenumber).save(provider)
        }
    }
}
