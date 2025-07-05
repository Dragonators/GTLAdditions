package com.gtladd.gtladditions.data.recipes

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.data.chemical.material.Material
import com.gregtechceu.gtceu.api.data.tag.TagPrefix
import com.gregtechceu.gtceu.api.data.tag.TagUtil
import com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLER_RECIPES
import com.gregtechceu.gtceu.common.data.GTMaterials.*
import com.gregtechceu.gtceu.common.data.*
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.registry.GTLAddRecipeBuilder
import com.gtladd.gtladditions.common.machine.GTLAddMachines
import com.hepdd.gtmthings.GTMThings
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.common.data.GTLMachines
import org.gtlcore.gtlcore.common.data.GTLMaterials.*
import org.gtlcore.gtlcore.utils.Registries
import java.util.*
import java.util.function.Consumer

object Assembler {
    private val nmChip : ItemStack = Registries.getItemStack("kubejs:nm_chip", 2)
    private val pmChip : ItemStack = Registries.getItemStack("kubejs:pm_chip", 2)
    private val fmChip : ItemStack = Registries.getItemStack("kubejs:fm_chip", 2)
    private val transFormer = arrayOf<Array<Material?>?>(
        arrayOf(Tin, Copper),
        arrayOf(Copper, Gold),
        arrayOf(Gold, Aluminium),
        arrayOf(Aluminium, Platinum),
        arrayOf(Platinum, NiobiumTitanium),
        arrayOf(NiobiumTitanium, VanadiumGallium),
        arrayOf(VanadiumGallium, YttriumBariumCuprate),
        arrayOf(YttriumBariumCuprate, Europium),
        arrayOf(Europium, Mithril),
        arrayOf(Mithril, Neutronium),
        arrayOf(Neutronium, Taranium),
        arrayOf(Taranium, Crystalmatrix),
        arrayOf(Crystalmatrix, CosmicNeutronium)
    )

