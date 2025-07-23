package com.gtladd.gtladditions.data.recipes

import com.gregtechceu.gtceu.api.GTValues.*
import com.gregtechceu.gtceu.api.data.chemical.material.Material
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.*
import com.gregtechceu.gtceu.api.data.tag.TagUtil.createModItemTag
import com.gregtechceu.gtceu.common.data.GTBlocks.*
import com.gregtechceu.gtceu.common.data.GTItems.*
import com.gregtechceu.gtceu.common.data.GTMachines.*
import com.gregtechceu.gtceu.common.data.GTMaterials.*
import com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLER_RECIPES
import com.gtladd.gtladditions.GTLAdditions.id
import com.gtladd.gtladditions.common.machine.GTLAddMachines
import com.hepdd.gtmthings.GTMThings
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.common.data.GTLMachines
import org.gtlcore.gtlcore.common.data.GTLMaterials.*
import org.gtlcore.gtlcore.common.data.machines.GCyMMachines.*
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineA
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import java.util.*
import java.util.function.Consumer

object Assembler {
    private val nmChip : ItemStack = getItemStack("kubejs:nm_chip", 2)
    private val pmChip : ItemStack = getItemStack("kubejs:pm_chip", 2)
    private val fmChip : ItemStack = getItemStack("kubejs:fm_chip", 2)
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
        ASSEMBLER_RECIPES.recipeBuilder(id("naquadria_charge_more"))
            .inputItems(QUANTUM_STAR)
            .inputItems(INDUSTRIAL_TNT)
            .inputItems(dust, Naquadria)
            .inputItems(dustTiny, Hexanitrohexaaxaisowurtzitane, 4)
            .inputItems(plateDouble, Thorium)
            .inputFluids(Antimatter.getFluid(1))
            .outputItems(getItemStack("kubejs:naquadria_charge", 16))
            .EUt(VA[OpV].toLong()).duration(200).save(provider)
        ASSEMBLER_RECIPES.recipeBuilder(id("leptonic_charge"))
            .inputItems(GRAVI_STAR)
            .inputItems(INDUSTRIAL_TNT)
            .inputItems(dust, DegenerateRhenium)
            .inputItems(dustSmall, Hexanitrohexaaxaisowurtzitane, 2)
            .inputItems(plateDouble, Enderium)
            .inputFluids(Antimatter.getFluid(10))
            .outputItems(getItemStack("kubejs:leptonic_charge", 16))
            .EUt(VA[MAX].toLong()).duration(200).save(provider)
        ASSEMBLER_RECIPES.recipeBuilder(id("quantum_chromodynamic_charge"))
            .notConsumable(getItemStack("gtceu:eternity_nanoswarm"))
            .inputItems(getItemStack("kubejs:unstable_star"))
            .inputItems(getItemStack("kubejs:leptonic_charge"))
            .inputItems(getItemStack("kubejs:quantumchromodynamic_protective_plating"))
            .inputFluids(Antimatter.getFluid(100))
            .outputItems(getItemStack("kubejs:quantum_chromodynamic_charge", 16))
            .duration(200).EUt(4L * VA[MAX]).save(provider)
        ASSEMBLER_RECIPES.recipeBuilder(id("electric_blast_furnace"))
            .inputItems(getItemStack("minecraft:furnace", 3))
            .inputItems(createModItemTag("circuits/lv"), 3)
            .inputItems(cableGtSingle, Tin, 2)
            .inputItems(CASING_INVAR_HEATPROOF)
            .outputItems(ELECTRIC_BLAST_FURNACE)
            .EUt(120).duration(1200).save(provider)
        ASSEMBLER_RECIPES.recipeBuilder(id("mega_blast_furnace"))
            .inputItems(ELECTRIC_BLAST_FURNACE)
            .inputItems(spring, Naquadah, 2)
            .inputItems(FIELD_GENERATOR_ZPM, 2)
            .inputItems(plateDense, NaquadahAlloy, 2)
            .inputItems(wireGtQuadruple, EnrichedNaquadahTriniumEuropiumDuranide)
            .inputItems(createModItemTag("circuits/zpm"), 1)
            .outputItems(MEGA_BLAST_FURNACE)
            .EUt(480).duration(1200).save(provider)
        ASSEMBLER_RECIPES.recipeBuilder(id("mega_alloy_blast_smelter"))
            .inputItems(BLAST_ALLOY_SMELTER)
            .inputItems(spring, NaquadahAlloy, 2)
            .inputItems(FIELD_GENERATOR_ZPM, 2)
            .inputItems(plateDense, Darmstadtium, 2)
            .inputItems(wireGtHex, EnrichedNaquadahTriniumEuropiumDuranide)
            .inputItems(createModItemTag("circuits/zpm"), 1)
            .outputItems(MultiBlockMachineA.MEGA_ALLOY_BLAST_SMELTER)
            .EUt(480).duration(1200).save(provider)
        ASSEMBLER_RECIPES.recipeBuilder(id("ev_alloy_smelter"))
            .inputItems(HULL[EV])
            .inputItems(wireGtQuadruple, Nichrome, 4)
            .inputItems(cableGtSingle, Aluminium, 2)
            .inputItems(createModItemTag("circuits/ev"), 1)
            .outputItems(ALLOY_SMELTER[EV])
            .EUt(120).duration(1200).save(provider)
        ASSEMBLER_RECIPES.recipeBuilder(id("alloy_blast_smelter"))
            .inputItems(ALLOY_SMELTER[EV])
            .inputItems(plate, TantalumCarbide, 4)
            .inputItems(cableGtSingle, Aluminium, 2)
            .inputItems(createModItemTag("circuits/ev"), 1)
            .outputItems(BLAST_ALLOY_SMELTER)
            .EUt(120).duration(1200).save(provider)
        ASSEMBLER_RECIPES.recipeBuilder(id("vacuum_freezer"))
            .inputItems(CASING_ALUMINIUM_FROSTPROOF)
            .inputItems(ELECTRIC_PUMP_EV, 3)
            .inputItems(createModItemTag("circuits/ev"), 3)
            .inputItems(cableGtSingle, Gold, 2)
            .outputItems(VACUUM_FREEZER)
            .EUt(120).duration(1200).save(provider)
        ASSEMBLER_RECIPES.recipeBuilder(id("mega_vacuum_freezer"))
            .inputItems(VACUUM_FREEZER)
            .inputItems(FIELD_GENERATOR_ZPM, 2)
            .inputItems(plateDense, RhodiumPlatedPalladium, 2)
            .inputItems(createModItemTag("circuits/zpm"), 1)
            .inputItems(wireGtQuadruple, RutheniumTriniumAmericiumNeutronate)
            .outputItems(MEGA_VACUUM_FREEZER)
            .EUt(480).duration(1200).save(provider)
        ASSEMBLER_RECIPES.recipeBuilder(id("item_filter"))
            .inputItems(foil, Zinc, 8)
            .inputItems(plate, Steel)
            .inputItems(ITEM_FILTER)
            .EUt(1).duration(80).save(provider)
        ASSEMBLER_RECIPES.recipeBuilder(id("fluid_filter_lapis"))
            .inputItems(foil, Zinc, 8)
            .inputItems(plate, Lapis)
            .inputItems(FLUID_FILTER)
            .EUt(1).duration(80).save(provider)
        ASSEMBLER_RECIPES.recipeBuilder(id("fluid_filter_lazurite"))
            .inputItems(foil, Zinc, 8)
            .inputItems(plate, Lazurite)
            .inputItems(FLUID_FILTER)
            .EUt(1).duration(80).save(provider)
        ASSEMBLER_RECIPES.recipeBuilder(id("fluid_filter_sodalite"))
            .inputItems(foil, Zinc, 8)
            .inputItems(plate, Sodalite)
            .inputItems(FLUID_FILTER)
            .EUt(1).duration(80).save(provider)
        ASSEMBLER_RECIPES.recipeBuilder(id("filter_casing"))
            .inputItems(getItemStack("minecraft:iron_bars", 3))
            .inputItems(ITEM_FILTER, 3)
            .inputItems(ELECTRIC_MOTOR_EV)
            .inputItems(frameGt, Steel)
            .inputItems(rotor, Steel)
            .outputItems(FILTER_CASING)
            .EUt(32).duration(240).save(provider)
        ASSEMBLER_RECIPES.recipeBuilder(id("power_substation"))
            .circuitMeta(8)
            .inputItems(CASING_PALLADIUM_SUBSTATION.asStack())
            .inputItems(LAPOTRON_CRYSTAL, 4)
            .inputItems(createModItemTag("circuits/luv"), 2)
            .inputItems(POWER_INTEGRATED_CIRCUIT, 2)
            .outputItems(POWER_SUBSTATION.asStack())
            .EUt(480).duration(1200).save(provider)
    }

    private fun addWorldAccelerator(provider : Consumer<FinishedRecipe?>) {
        for (i in 1 .. 8) {
            val tierName = VN[i].lowercase(Locale.getDefault())
            ASSEMBLER_RECIPES.recipeBuilder(id(tierName + "_world_accelerator"))
                .circuitMeta(24)
                .inputItems(getItemStack("gtceu:" + tierName + "_field_generator", 4))
                .inputItems(getItemStack("gtceu:" + tierName + "_emitter", 2))
                .inputItems(getItemStack("gtceu:" + tierName + "_sensor", 2))
                .inputItems(getItemStack("gtceu:" + tierName + "_machine_hull"))
                .outputItems(getItemStack("gtceu:" + tierName + "_world_accelerator"))
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
            val builder = ASSEMBLER_RECIPES.recipeBuilder(id(`val` !![0] + "_diode"))
                .circuitMeta(9)
            if (`val`[0] == "lv" || `val`[0] == "mv") builder.inputItems(createModItemTag(`val`[1] !!), 4)
            else builder.inputItems(getItemStack(`val`[1]!!, 4))
            builder.inputItems(getItemStack("gtceu:" + `val`[2] + "_plate", 2))
                .inputItems(getItemStack("gtceu:" +  `val`[3] + "_cable", 2))
                .inputItems(getItemStack("gtceu:" + `val`[0] + "_machine_hull"))
                .outputItems(getItemStack("gtceu:" + `val`[0] + "_diode"))
                .EUt(VA[4].toLong()).duration(1200).save(provider)
        }
    }

    private fun addHermeticCasing(provider : Consumer<FinishedRecipe?>) {
        val hermeticCasing = arrayOf<Array<String?>?>(
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
        for (`val` in hermeticCasing) {
            ASSEMBLER_RECIPES.recipeBuilder(id(`val` !![3] + "_hermetic_casing"))
                .circuitMeta(3)
                .inputItems(getItemStack("gtceu:" + `val`[5] + "_plate", 8))
                .inputItems(getItemStack("gtceu:" + `val`[6]))
                .outputItems(getItemStack(`val`[4] + ":" + `val`[3] + "_hermetic_casing"))
                .EUt(VA[3].toLong()).duration(400).save(provider)
            for (s in arrayOf("_tank", "_chest")) {
                val builder = ASSEMBLER_RECIPES.recipeBuilder(id(`val`[3] + "_" + `val`[2] + s))
                    .circuitMeta(if (s == "_tank") 9 else 10)
                    .inputItems(createModItemTag("circuits/" + `val`[3]), 4)
                if (s == "_tank") builder.inputItems(getItemStack("gtceu:" + `val`[5] + "_plate", if (`val`[1] == "") 3 else 2))
                    .inputItems(getItemStack(`val`[4] + ":" + `val`[3] + "_hermetic_casing"))
                    .inputItems(getItemStack("gtceu:" + `val`[0] + "_electric_pump"))
                if (s == "_chest") builder.inputItems(getItemStack("gtceu:" + `val`[5] + "_plate", if (`val`[1] == "") 4 else 3))
                    .inputItems(getItemStack(if (`val`[2] == "super") "gtceu:" + `val`[5] + "_crate" else "gtceu:" + `val`[3] + "_machine_hull"))
                if (`val`[1] != "") builder.inputItems(getItemStack("gtceu:" + `val`[1] + "_field_generator"))
                builder.outputItems(getItemStack("gtceu:" + `val`[3] + "_" + `val`[2] + s))
                    .EUt(VA[4].toLong()).duration(1200).save(provider)
            }
        }
    }

    private fun addTransformer(provider : Consumer<FinishedRecipe?>) {
        for (tier in 1 .. 13) {
            val pic = setPic(tier)
            val eu = VN[tier].lowercase(Locale.getDefault())
            val machineHull = getItemStack("gtceu:" + eu + "_machine_hull")
            for (e in intArrayOf(1, 2, 4)) {
                val builder =
                    ASSEMBLER_RECIPES.recipeBuilder(id(eu + "_transformer_" + e + "a"))
                        .inputItems(machineHull)
                        .outputItems(getItemStack("gtceu:" + eu + "_transformer_" + e + "a"))
                        .EUt(30).duration(200)
                val cable =
                    if (e == 1) cableGtSingle else (if (e == 2) cableGtDouble else cableGtQuadruple)
                if (pic != null) {
                    builder.inputItems(pic)
                    if ((pic.`is`(nmChip.item) || pic.`is`(pmChip.item) || pic.`is`(fmChip.item)) &&
                        e == 4
                    ) builder.inputItems(cableGtDouble, transFormer[tier - 1] !![0], 4)
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
            val s = VN[tier].lowercase(Locale.getDefault())
            ASSEMBLER_RECIPES.recipeBuilder(GTMThings.id("huge_output_dual_hatch_$s"))
                .inputItems(GTLMachines.HUGE_FLUID_EXPORT_HATCH[tier].asStack())
                .inputItems(if (tier > 4) QUANTUM_CHEST[tier] else SUPER_CHEST[tier])
                .inputFluids(SolderingAlloy.getFluid(144L))
                .outputItems(GTLAddMachines.HUGE_OUTPUT_DUAL_HATCH[tier] !!.asStack())
                .duration(200).EUt(VA[tier].toLong()).save(provider)
        }
    }

    private fun setPic(tier : Int) : ItemStack? {
        return when (tier) {
            2 -> ULTRA_LOW_POWER_INTEGRATED_CIRCUIT.asStack(2)
            3 -> LOW_POWER_INTEGRATED_CIRCUIT.asStack(2)
            4 -> POWER_INTEGRATED_CIRCUIT.asStack(2)
            5, 6 -> HIGH_POWER_INTEGRATED_CIRCUIT.asStack(2)
            7, 8, 9 -> ULTRA_HIGH_POWER_INTEGRATED_CIRCUIT.asStack(2)
            10 -> nmChip
            11, 12 -> pmChip
            13 -> fmChip
            else -> null
        }
    }
}
