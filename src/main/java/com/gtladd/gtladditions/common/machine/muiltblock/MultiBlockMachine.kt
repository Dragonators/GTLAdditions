package com.gtladd.gtladditions.common.machine.muiltblock

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.data.RotationState
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.frameGt
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo
import com.gregtechceu.gtceu.api.pattern.Predicates
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic
import com.gregtechceu.gtceu.api.recipe.logic.OCParams
import com.gregtechceu.gtceu.api.recipe.logic.OCResult
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier
import com.gregtechceu.gtceu.common.data.GCyMRecipeTypes.ALLOY_BLAST_RECIPES
import com.gregtechceu.gtceu.common.data.GTBlocks.*
import com.gregtechceu.gtceu.common.data.GTMachines
import com.gregtechceu.gtceu.common.data.GTMaterials.*
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers
import com.gregtechceu.gtceu.common.data.GTRecipeTypes.*
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.machine.GTLAddPartAbility
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddCoilWorkableElectricParallelHatchMultipleRecipesMachine
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricParallelHatchMultipleRecipesMachine
import com.gtladd.gtladditions.api.machine.mutable.MutableElectricMultiblockMachine
import com.gtladd.gtladditions.api.registry.GTLAddRegistration.Companion.REGISTRATE
import com.gtladd.gtladditions.client.render.machine.ForgeOfAntichristRenderer
import com.gtladd.gtladditions.client.render.machine.HeartOfTheUniverseRenderer
import com.gtladd.gtladditions.client.render.machine.LightHunterSpaceStationRenderer
import com.gtladd.gtladditions.client.render.machine.PartWorkableCasingMachineRenderer
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.CENTRAL_GRAVITON_FLOW_REGULATOR
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.GOD_FORGE_ENERGY_CASING
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.GOD_FORGE_INNER_CASING
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.GOD_FORGE_SUPPORT_CASING
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.GOD_FORGE_TRIM_CASING
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.MEDIARY_GRAVITON_FLOW_REGULATOR
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.PHONON_CONDUIT
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.QUANTUM_GLASS
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.REMOTE_GRAVITON_FLOW_REGULATOR
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.SPATIALLY_TRANSCENDENT_GRAVITATIONAL_LENS
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.SUPRACHRONAL_MAGNETIC_CONFINEMENT_CASING
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.TEMPORAL_ANCHOR_FIELD_CASING
import com.gtladd.gtladditions.common.machine.GTLAddMachines
import com.gtladd.gtladditions.common.machine.GTLAddMachines.ME_SUPER_PATTERN_BUFFER
import com.gtladd.gtladditions.common.machine.GTLAddMachines.ME_SUPER_PATTERN_BUFFER_PROXY
import com.gtladd.gtladditions.common.machine.GTLAddMachines.WIRELESS_LASER_INPUT_HATCH_67108864A
import com.gtladd.gtladditions.common.machine.GTLAddMachines.Wireless_Energy_Network_OUTPUT_Terminal
import com.gtladd.gtladditions.common.machine.muiltblock.controller.*
import com.gtladd.gtladditions.common.machine.muiltblock.controller.module.*
import com.gtladd.gtladditions.common.machine.muiltblock.structure.*
import com.gtladd.gtladditions.common.modify.GTLAddCreativeModeTabs
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.BIOLOGICAL_SIMULATION
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.CHAOS_WEAVE
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.CHAOTIC_ALCHEMY
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.EM_RESONANCE_CONVERSION_FIELD
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.GENESIS_ENGINE
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.INTER_STELLAR
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.MATTER_EXOTIC
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.MOLECULAR_DECONSTRUCTION
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.NIGHTMARE_CRAFTING
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.PHOTON_MATRIX_ETCH
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.STAR_CORE_STRIPPER
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.STELLAR_LGNITION
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.TECTONIC_FAULT_GENERATOR
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.VOIDFLUX_REACTION
import com.gtladd.gtladditions.utils.CommonUtils.createObfuscatedDeleteComponent
import com.hepdd.gtmthings.data.CustomMachines
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.ChatFormatting
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.level.block.Block
import org.gtlcore.gtlcore.GTLCore
import org.gtlcore.gtlcore.api.pattern.GTLPredicates
import org.gtlcore.gtlcore.client.renderer.machine.EyeOfHarmonyRenderer
import org.gtlcore.gtlcore.common.block.BlockMap
import org.gtlcore.gtlcore.common.block.GTLFusionCasingBlock
import org.gtlcore.gtlcore.common.data.GTLBlocks.*
import org.gtlcore.gtlcore.common.data.GTLMachines
import org.gtlcore.gtlcore.common.data.GTLMachines.GTAEMachines.ME_EXTENDED_EXPORT_BUFFER
import org.gtlcore.gtlcore.common.data.GTLMachines.GTAEMachines.STOCKING_IMPORT_BUS_ME
import org.gtlcore.gtlcore.common.data.GTLMaterials.Mithril
import org.gtlcore.gtlcore.common.data.GTLMaterials.QuantumChromodynamicallyConfinedMatter
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes.*
import org.gtlcore.gtlcore.config.ConfigHolder
import org.gtlcore.gtlcore.utils.Registries.getBlock
import org.gtlcore.gtlcore.utils.StructureSlicer
import java.util.function.Function
import kotlin.math.pow

object MultiBlockMachine {

    val NEXUS_SATELLITE_FACTORY_MKI: MultiblockMachineDefinition
    val NEXUS_SATELLITE_FACTORY_MKII: MultiblockMachineDefinition
    val NEXUS_SATELLITE_FACTORY_MKIII: MultiblockMachineDefinition
    val NEXUS_SATELLITE_FACTORY_MKIV: MultiblockMachineDefinition
    val LUCID_ETCHDREAMER: MultiblockMachineDefinition
    val ATOMIC_TRANSMUTATIOON_CORE: MultiblockMachineDefinition
    val ASTRAL_CONVERGENCE_NEXUS: MultiblockMachineDefinition
    val NEBULA_REAPER: MultiblockMachineDefinition
    val ARCANIC_ASTROGRAPH: MultiblockMachineDefinition
    val ARCANE_CACHE_VAULT: MultiblockMachineDefinition
    val DRACONIC_COLLAPSE_CORE: MultiblockMachineDefinition
    val TITAN_CRIP_EARTHBORE: MultiblockMachineDefinition
    val BIOLOGICAL_SIMULATION_LABORATORY: MultiblockMachineDefinition
    val DIMENSIONALLY_TRANSCENDENT_CHEMICAL_PLANT: MultiblockMachineDefinition
    val QUANTUM_SYPHON_MATRIX: MultiblockMachineDefinition
    val FUXI_BAGUA_HEAVEN_FORGING_FURNACE: MultiblockMachineDefinition
    val ANTIENTROPY_CONDENSATION_CENTER: MultiblockMachineDefinition
    val TAIXU_TURBID_ARRAY: MultiblockMachineDefinition
    val INFERNO_CLEFT_SMELTING_VAULT: MultiblockMachineDefinition
    val SKELETON_SHIFT_RIFT_ENGINE: MultiblockMachineDefinition
    val APOCALYPTIC_TORSION_QUANTUM_MATRIX: MultiblockMachineDefinition
    val FORGE_OF_THE_ANTICHRIST: MultiblockMachineDefinition
    val HELIOFUSION_EXOTICIZER: MultiblockMachineDefinition
    val HELIOFLARE_POWER_FORGE: MultiblockMachineDefinition
    val HELIOFLUIX_MELTING_CORE: MultiblockMachineDefinition
    val HELIOTHERMAL_PLASMA_FABRICATOR: MultiblockMachineDefinition
    val HEART_OF_THE_UNIVERSE: MultiblockMachineDefinition
    val LIGHT_HUNTER_SPACE_STATION: MultiblockMachineDefinition
    val DIMENSION_FOCUS_INFINITY_CRAFTING_ARRAY: MultiblockMachineDefinition
    val MACRO_ATOMIC_RESONANT_FRAGMENT_STRIPPER: MultiblockMachineDefinition?

