package com.gtladd.gtladditions.data.recipes

import com.gregtechceu.gtceu.api.data.tag.TagPrefix.*
import com.gregtechceu.gtceu.api.data.tag.TagUtil
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient
import com.gregtechceu.gtceu.common.data.GTMaterials.*
import com.gtladd.gtladditions.GTLAdditions.id
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.world.item.crafting.Ingredient
import org.gtlcore.gtlcore.common.data.GTLMaterials.Jasper
import org.gtlcore.gtlcore.common.data.GTLMaterials.RawTengam
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes.INTEGRATED_ORE_PROCESSOR
import org.gtlcore.gtlcore.config.ConfigHolder
import java.util.function.Consumer

object IntegratedtedOreProcessor {
    val orenumber : Int = ConfigHolder.INSTANCE.oreMultiplier
    val orefluid : Int = 100 * orenumber

    @JvmStatic
    fun init(provider : Consumer<FinishedRecipe?>) {
        INTEGRATED_ORE_PROCESSOR.recipeBuilder(id("jasper_ore_processed"))
            .circuitMeta(24)
            .inputItems(SizedIngredient.create(Ingredient.of(TagUtil.createItemTag("ores/jasper"))))
            .inputFluids(DistilledWater.getFluid(2L * orefluid))
            .outputItems(dust, Jasper, 2 * orenumber)
            .chancedOutput(dust, Talc, 1400, 850)
            .chancedOutput(dust, Talc, 2 * orenumber, 3300, 0)
            .outputItems(dust, Stone, 2 * orenumber)
            .chancedOutput(dust, Boron, 2 * orenumber, 1400, 850)
            .chancedOutput(dust, RawTengam, 2 * orenumber, 1000, 0)
            .chancedOutput(dust, RawTengam, 2 * orenumber, 500, 0)
            .EUt(30).duration(26 + 800 * 2 * orenumber).save(provider)
        val PlatinmGroupSludgeDustList = arrayOf(
            arrayOf(Cooperite, Nickel, Palladium, Mercury),
            arrayOf(Bornite, Pyrite, Gold, Mercury),
            arrayOf(Tetrahedrite, Antimony, Cadmium, SodiumPersulfate),
            arrayOf(Chalcocite, Sulfur)
        )
        for (pure in PlatinmGroupSludgeDustList) {
            INTEGRATED_ORE_PROCESSOR.recipeBuilder(id("purified_" + pure[0].name + "_ore_8"))
                .circuitMeta(8)
                .inputItems(TagUtil.createItemTag("ores/" + pure[0].name))
                .inputFluids(DistilledWater.getFluid(2L * orefluid))
                .outputItems(crushedPurified, pure[0], 2 * orenumber)
                .chancedOutput(dust, pure[1], 1400, 850)
                .chancedOutput(dust, pure[1], 2 * orenumber, 3300, 0)
                .outputItems(dust, Stone, 2 * orenumber)
                .EUt(30).duration(26 + 200 * 2 * orenumber).save(provider)
            if (pure[0] == Chalcocite) return
            INTEGRATED_ORE_PROCESSOR.recipeBuilder(id("purified_" + pure[0].name + "_ore_9"))
                .circuitMeta(9)
                .inputItems(TagUtil.createItemTag("ores/" + pure[0].name))
                .inputFluids(pure[3].getFluid(2L * orefluid))
                .outputItems(crushedPurified, pure[0], 2 * orenumber)
                .chancedOutput(dust, pure[1], 1400, 850)
                .chancedOutput(dust, pure[2], 2 * orenumber, 7000, 580)
                .outputItems(dust, Stone, 2 * orenumber)
                .EUt(30).duration(26 + 200 * 2 * orenumber).save(provider)
        }
    }
}
