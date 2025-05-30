package com.gtladd.gtladditions.data.recipes;

import org.gtlcore.gtlcore.common.data.GTLMaterials;
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.GTLAdditions;
import com.gtladd.gtladditions.api.registry.GTLAddRecipeBuilder;

import java.util.function.Consumer;

public class Qft {

    public Qft() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        new GTLAddRecipeBuilder("resonating_gem", GTLRecipeTypes.QFT_RECIPES).notConsumable("kubejs:eternity_catalyst")
                .InputItems("64x gtceu:sapphire_dust").inputFluids(GTLMaterials.Mana.getFluid(10000))
                .inputFluids(GTLMaterials.Starlight.getFluid(10000)).inputFluids(GTMaterials.Water.getFluid(10000000))
                .OutputItems("64x kubejs:resonating_gem")
                .TierEUtVA(14).duration(1200).save(provider);
        new GTLAddRecipeBuilder("gamma_rays_photoresist", GTLRecipeTypes.QFT_RECIPES).notConsumable("kubejs:spacetime_catalyst")
                .InputItems("16x gtceu:flerovium_dust").InputItems("16x gtceu:lanthanum_dust").InputItems("16x gtceu:unfolded_fullerene_dust")
                .InputItems("16x gtceu:holmium_dust").InputItems("16x gtceu:thulium_dust").InputItems("16x gtceu:copernicium_dust")
                .InputItems("16x gtceu:astatine_dust").InputItems("16x gtceu:francium_dust").InputItems("16x gtceu:boron_dust")
                .InputItems("16x gtceu:carbon_dust")
                .inputFluids(GTLMaterials.EuvPhotoresist.getFluid(10000)).inputFluids(GTMaterials.Chlorine.getFluid(16000))
                .inputFluids(GTMaterials.Nitrogen.getFluid(32000))
                .outputFluids(GTLMaterials.GammaRaysPhotoresist.getFluid(16000))
                .TierEUtVA(14).duration(2560).save(provider);
        GTLRecipeTypes.QFT_RECIPES.recipeBuilder(GTLAdditions.id("radox_easy"))
                .inputItems(TagPrefix.dust, GTMaterials.Chromium, 4).inputItems(TagPrefix.dust, GTMaterials.Boron, 4)
                .inputItems(TagPrefix.dust, GTMaterials.Silver, 4).inputItems(TagPrefix.dust, GTMaterials.Cobalt, 4)
                .inputItems(TagPrefix.dust, GTMaterials.Silicon, 4).inputItems(TagPrefix.dust, GTMaterials.Molybdenum, 4)
                .inputItems(TagPrefix.dust, GTMaterials.Zirconium, 4).inputItems(TagPrefix.dust, GTMaterials.Copper, 4)
                .inputItems(TagPrefix.dust, GTMaterials.Arsenic, 4).inputItems(TagPrefix.dust, GTMaterials.Antimony, 4)
                .inputItems(TagPrefix.dust, GTMaterials.Phosphorus, 4).inputItems(TagPrefix.dust, GTMaterials.Zinc, 4)
                .inputItems(TagPrefix.dust, GTMaterials.Sodium, 4).inputItems(TagPrefix.dust, GTMaterials.Magnesium, 4)
                .inputItems(TagPrefix.dust, GTMaterials.Lead, 4).inputItems(TagPrefix.dust, GTMaterials.Potassium, 4)
                .inputItems(TagPrefix.dust, GTMaterials.Germanium, 4).inputItems(TagPrefix.dust, GTMaterials.RareEarth, 4)
                .inputFluids(GTLMaterials.RadoxGas.getFluid(21600)).inputFluids(GTMaterials.Oxygen.getFluid(175000))
                .inputFluids(GTLMaterials.Titanium50.getFluid(576))
                .outputFluids(GTLMaterials.Radox.getFluid(10800))
                .EUt(GTValues.VA[14] * 256L).duration(2560).save(provider);
        new GTLAddRecipeBuilder("super_mutated_living_solder", GTLRecipeTypes.QFT_RECIPES).notConsumable("kubejs:spacetime_catalyst")
                .InputItems("256x kubejs:essence_seed").InputItems("256x kubejs:draconium_dust").InputItems("256x ae2:sky_dust").InputItems("4x gtceu:nether_star_dust")
                .inputFluids(GTLMaterials.MutatedLivingSolder.getFluid(100000)).inputFluids(GTMaterials.Biomass.getFluid(1000000))
                .inputFluids(GTMaterials.SterileGrowthMedium.getFluid(1000000))
                .outputFluids(GTLMaterials.SuperMutatedLivingSolder.getFluid(100000))
                .EUt(GTValues.VA[GTValues.MAX]).duration(7200).save(provider);
    }
}
