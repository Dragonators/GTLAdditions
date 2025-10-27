package com.gtladd.gtladditions.data.recipes

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.GTValues.MAX
import com.gregtechceu.gtceu.api.GTValues.VA
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.*
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys
import com.gregtechceu.gtceu.api.recipe.ResearchRecipeBuilder.StationRecipeBuilder
import com.gregtechceu.gtceu.common.data.GCyMRecipeTypes.ALLOY_BLAST_RECIPES
import com.gregtechceu.gtceu.common.data.GTItems.TOOL_DATA_MODULE
import com.gregtechceu.gtceu.common.data.GTMaterials
import com.gregtechceu.gtceu.common.data.GTMaterials.Plutonium241
import com.gregtechceu.gtceu.common.data.GTRecipeTypes.*
import com.gregtechceu.gtceu.data.recipe.builder.ShapedRecipeBuilder
import com.gtladd.gtladditions.GTLAdditions.id
import com.gtladd.gtladditions.common.items.GTLAddItems
import com.gtladd.gtladditions.common.items.GTLAddItems.BLACK_HOLE_SEED
import com.gtladd.gtladditions.common.items.GTLAddItems.STRANGE_ANNIHILATION_FUEL_ROD
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine
import com.gtladd.gtladditions.common.material.GTLAddMaterial.CREON
import com.gtladd.gtladditions.common.material.GTLAddMaterial.MELLION
import com.gtladd.gtladditions.common.material.GTLAddMaterial.PROTO_HALKONITE
import com.gtladd.gtladditions.common.material.GTLAddMaterial.PROTO_HALKONITE_BASE
import dev.latvian.mods.kubejs.KubeJS
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import org.gtlcore.gtlcore.common.data.GTLBlocks.CREATE_CASING
import org.gtlcore.gtlcore.common.data.GTLBlocks.DIMENSION_CONNECTION_CASING
import org.gtlcore.gtlcore.common.data.GTLItems.EMITTER_MAX
import org.gtlcore.gtlcore.common.data.GTLItems.FIELD_GENERATOR_MAX
import org.gtlcore.gtlcore.common.data.GTLMaterials.*
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes.*
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachine.COMPRESSED_FUSION_REACTOR
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachine.EYE_OF_HARMONY
import org.gtlcore.gtlcore.utils.Registries.getItem
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import java.util.function.Consumer