    @JvmStatic
    fun init(provider : Consumer<FinishedRecipe?>) {
        addWorldAccelerator(provider)
        addHermeticCasing(provider)
        addDiode(provider)
        addTransformer(provider)
        addHugeOutput(provider)
        GTLAddRecipeBuilder("naquadria_charge_more", ASSEMBLER_RECIPES)
            .inputItems("gtceu:quantum_star")
            .inputItems("gtceu:industrial_tnt")
            .inputItems("gtceu:naquadria_dust")
            .inputItems("gtceu:tiny_hexanitrohexaaxaisowurtzitane_dust", 4)
            .inputItems("gtceu:double_thorium_plate")
            .inputFluids(Antimatter.getFluid(1))
            .outputItems("kubejs:naquadria_charge", 16)
            .TierEUtVA(13).duration(200).save(provider)
        GTLAddRecipeBuilder("leptonic_charge", ASSEMBLER_RECIPES)
            .inputItems("gtceu:gravi_star")
            .inputItems("gtceu:industrial_tnt")
            .inputItems("gtceu:degenerate_rhenium_dust")
            .inputItems("gtceu:small_hexanitrohexaaxaisowurtzitane_dust", 2)
            .inputItems("gtceu:double_enderium_plate")
            .inputFluids(Antimatter.getFluid(10))
            .outputItems("kubejs:leptonic_charge", 16)
            .TierEUtVA(14).duration(200).save(provider)
        GTLAddRecipeBuilder("quantum_chromodynamic_charge", ASSEMBLER_RECIPES)
            .notConsumable("gtceu:eternity_nanoswarm")
            .inputItems("kubejs:unstable_star")
            .inputItems("kubejs:leptonic_charge")
            .inputItems("kubejs:quantumchromodynamic_protective_plating")
            .inputFluids(Antimatter.getFluid(100))
            .outputItems("kubejs:quantum_chromodynamic_charge", 16)
            .duration(200).EUt(4L * GTValues.VA[GTValues.MAX])
            .save(provider)
        GTLAddRecipeBuilder("electric_blast_furnace", ASSEMBLER_RECIPES)
            .InputItems("3x minecraft:furnace")
            .inputItemsModTag("circuits/lv", 3)
            .InputItems("2x gtceu:tin_single_cable")
            .inputItems("gtceu:heatproof_machine_casing")
            .outputItems("gtceu:electric_blast_furnace").EUt(120)
            .duration(1200).save(provider)
        GTLAddRecipeBuilder("mega_blast_furnace", ASSEMBLER_RECIPES)
            .inputItems("gtceu:electric_blast_furnace")
            .InputItems("2x gtceu:naquadah_spring")
            .InputItems("2x gtceu:zpm_field_generator")
            .InputItems("2x gtceu:dense_naquadah_alloy_plate")
            .inputItems("gtceu:enriched_naquadah_trinium_europium_duranide_quadruple_wire")
            .inputItemsModTag("circuits/zpm")
            .outputItems("gtceu:mega_blast_furnace")
            .EUt(480).duration(1200)
            .save(provider)
        GTLAddRecipeBuilder("mega_alloy_blast_smelter", ASSEMBLER_RECIPES)
            .inputItems("gtceu:alloy_blast_smelter")
            .InputItems("2x gtceu:naquadah_alloy_spring")
            .InputItems("2x gtceu:zpm_field_generator")
            .InputItems("2x gtceu:dense_darmstadtium_plate")
            .inputItems("gtceu:enriched_naquadah_trinium_europium_duranide_hex_wire")
            .inputItemsModTag("circuits/zpm")
            .outputItems("gtceu:mega_alloy_blast_smelter")
            .EUt(480).duration(1200)
            .save(provider)
        GTLAddRecipeBuilder("ev_alloy_smelter", ASSEMBLER_RECIPES)
            .inputItems("gtceu:ev_machine_hull")
            .InputItems("4x gtceu:nichrome_quadruple_wire")
            .InputItems("2x gtceu:aluminium_single_cable")
            .inputItemsModTag("circuits/ev", 2)
            .outputItems("gtceu:ev_alloy_smelter")
            .EUt(120).duration(1200)
            .save(provider)
        GTLAddRecipeBuilder("alloy_blast_smelter", ASSEMBLER_RECIPES)
            .inputItems("gtceu:ev_alloy_smelter")
            .InputItems("4x gtceu:tantalum_carbide_plate")
            .InputItems("2x gtceu:aluminium_single_cable")
            .inputItemsModTag("circuits/ev", 2)
            .outputItems("gtceu:alloy_blast_smelter")
            .EUt(120).duration(1200)
            .save(provider)
        GTLAddRecipeBuilder("vacuum_freezer", ASSEMBLER_RECIPES)
            .inputItems("gtceu:frostproof_machine_casing")
            .InputItems("3x gtceu:hv_electric_pump")
            .inputItemsModTag("circuits/ev", 3)
            .InputItems("2x gtceu:gold_single_cable")
            .outputItems("gtceu:vacuum_freezer")
            .EUt(120).duration(1200).save(provider)
        GTLAddRecipeBuilder("mega_vacuum_freezer", ASSEMBLER_RECIPES)
            .inputItems("gtceu:vacuum_freezer")
            .InputItems("2x gtceu:zpm_field_generator")
            .InputItems("2x gtceu:dense_rhodium_plated_palladium_plate")
            .inputItemsModTag("circuits/zpm")
            .inputItems("gtceu:ruthenium_trinium_americium_neutronate_quadruple_wire")
            .outputItems("gtceu:mega_vacuum_freezer")
            .EUt(480).duration(1200).save(provider)
        GTLAddRecipeBuilder("item_filter", ASSEMBLER_RECIPES)
            .InputItems("8x gtceu:zinc_foil")
            .inputItems("gtceu:steel_plate")
            .outputItems("gtceu:item_filter")
            .EUt(1).duration(80).save(provider)
        GTLAddRecipeBuilder("fluid_filter_lapis", ASSEMBLER_RECIPES)
            .InputItems("8x gtceu:zinc_foil")
            .inputItems("gtceu:lapis_plate")
            .outputItems("gtceu:fluid_filter")
            .EUt(1).duration(80).save(provider)
        GTLAddRecipeBuilder("fluid_filter_lazurite", ASSEMBLER_RECIPES)
            .InputItems("8x gtceu:zinc_foil")
            .inputItems("gtceu:lazurite_plate")
            .outputItems("gtceu:fluid_filter")
            .EUt(1).duration(80).save(provider)
        GTLAddRecipeBuilder("fluid_filter_sodalite", ASSEMBLER_RECIPES)
            .InputItems("8x gtceu:zinc_foil")
            .inputItems("gtceu:sodalite_plate")
            .outputItems("gtceu:fluid_filter")
            .EUt(1).duration(80).save(provider)
        GTLAddRecipeBuilder("filter_casing", ASSEMBLER_RECIPES)
            .InputItems("3x minecraft:iron_bars")
            .InputItems("3x gtceu:item_filter")
            .inputItems("gtceu:mv_electric_motor")
            .inputItems("gtceu:steel_frame")
            .inputItems("gtceu:steel_rotor")
            .outputItems("gtceu:filter_casing")
            .EUt(32).duration(240).save(provider)
        ASSEMBLER_RECIPES.recipeBuilder(GTLAdditions.id("power_substation"))
            .circuitMeta(8)
            .inputItems(GTBlocks.CASING_PALLADIUM_SUBSTATION.asStack())
            .inputItems(GTItems.LAPOTRON_CRYSTAL, 4)
            .inputItems(TagUtil.createModItemTag("circuits/luv"), 2)
            .inputItems(GTItems.POWER_INTEGRATED_CIRCUIT, 2)
            .outputItems(GTMachines.POWER_SUBSTATION.asStack())
            .EUt(480).duration(1200).save(provider)
    }

