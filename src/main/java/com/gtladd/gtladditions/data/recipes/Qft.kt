package com.gtladd.gtladditions.data.recipes

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.data.tag.TagPrefix
import com.gregtechceu.gtceu.common.data.GTMaterials.*
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.registry.GTLAddRecipeBuilder
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.common.data.GTLMaterials.*
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes.QFT_RECIPES
import java.util.function.Consumer

object Qft {
    @JvmStatic
    fun init(provider : Consumer<FinishedRecipe?>) {
        GTLAddRecipeBuilder("resonating_gem", QFT_RECIPES)
            .notConsumable("kubejs:eternity_catalyst")
            .InputItems("64x gtceu:sapphire_dust")
            .inputFluids(Mana.getFluid(10000))
            .inputFluids(Starlight.getFluid(10000))
            .inputFluids(Water.getFluid(10000000))
            .OutputItems("64x kubejs:resonating_gem")
            .TierEUtVA(14).duration(1200).save(provider)
        GTLAddRecipeBuilder("gamma_rays_photoresist", QFT_RECIPES)
            .notConsumable("kubejs:spacetime_catalyst")
            .InputItems("16x gtceu:flerovium_dust")
            .InputItems("16x gtceu:lanthanum_dust")
            .InputItems("16x gtceu:unfolded_fullerene_dust")
            .InputItems("16x gtceu:holmium_dust")
            .InputItems("16x gtceu:thulium_dust")
            .InputItems("16x gtceu:copernicium_dust")
            .InputItems("16x gtceu:astatine_dust")
            .InputItems("16x gtceu:francium_dust")
            .InputItems("16x gtceu:boron_dust")
            .InputItems("16x gtceu:carbon_dust")
            .inputFluids(EuvPhotoresist.getFluid(10000))
            .inputFluids(Chlorine.getFluid(16000))
            .inputFluids(Nitrogen.getFluid(32000))
            .outputFluids(GammaRaysPhotoresist.getFluid(16000))
            .TierEUtVA(14).duration(2560).save(provider)
        QFT_RECIPES.recipeBuilder(GTLAdditions.id("radox_easy"))
            .inputItems(TagPrefix.dust, Chromium, 4)
            .inputItems(TagPrefix.dust, Boron, 4)
            .inputItems(TagPrefix.dust, Silver, 4)
            .inputItems(TagPrefix.dust, Cobalt, 4)
            .inputItems(TagPrefix.dust, Silicon, 4)
            .inputItems(TagPrefix.dust, Molybdenum, 4)
            .inputItems(TagPrefix.dust, Zirconium, 4)
            .inputItems(TagPrefix.dust, Copper, 4)
            .inputItems(TagPrefix.dust, Arsenic, 4)
            .inputItems(TagPrefix.dust, Antimony, 4)
            .inputItems(TagPrefix.dust, Phosphorus, 4)
            .inputItems(TagPrefix.dust, Zinc, 4)
            .inputItems(TagPrefix.dust, Sodium, 4)
            .inputItems(TagPrefix.dust, Magnesium, 4)
            .inputItems(TagPrefix.dust, Lead, 4)
            .inputItems(TagPrefix.dust, Potassium, 4)
            .inputItems(TagPrefix.dust, Germanium, 4)
            .inputItems(TagPrefix.dust, RareEarth, 4)
            .inputFluids(RadoxGas.getFluid(21600))
            .inputFluids(Oxygen.getFluid(175000))
            .inputFluids(Titanium50.getFluid(576))
            .outputFluids(Radox.getFluid(10800))
            .EUt(GTValues.VA[14] * 256L).duration(2560).save(provider)
        GTLAddRecipeBuilder("super_mutated_living_solder", QFT_RECIPES)
            .notConsumable("kubejs:spacetime_catalyst")
            .InputItems("256x kubejs:essence_seed")
            .InputItems("256x kubejs:draconium_dust")
            .InputItems("256x ae2:sky_dust")
            .InputItems("4x gtceu:nether_star_dust")
            .inputFluids(MutatedLivingSolder.getFluid(100000))
            .inputFluids(Biomass.getFluid(1000000))
            .inputFluids(SterileGrowthMedium.getFluid(1000000))
            .outputFluids(SuperMutatedLivingSolder.getFluid(100000))
            .EUt(GTValues.VA[GTValues.MAX].toLong()).duration(7200).save(provider)
    }
}
