package com.gtladd.gtladditions.data.recipes.newmachinerecipe

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.data.chemical.material.Material
import com.gregtechceu.gtceu.api.data.tag.TagPrefix
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys
import com.gregtechceu.gtceu.common.data.GTItems
import com.gregtechceu.gtceu.common.data.GTMaterials.*
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.registry.GTLAddRecipeBuilder
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.common.data.GTLMaterials.*
import org.gtlcore.gtlcore.utils.Registries
import java.util.function.Consumer

object AntientropyCondensation {
    @JvmStatic
    fun init(provider : Consumer<FinishedRecipe?>) {
        val fluids = arrayOf<Material>(
            Argon, Helium, Nickel, Iron, Nitrogen, Oxygen, Mithril, Orichalcum, Enderium,
            Adamantium, Infuscolium, Echoite, Vibranium, TaraniumRichLiquidHelium4,
            Legendarium, HeavyQuarkDegenerateMatter, Starmetal,
            QuantumChromodynamicallyConfinedMatter, AstralTitanium, CelestialTungsten
        )
        for (fluid in fluids) {
            GTLAddRecipeBuilder(fluid.name + "_fluid_condenser", ANTIENTROPY_CONDENSATION)
                .inputFluids(fluid.getFluid(FluidStorageKeys.PLASMA, 1000))
                .outputFluids(fluid.getFluid(1000))
                .circuitMeta(1)
                .EUt(GTValues.VA[GTValues.UHV].toLong()).duration(600).save(provider)
        }
        val ingots = arrayOf<Material>(
            Mithril, Orichalcum, Enderium, Adamantium,
            Infuscolium, Echoite, Vibranium,
            Legendarium, HeavyQuarkDegenerateMatter, Starmetal,
            QuantumChromodynamicallyConfinedMatter
        )
        for (ingot in ingots) {
            GTLAddRecipeBuilder(ingot.name + "_ingot_condenser", ANTIENTROPY_CONDENSATION)
                .notConsumable("kubejs:ingot_field_shape")
                .inputFluids(ingot.getFluid(FluidStorageKeys.PLASMA, 144))
                .outputItems(TagPrefix.ingotHot, ingot)
                .EUt(GTValues.VA[GTValues.UHV].toLong()).duration(60).save(provider)
        }
        ANTIENTROPY_CONDENSATION.recipeBuilder(GTLAdditions.id("ice"))
            .inputFluids(Water.getFluid(1000))
            .outputFluids(Ice.getFluid(1000))
            .EUt(GTValues.VA[GTValues.ULV].toLong()).duration(50).save(provider)
        ANTIENTROPY_CONDENSATION.recipeBuilder(GTLAdditions.id("liquid_hydrogen"))
            .inputFluids(Hydrogen.getFluid(1000))
            .outputFluids(LiquidHydrogen.getFluid(1000))
            .EUt(GTValues.VA[GTValues.EV].toLong()).duration(240).save(provider)
        ANTIENTROPY_CONDENSATION.recipeBuilder(GTLAdditions.id("metaastable_oganesson"))
            .inputItems(Registries.getItemStack("kubejs:dust_cryotheum"))
            .inputFluids(HotOganesson.getFluid(4000))
            .outputFluids(MetastableOganesson.getFluid(576))
            .outputItems(TagPrefix.dustSmall, Enderium, 2)
            .EUt(GTValues.VA[GTValues.UV].toLong()).duration(280).save(provider)
        ANTIENTROPY_CONDENSATION.recipeBuilder(GTLAdditions.id("draconium"))
            .inputItems(TagPrefix.ingotHot, Draconium)
            .outputItems(TagPrefix.ingot, Draconium)
            .EUt(GTValues.VA[GTValues.UXV].toLong()).duration(100).save(provider)
        GTLAddRecipeBuilder("fullerene_polymer_matrix_fine_tubing", ANTIENTROPY_CONDENSATION)
            .inputItemString("kubejs:fullerene_polymer_matrix_soft_tubing")
            .outputItems("kubejs:fullerene_polymer_matrix_fine_tubing")
            .EUt(500).duration(240).save(provider)
        GTLAddRecipeBuilder("fuming_nitric_acid", ANTIENTROPY_CONDENSATION)
            .inputFluids(FumingNitricAcid.getFluid(1000))
            .OutputItems("5x gtceu:crystalline_nitric_acid_dust")
            .EUt(GTValues.VA[GTValues.MV].toLong()).duration(180).save(provider)
        GTLAddRecipeBuilder("cosmic_mesh", ANTIENTROPY_CONDENSATION)
            .inputItemString("kubejs:cosmic_mesh_containment_unit")
            .outputItems("kubejs:time_dilation_containment_unit")
            .outputFluids(CosmicMesh.getFluid(FluidStorageKeys.LIQUID, 1000))
            .EUt(GTValues.VA[GTValues.OpV].toLong()).duration(800).save(provider)
        GTLAddRecipeBuilder("iron_ingot", ANTIENTROPY_CONDENSATION)
            .notConsumable("kubejs:ingot_field_shape")
            .inputFluids(Iron.getFluid(FluidStorageKeys.PLASMA, 144))
            .outputItems("minecraft:iron_ingot")
            .EUt(GTValues.VA[GTValues.UHV].toLong()).duration(50).save(provider)
        GTLAddRecipeBuilder("nickel_ingot", ANTIENTROPY_CONDENSATION)
            .notConsumable("kubejs:ingot_field_shape")
            .inputFluids(Nickel.getFluid(FluidStorageKeys.PLASMA, 144))
            .outputItems("gtceu:nickel_ingot")
            .EUt(GTValues.VA[GTValues.UHV].toLong()).duration(50).save(provider)
        GTLAddRecipeBuilder("degenerate_rhenium", ANTIENTROPY_CONDENSATION)
            .inputItemString("kubejs:rhenium_plasma_containment_cell")
            .outputFluids(DegenerateRhenium.getFluid(FluidStorageKeys.LIQUID, 1000))
            .outputItems("kubejs:plasma_containment_cell")
            .EUt(GTValues.VA[GTValues.UEV].toLong()).duration(1200).save(provider)
        GTLAddRecipeBuilder("draconiumawakened", ANTIENTROPY_CONDENSATION)
            .inputItemString("kubejs:draconiumawakened_plasma_containment_cell")
            .outputFluids(DraconiumAwakened.getFluid(1000))
            .outputItems("kubejs:plasma_containment_cell")
            .EUt(GTValues.VA[GTValues.UXV].toLong()).duration(1200).save(provider)
        GTLAddRecipeBuilder("neutronium_sphere", ANTIENTROPY_CONDENSATION)
            .inputItemString("kubejs:neutron_plasma_containment_cell")
            .OutputItems("4x kubejs:neutronium_sphere")
            .outputItems("kubejs:plasma_containment_cell")
            .EUt(GTValues.VA[GTValues.UHV].toLong()).duration(800).save(provider)
        GTLAddRecipeBuilder("quantumchromodynamic_protective_plating", ANTIENTROPY_CONDENSATION)
            .notConsumable("gtceu:vibranium_nanoswarm")
            .notConsumable("gtceu:infuscolium_nanoswarm")
            .inputFluids(HighEnergyQuarkGluon.getFluid(100))
            .outputItems("kubejs:quantumchromodynamic_protective_plating")
            .EUt(GTValues.VA[GTValues.UXV].toLong()).duration(300).save(provider)
        GTLAddRecipeBuilder("cosmicneutronium", ANTIENTROPY_CONDENSATION)
            .inputItemString("kubejs:cosmic_neutron_plasma_cell")
            .outputFluids(CosmicNeutronium.getFluid(1000))
            .outputItems("kubejs:extremely_durable_plasma_cell")
            .EUt(GTValues.VA[GTValues.OpV].toLong()).duration(1200).save(provider)
        GTLAddRecipeBuilder("crystalmatrix", ANTIENTROPY_CONDENSATION)
            .inputItemString("kubejs:crystalmatrix_plasma_containment_cell")
            .outputFluids(Crystalmatrix.getFluid(1000))
            .outputItems("kubejs:plasma_containment_cell")
            .EUt(GTValues.VA[GTValues.OpV].toLong()).duration(1000).save(provider)
        GTLAddRecipeBuilder("chaos", ANTIENTROPY_CONDENSATION)
            .inputItemString("kubejs:chaos_containment_unit")
            .outputFluids(Chaos.getFluid(1000))
            .outputItems("kubejs:time_dilation_containment_unit")
            .EUt(GTValues.VA[GTValues.OpV].toLong()).duration(1600).save(provider)
        GTLAddRecipeBuilder("metastable_hassium", ANTIENTROPY_CONDENSATION)
            .inputFluids(MetastableHassium.getFluid(FluidStorageKeys.PLASMA, 1000))
            .outputFluids(MetastableHassium.getFluid(FluidStorageKeys.LIQUID, 1000))
            .EUt(GTValues.VA[GTValues.UHV].toLong()).duration(1200).save(provider)
        GTLAddRecipeBuilder("actinium_superhydride_dust", ANTIENTROPY_CONDENSATION)
            .inputItemString("kubejs:actinium_superhydride_plasma_containment_cell")
            .OutputItems("13x gtceu:actinium_superhydride_dust")
            .outputItems("kubejs:plasma_containment_cell")
            .EUt(GTValues.VA[GTValues.UIV].toLong()).duration(340).save(provider)
        GTLAddRecipeBuilder("grade_14_purified_water", ANTIENTROPY_CONDENSATION)
            .notConsumable(GTItems.FLUID_REGULATOR_UHV)
            .inputFluids(GradePurifiedWater13.getFluid(10000))
            .inputFluids(Mithril.getFluid(FluidStorageKeys.PLASMA, 1000))
            .OutputItems("60x gtceu:tiny_mithril_dust")
            .outputFluids(GradePurifiedWater14.getFluid(9900))
            .EUt(GTValues.VA[GTValues.UHV].toLong()).duration(800).save(provider)
        GTLAddRecipeBuilder("grade_15_purified_water", ANTIENTROPY_CONDENSATION)
            .notConsumable(GTItems.FLUID_REGULATOR_UEV)
            .inputFluids(GradePurifiedWater14.getFluid(10000))
            .inputFluids(Enderium.getFluid(FluidStorageKeys.PLASMA, 1000))
            .OutputItems("61x gtceu:tiny_enderium_dust")
            .outputFluids(GradePurifiedWater15.getFluid(9990))
            .EUt(GTValues.VA[GTValues.UEV].toLong()).duration(800).save(provider)
        GTLAddRecipeBuilder("grade_16_purified_water", ANTIENTROPY_CONDENSATION)
            .notConsumable(GTItems.FLUID_REGULATOR_UIV)
            .inputFluids(GradePurifiedWater15.getFluid(10000))
            .inputFluids(Echoite.getFluid(FluidStorageKeys.PLASMA, 1000))
            .OutputItems("62x gtceu:tiny_echoite_dust")
            .outputFluids(GradePurifiedWater16.getFluid(9999))
            .EUt(GTValues.VA[GTValues.UIV].toLong()).duration(800).save(provider)
        addRecipe(Helium, provider)
        addRecipe(Oxygen, provider)
    }

    private fun addRecipe(material : Material, provider : Consumer<FinishedRecipe?>) {
        ANTIENTROPY_CONDENSATION.recipeBuilder(GTLAdditions.id(material.name))
            .inputFluids(material.getFluid(1000))
            .outputFluids(material.getFluid(FluidStorageKeys.LIQUID, 1000))
            .EUt(GTValues.VA[GTValues.HV].toLong()).duration(240).save(provider)
    }
}
