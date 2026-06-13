package com.gtladd.gtladditions.common.machine.multiblock

import appeng.core.definitions.AEBlocks
import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.data.RotationState
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.frameGt
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility.EXPORT_FLUIDS
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility.EXPORT_ITEMS
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility.IMPORT_FLUIDS
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility.IMPORT_ITEMS
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility.INPUT_ENERGY
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility.INPUT_LASER
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility.MAINTENANCE
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo
import com.gregtechceu.gtceu.api.pattern.Predicates.*
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic
import com.gregtechceu.gtceu.api.recipe.logic.OCParams
import com.gregtechceu.gtceu.api.recipe.logic.OCResult
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier
import com.gregtechceu.gtceu.common.data.GCyMBlocks
import com.gregtechceu.gtceu.common.data.GCyMBlocks.HEAT_VENT
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
import com.gtladd.gtladditions.client.render.machine.ArcanicAstrographRender
import com.gtladd.gtladditions.client.render.machine.ForgeOfAntichristRenderer
import com.gtladd.gtladditions.client.render.machine.HeartOfTheUniverseRenderer
import com.gtladd.gtladditions.client.render.machine.PartWorkableCasingMachineRenderer
import com.gtladd.gtladditions.client.render.machine.SubspaceCorridorHubIndustrialArrayRenderer
import com.gtladd.gtladditions.client.render.machine.TimeSpaceDistorterRenderer
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.CENTRAL_GRAVITON_FLOW_REGULATOR
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.GOD_FORGE_ENERGY_CASING
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.GOD_FORGE_INNER_CASING
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.GOD_FORGE_SUPPORT_CASING
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.GOD_FORGE_TRIM_CASING
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.MEDIARY_GRAVITON_FLOW_REGULATOR
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.PHONON_CONDUIT
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks.POWER_MODULE_7
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
import com.gtladd.gtladditions.common.machine.GTLAddPredicates
import com.gtladd.gtladditions.common.machine.multiblock.controller.*
import com.gtladd.gtladditions.common.machine.multiblock.controller.module.*
import com.gtladd.gtladditions.common.machine.multiblock.controller.rrf.*
import com.gtladd.gtladditions.common.machine.multiblock.structure.*
import com.gtladd.gtladditions.common.modify.GTLAddCreativeModeTabs
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.BIOLOGICAL_SIMULATION
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.CHAOS_WEAVE
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.CHAOTIC_ALCHEMY
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.COMPRESSED_ASTRAL_ARRAY
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.EM_RESONANCE_CONVERSION_FIELD
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.GENESIS_ENGINE
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.INTER_STELLAR
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.LEYLINE_CRYSTALLIZE
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.MATTER_EXOTIC
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.MOLECULAR_DECONSTRUCTION
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.NIGHTMARE_CRAFTING
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.PHOTON_MATRIX_ETCH
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.SPACE_ORE_PROCESSOR
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.STAR_CORE_STRIPPER
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.STELLAR_LGNITION
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.TECTONIC_FAULT_GENERATOR
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.TRANSMUTATION_BLOCK_CONVERSION
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.VOIDFLUX_REACTION
import com.gtladd.gtladditions.utils.CommonUtils.createObfuscatedDeleteComponent
import com.gtladd.gtladditions.utils.ComponentExtensions.literal
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.hepdd.gtmthings.data.CustomMachines
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.ChatFormatting
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.SlabType
import org.gtlcore.gtlcore.GTLCore
import org.gtlcore.gtlcore.api.pattern.GTLPredicates
import org.gtlcore.gtlcore.common.block.BlockMap
import org.gtlcore.gtlcore.common.block.GTLFusionCasingBlock
import org.gtlcore.gtlcore.common.data.GTLBlocks.*
import org.gtlcore.gtlcore.common.data.GTLMachines
import org.gtlcore.gtlcore.common.data.GTLMachines.GTAEMachines.ME_EXTENDED_EXPORT_BUFFER
import org.gtlcore.gtlcore.common.data.GTLMachines.GTAEMachines.STOCKING_IMPORT_BUS_ME
import org.gtlcore.gtlcore.common.data.GTLMaterials.*
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
    val SUBATOMIC_TRANSMUTATIOON_CORE: MultiblockMachineDefinition
    val ASTRAL_CONVERGENCE_NEXUS: MultiblockMachineDefinition
    val NEBULA_REAPER: MultiblockMachineDefinition
    val ARCANIC_ASTROGRAPH: MultiblockMachineDefinition
    val ARCANE_CACHE_VAULT: MultiblockMachineDefinition
    val SPACE_SCALING_INSTRUMENT: MultiblockMachineDefinition
    val DRACONIC_COLLAPSE_CORE: MultiblockMachineDefinition
    val TITAN_CRIP_EARTHBORE: MultiblockMachineDefinition
    val BIOLOGICAL_SIMULATION_LABORATORY: MultiblockMachineDefinition
    val DIMENSIONALLY_TRANSCENDENT_CHEMICAL_PLANT: MultiblockMachineDefinition
    val QUANTUM_SYPHON_MATRIX: MultiblockMachineDefinition
    val FUXI_BAGUA_HEAVEN_FORGING_FURNACE: MultiblockMachineDefinition
    val ANTIENTROPY_CONDENSATION_CENTER: MultiblockMachineDefinition
    val TAIXU_TURBID_ARRAY: MultiblockMachineDefinition
    val PLANETARY_IONISATION_CONVERGENCE_TOWER: MultiblockMachineDefinition
    val INFERNO_CLEFT_SMELTING_VAULT: MultiblockMachineDefinition
    val SKELETON_SHIFT_RIFT_ENGINE: MultiblockMachineDefinition
    val TIME_SPACE_DISTORTER: MultiblockMachineDefinition
    val APOCALYPTIC_TORSION_QUANTUM_MATRIX: MultiblockMachineDefinition
    val FORGE_OF_THE_ANTICHRIST: MultiblockMachineDefinition
    val RECURSIVE_REVERSE_ARRAY: MultiblockMachineDefinition
    val CATALYTIC_CASCADE_ARRAY: MultiblockMachineDefinition
    val MAGNETORHEOLOGICAL_CONVERGENCE_CORE: MultiblockMachineDefinition
    val SPACETIME_STASIS_DEVICE: MultiblockMachineDefinition
    val SUPRATEMPORAL_BOOSTING_ENGINE: MultiblockMachineDefinition
    val HELIOFUSION_EXOTICIZER: MultiblockMachineDefinition
    val HELIOFLARE_POWER_FORGE: MultiblockMachineDefinition
    val HELIOFLUIX_MELTING_CORE: MultiblockMachineDefinition
    val HELIOTHERMAL_PLASMA_FABRICATOR: MultiblockMachineDefinition
    val HELIOPHASE_LEYLINE_CRYSTALLIZER: MultiblockMachineDefinition
    val HEART_OF_THE_UNIVERSE: MultiblockMachineDefinition
    val SUBSPACE_CORRIDOR_HUB_INDUSTRIAL_ARRAY: MultiblockMachineDefinition
    val DIMENSION_FOCUS_INFINITY_CRAFTING_ARRAY: MultiblockMachineDefinition
    val SPACE_INFINITY_INTEGRATED_ORE_PROCESSOR: MultiblockMachineDefinition
    val MACRO_ATOMIC_RESONANT_FRAGMENT_STRIPPER: MultiblockMachineDefinition

    init {
        REGISTRATE.creativeModeTab { GTLAddCreativeModeTabs.GTLADD_MACHINE }
        NEXUS_SATELLITE_FACTORY_MKI = REGISTRATE.multiblock(
            "nexus_satellite_factory_mk1",
            Function { SubspaceCorridorHubIndustrialArrayModuleBase(it) }
        )
            .nonYAxisRotation()
            .tooltipTextKey("gtladditions.multiblock.nexus_satellite_factory.tooltip.0".toComponent)
            .tooltipTextMaxParallels(Int.MAX_VALUE.toString())
            .tooltipTextLaser()
            .tooltipTextMultiRecipes()
            .tooltipTextRecipeTypes(
                LATHE_RECIPES, BENDER_RECIPES, COMPRESSOR_RECIPES, FORGE_HAMMER_RECIPES, CUTTER_RECIPES,
                EXTRUDER_RECIPES, MIXER_RECIPES, WIREMILL_RECIPES, FORMING_PRESS_RECIPES, POLARIZER_RECIPES
            )
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
            .appearanceBlock(DIMENSIONALLY_TRANSCENDENT_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.SUBSPACE_CORRIDOR_HUB_INDUSTRIAL_ARRAY_MODULE!!
                    .where("~", controller(blocks(definition!!.get())))
                    .where("A", blocks(getBlock("kubejs:module_connector")))
                    .where(
                        "B",
                        blocks(DIMENSIONALLY_TRANSCENDENT_CASING.get())
                            .or(abilities(MAINTENANCE).setExactLimit(1))
                            .or(abilities(EXPORT_ITEMS).setPreviewCount(1))
                            .or(abilities(IMPORT_ITEMS).setPreviewCount(1))
                            .or(abilities(EXPORT_FLUIDS).setPreviewCount(1))
                            .or(abilities(IMPORT_FLUIDS).setPreviewCount(1))
                            .or(abilities(INPUT_ENERGY).setMaxGlobalLimited(2))
                            .or(abilities(INPUT_LASER).setMaxGlobalLimited(1))
                            .or(abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1))
                    )
                    .where("C", blocks(getBlock("kubejs:module_base")))
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/dimensionally_transcendent_casing"),
                GTCEu.id("block/multiblock/fusion_reactor")
            )
            .register()

        NEXUS_SATELLITE_FACTORY_MKII = REGISTRATE.multiblock(
            "nexus_satellite_factory_mk2",
            Function { SubspaceCorridorHubIndustrialArrayModuleBase(it) }
        )
            .nonYAxisRotation()
            .tooltipTextKey("gtladditions.multiblock.nexus_satellite_factory.tooltip.0".toComponent)
            .tooltipTextMaxParallels(Int.MAX_VALUE.toString())
            .tooltipTextLaser()
            .tooltipTextMultiRecipes()
            .tooltipTextRecipeTypes(
                ROCK_BREAKER_RECIPES, ORE_WASHER_RECIPES, CENTRIFUGE_RECIPES, ELECTROLYZER_RECIPES,
                SIFTER_RECIPES, MACERATOR_RECIPES, DEHYDRATOR_RECIPES, THERMAL_CENTRIFUGE_RECIPES, ELECTROMAGNETIC_SEPARATOR_RECIPES
            )
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
            .appearanceBlock(DIMENSIONALLY_TRANSCENDENT_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.SUBSPACE_CORRIDOR_HUB_INDUSTRIAL_ARRAY_MODULE!!
                    .where("~", controller(blocks(definition!!.get())))
                    .where("A", blocks(getBlock("kubejs:module_connector")))
                    .where(
                        "B",
                        blocks(DIMENSIONALLY_TRANSCENDENT_CASING.get())
                            .or(abilities(MAINTENANCE).setExactLimit(1))
                            .or(abilities(EXPORT_ITEMS).setPreviewCount(1))
                            .or(abilities(IMPORT_ITEMS).setPreviewCount(1))
                            .or(abilities(EXPORT_FLUIDS).setPreviewCount(1))
                            .or(abilities(IMPORT_FLUIDS).setPreviewCount(1))
                            .or(abilities(INPUT_ENERGY).setMaxGlobalLimited(2))
                            .or(abilities(INPUT_LASER).setMaxGlobalLimited(1))
                            .or(abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1))
                    )
                    .where("C", blocks(getBlock("kubejs:module_base")))
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/dimensionally_transcendent_casing"),
                GTCEu.id("block/multiblock/fusion_reactor")
            )
            .register()

        NEXUS_SATELLITE_FACTORY_MKIII = REGISTRATE.multiblock(
            "nexus_satellite_factory_mk3",
            Function { SubspaceCorridorHubIndustrialArrayModuleBase(it) }
        )
            .nonYAxisRotation()
            .tooltipTextKey("gtladditions.multiblock.nexus_satellite_factory.tooltip.0".toComponent)
            .tooltipTextMaxParallels(Int.MAX_VALUE.toString())
            .tooltipTextLaser()
            .tooltipTextMultiRecipes()
            .tooltipTextRecipeTypes(
                EVAPORATION_RECIPES, AUTOCLAVE_RECIPES, EXTRACTOR_RECIPES, BREWING_RECIPES, FERMENTING_RECIPES,
                DISTILLERY_RECIPES, DISTILLATION_RECIPES, FLUID_HEATER_RECIPES, FLUID_SOLIDFICATION_RECIPES, CHEMICAL_BATH_RECIPES
            )
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
            .appearanceBlock(DIMENSIONALLY_TRANSCENDENT_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.SUBSPACE_CORRIDOR_HUB_INDUSTRIAL_ARRAY_MODULE!!
                    .where("~", controller(blocks(definition!!.get())))
                    .where("A", blocks(getBlock("kubejs:module_connector")))
                    .where(
                        "B",
                        blocks(DIMENSIONALLY_TRANSCENDENT_CASING.get())
                            .or(abilities(MAINTENANCE).setExactLimit(1))
                            .or(abilities(EXPORT_ITEMS).setPreviewCount(1))
                            .or(abilities(IMPORT_ITEMS).setPreviewCount(1))
                            .or(abilities(EXPORT_FLUIDS).setPreviewCount(1))
                            .or(abilities(IMPORT_FLUIDS).setPreviewCount(1))
                            .or(abilities(INPUT_ENERGY).setMaxGlobalLimited(2))
                            .or(abilities(INPUT_LASER).setMaxGlobalLimited(1))
                            .or(abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1))
                    )
                    .where("C", blocks(getBlock("kubejs:module_base")))
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/dimensionally_transcendent_casing"),
                GTCEu.id("block/multiblock/fusion_reactor")
            )
            .register()

        NEXUS_SATELLITE_FACTORY_MKIV = REGISTRATE.multiblock(
            "nexus_satellite_factory_mk4",
            Function { SubspaceCorridorHubIndustrialArrayModuleBase(it) }
        )
            .nonYAxisRotation()
            .tooltipTextKey("gtladditions.multiblock.nexus_satellite_factory.tooltip.0".toComponent)
            .tooltipTextMaxParallels(Int.MAX_VALUE.toString())
            .tooltipTextLaser()
            .tooltipTextMultiRecipes()
            .tooltipTextRecipeTypes(
                CANNER_RECIPES,
                ARC_FURNACE_RECIPES,
                LIGHTNING_PROCESSOR_RECIPES,
                ASSEMBLER_RECIPES,
                PRECISION_ASSEMBLER_RECIPES,
                CIRCUIT_ASSEMBLER_RECIPES
            )
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(CANNER_RECIPES) // 装罐机
            .recipeType(ARC_FURNACE_RECIPES) // 电弧炉
            .recipeType(LIGHTNING_PROCESSOR_RECIPES) // 闪电处理
            .recipeType(ASSEMBLER_RECIPES) // 组装机
            .recipeType(PRECISION_ASSEMBLER_RECIPES) // 精密组装
            .recipeType(CIRCUIT_ASSEMBLER_RECIPES) // 电路组装机
            .appearanceBlock(DIMENSIONALLY_TRANSCENDENT_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.SUBSPACE_CORRIDOR_HUB_INDUSTRIAL_ARRAY_MODULE!!
                    .where("~", controller(blocks(definition!!.get())))
                    .where("A", blocks(getBlock("kubejs:module_connector")))
                    .where(
                        "B",
                        blocks(DIMENSIONALLY_TRANSCENDENT_CASING.get())
                            .or(abilities(MAINTENANCE).setExactLimit(1))
                            .or(abilities(EXPORT_ITEMS).setPreviewCount(1))
                            .or(abilities(IMPORT_ITEMS).setPreviewCount(1))
                            .or(abilities(EXPORT_FLUIDS).setPreviewCount(1))
                            .or(abilities(IMPORT_FLUIDS).setPreviewCount(1))
                            .or(abilities(INPUT_ENERGY).setMaxGlobalLimited(2))
                            .or(abilities(INPUT_LASER).setMaxGlobalLimited(1))
                            .or(abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1))
                    )
                    .where("C", blocks(getBlock("kubejs:module_base")))
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/dimensionally_transcendent_casing"),
                GTCEu.id("block/multiblock/fusion_reactor")
            )
            .register()

        LUCID_ETCHDREAMER = REGISTRATE.multiblock(
            "lucid_etchdreamer",
            Function { GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(it) }
        )
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
                    .where("I", controller(blocks(definition!!.get())))
                    .where(
                        "A",
                        blocks(IRIDIUM_CASING.get())
                            .or(autoAbilities(*definition.recipeTypes))
                            .or(abilities(MAINTENANCE).setExactLimit(1))
                            .or(abilities(INPUT_LASER).setMaxGlobalLimited(1))
                            .or(abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1))
                    )
                    .where("D", heatingCoils())
                    .where("E", blocks(getBlock("kubejs:neutronium_pipe_casing")))
                    .where("B", blocks(DIMENSIONALLY_TRANSCENDENT_CASING.get()))
                    .where("C", blocks(DIMENSION_INJECTION_CASING.get()))
                    .where("E", blocks(getBlock("kubejs:neutronium_pipe_casing")))
                    .where("G", blocks(GRAVITON_FIELD_CONSTRAINT_CASING.get()))
                    .where("F", blocks(CLEANROOM_GLASS.get()))
                    .where("H", blocks(getBlock("kubejs:annihilate_core")))
                    .where(" ", any())
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/iridium_casing"),
                GTCEu.id("block/multiblock/gcym/large_engraving_laser")
            )
            .register()

        ATOMIC_TRANSMUTATIOON_CORE = REGISTRATE.multiblock(
            "atomic_transmutation_core",
            Function { GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(it) }
        )
            .noneRotation()
            .tooltipTextKey("tooltip.gtladditions.discontinued".toComponent)
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
                    .where("~", controller(blocks(definition!!.get())))
                    .where(
                        "A",
                        blocks(ALUMINIUM_BRONZE_CASING.get())
                            .or(autoAbilities(*definition.recipeTypes))
                            .or(abilities(MAINTENANCE).setExactLimit(1))
                            .or(abilities(INPUT_LASER).setMaxGlobalLimited(1))
                            .or(abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1))
                    )
                    .where("C", heatingCoils())
                    .where("D", blocks(getBlock("kubejs:infused_obsidian")))
                    .where("B", blocks(CLEANROOM_GLASS.get()))
                    .where(" ", any())
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/aluminium_bronze_casing"),
                GTCEu.id("block/multiblock/cleanroom")
            )
            .register()

        SUBATOMIC_TRANSMUTATIOON_CORE = REGISTRATE.multiblock("subatomic_transmutatioon_core", Function { ConversationMachine(it) })
            .noneRotation()
            .tooltipTextKey(
                "gtceu.multiblock.subatomic_transmutatioon_core.tooltip.0".toComponent,
                "gtceu.multiblock.subatomic_transmutatioon_core.tooltip.1".toComponent,
                "gtceu.machine.hold_g.tooltip.1".toComponent
            )
            .tooltipTextRecipeTypes(EM_RESONANCE_CONVERSION_FIELD)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(TRANSMUTATION_BLOCK_CONVERSION)
            .appearanceBlock(LAFIUM_MECHANICAL_CASING)
            .pattern {
                MultiBlockStructure.SUBATOMIC_TRANSMUTATIOON_CORE
                    .where("~", controller(blocks(it.get())))
                    .where(
                        "d",
                        blocks(LAFIUM_MECHANICAL_CASING.get())
                            .or(blocks(GTLAddMachines.ME_BLOCK_CONVERSATION.block).setMaxGlobalLimited(1))
                            .or(abilities(INPUT_ENERGY).setMaxGlobalLimited(1))
                    )
                    .where("E", blocks(DRAGON_STRENGTH_TRITANIUM_CASING.get()))
                    .where("B", blocks(ECHO_CASING.get()))
                    .where("L", blocks(getBlock("kubejs:dyson_deployment_magnet")))
                    .where("D", blocks(LAFIUM_MECHANICAL_CASING.get()))
                    .where("F", blocks(RHENIUM_REINFORCED_ENERGY_GLASS.get()))
                    .where("A", blocks(SPS_CASING.get()))
                    .where("K", blocks(getBlock("kubejs:force_field_glass")))
                    .where("I", blocks(DIMENSION_INJECTION_CASING.get()))
                    .where("H", blocks(getBlock("kubejs:containment_field_generator")))
                    .where("C", blocks(ChemicalHelper.getBlock(frameGt, Adamantium)))
                    .where("G", GTLAddPredicates.heatingCoils(14400))
                    .where("J", blocks(getBlock("kubejs:restraint_device")))
                    .where(" ", any())
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/lafium_mechanical_casing"),
                GTCEu.id("block/multiblock/cleanroom")
            )
            .register()

        ASTRAL_CONVERGENCE_NEXUS = REGISTRATE.multiblock(
            "astral_convergence_nexus",
            Function { AdvancedSpaceElevatorModuleMachine(it) }
        )
            .nonYAxisRotation()
            .tooltipTextMaxParallels("gtceu.multiblock.max_parallel.space_elevator_module".toComponent)
            .tooltipTextLaser()
            .tooltipTextMultiRecipes()
            .tooltipTextRecipeTypes(ASSEMBLER_MODULE_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(ASSEMBLER_MODULE_RECIPES) // 婢额亞鈹栫紒鍕棅
            .appearanceBlock(SPACE_ELEVATOR_MECHANICAL_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                FactoryBlockPattern.start()
                    .aisle("aaa", "bcb", "bbb", "bbb", "bbb")
                    .aisle("aaa", "bbb", "bbb", "bbb", "bbb")
                    .aisle("aaa", "bbb", "bbb", "b~b", "bbb")
                    .where("~", controller(blocks(definition!!.get())))
                    .where(
                        "b",
                        blocks(SPACE_ELEVATOR_MECHANICAL_CASING.get())
                            .or(autoAbilities(*definition.recipeTypes))
                            .or(abilities(INPUT_LASER).setMaxGlobalLimited(1))
                            .or(abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1))
                    )
                    .where("a", blocks(getBlock("kubejs:module_base")))
                    .where("c", blocks(getBlock("kubejs:module_connector")))
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/space_elevator_mechanical_casing"),
                GTCEu.id("block/multiblock/gcym/large_assembler")
            )
            .register()

        NEBULA_REAPER = REGISTRATE.multiblock(
            "nebula_reaper",
            Function { AdvancedSpaceElevatorModuleMachine(it) }
        )
            .nonYAxisRotation()
            .tooltipTextMaxParallels("gtceu.multiblock.max_parallel.space_elevator_module".toComponent)
            .tooltipTextLaser()
            .tooltipTextMultiRecipes()
            .tooltipTextRecipeTypes(MINER_MODULE_RECIPES, DRILLING_MODULE_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(MINER_MODULE_RECIPES) // 婢额亞鈹栭柌鍥╃唵
            .recipeType(DRILLING_MODULE_RECIPES) // 婢额亞鈹栭柦璁崇俺
            .appearanceBlock(SPACE_ELEVATOR_MECHANICAL_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                FactoryBlockPattern.start()
                    .aisle("aaa", "bcb", "bbb", "bbb", "bbb")
                    .aisle("aaa", "bbb", "bbb", "bbb", "bbb")
                    .aisle("aaa", "bbb", "bbb", "b~b", "bbb")
                    .where("~", controller(blocks(definition!!.get())))
                    .where(
                        "b",
                        blocks(SPACE_ELEVATOR_MECHANICAL_CASING.get())
                            .or(autoAbilities(*definition.recipeTypes))
                            .or(abilities(INPUT_LASER).setMaxGlobalLimited(1))
                            .or(abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1))
                    )
                    .where("a", blocks(getBlock("kubejs:module_base")))
                    .where("c", blocks(getBlock("kubejs:module_connector")))
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/space_elevator_mechanical_casing"),
                GTCEu.id("block/multiblock/gcym/large_assembler")
            )
            .register()

        ARCANIC_ASTROGRAPH = REGISTRATE.multiblock(
            "arcanic_astrograph",
            Function { ArcanicAstrograph(it) }
        )
            .nonYAxisRotation()
            .recipeType(COSMOS_SIMULATION_RECIPES)
            .recipeType(COMPRESSED_ASTRAL_ARRAY)
            .tooltips(*arrayOf<Component>("gtladditions.multiblock.base_parallel".toComponent("2048".literal.withStyle(ChatFormatting.GOLD))))
            .tooltips(*arrayOf<Component>("gtceu.multiblock.arcanic_astrograph".toComponent))
            .tooltips(*arrayOf<Component>("gtceu.machine.eye_of_harmony.tooltip.0".toComponent))
            .tooltips(*arrayOf<Component>("gtceu.machine.eye_of_harmony.tooltip.1".toComponent))
            .tooltips(*arrayOf<Component>("gtceu.machine.eye_of_harmony.tooltip.2".toComponent))
            .tooltips(*arrayOf<Component>("gtceu.machine.eye_of_harmony.tooltip.3".toComponent))
            .tooltips(*arrayOf<Component>("gtceu.machine.eye_of_harmony.tooltip.4".toComponent))
            .tooltips(*arrayOf<Component>("gtceu.machine.eye_of_harmony.tooltip.5".toComponent))
            .tooltips(*arrayOf<Component>("gtceu.machine.eye_of_harmony.tooltip.6".toComponent))
            .tooltips(
                *arrayOf<Component>(
                    "gtceu.machine.available_recipe_map_2.tooltip".toComponent(
                        "gtceu.cosmos_simulation".toComponent,
                        "gtceu.compressed_astral_array".toComponent
                    )
                )
            )
            .tooltips("gtladditions.multiblock.arcanic_astrograph.tooltip.0".toComponent)
            .tooltips("gtladditions.multiblock.arcanic_astrograph.tooltip.1".toComponent)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .appearanceBlock(CREATE_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.EYE_OF_HARMONY_STRUCTURE!!
                    .where('~', controller(blocks(definition!!.get())))
                    .where('A', blocks(CREATE_CASING.get()))
                    .where(
                        'B',
                        blocks(HIGH_POWER_CASING.get())
                            .or(abilities(EXPORT_ITEMS).setPreviewCount(1))
                            .or(abilities(IMPORT_ITEMS).setMaxGlobalLimited(1))
                            .or(abilities(EXPORT_FLUIDS).setPreviewCount(1))
                            .or(abilities(IMPORT_FLUIDS).setPreviewCount(1))
                    )
                    .where('D', blocks(DIMENSION_INJECTION_CASING.get()))
                    .where('E', blocks(getBlock("kubejs:dimension_creation_casing")))
                    .where('F', blocks(getBlock("kubejs:spacetime_compression_field_generator")))
                    .where('G', blocks(getBlock("kubejs:dimensional_stability_casing")))
                    .where(" ", any())
                    .build()
            }
            .renderer { ArcanicAstrographRender() }
            .partAppearance { _, _, _ -> HIGH_POWER_CASING.get().defaultBlockState() }
            .hasTESR(true)
            .register()

        ARCANE_CACHE_VAULT = REGISTRATE.multiblock(
            "arcane_cache_vault",
            Function { GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(it) }
        )
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
                    .where("~", controller(blocks(definition!!.get())))
                    .where(
                        "A",
                        blocks(PIKYONIUM_MACHINE_CASING.get())
                            .or(autoAbilities(*definition.recipeTypes))
                            .or(abilities(MAINTENANCE).setExactLimit(1))
                            .or(abilities(INPUT_LASER).setMaxGlobalLimited(1))
                            .or(abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1))
                    )
                    .where("B", heatingCoils())
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/pikyonium_machine_casing"),
                GTCEu.id("block/multiblock/gcym/large_packer")
            )
            .register()

        SPACE_SCALING_INSTRUMENT = REGISTRATE.multiblock(
            "space_scaling_instrument",
            Function { SpaceScalingInstrument(it) }
        )
            .allRotation()
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist.tooltip.8".toComponent)
            .tooltipTextKey("gtladditions.multiblock.space_scaling_instrument.tooltip.0".toComponent)
            .tooltipTextLaser()
            .tooltipTextMultiRecipes()
            .tooltipTextRecipeTypes(PACKER_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(PACKER_RECIPES)
            .appearanceBlock(OXIDATION_RESISTANT_HASTELLOY_N_MECHANICAL_CASING)
            .pattern {
                MultiBlockStructure.ARCANE_CACHE_VAULT_STRUCTURE
                    .where("W", controller(blocks(it.get())))
                    .where(
                        "C",
                        blocks(OXIDATION_RESISTANT_HASTELLOY_N_MECHANICAL_CASING.get())
                            .or(autoAbilities(*it.recipeTypes))
                            .or(abilities(MAINTENANCE).setExactLimit(1))
                            .or(abilities(INPUT_LASER).setMaxGlobalLimited(1))
                    )
                    .where("R", blocks(getBlock("kubejs:force_field_glass")))
                    .where("S", blocks(PIKYONIUM_MACHINE_CASING.get()))
                    .where("T", GTLAddPredicates.heatingCoils(273, true))
                    .where("N", blocks(ChemicalHelper.getBlock(frameGt, NaquadahAlloy)))
                    .where("G", blocks(DIMENSIONALLY_TRANSCENDENT_CASING.get()))
                    .where("M", blocks(getBlock("kubejs:molecular_coil")))
                    .where("K", blocks(FUSION_GLASS.get()))
                    .where("D", blocks(NAQUADAH_ALLOY_CASING.get()))
                    .where("B", blocks(IRIDIUM_CASING.get()))
                    .where("P", blocks(getBlock("kubejs:neutronium_gearbox")))
                    .where("L", blocks(FILTER_CASING_STERILE.get()))
                    .where("H", blocks(GCyMBlocks.CASING_ATOMIC.get()))
                    .where("Q", blocks(ANTIFREEZE_HEATPROOF_MACHINE_CASING.get()))
                    .where("J", blocks(HERMETIC_CASING_UHV.get()))
                    .where("I", blocks(MOLECULAR_CASING.get()))
                    .where("V", blocks(getBlock("kubejs:hollow_casing")))
                    .where("E", blocks(DIMENSION_INJECTION_CASING.get()))
                    .where("U", blocks(getBlock("kubejs:neutronium_pipe_casing")))
                    .where("F", blocks(ChemicalHelper.getBlock(frameGt, Quantanium)))
                    .where("O", blocks(ADVANCED_COMPUTER_CASING.get()))
                    .where(" ", any())
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/oxidation_resistant_hastelloy_n_mechanical_casing"),
                GTCEu.id("block/multiblock/gcym/large_packer")
            )
            .register()

        DRACONIC_COLLAPSE_CORE = REGISTRATE.multiblock(
            "draconic_collapse_core",
            Function {
                object : MutableElectricMultiblockMachine(it) {
                    override fun getMaxParallel(): Int = 8.0.pow(tier - 10).toInt()
                }
            }
        )
            .nonYAxisRotation()
            .tooltipTextKey("gtceu.multiblock.max_parallel.draconic_collapse_core".toComponent)
            .tooltipOnlyTextLaser()
            .tooltipTextPerfectOverclock()
            .tooltipTextRecipeTypes(AGGREGATION_DEVICE_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(AGGREGATION_DEVICE_RECIPES)
            .recipeModifiers(
                { machine: MetaMachine?, recipe: GTRecipe?, _: OCParams?, _: OCResult? ->
                    GTRecipeModifiers.accurateParallel(
                        machine,
                        recipe!!,
                        8.0.pow(((machine as WorkableElectricMultiblockMachine).getTier() - 10).toDouble()).toInt(),
                        false
                    ).getFirst()
                },
                GTRecipeModifiers.ELECTRIC_OVERCLOCK.apply(OverclockingLogic.PERFECT_OVERCLOCK)
            )
            .appearanceBlock(FUSION_CASING_MK5)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.DRACONIC_COLLAPSE_CORE_STRUCTURE!!
                    .where("E", controller(blocks(definition!!.get())))
                    .where(
                        "D",
                        blocks(GTLFusionCasingBlock.getCasingState(10))
                            .or(abilities(INPUT_LASER).setMaxGlobalLimited(2).setMinGlobalLimited(1))
                            .or(abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1))
                    )
                    .where(
                        "L",
                        blocks(GTLFusionCasingBlock.getCasingState(10))
                            .or(blocks(GTMachines.ITEM_IMPORT_BUS[0].get()))
                            .or(blocks(CustomMachines.HUGE_ITEM_IMPORT_BUS[0].get()))
                            .or(blocks(ME_SUPER_PATTERN_BUFFER.get()))
                            .or(blocks(ME_SUPER_PATTERN_BUFFER_PROXY.get()))
                    )
                    .where("I", blocks(MOLECULAR_CASING.get()))
                    .where("K", blocks(getBlock("kubejs:annihilate_core")))
                    .where("J", blocks(getBlock("kubejs:aggregatione_core")))
                    .where("F", blocks(DIMENSIONALLY_TRANSCENDENT_CASING.get()))
                    .where("B", blocks(ChemicalHelper.getBlock(frameGt, Neutronium)))
                    .where("A", blocks(DIMENSION_INJECTION_CASING.get()))
                    .where("C", blocks(GTLFusionCasingBlock.getCasingState(10)))
                    .where("H", blocks(getBlock("kubejs:hollow_casing")))
                    .where("G", blocks(GTLFusionCasingBlock.getCompressedCoilState(10)))
                    .where(
                        "O",
                        blocks(GTLFusionCasingBlock.getCasingState(10))
                            .or(
                                GTLPredicates.diffAbilities(
                                    listOf<PartAbility?>(EXPORT_ITEMS),
                                    listOf<PartAbility?>(IMPORT_ITEMS, IMPORT_FLUIDS)
                                )
                            )
                    )
                    .where(" ", any())
                    .build()
            }
            .additionalDisplay { controller: IMultiController?, components: MutableList<Component?>? ->
                if (controller!!.isFormed) {
                    components!!.add(
                        "gtceu.multiblock.parallel".toComponent(
                            (
                                FormattingUtil.formatNumbers(
                                    8.0.pow(((controller as WorkableElectricMultiblockMachine).getTier() - 10).toDouble())
                                )
                                ).toComponent
                                .withStyle(ChatFormatting.DARK_PURPLE)
                        ).withStyle(ChatFormatting.GRAY)
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
                    override fun getMaxParallel(): Int = 2.0.pow(tier - 6).toInt()
                }
            }
        )
            .noneRotation()
            .tooltipTextKey("gtceu.multiblock.max_parallel.titan_crip_earthbore".toComponent)
            .tooltipTextPerfectOverclock()
            .tooltipTextRecipeTypes(TECTONIC_FAULT_GENERATOR)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(TECTONIC_FAULT_GENERATOR)
            .recipeModifiers(
                *arrayOf<RecipeModifier?>(
                    RecipeModifier { machine: MetaMachine?, recipe: GTRecipe?, _: OCParams?, _: OCResult? ->
                        GTRecipeModifiers.accurateParallel(
                            machine,
                            recipe!!,
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
                    .where("~", controller(blocks(definition!!.get())))
                    .where("I", blocks(getBlock("kubejs:neutronium_gearbox")))
                    .where("H", blocks(getBlock("kubejs:neutronium_pipe_casing")))
                    .where("G", blocks(getBlock("kubejs:machine_casing_grinding_head")))
                    .where("B", blocks(ChemicalHelper.getBlock(frameGt, Neutronium)))
                    .where("C", blocks(ECHO_CASING.get()))
                    .where("A", blocks(MOLECULAR_CASING.get()))
                    .where("F", blocks(getBlock("minecraft:bedrock")))
                    .where("D", blocks(getBlock("kubejs:molecular_coil")))
                    .where(
                        "E",
                        blocks(ECHO_CASING.get())
                            .or(autoAbilities(*definition.recipeTypes))
                            .or(abilities(MAINTENANCE).setExactLimit(1))
                            .or(abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1))
                    )
                    .build()
            }
            .additionalDisplay { controller: IMultiController?, components: MutableList<Component?>? ->
                if (controller!!.isFormed) {
                    components!!.add(
                        "gtceu.multiblock.parallel".toComponent(
                            (FormattingUtil.formatNumbers(2.0.pow(((controller as WorkableElectricMultiblockMachine).getTier() - 6).toDouble()))).literal
                                .withStyle(ChatFormatting.DARK_PURPLE)
                        ).withStyle(ChatFormatting.GRAY)
                    )
                }
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/echo_casing"),
                GTCEu.id("block/multiblock/cleanroom")
            )
            .register()

        BIOLOGICAL_SIMULATION_LABORATORY = REGISTRATE.multiblock(
            "biological_simulation_laboratory",
            Function { BiologicalSimulationLaboratory(it) }
        )
            .allRotation()
            .tooltipTextKey("gtceu.multiblock.biological_simulation_laboratory.tooltip.0".toComponent)
            .tooltipTextKey("gtceu.multiblock.biological_simulation_laboratory.tooltip.1".toComponent)
            .tooltipTextKey("gtceu.multiblock.biological_simulation_laboratory.tooltip.2".toComponent)
            .tooltipTextKey("gtceu.multiblock.biological_simulation_laboratory.tooltip.3".toComponent)
            .tooltipTextKey("gtceu.multiblock.biological_simulation_laboratory.tooltip.4".toComponent)
            .tooltipTextKey("gtceu.multiblock.biological_simulation_laboratory.tooltip.5".toComponent)
            .tooltipTextKey("gtceu.multiblock.biological_simulation_laboratory.tooltip.6".toComponent)
            .tooltipTextKey("gtceu.multiblock.biological_simulation_laboratory.tooltip.7".toComponent)
            .tooltipTextKey("gtceu.multiblock.biological_simulation_laboratory.tooltip.8".toComponent)
            .tooltipTextKey("gtceu.multiblock.biological_simulation_laboratory.tooltip.9".toComponent)
            .tooltipTextRecipeTypes(BIOLOGICAL_SIMULATION)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(BIOLOGICAL_SIMULATION)
            .appearanceBlock(NAQUADAH_ALLOY_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.BIOLOGICAL_SIMULATION_LABORATORY_STRUCTURE!!
                    .where("~", controller(blocks(definition!!.get())))
                    .where(
                        "A",
                        blocks(NAQUADAH_ALLOY_CASING.get())
                            .or(autoAbilities(*definition.recipeTypes))
                            .or(blocks(*INPUT_LASER.getBlockRange(12, 14).toTypedArray<Block?>()).setMaxGlobalLimited(1))
                            .or(abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1))
                    )
                    .where("B", blocks(ChemicalHelper.getBlock(frameGt, NaquadahAlloy)))
                    .where("C", blocks(HERMETIC_CASING_LuV.get()))
                    .where("E", blocks(FUSION_GLASS.get()))
                    .where("G", blocks(COMPUTER_HEAT_VENT.get()))
                    .where("D", blocks(ADVANCED_COMPUTER_CASING.get()))
                    .where("H", blocks(FILTER_CASING_STERILE.get()))
                    .where("F", blocks(HERMETIC_CASING_ZPM.get()))
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/hyper_mechanical_casing"),
                GTCEu.id("block/multiblock/fusion_reactor")
            )
            .register()

        DIMENSIONALLY_TRANSCENDENT_CHEMICAL_PLANT = REGISTRATE.multiblock(
            "dimensionally_transcendent_chemical_plant",
            Function { DimensionallyTranscendentChemicalPlant(it) }
        )
            .nonYAxisRotation()
            .tooltipTextKey("gtceu.multiblock.dimensionally_transcendent_chemical_plant".toComponent)
            .tooltipTextCoilParallel()
            .tooltipTextLaser()
            .tooltipTextMultiRecipes()
            .tooltipTextRecipeTypes(LARGE_CHEMICAL_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(LARGE_CHEMICAL_RECIPES)
            .appearanceBlock(CASING_PTFE_INERT)
            .pattern { definition: MultiblockMachineDefinition? ->
                GTLMachines.DTPF
                    .where("a", controller(blocks(definition!!.get())))
                    .where(
                        "e",
                        blocks(CASING_PTFE_INERT.get())
                            .or(abilities(MAINTENANCE).setExactLimit(1))
                            .or(abilities(EXPORT_ITEMS).setPreviewCount(1))
                            .or(abilities(IMPORT_ITEMS).setPreviewCount(1))
                            .or(abilities(EXPORT_FLUIDS).setPreviewCount(1))
                            .or(abilities(IMPORT_FLUIDS).setPreviewCount(1))
                            .or(abilities(INPUT_ENERGY).setMaxGlobalLimited(2))
                            .or(abilities(INPUT_LASER).setMaxGlobalLimited(1))
                            .or(abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1))
                    )
                    .where("b", blocks(HIGH_POWER_CASING.get()))
                    .where("C", heatingCoils())
                    .where("d", blocks(CASING_PTFE_INERT.get()))
                    .where("s", blocks(getBlock("gtceu:ptfe_pipe_casing")))
                    .where(" ", any())
                    .build()
            }
            .workableCasingRenderer(
                GTCEu.id("block/casings/solid/machine_casing_inert_ptfe"),
                GTCEu.id("block/machines/chemical_reactor")
            )
            .register()

        QUANTUM_SYPHON_MATRIX = REGISTRATE.multiblock(
            "quantum_syphon_matrix",
            Function { GTLAddWorkableElectricParallelHatchMultipleRecipesMachine(it) }
        )
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
                    .where("~", controller(blocks(definition!!.get())))
                    .where("C", blocks(ChemicalHelper.getBlock(frameGt, Neutronium)))
                    .where("G", blocks(getBlock("kubejs:accelerated_pipeline")))
                    .where("D", blocks(MOLECULAR_CASING.get()))
                    .where("H", blocks(getBlock("kubejs:neutronium_gearbox")))
                    .where(
                        "F",
                        blocks(HIGH_POWER_CASING.get())
                            .or(autoAbilities(*definition.recipeTypes))
                            .or(abilities(MAINTENANCE).setExactLimit(1))
                            .or(abilities(INPUT_LASER).setMaxGlobalLimited(1))
                            .or(abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1))
                            .or(abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1))
                    )
                    .where("J", blocks(getBlock("kubejs:neutronium_pipe_casing")))
                    .where("A", blocks(NAQUADAH_ALLOY_CASING.get()))
                    .where("B", blocks(getBlock("gtceu:assembly_line_grating")))
                    .where("I", blocks(HERMETIC_CASING_UHV.get()))
                    .where("E", blocks(getBlock("kubejs:hollow_casing")))
                    .where(" ", any())
                    .build()
            }
            .workableCasingRenderer(
                GTCEu.id("block/casings/hpca/high_power_casing"),
                GTCEu.id("block/machines/gas_collector")
            )
            .register()

        FUXI_BAGUA_HEAVEN_FORGING_FURNACE = REGISTRATE.multiblock(
            "fuxi_bagua_heaven_forging_furnace",
            Function { GTLAddCoilWorkableElectricParallelHatchMultipleRecipesMachine(it) }
        )
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
                    .where("D", controller(blocks(definition!!.get())))
                    .where("K", blocks(getBlock("kubejs:neutronium_pipe_casing")))
                    .where(
                        "C",
                        blocks(DIMENSION_INJECTION_CASING.get())
                            .or(abilities(EXPORT_ITEMS).setPreviewCount(1))
                            .or(abilities(IMPORT_ITEMS).setPreviewCount(1))
                            .or(abilities(EXPORT_FLUIDS).setPreviewCount(1))
                            .or(abilities(IMPORT_FLUIDS).setPreviewCount(1))
                            .or(abilities(INPUT_LASER).setMaxGlobalLimited(2))
                            .or(abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1))
                            .or(abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1))
                    )
                    .where("X", heatingCoils())
                    .where("J", blocks(getBlock("kubejs:dimensional_bridge_casing")))
                    .where("F", blocks(GRAVITON_FIELD_CONSTRAINT_CASING.get()))
                    .where("I", blocks(getBlock("kubejs:molecular_coil")))
                    .where("A", blocks(getBlock("gtceu:atomic_casing")))
                    .where("G", blocks(DEGENERATE_RHENIUM_CONSTRAINED_CASING.get()))
                    .where("N", blocks(ULTIMATE_STELLAR_CONTAINMENT_CASING.get()))
                    .where("B", blocks(DIMENSION_INJECTION_CASING.get()))
                    .where("E", blocks(getBlock("kubejs:dimension_creation_casing")))
                    .where("H", blocks(getBlock("kubejs:spacetime_compression_field_generator")))
                    .where("L", blocks(COMPRESSED_FUSION_COIL_MK2_PROTOTYPE.get()))
                    .where("M", blocks(getBlock("kubejs:dimensional_stability_casing")))
                    .where("O", blocks(getBlock("kubejs:restraint_device")))
                    .build()
            }
            .additionalDisplay { controller: IMultiController?, components: MutableList<Component?>? ->
                if (controller is GTLAddCoilWorkableElectricParallelHatchMultipleRecipesMachine) {
                    if (controller.isFormed()) {
                        components!!.add(
                            "gtceu.multiblock.blast_furnace.max_temperature".toComponent(
                                (FormattingUtil.formatNumbers(controller.coilType.coilTemperature) + "K").toComponent
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

        ANTIENTROPY_CONDENSATION_CENTER = REGISTRATE.multiblock(
            "antientropy_condensation_center",
            Function { AntientropyCondensationCenter(it) }
        )
            .allRotation()
            .tooltipTextKey("gtceu.multiblock.antientropy_condensation_center.0".toComponent)
            .tooltipTextKey("gtceu.multiblock.antientropy_condensation_center.1".toComponent)
            .tooltipTextKey("gtceu.multiblock.antientropy_condensation_center.2".toComponent)
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
                    .where("B", controller(blocks(definition!!.get())))
                    .where("C", blocks(MOLECULAR_CASING.get()))
                    .where("K", blocks(ChemicalHelper.getBlock(frameGt, Mithril)))
                    .where("D", blocks(HERMETIC_CASING_UXV.get()))
                    .where("M", blocks(getBlock("kubejs:containment_field_generator")))
                    .where("J", blocks(getBlock("kubejs:force_field_glass")))
                    .where("I", blocks(getBlock("kubejs:dimensional_bridge_casing")))
                    .where("A", blocks(ANTIFREEZE_HEATPROOF_MACHINE_CASING.get()))
                    .where(
                        "X",
                        blocks(ANTIFREEZE_HEATPROOF_MACHINE_CASING.get())
                            .or(abilities(EXPORT_ITEMS).setPreviewCount(1))
                            .or(abilities(IMPORT_ITEMS).setPreviewCount(1))
                            .or(abilities(EXPORT_FLUIDS).setPreviewCount(1))
                            .or(abilities(IMPORT_FLUIDS).setPreviewCount(1))
                            .or(abilities(INPUT_LASER).setMaxGlobalLimited(2))
                            .or(abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1))
                            .or(abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1))
                    )
                    .where("F", blocks(COMPRESSED_FUSION_COIL_MK2.get()))
                    .where("G", blocks(getBlock("gtlcore:law_filter_casing")))
                    .where("H", blocks(getBlock("kubejs:hollow_casing")))
                    .where("E", blocks(DIMENSIONALLY_TRANSCENDENT_CASING.get()))
                    .where("L", blocks(DIMENSION_INJECTION_CASING.get()))
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/antifreeze_heatproof_machine_casing"),
                GTCEu.id("block/multiblock/vacuum_freezer")
            )
            .register()

        TAIXU_TURBID_ARRAY = REGISTRATE.multiblock(
            "taixu_turbid_array",
            Function { TaixuTurbidArray(it) }
        )
            .tooltipTextKey("gtceu.machine.taixuturbidarray.tooltip.0".toComponent)
            .tooltipTextKey("gtceu.machine.taixuturbidarray.tooltip.1".toComponent)
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist.tooltip.3".toComponent)
            .tooltipTextKey("gtceu.machine.taixuturbidarray.tooltip.12".toComponent)
            .tooltipTextKey("gtceu.machine.taixuturbidarray.tooltip.2".toComponent)
            .tooltipTextKey("gtceu.machine.taixuturbidarray.tooltip.3".toComponent)
            .tooltipTextKey("gtceu.machine.taixuturbidarray.tooltip.13".toComponent)
            .tooltipTextKey("gtceu.machine.taixuturbidarray.tooltip.4".toComponent)
            .tooltipTextKey("gtceu.machine.taixuturbidarray.tooltip.5".toComponent)
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist.tooltip.3".toComponent)
            .tooltipTextKey("gtceu.machine.taixuturbidarray.tooltip.6".toComponent)
            .tooltipTextKey("gtceu.machine.taixuturbidarray.tooltip.8".toComponent)
            .tooltipTextKey("gtceu.machine.taixuturbidarray.tooltip.9".toComponent)
            .tooltipTextKey("gtceu.machine.taixuturbidarray.tooltip.14".toComponent(5))
            .tooltipTextKey("gtceu.machine.taixuturbidarray.tooltip.15".toComponent)
            .tooltipTextKey("gtceu.machine.taixuturbidarray.tooltip.7".toComponent)
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist.tooltip.3".toComponent)
            .tooltipTextKey("gtceu.machine.taixuturbidarray.tooltip.16".toComponent)
            .tooltipTextKey("gtceu.machine.taixuturbidarray.tooltip.17".toComponent)
            .tooltipTextKey("gtceu.machine.taixuturbidarray.tooltip.18".toComponent)
            .tooltipTextKey("gtceu.machine.eut_multiplier.tooltip".toComponent(16))
            .tooltipTextKey("gtceu.machine.taixuturbidarray.tooltip.14".toComponent(1))
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist.tooltip.3".toComponent)
            .tooltipTextKey("gtceu.machine.taixuturbidarray.tooltip.11".toComponent)
            .tooltipTextKey("gtceu.machine.taixuturbidarray.tooltip.10".toComponent)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .rotationState(RotationState.Y_AXIS)
            .recipeType(CHAOS_WEAVE)
            .recipeModifier { machine: MetaMachine?, recipe: GTRecipe?, _: OCParams?, _: OCResult? ->
                TaixuTurbidArray.recipeModifier(machine!!, recipe!!)
            }
            .appearanceBlock(MACHINE_CASING_UHV)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.TAIXU_TURBID_ARRAY_STRUCTURE!!
                    .where("T", controller(blocks(definition!!.get())))
                    .where(
                        "K",
                        blocks(MACHINE_CASING_UHV.get())
                            .or(abilities(INPUT_LASER).setPreviewCount(1))
                            .or(abilities(IMPORT_ITEMS).setPreviewCount(1))
                            .or(abilities(EXPORT_ITEMS).setPreviewCount(1))
                            .or(abilities(EXPORT_FLUIDS).setPreviewCount(1))
                    )
                    .where("H", blocks(MACHINE_CASING_UHV.get()))
                    .where("E", blocks(getBlock("gtceu:woods_glass_block")))
                    .where("J", blocks(DIMENSION_INJECTION_CASING.get()))
                    .where("B", blocks(DIMENSIONALLY_TRANSCENDENT_CASING.get()))
                    .where("R", blocks(getBlock("kubejs:force_field_glass")))
                    .where("S", GTLPredicates.countBlock("SpeedPipe", getBlock("kubejs:speeding_pipe")))
                    .where("G", blocks(getBlock("kubejs:hollow_casing")))
                    .where("F", blocks(ChemicalHelper.getBlock(frameGt, NaquadahAlloy)))
                    .where("N", blocks(FUSION_CASING_MK5.get()))
                    .where("I", blocks(SPS_CASING.get()))
                    .where("P", blocks(FUSION_GLASS.get()))
                    .where("M", GTLPredicates.tierCasings(BlockMap.scMap, "SCTier"))
                    .where("A", blocks(IRIDIUM_CASING.get()))
                    .where("L", blocks(getBlock("kubejs:containment_field_generator")))
                    .where("Q", blocks(getBlock("kubejs:dimensional_bridge_casing")))
                    .where("C", blocks(getBlock("gtceu:atomic_casing")))
                    .where("D", blocks(ChemicalHelper.getBlock(frameGt, Mithril)))
                    .where("O", heatingCoils())
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

                StructureSlicer.sliceAndInsert(arrays, 10, 12, 11, 1, 16).forEach {
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

        PLANETARY_IONISATION_CONVERGENCE_TOWER = REGISTRATE.multiblock(
            "planetary_ionisation_convergence_tower",
            Function { PlanetaryIonisationConvergenceTower(it) }
        )
            .nonYAxisRotation()
            .tooltipTextKey("gtceu.machine.hold_g.tooltip.0".toComponent)
            .tooltipTextKey("gtceu.machine.hold_g.tooltip.1".toComponent)
            .tooltipOnlyTextLaser()
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(DUMMY_RECIPES)
            .generator(true)
            .appearanceBlock(SPS_CASING)
            .pattern { definition ->
                MultiBlockStructure.PLANETARY_IONISATION_CONVERGENCE_TOWER
                    .where("Z", controller(blocks(definition!!.get())))
                    .where(
                        "I",
                        blocks(SPS_CASING.get())
                            .or(abilities(IMPORT_FLUIDS).setMaxGlobalLimited(1))
                            .or(abilities(PartAbility.OUTPUT_LASER).setExactLimit(1))
                    )
                    .where("U", blocks(PLASTCRETE.get()))
                    .where("G", blocks(GCyMBlocks.ELECTROLYTIC_CELL.get()))
                    .where("N", blocks(RHENIUM_REINFORCED_ENERGY_GLASS.get()))
                    .where("H", GTLAddPredicates.slabBlock(SlabType.BOTTOM, Blocks.POLISHED_DEEPSLATE_SLAB))
                    .where("O", blocks(Blocks.POLISHED_DEEPSLATE_WALL))
                    .where("P", blocks(CASING_EXTREME_ENGINE_INTAKE.get()))
                    .where("R", blocks(FUSION_GLASS.get()))
                    .where("X", blocks(SUPERCONDUCTING_COIL.get()))
                    .where("M", GTLPredicates.tierCasings(BlockMap.scMap, "SCTier"))
                    .where("B", blocks(ENHANCE_HYPER_MECHANICAL_CASING.get()))
                    .where("W", blocks(ChemicalHelper.getBlock(frameGt, NaquadahAlloy)))
                    .where("C", blocks(HIGH_POWER_CASING.get()))
                    .where("Q", GTLAddPredicates.heatingCoils(14400))
                    .where("T", blocks(AEBlocks.QUARTZ_VIBRANT_GLASS.block()))
                    .where("D", blocks(HYPER_MECHANICAL_CASING.get()))
                    .where("F", blocks(ChemicalHelper.getBlock(frameGt, Mithril)))
                    .where("J", blocks(OXIDATION_RESISTANT_HASTELLOY_N_MECHANICAL_CASING.get()))
                    .where("L", blocks(Blocks.IRON_TRAPDOOR))
                    .where("K", blocks(HEAT_VENT.get()))
                    .where("E", blocks(MOLECULAR_CASING.get()))
                    .where("V", blocks(CASING_PTFE_INERT.get()))
                    .where("S", blocks(HSSS_REINFORCED_BOROSILICATE_GLASS.get()))
                    .where("[", blocks(Blocks.BEACON))
                    .where("a", blocks(CLEANROOM_GLASS.get()))
                    .where("Y", blocks(Blocks.IRON_BLOCK))
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/sps_casing"),
                GTCEu.id("block/multiblock/gcym/large_assembler")
            )
            .register()

        INFERNO_CLEFT_SMELTING_VAULT = REGISTRATE.multiblock(
            "inferno_cleft_smelting_vault",
            Function { GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(it) }
        )
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
                    .where("L", controller(blocks(definition!!.get())))
                    .where(
                        "I",
                        blocks(IRIDIUM_CASING.get())
                            .or(abilities(EXPORT_ITEMS).setPreviewCount(1))
                            .or(abilities(IMPORT_ITEMS).setPreviewCount(1))
                            .or(abilities(EXPORT_FLUIDS).setPreviewCount(1))
                            .or(abilities(IMPORT_FLUIDS).setPreviewCount(1))
                            .or(abilities(INPUT_ENERGY).setMaxGlobalLimited(2))
                            .or(abilities(INPUT_LASER).setMaxGlobalLimited(1))
                            .or(abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1))
                    )
                    .where("M", blocks(getBlock("gtceu:uv_muffler_hatch")))
                    .where("G", heatingCoils())
                    .where("H", heatingCoils())
                    .where("B", blocks(IRIDIUM_CASING.get()))
                    .where("A", blocks(MOLECULAR_CASING.get()))
                    .where("J", blocks(HERMETIC_CASING_LuV.get()))
                    .where("C", blocks(HYPER_MECHANICAL_CASING.get()))
                    .where("E", blocks(getBlock("kubejs:neutronium_pipe_casing")))
                    .where("K", blocks(HYPER_CORE.get()))
                    .where("D", blocks(getBlock("gtceu:high_temperature_smelting_casing")))
                    .where("F", blocks(FUSION_GLASS.get()))
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/iridium_casing"),
                GTCEu.id("block/multiblock/pyrolyse_oven")
            )
            .register()

        SKELETON_SHIFT_RIFT_ENGINE = REGISTRATE.multiblock(
            "skeleton_shift_rift_engine",
            Function { SkeletonShiftRiftEngine(it) }
        )
            .nonYAxisRotation()
            .tooltipTextKey("gtceu.multiblock.skeleton_shift_rift_engine.0".toComponent)
            .tooltipTextKey("gtceu.multiblock.skeleton_shift_rift_engine.1".toComponent)
            .tooltipTextLaser()
            .tooltipTextPerfectOverclock()
            .tooltipTextRecipeTypes(DECAY_HASTENER_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeTypes(DECAY_HASTENER_RECIPES)
            .recipeModifier(SkeletonShiftRiftEngine::recipeModifier)
            .appearanceBlock(HYPER_MECHANICAL_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.SKELETON_SHIFT_RIFT_ENGINE!!
                    .where("Q", controller(blocks(definition!!.get())))
                    .where("P", GTLPredicates.tierCasings(BlockMap.scMap, "SCTier"))
                    .where("E", blocks(ChemicalHelper.getBlock(frameGt, BlackSteel)))
                    .where("B", blocks(HIGH_POWER_CASING.get()))
                    .where("D", blocks(SPS_CASING.get()))
                    .where("J", blocks(getBlock("gtceu:steel_pipe_casing")))
                    .where("A", blocks(IRIDIUM_CASING.get()))
                    .where("M", blocks(getBlock("gtceu:tungstensteel_gearbox")))
                    .where("H", blocks(HYPER_MECHANICAL_CASING.get()))
                    .where(
                        "h",
                        blocks(HYPER_MECHANICAL_CASING.get())
                            .or(abilities(EXPORT_ITEMS).setPreviewCount(1))
                            .or(abilities(IMPORT_ITEMS).setPreviewCount(1))
                            .or(abilities(EXPORT_FLUIDS).setPreviewCount(1))
                            .or(abilities(IMPORT_FLUIDS).setPreviewCount(1))
                            .or(abilities(INPUT_ENERGY).setMaxGlobalLimited(2))
                            .or(abilities(INPUT_LASER).setMaxGlobalLimited(1))
                            .or(abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1))
                    )
                    .where("O", blocks(DEGENERATE_RHENIUM_CONSTRAINED_CASING.get()))
                    .where("F", blocks(getBlock("kubejs:neutronium_pipe_casing")))
                    .where("I", blocks(getBlock("kubejs:dimensional_bridge_casing")))
                    .where("N", heatingCoils())
                    .where("G", blocks(DIMENSIONALLY_TRANSCENDENT_CASING.get()))
                    .where("C", blocks(ENHANCE_HYPER_MECHANICAL_CASING.get()))
                    .where("K", blocks(HYPER_CORE.get()))
                    .where("L", blocks(FUSION_GLASS.get()))
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/hyper_mechanical_casing"),
                GTCEu.id("block/multiblock/fusion_reactor")
            )
            .register()

        TIME_SPACE_DISTORTER = REGISTRATE.multiblock("time_space_distorter", Function { TimeSpaceDistorter(it) })
            .nonYAxisRotation()
            .tooltipTextKey("gtceu.multiblock.time_space_distorter.tooltip.0".toComponent)
            .tooltipTextKey("gtceu.multiblock.time_space_distorter.tooltip.1".toComponent)
            .tooltipTextKey("gtladditions.multiblock.time_space_distorter.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.time_space_distorter.tooltip.1".toComponent)
            .tooltipTextRecipeTypes(GTLAddRecipesTypes.TIME_SPACE_DISTORTION)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(GTLAddRecipesTypes.TIME_SPACE_DISTORTION)
            .recipeModifier(GTRecipeModifiers.ELECTRIC_OVERCLOCK.apply(OverclockingLogic(1.0, 1.0, false)))
            .appearanceBlock(DIMENSION_INJECTION_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.TIME_SPACE_DISTORTER_STRUCTURE
                    .where("~", controller(blocks(definition!!.get())))
                    .where(
                        "B",
                        blocks(DIMENSION_INJECTION_CASING.get())
                            .or(abilities(IMPORT_ITEMS))
                            .or(abilities(IMPORT_FLUIDS))
                            .or(abilities(INPUT_ENERGY))
                    )
                    .where("F", blocks(RHENIUM_REINFORCED_ENERGY_GLASS.get()))
                    .where("C", blocks(DIMENSION_INJECTION_CASING.get()))
                    .where("U", blocks(getBlock("kubejs:dyson_deployment_casing")))
                    .where("N", blocks(getBlock("kubejs:dimensional_bridge_casing")))
                    .where("V", blocks(MANIPULATOR.get()))
                    .where("G", blocks(ChemicalHelper.getBlock(frameGt, MagnetohydrodynamicallyConstrainedStarMatter)))
                    .where("E", blocks(DIMENSIONALLY_TRANSCENDENT_CASING.get()))
                    .where("I", blocks(getBlock("kubejs:force_field_glass")))
                    .where("X", blocks(getBlock("kubejs:dimensional_stability_casing")))
                    .where("A", blocks(CREATE_CASING.get()))
                    .where("S", blocks(Blocks.BEACON))
                    .where("H", blocks(ChemicalHelper.getBlock(frameGt, QuantumChromodynamicallyConfinedMatter)))
                    .where("Z", blocks(getBlock("kubejs:dimension_creation_casing")))
                    .where("W", blocks(HYPER_CORE.get()))
                    .where("M", blocks(getBlock("gtceu:atomic_casing")))
                    .where("Y", blocks(getBlock("kubejs:annihilate_core")))
                    .where("Q", blocks(Blocks.DIAMOND_BLOCK))
                    .where("P", blocks(ChemicalHelper.getBlock(frameGt, Infinity)))
                    .where("R", blocks(INFINITY_GLASS.get()))
                    .where("T", blocks(getBlock("kubejs:dyson_control_casing")))
                    .where("[", blocks(getBlock("kubejs:create_aggregatione_core")))
                    .where("L", blocks(getBlock("kubejs:eternity_coil_block")))
                    .where("O", blocks(ChemicalHelper.getBlock(frameGt, Eternity)))
                    .where("K", blocks(getBlock("kubejs:molecular_coil")))
                    .where("J", blocks(QFT_COIL.get()))
                    .where("D", blocks(getBlock("kubejs:uruium_coil_block")))
                    .build()
            }
            .renderer { TimeSpaceDistorterRenderer() }
            .hasTESR(true)
            .register()

        APOCALYPTIC_TORSION_QUANTUM_MATRIX = REGISTRATE.multiblock(
            "apocalyptic_torsion_quantum_matrix",
            Function { ApocalypticTorsionQuantumMatrix(it) }
        )
            .nonYAxisRotation()
            .tooltipTextKey("gtladditions.multiblock.apocalyptic_torsion_quantum_matrix.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.apocalyptic_torsion_quantum_matrix.tooltip.1".toComponent)
            .tooltipTextKey("gtladditions.multiblock.apocalyptic_torsion_quantum_matrix.tooltip.2".toComponent)
            .tooltipTextKey("gtladditions.multiblock.apocalyptic_torsion_quantum_matrix.tooltip.3".toComponent)
            .tooltipTextKey("gtladditions.multiblock.apocalyptic_torsion_quantum_matrix.tooltip.4".toComponent)
            .tooltipTextKey("gtladditions.multiblock.apocalyptic_torsion_quantum_matrix.tooltip.5".toComponent)
            .tooltipTextKey("gtceu.machine.eut_multiplier.tooltip".toComponent(0.2))
            .tooltipTextParallelHatch()
            .tooltipTextKey("gtladditions.multiblock.multiple_recipe_types_machine.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.1".toComponent)
            .tooltipTextKey("tooltip.gtlcore.structure.source".toComponent("TST"))
            .tooltipTextRecipeTypes(QFT_RECIPES, DISTORT_RECIPES, NEUTRON_COMPRESSOR_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeTypes(QFT_RECIPES, DISTORT_RECIPES, NEUTRON_COMPRESSOR_RECIPES)
            .appearanceBlock(DIMENSIONALLY_TRANSCENDENT_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.APOCALYPTIC_TORSION_QUANTUM_MATRIX!!
                    .where("~", controller(blocks(definition!!.get())))
                    .where("A", blocks(QFT_COIL.get()))
                    .where("B", blocks(MANIPULATOR.get()))
                    .where("C", blocks(getBlock("kubejs:dimensional_bridge_casing")))
                    .where("D", blocks(DIMENSIONALLY_TRANSCENDENT_CASING.get()))
                    .where("E", blocks(QUANTUM_GLASS.get()))
                    .where(
                        "F",
                        blocks(MANIPULATOR.get())
                            .or(abilities(IMPORT_ITEMS))
                            .or(abilities(IMPORT_FLUIDS))
                            .or(abilities(EXPORT_ITEMS))
                            .or(abilities(EXPORT_FLUIDS))
                            .or(abilities(MAINTENANCE).setExactLimit(1))
                            .or(abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1))
                            .or(abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1))
                    )
                    .where("G", blocks(SPACETIMECONTINUUMRIPPER.get()))
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
            .partAppearance { _, _, _ -> MANIPULATOR.get().defaultBlockState() }
            .register()

        FORGE_OF_THE_ANTICHRIST = REGISTRATE.multiblock(
            "forge_of_the_antichrist",
            Function { ForgeOfTheAntichrist(it) }
        )
            .nonYAxisRotation()
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist.tooltip.1".toComponent)
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist.tooltip.2".toComponent)
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist.tooltip.3".toComponent)
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist.tooltip.4".toComponent)
            .tooltipTextKey(
                "gtladditions.multiblock.forge_of_the_antichrist.tooltip.5".toComponent(
                    createObfuscatedDeleteComponent(
                        Long.MAX_VALUE.toString()
                    )
                )
            )
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist.tooltip.6".toComponent)
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist.tooltip.7".toComponent)
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist.tooltip.8".toComponent)
            .tooltipTextKey("gtceu.machine.eut_multiplier.tooltip".toComponent(0.2))
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist.tooltip.3".toComponent)
            .tooltipTextKey("gtladditions.multiblock.multiple_recipe_types_machine.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.1".toComponent)
            .tooltipTextKey("tooltip.gtlcore.structure.source".toComponent("GTNH"))
            .tooltipTextRecipeTypes(DIMENSIONALLY_TRANSCENDENT_PLASMA_FORGE_RECIPES, STELLAR_FORGE_RECIPES, ULTIMATE_MATERIAL_FORGE_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .appearanceBlock(GOD_FORGE_INNER_CASING)
            .recipeTypes(DIMENSIONALLY_TRANSCENDENT_PLASMA_FORGE_RECIPES, STELLAR_FORGE_RECIPES, ULTIMATE_MATERIAL_FORGE_RECIPES)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.FORGE_OF_THE_ANTICHRIST!!
                    .where("~", controller(blocks(definition!!.get())))
                    .where(
                        "A",
                        blocks(GOD_FORGE_SUPPORT_CASING.get())
                            .or(abilities(IMPORT_ITEMS))
                            .or(abilities(IMPORT_FLUIDS))
                            .or(abilities(EXPORT_ITEMS))
                            .or(abilities(EXPORT_FLUIDS))
                    )
                    .where("B", blocks(GOD_FORGE_TRIM_CASING.get()))
                    .where("C", blocks(GOD_FORGE_INNER_CASING.get()))
                    .where("D", blocks(GOD_FORGE_SUPPORT_CASING.get()))
                    .where("E", blocks(SUPRACHRONAL_MAGNETIC_CONFINEMENT_CASING.get()))
                    .where("F", blocks(GOD_FORGE_ENERGY_CASING.get()))
                    .where("G", blocks(REMOTE_GRAVITON_FLOW_REGULATOR.get()))
                    .where("H", blocks(SPATIALLY_TRANSCENDENT_GRAVITATIONAL_LENS.get()))
                    .where("I", blocks(CENTRAL_GRAVITON_FLOW_REGULATOR.get()))
                    .where(
                        "J",
                        blocks(GOD_FORGE_TRIM_CASING.get())
                            .or(blocks(HELIOFUSION_EXOTICIZER.get()))
                            .or(blocks(HELIOFLARE_POWER_FORGE.get()))
                            .or(blocks(HELIOFLUIX_MELTING_CORE.get()))
                            .or(blocks(HELIOTHERMAL_PLASMA_FABRICATOR.get()))
                            .or(blocks(HELIOPHASE_LEYLINE_CRYSTALLIZER.get()))
                    )
                    .where("K", blocks(MEDIARY_GRAVITON_FLOW_REGULATOR.get()))
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
            .partAppearance { _, _, _ -> GOD_FORGE_SUPPORT_CASING.get().defaultBlockState() }
            .hasTESR(true)
            .register()

        RECURSIVE_REVERSE_ARRAY = REGISTRATE.multiblock("recursive_reverse_array", Function { RecursiveReverseArray(it) })
            .noneRotation()
            .tooltipTextKey("gtladditions.multiblock.recursive_reverse_array.tooltip.1".toComponent)
            .tooltipTextKey("gtladditions.multiblock.recursive_reverse_array.tooltip.0".toComponent)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .appearanceBlock(CREATE_CASING)
            .pattern {
                MultiBlockStructure.RECURSIVE_REVERSE_FORGE_STRUCTURE
                    .where("V", controller(blocks(it.get())))
                    .where(
                        "B",
                        blocks(DIMENSION_INJECTION_CASING.get())
                            .or(abilities(EXPORT_ITEMS).setMaxGlobalLimited(2))
                            .or(abilities(IMPORT_ITEMS).setMaxGlobalLimited(1))
                            .or(abilities(EXPORT_FLUIDS).setMaxGlobalLimited(2))
                            .or(abilities(IMPORT_FLUIDS).setMaxGlobalLimited(1))
                            .or(abilities(INPUT_LASER).setMaxGlobalLimited(1))
                    )
                    .where("D", blocks(DIMENSION_INJECTION_CASING.get()))
                    .where("Q", blocks(HYPER_CORE.get()))
                    .where("A", blocks(CREATE_CASING.get()))
                    .where("L", blocks(getBlock("kubejs:dimension_creation_casing")))
                    .where("U", blocks(FUSION_GLASS.get()))
                    .where("G", blocks(getBlock("kubejs:annihilate_core")))
                    .where("C", blocks(TEMPORAL_ANCHOR_FIELD_CASING.get()))
                    .where("E", blocks(ADVANCED_FUSION_COIL.get()))
                    .where("J", blocks(FUSION_CASING_MK4.get()))
                    .where("O", blocks(GRAVITON_FIELD_CONSTRAINT_CASING.get()))
                    .where("M", blocks(DRAGON_STRENGTH_TRITANIUM_CASING.get()))
                    .where("P", blocks(IMPROVED_SUPERCONDUCTOR_COIL.get()))
                    .where("K", blocks(DEGENERATE_RHENIUM_CONSTRAINED_CASING.get()))
                    .where("F", blocks(ChemicalHelper.getBlock(frameGt, Infinity)))
                    .where("N", blocks(DIMENSIONALLY_TRANSCENDENT_CASING.get()))
                    .where("S", blocks(getBlock("kubejs:magic_core")))
                    .where("T", blocks(Blocks.CRYING_OBSIDIAN))
                    .where("H", blocks(DIMENSION_CONNECTION_CASING.get()))
                    .where("I", blocks(HIGH_POWER_CASING.get()))
                    .where("R", blocks(FUSION_CASING_MK5.get()))
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/create_casing"),
                GTCEu.id("block/multiblock/top/cosmos_simulation")
            )
            .register()

        CATALYTIC_CASCADE_ARRAY = REGISTRATE.multiblock("catalytic_cascade_array", Function { CatalyticCascadeArray(it) })
            .nonYAxisRotation()
            .tooltipTextKey("tooltip.gtladditions.recursive_reverse_array_module".toComponent)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .appearanceBlock(DIMENSION_INJECTION_CASING)
            .pattern {
                MultiBlockStructure.RECURSIVE_REVERSE_FORGE_MODULE_STRUCTURE
                    .where("F", controller(blocks(it.get())))
                    .where(
                        "B",
                        blocks(DIMENSION_INJECTION_CASING.get())
                            .or(blocks(GTLMachines.HUGE_FLUID_IMPORT_HATCH[1].get()).setExactLimit(1))
                            .or(blocks(GTLAddMachines.VIENTIANE_TRANSCRIPTION_NODE.get()).setExactLimit(1))
                    )
                    .where("C", blocks(DEGENERATE_RHENIUM_CONSTRAINED_CASING.get()))
                    .where("E", blocks(GRAVITON_FIELD_CONSTRAINT_CASING.get()))
                    .where("D", blocks(getBlock("gtceu:attuned_tengam_block")))
                    .where("A", blocks(FUSION_CASING_MK3.get()))
                    .build()
            }
            .workableCasingRenderer(GTLCore.id("block/casings/dimension_injection_casing"), GTCEu.id("block/multiblock/fusion_reactor"))
            .register()

        MAGNETORHEOLOGICAL_CONVERGENCE_CORE = REGISTRATE.multiblock("magnetorheological_convergence_core", Function { MagnetorheologicalConvergenceCore(it) })
            .nonYAxisRotation()
            .tooltipTextKey("tooltip.gtladditions.recursive_reverse_array_module".toComponent)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .appearanceBlock(DIMENSION_INJECTION_CASING)
            .pattern {
                MultiBlockStructure.RECURSIVE_REVERSE_FORGE_MODULE_STRUCTURE
                    .where("F", controller(blocks(it.get())))
                    .where(
                        "B",
                        blocks(DIMENSION_INJECTION_CASING.get())
                            .or(blocks(CustomMachines.HUGE_ITEM_IMPORT_BUS[0].get()).setExactLimit(2))
                            .or(blocks(GTLMachines.HUGE_FLUID_IMPORT_HATCH[1].get()).setExactLimit(1))
                            .or(abilities(IMPORT_ITEMS).setMaxGlobalLimited(3))
                    )
                    .where("C", blocks(DEGENERATE_RHENIUM_CONSTRAINED_CASING.get()))
                    .where("E", blocks(GRAVITON_FIELD_CONSTRAINT_CASING.get()))
                    .where("D", blocks(getBlock("gtceu:attuned_tengam_block")))
                    .where("A", blocks(FUSION_CASING_MK3.get()))
                    .build()
            }
            .workableCasingRenderer(GTLCore.id("block/casings/dimension_injection_casing"), GTCEu.id("block/multiblock/fusion_reactor"))
            .register()

        SPACETIME_STASIS_DEVICE = REGISTRATE.multiblock("spacetime_stasis_device", Function { SpacetimeStasisDevice(it) })
            .nonYAxisRotation()
            .tooltipTextKey("tooltip.gtladditions.recursive_reverse_array_module".toComponent)
            .tooltipTextRecipeTypes(GTLAddRecipesTypes.SPACETIME_STASIS)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(GTLAddRecipesTypes.SPACETIME_STASIS)
            .recipeModifier(GTRecipeModifiers.ELECTRIC_OVERCLOCK.apply(OverclockingLogic(1.0, 1.0, false)))
            .appearanceBlock(DIMENSION_INJECTION_CASING)
            .pattern {
                MultiBlockStructure.RECURSIVE_REVERSE_FORGE_MODULE_STRUCTURE
                    .where("F", controller(blocks(it.get())))
                    .where(
                        "B",
                        blocks(DIMENSION_INJECTION_CASING.get())
                            .or(abilities(IMPORT_FLUIDS))
                            .or(abilities(INPUT_ENERGY).setExactLimit(1))
                    )
                    .where("C", blocks(DEGENERATE_RHENIUM_CONSTRAINED_CASING.get()))
                    .where("E", blocks(GRAVITON_FIELD_CONSTRAINT_CASING.get()))
                    .where("D", blocks(getBlock("gtceu:attuned_tengam_block")))
                    .where("A", blocks(FUSION_CASING_MK3.get()))
                    .build()
            }
            .workableCasingRenderer(GTLCore.id("block/casings/dimension_injection_casing"), GTCEu.id("block/multiblock/fusion_reactor"))
            .register()

        SUPRATEMPORAL_BOOSTING_ENGINE = REGISTRATE.multiblock("supratemporal_boosting_engine", Function { SupratemporalBoostingEngine(it) })
            .nonYAxisRotation()
            .tooltipTextKey("tooltip.gtladditions.recursive_reverse_array_module".toComponent)
            .tooltipTextRecipeTypes(GTLAddRecipesTypes.SUPRATEMPORAL_BOOSTING)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(GTLAddRecipesTypes.SUPRATEMPORAL_BOOSTING)
            .recipeModifier(GTRecipeModifiers.ELECTRIC_OVERCLOCK.apply(OverclockingLogic(1.0, 1.0, false)))
            .appearanceBlock(DIMENSION_INJECTION_CASING)
            .pattern {
                MultiBlockStructure.RECURSIVE_REVERSE_FORGE_MODULE_STRUCTURE
                    .where("F", controller(blocks(it.get())))
                    .where(
                        "B",
                        blocks(DIMENSION_INJECTION_CASING.get())
                            .or(abilities(IMPORT_FLUIDS))
                            .or(abilities(IMPORT_ITEMS).setMaxGlobalLimited(1))
                            .or(abilities(INPUT_ENERGY).setExactLimit(1))
                            .or(blocks(GTLAddMachines.VIENTIANE_TRANSCRIPTION_NODE.get()).setExactLimit(1))
                    )
                    .where("C", blocks(DEGENERATE_RHENIUM_CONSTRAINED_CASING.get()))
                    .where("E", blocks(GRAVITON_FIELD_CONSTRAINT_CASING.get()))
                    .where("D", blocks(getBlock("gtceu:attuned_tengam_block")))
                    .where("A", blocks(FUSION_CASING_MK3.get()))
                    .build()
            }
            .workableCasingRenderer(GTLCore.id("block/casings/dimension_injection_casing"), GTCEu.id("block/multiblock/fusion_reactor"))
            .register()

        HELIOFUSION_EXOTICIZER = REGISTRATE.multiblock(
            "heliofusion_exoticizer",
            Function { HelioFusionExoticizer(it) }
        )
            .allRotation()
            .tooltipTextKey("gtladditions.multiblock.heliofusion_exoticizer.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.heliofusion_exoticizer.tooltip.1".toComponent)
            .tooltipTextKey("gtladditions.multiblock.heliofusion_exoticizer.tooltip.2".toComponent)
            .tooltipTextKey("gtladditions.multiblock.heliofusion_exoticizer.tooltip.3".toComponent)
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist_module.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist_module.tooltip.1".toComponent)
            .tooltipTextKey("gtceu.machine.eut_multiplier.tooltip".toComponent(0.5))
            .tooltipTextKey("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.1".toComponent)
            .tooltipTextKey("tooltip.gtlcore.structure.source".toComponent("GTNH"))
            .tooltipTextRecipeTypes(MATTER_EXOTIC)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeTypes(MATTER_EXOTIC)
            .appearanceBlock(GOD_FORGE_TRIM_CASING)
            .pattern { definition ->
                MultiBlockStructure.FORGE_OF_THE_ANTICHRIST_MODULE!!
                    .where("~", controller(blocks(definition.get())))
                    .where(
                        "B",
                        blocks(GOD_FORGE_TRIM_CASING.get())
                            .or(abilities(IMPORT_ITEMS))
                            .or(abilities(IMPORT_FLUIDS))
                            .or(abilities(EXPORT_ITEMS))
                            .or(abilities(EXPORT_FLUIDS))
                    )
                    .where("F", blocks(PHONON_CONDUIT.get()))
                    .where("G", blocks(GOD_FORGE_ENERGY_CASING.get()))
                    .where("D", blocks(SUPRACHRONAL_MAGNETIC_CONFINEMENT_CASING.get()))
                    .where("E", blocks(GOD_FORGE_SUPPORT_CASING.get()))
                    .build()
            }
            .workableCasingRenderer(GTLAdditions.id("block/casings/god_forge_trim_casing"), GTLAdditions.id("block/multiblock/heliofusion_exoticizer"))
            .register()

        HELIOFLARE_POWER_FORGE = REGISTRATE.multiblock(
            "helioflare_power_forge",
            Function { HelioflarePowerForge(it) }
        )
            .allRotation()
            .tooltipTextKey("gtladditions.multiblock.helioflare_power_forge.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.helioflare_power_forge.tooltip.1".toComponent)
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist.tooltip.8".toComponent)
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist_module.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist_module.tooltip.2".toComponent)
            .tooltipTextKey(
                "gtladditions.multiblock.forge_of_the_antichrist_module.tooltip.3".toComponent(ALLOY_BLAST_RECIPES.registryName.toLanguageKey().toComponent)
            )
            .tooltipTextKey("gtceu.machine.eut_multiplier.tooltip".toComponent(0.2))
            .tooltipTextKey("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.1".toComponent)
            .tooltipTextKey("tooltip.gtlcore.structure.source".toComponent("GTNH"))
            .tooltipTextRecipeTypes(FURNACE_RECIPES, BLAST_RECIPES, ALLOY_SMELTER_RECIPES, ALLOY_BLAST_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeTypes(FURNACE_RECIPES, BLAST_RECIPES, ALLOY_SMELTER_RECIPES, ALLOY_BLAST_RECIPES)
            .appearanceBlock(GOD_FORGE_TRIM_CASING)
            .pattern { definition ->
                MultiBlockStructure.FORGE_OF_THE_ANTICHRIST_MODULE!!
                    .where("~", controller(blocks(definition.get())))
                    .where(
                        "B",
                        blocks(GOD_FORGE_TRIM_CASING.get())
                            .or(abilities(IMPORT_ITEMS))
                            .or(abilities(IMPORT_FLUIDS))
                            .or(abilities(EXPORT_ITEMS))
                            .or(abilities(EXPORT_FLUIDS))
                    )
                    .where("F", blocks(PHONON_CONDUIT.get()))
                    .where("G", blocks(GOD_FORGE_ENERGY_CASING.get()))
                    .where("D", blocks(SUPRACHRONAL_MAGNETIC_CONFINEMENT_CASING.get()))
                    .where("E", blocks(GOD_FORGE_SUPPORT_CASING.get()))
                    .build()
            }
            .workableCasingRenderer(GTLAdditions.id("block/casings/god_forge_trim_casing"), GTLAdditions.id("block/multiblock/heliofusion_exoticizer"))
            .register()

        HELIOFLUIX_MELTING_CORE = REGISTRATE.multiblock(
            "heliofluix_melting_core",
            Function { HeliofluixMeltingCore(it) }
        )
            .allRotation()
            .tooltipTextKey("gtladditions.multiblock.heliofluix_melting_core.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.heliofluix_melting_core.tooltip.1".toComponent)
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist.tooltip.8".toComponent)
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist_module.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist_module.tooltip.2".toComponent)
            .tooltipTextKey(
                "gtladditions.multiblock.forge_of_the_antichrist_module.tooltip.3".toComponent(CHAOTIC_ALCHEMY.registryName.toLanguageKey().toComponent)
            )
            .tooltipTextKey("gtceu.machine.eut_multiplier.tooltip".toComponent(0.2))
            .tooltipTextKey("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.1".toComponent)
            .tooltipTextKey("tooltip.gtlcore.structure.source".toComponent("GTNH"))
            .tooltipTextRecipeTypes(CHAOTIC_ALCHEMY, MOLECULAR_DECONSTRUCTION)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeTypes(CHAOTIC_ALCHEMY, MOLECULAR_DECONSTRUCTION)
            .appearanceBlock(GOD_FORGE_TRIM_CASING)
            .pattern { definition ->
                MultiBlockStructure.FORGE_OF_THE_ANTICHRIST_MODULE!!
                    .where("~", controller(blocks(definition.get())))
                    .where(
                        "B",
                        blocks(GOD_FORGE_TRIM_CASING.get())
                            .or(abilities(IMPORT_ITEMS))
                            .or(abilities(IMPORT_FLUIDS))
                            .or(abilities(EXPORT_ITEMS))
                            .or(abilities(EXPORT_FLUIDS))
                    )
                    .where("F", blocks(PHONON_CONDUIT.get()))
                    .where("G", blocks(GOD_FORGE_ENERGY_CASING.get()))
                    .where("D", blocks(SUPRACHRONAL_MAGNETIC_CONFINEMENT_CASING.get()))
                    .where("E", blocks(GOD_FORGE_SUPPORT_CASING.get()))
                    .build()
            }
            .workableCasingRenderer(GTLAdditions.id("block/casings/god_forge_trim_casing"), GTLAdditions.id("block/multiblock/heliofusion_exoticizer"))
            .register()

        HELIOTHERMAL_PLASMA_FABRICATOR = REGISTRATE.multiblock(
            "heliothermal_plasma_fabricator",
            Function { HeliothermalPlasmaFabricator(it) }
        )
            .allRotation()
            .tooltipTextKey("gtladditions.multiblock.heliothermal_plasma_fabricator.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.heliothermal_plasma_fabricator.tooltip.1".toComponent)
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist.tooltip.8".toComponent)
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist_module.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist_module.tooltip.2".toComponent)
            .tooltipTextKey(
                "gtladditions.multiblock.forge_of_the_antichrist_module.tooltip.3".toComponent(
                    FUSION_RECIPES.registryName.toLanguageKey().toComponent
                        .append(", ".literal)
                        .append(SUPER_PARTICLE_COLLIDER_RECIPES.registryName.toLanguageKey().toComponent)
                )
            )
            .tooltipTextKey("gtceu.machine.eut_multiplier.tooltip".toComponent(0.2))
            .tooltipTextKey("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.1".toComponent)
            .tooltipTextKey("tooltip.gtlcore.structure.source".toComponent("GTNH"))
            .tooltipTextRecipeTypes(STELLAR_LGNITION, FUSION_RECIPES, SUPER_PARTICLE_COLLIDER_RECIPES)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeTypes(STELLAR_LGNITION, FUSION_RECIPES, SUPER_PARTICLE_COLLIDER_RECIPES)
            .appearanceBlock(GOD_FORGE_TRIM_CASING)
            .pattern { definition ->
                MultiBlockStructure.FORGE_OF_THE_ANTICHRIST_MODULE!!
                    .where("~", controller(blocks(definition.get())))
                    .where(
                        "B",
                        blocks(GOD_FORGE_TRIM_CASING.get())
                            .or(abilities(IMPORT_ITEMS))
                            .or(abilities(IMPORT_FLUIDS))
                            .or(abilities(EXPORT_ITEMS))
                            .or(abilities(EXPORT_FLUIDS))
                    )
                    .where("F", blocks(PHONON_CONDUIT.get()))
                    .where("G", blocks(GOD_FORGE_ENERGY_CASING.get()))
                    .where("D", blocks(SUPRACHRONAL_MAGNETIC_CONFINEMENT_CASING.get()))
                    .where("E", blocks(GOD_FORGE_SUPPORT_CASING.get()))
                    .build()
            }
            .workableCasingRenderer(GTLAdditions.id("block/casings/god_forge_trim_casing"), GTLAdditions.id("block/multiblock/heliofusion_exoticizer"))
            .register()

        HELIOPHASE_LEYLINE_CRYSTALLIZER = REGISTRATE.multiblock(
            "heliophase_leyline_crystallizer",
            Function { HeliophaseLeylineCrystallizer(it) }
        )
            .allRotation()
            .tooltipTextKey("gtladditions.multiblock.heliophase_leyline_crystallizer.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.heliophase_leyline_crystallizer.tooltip.1".toComponent)
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist.tooltip.8".toComponent)
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist_module.tooltip.4".toComponent)
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist_module.tooltip.2".toComponent)
            .tooltipTextKey("gtceu.machine.eut_multiplier.tooltip".toComponent(256))
            .tooltipTextKey("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.wireless_multiple_recipes_machine.tooltip.1".toComponent)
            .tooltipTextKey("tooltip.gtlcore.structure.source".toComponent("GTNH"))
            .tooltipTextRecipeTypes(LEYLINE_CRYSTALLIZE)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeTypes(LEYLINE_CRYSTALLIZE)
            .appearanceBlock(GOD_FORGE_TRIM_CASING)
            .pattern { definition ->
                MultiBlockStructure.FORGE_OF_THE_ANTICHRIST_MODULE!!
                    .where("~", controller(blocks(definition.get())))
                    .where(
                        "B",
                        blocks(GOD_FORGE_TRIM_CASING.get())
                            .or(abilities(IMPORT_ITEMS))
                            .or(abilities(IMPORT_FLUIDS))
                            .or(abilities(EXPORT_ITEMS))
                            .or(abilities(EXPORT_FLUIDS))
                    )
                    .where("F", blocks(PHONON_CONDUIT.get()))
                    .where("G", blocks(GOD_FORGE_ENERGY_CASING.get()))
                    .where("D", blocks(SUPRACHRONAL_MAGNETIC_CONFINEMENT_CASING.get()))
                    .where("E", blocks(GOD_FORGE_SUPPORT_CASING.get()))
                    .build()
            }
            .workableCasingRenderer(GTLAdditions.id("block/casings/god_forge_trim_casing"), GTLAdditions.id("block/multiblock/heliofusion_exoticizer"))
            .register()

        HEART_OF_THE_UNIVERSE = REGISTRATE.multiblock(
            "heart_of_the_universe",
            Function { HeartOfTheUniverse(it) }
        )
            .nonYAxisRotation()
            .tooltipTextKey("gtladditions.multiblock.heart_of_the_universe.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.heart_of_the_universe.tooltip.1".toComponent)
            .tooltipTextKey("gtladditions.multiblock.heart_of_the_universe.tooltip.2".toComponent)
            .tooltipTextKey("gtladditions.multiblock.heart_of_the_universe.tooltip.3".toComponent)
            .tooltipTextKey("gtladditions.multiblock.heart_of_the_universe.tooltip.4".toComponent)
            .tooltipTextKey("gtladditions.multiblock.heart_of_the_universe.tooltip.5".toComponent)
            .tooltipTextKey("gtladditions.multiblock.heart_of_the_universe.tooltip.6".toComponent)
            .tooltipTextRecipeTypes(GENESIS_ENGINE)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeTypes(GENESIS_ENGINE)
            .appearanceBlock(DIMENSION_INJECTION_CASING)
            .pattern { definition ->
                MultiBlockStructure.ANNIHILATE_GENERATOR_STRUCTURE!!
                    .where("~", controller(blocks(definition.get())))
                    .where("A", blocks(GRAVITON_FIELD_CONSTRAINT_CASING.get()))
                    .where("B", blocks(getBlock("kubejs:annihilate_core")))
                    .where("C", blocks(HYPER_MECHANICAL_CASING.get()))
                    .where("D", blocks(getBlock("kubejs:dimensional_stability_casing")))
                    .where("E", blocks(TEMPORAL_ANCHOR_FIELD_CASING.get()))
                    .where("F", blocks(FUSION_GLASS.get()))
                    .where("G", blocks(getBlock("kubejs:annihilate_core")))
                    .where("H", blocks(RHENIUM_REINFORCED_ENERGY_GLASS.get()))
                    .where("P", blocks(DIMENSION_CONNECTION_CASING.get()))
                    .where(
                        "S",
                        blocks(DIMENSION_INJECTION_CASING.get())
                            .or(blocks(Wireless_Energy_Network_OUTPUT_Terminal.get()))
                            .or(abilities(IMPORT_ITEMS))
                            .or(abilities(EXPORT_ITEMS))
                            .or(abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1))
                    )
                    .where("T", blocks(CREATE_CASING.get()))
                    .where("R", blocks(getBlock("kubejs:dyson_deployment_magnet")))
                    .where(" ", any())
                    .build()
            }
            .renderer { HeartOfTheUniverseRenderer() }
            .hasTESR(true)
            .register()

        DIMENSION_FOCUS_INFINITY_CRAFTING_ARRAY = REGISTRATE.multiblock(
            "dimension_focus_infinity_crafting_array",
            Function { DimensionFocusInfinityCraftingArray(it) }
        )
            .nonYAxisRotation()
            .tooltipTextMaxParallels(4096.toString())
            .tooltipTextLaser()
            .tooltipTextMultiRecipes()
            .tooltipTextKey("gtladditions.multiblock.dimension_focus_infinity_crafting_array.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.dimension_focus_infinity_crafting_array.tooltip.1".toComponent)
            .tooltipTextKey("tooltip.gtlcore.structure.source".toComponent("TST"))
            .tooltipTextRecipeTypes(NIGHTMARE_CRAFTING)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(NIGHTMARE_CRAFTING)
            .appearanceBlock(TEMPORAL_ANCHOR_FIELD_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.DIMENSION_FOCUS_INFINITY_CRAFTING_ARRAY!!
                    .where("~", controller(blocks(definition!!.get())))
                    .where("N", blocks(getBlock("gtceu:assembly_line_grating")))
                    .where("O", blocks(getBlock("gtceu:assembly_line_casing")))
                    .where("I", blocks(getBlock("gtceu:trinium_frame")))
                    .where("L", blocks(getBlock("gtlcore:graviton_field_constraint_casing")))
                    .where("B", blocks(getBlock("gtlcore:iridium_casing")))
                    .where("F", blocks(getBlock("gtceu:high_power_casing")))
                    .where("H", blocks(getBlock("gtceu:superconducting_coil")))
                    .where("E", blocks(getBlock("gtlcore:advanced_assembly_line_unit")))
                    .where("D", blocks(getBlock("gtladditions:temporal_anchor_field_casing")))
                    .where(
                        "S",
                        blocks(getBlock("gtladditions:temporal_anchor_field_casing"))
                            .or(abilities(IMPORT_ITEMS))
                            .or(abilities(INPUT_LASER).setMaxGlobalLimited(1))
                            .or(abilities(EXPORT_ITEMS))
                            .or(abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1))
                    )
                    .where("J", blocks(getBlock("gtlcore:molecular_casing")))
                    .where("Q", blocks(getBlock("gtlcore:hyper_mechanical_casing")))
                    .where("P", blocks(getBlock("gtceu:naquadah_alloy_frame")))
                    .where("C", blocks(getBlock("gtceu:fusion_glass")))
                    .where("K", blocks(getBlock("kubejs:containment_field_generator")))
                    .where("M", blocks(getBlock("kubejs:annihilate_core")))
                    .where("A", blocks(getBlock("gtlcore:dimensionally_transcendent_casing")))
                    .where("G", blocks(getBlock("gtceu:advanced_computer_casing")))
                    .where("R", blocks(getBlock("gtlcore:rhenium_reinforced_energy_glass")))
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/sps_casing"),
                GTCEu.id("block/multiblock/research_station")
            )
            .register()

        SUBSPACE_CORRIDOR_HUB_INDUSTRIAL_ARRAY = REGISTRATE.multiblock(
            "subspace_corridor_hub_industrial_array",
            Function { SubspaceCorridorHubIndustrialArray(it) }
        )
            .nonYAxisRotation()
            .tooltipTextKey("gtladditions.multiblock.subspace_corridor_hub_industrial_array.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.subspace_corridor_hub_industrial_array.tooltip.1".toComponent)
            .tooltipTextKey("gtladditions.multiblock.subspace_corridor_hub_industrial_array.tooltip.2".toComponent)
            .tooltipTextKey("gtladditions.multiblock.subspace_corridor_hub_industrial_array.tooltip.3".toComponent)
            .tooltipTextKey("gtladditions.multiblock.subspace_corridor_hub_industrial_array.tooltip.4".toComponent)
            .tooltipTextKey("gtladditions.multiblock.subspace_corridor_hub_industrial_array.tooltip.5".toComponent)
            .tooltipTextKey("gtladditions.multiblock.subspace_corridor_hub_industrial_array.tooltip.6".toComponent)
            .tooltipTextKey("gtladditions.multiblock.subspace_corridor_hub_industrial_array.tooltip.7".toComponent)
            .tooltipTextKey("tooltip.gtlcore.structure.source".toComponent("BV11x4y1L7GZ"))
            .tooltipTextRecipeTypes(INTER_STELLAR)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(INTER_STELLAR)
            .recipeModifier(GTRecipeModifiers.ELECTRIC_OVERCLOCK.apply(OverclockingLogic(1.0, 1.0, false)))
            .appearanceBlock(HIGH_POWER_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.SUBSPACE_CORRIDOR_HUB_INDUSTRIAL_ARRAY!!
                    .where("~", controller(blocks(definition!!.get())))
                    .where("S", blocks(getBlock("gtceu:nonconducting_casing")))
                    .where("H", blocks(getBlock("gtladditions:extreme_density_casing")))
                    .where("X", blocks(getBlock("gtlcore:hyper_core")))
                    .where("M", blocks(getBlock("gtceu:fusion_glass")))
                    .where("^", blocks(getBlock("gtlcore:sps_casing")))
                    .where("A", blocks(getBlock("gtlcore:ultimate_stellar_containment_casing")))
                    .where("f", blocks(getBlock("gtlcore:super_computation_component")))
                    .where("W", blocks(getBlock("kubejs:force_field_glass")))
                    .where("g", blocks(getBlock("kubejs:restraint_device")))
                    .where("T", blocks(getBlock("gtlcore:compressed_fusion_coil_mk2")))
                    .where("F", blocks(getBlock("kubejs:module_base")))
                    .where("B", blocks(getBlock("kubejs:high_strength_concrete")))
                    .where("E", blocks(getBlock("kubejs:containment_field_generator")))
                    .where("I", blocks(getBlock("gtlcore:dimension_injection_casing")))
                    .where("N", blocks(getBlock("gtladditions:gravity_stabilization_casing")))
                    .where("]", blocks(getBlock("gtlcore:dimensionally_transcendent_casing")))
                    .where("e", blocks(getBlock("kubejs:spacetime_assembly_line_unit")))
                    .where("[", blocks(getBlock("gtlcore:dragon_strength_tritanium_casing")))
                    .where("P", blocks(getBlock("gtlcore:power_core")))
                    .where("O", blocks(getBlock("gtlcore:antifreeze_heatproof_machine_casing")))
                    .where("_", blocks(getBlock("kubejs:magic_core")))
                    .where("J", blocks(getBlock("gtlcore:naquadah_alloy_casing")))
                    .where("L", blocks(getBlock("gtlcore:iridium_casing")))
                    .where("b", blocks(getBlock("gtceu:advanced_computer_casing")))
                    .where("Y", blocks(getBlock("kubejs:dimensional_bridge_casing")))
                    .where("c", blocks(getBlock("kubejs:spacetime_assembly_line_casing")))
                    .where(
                        "h",
                        blocks(getBlock("gtceu:high_power_casing"))
                            .or(abilities(IMPORT_ITEMS))
                            .or(abilities(INPUT_ENERGY).setMaxGlobalLimited(1))
                    )
                    .where("R", blocks(getBlock("gtlcore:space_elevator_support")))
                    .where("U", blocks(getBlock("gtlcore:enhance_hyper_mechanical_casing")))
                    .where("a", blocks(getBlock("gtceu:computer_casing")))
                    .where("K", blocks(getBlock("gtceu:high_temperature_smelting_casing")))
                    .where("d", blocks(getBlock("kubejs:molecular_coil")))
                    .where("C", blocks(getBlock("kubejs:space_elevator_internal_support")))
                    .where("V", blocks(getBlock("gtlcore:echo_casing")))
                    .where("Z", blocks(getBlock("gtceu:plascrete")))
                    .where("Q", blocks(getBlock("gtlcore:oxidation_resistant_hastelloy_n_mechanical_casing")))
                    .where("`", blocks(getBlock("gtceu:computer_heat_vent")))
                    .where("D", blocks(getBlock("gtceu:fusion_casing")))
                    .where("G", blocks(getBlock("kubejs:module_connector")))
                    .build()
            }
            .renderer { SubspaceCorridorHubIndustrialArrayRenderer() }
            .hasTESR(true)
            .register()

        SPACE_INFINITY_INTEGRATED_ORE_PROCESSOR = REGISTRATE.multiblock(
            "space_infinity_integrated_ore_processor",
            Function { SpaceInfinityIntegratedOreProcessor(it) }
        )
            .allRotation()
            .tooltipTextKey("gtladditions.multiblock.space_infinity_integrated_ore_processor.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.space_infinity_integrated_ore_processor.tooltip.1".toComponent)
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist.tooltip.8".toComponent)
            .tooltipTextKey("gtceu.multiblock.only.laser.tooltip".toComponent)
            .tooltipTextKey("tooltip.gtlcore.structure.source".toComponent("TST"))
            .tooltipTextRecipeTypes(SPACE_ORE_PROCESSOR)
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .recipeType(SPACE_ORE_PROCESSOR)
            .appearanceBlock(CASING_TUNGSTENSTEEL_ROBUST)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.SPACE_INFINITY_INTEGRATED_ORE_PROCESSOR!!
                    .where("~", controller(blocks(definition!!.get())))
                    .where("A", blocks(IRIDIUM_CASING.get()))
                    .where("B", blocks(SPACE_ELEVATOR_MECHANICAL_CASING.get()))
                    .where("C", blocks(SPACE_ELEVATOR_SUPPORT.get()))
                    .where("D", blocks(getBlock("kubejs:space_elevator_internal_support")))
                    .where("E", blocks(POWER_MODULE_7.get()))
                    .where("F", blocks(getBlock("kubejs:dimensional_bridge_casing")))
                    .where("G", blocks(getBlock("kubejs:high_strength_concrete")))
                    .where(
                        "H",
                        blocks(SPACE_ELEVATOR_MECHANICAL_CASING.get())
                            .or(abilities(INPUT_LASER))
                            .or(abilities(IMPORT_ITEMS))
                            .or(abilities(EXPORT_ITEMS))
                            .or(abilities(IMPORT_FLUIDS))
                            .or(blocks(GTLAddMachines.ORE_PROCESSOR_HATCH.get()).setMaxGlobalLimited(1))
                    )
                    .where("I", blocks(ChemicalHelper.getBlock(frameGt, Infinity)))
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/space_elevator_mechanical_casing"),
                GTCEu.id("block/multiblock/data_bank")
            )
            .register()

        MACRO_ATOMIC_RESONANT_FRAGMENT_STRIPPER = REGISTRATE.multiblock(
            "macro_atomic_resonant_fragment_stripper",
            Function { MacroAtomicResonantFragmentStripper(it) }
        )
            .nonYAxisRotation()
            .tooltipTextKey("gtladditions.multiblock.macro_atomic_resonant_fragment_stripper.tooltip.0".toComponent)
            .tooltipTextKey("gtladditions.multiblock.macro_atomic_resonant_fragment_stripper.tooltip.1".toComponent)
            .tooltipTextKey("gtladditions.multiblock.macro_atomic_resonant_fragment_stripper.tooltip.2".toComponent)
            .tooltipTextKey("gtceu.machine.eut_multiplier.tooltip".toComponent(4))
            .tooltipTextLaser()
            .tooltipTextKey("gtladditions.multiblock.macro_atomic_resonant_fragment_stripper.tooltip.3".toComponent)
            .tooltipTextKey("gtladditions.multiblock.macro_atomic_resonant_fragment_stripper.tooltip.4".toComponent)
            .apply {
                if (ConfigHolder.INSTANCE.enableSkyBlokeMode) {
                    tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist.tooltip.3".toComponent)
                    tooltipTextKey(
                        "gtladditions.multiblock.base_parallel".toComponent("1536".literal.withStyle(ChatFormatting.GOLD))
                    )
                    tooltipTextKey("gtladditions.multiblock.macro_atomic_resonant_fragment_stripper.tooltip.5".toComponent)
                    tooltipTextKey("gtladditions.multiblock.macro_atomic_resonant_fragment_stripper.tooltip.6".toComponent)
                    tooltipTextKey("gtladditions.multiblock.macro_atomic_resonant_fragment_stripper.tooltip.7".toComponent)
                    tooltipTextKey("gtladditions.multiblock.macro_atomic_resonant_fragment_stripper.tooltip.8".toComponent)
                }
            }
            .tooltipTextKey("gtladditions.multiblock.forge_of_the_antichrist.tooltip.3".toComponent)
            .tooltipTextKey("gtladditions.multiblock.macro_atomic_resonant_fragment_stripper.tooltip.9".toComponent)
            .tooltipTextKey("gtladditions.multiblock.macro_atomic_resonant_fragment_stripper.tooltip.10".toComponent)
            .tooltipTextKey("tooltip.gtlcore.structure.source".toComponent("TST"))
            .apply {
                if (ConfigHolder.INSTANCE.enableSkyBlokeMode) {
                    tooltipTextRecipeTypes(STAR_CORE_STRIPPER, ELEMENT_COPYING_RECIPES)
                } else {
                    tooltipTextRecipeTypes(ELEMENT_COPYING_RECIPES)
                }
            }
            .tooltipBuilder(GTLAddMachines.GTLAdd_ADD)
            .apply {
                if (ConfigHolder.INSTANCE.enableSkyBlokeMode) {
                    recipeTypes(STAR_CORE_STRIPPER, ELEMENT_COPYING_RECIPES)
                } else {
                    recipeTypes(ELEMENT_COPYING_RECIPES)
                }
            }
            .appearanceBlock(HYPER_MECHANICAL_CASING)
            .pattern { definition: MultiblockMachineDefinition? ->
                MultiBlockStructure.MACRO_ATOMIC_RESONANT_FRAGMENT_STRIPPER!!
                    .where("~", controller(blocks(definition!!.get())))
                    .where("A", blocks(FUSION_GLASS.get()))
                    .where("B", blocks(QFT_COIL.get()))
                    .where("C", blocks(getBlock("kubejs:annihilate_core")))
                    .where("D", GTLAddPredicates.heatingCoils(21600))
                    .where("E", blocks(ECHO_CASING.get()))
                    .where("F", blocks(getBlock("kubejs:dimensional_stability_casing")))
                    .where("G", blocks(HYPER_MECHANICAL_CASING.get()))
                    .where("H", blocks(FUSION_CASING_MK5.get()))
                    .where("I", blocks(SPS_CASING.get()))
                    .where("J", blocks(getBlock("kubejs:dyson_control_toroid")))
                    .where("K", blocks(getBlock("kubejs:dyson_receiver_casing")))
                    .where("L", blocks(getBlock("kubejs:dyson_control_casing")))
                    .where("M", blocks(getBlock("kubejs:dimensional_bridge_casing")))
                    .where(
                        "N",
                        blocks(ChemicalHelper.getBlock(frameGt, QuantumChromodynamicallyConfinedMatter))
                    )
                    .where("O", blocks(ChemicalHelper.getBlock(frameGt, Neutronium)))
                    .where(
                        "Y",
                        blocks(HYPER_MECHANICAL_CASING.get())
                            .or(abilities(EXPORT_ITEMS))
                            .or(abilities(IMPORT_ITEMS))
                            .or(abilities(EXPORT_FLUIDS))
                            .or(abilities(IMPORT_FLUIDS))
                            .or(abilities(INPUT_ENERGY).setMaxGlobalLimited(2))
                            .or(abilities(INPUT_LASER).setMaxGlobalLimited(1))
                    )
                    .where("Z", blocks(HIGH_POWER_CASING.get()))
                    .build()
            }
            .workableCasingRenderer(
                GTLCore.id("block/casings/hyper_mechanical_casing"),
                GTCEu.id("block/multiblock/fusion_reactor")
            )
            .register()

        REGISTRATE.creativeModeTab { GTLAddCreativeModeTabs.GTLADD_ITEMS }
    }

    fun init() {}
}