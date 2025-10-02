package com.gtladd.gtladditions.data.recipes

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.*
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys
import com.gregtechceu.gtceu.common.data.GCyMRecipeTypes.ALLOY_BLAST_RECIPES
import com.gregtechceu.gtceu.common.data.GTMaterials
import com.gregtechceu.gtceu.common.data.GTRecipeTypes.BLAST_RECIPES
import com.gregtechceu.gtceu.common.data.GTRecipeTypes.CHEMICAL_BATH_RECIPES
import com.gregtechceu.gtceu.common.data.GTRecipeTypes.FUSION_RECIPES
import com.gregtechceu.gtceu.data.recipe.builder.ShapedRecipeBuilder
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.material.GTLAddMaterial.CREON
import com.gtladd.gtladditions.common.material.GTLAddMaterial.MELLION
import com.gtladd.gtladditions.common.material.GTLAddMaterial.PROTO_HALKONITE
import com.gtladd.gtladditions.common.material.GTLAddMaterial.PROTO_HALKONITE_BASE
import dev.latvian.mods.kubejs.KubeJS
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Blocks
import org.gtlcore.gtlcore.common.data.GTLMaterials
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes.*
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import java.util.function.Consumer

object Misc {
    @JvmStatic
    fun init(provider : Consumer<FinishedRecipe?>) {
        DECAY_HASTENER_RECIPES.recipeBuilder(GTLAdditions.id("tiranium50"))
            .inputFluids(GTMaterials.Titanium.getFluid(144))
            .outputFluids(GTLMaterials.Titanium50.getFluid(144))
            .EUt(GTValues.VA[14].toLong()).duration(10).save(provider)
        DOOR_OF_CREATE_RECIPES.recipeBuilder(GTLAdditions.id("command_block"))
            .inputItems(block, GTLMaterials.MagnetohydrodynamicallyConstrainedStarMatter)
            .outputItems(Blocks.COMMAND_BLOCK.asItem())
            .dimension(ResourceLocation("overworld"))
            .EUt(GTValues.V[14]).duration(5).save(provider)
        DOOR_OF_CREATE_RECIPES.recipeBuilder(GTLAdditions.id("magmatter_block"))
            .inputItems(ingot, GTLMaterials.Magmatter, 64)
            .outputItems(block, GTLMaterials.Magmatter)
            .dimension(ResourceLocation("overworld"))
            .EUt(GTValues.V[14]).duration(5).save(provider)
        CREATE_AGGREGATION_RECIPES.recipeBuilder(GTLAdditions.id("chain_command_block"))
            .inputItems(getItemStack("kubejs:chain_command_block_core"))
            .inputItems(getItemStack("kubejs:command_block_broken"))
            .outputItems(Blocks.CHAIN_COMMAND_BLOCK.asItem())
            .dimension(KubeJS.id("create")).CWUt(Int.Companion.MAX_VALUE / 2)
            .EUt(GTValues.V[14]).duration(20)
            .save(provider)
        CREATE_AGGREGATION_RECIPES.recipeBuilder(GTLAdditions.id("repeating_command_block"))
            .inputItems(getItemStack("kubejs:repeating_command_block_core"))
            .inputItems(getItemStack("kubejs:chain_command_block_broken"))
            .outputItems(Blocks.REPEATING_COMMAND_BLOCK.asItem())
            .dimension(KubeJS.id("create")).CWUt(Int.Companion.MAX_VALUE / 2)
            .EUt(GTValues.V[14]).duration(20)
            .save(provider)
        ShapedRecipeBuilder(GTLAdditions.id("ultimate_input_dual_hatch"))
            .output(getItemStack("gtladditions:ultimate_input_dual_hatch"))
            .pattern(" S ")
            .pattern("SDS")
            .pattern(" S ")
            .define('S', getItemStack("gtladditions:super_input_dual_hatch"))
            .define('D', getItemStack("gtlcore:max_storage"))
            .save(provider)
        initAdditionMaterial(provider)
    }

