package com.gtladd.gtladditions.data.recipes

import com.gregtechceu.gtceu.api.GTValues.*
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.*
import com.gregtechceu.gtceu.api.data.tag.TagUtil
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType
import com.gregtechceu.gtceu.api.recipe.ResearchRecipeBuilder.StationRecipeBuilder
import com.gregtechceu.gtceu.common.data.GTItems.*
import com.gregtechceu.gtceu.common.data.GTMaterials.*
import com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLER_RECIPES
import com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLY_LINE_RECIPES
import com.gregtechceu.gtceu.data.recipe.CustomTags
import com.gtladd.gtladditions.GTLAdditions.Companion.id
import com.gtladd.gtladditions.common.items.GTLAddItems
import com.gtladd.gtladditions.common.machine.GTLAddMachines
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.api.data.tag.GTLTagPrefix.nanoswarm
import org.gtlcore.gtlcore.common.data.GTLBlocks.QFT_COIL
import org.gtlcore.gtlcore.common.data.GTLBlocks.SPACETIMEBENDINGCORE
import org.gtlcore.gtlcore.common.data.GTLItems.*
import org.gtlcore.gtlcore.common.data.GTLMachines
import org.gtlcore.gtlcore.common.data.GTLMaterials.*
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes.SUPRACHRONAL_ASSEMBLY_LINE_RECIPES
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineA.A_MASS_FABRICATOR
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import java.util.function.Consumer

