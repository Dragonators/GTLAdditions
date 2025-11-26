package com.gtladd.gtladditions.data.recipes

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.GTValues.VEX
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.*
import com.gregtechceu.gtceu.api.recipe.ResearchRecipeBuilder.StationRecipeBuilder
import com.gregtechceu.gtceu.common.data.GTItems
import com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLY_LINE_RECIPES
import com.gregtechceu.gtceu.data.recipe.CustomTags
import com.gtladd.gtladditions.GTLAdditions.Companion.id
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.GOD_FORGE_ENERGY_CASING
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.GOD_FORGE_TRIM_CASING
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.PHONON_CONDUIT
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.SPATIALLY_TRANSCENDENT_GRAVITATIONAL_LENS
import com.gtladd.gtladditions.common.items.GTLAddItems.ASTRAL_ARRAY
import com.gtladd.gtladditions.common.items.GTLAddItems.BLACK_HOLE_SEED
import com.gtladd.gtladditions.common.items.GTLAddItems.PRIMARY_SOC
import com.gtladd.gtladditions.common.items.GTLAddItems.STARGATE_CHEVRON_UPGRADE
import com.gtladd.gtladditions.common.items.GTLAddItems.STARGATE_FRAME_PART
import com.gtladd.gtladditions.common.items.GTLAddItems.STARGATE_SHIELDING_FOIL
import com.gtladd.gtladditions.common.items.GTLAddItems.SUPER_DENSE_MAGMATTER_PLATE
import com.gtladd.gtladditions.common.machine.GTLAddMachines.THREAD_MODIFIER_HATCH
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.APOCALYPTIC_TORSION_QUANTUM_MATRIX
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.ARCANIC_ASTROGRAPH
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.FORGE_OF_THE_ANTICHRIST
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.HELIOFUSION_EXOTICIZER
import com.gtladd.gtladditions.common.material.GTLAddMaterial.CREON
import com.gtladd.gtladditions.common.material.GTLAddMaterial.MELLION
import com.gtladd.gtladditions.common.material.GTLAddMaterial.PHONON_MEDIUM
import com.gtladd.gtladditions.common.material.GTLAddMaterial.PROTO_HALKONITE
import com.gtladd.gtladditions.common.material.GTLAddMaterial.STAR_GATE_CRYSTAL_SLURRY
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.NIGHTMARE_CRAFTING
import net.minecraft.data.recipes.FinishedRecipe
import net.povstalec.sgjourney.common.init.ItemInit
import org.gtlcore.gtlcore.api.data.tag.GTLTagPrefix.nanoswarm
import org.gtlcore.gtlcore.api.machine.multiblock.GTLCleanroomType
import org.gtlcore.gtlcore.common.data.GTLBlocks
import org.gtlcore.gtlcore.common.data.GTLItems.*
import org.gtlcore.gtlcore.common.data.GTLMaterials.*
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes.*
import org.gtlcore.gtlcore.common.recipe.condition.GravityCondition
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import java.util.function.Consumer


object StarGate {
    fun init(provider: Consumer<FinishedRecipe?>) {
        gregTech(provider)
    }