object Misc {
    @JvmStatic
    fun init(provider : Consumer<FinishedRecipe?>) {
        DECAY_HASTENER_RECIPES.recipeBuilder(id("tiranium50"))
            .inputFluids(GTMaterials.Titanium.getFluid(144))
            .outputFluids(Titanium50.getFluid(144))
            .EUt(VA[14].toLong()).duration(10).save(provider)
        DOOR_OF_CREATE_RECIPES.recipeBuilder(id("command_block"))
            .inputItems(block, MagnetohydrodynamicallyConstrainedStarMatter)
            .outputItems(Blocks.COMMAND_BLOCK.asItem())
            .dimension(ResourceLocation("overworld"))
            .EUt(GTValues.V[14]).duration(5).save(provider)
        DOOR_OF_CREATE_RECIPES.recipeBuilder(id("magmatter_block"))
            .inputItems(ingot, Magmatter, 64)
            .outputItems(block, Magmatter)
            .dimension(ResourceLocation("overworld"))
            .EUt(GTValues.V[14]).duration(5).save(provider)
        CREATE_AGGREGATION_RECIPES.recipeBuilder(id("chain_command_block"))
            .inputItems(getItemStack("kubejs:chain_command_block_core"))
            .inputItems(getItemStack("kubejs:command_block_broken"))
            .outputItems(Blocks.CHAIN_COMMAND_BLOCK.asItem())
            .dimension(KubeJS.id("create")).CWUt(Int.Companion.MAX_VALUE / 2)
            .EUt(GTValues.V[14]).duration(20)
            .save(provider)
        CREATE_AGGREGATION_RECIPES.recipeBuilder(id("repeating_command_block"))
            .inputItems(getItemStack("kubejs:repeating_command_block_core"))
            .inputItems(getItemStack("kubejs:chain_command_block_broken"))
            .outputItems(Blocks.REPEATING_COMMAND_BLOCK.asItem())
            .dimension(KubeJS.id("create")).CWUt(Int.Companion.MAX_VALUE / 2)
            .EUt(GTValues.V[14]).duration(20)
            .save(provider)
        MAGIC_MANUFACTURER_RECIPES.recipeBuilder(id("mana_max"))
            .notConsumable(FIELD_GENERATOR_MAX.asStack(64))
            .circuitMeta(4)
            .outputFluids(Mana.getFluid(256000000))
            .EUt(VA[MAX].toLong()).duration(320)
            .save(provider)
        ShapedRecipeBuilder(id("ultimate_input_dual_hatch"))
            .output(getItemStack("gtladditions:ultimate_input_dual_hatch"))
            .pattern(" S ")
            .pattern("SDS")
            .pattern(" S ")
            .define('S', getItemStack("gtladditions:super_input_dual_hatch"))
            .define('D', getItemStack("gtlcore:max_storage"))
            .save(provider)
        SUPRACHRONAL_ASSEMBLY_LINE_RECIPES.recipeBuilder(id("astral_array"))
            .inputItems(EYE_OF_HARMONY, 64)
            .inputItems(EYE_OF_HARMONY, 64)
            .inputItems(getItemStack("kubejs:annihilation_constrainer", 64))
            .inputItems(getItemStack("kubejs:annihilation_constrainer", 64))
            .inputItems(getItemStack("kubejs:dimension_creation_casing", 64))
            .inputItems(getItemStack("kubejs:dimension_creation_casing", 64))
            .inputItems(getItemStack("kubejs:dimension_creation_casing", 10))
            .inputItems(getItemStack("kubejs:spacetime_compression_field_generator", 64))
            .inputItems(getItemStack("kubejs:spacetime_compression_field_generator", 64))
            .inputItems(getItemStack("kubejs:spacetime_compression_field_generator", 40))
            .inputItems(getItemStack("kubejs:dimensional_stability_casing", 48))
            .inputItems(DIMENSION_CONNECTION_CASING.asStack(64))
            .inputItems(CREATE_CASING.asStack(64))
            .inputItems(getItemStack("kubejs:create_aggregatione_core", 32))
            .inputItems(getItemStack("kubejs:chaotic_energy_core", 16))
            .inputFluids(SpatialFluid.getFluid(2097152))
            .inputFluids(Eternity.getFluid(1048576))
            .inputFluids(ExcitedDtsc.getFluid(524288))
            .outputItems(GTLAddItems.ASTRAL_ARRAY)
            .EUt(VA[MAX].toLong()).duration(12000)
            .stationResearch { b : StationRecipeBuilder? ->
                b !!.researchStack(MultiBlockMachine.ARCANIC_ASTROGRAPH.asStack())
                    .dataStack(TOOL_DATA_MODULE.asStack())
                    .EUt(VA[MAX]).CWUt(67108864, 2147483647)
            }
            .save(provider)
        PRECISION_ASSEMBLER_RECIPES.recipeBuilder(id("strange_annihilation_fuel_rod"))
            .inputItems(getItemStack("kubejs:infinity_antimatter_fuel_rod", 64))
            .inputItems(getItemStack("kubejs:infinity_antimatter_fuel_rod", 64))
            .inputItems(getItemStack("kubejs:infinity_antimatter_fuel_rod", 64))
            .inputItems(getItemStack("kubejs:hypercube", 64))
            .inputFluids(Cosmic.getFluid(1440))
            .inputFluids(MagnetohydrodynamicallyConstrainedStarMatter.getFluid(1600))
            .inputFluids(SpaceTime.getFluid(5760))
            .inputFluids(PROTO_HALKONITE.getFluid(1440))
            .outputItems(STRANGE_ANNIHILATION_FUEL_ROD)
            .EUt(VA[MAX].toLong())
            .duration(12000)
            .save(provider)
        ASSEMBLER_MODULE_RECIPES.recipeBuilder(id("black_hole_seed"))
            .inputItems(EMITTER_MAX, 16)
            .inputItems(COMPRESSED_FUSION_REACTOR[GTValues.UEV], 16)
            .inputItems(COMPRESSED_FUSION_REACTOR[GTValues.UEV], 16)
            .inputItems(EMITTER_MAX, 16)
            .inputItems(plateDouble, PROTO_HALKONITE, 8)
            .inputItems(getItemStack("kubejs:hypercube", 64))
            .inputItems(getItemStack("kubejs:hypercube", 64))
            .inputItems(plateDouble, PROTO_HALKONITE, 8)
            .inputItems(plateDouble, PROTO_HALKONITE, 8)
            .inputItems(getItemStack("kubejs:hypercube", 64))
            .inputItems(getItemStack("kubejs:hypercube", 64))
            .inputItems(plateDouble, PROTO_HALKONITE, 8)
            .inputItems(EMITTER_MAX, 16)
            .inputItems(COMPRESSED_FUSION_REACTOR[GTValues.UEV], 16)
            .inputItems(COMPRESSED_FUSION_REACTOR[GTValues.UEV], 16)
            .inputItems(EMITTER_MAX, 16)
            .inputFluids(CREON.getFluid(92160))
            .inputFluids(WhiteDwarfMatter.getFluid(92160))
            .inputFluids(Tartarite.getFluid(92160))
            .inputFluids(BlackDwarfMatter.getFluid(92160))
            .outputItems(BLACK_HOLE_SEED)
            .addData("SEPMTier", 5)
            .EUt(VA[MAX].toLong())
            .duration(11520)
            .save(provider)
        NEUTRON_COMPRESSOR_RECIPES.recipeBuilder(id("avaritia_singularity_spacetime"))
            .inputItems(block, SpaceTime, 64)
            .inputItems(block, Eternity, 64)
            .outputItems(ItemStack(getItem("avaritia:singularity")).apply {
                orCreateTag.putString("Id", "avaritia:spacetime")
            }, 64)
            .EUt(VA[MAX].toLong())
            .duration(1200)
            .save(provider)
        initAdditionMaterial(provider)
    }