object PartMachine {
    fun init(provider : Consumer<FinishedRecipe?>){
        ASSEMBLER_RECIPES.recipeBuilder(id("huge_steam_hatch"))
            .inputItems(GTLMachines.LARGE_STEAM_HATCH, 4)
            .inputItems(CustomTags.IV_CIRCUITS, 4)
            .inputItems(ELECTRIC_PUMP_EV, 4)
            .inputItems(ELECTRIC_PUMP_HV, 4)
            .inputItems(ELECTRIC_PUMP_MV, 4)
            .inputItems(FIELD_GENERATOR_EV, 4)
            .inputItems(gear, HSSG, 4)
            .inputItems(getItemStack("ad_astra:desh_plate", 4))
            .inputFluids(SolderingAlloy.getFluid(1296))
            .outputItems(GTLAddMachines.HUGE_STEAM_HATCH)
            .EUt(VA[EV].toLong()).duration(1200)
            .cleanroom(CleanroomType.CLEANROOM).save(provider)
        ASSEMBLY_LINE_RECIPES.recipeBuilder(id("super_parallel_hatch"))
            .inputItems(getItemStack("gtceu:iv_parallel_hatch", 64))
            .inputItems(getItemStack("gtceu:luv_parallel_hatch", 64))
            .inputItems(getItemStack("gtceu:zpm_parallel_hatch", 64))
            .inputItems(getItemStack("gtceu:uv_parallel_hatch", 64))
            .inputItems(getItemStack("gtceu:uhv_parallel_hatch", 64))
            .inputItems(getItemStack("gtceu:uev_parallel_hatch", 64))
            .inputItems(getItemStack("gtceu:uiv_parallel_hatch", 64))
            .inputItems(getItemStack("gtceu:uxv_parallel_hatch", 64))
            .inputItems(getItemStack("gtceu:opv_parallel_hatch", 64))
            .inputItems(getItemStack("gtceu:max_parallel_hatch", 64))
            .inputItems(getItemStack("avaritia:endest_pearl", 64))
            .inputItems(getItemStack("kubejs:chaotic_energy_core", 16))
            .inputItems(getItemStack("kubejs:create_ultimate_battery", 16))
            .inputItems(TagUtil.createModItemTag("circuits/max"), 16)
            .inputItems(getItemStack("gtceu:create_computation", 4))
            .inputItems(getItemStack("minecraft:repeating_command_block", 4))
            .inputFluids(SpaceTime.getFluid(6553600))
            .inputFluids(TranscendentMetal.getFluid(6553600))
            .inputFluids(WhiteDwarfMatter.getFluid(65536000))
            .inputFluids(BlackDwarfMatter.getFluid(65536000))
            .outputItems(GTLAddMachines.SUPER_PARALLEL_HATCH)
            .EUt(VA[UIV].toLong()).duration(5120)
            .stationResearch { b : StationRecipeBuilder? ->
                b !!.researchStack(getItemStack("gtceu:max_parallel_hatch"))
                    .dataStack(TOOL_DATA_MODULE.asStack())
                    .EUt(VA[MAX]).CWUt(67108864, 2147483647)
            }
            .save(provider)
        SUPRACHRONAL_ASSEMBLY_LINE_RECIPES.recipeBuilder(id("me_super_pattern_buffer"))
            .inputItems(getItemStack("gtlcore:spacetimebendingcore", 64))
            .inputItems(getItemStack("kubejs:proto_matter", 64))
            .inputItems(EXTREMELY_ULTIMATE_BATTERY, 16)
            .inputItems(TagUtil.createModItemTag("circuits/uxv"), 16)
            .inputItems(getItemStack("gtceu:me_extend_pattern_buffer", 16))
            .inputItems(getItemStack("gtceu:me_dual_hatch_stock_part_machine", 16))
            .inputItems(EMITTER_UXV, 8)
            .inputItems(getItemStack("kubejs:cosmic_processing_core", 4))
            .inputItems(getItemStack("kubejs:photocoated_hassium_wafer", 64))
            .inputItems(getItemStack("kubejs:photocoated_hassium_wafer", 64))
            .inputItems(getItemStack("gtceu:fine_legendarium_wire", 64))
            .inputItems(getItemStack("gtceu:fine_legendarium_wire", 64))
            .inputItems(getItemStack("gtceu:fine_legendarium_wire", 64))
            .inputItems(getItemStack("gtceu:fine_legendarium_wire", 64))
            .inputItems(getItemStack("gtceu:fine_legendarium_wire", 64))
            .inputItems(getItemStack("gtceu:fine_legendarium_wire", 64))
            .inputFluids(UUMatter.getFluid(32000))
            .inputFluids(LiquidStarlight.getFluid(32000))
            .inputFluids(SuperheavyHAlloy.getFluid(16000))
            .inputFluids(SuperheavyLAlloy.getFluid(16000))
            .outputItems(GTLAddMachines.ME_SUPER_PATTERN_BUFFER)
            .EUt(VA[UIV].toLong()).duration(5120)
            .stationResearch { b : StationRecipeBuilder? ->
                b !!.researchStack(getItemStack("gtceu:me_pattern_buffer_proxy"))
                    .dataStack(TOOL_DATA_MODULE.asStack())
                    .EUt(VA[UXV]).CWUt(640)
            }
            .save(provider)
        SUPRACHRONAL_ASSEMBLY_LINE_RECIPES.recipeBuilder(id("me_super_pattern_buffer_proxy"))
            .inputItems(getItemStack("gtlcore:spacetimecontinuumripper", 16))
            .inputItems(SENSOR_UXV, 16)
            .inputItems(ROBOT_ARM_UXV, 16)
            .inputItems(CONVEYOR_MODULE_UXV, 16)
            .inputItems(getItemStack("gtceu:me_dual_hatch_stock_part_machine", 4))
            .inputItems(TagUtil.createModItemTag("circuits/uxv"), 4)
            .inputItems(getItemStack("kubejs:entangled_singularity", 64))
            .inputItems(getItemStack("ae2:quantum_ring", 64))
            .inputItems(getItemStack("ae2:quantum_link", 32))
            .inputItems(getItemStack("gtceu:fine_legendarium_wire", 64))
            .inputItems(getItemStack("gtceu:fine_legendarium_wire", 64))
            .inputItems(getItemStack("gtceu:fine_legendarium_wire", 64))
            .inputItems(getItemStack("gtceu:fine_legendarium_wire", 64))
            .inputFluids(UUMatter.getFluid(32000))
            .inputFluids(LiquidStarlight.getFluid(32000))
            .inputFluids(SuperheavyHAlloy.getFluid(16000))
            .inputFluids(SuperheavyLAlloy.getFluid(16000))
            .outputItems(GTLAddMachines.ME_SUPER_PATTERN_BUFFER_PROXY)
            .EUt(VA[UIV].toLong()).duration(5120)
            .stationResearch { b : StationRecipeBuilder? ->
                b !!.researchStack(getItemStack("gtladditions:me_super_pattern_buffer"))
                    .dataStack(TOOL_DATA_MODULE.asStack())
                    .EUt(VA[UXV]).CWUt(640)
            }
            .save(provider)
        SUPRACHRONAL_ASSEMBLY_LINE_RECIPES.recipeBuilder(id("wireless_energy_network_input_terminal"))
            .inputItems(GTLAddMachines.WIRELESS_LASER_INPUT_HATCH_67108864A[MAX]!!, 16)
            .inputItems(QFT_COIL, 64)
            .inputItems(MEGA_ULTIMATE_BATTERY, 64)
            .inputItems(A_MASS_FABRICATOR, 24)
            .inputItems(SPACETIMEBENDINGCORE, 64)
            .inputItems(wireGtHex, SpaceTime, 64)
            .inputItems(wireGtHex, Eternity, 32)
            .inputItems(plateDouble, MagnetohydrodynamicallyConstrainedStarMatter, 16)
            .inputItems(getItemStack("kubejs:suprachronal_mainframe_complex", 16))
            .inputItems(getItemStack("kubejs:hypercube", 64))
            .inputFluids(SuperMutatedLivingSolder.getFluid(132710400))
            .inputFluids(ExcitedDtsc.getFluid(128000000))
            .inputFluids(Infinity.getFluid(46080000))
            .outputItems(GTLAddMachines.Wireless_Energy_Network_INPUT_Terminal)
            .EUt(VA[MAX].toLong()).duration(2400)
            .stationResearch { b : StationRecipeBuilder? ->
                b !!.researchStack(GTLAddMachines.WIRELESS_LASER_INPUT_HATCH_67108864A[MAX]!!.asStack())
                    .dataStack(TOOL_DATA_MODULE.asStack())
                    .EUt(VA[MAX]).CWUt(67108864, 2147483647)
            }
            .save(provider)
        SUPRACHRONAL_ASSEMBLY_LINE_RECIPES.recipeBuilder(id("wireless_energy_network_output_terminal"))
            .inputItems(GTLAddMachines.WIRELESS_LASER_OUTPUT_HATCH_67108863A[MAX]!!, 16)
            .inputItems(QFT_COIL, 64)
            .inputItems(MEGA_ULTIMATE_BATTERY, 64)
            .inputItems(A_MASS_FABRICATOR, 24)
            .inputItems(SPACETIMEBENDINGCORE, 64)
            .inputItems(wireGtHex, SpaceTime, 64)
            .inputItems(wireGtHex, Eternity, 32)
            .inputItems(plateDouble, MagnetohydrodynamicallyConstrainedStarMatter, 16)
            .inputItems(getItemStack("kubejs:suprachronal_mainframe_complex", 16))
            .inputItems(getItemStack("kubejs:hypercube", 64))
            .inputFluids(SuperMutatedLivingSolder.getFluid(132710400))
            .inputFluids(ExcitedDtsc.getFluid(128000000))
            .inputFluids(Infinity.getFluid(46080000))
            .outputItems(GTLAddMachines.Wireless_Energy_Network_OUTPUT_Terminal)
            .EUt(VA[MAX].toLong()).duration(2400)
            .stationResearch { b : StationRecipeBuilder? ->
                b !!.researchStack(GTLAddMachines.WIRELESS_LASER_OUTPUT_HATCH_67108863A[MAX]!!.asStack())
                    .dataStack(TOOL_DATA_MODULE.asStack())
                    .EUt(VA[MAX]).CWUt(67108864, 2147483647)
            }
            .save(provider)
        SUPRACHRONAL_ASSEMBLY_LINE_RECIPES.recipeBuilder(id("thread_modifier_hatch"))
            .inputItems(getItemStack("kubejs:giga_chad", 4096))
            .inputItems(GTLAddMachines.SUPER_PARALLEL_HATCH, 4)
            .inputItems(FIELD_GENERATOR_MAX, 4096)
            .inputItems(frameGt, Eternity, 6144)
            .inputItems(nanoswarm, CosmicNeutronium, 8192)
            .inputItems(nanoswarm, Eternity, 8192)
            .inputItems(nanoswarm, BlackDwarfMatter, 8192)
            .inputItems(nanoswarm, WhiteDwarfMatter, 8192)
            .inputItems(GTLAddItems.SPACETIME_LENS, 4096)
            .inputItems(TagUtil.createModItemTag("circuits/uxv"), 131072)
            .inputFluids(Radox.getFluid(589000000))
            .inputFluids(TemporalFluid.getFluid(46080000))
            .inputFluids(Shirabon.getFluid(92160000))
            .inputFluids(CosmicElement.getFluid(18874368000))
            .outputItems(GTLAddMachines.THREAD_MODIFIER_HATCH)
            .EUt(VA[MAX].toLong()).duration(12000)
            .stationResearch { b : StationRecipeBuilder? ->
                b !!.researchStack(GTLAddMachines.SUPER_PARALLEL_HATCH.asStack())
                    .dataStack(TOOL_DATA_MODULE.asStack())
                    .EUt(VA[MAX]).CWUt(67108864, 2147483647)
            }
            .save(provider)
    }
}