    private fun addWorldAccelerator(provider : Consumer<FinishedRecipe?>) {
        for (i in 1 .. 8) {
            val tierName = GTValues.VN[i].lowercase(Locale.getDefault())
            GTLAddRecipeBuilder(tierName + "_world_accelerator", ASSEMBLER_RECIPES)
                .circuitMeta(24)
                .inputItems("gtceu:" + tierName + "_field_generator", 4)
                .inputItems("gtceu:" + tierName + "_emitter", 2)
                .inputItems("gtceu:" + tierName + "_sensor", 2)
                .inputItems("gtceu:" + tierName + "_machine_hull")
                .outputItems("gtceu:" + tierName + "_world_accelerator")
                .duration(200).EUt(480).save(provider)
        }
    }

    private fun addDiode(provider : Consumer<FinishedRecipe?>) {
        val diode = arrayOf<Array<String?>?>(
            arrayOf("lv", "diodes", "steel", "tin_quadruple"),
            arrayOf("mv", "diodes", "aluminium", "copper_quadruple"),
            arrayOf("hv", "gtceu:smd_diode", "stainless_steel", "gold_quadruple"),
            arrayOf("ev", "gtceu:smd_diode", "titanium", "aluminium_quadruple"),
            arrayOf("iv", "gtceu:smd_diode", "tungsten_steel", "platinum_quadruple"),
            arrayOf("luv", "gtceu:advanced_smd_diode", "rhodium_plated_palladium", "niobium_titanium_quadruple"),
            arrayOf("zpm", "gtceu:advanced_smd_diode", "naquadah_alloy", "vanadium_gallium_quadruple"),
            arrayOf("uv", "gtceu:advanced_smd_diode", "darmstadtium", "yttrium_barium_cuprate_quadruple"),
            arrayOf("uhv", "gtceu:advanced_smd_diode", "neutronium", "europium_quadruple"),
            arrayOf("uev", "gtceu:advanced_smd_diode", "quantanium", "mithril_double"),
            arrayOf("uiv", "gtceu:advanced_smd_diode", "adamantium", "neutronium_double"),
            arrayOf("uxv", "gtceu:advanced_smd_diode", "vibranium", "taranium_double"),
            arrayOf("opv", "gtceu:advanced_smd_diode", "draconium", "crystalmatrix_double")
        )
        for (`val` in diode) {
            val builder = GTLAddRecipeBuilder(`val` !![0] + "_diode", ASSEMBLER_RECIPES)
                .circuitMeta(9)
            if (`val`[0] == "lv" || `val`[0] == "mv") builder.inputItemsModTag(`val`[1] !!, 4)
            else builder.inputItems(`val`[1], 4)
            builder.inputItems("gtceu:" + `val`[2] + "_plate", 2)
                .inputItems("gtceu:" + `val`[3] + "_cable", 2)
                .inputItems("gtceu:" + `val`[0] + "_machine_hull")
                .outputItems("gtceu:" + `val`[0] + "_diode")
                .TierEUtVA(4).duration(1200).save(provider)
        }
    }

