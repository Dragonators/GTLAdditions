package com.gtladd.gtladditions.data.recipes.newmachinerecipe;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gtladd.gtladditions.api.recipe.GTLAddRecipesTypes;
import com.gtladd.gtladditions.api.registry.GTLAddRecipeBuilder;
import net.minecraft.data.recipes.FinishedRecipe;
import org.gtlcore.gtlcore.common.data.GTLMaterials;

import java.util.function.Consumer;

public class AntientropyCondensation {
    public AntientropyCondensation() {}
    public static void init(Consumer<FinishedRecipe> provider) {
        Material[] fluids = {GTMaterials.Argon, GTMaterials.Helium, GTMaterials.Nickel, GTMaterials.Iron,
                GTMaterials.Nitrogen, GTMaterials.Oxygen, GTLMaterials.Mithril, GTLMaterials.Orichalcum, GTLMaterials.Enderium,
                GTLMaterials.Adamantium, GTLMaterials.Infuscolium, GTLMaterials.Echoite, GTLMaterials.Vibranium, GTLMaterials.TaraniumRichLiquidHelium4,
                GTLMaterials.Legendarium, GTLMaterials.HeavyQuarkDegenerateMatter, GTLMaterials.Starmetal,
                GTLMaterials.QuantumChromodynamicallyConfinedMatter, GTLMaterials.AstralTitanium, GTLMaterials.CelestialTungsten};
        for (Material fluid : fluids) {
            new GTLAddRecipeBuilder(fluid.getName() + "_fluid_condenser", GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION)
                    .inputFluids(fluid.getFluid(FluidStorageKeys.PLASMA, 1000)).outputFluids(fluid.getFluid(1000)).circuitMeta(1)
                    .EUt(GTValues.VA[GTValues.UHV]).duration(600).save(provider);
        }
        Material[] ingots = {GTLMaterials.Mithril, GTLMaterials.Orichalcum, GTLMaterials.Enderium, GTLMaterials.Adamantium,
                GTLMaterials.Infuscolium, GTLMaterials.Echoite, GTLMaterials.Vibranium,
                GTLMaterials.Legendarium, GTLMaterials.HeavyQuarkDegenerateMatter, GTLMaterials.Starmetal,
                GTLMaterials.QuantumChromodynamicallyConfinedMatter};
        for (Material ingot : ingots) {
            new GTLAddRecipeBuilder(ingot.getName() + "_ingot_condenser", GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION).notConsumable("kubejs:ingot_field_shape")
                    .inputFluids(ingot.getFluid(FluidStorageKeys.PLASMA, 144)).outputItems(TagPrefix.ingotHot, ingot)
                    .EUt(GTValues.VA[GTValues.UHV]).duration(60).save(provider);
        }
        GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION.recipeBuilder("ice")
                .inputFluids(GTMaterials.Water.getFluid(1000)).outputFluids(GTMaterials.Ice.getFluid(1000))
                .EUt(GTValues.VA[GTValues.ULV]).duration(50).save(provider);
        GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION.recipeBuilder("liquid_hydrogen")
                .inputFluids(GTMaterials.Hydrogen.getFluid(1000)).outputFluids(GTLMaterials.LiquidHydrogen.getFluid(1000))
                .EUt(GTValues.VA[GTValues.EV]).duration(240).save(provider);
        new GTLAddRecipeBuilder("fullerene_polymer_matrix_fine_tubing", GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION)
                .inputItems("kubejs:fullerene_polymer_matrix_soft_tubing").outputItems("kubejs:fullerene_polymer_matrix_fine_tubing")
                .EUt(500).duration(240).save(provider);
        new GTLAddRecipeBuilder("fuming_nitric_acid", GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION)
                .inputFluids(GTLMaterials.FumingNitricAcid.getFluid(1000)).OutputItems("5x gtceu:crystalline_nitric_acid_dust")
                .EUt(GTValues.VA[GTValues.MV]).duration(180).save(provider);
        new GTLAddRecipeBuilder("cosmic_mesh", GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION)
                .inputItems("kubejs:cosmic_mesh_containment_unit")
                .outputItems("kubejs:time_dilation_containment_unit").outputFluids(GTLMaterials.CosmicMesh.getFluid(FluidStorageKeys.LIQUID, 1000))
                .EUt(GTValues.VA[GTValues.OpV]).duration(800).save(provider);
        new GTLAddRecipeBuilder("iron_ingot", GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION)
                .notConsumable("kubejs:ingot_field_shape")
                .inputFluids(GTMaterials.Iron.getFluid(FluidStorageKeys.PLASMA, 144)).outputItems("minecraft:iron_ingot")
                .EUt(GTValues.VA[GTValues.UHV]).duration(50).save(provider);
        new GTLAddRecipeBuilder("nickel_ingot", GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION)
                .notConsumable("kubejs:ingot_field_shape")
                .inputFluids(GTMaterials.Nickel.getFluid(FluidStorageKeys.PLASMA, 144)).outputItems("gtceu:nickel_ingot")
                .EUt(GTValues.VA[GTValues.UHV]).duration(50).save(provider);
        new GTLAddRecipeBuilder("degenerate_rhenium", GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION)
                .inputItems("kubejs:rhenium_plasma_containment_cell")
                .outputFluids(GTLMaterials.DegenerateRhenium.getFluid(FluidStorageKeys.LIQUID, 1000)).outputItems("kubejs:plasma_containment_cell")
                .EUt(GTValues.VA[GTValues.UEV]).duration(1200).save(provider);
        new GTLAddRecipeBuilder("draconiumawakened", GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION)
                .inputItems("kubejs:draconiumawakened_plasma_containment_cell")
                .outputFluids(GTLMaterials.DraconiumAwakened.getFluid(1000)).outputItems("kubejs:plasma_containment_cell")
                .EUt(GTValues.VA[GTValues.UXV]).duration(1200).save(provider);
        new GTLAddRecipeBuilder("neutronium_sphere", GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION)
                .inputItems("kubejs:neutron_plasma_containment_cell")
                .OutputItems("4x kubejs:neutronium_sphere").outputItems("kubejs:plasma_containment_cell")
                .EUt(GTValues.VA[GTValues.UHV]).duration(800).save(provider);
        new GTLAddRecipeBuilder("quantumchromodynamic_protective_plating", GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION)
                .notConsumable("gtceu:vibranium_nanoswarm").notConsumable("gtceu:infuscolium_nanoswarm")
                .inputFluids(GTLMaterials.HighEnergyQuarkGluon.getFluid(100))
                .outputItems("kubejs:quantumchromodynamic_protective_plating")
                .EUt(GTValues.VA[GTValues.UXV]).duration(300).save(provider);
        new GTLAddRecipeBuilder("cosmicneutronium", GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION)
                .inputItems("kubejs:cosmic_neutron_plasma_cell")
                .outputFluids(GTLMaterials.CosmicNeutronium.getFluid(1000)).outputItems("kubejs:extremely_durable_plasma_cell")
                .EUt(GTValues.VA[GTValues.OpV]).duration(1200).save(provider);
        new GTLAddRecipeBuilder("crystalmatrix", GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION)
                .inputItems("kubejs:crystalmatrix_plasma_containment_cell")
                .outputFluids(GTLMaterials.Crystalmatrix.getFluid(1000)).outputItems("kubejs:plasma_containment_cell")
                .EUt(GTValues.VA[GTValues.OpV]).duration(1000).save(provider);
        new GTLAddRecipeBuilder("chaos", GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION)
                .inputItems("kubejs:chaos_containment_unit")
                .outputFluids(GTLMaterials.Chaos.getFluid(1000)).outputItems("kubejs:time_dilation_containment_unit")
                .EUt(GTValues.VA[GTValues.OpV]).duration(1600).save(provider);
        new GTLAddRecipeBuilder("metastable_hassium", GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION)
                .inputFluids(GTLMaterials.MetastableHassium.getFluid(FluidStorageKeys.PLASMA, 1000))
                .outputFluids(GTLMaterials.MetastableHassium.getFluid(FluidStorageKeys.LIQUID, 1000))
                .EUt(GTValues.VA[GTValues.UHV]).duration(1200).save(provider);
        new GTLAddRecipeBuilder("actinium_superhydride_dust", GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION)
                .inputItems("kubejs:actinium_superhydride_plasma_containment_cell")
                .OutputItems("13x gtceu:actinium_superhydride_dust").outputItems("kubejs:plasma_containment_cell")
                .EUt(GTValues.VA[GTValues.UIV]).duration(340).save(provider);
        new GTLAddRecipeBuilder("grade_14_purified_water", GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION)
                .notConsumable(GTItems.FLUID_REGULATOR_UHV)
                .inputFluids(GTLMaterials.GradePurifiedWater13.getFluid(10000)).inputFluids(GTLMaterials.Mithril.getFluid(FluidStorageKeys.PLASMA, 1000))
                .OutputItems("60x gtceu:tiny_mithril_dust").outputFluids(GTLMaterials.GradePurifiedWater14.getFluid(9900))
                .EUt(GTValues.VA[GTValues.UHV]).duration(800).save(provider);
        new GTLAddRecipeBuilder("grade_15_purified_water", GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION)
                .notConsumable(GTItems.FLUID_REGULATOR_UEV)
                .inputFluids(GTLMaterials.GradePurifiedWater14.getFluid(10000)).inputFluids(GTLMaterials.Enderium.getFluid(FluidStorageKeys.PLASMA, 1000))
                .OutputItems("61x gtceu:tiny_enderium_dust").outputFluids(GTLMaterials.GradePurifiedWater15.getFluid(9990))
                .EUt(GTValues.VA[GTValues.UEV]).duration(800).save(provider);
        new GTLAddRecipeBuilder("grade_16_purified_water", GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION)
                .notConsumable(GTItems.FLUID_REGULATOR_UIV)
                .inputFluids(GTLMaterials.GradePurifiedWater15.getFluid(10000)).inputFluids(GTLMaterials.Echoite.getFluid(FluidStorageKeys.PLASMA, 1000))
                .OutputItems("62x gtceu:tiny_echoite_dust").outputFluids(GTLMaterials.GradePurifiedWater16.getFluid(9999))
                .EUt(GTValues.VA[GTValues.UIV]).duration(800).save(provider);
        addRecipe(GTMaterials.Helium, provider);
        addRecipe(GTMaterials.Oxygen, provider);
    }
    private static void addRecipe(Material material, Consumer<FinishedRecipe> provider) {
        GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION.recipeBuilder(material.getName())
                .inputFluids(material.getFluid(1000)).outputFluids(material.getFluid(FluidStorageKeys.LIQUID, 1000))
                .EUt(GTValues.VA[GTValues.HV]).duration(240).save(provider);
    }
}