    init {
        REGISTRATE.creativeModeTab { GTLAddCreativeModeTabs.GTLADD_MACHINE }
        NEXUS_SATELLITE_FACTORY_MKI = REGISTRATE.multiblock("nexus_satellite_factory_mk1",
            Function { LightHunterSpaceStationModuleBase(it) })
            .nonYAxisRotation()
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.nexus_satellite_factory.tooltip.0"))
            .tooltipTextMaxParallels(Int.MAX_VALUE.toString())
            .tooltipTextLaser()
            .tooltipTextMultiRecipes()
            .tooltipTextRecipeTypes(LATHE_RECIPES, BENDER_RECIPES, COMPRESSOR_RECIPES, FORGE_HAMMER_RECIPES, CUTTER_RECIPES,
                EXTRUDER_RECIPES, MIXER_RECIPES, WIREMILL_RECIPES, FORMING_PRESS_RECIPES, POLARIZER_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(LATHE_RECIPES) // 车床
            .recipeType(BENDER_RECIPES) // 卷板机
            .recipeType(COMPRESSOR_RECIPES) // 压缩机
            .recipeType(FORGE_HAMMER_RECIPES) // 锻造锤
            .recipeType(CUTTER_RECIPES) // 切割机
            .recipeType(EXTRUDER_RECIPES) // 压模器
            .recipeType(MIXER_RECIPES) // 搅拌机
            .recipeType(WIREMILL_RECIPES) // 线材轧机
            .recipeType(FORMING_PRESS_RECIPES) // 冲压车床
            .recipeType(POLARIZER_RECIPES) // 两极磁化机
            .appearanceBlock(HIGH_POWER_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructureE.LIGHT_HUNTER_SPACE_STATION_MODULE!!
                    .where("~", Predicates.controller(Predicates.blocks(definition!!.get())))
                    .where("N", Predicates.blocks(getBlock("kubejs:module_connector")))
                    .where("J", Predicates.blocks(getBlock("kubejs:restraint_device")))
                    .where("O", Predicates.blocks(getBlock("gtceu:high_power_casing"))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS))
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                        .or(Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1)))
                    .where("C", Predicates.blocks(getBlock("gtceu:atomic_casing")))
                    .where("L", Predicates.blocks(getBlock("kubejs:spacetime_assembly_line_unit")))
                    .where("E", Predicates.blocks(getBlock("gtlcore:hyper_core")))
                    .where("M", Predicates.blocks(getBlock("kubejs:dimensional_bridge_casing")))
                    .where("D", Predicates.blocks(getBlock("gtlcore:dragon_strength_tritanium_casing")))
                    .where("F", Predicates.blocks(getBlock("gtlcore:dimensionally_transcendent_casing")))
                    .where("H", Predicates.blocks(getBlock("gtlcore:naquadah_alloy_casing")))
                    .where("A", Predicates.blocks(getBlock("gtlcore:echo_casing")))
                    .where("K", Predicates.blocks(getBlock("gtlcore:dimension_injection_casing")))
                    .where("G", Predicates.blocks(getBlock("gtceu:uhv_machine_casing")))
                    .where("B", Predicates.blocks(getBlock("gtceu:uxv_machine_casing")))
                    .where("I", Predicates.blocks(getBlock("gtlcore:uxv_hermetic_casing")))
                    .build()
            }
            .workableCasingRenderer(
                GTCEu.id("block/casings/hpca/high_power_casing"),
                GTCEu.id("block/multiblock/fusion_reactor")
            )
            .register()

        NEXUS_SATELLITE_FACTORY_MKII = REGISTRATE.multiblock("nexus_satellite_factory_mk2",
            Function { LightHunterSpaceStationModuleBase(it) })
            .nonYAxisRotation()
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.nexus_satellite_factory.tooltip.0"))
            .tooltipTextMaxParallels(Int.MAX_VALUE.toString())
            .tooltipTextLaser()
            .tooltipTextMultiRecipes()
            .tooltipTextRecipeTypes(ROCK_BREAKER_RECIPES, ORE_WASHER_RECIPES, CENTRIFUGE_RECIPES, ELECTROLYZER_RECIPES,
                SIFTER_RECIPES, MACERATOR_RECIPES, DEHYDRATOR_RECIPES, THERMAL_CENTRIFUGE_RECIPES, ELECTROMAGNETIC_SEPARATOR_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(ROCK_BREAKER_RECIPES) // 碎岩机
            .recipeType(ORE_WASHER_RECIPES) // 洗矿机
            .recipeType(CENTRIFUGE_RECIPES) // 离心机
            .recipeType(ELECTROLYZER_RECIPES) // 电解机
            .recipeType(SIFTER_RECIPES) // 筛选机
            .recipeType(MACERATOR_RECIPES) // 研磨机
            .recipeType(DEHYDRATOR_RECIPES) // 脱水机
            .recipeType(THERMAL_CENTRIFUGE_RECIPES) // 热力离心机
            .recipeType(ELECTROMAGNETIC_SEPARATOR_RECIPES) // 电磁选矿机
            .appearanceBlock(HIGH_POWER_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructureE.LIGHT_HUNTER_SPACE_STATION_MODULE!!
                    .where("~", Predicates.controller(Predicates.blocks(definition!!.get())))
                    .where("N", Predicates.blocks(getBlock("kubejs:module_connector")))
                    .where("J", Predicates.blocks(getBlock("kubejs:restraint_device")))
                    .where("O", Predicates.blocks(getBlock("gtceu:high_power_casing"))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS))
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                        .or(Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1)))
                    .where("C", Predicates.blocks(getBlock("gtceu:atomic_casing")))
                    .where("L", Predicates.blocks(getBlock("kubejs:spacetime_assembly_line_unit")))
                    .where("E", Predicates.blocks(getBlock("gtlcore:hyper_core")))
                    .where("M", Predicates.blocks(getBlock("kubejs:dimensional_bridge_casing")))
                    .where("D", Predicates.blocks(getBlock("gtlcore:dragon_strength_tritanium_casing")))
                    .where("F", Predicates.blocks(getBlock("gtlcore:dimensionally_transcendent_casing")))
                    .where("H", Predicates.blocks(getBlock("gtlcore:naquadah_alloy_casing")))
                    .where("A", Predicates.blocks(getBlock("gtlcore:echo_casing")))
                    .where("K", Predicates.blocks(getBlock("gtlcore:dimension_injection_casing")))
                    .where("G", Predicates.blocks(getBlock("gtceu:uhv_machine_casing")))
                    .where("B", Predicates.blocks(getBlock("gtceu:uxv_machine_casing")))
                    .where("I", Predicates.blocks(getBlock("gtlcore:uxv_hermetic_casing")))
                    .build()
            }
            .workableCasingRenderer(
                GTCEu.id("block/casings/hpca/high_power_casing"),
                GTCEu.id("block/multiblock/fusion_reactor")
            )
            .register()

        NEXUS_SATELLITE_FACTORY_MKIII = REGISTRATE.multiblock("nexus_satellite_factory_mk3",
            Function { LightHunterSpaceStationModuleBase(it) })
            .nonYAxisRotation()
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.nexus_satellite_factory.tooltip.0"))
            .tooltipTextMaxParallels(Int.MAX_VALUE.toString())
            .tooltipTextLaser()
            .tooltipTextMultiRecipes()
            .tooltipTextRecipeTypes(EVAPORATION_RECIPES, AUTOCLAVE_RECIPES, EXTRACTOR_RECIPES, BREWING_RECIPES, FERMENTING_RECIPES,
                DISTILLERY_RECIPES, DISTILLATION_RECIPES, FLUID_HEATER_RECIPES, FLUID_SOLIDFICATION_RECIPES, CHEMICAL_BATH_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(EVAPORATION_RECIPES) // 蒸发
            .recipeType(AUTOCLAVE_RECIPES) // 高压釜
            .recipeType(EXTRACTOR_RECIPES) // 提取机
            .recipeType(BREWING_RECIPES) // 酿造机
            .recipeType(FERMENTING_RECIPES) // 发酵槽
            .recipeType(DISTILLERY_RECIPES) // 蒸馏室
            .recipeType(DISTILLATION_RECIPES) // 蒸馏塔
            .recipeType(FLUID_HEATER_RECIPES) // 流体加热机
            .recipeType(FLUID_SOLIDFICATION_RECIPES) // 流体固化机
            .recipeType(CHEMICAL_BATH_RECIPES) // 化学浸洗机
            .appearanceBlock(HIGH_POWER_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructureE.LIGHT_HUNTER_SPACE_STATION_MODULE!!
                    .where("~", Predicates.controller(Predicates.blocks(definition!!.get())))
                    .where("N", Predicates.blocks(getBlock("kubejs:module_connector")))
                    .where("J", Predicates.blocks(getBlock("kubejs:restraint_device")))
                    .where("O", Predicates.blocks(getBlock("gtceu:high_power_casing"))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS))
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                        .or(Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1)))
                    .where("C", Predicates.blocks(getBlock("gtceu:atomic_casing")))
                    .where("L", Predicates.blocks(getBlock("kubejs:spacetime_assembly_line_unit")))
                    .where("E", Predicates.blocks(getBlock("gtlcore:hyper_core")))
                    .where("M", Predicates.blocks(getBlock("kubejs:dimensional_bridge_casing")))
                    .where("D", Predicates.blocks(getBlock("gtlcore:dragon_strength_tritanium_casing")))
                    .where("F", Predicates.blocks(getBlock("gtlcore:dimensionally_transcendent_casing")))
                    .where("H", Predicates.blocks(getBlock("gtlcore:naquadah_alloy_casing")))
                    .where("A", Predicates.blocks(getBlock("gtlcore:echo_casing")))
                    .where("K", Predicates.blocks(getBlock("gtlcore:dimension_injection_casing")))
                    .where("G", Predicates.blocks(getBlock("gtceu:uhv_machine_casing")))
                    .where("B", Predicates.blocks(getBlock("gtceu:uxv_machine_casing")))
                    .where("I", Predicates.blocks(getBlock("gtlcore:uxv_hermetic_casing")))
                    .build()
            }
            .workableCasingRenderer(
                GTCEu.id("block/casings/hpca/high_power_casing"),
                GTCEu.id("block/multiblock/fusion_reactor")
            )
            .register()

        NEXUS_SATELLITE_FACTORY_MKIV = REGISTRATE.multiblock("nexus_satellite_factory_mk4",
            Function { LightHunterSpaceStationModuleBase(it) })
            .nonYAxisRotation()
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.nexus_satellite_factory.tooltip.0"))
            .tooltipTextMaxParallels(Int.MAX_VALUE.toString())
            .tooltipTextLaser()
            .tooltipTextMultiRecipes()
            .tooltipTextRecipeTypes(CANNER_RECIPES, ARC_FURNACE_RECIPES, LIGHTNING_PROCESSOR_RECIPES,
                ASSEMBLER_RECIPES, PRECISION_ASSEMBLER_RECIPES, CIRCUIT_ASSEMBLER_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(CANNER_RECIPES) // 装罐机
            .recipeType(ARC_FURNACE_RECIPES) // 电弧炉
            .recipeType(LIGHTNING_PROCESSOR_RECIPES) // 闪电处理
            .recipeType(ASSEMBLER_RECIPES) // 组装机
            .recipeType(PRECISION_ASSEMBLER_RECIPES) // 精密组装
            .recipeType(CIRCUIT_ASSEMBLER_RECIPES) // 电路组装机
            .appearanceBlock(HIGH_POWER_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructureE.LIGHT_HUNTER_SPACE_STATION_MODULE!!
                    .where("~", Predicates.controller(Predicates.blocks(definition!!.get())))
                    .where("N", Predicates.blocks(getBlock("kubejs:module_connector")))
                    .where("J", Predicates.blocks(getBlock("kubejs:restraint_device")))
                    .where("O", Predicates.blocks(getBlock("gtceu:high_power_casing"))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS))
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                        .or(Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1)))
                    .where("C", Predicates.blocks(getBlock("gtceu:atomic_casing")))
                    .where("L", Predicates.blocks(getBlock("kubejs:spacetime_assembly_line_unit")))
                    .where("E", Predicates.blocks(getBlock("gtlcore:hyper_core")))
                    .where("M", Predicates.blocks(getBlock("kubejs:dimensional_bridge_casing")))
                    .where("D", Predicates.blocks(getBlock("gtlcore:dragon_strength_tritanium_casing")))
                    .where("F", Predicates.blocks(getBlock("gtlcore:dimensionally_transcendent_casing")))
                    .where("H", Predicates.blocks(getBlock("gtlcore:naquadah_alloy_casing")))
                    .where("A", Predicates.blocks(getBlock("gtlcore:echo_casing")))
                    .where("K", Predicates.blocks(getBlock("gtlcore:dimension_injection_casing")))
                    .where("G", Predicates.blocks(getBlock("gtceu:uhv_machine_casing")))
                    .where("B", Predicates.blocks(getBlock("gtceu:uxv_machine_casing")))
                    .where("I", Predicates.blocks(getBlock("gtlcore:uxv_hermetic_casing")))
                    .build()
            }
            .workableCasingRenderer(
                GTCEu.id("block/casings/hpca/high_power_casing"),
                GTCEu.id("block/multiblock/fusion_reactor")
            )
            .register()

        LUCID_ETCHDREAMER = REGISTRATE.multiblock("lucid_etchdreamer",
            Function { GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(it)})
            .nonYAxisRotation()
            .tooltipTextCoilParallel()
            .tooltipTextLaser()
            .tooltipTextMultiRecipes()
            .tooltipTextRecipeTypes(PHOTON_MATRIX_ETCH)
            .coilParallelDisplay()
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(PHOTON_MATRIX_ETCH)
            .appearanceBlock(IRIDIUM_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.LUCID_ETCHDREAMER_STRUCTURE!!
                    .where("I", Predicates.controller(Predicates.blocks(definition!!.get())))
                    .where("A", Predicates.blocks(IRIDIUM_CASING.get())
                        .or(Predicates.autoAbilities(*definition.recipeTypes))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1))
                        .or(Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1)))
                    .where("D", Predicates.heatingCoils())
                    .where("E", Predicates.blocks(getBlock("kubejs:neutronium_pipe_casing")))
                    .where("B", Predicates.blocks(DIMENSIONALLY_TRANSCENDENT_CASING.get()))
                    .where("C", Predicates.blocks(DIMENSION_INJECTION_CASING.get()))
                    .where("E", Predicates.blocks(getBlock("kubejs:neutronium_pipe_casing")))
                    .where("G", Predicates.blocks(GRAVITON_FIELD_CONSTRAINT_CASING.get()))
                    .where("F", Predicates.blocks(CLEANROOM_GLASS.get()))
                    .where("H", Predicates.blocks(getBlock("kubejs:annihilate_core")))
                    .where(" ", Predicates.any())
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/iridium_casing"),
                GTCEu.id("block/multiblock/gcym/large_engraving_laser")
            )
            .register()

        ATOMIC_TRANSMUTATIOON_CORE = REGISTRATE.multiblock("atomic_transmutation_core",
            Function { GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(it) })
            .noneRotation()
            .tooltipTextCoilParallel()
            .tooltipTextLaser()
            .tooltipTextMultiRecipes()
            .tooltipTextRecipeTypes(EM_RESONANCE_CONVERSION_FIELD)
            .coilParallelDisplay()
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(EM_RESONANCE_CONVERSION_FIELD)
            .appearanceBlock(ALUMINIUM_BRONZE_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                FactoryBlockPattern.start()
                    .aisle("AAAAAAAAA", "AAAAAAAAA", "ABBBBBBBA", "ABBBBBBBA", "ABBBBBBBA", "ABBBBBBBA", "AAAAAAAAA")
                    .aisle("AAAAAAAAA", "ACCCCCCCA", "B       B", "B       B", "B       B", "B       B", "AAAAAAAAA")
                    .aisle("AAAAAAAAA", "ACDDDDDCA", "B       B", "B       B", "B       B", "B       B", "AAAAAAAAA")
                    .aisle("AAAAAAAAA", "ACDDDDDCA", "B       B", "B       B", "B       B", "B       B", "AAAAAAAAA")
                    .aisle("AAAAAAAAA", "ACDDDDDCA", "B       B", "B       B", "B       B", "B       B", "AAAA~AAAA")
                    .aisle("AAAAAAAAA", "ACDDDDDCA", "B       B", "B       B", "B       B", "B       B", "AAAAAAAAA")
                    .aisle("AAAAAAAAA", "ACDDDDDCA", "B       B", "B       B", "B       B", "B       B", "AAAAAAAAA")
                    .aisle("AAAAAAAAA", "ACCCCCCCA", "B       B", "B       B", "B       B", "B       B", "AAAAAAAAA")
                    .aisle("AAAAAAAAA", "AAAAAAAAA", "ABBBBBBBA", "ABBBBBBBA", "ABBBBBBBA", "ABBBBBBBA", "AAAAAAAAA")
                    .where("~", Predicates.controller(Predicates.blocks(definition!!.get())))
                    .where("A", Predicates.blocks(ALUMINIUM_BRONZE_CASING.get())
                        .or(Predicates.autoAbilities(*definition.recipeTypes))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1))
                        .or(Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1)))
                    .where("C", Predicates.heatingCoils())
                    .where("D", Predicates.blocks(getBlock("kubejs:infused_obsidian")))
                    .where("B", Predicates.blocks(CLEANROOM_GLASS.get()))
                    .where(" ", Predicates.any())
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/aluminium_bronze_casing"),
                GTCEu.id("block/multiblock/cleanroom")
            )
            .register()

        ASTRAL_CONVERGENCE_NEXUS = REGISTRATE.multiblock("astral_convergence_nexus",
            Function { AdvancedSpaceElevatorModuleMachine(it) })
            .nonYAxisRotation()
            .tooltipTextMaxParallels(Component.translatable("gtceu.multiblock.max_parallel.space_elevator_module"))
            .tooltipTextLaser()
            .tooltipTextMultiRecipes()
            .tooltipTextRecipeTypes(ASSEMBLER_MODULE_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(ASSEMBLER_MODULE_RECIPES) // 太空组装
            .appearanceBlock(SPACE_ELEVATOR_MECHANICAL_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                FactoryBlockPattern.start()
                    .aisle("aaa", "bcb", "bbb", "bbb", "bbb")
                    .aisle("aaa", "bbb", "bbb", "bbb", "bbb")
                    .aisle("aaa", "bbb", "bbb", "b~b", "bbb")
                    .where("~", Predicates.controller(Predicates.blocks(definition!!.get())))
                    .where("b", Predicates.blocks(SPACE_ELEVATOR_MECHANICAL_CASING.get())
                        .or(Predicates.autoAbilities(*definition.recipeTypes))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1))
                        .or(Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1)))
                    .where("a", Predicates.blocks(getBlock("kubejs:module_base")))
                    .where("c", Predicates.blocks(getBlock("kubejs:module_connector")))
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/space_elevator_mechanical_casing"),
                GTCEu.id("block/multiblock/gcym/large_assembler")
            )
            .register()

        NEBULA_REAPER = REGISTRATE.multiblock("nebula_reaper",
            Function { AdvancedSpaceElevatorModuleMachine(it) })
            .nonYAxisRotation()
            .tooltipTextMaxParallels(Component.translatable("gtceu.multiblock.max_parallel.space_elevator_module"))
            .tooltipTextLaser()
            .tooltipTextMultiRecipes()
            .tooltipTextRecipeTypes(MINER_MODULE_RECIPES, DRILLING_MODULE_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(MINER_MODULE_RECIPES) // 太空采矿
            .recipeType(DRILLING_MODULE_RECIPES) // 太空钻井
            .appearanceBlock(SPACE_ELEVATOR_MECHANICAL_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                FactoryBlockPattern.start()
                    .aisle("aaa", "bcb", "bbb", "bbb", "bbb")
                    .aisle("aaa", "bbb", "bbb", "bbb", "bbb")
                    .aisle("aaa", "bbb", "bbb", "b~b", "bbb")
                    .where("~", Predicates.controller(Predicates.blocks(definition!!.get())))
                    .where("b", Predicates.blocks(SPACE_ELEVATOR_MECHANICAL_CASING.get())
                        .or(Predicates.autoAbilities(*definition.recipeTypes))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1))
                        .or(Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1)))
                    .where("a", Predicates.blocks(getBlock("kubejs:module_base")))
                    .where("c", Predicates.blocks(getBlock("kubejs:module_connector")))
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/space_elevator_mechanical_casing"),
                GTCEu.id("block/multiblock/gcym/large_assembler")
            )
            .register()

        ARCANIC_ASTROGRAPH = REGISTRATE.multiblock("arcanic_astrograph",
            Function { ArcanicAstrograph(it) })
            .nonYAxisRotation()
            .recipeType(COSMOS_SIMULATION_RECIPES)
            .recipeModifier { machine: MetaMachine, recipe: GTRecipe?, params: OCParams?, result: OCResult? ->
                ArcanicAstrograph.recipeModifier(machine, recipe!!, params!!, result!!)
            }
            .tooltips(*arrayOf<Component>(Component.translatable("gtladditions.multiblock.base_parallel", Component.literal("2048").withStyle(ChatFormatting.GOLD))))
            .tooltips(*arrayOf<Component>(Component.translatable("gtceu.multiblock.arcanic_astrograph")))
            .tooltips(*arrayOf<Component>(Component.translatable("gtceu.machine.eye_of_harmony.tooltip.0")))
            .tooltips(*arrayOf<Component>(Component.translatable("gtceu.machine.eye_of_harmony.tooltip.1")))
            .tooltips(*arrayOf<Component>(Component.translatable("gtceu.machine.eye_of_harmony.tooltip.2")))
            .tooltips(*arrayOf<Component>(Component.translatable("gtceu.machine.eye_of_harmony.tooltip.3")))
            .tooltips(*arrayOf<Component>(Component.translatable("gtceu.machine.eye_of_harmony.tooltip.4")))
            .tooltips(*arrayOf<Component>(Component.translatable("gtceu.machine.eye_of_harmony.tooltip.5")))
            .tooltips(*arrayOf<Component>(Component.translatable("gtceu.machine.eye_of_harmony.tooltip.6")))
            .tooltips(*arrayOf<Component>(Component.translatable("gtceu.machine.available_recipe_map_1.tooltip", Component.translatable("gtceu.cosmos_simulation"))))
            .tooltips(Component.translatable("gtladditions.multiblock.arcanic_astrograph.tooltip.0"))
            .tooltips(Component.translatable("gtladditions.multiblock.arcanic_astrograph.tooltip.1"))
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .appearanceBlock(HIGH_POWER_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.EYE_OF_HARMONY_STRUCTURE!!
                    .where('~', Predicates.controller(Predicates.blocks(definition!!.get())))
                    .where('A', Predicates.blocks(CREATE_CASING.get()))
                    .where('B', Predicates.blocks(HIGH_POWER_CASING.get())
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(1))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1)))
                    .where('D', Predicates.blocks(DIMENSION_INJECTION_CASING.get()))
                    .where('E', Predicates.blocks(getBlock("kubejs:dimension_creation_casing")))
                    .where('F', Predicates.blocks(getBlock("kubejs:spacetime_compression_field_generator")))
                    .where('G', Predicates.blocks(getBlock("kubejs:dimensional_stability_casing")))
                    .where(" ", Predicates.any())
                    .build()
            }
            .renderer { EyeOfHarmonyRenderer() }
            .hasTESR(true)
            .register()

        ARCANE_CACHE_VAULT = REGISTRATE.multiblock("arcane_cache_vault",
            Function { GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(it)})
            .allRotation()
            .tooltipTextCoilParallel()
            .tooltipTextLaser()
            .tooltipTextMultiRecipes()
            .tooltipTextRecipeTypes(PACKER_RECIPES)
            .coilParallelDisplay()
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(PACKER_RECIPES)
            .appearanceBlock(PIKYONIUM_MACHINE_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                FactoryBlockPattern.start()
                    .aisle("AAA", "AAA", "AAA")
                    .aisle("AAA", "ABA", "AAA")
                    .aisle("AAA", "ABA", "AAA")
                    .aisle("AAA", "ABA", "AAA")
                    .aisle("AAA", "ABA", "AAA")
                    .aisle("AAA", "A~A", "AAA")
                    .where("~", Predicates.controller(Predicates.blocks(definition!!.get())))
                    .where("A", Predicates.blocks(PIKYONIUM_MACHINE_CASING.get())
                        .or(Predicates.autoAbilities(*definition.recipeTypes))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1))
                        .or(Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1)))
                    .where("B", Predicates.heatingCoils())
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/pikyonium_machine_casing"),
                GTCEu.id("block/multiblock/gcym/large_packer")
            )
            .register()

        DRACONIC_COLLAPSE_CORE = REGISTRATE.multiblock(
            "draconic_collapse_core",
            Function {
                object : MutableElectricMultiblockMachine(it) {
                    override fun getMaxParallel(): Int {
                        return 8.0.pow(tier - 10).toInt()
                    }
                }
            })
            .nonYAxisRotation()
            .tooltipTextKey(Component.translatable("gtceu.multiblock.max_parallel.draconic_collapse_core"))
            .tooltipOnlyTextLaser()
            .tooltipTextPerfectOverclock()
            .tooltipTextRecipeTypes(AGGREGATION_DEVICE_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(AGGREGATION_DEVICE_RECIPES)
            .recipeModifiers(
                { machine: MetaMachine?, recipe: GTRecipe?, params: OCParams?, result: OCResult? ->
                    GTRecipeModifiers.accurateParallel(
                        machine, recipe!!,
                        8.0.pow(((machine as WorkableElectricMultiblockMachine).getTier() - 10).toDouble()).toInt(),
                        false
                    ).getFirst()
                },
                GTRecipeModifiers.ELECTRIC_OVERCLOCK.apply(OverclockingLogic.PERFECT_OVERCLOCK))
            .appearanceBlock(FUSION_CASING_MK5)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.DRACONIC_COLLAPSE_CORE_STRUCTURE!!
                    .where("E", Predicates.controller(Predicates.blocks(definition!!.get())))
                    .where("D", Predicates.blocks(GTLFusionCasingBlock.getCasingState(10))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(2).setMinGlobalLimited(1))
                        .or(Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1)))
                    .where("L", Predicates.blocks(GTLFusionCasingBlock.getCasingState(10))
                        .or(Predicates.blocks(GTMachines.ITEM_IMPORT_BUS[0].get()))
                        .or(Predicates.blocks(CustomMachines.HUGE_ITEM_IMPORT_BUS[0].get()))
                        .or(Predicates.blocks(ME_SUPER_PATTERN_BUFFER.get()))
                        .or(Predicates.blocks(ME_SUPER_PATTERN_BUFFER_PROXY.get())))
                    .where("I", Predicates.blocks(MOLECULAR_CASING.get()))
                    .where("K", Predicates.blocks(getBlock("kubejs:annihilate_core")))
                    .where("J", Predicates.blocks(getBlock("kubejs:aggregatione_core")))
                    .where("F", Predicates.blocks(DIMENSIONALLY_TRANSCENDENT_CASING.get()))
                    .where("B", Predicates.blocks(ChemicalHelper.getBlock(frameGt, Neutronium)))
                    .where("A", Predicates.blocks(DIMENSION_INJECTION_CASING.get()))
                    .where("C", Predicates.blocks(GTLFusionCasingBlock.getCasingState(10)))
                    .where("H", Predicates.blocks(getBlock("kubejs:hollow_casing")))
                    .where("G", Predicates.blocks(GTLFusionCasingBlock.getCompressedCoilState(10)))
                    .where("O", Predicates.blocks(GTLFusionCasingBlock.getCasingState(10))
                        .or(GTLPredicates.diffAbilities(
                            listOf<PartAbility?>(PartAbility.EXPORT_ITEMS),
                            listOf<PartAbility?>(PartAbility.IMPORT_ITEMS, PartAbility.IMPORT_FLUIDS)
                        )))
                    .where(" ", Predicates.any())
                    .build()
            }
            .additionalDisplay { controller: IMultiController?, components: MutableList<Component?>? ->
                if (controller!!.isFormed) {
                    components!!.add(
                        Component
                            .translatable(
                                "gtceu.multiblock.parallel",
                                Component.translatable(
                                    FormattingUtil.formatNumbers(
                                        8.0.pow(((controller as WorkableElectricMultiblockMachine).getTier() - 10).toDouble())
                                    )
                                )
                                    .withStyle(ChatFormatting.DARK_PURPLE)
                            )
                            .withStyle(ChatFormatting.GRAY)
                    )
                }
            }
            .workableCasingRenderer(
                GTLFusionCasingBlock.getCasingType(10).texture,
                GTCEu.id("block/multiblock/fusion_reactor")
            )
            .register()

        TITAN_CRIP_EARTHBORE = REGISTRATE.multiblock(
            "titan_crip_earthbore",
            Function {
                object : MutableElectricMultiblockMachine(it) {
                    override fun getMaxParallel(): Int {
                        return 2.0.pow(tier - 6).toInt()
                    }
                }
            })
            .noneRotation()
            .tooltipTextKey(Component.translatable("gtceu.multiblock.max_parallel.titan_crip_earthbore"))
            .tooltipTextPerfectOverclock()
            .tooltipTextRecipeTypes(TECTONIC_FAULT_GENERATOR)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(TECTONIC_FAULT_GENERATOR)
            .recipeModifiers(
                *arrayOf<RecipeModifier?>(
                    RecipeModifier { machine: MetaMachine?, recipe: GTRecipe?, params: OCParams?, result: OCResult? ->
                        GTRecipeModifiers.accurateParallel(machine, recipe!!,
                            2.0.pow(((machine as WorkableElectricMultiblockMachine).getTier() - 6).toDouble()).toInt(),
                            false
                        ).getFirst()
                    },
                    GTRecipeModifiers.ELECTRIC_OVERCLOCK.apply(OverclockingLogic.PERFECT_OVERCLOCK)
                )
            )
            .appearanceBlock(ECHO_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.TITAN_CRIP_EARTHBORE_STRUCTURE!!
                    .where("~", Predicates.controller(Predicates.blocks(definition!!.get())))
                    .where("I", Predicates.blocks(getBlock("kubejs:neutronium_gearbox")))
                    .where("H", Predicates.blocks(getBlock("kubejs:neutronium_pipe_casing")))
                    .where("G", Predicates.blocks(getBlock("kubejs:machine_casing_grinding_head")))
                    .where("B", Predicates.blocks(ChemicalHelper.getBlock(frameGt, Neutronium)))
                    .where("C", Predicates.blocks(ECHO_CASING.get()))
                    .where("A", Predicates.blocks(MOLECULAR_CASING.get()))
                    .where("F", Predicates.blocks(getBlock("minecraft:bedrock")))
                    .where("D", Predicates.blocks(getBlock("kubejs:molecular_coil")))
                    .where("E", Predicates.blocks(ECHO_CASING.get())
                        .or(Predicates.autoAbilities(*definition.recipeTypes))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                        .or(Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1)))
                    .build()
            }
            .additionalDisplay { controller: IMultiController?, components: MutableList<Component?>? ->
                if (controller!!.isFormed) {
                    components!!.add(
                        Component.translatable("gtceu.multiblock.parallel", Component.literal(
                            FormattingUtil.formatNumbers(2.0.pow(((controller as WorkableElectricMultiblockMachine).getTier() - 6).toDouble())))
                            .withStyle(ChatFormatting.DARK_PURPLE)
                        ).withStyle(ChatFormatting.GRAY)
                    )
                }
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/echo_casing"),
                GTCEu.id("block/multiblock/cleanroom"))
            .register()

        BIOLOGICAL_SIMULATION_LABORATORY = REGISTRATE.multiblock("biological_simulation_laboratory",
            Function { BiologicalSimulationLaboratory(it) })
            .allRotation()
            .tooltipTextKey(Component.translatable("gtceu.multiblock.biological_simulation_laboratory.tooltip.0"))
            .tooltipTextKey(Component.translatable("gtceu.multiblock.biological_simulation_laboratory.tooltip.1"))
            .tooltipTextKey(Component.translatable("gtceu.multiblock.biological_simulation_laboratory.tooltip.2"))
            .tooltipTextKey(Component.translatable("gtceu.multiblock.biological_simulation_laboratory.tooltip.3"))
            .tooltipTextKey(Component.translatable("gtceu.multiblock.biological_simulation_laboratory.tooltip.4"))
            .tooltipTextKey(Component.translatable("gtceu.multiblock.biological_simulation_laboratory.tooltip.5"))
            .tooltipTextKey(Component.translatable("gtceu.multiblock.biological_simulation_laboratory.tooltip.6"))
            .tooltipTextKey(Component.translatable("gtceu.multiblock.biological_simulation_laboratory.tooltip.7"))
            .tooltipTextKey(Component.translatable("gtceu.multiblock.biological_simulation_laboratory.tooltip.8"))
            .tooltipTextKey(Component.translatable("gtceu.multiblock.biological_simulation_laboratory.tooltip.9"))
            .tooltipTextRecipeTypes(BIOLOGICAL_SIMULATION)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(BIOLOGICAL_SIMULATION)
            .appearanceBlock(NAQUADAH_ALLOY_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.BIOLOGICAL_SIMULATION_LABORATORY_STRUCTURE!!
                    .where("~", Predicates.controller(Predicates.blocks(definition!!.get())))
                    .where("A", Predicates.blocks(NAQUADAH_ALLOY_CASING.get())
                        .or(Predicates.autoAbilities(*definition.recipeTypes))
                        .or(Predicates.blocks(*PartAbility.INPUT_LASER.getBlockRange(12, 14).toTypedArray<Block?>()).setMaxGlobalLimited(1))
                        .or(Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1)))
                    .where("B", Predicates.blocks(ChemicalHelper.getBlock(frameGt, NaquadahAlloy)))
                    .where("C", Predicates.blocks(HERMETIC_CASING_LuV.get()))
                    .where("E", Predicates.blocks(FUSION_GLASS.get()))
                    .where("G", Predicates.blocks(COMPUTER_HEAT_VENT.get()))
                    .where("D", Predicates.blocks(ADVANCED_COMPUTER_CASING.get()))
                    .where("H", Predicates.blocks(FILTER_CASING_STERILE.get()))
                    .where("F", Predicates.blocks(HERMETIC_CASING_ZPM.get()))
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/hyper_mechanical_casing"),
                GTCEu.id("block/multiblock/fusion_reactor")
            )
            .register()

        DIMENSIONALLY_TRANSCENDENT_CHEMICAL_PLANT = REGISTRATE.multiblock("dimensionally_transcendent_chemical_plant",
            Function { GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(it) })
            .nonYAxisRotation()
            .tooltipTextKey(Component.translatable("gtceu.multiblock.dimensionally_transcendent_chemical_plant"))
            .tooltipTextCoilParallel()
            .tooltipTextLaser()
            .tooltipTextMultiRecipes()
            .tooltipTextRecipeTypes(LARGE_CHEMICAL_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(LARGE_CHEMICAL_RECIPES)
            .appearanceBlock(CASING_PTFE_INERT)
            .pattern { definition: MultiblockMachineDefinition? ->
                GTLMachines.DTPF
                    .where("a", Predicates.controller(Predicates.blocks(definition!!.get())))
                    .where("e", Predicates.blocks(CASING_PTFE_INERT.get())
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1))
                        .or(Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1)))
                    .where("b", Predicates.blocks(HIGH_POWER_CASING.get()))
                    .where("C", Predicates.heatingCoils())
                    .where("d", Predicates.blocks(CASING_PTFE_INERT.get()))
                    .where("s", Predicates.blocks(getBlock("gtceu:ptfe_pipe_casing")))
                    .where(" ", Predicates.any())
                    .build()
            }
            .workableCasingRenderer(
                GTCEu.id("block/casings/solid/machine_casing_inert_ptfe"),
                GTCEu.id("block/machines/chemical_reactor")
            )
            .register()

        QUANTUM_SYPHON_MATRIX = REGISTRATE.multiblock("quantum_syphon_matrix",
            Function { GTLAddWorkableElectricParallelHatchMultipleRecipesMachine(it)})
            .noneRotation()
            .tooltipTextParallelHatch()
            .tooltipTextLaser()
            .tooltipTextMultiRecipes()
            .tooltipTextRecipeTypes(VOIDFLUX_REACTION)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(VOIDFLUX_REACTION)
            .recipeModifier(GTRecipeModifiers.PARALLEL_HATCH)
            .appearanceBlock(HIGH_POWER_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.QUANTUM_SYPHON_MATRIX_STRUCTURE!!
                    .where("~", Predicates.controller(Predicates.blocks(definition!!.get())))
                    .where("C", Predicates.blocks(ChemicalHelper.getBlock(frameGt, Neutronium)))
                    .where("G", Predicates.blocks(getBlock("kubejs:accelerated_pipeline")))
                    .where("D", Predicates.blocks(MOLECULAR_CASING.get()))
                    .where("H", Predicates.blocks(getBlock("kubejs:neutronium_gearbox")))
                    .where("F", Predicates.blocks(HIGH_POWER_CASING.get())
                        .or(Predicates.autoAbilities(*definition.recipeTypes))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1))
                        .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1))
                        .or(Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1)))
                    .where("J", Predicates.blocks(getBlock("kubejs:neutronium_pipe_casing")))
                    .where("A", Predicates.blocks(NAQUADAH_ALLOY_CASING.get()))
                    .where("B", Predicates.blocks(getBlock("gtceu:assembly_line_grating")))
                    .where("I", Predicates.blocks(HERMETIC_CASING_UHV.get()))
                    .where("E", Predicates.blocks(getBlock("kubejs:hollow_casing")))
                    .where(" ", Predicates.any())
                    .build()
            }
            .workableCasingRenderer(
                GTCEu.id("block/casings/hpca/high_power_casing"),
                GTCEu.id("block/machines/gas_collector")
            )
            .register()

        FUXI_BAGUA_HEAVEN_FORGING_FURNACE = REGISTRATE.multiblock("fuxi_bagua_heaven_forging_furnace",
            Function { GTLAddCoilWorkableElectricParallelHatchMultipleRecipesMachine(it) })
            .nonYAxisRotation()
            .tooltipTextParallelHatch()
            .tooltipOnlyTextLaser()
            .tooltipTextMultiRecipes()
            .tooltipTextRecipeTypes(STELLAR_LGNITION, CHAOTIC_ALCHEMY, MOLECULAR_DECONSTRUCTION, ULTIMATE_MATERIAL_FORGE_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(STELLAR_LGNITION)
            .recipeType(CHAOTIC_ALCHEMY)
            .recipeType(MOLECULAR_DECONSTRUCTION)
            .recipeType(ULTIMATE_MATERIAL_FORGE_RECIPES)
            .recipeModifier(GTRecipeModifiers.PARALLEL_HATCH)
            .appearanceBlock(DIMENSION_INJECTION_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.FUXI_BAGUA_HEAVEN_FORGING_FURNACE_STRUCTURE!!
                    .where("D", Predicates.controller(Predicates.blocks(definition!!.get())))
                    .where("K", Predicates.blocks(getBlock("kubejs:neutronium_pipe_casing")))
                    .where("C", Predicates.blocks(DIMENSION_INJECTION_CASING.get())
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(2))
                        .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1))
                        .or(Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1)))
                    .where("X", Predicates.heatingCoils())
                    .where("J", Predicates.blocks(getBlock("kubejs:dimensional_bridge_casing")))
                    .where("F", Predicates.blocks(GRAVITON_FIELD_CONSTRAINT_CASING.get()))
                    .where("I", Predicates.blocks(getBlock("kubejs:molecular_coil")))
                    .where("A", Predicates.blocks(getBlock("gtceu:atomic_casing")))
                    .where("G", Predicates.blocks(DEGENERATE_RHENIUM_CONSTRAINED_CASING.get()))
                    .where("N", Predicates.blocks(ULTIMATE_STELLAR_CONTAINMENT_CASING.get()))
                    .where("B", Predicates.blocks(DIMENSION_INJECTION_CASING.get()))
                    .where("E", Predicates.blocks(getBlock("kubejs:dimension_creation_casing")))
                    .where("H", Predicates.blocks(getBlock("kubejs:spacetime_compression_field_generator")))
                    .where("L", Predicates.blocks(COMPRESSED_FUSION_COIL_MK2_PROTOTYPE.get()))
                    .where("M", Predicates.blocks(getBlock("kubejs:dimensional_stability_casing")))
                    .where("O", Predicates.blocks(getBlock("kubejs:restraint_device")))
                    .build()
            }
            .additionalDisplay { controller: IMultiController?, components: MutableList<Component?>? ->
                if (controller is GTLAddCoilWorkableElectricParallelHatchMultipleRecipesMachine) {
                    if (controller.isFormed()) {
                        components!!.add(
                            Component.translatable(
                                "gtceu.multiblock.blast_furnace.max_temperature",
                                Component.translatable(
                                    FormattingUtil.formatNumbers(controller.coilType.coilTemperature) + "K")
                                    .setStyle(Style.EMPTY.withColor(ChatFormatting.RED))
                            )
                        )
                    }
                }
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/dimension_injection_casing"),
                GTCEu.id("block/multiblock/fusion_reactor")
            )
            .register()

        ANTIENTROPY_CONDENSATION_CENTER = REGISTRATE.multiblock("antientropy_condensation_center",
            Function { AntientropyCondensationCenter(it) })
            .allRotation()
            .tooltipTextKey(Component.translatable("gtceu.multiblock.antientropy_condensation_center.0"))
            .tooltipTextKey(Component.translatable("gtceu.multiblock.antientropy_condensation_center.1"))
            .tooltipTextParallelHatch()
            .tooltipOnlyTextLaser()
            .tooltipTextMultiRecipes()
            .tooltipTextRecipeTypes(ANTIENTROPY_CONDENSATION)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(ANTIENTROPY_CONDENSATION)
            .recipeModifier(GTRecipeModifiers.PARALLEL_HATCH)
            .appearanceBlock(ANTIFREEZE_HEATPROOF_MACHINE_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.ANTIENTROPY_CONDENSATION_CENTER_STRUCTURE!!
                    .where("B", Predicates.controller(Predicates.blocks(definition!!.get())))
                    .where("C", Predicates.blocks(MOLECULAR_CASING.get()))
                    .where("K", Predicates.blocks(ChemicalHelper.getBlock(frameGt, Mithril)))
                    .where("D", Predicates.blocks(HERMETIC_CASING_UXV.get()))
                    .where("M", Predicates.blocks(getBlock("kubejs:containment_field_generator")))
                    .where("J", Predicates.blocks(getBlock("kubejs:force_field_glass")))
                    .where("I", Predicates.blocks(getBlock("kubejs:dimensional_bridge_casing")))
                    .where("A", Predicates.blocks(ANTIFREEZE_HEATPROOF_MACHINE_CASING.get()))
                    .where("X", Predicates.blocks(ANTIFREEZE_HEATPROOF_MACHINE_CASING.get())
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(2))
                        .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1))
                        .or(Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1)))
                    .where("F", Predicates.blocks(COMPRESSED_FUSION_COIL_MK2.get()))
                    .where("G", Predicates.blocks(getBlock("gtlcore:law_filter_casing")))
                    .where("H", Predicates.blocks(getBlock("kubejs:hollow_casing")))
                    .where("E", Predicates.blocks(DIMENSIONALLY_TRANSCENDENT_CASING.get()))
                    .where("L", Predicates.blocks(DIMENSION_INJECTION_CASING.get()))
                    .build()
            }
            .additionalDisplay{controller: IMultiController?, components: MutableList<Component?>? ->
                if (controller is AntientropyCondensationCenter) {
                    if (controller.isFormed()) {
                        components!!.add(
                            Component.translatable("gtceu.multiblock.antientropy_condensation_center.dust_cryotheum",
                                1 shl (GTValues.MAX - controller.getTier())))
                    }
                }
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/antifreeze_heatproof_machine_casing"),
                GTCEu.id("block/multiblock/vacuum_freezer")
            )
            .register()

        TAIXU_TURBID_ARRAY = REGISTRATE.multiblock("taixu_turbid_array",
            Function { TaixuTurbidArray(it) })
            .tooltipTextKey(Component.translatable("gtceu.machine.taixuturbidarray.tooltip.0"))
            .tooltipTextKey(Component.translatable("gtceu.machine.taixuturbidarray.tooltip.1"))
            .tooltipTextKey(Component.translatable("gtceu.machine.taixuturbidarray.tooltip.12"))
            .tooltipTextKey(Component.translatable("gtceu.machine.taixuturbidarray.tooltip.2"))
            .tooltipTextKey(Component.translatable("gtceu.machine.taixuturbidarray.tooltip.3"))
            .tooltipTextKey(Component.translatable("gtceu.machine.taixuturbidarray.tooltip.13"))
            .tooltipTextKey(Component.translatable("gtceu.machine.taixuturbidarray.tooltip.4"))
            .tooltipTextKey(Component.translatable("gtceu.machine.taixuturbidarray.tooltip.5"))
            .tooltipTextKey(Component.translatable("gtceu.machine.taixuturbidarray.tooltip.6"))
            .tooltipTextKey(Component.translatable("gtceu.machine.taixuturbidarray.tooltip.8"))
            .tooltipTextKey(Component.translatable("gtceu.machine.taixuturbidarray.tooltip.9"))
            .tooltipTextKey(Component.translatable("gtceu.machine.taixuturbidarray.tooltip.14"))
            .tooltipTextKey(Component.translatable("gtceu.machine.taixuturbidarray.tooltip.7"))
            .tooltipTextKey(Component.translatable("gtceu.machine.taixuturbidarray.tooltip.11"))
            .tooltipTextKey(Component.translatable("gtceu.machine.taixuturbidarray.tooltip.10"))
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .rotationState(RotationState.Y_AXIS)
            .recipeType(CHAOS_WEAVE)
            .recipeModifier { machine: MetaMachine?, recipe: GTRecipe?, params: OCParams?, result: OCResult? ->
                TaixuTurbidArray.recipeModifier(machine!!, recipe!!)
            }
            .appearanceBlock(MACHINE_CASING_UHV)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.TAIXU_TURBID_ARRAY_STRUCTURE!!
                    .where("T", Predicates.controller(Predicates.blocks(definition!!.get())))
                    .where("K", Predicates.blocks(MACHINE_CASING_UHV.get())
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setPreviewCount(1)))
                    .where("H", Predicates.blocks(MACHINE_CASING_UHV.get()))
                    .where("E", Predicates.blocks(getBlock("gtceu:woods_glass_block")))
                    .where("J", Predicates.blocks(DIMENSION_INJECTION_CASING.get()))
                    .where("B", Predicates.blocks(DIMENSIONALLY_TRANSCENDENT_CASING.get()))
                    .where("R", Predicates.blocks(getBlock("kubejs:force_field_glass")))
                    .where("S", GTLPredicates.countBlock("SpeedPipe", getBlock("kubejs:speeding_pipe")))
                    .where("G", Predicates.blocks(getBlock("kubejs:hollow_casing")))
                    .where("F", Predicates.blocks(ChemicalHelper.getBlock(frameGt, NaquadahAlloy)))
                    .where("N", Predicates.blocks(FUSION_CASING_MK5.get()))
                    .where("I", Predicates.blocks(SPS_CASING.get()))
                    .where("P", Predicates.blocks(FUSION_GLASS.get()))
                    .where("M", GTLPredicates.tierCasings(BlockMap.scMap, "SCTier"))
                    .where("A", Predicates.blocks(IRIDIUM_CASING.get()))
                    .where("L", Predicates.blocks(getBlock("kubejs:containment_field_generator")))
                    .where("Q", Predicates.blocks(getBlock("kubejs:dimensional_bridge_casing")))
                    .where("C", Predicates.blocks(getBlock("gtceu:atomic_casing")))
                    .where("D", Predicates.blocks(ChemicalHelper.getBlock(frameGt, Mithril)))
                    .where("O", Predicates.heatingCoils())
                    .build()
            }
            .shapeInfos { definition ->
                val results = ObjectArrayList<MultiblockShapeInfo>()
                val shapeInfo = MultiblockShapeInfo.builder()
                    .where('~', definition, Direction.DOWN)
                    .where('J', getBlock("gtlcore:dimension_injection_casing"))
                    .where('N', getBlock("kubejs:eternity_coil_block"))
                    .where('G', getBlock("kubejs:hollow_casing"))
                    .where('O', getBlock("gtceu:fusion_glass"))
                    .where('B', getBlock("gtlcore:dimensionally_transcendent_casing"))
                    .where('R', STOCKING_IMPORT_BUS_ME, Direction.DOWN)
                    .where('I', getBlock("gtlcore:sps_casing"))
                    .where('K', getBlock("kubejs:containment_field_generator"))
                    .where('L', getBlock("gtlcore:ultimate_stellar_containment_casing"))
                    .where('M', getBlock("gtlcore:fusion_casing_mk5"))
                    .where('S', ME_EXTENDED_EXPORT_BUFFER, Direction.DOWN)
                    .where('C', getBlock("gtceu:atomic_casing"))
                    .where('H', getBlock("gtceu:uhv_machine_casing"))
                    .where('P', WIRELESS_LASER_INPUT_HATCH_67108864A[13], Direction.DOWN)
                    .where('D', getBlock("gtceu:mithril_frame"))
                    .where('U', getBlock("kubejs:speeding_pipe"))
                    .where('V', getBlock("kubejs:dimensional_bridge_casing"))
                    .where('A', getBlock("gtlcore:iridium_casing"))
                    .where('F', getBlock("gtceu:naquadah_alloy_frame"))
                    .where('T', getBlock("kubejs:force_field_glass"))
                    .where('E', getBlock("gtceu:woods_glass_block"))

                val arrays = arrayOf(
                    arrayOf("             ", "             ", "             ", "             ", "             ", "             ", "             ", "             ", "             ", "             ", "      A      ", "             ", "             ", "             ", "             ", "             ", "             ", "             ", "             ", "             ", "             "),
                    arrayOf("             ", "     AAA     ", "      A      ", "      A      ", "      A      ", "     AAA     ", "      A      ", "      A      ", "      A      ", "      A      ", "     BBB     ", "      A      ", "      A      ", "      A      ", "      A      ", "     AAA     ", "      A      ", "      A      ", "      A      ", "     AAA     ", "             "),
                    arrayOf("     A A     ", "    CCCCC    ", "     BBB     ", "     BDB     ", "     B B     ", "   BBEEEBB   ", "    A   A    ", "             ", "             ", "     AAA     ", "     FGF     ", "     AAA     ", "             ", "             ", "    A   A    ", "   BBEEEBB   ", "     B B     ", "     BDB     ", "     BBB     ", "    CCCCC    ", "     A A     "),
                    arrayOf("    HAHAH    ", "   CCCCCCC   ", "    HIJIH    ", "      D      ", "             ", "  B       B  ", "   AEEEEEA   ", "             ", "             ", "             ", "             ", "             ", "             ", "             ", "   AEEEEEA   ", "  B       B  ", "             ", "      D      ", "    HIJIH    ", "   CCCCCCC   ", "    HAHAH    "),
                    arrayOf("   HAKHKAH   ", "  CCCCCCCCC  ", "   HAIJIAH   ", "    AAAAA    ", "     LLL     ", "  B  LLL  B  ", "  AE LLL EA  ", "    AEEEA    ", "    M   M    ", "    M   M    ", "    M   M    ", "    M   M    ", "    M   M    ", "    AEEEA    ", "  AE LLL EA  ", "  B  LLL  B  ", "     LLL     ", "    AAAAA    ", "   HAIJIAH   ", "  CCCCCCCCC  ", "   HAKHKAH   "),
                    arrayOf("  AAKHHHKAA  ", " ACCCCCCCCCA ", "  BIIIJIIIB  ", "  B ANNNA B  ", "  B LLLLL B  ", " AE L   L EA ", "   ELLLLLE   ", "    E O E    ", "     ADA     ", "  A       A  ", " BF   O   FB ", "  A       A  ", "     ADA     ", "    E O E    ", "   ELLLLLE   ", " AE L   L EA ", "  B LLLLL B  ", "  B ANNNA B  ", "  BIIIJIIIB  ", " ACCCCCCCCCA ", "  AAKHHHKAA  "),
                    arrayOf("   PPH~HRS   ", " ACCCCCCCCCA ", " ABJJJJJJJBA ", " ADDANKNADDA ", " A  LLTLL  A ", " AE L T L EA ", " A ELLTLLE A ", " A  EOKOE  A ", " A   DUD   A ", " AA   V   AA ", "ABG  OUO  GBA", " AA   V   AA ", " A   DUD   A ", " A  EOKOE  A ", " A ELLTLLE A ", " AE L T L EA ", " A  LLTLL  A ", " ADDANKNADDA ", " ABJJJJJJJBA ", " ACCCCCCCCCA ", "   HHHVHHH   "),
                    arrayOf("  AAKHHHKAA  ", " ACCCCCCCCCA ", "  BIIIJIIIB  ", "  B ANNNA B  ", "  B LLLLL B  ", " AE L   L EA ", "   ELLLLLE   ", "    E O E    ", "     ADA     ", "  A       A  ", " BF   O   FB ", "  A       A  ", "     ADA     ", "    E O E    ", "   ELLLLLE   ", " AE L   L EA ", "  B LLLLL B  ", "  B ANNNA B  ", "  BIIIJIIIB  ", " ACCCCCCCCCA ", "  AAKHHHKAA  "),
                    arrayOf("   HAKHKAH   ", "  CCCCCCCCC  ", "   HAIJIAH   ", "    AAAAA    ", "     LLL     ", "  B  LLL  B  ", "  AE LLL EA  ", "    AEEEA    ", "    M   M    ", "    M   M    ", "    M   M    ", "    M   M    ", "    M   M    ", "    AEEEA    ", "  AE LLL EA  ", "  B  LLL  B  ", "     LLL     ", "    AAAAA    ", "   HAIJIAH   ", "  CCCCCCCCC  ", "   HAKHKAH   "),
                    arrayOf("    HAHAH    ", "   CCCCCCC   ", "    HIJIH    ", "      D      ", "             ", "  B       B  ", "   AEEEEEA   ", "             ", "             ", "             ", "             ", "             ", "             ", "             ", "   AEEEEEA   ", "  B       B  ", "             ", "      D      ", "    HIJIH    ", "   CCCCCCC   ", "    HAHAH    "),
                    arrayOf("     A A     ", "    CCCCC    ", "     BBB     ", "     BDB     ", "     B B     ", "   BBEEEBB   ", "    A   A    ", "             ", "             ", "     AAA     ", "     FGF     ", "     AAA     ", "             ", "             ", "    A   A    ", "   BBEEEBB   ", "     B B     ", "     BDB     ", "     BBB     ", "    CCCCC    ", "     A A     "),
                    arrayOf("             ", "     AAA     ", "      A      ", "      A      ", "      A      ", "     AAA     ", "      A      ", "      A      ", "      A      ", "      A      ", "     BBB     ", "      A      ", "      A      ", "      A      ", "      A      ", "     AAA     ", "      A      ", "      A      ", "      A      ", "     AAA     ", "             "),
                    arrayOf("             ", "             ", "             ", "             ", "             ", "             ", "             ", "             ", "             ", "             ", "      A      ", "             ", "             ", "             ", "             ", "             ", "             ", "             ", "             ", "             ", "             ")
                )

                StructureSlicer.sliceAndInsert(arrays, 10, 12, 11, 1, 16).forEach { it ->
                    var copy = shapeInfo.shallowCopy()
                    it.forEach { strings ->
                        copy = copy.aisle(*strings)
                    }
                    val result = copy.build()
                    results.add(result)
                }
                results
            }
            .workableCasingRenderer(
                GTCEu.id("block/casings/voltage/uhv/side"),
                GTCEu.id("block/multiblock/fusion_reactor")
            )
            .register()

        INFERNO_CLEFT_SMELTING_VAULT = REGISTRATE.multiblock("inferno_cleft_smelting_vault",
            Function { GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(it) })
            .nonYAxisRotation()
            .tooltipTextCoilParallel()
            .tooltipTextLaser()
            .tooltipTextMultiRecipes()
            .tooltipTextRecipeTypes(PYROLYSE_RECIPES, CRACKING_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeTypes(PYROLYSE_RECIPES, CRACKING_RECIPES)
            .appearanceBlock(IRIDIUM_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.INFERNO_CLEFT_SMELTING_VAULT!!
                    .where("L", Predicates.controller(Predicates.blocks(definition!!.get())))
                    .where("I", Predicates.blocks(IRIDIUM_CASING.get())
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1))
                        .or(Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1)))
                    .where("M", Predicates.blocks(getBlock("gtceu:uv_muffler_hatch")))
                    .where("G", Predicates.heatingCoils())
                    .where("H", Predicates.heatingCoils())
                    .where("B", Predicates.blocks(IRIDIUM_CASING.get()))
                    .where("A", Predicates.blocks(MOLECULAR_CASING.get()))
                    .where("J", Predicates.blocks(HERMETIC_CASING_LuV.get()))
                    .where("C", Predicates.blocks(HYPER_MECHANICAL_CASING.get()))
                    .where("E", Predicates.blocks(getBlock("kubejs:neutronium_pipe_casing")))
                    .where("K", Predicates.blocks(HYPER_CORE.get()))
                    .where("D", Predicates.blocks(getBlock("gtceu:high_temperature_smelting_casing")))
                    .where("F", Predicates.blocks(FUSION_GLASS.get()))
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/iridium_casing"),
                GTCEu.id("block/multiblock/pyrolyse_oven")
            )
            .register()

        SKELETON_SHIFT_RIFT_ENGINE = REGISTRATE.multiblock("skeleton_shift_rift_engine",
            Function {  SkeletonShiftRiftEngine(it) })
            .nonYAxisRotation()
            .tooltipTextKey(Component.translatable("gtceu.multiblock.skeleton_shift_rift_engine.0"))
            .tooltipTextKey(Component.translatable("gtceu.multiblock.skeleton_shift_rift_engine.1"))
            .tooltipTextLaser()
            .tooltipTextPerfectOverclock()
            .tooltipTextRecipeTypes(DECAY_HASTENER_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeTypes(DECAY_HASTENER_RECIPES)
            .recipeModifier(SkeletonShiftRiftEngine::recipeModifier)
            .appearanceBlock(HYPER_MECHANICAL_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.SKELETON_SHIFT_RIFT_ENGINE!!
                    .where("Q", Predicates.controller(Predicates.blocks(definition!!.get())))
                    .where("P", GTLPredicates.tierCasings(BlockMap.scMap, "SCTier"))
                    .where("E", Predicates.blocks(ChemicalHelper.getBlock(frameGt, BlackSteel)))
                    .where("B", Predicates.blocks(HIGH_POWER_CASING.get()))
                    .where("D", Predicates.blocks(SPS_CASING.get()))
                    .where("J", Predicates.blocks(getBlock("gtceu:steel_pipe_casing")))
                    .where("A", Predicates.blocks(IRIDIUM_CASING.get()))
                    .where("M", Predicates.blocks(getBlock("gtceu:tungstensteel_gearbox")))
                    .where("H", Predicates.blocks(HYPER_MECHANICAL_CASING.get()))
                    .where("h", Predicates.blocks(HYPER_MECHANICAL_CASING.get())
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1))
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1))
                        .or(Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1)))
                    .where("O", Predicates.blocks(DEGENERATE_RHENIUM_CONSTRAINED_CASING.get()))
                    .where("F", Predicates.blocks(getBlock("kubejs:neutronium_pipe_casing")))
                    .where("I", Predicates.blocks(getBlock("kubejs:dimensional_bridge_casing")))
                    .where("N", Predicates.heatingCoils())
                    .where("G", Predicates.blocks(DIMENSIONALLY_TRANSCENDENT_CASING.get()))
                    .where("C", Predicates.blocks(ENHANCE_HYPER_MECHANICAL_CASING.get()))
                    .where("K", Predicates.blocks(HYPER_CORE.get()))
                    .where("L", Predicates.blocks(FUSION_GLASS.get()))
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/hyper_mechanical_casing"),
                GTCEu.id("block/multiblock/fusion_reactor")
            )
            .register()

        APOCALYPTIC_TORSION_QUANTUM_MATRIX = REGISTRATE.multiblock("apocalyptic_torsion_quantum_matrix",
            Function { ApocalypticTorsionQuantumMatrix(it) })
            .nonYAxisRotation()
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.apocalyptic_torsion_quantum_matrix.tooltip.0"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.apocalyptic_torsion_quantum_matrix.tooltip.1"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.apocalyptic_torsion_quantum_matrix.tooltip.2"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.apocalyptic_torsion_quantum_matrix.tooltip.3"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.apocalyptic_torsion_quantum_matrix.tooltip.4"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.apocalyptic_torsion_quantum_matrix.tooltip.5"))
            .tooltipTextKey(Component.translatable("gtceu.machine.eut_multiplier.tooltip", 0.2))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.multiple_recipe_types_machine.tooltip.0"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.0"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.1"))
            .tooltipTextKey(Component.translatable("tooltip.gtlcore.structure.source", "TST"))
            .tooltipTextRecipeTypes(QFT_RECIPES, DISTORT_RECIPES, NEUTRON_COMPRESSOR_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeTypes(QFT_RECIPES, DISTORT_RECIPES, NEUTRON_COMPRESSOR_RECIPES)
            .appearanceBlock(DIMENSIONALLY_TRANSCENDENT_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructureB.APOCALYPTIC_TORSION_QUANTUM_MATRIX!!
                    .where("~", Predicates.controller(Predicates.blocks(definition!!.get())))
                    .where("A", Predicates.blocks(QFT_COIL.get()))
                    .where("B", Predicates.blocks(MANIPULATOR.get()))
                    .where("C", Predicates.blocks(getBlock("kubejs:dimensional_bridge_casing")))
                    .where("D", Predicates.blocks(DIMENSIONALLY_TRANSCENDENT_CASING.get()))
                    .where("E", Predicates.blocks(QUANTUM_GLASS.get()))
                    .where("F", Predicates.blocks(MANIPULATOR.get())
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                        .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1))
                        .or(Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1)))
                    .where("G", Predicates.blocks(SPACETIMECONTINUUMRIPPER.get()))
                    .build()
            }
            .renderer {
                PartWorkableCasingMachineRenderer(
                    GTLCore.id("block/casings/dimensionally_transcendent_casing"),
                    GTCEu.id("block/multiblock/data_bank"),
                    MANIPULATOR,
                    GTLCore.id("block/manipulator")
                )
            }
            .partAppearance { controller, part, side -> MANIPULATOR.get().defaultBlockState() }
            .register()

        FORGE_OF_THE_ANTICHRIST = REGISTRATE.multiblock("forge_of_the_antichrist",
            Function { ForgeOfTheAntichrist(it) })
            .nonYAxisRotation()
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.tooltip.0"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.tooltip.1"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.tooltip.2"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.tooltip.3"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.tooltip.4"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.tooltip.5", createObfuscatedDeleteComponent(
                Long.MAX_VALUE.toString())))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.tooltip.6"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.tooltip.7"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.tooltip.8"))
            .tooltipTextKey(Component.translatable("gtceu.machine.eut_multiplier.tooltip", 0.2))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.tooltip.3"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.multiple_recipe_types_machine.tooltip.0"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.0"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.1"))
            .tooltipTextKey(Component.translatable("tooltip.gtlcore.structure.source", "GTNH"))
            .tooltipTextRecipeTypes(DIMENSIONALLY_TRANSCENDENT_PLASMA_FORGE_RECIPES, STELLAR_FORGE_RECIPES, ULTIMATE_MATERIAL_FORGE_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .appearanceBlock(GOD_FORGE_INNER_CASING)
            .recipeTypes(DIMENSIONALLY_TRANSCENDENT_PLASMA_FORGE_RECIPES, STELLAR_FORGE_RECIPES, ULTIMATE_MATERIAL_FORGE_RECIPES)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructureC.FORGE_OF_THE_ANTICHRIST!!
                    .where("~", Predicates.controller(Predicates.blocks(definition!!.get())))
                    .where("A", Predicates.blocks(GOD_FORGE_SUPPORT_CASING.get())
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS)))
                    .where("B", Predicates.blocks(GOD_FORGE_TRIM_CASING.get()))
                    .where("C", Predicates.blocks(GOD_FORGE_INNER_CASING.get()))
                    .where("D", Predicates.blocks(GOD_FORGE_SUPPORT_CASING.get()))
                    .where("E", Predicates.blocks(SUPRACHRONAL_MAGNETIC_CONFINEMENT_CASING.get()))
                    .where("F", Predicates.blocks(GOD_FORGE_ENERGY_CASING.get()))
                    .where("G", Predicates.blocks(REMOTE_GRAVITON_FLOW_REGULATOR.get()))
                    .where("H", Predicates.blocks(SPATIALLY_TRANSCENDENT_GRAVITATIONAL_LENS.get()))
                    .where("I", Predicates.blocks(CENTRAL_GRAVITON_FLOW_REGULATOR.get()))
                    .where("J", Predicates.blocks(GOD_FORGE_TRIM_CASING.get())
                        .or(Predicates.blocks(HELIOFUSION_EXOTICIZER.get()))
                        .or(Predicates.blocks(HELIOFLARE_POWER_FORGE.get()))
                        .or(Predicates.blocks(HELIOFLUIX_MELTING_CORE.get()))
                        .or(Predicates.blocks(HELIOTHERMAL_PLASMA_FABRICATOR.get())))
                    .where("K", Predicates.blocks(MEDIARY_GRAVITON_FLOW_REGULATOR.get()))
                    .build()
            }
            .renderer {
                ForgeOfAntichristRenderer(
                    GTLAdditions.id("block/casings/god_forge_inner_casing"),
                    GTLAdditions.id("block/multiblock/forge_of_antichrist"),
                    GOD_FORGE_SUPPORT_CASING,
                    GTLAdditions.id("block/casings/god_forge_support_casing")
                )
            }
            .partAppearance { controller, part, side -> GOD_FORGE_SUPPORT_CASING.get().defaultBlockState() }
            .hasTESR(true)
            .register()

        HELIOFUSION_EXOTICIZER = REGISTRATE.multiblock("heliofusion_exoticizer",
            Function { HelioFusionExoticizer(it) })
            .allRotation()
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.heliofusion_exoticizer.tooltip.0"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.heliofusion_exoticizer.tooltip.1"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.heliofusion_exoticizer.tooltip.2"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.heliofusion_exoticizer.tooltip.3"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist_module.tooltip.0"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist_module.tooltip.1"))
            .tooltipTextKey(Component.translatable("gtceu.machine.eut_multiplier.tooltip", 0.5))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.0"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.1"))
            .tooltipTextKey(Component.translatable("tooltip.gtlcore.structure.source", "GTNH"))
            .tooltipTextRecipeTypes(MATTER_EXOTIC)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeTypes(MATTER_EXOTIC)
            .appearanceBlock(GOD_FORGE_TRIM_CASING)
            .pattern { definition ->
                MultiBlockStructureD.FORGE_OF_THE_ANTICHRIST_MODULE!!
                    .where("~", Predicates.controller(Predicates.blocks(definition.get())))
                    .where("B", Predicates.blocks(GOD_FORGE_TRIM_CASING.get())
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS)))
                    .where("F", Predicates.blocks(PHONON_CONDUIT.get()))
                    .where("G", Predicates.blocks(GOD_FORGE_ENERGY_CASING.get()))
                    .where("D", Predicates.blocks(SUPRACHRONAL_MAGNETIC_CONFINEMENT_CASING.get()))
                    .where("E", Predicates.blocks(GOD_FORGE_SUPPORT_CASING.get()))
                    .build()
            }
            .workableCasingRenderer(GTLAdditions.id("block/casings/god_forge_trim_casing"), GTLAdditions.id("block/multiblock/heliofusion_exoticizer"))
            .register()

        HELIOFLARE_POWER_FORGE = REGISTRATE.multiblock("helioflare_power_forge",
            Function { HelioflarePowerForge(it) })
            .allRotation()
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.helioflare_power_forge.tooltip.0"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.helioflare_power_forge.tooltip.1"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.tooltip.8"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist_module.tooltip.0"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist_module.tooltip.2"))
            .tooltipTextKey(
                Component.translatable(
                    "gtladditions.multiblock.forge_of_the_antichrist_module.tooltip.3",
                    Component.translatable(ALLOY_BLAST_RECIPES.registryName.toLanguageKey())
                )
            )
            .tooltipTextKey(Component.translatable("gtceu.machine.eut_multiplier.tooltip", 0.2))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.0"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.1"))
            .tooltipTextKey(Component.translatable("tooltip.gtlcore.structure.source", "GTNH"))
            .tooltipTextRecipeTypes(FURNACE_RECIPES, BLAST_RECIPES, ALLOY_SMELTER_RECIPES, ALLOY_BLAST_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeTypes(FURNACE_RECIPES, BLAST_RECIPES, ALLOY_SMELTER_RECIPES, ALLOY_BLAST_RECIPES)
            .appearanceBlock(GOD_FORGE_TRIM_CASING)
            .pattern { definition ->
                MultiBlockStructureD.FORGE_OF_THE_ANTICHRIST_MODULE!!
                    .where("~", Predicates.controller(Predicates.blocks(definition.get())))
                    .where("B", Predicates.blocks(GOD_FORGE_TRIM_CASING.get())
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS)))
                    .where("F", Predicates.blocks(PHONON_CONDUIT.get()))
                    .where("G", Predicates.blocks(GOD_FORGE_ENERGY_CASING.get()))
                    .where("D", Predicates.blocks(SUPRACHRONAL_MAGNETIC_CONFINEMENT_CASING.get()))
                    .where("E", Predicates.blocks(GOD_FORGE_SUPPORT_CASING.get()))
                    .build()
            }
            .workableCasingRenderer(GTLAdditions.id("block/casings/god_forge_trim_casing"), GTLAdditions.id("block/multiblock/heliofusion_exoticizer"))
            .register()

        HELIOFLUIX_MELTING_CORE = REGISTRATE.multiblock("heliofluix_melting_core",
            Function { HeliofluixMeltingCore(it) })
            .allRotation()
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.heliofluix_melting_core.tooltip.0"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.heliofluix_melting_core.tooltip.1"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.tooltip.8"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist_module.tooltip.0"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist_module.tooltip.2"))
            .tooltipTextKey(
                Component.translatable(
                    "gtladditions.multiblock.forge_of_the_antichrist_module.tooltip.3",
                    Component.translatable(CHAOTIC_ALCHEMY.registryName.toLanguageKey())
                )
            )
            .tooltipTextKey(Component.translatable("gtceu.machine.eut_multiplier.tooltip", 0.2))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.0"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.1"))
            .tooltipTextKey(Component.translatable("tooltip.gtlcore.structure.source", "GTNH"))
            .tooltipTextRecipeTypes(CHAOTIC_ALCHEMY, MOLECULAR_DECONSTRUCTION)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeTypes(CHAOTIC_ALCHEMY, MOLECULAR_DECONSTRUCTION)
            .appearanceBlock(GOD_FORGE_TRIM_CASING)
            .pattern { definition ->
                MultiBlockStructureD.FORGE_OF_THE_ANTICHRIST_MODULE!!
                    .where("~", Predicates.controller(Predicates.blocks(definition.get())))
                    .where("B", Predicates.blocks(GOD_FORGE_TRIM_CASING.get())
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS)))
                    .where("F", Predicates.blocks(PHONON_CONDUIT.get()))
                    .where("G", Predicates.blocks(GOD_FORGE_ENERGY_CASING.get()))
                    .where("D", Predicates.blocks(SUPRACHRONAL_MAGNETIC_CONFINEMENT_CASING.get()))
                    .where("E", Predicates.blocks(GOD_FORGE_SUPPORT_CASING.get()))
                    .build()
            }
            .workableCasingRenderer(GTLAdditions.id("block/casings/god_forge_trim_casing"), GTLAdditions.id("block/multiblock/heliofusion_exoticizer"))
            .register()

        HELIOTHERMAL_PLASMA_FABRICATOR = REGISTRATE.multiblock("heliothermal_plasma_fabricator",
            Function { HeliothermalPlasmaFabricator(it) })
            .allRotation()
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.heliothermal_plasma_fabricator.tooltip.0"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.heliothermal_plasma_fabricator.tooltip.1"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.tooltip.8"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist_module.tooltip.0"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist_module.tooltip.2"))
            .tooltipTextKey(
                Component.translatable(
                    "gtladditions.multiblock.forge_of_the_antichrist_module.tooltip.3",
                    Component.translatable(FUSION_RECIPES.registryName.toLanguageKey())
                        .append(Component.literal(", "))
                        .append(Component.translatable(SUPER_PARTICLE_COLLIDER_RECIPES.registryName.toLanguageKey()))
                )
            )
            .tooltipTextKey(Component.translatable("gtceu.machine.eut_multiplier.tooltip", 0.2))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.0"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.1"))
            .tooltipTextKey(Component.translatable("tooltip.gtlcore.structure.source", "GTNH"))
            .tooltipTextRecipeTypes(STELLAR_LGNITION, FUSION_RECIPES, SUPER_PARTICLE_COLLIDER_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeTypes(STELLAR_LGNITION, FUSION_RECIPES, SUPER_PARTICLE_COLLIDER_RECIPES)
            .appearanceBlock(GOD_FORGE_TRIM_CASING)
            .pattern { definition ->
                MultiBlockStructureD.FORGE_OF_THE_ANTICHRIST_MODULE!!
                    .where("~", Predicates.controller(Predicates.blocks(definition.get())))
                    .where("B", Predicates.blocks(GOD_FORGE_TRIM_CASING.get())
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS))
                        .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS))
                        .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS)))
                    .where("F", Predicates.blocks(PHONON_CONDUIT.get()))
                    .where("G", Predicates.blocks(GOD_FORGE_ENERGY_CASING.get()))
                    .where("D", Predicates.blocks(SUPRACHRONAL_MAGNETIC_CONFINEMENT_CASING.get()))
                    .where("E", Predicates.blocks(GOD_FORGE_SUPPORT_CASING.get()))
                    .build()
            }
            .workableCasingRenderer(GTLAdditions.id("block/casings/god_forge_trim_casing"), GTLAdditions.id("block/multiblock/heliofusion_exoticizer"))
            .register()

        HEART_OF_THE_UNIVERSE = REGISTRATE.multiblock("heart_of_the_universe",
            Function { HeartOfTheUniverse(it) })
            .nonYAxisRotation()
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.heart_of_the_universe.tooltip.0"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.heart_of_the_universe.tooltip.1"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.heart_of_the_universe.tooltip.2"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.heart_of_the_universe.tooltip.3"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.heart_of_the_universe.tooltip.4"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.heart_of_the_universe.tooltip.5"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.heart_of_the_universe.tooltip.6"))
            .tooltipTextRecipeTypes(GENESIS_ENGINE)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeTypes(GENESIS_ENGINE)
            .appearanceBlock(DIMENSION_INJECTION_CASING)
            .pattern { definition ->
                MultiBlockStructureD.ANNIHILATE_GENERATOR_STRUCTURE!!
                    .where("~", Predicates.controller(Predicates.blocks(definition.get())))
                    .where("A", Predicates.blocks(GRAVITON_FIELD_CONSTRAINT_CASING.get()))
                    .where("B", Predicates.blocks(getBlock("kubejs:annihilate_core")))
                    .where("C", Predicates.blocks(HYPER_MECHANICAL_CASING.get()))
                    .where("D", Predicates.blocks(getBlock("kubejs:dimensional_stability_casing")))
                    .where("E", Predicates.blocks(TEMPORAL_ANCHOR_FIELD_CASING.get()))
                    .where("F", Predicates.blocks(FUSION_GLASS.get()))
                    .where("G", Predicates.blocks(getBlock("kubejs:annihilate_core")))
                    .where("H", Predicates.blocks(RHENIUM_REINFORCED_ENERGY_GLASS.get()))
                    .where("P", Predicates.blocks(DIMENSION_CONNECTION_CASING.get()))
                    .where("S", Predicates.blocks(DIMENSION_INJECTION_CASING.get())
                        .or(Predicates.blocks(Wireless_Energy_Network_OUTPUT_Terminal.get()))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS))
                        .or(Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1)))
                    .where("T", Predicates.blocks(CREATE_CASING.get()))
                    .where("R", Predicates.blocks(getBlock("kubejs:dyson_deployment_magnet")))
                    .where(" ", Predicates.any())
                    .build()
            }
            .renderer{ HeartOfTheUniverseRenderer() }
            .hasTESR(true)
            .register()

        DIMENSION_FOCUS_INFINITY_CRAFTING_ARRAY = REGISTRATE.multiblock("dimension_focus_infinity_crafting_array",
            Function { DimensionFocusInfinityCraftingArray(it) })
            .nonYAxisRotation()
            .tooltipTextMaxParallels(4096.toString())
            .tooltipTextLaser()
            .tooltipTextMultiRecipes()
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.dimension_focus_infinity_crafting_array.tooltip.0"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.dimension_focus_infinity_crafting_array.tooltip.1"))
            .tooltipTextKey(Component.translatable("tooltip.gtlcore.structure.source", "TST"))
            .tooltipTextRecipeTypes(NIGHTMARE_CRAFTING)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(NIGHTMARE_CRAFTING)
            .appearanceBlock(TEMPORAL_ANCHOR_FIELD_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructureD.DIMENSION_FOCUS_INFINITY_CRAFTING_ARRAY!!
                    .where("~", Predicates.controller(Predicates.blocks(definition!!.get())))
                    .where("N", Predicates.blocks(getBlock("gtceu:assembly_line_grating")))
                    .where("O", Predicates.blocks(getBlock("gtceu:assembly_line_casing")))
                    .where("I", Predicates.blocks(getBlock("gtceu:trinium_frame")))
                    .where("L", Predicates.blocks(getBlock("gtlcore:graviton_field_constraint_casing")))
                    .where("B", Predicates.blocks(getBlock("gtlcore:iridium_casing")))
                    .where("F", Predicates.blocks(getBlock("gtceu:high_power_casing")))
                    .where("H", Predicates.blocks(getBlock("gtceu:superconducting_coil")))
                    .where("E", Predicates.blocks(getBlock("gtlcore:advanced_assembly_line_unit")))
                    .where("D", Predicates.blocks(getBlock("gtladditions:temporal_anchor_field_casing")))
                    .where("S", Predicates.blocks(getBlock("gtladditions:temporal_anchor_field_casing"))
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1))
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS))
                        .or(Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1)))
                    .where("J", Predicates.blocks(getBlock("gtlcore:molecular_casing")))
                    .where("Q", Predicates.blocks(getBlock("gtlcore:hyper_mechanical_casing")))
                    .where("P", Predicates.blocks(getBlock("gtceu:naquadah_alloy_frame")))
                    .where("C", Predicates.blocks(getBlock("gtceu:fusion_glass")))
                    .where("K", Predicates.blocks(getBlock("kubejs:containment_field_generator")))
                    .where("M", Predicates.blocks(getBlock("kubejs:annihilate_core")))
                    .where("A", Predicates.blocks(getBlock("gtlcore:dimensionally_transcendent_casing")))
                    .where("G", Predicates.blocks(getBlock("gtceu:advanced_computer_casing")))
                    .where("R", Predicates.blocks(getBlock("gtlcore:rhenium_reinforced_energy_glass")))
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/sps_casing"),
                GTCEu.id("block/multiblock/research_station")
            )
            .register()

        LIGHT_HUNTER_SPACE_STATION = REGISTRATE.multiblock("light_hunter_space_station",
            Function { LightHunterSpaceStation(it) })
            .nonYAxisRotation()
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.light_hunter_space_station.tooltip.0"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.light_hunter_space_station.tooltip.1"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.light_hunter_space_station.tooltip.2"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.light_hunter_space_station.tooltip.3"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.light_hunter_space_station.tooltip.4"))
            .tooltipTextKey(Component.translatable("gtladditions.multiblock.light_hunter_space_station.tooltip.5"))
            .tooltipTextKey(Component.translatable("tooltip.gtlcore.structure.source", "BV1Gu411V7oT"))
            .tooltipTextRecipeTypes(INTER_STELLAR)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(INTER_STELLAR)
            .recipeModifier(GTRecipeModifiers.ELECTRIC_OVERCLOCK.apply(OverclockingLogic(1.0, 1.0, false)))
            .appearanceBlock(HIGH_POWER_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructureE.LIGHT_HUNTER_SPACE_STATION!!
                    .where("~", Predicates.controller(Predicates.blocks(definition!!.get())))
                    .where("b", Predicates.blocks(getBlock("gtceu:uiv_machine_casing")))
                    .where("G", Predicates.blocks(getBlock("kubejs:containment_field_generator")))
                    .where("P", Predicates.blocks(getBlock("kubejs:restraint_device")))
                    .where("`", Predicates.blocks(getBlock("gtceu:computer_heat_vent")))
                    .where("a", Predicates.blocks(getBlock("gtlcore:super_computation_component")))
                    .where("C", Predicates.blocks(getBlock("gtceu:uxv_machine_casing")))
                    .where("^", Predicates.blocks(getBlock("kubejs:module_connector")))
                    .where("X", Predicates.blocks(getBlock("gtlcore:ultimate_stellar_containment_casing")))
                    .where("D", Predicates.blocks(getBlock("gtceu:fusion_glass")))
                    .where("N", Predicates.blocks(getBlock("gtlcore:dimensionally_transcendent_casing")))
                    .where("Q", Predicates.blocks(getBlock("kubejs:spacetime_assembly_line_unit")))
                    .where("U", Predicates.blocks(getBlock("gtlcore:fusion_casing_mk4")))
                    .where("T", Predicates.blocks(getBlock("ae2:quartz_vibrant_glass")))
                    .where("L", Predicates.blocks(getBlock("gtlcore:dragon_strength_tritanium_casing")))
                    .where("A", Predicates.blocks(getBlock("gtceu:uhv_machine_casing")))
                    .where("F", Predicates.blocks(getBlock("kubejs:dimensional_bridge_casing")))
                    .where("I", Predicates.blocks(getBlock("gtladditions:extreme_density_casing")))
                    .where("d", Predicates.blocks(HIGH_POWER_CASING.get())
                        .or(Predicates.abilities(PartAbility.IMPORT_ITEMS))
                        .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(1)))
                    .where("V", Predicates.blocks(getBlock("gtlcore:sps_casing")))
                    .where("R", Predicates.blocks(getBlock("gtceu:high_power_casing")))
                    .where("E", Predicates.blocks(getBlock("gtlcore:dimension_injection_casing")))
                    .where("J", Predicates.blocks(getBlock("gtceu:neutronium_frame")))
                    .where("c", Predicates.blocks(getBlock("gtceu:uhv_ultimate_battery")))
                    .where("_", Predicates.blocks(getBlock("gtceu:advanced_computer_casing")))
                    .where("K", Predicates.blocks(getBlock("gtlcore:echo_casing")))
                    .where("M", Predicates.blocks(getBlock("gtlcore:hyper_core")))
                    .where("S", Predicates.blocks(getBlock("gtlcore:molecular_casing")))
                    .where("=", Predicates.blocks(getBlock("kubejs:neutronium_pipe_casing")))
                    .where("W", Predicates.blocks(getBlock("gtlcore:fusion_casing_mk5")))
                    .where("O", Predicates.blocks(getBlock("gtlcore:uxv_hermetic_casing")))
                    .where("]", Predicates.blocks(getBlock("gtladditions:gravity_stabilization_casing")))
                    .where("H", Predicates.blocks(getBlock("gtceu:atomic_casing")))
                    .where("Z", Predicates.blocks(getBlock("gtlcore:improved_superconductor_coil")))
                    .where("[", Predicates.blocks(getBlock("kubejs:force_field_glass")))
                    .where("B", Predicates.blocks(getBlock("gtlcore:naquadah_alloy_casing")))
                    .where("Y", Predicates.blocks(getBlock("gtlcore:enhance_hyper_mechanical_casing")))
                    .build()
            }
            .renderer { LightHunterSpaceStationRenderer() }
            .hasTESR(true)
            .register()

        MACRO_ATOMIC_RESONANT_FRAGMENT_STRIPPER = if (ConfigHolder.INSTANCE.enableSkyBlokeMode)
            REGISTRATE.multiblock(
                "macro_atomic_resonant_fragment_stripper",
                Function { MacroAtomicResonantFragmentStripper(it) })
                .nonYAxisRotation()
                .tooltipTextKey(Component.translatable("gtladditions.multiblock.macro_atomic_resonant_fragment_stripper.tooltip.0"))
                .tooltipTextKey(Component.translatable("gtladditions.multiblock.macro_atomic_resonant_fragment_stripper.tooltip.1"))
                .tooltipTextKey(Component.translatable("gtladditions.multiblock.macro_atomic_resonant_fragment_stripper.tooltip.2"))
                .tooltipTextKey(Component.translatable("gtceu.machine.eut_multiplier.tooltip", 4))
                .tooltipTextLaser()
                .tooltipTextKey(Component.translatable("gtladditions.multiblock.base_parallel", Component.literal("1536").withStyle(ChatFormatting.GOLD)))
                .tooltipTextKey(Component.translatable("gtladditions.multiblock.macro_atomic_resonant_fragment_stripper.tooltip.3"))
                .tooltipTextKey(Component.translatable("gtladditions.multiblock.macro_atomic_resonant_fragment_stripper.tooltip.4"))
                .tooltipTextKey(Component.translatable("gtladditions.multiblock.macro_atomic_resonant_fragment_stripper.tooltip.5"))
                .tooltipTextKey(Component.translatable("gtladditions.multiblock.macro_atomic_resonant_fragment_stripper.tooltip.6"))
                .tooltipTextKey(Component.translatable("gtladditions.multiblock.macro_atomic_resonant_fragment_stripper.tooltip.7"))
                .tooltipTextKey(Component.translatable("gtladditions.multiblock.macro_atomic_resonant_fragment_stripper.tooltip.8"))
                .tooltipTextKey(Component.translatable("tooltip.gtlcore.structure.source", "TST"))
                .tooltipTextRecipeTypes(STAR_CORE_STRIPPER)
                .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
                .recipeTypes(STAR_CORE_STRIPPER)
                .appearanceBlock(HYPER_MECHANICAL_CASING)
                .pattern { definition: MultiblockMachineDefinition? ->
                    MultiBlockStructureE.MACRO_ATOMIC_RESONANT_FRAGMENT_STRIPPER!!
                        .where("~", Predicates.controller(Predicates.blocks(definition!!.get())))
                        .where("A", Predicates.blocks(FUSION_GLASS.get()))
                        .where("B", Predicates.blocks(QFT_COIL.get()))
                        .where("C", Predicates.blocks(getBlock("kubejs:annihilate_core")))
                        .where("D", Predicates.heatingCoils())
                        .where("E", Predicates.blocks(ECHO_CASING.get()))
                        .where("F", Predicates.blocks(getBlock("kubejs:dimensional_stability_casing")))
                        .where("G", Predicates.blocks(HYPER_MECHANICAL_CASING.get()))
                        .where("H", Predicates.blocks(FUSION_CASING_MK5.get()))
                        .where("I", Predicates.blocks(SPS_CASING.get()))
                        .where("J", Predicates.blocks(getBlock("kubejs:dyson_control_toroid")))
                        .where("K", Predicates.blocks(getBlock("kubejs:dyson_receiver_casing")))
                        .where("L", Predicates.blocks(getBlock("kubejs:dyson_control_casing")))
                        .where("M", Predicates.blocks(getBlock("kubejs:dimensional_bridge_casing")))
                        .where("N", Predicates.blocks(ChemicalHelper.getBlock(frameGt, QuantumChromodynamicallyConfinedMatter)))
                        .where("O", Predicates.blocks(ChemicalHelper.getBlock(frameGt, Neutronium)))
                        .where("Y", Predicates.blocks(HYPER_MECHANICAL_CASING.get())
                                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS))
                                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS))
                                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS))
                                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS))
                                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2))
                                .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1)))
                        .where("Z", Predicates.blocks(HIGH_POWER_CASING.get()))
                        .build()
                }
                .workableCasingRenderer(
                    GTLCore.id("block/casings/hyper_mechanical_casing"),
                    GTCEu.id("block/multiblock/fusion_reactor")
                )
                .register()
        else null

        REGISTRATE.creativeModeTab { GTLAddCreativeModeTabs.GTLADD_ITEMS }
    }

    fun init() {}
}