    private fun addHermeticCasing(provider : Consumer<FinishedRecipe?>) {
        val hermetic_casing = arrayOf<Array<String?>?>(
            arrayOf("lv", "", "super", "lv", "gtceu", "steel", "polyethylene_large_fluid_pipe"),
            arrayOf("mv", "", "super", "mv", "gtceu", "aluminium", "polyvinyl_chloride_large_item_pipe"),
            arrayOf("hv", "lv", "super", "hv", "gtceu", "stainless_steel", "polytetrafluoroethylene_large_fluid_pipe"),
            arrayOf("ev", "mv", "super", "ev", "gtceu", "titanium", "stainless_steel_large_fluid_pipe"),
            arrayOf("iv", "hv", "quantum", "iv", "gtceu", "dense_tungsten_steel", "titanium_large_fluid_pipe"),
            arrayOf("luv", "ev", "quantum", "luv", "gtceu", "dense_rhodium_plated_palladium", "tungsten_steel_large_fluid_pipe"),
            arrayOf("zpm", "iv", "quantum", "zpm", "gtceu", "dense_naquadah_alloy", "niobium_titanium_large_fluid_pipe"),
            arrayOf("uv", "luv", "quantum", "uv", "gtceu", "dense_darmstadtium", "naquadah_large_fluid_pipe"),
            arrayOf("uv", "zpm", "quantum", "uhv", "gtceu", "neutronium", "duranium_large_fluid_pipe"),
            arrayOf("uhv", "uv", "quantum", "uev", "gtlcore", "quantanium", "neutronium_large_fluid_pipe"),
            arrayOf("uev", "uhv", "quantum", "uiv", "gtlcore", "adamantium", "neutronium_large_fluid_pipe"),
            arrayOf("uiv", "uev", "quantum", "uxv", "gtlcore", "vibranium", "enderium_large_fluid_pipe"),
            arrayOf("uxv", "uiv", "quantum", "opv", "gtlcore", "draconium", "heavy_quark_degenerate_matter_large_fluid_pipe")
        )
        for (`val` in hermetic_casing) {
            GTLAddRecipeBuilder(`val` !![3] + "_hermetic_casing", ASSEMBLER_RECIPES)
                .circuitMeta(3)
                .inputItems("gtceu:" + `val`[5] + "_plate", 8)
                .inputItems("gtceu:" + `val`[6])
                .outputItems(`val`[4] + ":" + `val`[3] + "_hermetic_casing")
                .TierEUtVA(3).duration(400).save(provider)
            for (s in arrayOf("_tank", "_chest")) {
                val builder = GTLAddRecipeBuilder(`val`[3] + "_" + `val`[2] + s, ASSEMBLER_RECIPES)
                    .circuitMeta(if (s == "_tank") 9 else 10)
                    .inputItemsModTag("circuits/" + `val`[3], 4)
                if (s == "_tank") builder.inputItems("gtceu:" + `val`[5] + "_plate", if (`val`[1] == "") 3 else 2)
                    .inputItems(`val`[4] + ":" + `val`[3] + "_hermetic_casing")
                    .inputItems("gtceu:" + `val`[0] + "_electric_pump")
                if (s == "_chest") builder.inputItems(
                    "gtceu:" + `val`[5] + "_plate",
                    if (`val`[1] == "") 4 else 3
                )
                    .inputItems(if (`val`[2] == "super") "gtceu:" + `val`[5] + "_crate" else "gtceu:" + `val`[3] + "_machine_hull")
                if (`val`[1] != "") builder.inputItems("gtceu:" + `val`[1] + "_field_generator")
                builder.outputItems("gtceu:" + `val`[3] + "_" + `val`[2] + s).TierEUtVA(4).duration(1200)
                    .save(provider)
            }
        }
    }

