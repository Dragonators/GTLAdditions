package com.gtladd.gtladditions.data.recipes

import com.gregtechceu.gtceu.api.data.tag.TagPrefix.crushedPurified
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.dust
import com.gregtechceu.gtceu.api.data.tag.TagUtil
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient
import com.gregtechceu.gtceu.common.data.GTMaterials.*
import com.gtladd.gtladditions.GTLAdditions.Companion.id
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.world.item.crafting.Ingredient
import org.gtlcore.gtlcore.common.data.GTLMaterials.Jasper
import org.gtlcore.gtlcore.common.data.GTLMaterials.RawTengam
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes.INTEGRATED_ORE_PROCESSOR
import org.gtlcore.gtlcore.config.ConfigHolder
import java.util.function.Consumer

object IntegratedOreProcessor {
    val oreNumber : Int = ConfigHolder.INSTANCE.oreMultiplier
    val oreFluid : Int = 100 * oreNumber

    fun init(provider : Consumer<FinishedRecipe?>) {
        INTEGRATED_ORE_PROCESSOR.recipeBuilder(id("jasper_ore_processed"))
            .circuitMeta(24)
            .inputItems(SizedIngredient.create(Ingredient.of(TagUtil.createItemTag("ores/jasper"))))
            .inputFluids(DistilledWater.getFluid(2L * oreFluid))
            .outputItems(dust, Jasper, 2 * oreNumber)
            .chancedOutput(dust, Talc, 1400, 850)
            .chancedOutput(dust, Talc, 2 * oreNumber, 3300, 0)
            .outputItems(dust, Stone, 2 * oreNumber)
            .chancedOutput(dust, Boron, 2 * oreNumber, 1400, 850)
            .chancedOutput(dust, RawTengam, 2 * oreNumber, 1000, 0)
            .chancedOutput(dust, RawTengam, 2 * oreNumber, 500, 0)
            .EUt(30).duration(26 + 800 * 2 * oreNumber).save(provider)
        val platinmGroupSludgeDustList = arrayOf(
            arrayOf(Cooperite, Nickel, Palladium, Mercury),
            arrayOf(Bornite, Pyrite, Gold, Mercury),
            arrayOf(Tetrahedrite, Antimony, Cadmium, SodiumPersulfate),
            arrayOf(Chalcocite, Sulfur)
        )
        for (pure in platinmGroupSludgeDustList) {
            INTEGRATED_ORE_PROCESSOR.recipeBuilder(id("purified_" + pure[0].name + "_ore_8"))
                .circuitMeta(8)
                .inputItems(TagUtil.createItemTag("ores/" + pure[0].name))
                .inputFluids(DistilledWater.getFluid(2L * oreFluid))
                .outputItems(crushedPurified, pure[0], 2 * oreNumber)
                .chancedOutput(dust, pure[1], 1400, 850)
                .chancedOutput(dust, pure[1], 2 * oreNumber, 3300, 0)
                .outputItems(dust, Stone, 2 * oreNumber)
                .EUt(30).duration(26 + 200 * 2 * oreNumber).save(provider)
            if (pure[0] == Chalcocite) return
            INTEGRATED_ORE_PROCESSOR.recipeBuilder(id("purified_" + pure[0].name + "_ore_9"))
                .circuitMeta(9)
                .inputItems(TagUtil.createItemTag("ores/" + pure[0].name))
                .inputFluids(pure[3].getFluid(2L * oreFluid))
                .outputItems(crushedPurified, pure[0], 2 * oreNumber)
                .chancedOutput(dust, pure[1], 1400, 850)
                .chancedOutput(dust, pure[2], 2 * oreNumber, 7000, 580)
                .outputItems(dust, Stone, 2 * oreNumber)
                .EUt(30).duration(26 + 200 * 2 * oreNumber).save(provider)
        }
    }
}