    fun initAdditionMaterial(provider : Consumer<FinishedRecipe?>) {
        FUSION_RECIPES.recipeBuilder(id("plutonium241_plasma"))
            .inputFluids(GTMaterials.Lutetium.getFluid(16))
            .inputFluids(GTMaterials.Vanadium.getFluid(16))
            .outputFluids(Plutonium241.getFluid(FluidStorageKeys.PLASMA, 16))
            .EUt(VA[GTValues.UHV].toLong())
            .duration(64)
            .fusionStartEU(720000000)
            .save(provider)
        STELLAR_FORGE_RECIPES.recipeBuilder(id("creon_plasma"))
            .inputItems(getItemStack("kubejs:quantum_chromodynamic_charge", 4))
            .inputFluids(GTMaterials.Fermium.getFluid(5760))
            .inputFluids(GTMaterials.Thorium.getFluid(5760))
            .inputFluids(CelestialTungsten.getFluid(2304))
            .inputFluids(GTMaterials.Calcium.getFluid(5760))
            .inputFluids(DimensionallyTranscendentResidue.getFluid(2736))
            .outputFluids(CREON.getFluid(FluidStorageKeys.PLASMA, 1000))
            .EUt(1258291200)
            .duration(200)
            .addData("SCTier", 3)
            .save(provider)
        PLASMA_CONDENSER_RECIPES.recipeBuilder(id("creon_condenser"))
            .inputFluids(CREON.getFluid(FluidStorageKeys.PLASMA, 1000))
            .inputFluids(GTMaterials.Helium.getFluid(FluidStorageKeys.LIQUID, 100000))
            .outputFluids(CREON.getFluid(1000))
            .outputFluids(GTMaterials.Helium.getFluid(FluidStorageKeys.GAS, 100000))
            .circuitMeta(1)
            .EUt(7864320)
            .duration(1200)
            .save(provider)
        DIMENSIONALLY_TRANSCENDENT_MIXER_RECIPES.recipeBuilder(id("mellion_dust"))
            .inputItems(dust, GTMaterials.Tritanium, 11)
            .inputItems(dust, GTMaterials.Rubidium, 11)
            .inputItems(dust, Highurabilityompoundteel, 7)
            .inputItems(dust, Tartarite, 13)
            .inputItems(dust, Jasper, 8)
            .inputItems(getItemStack("avaritia:infinity_catalyst", 13))
            .inputFluids(DimensionallyTranscendentResidue.getFluid(5000))
            .outputItems(dust, MELLION, 50)
            .EUt(425829120)
            .duration(600)
            .save(provider)
        BLAST_RECIPES.recipeBuilder(id("hot_mellion_ingot"))
            .inputItems(dust, MELLION)
            .inputFluids(GTMaterials.Oganesson.getFluid(1000))
            .outputItems(ingotHot, MELLION)
            .blastFurnaceTemp(14000)
            .EUt(503316480)
            .duration(856)
            .save(provider)
        ALLOY_BLAST_RECIPES.recipeBuilder(id("molten_proto_halkonite_base"))
            .inputItems(dust, TranscendentMetal, 4)
            .inputItems(dust, Tairitsu, 4)
            .inputItems(dust, Tartarite, 4)
            .inputItems(dust, TitanPrecisionSteel, 2)
            .inputItems(dust, Eternity, 2)
            .inputFluids(DimensionallyTranscendentResidue.getFluid(576))
            .outputFluids(PROTO_HALKONITE_BASE.getFluid(FluidStorageKeys.MOLTEN, 1152))
            .blastFurnaceTemp(48000)
            .EUt(503320000)
            .duration(4080)
            .save(provider)
        CHEMICAL_BATH_RECIPES.recipeBuilder(id("hot_proto_halkonite_ingot"))
            .inputItems(ingot, Infinity)
            .inputFluids(PROTO_HALKONITE_BASE.getFluid(1440))
            .outputItems(ingotHot, PROTO_HALKONITE)
            .EUt(31457280)
            .duration(320)
            .save(provider)
    }
}
