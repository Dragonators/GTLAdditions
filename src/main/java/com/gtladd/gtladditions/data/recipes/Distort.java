package com.gtladd.gtladditions.data.recipes;

import org.gtlcore.gtlcore.api.machine.multiblock.GTLCleanroomType;
import org.gtlcore.gtlcore.common.data.GTLMaterials;
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.api.registry.GTLAddRecipeBuilder;

import java.util.function.Consumer;

public class Distort {

    public Distort() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        new GTLAddRecipeBuilder("rare_earth_dust_monazite", GTLRecipeTypes.DISTORT_RECIPES).notConsumable("gtceu:rhenium_nanoswarm")
                .inputItems(TagPrefix.dust, GTMaterials.Monazite, 64).inputItems(TagPrefix.dust, GTLMaterials.CeriumRichMixturePowder, 288)
                .inputItems(TagPrefix.dust, GTLMaterials.SamariumRefinedPowder, 360).inputFluids(GTMaterials.NitricAcid.getFluid(120000))
                .inputFluids(GTMaterials.Chlorine.getFluid(160000)).inputFluids(GTMaterials.Acetone.getFluid(160000))
                .outputItems(TagPrefix.dust, GTMaterials.RareEarth, 1600).outputItems(TagPrefix.dust, GTMaterials.Uranium235, 512)
                .outputItems(TagPrefix.dust, GTMaterials.Samarium, 480).outputItems(TagPrefix.dust, GTMaterials.Phosphorus, 384)
                .outputFluids(GTMaterials.Hydrogen.getFluid(96000)).outputFluids(GTMaterials.DilutedHydrochloricAcid.getFluid(96000))
                .outputFluids(GTMaterials.Nitrogen.getFluid(90000))
                .TierEUtVA(9).duration(200).blastFurnaceTemp(12000).cleanroom(CleanroomType.CLEANROOM).save(provider);
        new GTLAddRecipeBuilder("composite_1", GTLRecipeTypes.DISTORT_RECIPES).notConsumable("gtceu:cosmicneutronium_nanoswarm")
                .inputItems(TagPrefix.dust, GTMaterials.Carbon, 1440).inputItems(TagPrefix.dust, GTMaterials.Sulfur, 36)
                .inputItems(TagPrefix.dust, GTMaterials.Silicon, 36).inputFluids(GTMaterials.Hydrogen.getFluid(1908000))
                .inputFluids(GTMaterials.Oxygen.getFluid(144000)).inputFluids(GTMaterials.Fluorine.getFluid(144000))
                .inputFluids(GTMaterials.Chlorine.getFluid(36000))
                .outputFluids(GTMaterials.PolyvinylChloride.getFluid(36000)).outputFluids(GTMaterials.Polytetrafluoroethylene.getFluid(36000))
                .outputFluids(GTMaterials.SiliconeRubber.getFluid(36000)).outputFluids(GTMaterials.PolyphenyleneSulfide.getFluid(36000))
                .outputFluids(GTMaterials.StyreneButadieneRubber.getFluid(36000)).outputFluids(GTMaterials.PolyvinylButyral.getFluid(36000))
                .TierEUtVA(12).duration(720).blastFurnaceTemp(43200).cleanroom(GTLCleanroomType.LAW_CLEANROOM).save(provider);
        new GTLAddRecipeBuilder("composite_2", GTLRecipeTypes.DISTORT_RECIPES).notConsumable("gtceu:spacetime_nanoswarm")
                .inputItems(TagPrefix.dust, GTMaterials.Carbon, 1440).inputFluids(GTMaterials.Hydrogen.getFluid(440000))
                .inputFluids(GTMaterials.Oxygen.getFluid(88000)).inputFluids(GTMaterials.Nitrogen.getFluid(40000))
                .inputFluids(GTMaterials.Chlorine.getFluid(8000))
                .outputFluids(GTMaterials.Epoxy.getFluid(8000)).outputFluids(GTLMaterials.Polyetheretherketone.getFluid(8000))
                .outputFluids(GTMaterials.Polybenzimidazole.getFluid(8000)).outputFluids(GTLMaterials.Polyimide.getFluid(8000))
                .TierEUtVA(13).duration(960).blastFurnaceTemp(48000).cleanroom(GTLCleanroomType.LAW_CLEANROOM).save(provider);
        new GTLAddRecipeBuilder("composite_3", GTLRecipeTypes.DISTORT_RECIPES).notConsumable("gtceu:transcendentmetal_nanoswarm")
                .inputItems(TagPrefix.dust, GTMaterials.Carbon, 5828).inputItems(TagPrefix.dust, GTMaterials.Iodine, 64)
                .inputItems(TagPrefix.dust, GTMaterials.Tin, 45).inputItems(TagPrefix.dust, GTMaterials.Silicon, 36)
                .inputItems(TagPrefix.dust, GTMaterials.Palladium, 5).inputItems(TagPrefix.dust, GTMaterials.Nickel, 5)
                .inputItems(TagPrefix.dust, GTMaterials.Iron, 5).inputItems(TagPrefix.dust, GTMaterials.Calcium, 2)
                .inputFluids(GTMaterials.Oxygen.getFluid(2078000)).inputFluids(GTMaterials.Hydrogen.getFluid(1269000))
                .inputFluids(GTMaterials.Chlorine.getFluid(982000)).inputFluids(GTMaterials.Nitrogen.getFluid(123000))
                .inputFluids(GTMaterials.Fluorine.getFluid(96000)).inputFluids(GTMaterials.Methane.getFluid(60000))
                .inputFluids(GTMaterials.Bromine.getFluid(60000))
                .outputItems(TagPrefix.dust, GTLMaterials.UnfoldedFullerene, 64).outputFluids(GTLMaterials.Cycloparaphenylene.getFluid(32000))
                .outputFluids(GTLMaterials.PolyurethaneResin.getFluid(45000)).outputFluids(GTLMaterials.LiquidCrystalKevlar.getFluid(45000))
                .outputFluids(GTLMaterials.HydrobromicAcid.getFluid(60000)).outputFluids(GTMaterials.Fluorine.getFluid(4800))
                .outputFluids(GTMaterials.Chlorine.getFluid(3200))
                .TierEUtVA(14).duration(1800).blastFurnaceTemp(51200).cleanroom(GTLCleanroomType.LAW_CLEANROOM).save(provider);
        new GTLAddRecipeBuilder("biology_process", GTLRecipeTypes.DISTORT_RECIPES).notConsumable("avaritia:infinity_catalyst")
                .inputItems("gtceu:stem_cells", 256).InputItems("256x kubejs:tcetieseaweedextract").InputItems("256x gtceu:agar_dust")
                .OutputItems("8192x kubejs:biological_cells")
                .outputFluids(GTLMaterials.MutatedLivingSolder.getFluid(73728))
                .outputFluids(GTLMaterials.BiohmediumSterilized.getFluid(1024000))
                .outputFluids(GTMaterials.SterileGrowthMedium.getFluid(1024000))
                .cleanroom(CleanroomType.STERILE_CLEANROOM).duration(1200).blastFurnaceTemp(57600).EUt(GTValues.VA[GTValues.OpV]).save(provider);
        new GTLAddRecipeBuilder("rhenium_dust", GTLRecipeTypes.DISTORT_RECIPES).notConsumable("gtceu:rhenium_nanoswarm")
                .InputItems("48x gtceu:molybdenite_dust").InputItems("3x gtceu:iron_dust")
                .OutputItems("48x gtceu:rhenium_dust").OutputItems("16x gtceu:gold_dust").OutputItems("16x gtceu:molybdenum_dust")
                .inputFluids(GTMaterials.Hydrogen.getFluid(112000)).inputFluids(GTMaterials.Oxygen.getFluid(68000))
                .inputFluids(GTMaterials.Chlorine.getFluid(12000)).outputFluids(GTMaterials.HydrogenSulfide.getFluid(48000))
                .TierEUtVA(11).duration(960).blastFurnaceTemp(14400).cleanroom(CleanroomType.STERILE_CLEANROOM).save(provider);
        new GTLAddRecipeBuilder("bedrock_gas", GTLRecipeTypes.DISTORT_RECIPES).notConsumable("gtceu:uruium_nanoswarm")
                .InputItems("72x gtceu:bedrock_dust").InputItems("63x gtceu:naquadah_dust")
                .OutputItems("4x gtceu:enriched_naquadah_dust").OutputItems("4x gtceu:naquadria_dust")
                .inputFluids(GTMaterials.DistilledWater.getFluid(72000)).inputFluids(GTMaterials.Xenon.getFluid(7200))
                .outputFluids(GTLMaterials.BedrockGas.getFluid(36000))
                .TierEUtVA(12).duration(1280).blastFurnaceTemp(18000).cleanroom(GTLCleanroomType.LAW_CLEANROOM).save(provider);
        new GTLAddRecipeBuilder("trinium_compound", GTLRecipeTypes.DISTORT_RECIPES)
                .notConsumable("gtceu:starmetal_nanoswarm").notConsumable("gtceu:fluorocarborane_dust", 50)
                .InputItems("360x gtceu:trinium_compound_dust").InputItems("512x gtceu:sodium_hydroxide_dust")
                .InputItems("16x gtceu:fullerene_dust").InputItems("16x gtceu:carbon_nanotubes_ingot")
                .notConsumableFluid(GTLMaterials.Perfluorobenzene.getFluid(2000))
                .inputFluids(GTMaterials.NitricAcid.getFluid(300000)).inputFluids(GTLMaterials.HydrogenPeroxide.getFluid(12000))
                .inputFluids(GTMaterials.SulfurDioxide.getFluid(64000)).inputFluids(GTMaterials.Chlorine.getFluid(64000))
                .OutputItems("360x gtceu:trinium_dust").OutputItems("360x gtceu:actinium_dust").OutputItems("320x gtceu:selenium_dust")
                .OutputItems("320x gtceu:astatine_dust").OutputItems("64x gtceu:salt_dust")
                .outputFluids(GTLMaterials.ResidualTriniiteSolution.getFluid(128000)).outputFluids(GTLMaterials.ActiniumRadiumNitrateSolution.getFluid(320000))
                .TierEUtVA(12).duration(1200).blastFurnaceTemp(18000).cleanroom(GTLCleanroomType.LAW_CLEANROOM).save(provider);
        new GTLAddRecipeBuilder("agar_dust", GTLRecipeTypes.DISTORT_RECIPES).notConsumable("gtceu:neutronium_nanoswarm")
                .InputItems("256x gtceu:meat_dust").InputItems("225x minecraft:bone").InputItems("64x gtceu:sulfur_dust").InputItems("16x gtceu:phosphorus_dust")
                .inputFluids(GTMaterials.DistilledWater.getFluid(128000)).inputFluids(GTMaterials.Oxygen.getFluid(128000))
                .OutputItems("384x gtceu:agar_dust")
                .TierEUtVA(9).duration(2560).blastFurnaceTemp(16000).cleanroom(CleanroomType.STERILE_CLEANROOM).save(provider);
        new GTLAddRecipeBuilder("tcetieseaweedextract", GTLRecipeTypes.DISTORT_RECIPES).notConsumable("gtceu:infuscolium_nanoswarm")
                .InputItems("450x gtceu:salt_dust").InputItems("450x gtceu:agar_dust").InputItems("450x gtceu:meat_dust").InputItems("228x minecraft:kelp")
                .InputItems("90x gtceu:alien_algae_dust").InputItems("36x gtceu:energium_dust").InputItems("4x gtceu:mithril_dust")
                .inputFluids(GTLMaterials.UnknowWater.getFluid(112000)).inputFluids(GTMaterials.Methane.getFluid(225000))
                .inputFluids(GTMaterials.Naphthalene.getFluid(90000)).inputFluids(GTMaterials.Oxygen.getFluid(180000))
                .OutputItems("256x kubejs:tcetieseaweedextract")
                .TierEUtVA(11).duration(1200).blastFurnaceTemp(18000).cleanroom(GTLCleanroomType.LAW_CLEANROOM).save(provider);
    }
}
