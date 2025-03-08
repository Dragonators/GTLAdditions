package com.gtladd.gtladditions.data.Recipes;

import org.gtlcore.gtlcore.api.machine.multiblock.GTLCleanroomType;
import org.gtlcore.gtlcore.common.data.GTLMaterials;
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.api.Registry.GTLAddRecipeBuilder;

import java.util.function.Consumer;

public class Distort {

    public Distort() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        new GTLAddRecipeBuilder("rare_earth_dust_monazite", GTLRecipeTypes.DISTORT_RECIPES)
                .notConsumable("gtceu:rhenium_nanoswarm")
                .inputItems(TagPrefix.dust, GTMaterials.Monazite, 64)
                .inputItems(TagPrefix.dust, GTLMaterials.CeriumRichMixturePowder, 288)
                .inputItems(TagPrefix.dust, GTLMaterials.SamariumRefinedPowder, 360)
                .inputFluids(GTMaterials.NitricAcid.getFluid(120000))
                .inputFluids(GTMaterials.Chlorine.getFluid(160000))
                .inputFluids(GTMaterials.Acetone.getFluid(160000))
                .outputItems(TagPrefix.dust, GTMaterials.RareEarth, 1600)
                .outputItems(TagPrefix.dust, GTMaterials.Uranium235, 512)
                .outputItems(TagPrefix.dust, GTMaterials.Samarium, 480)
                .outputItems(TagPrefix.dust, GTMaterials.Phosphorus, 384)
                .outputFluids(GTMaterials.Hydrogen.getFluid(96000))
                .outputFluids(GTMaterials.DilutedHydrochloricAcid.getFluid(96000))
                .outputFluids(GTMaterials.Nitrogen.getFluid(90000))
                .TierEUtVA(9)
                .duration(200)
                .blastFurnaceTemp(12000)
                .cleanroom(CleanroomType.CLEANROOM)
                .save(provider);

        new GTLAddRecipeBuilder("composite_1", GTLRecipeTypes.DISTORT_RECIPES)
                .notConsumable("gtceu:cosmicneutronium_nanoswarm")
                .inputItems(TagPrefix.dust, GTMaterials.Carbon, 1440)
                .inputItems(TagPrefix.dust, GTMaterials.Sulfur, 36)
                .inputItems(TagPrefix.dust, GTMaterials.Silicon, 36)
                .inputFluids(GTMaterials.Hydrogen.getFluid(1908000))
                .inputFluids(GTMaterials.Oxygen.getFluid(144000))
                .inputFluids(GTMaterials.Fluorine.getFluid(144000))
                .inputFluids(GTMaterials.Chlorine.getFluid(36000))
                .outputFluids(GTMaterials.PolyvinylChloride.getFluid(36000))
                .outputFluids(GTMaterials.Polytetrafluoroethylene.getFluid(36000))
                .outputFluids(GTMaterials.SiliconeRubber.getFluid(36000))
                .outputFluids(GTMaterials.PolyphenyleneSulfide.getFluid(36000))
                .outputFluids(GTMaterials.StyreneButadieneRubber.getFluid(36000))
                .outputFluids(GTMaterials.PolyvinylButyral.getFluid(36000))
                .TierEUtVA(12)
                .duration(720)
                .blastFurnaceTemp(43200)
                .cleanroom(GTLCleanroomType.LAW_CLEANROOM)
                .save(provider);

        new GTLAddRecipeBuilder("composite_2", GTLRecipeTypes.DISTORT_RECIPES)
                .notConsumable("gtceu:spacetime_nanoswarm")
                .inputItems(TagPrefix.dust, GTMaterials.Carbon, 1440)
                .inputFluids(GTMaterials.Hydrogen.getFluid(440000))
                .inputFluids(GTMaterials.Oxygen.getFluid(88000))
                .inputFluids(GTMaterials.Nitrogen.getFluid(40000))
                .inputFluids(GTMaterials.Chlorine.getFluid(8000))
                .outputFluids(GTMaterials.Epoxy.getFluid(8000))
                .outputFluids(GTLMaterials.Polyetheretherketone.getFluid(8000))
                .outputFluids(GTMaterials.Polybenzimidazole.getFluid(8000))
                .outputFluids(GTLMaterials.Polyimide.getFluid(8000))
                .TierEUtVA(13)
                .duration(960)
                .blastFurnaceTemp(48000)
                .cleanroom(GTLCleanroomType.LAW_CLEANROOM)
                .save(provider);

        new GTLAddRecipeBuilder("composite_3", GTLRecipeTypes.DISTORT_RECIPES)
                .notConsumable("gtceu:spacetime_nanoswarm")
                .inputItems(TagPrefix.dust, GTMaterials.Carbon, 5828)
                .inputItems(TagPrefix.dust, GTMaterials.Iodine, 64)
                .inputItems(TagPrefix.dust, GTMaterials.Tin, 45)
                .inputItems(TagPrefix.dust, GTMaterials.Silicon, 36)
                .inputItems(TagPrefix.dust, GTMaterials.Palladium, 5)
                .inputItems(TagPrefix.dust, GTMaterials.Nickel, 5)
                .inputItems(TagPrefix.dust, GTMaterials.Iron, 5)
                .inputItems(TagPrefix.dust, GTMaterials.Calcium, 2)
                .inputFluids(GTMaterials.Oxygen.getFluid(2078000))
                .inputFluids(GTMaterials.Hydrogen.getFluid(1269000))
                .inputFluids(GTMaterials.Chlorine.getFluid(982000))
                .inputFluids(GTMaterials.Nitrogen.getFluid(123000))
                .inputFluids(GTMaterials.Fluorine.getFluid(96000))
                .inputFluids(GTMaterials.Methane.getFluid(60000))
                .inputFluids(GTMaterials.Bromine.getFluid(60000))
                .outputItems(TagPrefix.dust, GTLMaterials.UnfoldedFullerene, 64)
                .outputFluids(GTLMaterials.Cycloparaphenylene.getFluid(32000))
                .outputFluids(GTLMaterials.PolyurethaneResin.getFluid(45000))
                .outputFluids(GTLMaterials.LiquidCrystalKevlar.getFluid(45000))
                .outputFluids(GTLMaterials.HydrobromicAcid.getFluid(60000))
                .outputFluids(GTMaterials.Fluorine.getFluid(4800))
                .outputFluids(GTMaterials.Chlorine.getFluid(3200))
                .TierEUtVA(14)
                .duration(1800)
                .blastFurnaceTemp(51200)
                .cleanroom(GTLCleanroomType.LAW_CLEANROOM)
                .save(provider);

        new GTLAddRecipeBuilder("biology_process", GTLRecipeTypes.DISTORT_RECIPES)
                .notConsumable("avaritia:infinity_catalyst")
                .inputItems("gtceu:stem_cells", 16)
                .chancedOutputItems("kubejs:biological_cells", 2048, 25, 0)
                .chancedFluidOutput(GTLMaterials.MutatedLivingSolder.getFluid(18432), "1/4", 0)
                .chancedFluidOutput(GTLMaterials.BiohmediumSterilized.getFluid(256000), "1/4", 0)
                .chancedFluidOutput(GTMaterials.SterileGrowthMedium.getFluid(256000), "1/4", 0)
                .cleanroom(CleanroomType.STERILE_CLEANROOM)
                .duration(1200)
                .blastFurnaceTemp(62000)
                .EUt(GTValues.VA[GTValues.OpV])
                .save(provider);
    }
}
