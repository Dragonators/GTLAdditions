package com.gtladd.gtladditions.data.recipes

import com.gregtechceu.gtceu.api.GTValues.*
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.*
import com.gregtechceu.gtceu.common.data.GTBlocks.CASING_PALLADIUM_SUBSTATION
import com.gregtechceu.gtceu.common.data.GTBlocks.INDUSTRIAL_TNT
import com.gregtechceu.gtceu.common.data.GTItems.*
import com.gregtechceu.gtceu.common.data.GTMachines.*
import com.gregtechceu.gtceu.common.data.GTMaterials.*
import com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLER_RECIPES
import com.gregtechceu.gtceu.data.recipe.CustomTags
import com.gtladd.gtladditions.GTLAdditions.id
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.QUANTUM_GLASS
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.TEMPORAL_ANCHOR_FIELD_CASING
import com.gtladd.gtladditions.common.machine.GTLAddMachines
import com.hepdd.gtmthings.GTMThings
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.common.data.GTLBlocks.*
import org.gtlcore.gtlcore.common.data.GTLMachines
import org.gtlcore.gtlcore.common.data.GTLMaterials.*
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import java.util.*
import java.util.function.Consumer

object Assembler {

    @JvmStatic
    fun init(provider : Consumer<FinishedRecipe?>) {
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
        ASSEMBLER_RECIPES.recipeBuilder(id("power_substation"))
            .circuitMeta(8)
            .inputItems(CASING_PALLADIUM_SUBSTATION.asStack())
            .inputItems(LAPOTRON_CRYSTAL, 4)
            .inputItems(CustomTags.LuV_CIRCUITS, 2)
            .inputItems(POWER_INTEGRATED_CIRCUIT, 2)
            .outputItems(POWER_SUBSTATION.asStack())
            .EUt(480).duration(1200).save(provider)
        ASSEMBLER_RECIPES.recipeBuilder(id("quantum_glass"))
            .inputItems(DIMENSIONALLY_TRANSCENDENT_CASING.asStack())
            .inputItems(RHENIUM_REINFORCED_ENERGY_GLASS.asStack())
            .inputItems(getItemStack("kubejs:quantum_anomaly"))
            .inputFluids(Rhugnor.getFluid(1000000))
            .outputItems(QUANTUM_GLASS.asStack())
            .EUt(491520).duration(400).save(provider)
        ASSEMBLER_RECIPES.recipeBuilder(id("temporal_anchor_field_casing"))
            .inputItems(frameGt, TranscendentMetal, 32)
            .inputItems(frameGt, Eternity, 32)
            .inputItems(CustomTags.MAX_CIRCUITS, 8)
            .inputItems(gear, AstralTitanium, 8)
            .inputItems(SPACETIMEBENDINGCORE.asStack(8))
            .inputItems(getItemStack("kubejs:spacetime_compression_field_generator", 4))
            .inputItems(foil, Cosmic, 4)
            .inputFluids(WhiteDwarfMatter.getFluid(23040))
            .outputItems(TEMPORAL_ANCHOR_FIELD_CASING.asStack(2))
            .EUt(VA[MAX].toLong()).duration(400).save(provider)
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
}