    private fun addTransformer(provider : Consumer<FinishedRecipe?>) {
        for (tier in 1 .. 13) {
            val pic = setPic(tier)
            val eu = GTValues.VN[tier].lowercase(Locale.getDefault())
            val machine_hull = Registries.getItemStack("gtceu:" + eu + "_machine_hull")
            for (e in intArrayOf(1, 2, 4)) {
                val builder =
                    ASSEMBLER_RECIPES.recipeBuilder(GTLAdditions.id(eu + "_transformer_" + e + "a"))
                        .inputItems(machine_hull)
                        .outputItems(Registries.getItemStack("gtceu:" + eu + "_transformer_" + e + "a"))
                        .EUt(30).duration(200)
                val cable =
                    if (e == 1) TagPrefix.cableGtSingle else (if (e == 2) TagPrefix.cableGtDouble else TagPrefix.cableGtQuadruple)
                if (pic != null) {
                    builder.inputItems(pic)
                    if ((pic.`is`(nmChip.item) || pic.`is`(pmChip.item) || pic.`is`(fmChip.item)) &&
                        e == 4
                    ) builder.inputItems(TagPrefix.cableGtDouble, transFormer[tier - 1] !![0], 4)
                    else builder.inputItems(cable, transFormer[tier - 1] !![0], 4)
                }
                if (tier == 1) builder.inputItems(cable, transFormer[0] !![0], 4)
                builder.inputItems(cable, transFormer[tier - 1] !![1])
                    .circuitMeta(1).save(provider)
            }
        }
    }

    private fun addHugeOutput(provider : Consumer<FinishedRecipe?>) {
        for (tier in 1 .. 13) {
            val s = GTValues.VN[tier].lowercase(Locale.getDefault())
            ASSEMBLER_RECIPES.recipeBuilder(GTMThings.id("huge_output_dual_hatch_$s"))
                .inputItems(GTLMachines.HUGE_FLUID_EXPORT_HATCH[tier].asStack())
                .inputItems(if (tier > 4) GTMachines.QUANTUM_CHEST[tier] else GTMachines.SUPER_CHEST[tier])
                .inputFluids(SolderingAlloy.getFluid(144L))
                .outputItems(GTLAddMachines.HUGE_OUTPUT_DUAL_HATCH[tier] !!.asStack())
                .duration(200).EUt(GTValues.VA[tier].toLong()).save(provider)
        }
    }

    private fun setPic(tier : Int) : ItemStack? {
        return when (tier) {
            2 -> GTItems.ULTRA_LOW_POWER_INTEGRATED_CIRCUIT.asStack(2)
            3 -> GTItems.LOW_POWER_INTEGRATED_CIRCUIT.asStack(2)
            4 -> GTItems.POWER_INTEGRATED_CIRCUIT.asStack(2)
            5, 6 -> GTItems.HIGH_POWER_INTEGRATED_CIRCUIT.asStack(2)
            7, 8, 9 -> GTItems.ULTRA_HIGH_POWER_INTEGRATED_CIRCUIT.asStack(2)
            10 -> nmChip
            11, 12 -> pmChip
            13 -> fmChip
            else -> null
        }
    }
}