    private fun gregTech(provider: Consumer<FinishedRecipe?>) {
        SUPRACHRONAL_ASSEMBLY_LINE_RECIPES.recipeBuilder(id("ultimate_tea"))
            .inputItems(getItemStack("kubejs:heartofthesmogus", 64))
            .inputItems(getItemStack("kubejs:temporal_matter", 671088640))
            .inputItems(getItemStack("kubejs:dark_matter", 671088640))
            .inputItems(getItemStack("kubejs:extremely_durable_plasma_cell", 1073741824))
            .inputItems(getItemStack("kubejs:time_dilation_containment_unit", 1073741824))
            .inputItems(getItemStack("kubejs:plasma_containment_cell", 1073741824))
            .inputItems(getItemStack("kubejs:empty_laser_cooling_container", 1073741824))
            .inputItems(getItemStack("avaritia:infinity_bucket", 1024))
            .inputFluids(Miracle.getFluid(1000000000000L))
            .inputFluids(MagnetohydrodynamicallyConstrainedStarMatter.getFluid(1000000000000L))
            .inputFluids(PHONON_MEDIUM.getFluid(384000000000L))
            .inputFluids(PrimordialMatter.getFluid(1000000000000L))
            .outputItems(ULTIMATE_TEA)
            .EUt(VEX[26])
            .duration(2000000)
            .stationResearch { b: StationRecipeBuilder? ->
                b!!.researchStack(getItemStack("expatternprovider:fishbig"))
                    .dataStack(GTItems.TOOL_DATA_MODULE.asStack())
                    .EUt(GTValues.VA[GTValues.MAX])
                    .CWUt(262144)
            }
            .save(provider)

        ASSEMBLY_LINE_RECIPES.recipeBuilder(id("infinity_bucket"))
            .inputItems(getItemStack("gtceu:opv_quantum_tank", 1024))
            .inputItems(getItemStack("gtceu:uxv_quantum_tank", 1024))
            .inputItems(getItemStack("gtceu:uiv_quantum_tank", 1024))
            .inputItems(getItemStack("gtceu:uev_quantum_tank", 1024))
            .inputItems(getItemStack("gtceu:uhv_quantum_tank", 1024))
            .inputItems(getItemStack("gtceu:uv_quantum_tank", 1024))
            .inputItems(getItemStack("gtceu:zpm_quantum_tank", 1024))
            .inputItems(getItemStack("gtceu:luv_quantum_tank", 1024))
            .inputItems(getItemStack("gtceu:iv_quantum_tank", 1024))
            .inputItems(plateDense, CosmicNeutronium, 29000)
            .inputItems(plateDense, Infinity, 70000)
            .inputItems(getItemStack("kubejs:eternity_catalyst", 400))
            .inputItems(getItemStack("kubejs:annihilation_constrainer", 1536))
            .inputFluids(Infinity.getFluid(117640000))
            .inputFluids(SpatialFluid.getFluid(144000000))
            .outputItems(getItemStack("avaritia:infinity_bucket"))
            .EUt(VEX[22])
            .duration(20000)
            .stationResearch { b: StationRecipeBuilder? ->
                b!!.researchStack(getItemStack("avaritia:infinity_bucket"))
                    .dataStack(GTItems.TOOL_DATA_MODULE.asStack())
                    .EUt(GTValues.VA[GTValues.MAX])
                    .CWUt(512000)
            }
            .save(provider)

        ASSEMBLY_LINE_RECIPES.recipeBuilder(id("stargate_frame_part"))
            .inputItems(rodLong, Infinity, 2000000)
            .inputItems(rodLong, MELLION, 2000000)
            .inputItems(rodLong, Cosmic, 2000000)
            .inputItems(rodLong, Eternity, 2000000)
            .inputItems(rodLong, CREON, 2000000)
            .inputItems(rodLong, SpaceTime, 2000000)
            .inputItems(rodLong, Echoite, 2000000)
            .inputItems(rodLong, Shirabon, 2000000)
            .inputItems(rodLong, Hypogen, 2000000)
            .inputItems(rodLong, Chaos, 2000000)
            .inputItems(rodLong, MagnetohydrodynamicallyConstrainedStarMatter, 2000000)
            .inputItems(rodLong, PROTO_HALKONITE, 2000000)
            .inputItems(rodLong, WhiteDwarfMatter, 2000000)
            .inputItems(rodLong, Magmatter, 2000000)
            .inputItems(rodLong, BlackDwarfMatter, 2000000)
            .inputItems(rodLong, TranscendentMetal, 2000000)
            .inputFluids(Hypogen.getFluid(10240000000))
            .inputFluids(PHONON_MEDIUM.getFluid(256000000))
            .inputFluids(Cosmic.getFluid(1474560000))
            .inputFluids(ExcitedDtsc.getFluid(5120000000))
            .outputItems(STARGATE_FRAME_PART)
            .EUt(VEX[25])
            .duration(2500000)
            .stationResearch { b: StationRecipeBuilder? ->
                b!!.researchStack(getItemStack("gtceu:eternity_frame"))
                    .dataStack(GTItems.TOOL_DATA_MODULE.asStack())
                    .EUt(GTValues.VA[GTValues.MAX])
                    .CWUt(262144)
            }
            .save(provider)

        SUPRACHRONAL_ASSEMBLY_LINE_RECIPES.recipeBuilder(id("stargate_shielding_foil"))
            .inputItems(GOD_FORGE_ENERGY_CASING, 64)
            .inputItems(getItemStack("kubejs:spacetime_compression_field_generator", 64))
            .inputItems(PHONON_CONDUIT, 64)
            .inputItems(block, Magmatter, 64)
            .inputItems(SUPER_DENSE_MAGMATTER_PLATE, 64)
            .inputItems(plateDense, Cosmic, 64)
            .inputItems(plateDense, Eternity, 64)
            .inputItems(plateDense, PROTO_HALKONITE, 64)
            .inputItems(getItemStack("kubejs:suprachronal_mainframe_complex", 16))
            .inputItems(SUPER_GLUE, 64)
            .inputItems(getItemStack("kubejs:fishbig_fabric", 64))
            .inputItems(nanoswarm, Cosmic, 8192)
            .inputItems(nanoswarm, BlackDwarfMatter, 8192)
            .inputItems(nanoswarm, WhiteDwarfMatter, 8192)
            .inputItems(nanoswarm, Eternity, 8192)
            .inputFluids(Miracle.getFluid(1024000000))
            .inputFluids(PHONON_MEDIUM.getFluid(512000000))
            .inputFluids(Cosmic.getFluid(589824000))
            .inputFluids(ExcitedDtsc.getFluid(512000000))
            .outputItems(STARGATE_SHIELDING_FOIL)
            .EUt(VEX[25])
            .duration(2500000)
            .stationResearch { b: StationRecipeBuilder? ->
                b!!.researchStack(getItemStack("gtceu:magnetohydrodynamicallyconstrainedstarmatter_plate"))
                    .dataStack(GTItems.TOOL_DATA_MODULE.asStack())
                    .EUt(GTValues.VA[GTValues.MAX])
                    .CWUt(262144)
            }
            .save(provider)

        ASSEMBLY_LINE_RECIPES.recipeBuilder(id("universe_stargate_chevron"))
            .inputItems(GTLBlocks.DIMENSION_CONNECTION_CASING, 4096)
            .inputItems(GTLBlocks.CREATE_CASING, 4096)
            .inputItems(SPATIALLY_TRANSCENDENT_GRAVITATIONAL_LENS, 4096)
            .inputItems(GOD_FORGE_TRIM_CASING, 4096)
            .inputItems(frameGt, Eternity, 32000000)
            .inputItems(SUPER_DENSE_MAGMATTER_PLATE, 1024)
            .inputItems(plateDouble, MagnetohydrodynamicallyConstrainedStarMatter, 1024)
            .inputItems(frameGt, MagnetohydrodynamicallyConstrainedStarMatter, 32000000)
            .inputItems(getItemStack("gtceu:exquisite_ruby_gem", 67108864))
            .inputItems(getItemStack("gtceu:exquisite_jasper_gem", 67108864))
            .inputItems(getItemStack("gtceu:exquisite_sapphire_gem", 67108864))
            .inputItems(getItemStack("gtceu:exquisite_magneto_resonatic_gem", 67108864))
            .inputItems(EMITTER_MAX, 64000000)
            .inputItems(ELECTRIC_PISTON_MAX, 64000000)
            .inputItems(FIELD_GENERATOR_MAX, 16000000)
            .inputItems(CustomTags.MAX_CIRCUITS, 32000000)
            .inputFluids(PROTO_HALKONITE.getFluid(10240000000))
            .inputFluids(PHONON_MEDIUM.getFluid(2560000000))
            .inputFluids(Magmatter.getFluid(117964800000))
            .inputFluids(ExcitedDtec.getFluid(512000000000))
            .outputItems(getItemStack("sgjourney:universe_stargate_chevron"))
            .EUt(GTValues.V[GTValues.MAX])
            .duration(5000)
            .stationResearch { b: StationRecipeBuilder? ->
                b!!.researchStack(STARGATE_SHIELDING_FOIL.asStack())
                    .dataStack(GTItems.TOOL_DATA_MODULE.asStack())
                    .EUt(GTValues.VA[GTValues.MAX])
                    .CWUt(262144)
            }
            .save(provider)

        SPS_CRAFTING_RECIPES.recipeBuilder(id("heartofthesmogus"))
            .inputItems(getItemStack("expatternprovider:fishbig", 64))
            .inputItems(MIRACLE_CRYSTAL, 10000000)
            .inputItems(getItemStack("avaritia:ultimate_stew", 64))
            .outputItems(getItemStack("kubejs:heartofthesmogus"))
            .inputFluids(MagnetohydrodynamicallyConstrainedStarMatter.getFluid(230400000000))
            .inputFluids(Miracle.getFluid(460400000000))
            .inputFluids(DragonBlood.getFluid(576000000000))
            .EUt(VEX[24])
            .duration(288000)
            .save(provider)

        AGGREGATION_DEVICE_RECIPES.recipeBuilder(id("reaction_chamber"))
            .notConsumable(getItemStack("kubejs:dragon_stabilizer_core"))
            .inputItems(nanoswarm, Cosmic, 64)
            .inputItems(SUPER_GLUE, 64)
            .inputItems(BLACK_HOLE_SEED, 64)
            .inputItems(block, Magmatter, 64)
            .inputItems(STARGATE_SHIELDING_FOIL, 64)
            .inputItems(getItemStack("kubejs:heartofthesmogus", 4))
            .inputItems(getItemStack("kubejs:nuclear_star", 64))
            .inputItems(block, SpaceTime, 64)
            .outputItems(getItemStack("sgjourney:reaction_chamber", 2))
            .EUt(VEX[26])
            .duration(800000)
            .save(provider)

        ASSEMBLER_MODULE_RECIPES.recipeBuilder("crystal_base")
            .inputItems(ASTRAL_ARRAY, 576000)
            .inputItems(PRIMARY_SOC, 1000000000)
            .inputItems(getItemStack("kubejs:chaotic_energy_core", 1000000))
            .inputItems(nanoswarm, Eternity, 16777216)
            .inputItems(nanoswarm, Cosmic, 16777216)
            .inputItems(getItemStack("expatternprovider:fishbig", 4096))
            .inputItems(SUPER_GLUE, 4194304)
            .inputItems(SUPER_DENSE_MAGMATTER_PLATE, 4096000)
            .inputItems(wireGtHex, Crystalmatrix, 2000000000)
            .inputItems(wireGtHex, Infinity, 1000000000)
            .inputItems(wireGtHex, SpaceTime, 1000000000)
            .inputItems(wireGtHex, Eternity, 1000000000)
            .inputFluids(GradePurifiedWater16.getFluid(14400000000000L))
            .inputFluids(CosmicSuperconductor.getFluid(5760000000000L))
            .inputFluids(STAR_GATE_CRYSTAL_SLURRY.getFluid(128000000000))
            .inputFluids(TranscendentMetal.getFluid(10000000000000L))
            .outputItems(ItemInit.CRYSTAL_BASE.get())
            .addData("SEPMTier", 5)
            .EUt(VEX[22])
            .duration(11520000)
            .save(provider)

        DIMENSIONALLY_TRANSCENDENT_MIXER_RECIPES.recipeBuilder("star_gate_crystal_slurry")
            .inputItems(getItemStack("kubejs:void_matter", 4096))
            .inputItems(getItemStack("kubejs:temporal_matter", 4096))
            .inputItems(getItemStack("kubejs:omni_matter", 4096))
            .inputItems(getItemStack("kubejs:kinetic_matter", 4096))
            .inputItems(getItemStack("kubejs:essentia_matter", 4096))
            .inputItems(getItemStack("kubejs:corporeal_matter", 4096))
            .inputItems(getItemStack("kubejs:amorphous_matter", 4096))
            .inputItems(getItemStack("kubejs:proto_matter", 4096))
            .inputItems(getItemStack("kubejs:dark_matter", 4096))
            .inputFluids(Infinity.getFluid(1000000))
            .inputFluids(Eternity.getFluid(1000000))
            .inputFluids(Chaos.getFluid(1000000))
            .inputFluids(Cosmic.getFluid(1000000))
            .inputFluids(Miracle.getFluid(1000000))
            .inputFluids(SpatialFluid.getFluid(1000000))
            .inputFluids(CosmicNeutronium.getFluid(1000000))
            .inputFluids(MagnetohydrodynamicallyConstrainedStarMatter.getFluid(1000000))
            .inputFluids(Magmatter.getFluid(1000000))
            .inputFluids(PrimordialMatter.getFluid(1000000))
            .inputFluids(SpaceTime.getFluid(1000000))
            .inputFluids(TemporalFluid.getFluid(1000000))
            .inputFluids(Shirabon.getFluid(1000000))
            .inputFluids(PHONON_MEDIUM.getFluid(1000000))
            .inputFluids(ExcitedDtec.getFluid(1000000))
            .inputFluids(ExcitedDtsc.getFluid(1000000))
            .outputFluids(STAR_GATE_CRYSTAL_SLURRY.getFluid(1000))
            .EUt(VEX[22])
            .duration(80000)
            .cleanroom(GTLCleanroomType.LAW_CLEANROOM).addCondition(GravityCondition())
            .save(provider)

        NIGHTMARE_CRAFTING.recipeBuilder(id("stargate_chevron_upgrade"))
            .inputItems(STARGATE_FRAME_PART, 13)
            .inputItems(ELECTRIC_PISTON_MAX, 6)
            .inputItems(FIELD_GENERATOR_MAX, 4)
            .inputItems(getItemStack("sgjourney:universe_stargate_chevron", 4))
            .inputItems(SENSOR_MAX, 2)
            .inputItems(EMITTER_MAX, 2)
            .outputItems(STARGATE_CHEVRON_UPGRADE)
            .duration(2000000)
            .EUt(VEX[22])
            .save(provider)

        NIGHTMARE_CRAFTING.recipeBuilder(id("classic_stargate_ring_block"))
            .inputItems(HELIOFUSION_EXOTICIZER, 18)
            .inputItems(STARGATE_FRAME_PART, 21)
            .inputItems(getItemStack("sgjourney:universe_stargate_chevron", 3))
            .inputItems(STARGATE_SHIELDING_FOIL, 11)
            .inputItems(getItemStack("sgjourney:reaction_chamber", 9))
            .inputItems(FORGE_OF_THE_ANTICHRIST, 9)
            .outputItems(getItemStack("sgjourney:classic_stargate_ring_block"))
            .duration(4000000)
            .EUt(VEX[22])
            .save(provider)

        NIGHTMARE_CRAFTING.recipeBuilder(id("classic_stargate_chevron_block"))
            .inputItems(HELIOFUSION_EXOTICIZER, 16)
            .inputItems(APOCALYPTIC_TORSION_QUANTUM_MATRIX, 12)
            .inputItems(ULTIMATE_TEA, 4)
            .inputItems(GOD_FORGE_ENERGY_CASING, 4)
            .inputItems(STARGATE_CHEVRON_UPGRADE, 4)
            .inputItems(getItemStack("sgjourney:classic_stargate_ring_block"))
            .outputItems(getItemStack("sgjourney:classic_stargate_chevron_block"))
            .duration(4000000)
            .EUt(VEX[22])
            .save(provider)

        NIGHTMARE_CRAFTING.recipeBuilder(id("classic_stargate_base_block"))
            .inputItems(THREAD_MODIFIER_HATCH, 4)
            .inputItems(getItemStack("sgjourney:reaction_chamber", 16))
            .inputItems(ULTIMATE_TEA, 8)
            .inputItems(nanoswarm, Cosmic, 4)
            .inputItems(FORGE_OF_THE_ANTICHRIST, 8)
            .inputItems(SUPER_DENSE_MAGMATTER_PLATE, 4)
            .inputItems(STARGATE_SHIELDING_FOIL, 8)
            .inputItems(STARGATE_CHEVRON_UPGRADE, 2)
            .inputItems(getItemStack("kubejs:create_ultimate_battery", 4))
            .inputItems(getItemStack("gtlcore:item_infinity_cell"))
            .inputItems(ASTRAL_ARRAY, 6)
            .inputItems(ARCANIC_ASTROGRAPH, 4)
            .inputItems(getItemStack("sgjourney:classic_stargate_chevron_block", 8))
            .inputItems(APOCALYPTIC_TORSION_QUANTUM_MATRIX, 2)
            .inputItems(ItemInit.CRYSTAL_BASE.get())
            .inputItems(getItemStack("gtlcore:fluid_infinity_cell"))
            .outputItems(getItemStack("sgjourney:classic_stargate_base_block"))
            .duration(16000000)
            .EUt(VEX[22])
            .save(provider)
    }
}