    fun initAdditionMaterial(provider : Consumer<FinishedRecipe?>) {
        FUSION_RECIPES.recipeBuilder(GTLAdditions.id("plutonium241_plasma"))
            .inputFluids(GTMaterials.Lutetium.getFluid(16))
            .inputFluids(GTMaterials.Vanadium.getFluid(16))
            .outputFluids(GTMaterials.Plutonium241.getFluid(FluidStorageKeys.PLASMA, 16))
            .EUt(GTValues.VA[GTValues.UHV].toLong())
            .duration(64)
            .fusionStartEU(720000000)
            .save(provider)
        STELLAR_FORGE_RECIPES.recipeBuilder(GTLAdditions.id("creon_plasma"))
            .inputItems(getItemStack("kubejs:quantum_chromodynamic_charge", 4))
            .inputFluids(GTMaterials.Fermium.getFluid(5760))
            .inputFluids(GTMaterials.Thorium.getFluid(5760))
            .inputFluids(GTLMaterials.CelestialTungsten.getFluid(2304))
            .inputFluids(GTMaterials.Calcium.getFluid(5760))
            .inputFluids(GTLMaterials.DimensionallyTranscendentResidue.getFluid(2736))
            .outputFluids(CREON.getFluid(FluidStorageKeys.PLASMA, 1000))
            .EUt(1258291200)
            .duration(200)
            .addData("SCTier", 3)
            .save(provider)
        PLASMA_CONDENSER_RECIPES.recipeBuilder(GTLAdditions.id("creon_condenser"))
            .inputFluids(CREON.getFluid(FluidStorageKeys.PLASMA, 1000))
            .inputFluids(GTMaterials.Helium.getFluid(100000))
            .outputFluids(CREON.getFluid(1000))
            .outputFluids(GTMaterials.Helium.getFluid(FluidStorageKeys.GAS, 100000))
            .circuitMeta(1)
            .EUt(7864320)
            .duration(1200)
            .save(provider)
        DIMENSIONALLY_TRANSCENDENT_MIXER_RECIPES.recipeBuilder(GTLAdditions.id("mellion_dust"))
            .inputItems(dust, GTMaterials.Tritanium, 11)
            .inputItems(dust, GTMaterials.Rubidium, 11)
            .inputItems(dust, GTLMaterials.Highurabilityompoundteel, 7)
            .inputItems(dust, GTLMaterials.Tartarite, 13)
            .inputItems(dust, GTLMaterials.Jasper, 8)
            .inputItems(getItemStack("avaritia:infinity_catalyst", 13))
            .inputFluids(GTLMaterials.DimensionallyTranscendentResidue.getFluid(5000))
            .outputItems(dust, MELLION, 50)
            .EUt(425829120)
            .duration(600)
            .save(provider)
        BLAST_RECIPES.recipeBuilder(GTLAdditions.id("hot_mellion_ingot"))
            .inputItems(dust, MELLION)
            .inputFluids(GTMaterials.Oganesson.getFluid(1000))
            .outputItems(ingotHot, MELLION)
            .blastFurnaceTemp(14000)
            .EUt(503316480)
            .duration(856)
            .save(provider)
        ALLOY_BLAST_RECIPES.recipeBuilder(GTLAdditions.id("molten_proto_halkonite_base"))
            .inputItems(dust, GTLMaterials.TranscendentMetal, 4)
            .inputItems(dust, GTLMaterials.Tairitsu, 4)
            .inputItems(dust, GTLMaterials.Tartarite, 4)
            .inputItems(dust, GTLMaterials.TitanPrecisionSteel, 2)
            .inputItems(dust, GTLMaterials.Eternity, 2)
            .inputFluids(GTLMaterials.DimensionallyTranscendentResidue.getFluid(576))
            .outputFluids(PROTO_HALKONITE_BASE.getFluid(FluidStorageKeys.MOLTEN, 1152))
            .blastFurnaceTemp(48000)
            .EUt(503320000)
            .duration(4080)
            .save(provider)
        CHEMICAL_BATH_RECIPES.recipeBuilder(GTLAdditions.id("hot_proto_halkonite_ingot"))
            .inputItems(ingot, GTLMaterials.Infinity)
            .inputFluids(PROTO_HALKONITE_BASE.getFluid(1440))
            .outputItems(ingotHot, PROTO_HALKONITE)
            .EUt(31457280)
            .duration(320)
            .save(provider)
    }
}
