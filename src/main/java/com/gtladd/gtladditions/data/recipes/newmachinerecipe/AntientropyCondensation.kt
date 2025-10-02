package com.gtladd.gtladditions.data.recipes.newmachinerecipe

import com.gregtechceu.gtceu.api.GTValues.*
import com.gregtechceu.gtceu.api.data.chemical.material.Material
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.*
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys.LIQUID
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys.PLASMA
import com.gregtechceu.gtceu.common.data.GTItems
import com.gregtechceu.gtceu.common.data.GTMaterials.*
import com.gtladd.gtladditions.GTLAdditions.id
import com.gtladd.gtladditions.common.material.GTLAddMaterial
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.common.data.GTLMaterials.*
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import java.util.function.Consumer

object AntientropyCondensation {
    @JvmStatic
    fun init(provider : Consumer<FinishedRecipe?>) {
        val fluids = arrayOf<Material>(
            Argon, Helium, Nickel, Iron, Nitrogen, Oxygen, Mithril, Orichalcum, Enderium,
            Adamantium, Infuscolium, Echoite, Vibranium, TaraniumRichLiquidHelium4,
            Legendarium, HeavyQuarkDegenerateMatter, Starmetal,
            QuantumChromodynamicallyConfinedMatter, AstralTitanium, CelestialTungsten, GTLAddMaterial.CREON
        )
        for (fluid in fluids) {
            ANTIENTROPY_CONDENSATION.recipeBuilder(id(fluid.name + "_fluid_condenser"))
                .inputFluids(fluid.getFluid(PLASMA, 1000))
                .outputFluids(fluid.getFluid(1000))
                .circuitMeta(1)
                .EUt(VA[UHV].toLong()).duration(600).save(provider)
        }
        val ingots = arrayOf<Material>(
            Mithril, Orichalcum, Enderium, Adamantium,
            Infuscolium, Echoite, Vibranium,
            Legendarium, HeavyQuarkDegenerateMatter, Starmetal,
            QuantumChromodynamicallyConfinedMatter
        )
        for (ingot in ingots) {
            ANTIENTROPY_CONDENSATION.recipeBuilder(id(ingot.name + "_ingot_condenser"))
                .notConsumable(getItemStack("kubejs:ingot_field_shape"))
                .inputFluids(ingot.getFluid(PLASMA, 144))
                .outputItems(ingotHot, ingot)
                .EUt(VA[UHV].toLong()).duration(60).save(provider)
        }
        ANTIENTROPY_CONDENSATION.recipeBuilder(id("ice"))
            .inputFluids(Water.getFluid(1000))
            .outputFluids(Ice.getFluid(1000))
            .EUt(VA[ULV].toLong()).duration(50).save(provider)
        ANTIENTROPY_CONDENSATION.recipeBuilder(id("liquid_hydrogen"))
            .inputFluids(Hydrogen.getFluid(1000))
            .outputFluids(LiquidHydrogen.getFluid(1000))
            .EUt(VA[EV].toLong()).duration(240).save(provider)
        ANTIENTROPY_CONDENSATION.recipeBuilder(id("metaastable_oganesson"))
            .inputItems(getItemStack("kubejs:dust_cryotheum"))
            .inputFluids(HotOganesson.getFluid(4000))
            .outputFluids(MetastableOganesson.getFluid(576))
            .outputItems(dustSmall, Enderium, 2)
            .EUt(VA[UV].toLong()).duration(280).save(provider)
        ANTIENTROPY_CONDENSATION.recipeBuilder(id("draconium"))
            .inputItems(ingotHot, Draconium)
            .outputItems(ingot, Draconium)
            .EUt(VA[UXV].toLong()).duration(100).save(provider)
        ANTIENTROPY_CONDENSATION.recipeBuilder(id("fullerene_polymer_matrix_fine_tubing"))
            .inputItems(getItemStack("kubejs:fullerene_polymer_matrix_soft_tubing"))
            .outputItems(getItemStack("kubejs:fullerene_polymer_matrix_fine_tubing"))
            .EUt(500).duration(240).save(provider)
        ANTIENTROPY_CONDENSATION.recipeBuilder(id("fuming_nitric_acid"))
            .outputItems(dust, CrystallineNitricAcid, 5)
            .inputFluids(FumingNitricAcid.getFluid(1000))
            .EUt(VA[MV].toLong()).duration(180).save(provider)
        ANTIENTROPY_CONDENSATION.recipeBuilder(id("cosmic_mesh"))
            .inputItems(getItemStack("kubejs:cosmic_mesh_containment_unit"))
            .outputItems(getItemStack("kubejs:time_dilation_containment_unit"))
            .outputFluids(CosmicMesh.getFluid(LIQUID, 1000))
            .EUt(VA[OpV].toLong()).duration(800).save(provider)
        ANTIENTROPY_CONDENSATION.recipeBuilder(id("iron_ingot"))
            .notConsumable(getItemStack("kubejs:ingot_field_shape"))
            .outputItems(getItemStack("minecraft:iron_ingot"))
            .inputFluids(Iron.getFluid(PLASMA, 144))
            .EUt(VA[UHV].toLong()).duration(50).save(provider)
        ANTIENTROPY_CONDENSATION.recipeBuilder(id("nickel_ingot"))
            .notConsumable(getItemStack("kubejs:ingot_field_shape"))
            .outputItems(ingot, Nickel)
            .inputFluids(Nickel.getFluid(PLASMA, 144))
            .EUt(VA[UHV].toLong()).duration(50).save(provider)
        ANTIENTROPY_CONDENSATION.recipeBuilder(id("degenerate_rhenium"))
            .inputItems(getItemStack("kubejs:rhenium_plasma_containment_cell"))
            .outputItems(getItemStack("kubejs:plasma_containment_cell"))
            .outputFluids(DegenerateRhenium.getFluid(LIQUID, 1000))
            .EUt(VA[UEV].toLong()).duration(1200).save(provider)
        ANTIENTROPY_CONDENSATION.recipeBuilder(id("draconiumawakened"))
            .inputItems(getItemStack("kubejs:draconiumawakened_plasma_containment_cell"))
            .outputItems(getItemStack("kubejs:plasma_containment_cell"))
            .outputFluids(DraconiumAwakened.getFluid(1000))
            .EUt(VA[UXV].toLong()).duration(1200).save(provider)
        ANTIENTROPY_CONDENSATION.recipeBuilder(id("neutronium_sphere"))
            .inputItems(getItemStack("kubejs:neutron_plasma_containment_cell"))
            .outputItems(getItemStack("kubejs:neutronium_sphere", 4))
            .outputItems(getItemStack("kubejs:plasma_containment_cell"))
            .EUt(VA[UHV].toLong()).duration(800).save(provider)
        ANTIENTROPY_CONDENSATION.recipeBuilder(id("quantumchromodynamic_protective_plating"))
            .notConsumable(getItemStack("gtceu:vibranium_nanoswarm"))
            .notConsumable(getItemStack("gtceu:infuscolium_nanoswarm"))
            .outputItems(getItemStack("kubejs:quantumchromodynamic_protective_plating"))
            .inputFluids(HighEnergyQuarkGluon.getFluid(100))
            .EUt(VA[UXV].toLong()).duration(300).save(provider)
        ANTIENTROPY_CONDENSATION.recipeBuilder(id("cosmicneutronium"))
            .inputItems(getItemStack("kubejs:cosmic_neutron_plasma_cell"))
            .outputItems(getItemStack("kubejs:extremely_durable_plasma_cell"))
            .outputFluids(CosmicNeutronium.getFluid(1000))
            .EUt(VA[OpV].toLong()).duration(1200).save(provider)
        ANTIENTROPY_CONDENSATION.recipeBuilder(id("crystalmatrix"))
            .inputItems(getItemStack("kubejs:crystalmatrix_plasma_containment_cell"))
            .outputItems(getItemStack("kubejs:plasma_containment_cell"))
            .outputFluids(Crystalmatrix.getFluid(1000))
            .EUt(VA[OpV].toLong()).duration(1000).save(provider)
        ANTIENTROPY_CONDENSATION.recipeBuilder(id("chaos"))
            .inputItems(getItemStack("kubejs:chaos_containment_unit"))
            .outputItems(getItemStack("kubejs:time_dilation_containment_unit"))
            .outputFluids(Chaos.getFluid(1000))
            .EUt(VA[OpV].toLong()).duration(1600).save(provider)
        ANTIENTROPY_CONDENSATION.recipeBuilder(id("metastable_hassium"))
            .inputFluids(MetastableHassium.getFluid(PLASMA, 1000))
            .outputFluids(MetastableHassium.getFluid(LIQUID, 1000))
            .EUt(VA[UHV].toLong()).duration(1200).save(provider)
        ANTIENTROPY_CONDENSATION.recipeBuilder(id("actinium_superhydride_dust"))
            .inputItems(getItemStack("kubejs:actinium_superhydride_plasma_containment_cell"))
            .outputItems(dust, ActiniumSuperhydride, 13)
            .outputItems(getItemStack("kubejs:plasma_containment_cell"))
            .EUt(VA[UIV].toLong()).duration(340).save(provider)
        ANTIENTROPY_CONDENSATION.recipeBuilder(id("grade_14_purified_water"))
            .notConsumable(GTItems.FLUID_REGULATOR_UHV)
            .inputFluids(GradePurifiedWater13.getFluid(10000))
            .inputFluids(Mithril.getFluid(PLASMA, 1000))
            .outputItems(dustTiny, Mithril, 60)
            .outputFluids(GradePurifiedWater14.getFluid(9900))
            .EUt(VA[UHV].toLong()).duration(800).save(provider)
        ANTIENTROPY_CONDENSATION.recipeBuilder(id("grade_15_purified_water"))
            .notConsumable(GTItems.FLUID_REGULATOR_UEV)
            .inputFluids(GradePurifiedWater14.getFluid(10000))
            .inputFluids(Enderium.getFluid(PLASMA, 1000))
            .outputItems(dustTiny, Enderium, 61)
            .outputFluids(GradePurifiedWater15.getFluid(9990))
            .EUt(VA[UEV].toLong()).duration(800).save(provider)
        ANTIENTROPY_CONDENSATION.recipeBuilder(id("grade_16_purified_water"))
            .notConsumable(GTItems.FLUID_REGULATOR_UIV)
            .inputFluids(GradePurifiedWater15.getFluid(10000))
            .inputFluids(Echoite.getFluid(PLASMA, 1000))
            .outputItems(dustTiny, Echoite, 62)
            .outputFluids(GradePurifiedWater16.getFluid(9999))
            .EUt(VA[UIV].toLong()).duration(800).save(provider)
        addRecipe(Helium, provider)
        addRecipe(Oxygen, provider)
    }

    private fun addRecipe(material : Material, provider : Consumer<FinishedRecipe?>) {
        ANTIENTROPY_CONDENSATION.recipeBuilder(id(material.name))
            .inputFluids(material.getFluid(1000))
            .outputFluids(material.getFluid(LIQUID, 1000))
            .EUt(VA[HV].toLong()).duration(240).save(provider)
    }
}
