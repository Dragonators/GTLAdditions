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
import com.gregtechceu.gtceu.api.pattern.Predicates
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic
import com.gregtechceu.gtceu.api.recipe.logic.OCParams
import com.gregtechceu.gtceu.api.recipe.logic.OCResult
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier
import com.gregtechceu.gtceu.common.data.GTBlocks.*
import com.gregtechceu.gtceu.common.data.GTMachines
import com.gregtechceu.gtceu.common.data.GTMaterials.*
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers
import com.gregtechceu.gtceu.common.data.GTRecipeTypes
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine
import com.gtladd.gtladditions.api.machine.GTLAddCoilWorkableElectricParallelHatchMultipleRecipesMachine
import com.gtladd.gtladditions.api.machine.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.api.machine.GTLAddWorkableElectricParallelHatchMultipleRecipesMachine
import com.gtladd.gtladditions.api.registry.GTLAddRegistration.REGISTRATE
import com.gtladd.gtladditions.common.machine.GTLAddMachines
import com.gtladd.gtladditions.common.machine.muiltblock.controller.*
import com.gtladd.gtladditions.common.machine.muiltblock.structure.MultiBlockStructure
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import com.hepdd.gtmthings.data.CustomMachines
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.level.block.Block
import org.gtlcore.gtlcore.GTLCore
import org.gtlcore.gtlcore.api.pattern.GTLPredicates
import org.gtlcore.gtlcore.client.renderer.machine.EyeOfHarmonyRenderer
import org.gtlcore.gtlcore.common.block.GTLFusionCasingBlock
import org.gtlcore.gtlcore.common.data.GTLBlocks.*
import org.gtlcore.gtlcore.common.data.GTLMachines
import org.gtlcore.gtlcore.common.data.GTLMaterials.*
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes
import org.gtlcore.gtlcore.utils.Registries.getBlock
import java.util.function.Function
import kotlin.math.pow

object MultiBlockMachine {
    @JvmField
    val SUPER_FACTORY_MKI: MultiblockMachineDefinition = REGISTRATE.multiblock("super_factory_mk1",
        Function { GTLAddWorkableElectricMultipleRecipesMachine(it!!) })
        .allRotation()
        .tooltipText("最大并行数：2147483647")
        .tooltipTextLaser()
        .tooltipTextMultiRecipes()
        .tooltipText("可用配方类型：车床，卷板机，压缩机，锻造锤，切割机，压模器，搅拌机，线材轧机，冲压车床，两极磁化机")
        .tooltipBuilder(GTLAddMachines.GTLAdd_TOOLTIP)
        .recipeType(GTRecipeTypes.LATHE_RECIPES) // 车床
        .recipeType(GTRecipeTypes.BENDER_RECIPES) // 卷板机
        .recipeType(GTRecipeTypes.COMPRESSOR_RECIPES) // 压缩机
        .recipeType(GTRecipeTypes.FORGE_HAMMER_RECIPES) // 锻造锤
        .recipeType(GTRecipeTypes.CUTTER_RECIPES) // 切割机
        .recipeType(GTRecipeTypes.EXTRUDER_RECIPES) // 压模器
        .recipeType(GTRecipeTypes.MIXER_RECIPES) // 搅拌机
        .recipeType(GTRecipeTypes.WIREMILL_RECIPES) // 线材轧机
        .recipeType(GTRecipeTypes.FORMING_PRESS_RECIPES) // 冲压车床
        .recipeType(GTRecipeTypes.POLARIZER_RECIPES) // 两极磁化机
        .appearanceBlock(MULTI_FUNCTIONAL_CASING)
        .pattern { definition: MultiblockMachineDefinition? ->
            MultiBlockStructure.EYE_OF_HARMONY_STRUCTURE!!
                .where("~", Predicates.controller(Predicates.blocks(definition!!.get())))
                .where("A", Predicates.blocks(MULTI_FUNCTIONAL_CASING.get()))
                .where("B", Predicates.blocks(MULTI_FUNCTIONAL_CASING.get())
                        .or(Predicates.autoAbilities(*definition.recipeTypes))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1)))
                .where("D", Predicates.blocks(MULTI_FUNCTIONAL_CASING.get()))
                .where("E", Predicates.blocks(getBlock("kubejs:neutronium_pipe_casing")))
                .where("F", Predicates.blocks(getBlock("gtceu:bronze_pipe_casing")))
                .where("G", Predicates.blocks(getBlock("gtceu:ptfe_pipe_casing")))
                .where(" ", Predicates.any())
                .build()
        }
        .additionalDisplay(GTLAddMultiBlockMachineModifier.INT_MAX_PARALLEL)
        .workableCasingRenderer(
            GTLCore.id("block/multi_functional_casing"),
            GTCEu.id("block/multiblock/gcym/large_assembler")
        )
        .register()

    @JvmField
    val SUPER_FACTORY_MKII: MultiblockMachineDefinition = REGISTRATE.multiblock("super_factory_mk2",
        Function { GTLAddWorkableElectricMultipleRecipesMachine(it!!) })
        .allRotation()
        .tooltipText("最大并行数：2147483647")
        .tooltipTextLaser()
        .tooltipTextMultiRecipes()
        .tooltipText("可用配方类型：碎岩机, 洗矿机，离心机，电解机，筛选机，研磨机，脱水机，热力离心机，电磁选矿机")
        .tooltipBuilder(GTLAddMachines.GTLAdd_TOOLTIP)
        .recipeType(GTRecipeTypes.ROCK_BREAKER_RECIPES) // 碎岩机
        .recipeType(GTRecipeTypes.ORE_WASHER_RECIPES) // 洗矿机
        .recipeType(GTRecipeTypes.CENTRIFUGE_RECIPES) // 离心机
        .recipeType(GTRecipeTypes.ELECTROLYZER_RECIPES) // 电解机
        .recipeType(GTRecipeTypes.SIFTER_RECIPES) // 筛选机
        .recipeType(GTRecipeTypes.MACERATOR_RECIPES) // 研磨机
        .recipeType(GTLRecipeTypes.DEHYDRATOR_RECIPES) // 脱水机
        .recipeType(GTRecipeTypes.THERMAL_CENTRIFUGE_RECIPES) // 热力离心机
        .recipeType(GTRecipeTypes.ELECTROMAGNETIC_SEPARATOR_RECIPES) // 电磁选矿机
        .appearanceBlock(MULTI_FUNCTIONAL_CASING)
        .pattern { definition: MultiblockMachineDefinition? ->
            MultiBlockStructure.EYE_OF_HARMONY_STRUCTURE!!
                .where("~", Predicates.controller(Predicates.blocks(definition!!.get())))
                .where("A", Predicates.blocks(MULTI_FUNCTIONAL_CASING.get()))
                .where("B", Predicates.blocks(MULTI_FUNCTIONAL_CASING.get())
                        .or(Predicates.autoAbilities(*definition.recipeTypes))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1)))
                .where("D", Predicates.blocks(MULTI_FUNCTIONAL_CASING.get()))
                .where("E", Predicates.blocks(getBlock("kubejs:neutronium_pipe_casing")))
                .where("F", Predicates.blocks(getBlock("gtceu:bronze_pipe_casing")))
                .where("G", Predicates.blocks(getBlock("gtceu:ptfe_pipe_casing")))
                .where(" ", Predicates.any())
                .build()
        }
        .additionalDisplay(GTLAddMultiBlockMachineModifier.INT_MAX_PARALLEL)
        .workableCasingRenderer(
            GTLCore.id("block/multi_functional_casing"),
            GTCEu.id("block/multiblock/gcym/large_assembler")
        )
        .register()

    @JvmField
    val SUPER_FACTORY_MKIII: MultiblockMachineDefinition = REGISTRATE.multiblock("super_factory_mk3",
        Function { GTLAddWorkableElectricMultipleRecipesMachine(it!!) })
        .allRotation()
        .tooltipText("最大并行数：2147483647")
        .tooltipTextLaser()
        .tooltipTextMultiRecipes()
        .tooltipText("可用配方类型：蒸发, 高压釜, 提取机, 酿造机, 发酵槽, 蒸馏室, 蒸馏塔, 流体加热机, 流体固化机, 化学浸洗机")
        .tooltipBuilder(GTLAddMachines.GTLAdd_TOOLTIP)
        .recipeType(GTRecipeTypes.EVAPORATION_RECIPES) // 蒸发
        .recipeType(GTRecipeTypes.AUTOCLAVE_RECIPES) // 高压釜
        .recipeType(GTRecipeTypes.EXTRACTOR_RECIPES) // 提取机
        .recipeType(GTRecipeTypes.BREWING_RECIPES) // 酿造机
        .recipeType(GTRecipeTypes.FERMENTING_RECIPES) // 发酵槽
        .recipeType(GTRecipeTypes.DISTILLERY_RECIPES) // 蒸馏室
        .recipeType(GTRecipeTypes.DISTILLATION_RECIPES) // 蒸馏塔
        .recipeType(GTRecipeTypes.FLUID_HEATER_RECIPES) // 流体加热机
        .recipeType(GTRecipeTypes.FLUID_SOLIDFICATION_RECIPES) // 流体固化机
        .recipeType(GTRecipeTypes.CHEMICAL_BATH_RECIPES) // 化学浸洗机
        .appearanceBlock(MULTI_FUNCTIONAL_CASING)
        .pattern { definition: MultiblockMachineDefinition? ->
            MultiBlockStructure.EYE_OF_HARMONY_STRUCTURE!!
                .where("~", Predicates.controller(Predicates.blocks(definition!!.get())))
                .where("A", Predicates.blocks(MULTI_FUNCTIONAL_CASING.get()))
                .where("B", Predicates.blocks(MULTI_FUNCTIONAL_CASING.get())
                        .or(Predicates.autoAbilities(*definition.recipeTypes))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1)))
                .where("D", Predicates.blocks(MULTI_FUNCTIONAL_CASING.get()))
                .where("E", Predicates.blocks(getBlock("kubejs:neutronium_pipe_casing")))
                .where("F", Predicates.blocks(getBlock("gtceu:bronze_pipe_casing")))
                .where("G", Predicates.blocks(getBlock("gtceu:ptfe_pipe_casing")))
                .where(" ", Predicates.any())
                .build()
        }
        .additionalDisplay(GTLAddMultiBlockMachineModifier.INT_MAX_PARALLEL)
        .workableCasingRenderer(
            GTLCore.id("block/multi_functional_casing"),
            GTCEu.id("block/multiblock/gcym/large_assembler")
        )
        .register()

    @JvmField
    val SUPER_FACTORY_MKIV: MultiblockMachineDefinition = REGISTRATE.multiblock("super_factory_mk4",
        Function { GTLAddWorkableElectricMultipleRecipesMachine(it!!) })
        .allRotation()
        .tooltipText("最大并行数：2147483647")
        .tooltipTextLaser()
        .tooltipTextMultiRecipes()
        .tooltipText("可用配方类型：装罐机, 电弧炉, 闪电处理, 组装机, 精密组装, 电路组装机")
        .tooltipBuilder(GTLAddMachines.GTLAdd_TOOLTIP)
        .recipeType(GTRecipeTypes.CANNER_RECIPES) // 装罐机
        .recipeType(GTRecipeTypes.ARC_FURNACE_RECIPES) // 电弧炉
        .recipeType(GTLRecipeTypes.LIGHTNING_PROCESSOR_RECIPES) // 闪电处理
        .recipeType(GTRecipeTypes.ASSEMBLER_RECIPES) // 组装机
        .recipeType(GTLRecipeTypes.PRECISION_ASSEMBLER_RECIPES) // 精密组装
        .recipeType(GTRecipeTypes.CIRCUIT_ASSEMBLER_RECIPES) // 电路组装机
        .appearanceBlock(MULTI_FUNCTIONAL_CASING)
        .pattern { definition: MultiblockMachineDefinition? ->
            MultiBlockStructure.EYE_OF_HARMONY_STRUCTURE!!
                .where("~", Predicates.controller(Predicates.blocks(definition!!.get())))
                .where("A", Predicates.blocks(MULTI_FUNCTIONAL_CASING.get()))
                .where("B", Predicates.blocks(MULTI_FUNCTIONAL_CASING.get())
                        .or(Predicates.autoAbilities(*definition.recipeTypes))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1)))
                .where("D", Predicates.blocks(MULTI_FUNCTIONAL_CASING.get()))
                .where("E", Predicates.blocks(getBlock("kubejs:neutronium_pipe_casing")))
                .where("F", Predicates.blocks(getBlock("gtceu:bronze_pipe_casing")))
                .where("G", Predicates.blocks(getBlock("gtceu:ptfe_pipe_casing")))
                .where(" ", Predicates.any())
                .build()
        }
        .additionalDisplay(GTLAddMultiBlockMachineModifier.INT_MAX_PARALLEL)
        .workableCasingRenderer(
            GTLCore.id("block/multi_functional_casing"),
            GTCEu.id("block/multiblock/gcym/large_assembler")
        )
        .register()

    @JvmField
    val LUCID_ETCHDREAMER: MultiblockMachineDefinition = REGISTRATE.multiblock("lucid_etchdreamer",
        Function { GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(it!!)})
        .nonYAxisRotation()
        .tooltipTextCoilParallel()
        .tooltipTextLaser()
        .tooltipTextMultiRecipes()
        .tooltipText("可用配方类型：光子晶阵蚀刻")
        .coilparalleldisplay()
        .tooltipBuilder(GTLAddMachines.GTLAdd_TOOLTIP)
        .recipeType(GTLAddRecipesTypes.PHOTON_MATRIX_ETCH)
        .appearanceBlock(IRIDIUM_CASING)
        .pattern { definition: MultiblockMachineDefinition? ->
            MultiBlockStructure.LUCID_ETCHDREAMER_STRUCTURE!!
                .where("I", Predicates.controller(Predicates.blocks(definition!!.get())))
                .where("A", Predicates.blocks(IRIDIUM_CASING.get())
                        .or(Predicates.autoAbilities(*definition.recipeTypes))
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1)))
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
        .additionalDisplay(GTLAddMultiBlockMachineModifier.MULTIPLERECIPES_COIL_PARALLEL)
        .workableCasingRenderer(
            GTLCore.id("block/casings/iridium_casing"),
            GTCEu.id("block/multiblock/gcym/large_engraving_laser")
        )
        .register()

    @JvmField
    val ATOMIC_TRANSMUTATIOON_CORE: MultiblockMachineDefinition = REGISTRATE.multiblock("atomic_transmutation_core",
        Function { GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(it!!) })
        .noneRotation()
        .tooltipTextCoilParallel()
        .tooltipTextLaser()
        .tooltipTextMultiRecipes()
        .tooltipText("可用配方类型：电磁共振转化场")
        .coilparalleldisplay()
        .tooltipBuilder(GTLAddMachines.GTLAdd_TOOLTIP)
        .recipeType(GTLAddRecipesTypes.EM_RESONANCE_CONVERSION_FIELD)
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
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1)))
                .where("C", Predicates.heatingCoils())
                .where("D", Predicates.blocks(getBlock("kubejs:infused_obsidian")))
                .where("B", Predicates.blocks(CLEANROOM_GLASS.get()))
                .where(" ", Predicates.any())
                .build()
        }
        .additionalDisplay(GTLAddMultiBlockMachineModifier.MULTIPLERECIPES_COIL_PARALLEL)
        .workableCasingRenderer(
            GTLCore.id("block/casings/aluminium_bronze_casing"),
            GTCEu.id("block/multiblock/cleanroom")
        )
        .register()

    @JvmField
    val ASTRAL_CONVERGENCE_NEXUS: MultiblockMachineDefinition = REGISTRATE.multiblock("astral_convergence_nexus",
        Function { AdvancedSpaceElevatorModuleMachine(it!!) })
        .nonYAxisRotation()
        .tooltipText("最大并行数：8^(动力模块等级-1)")
        .tooltipTextLaser()
        .tooltipTextMultiRecipes()
        .tooltipText("可用配方类型：太空组装")
        .tooltipBuilder(GTLAddMachines.GTLAdd_TOOLTIP)
        .recipeType(GTLRecipeTypes.ASSEMBLER_MODULE_RECIPES) // 太空组装
        .appearanceBlock(SPACE_ELEVATOR_MECHANICAL_CASING)
        .pattern { definition: MultiblockMachineDefinition? ->
            FactoryBlockPattern.start()
                .aisle("aaa", "bcb", "bbb", "bbb", "bbb")
                .aisle("aaa", "bbb", "bbb", "bbb", "bbb")
                .aisle("aaa", "bbb", "bbb", "b~b", "bbb")
                .where("~", Predicates.controller(Predicates.blocks(definition!!.get())))
                .where("b", Predicates.blocks(SPACE_ELEVATOR_MECHANICAL_CASING.get())
                        .or(Predicates.autoAbilities(*definition.recipeTypes))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1)))
                .where("a", Predicates.blocks(getBlock("kubejs:module_base")))
                .where("c", Predicates.blocks(getBlock("kubejs:module_connector")))
                .build()
        }
        .workableCasingRenderer(
            GTLCore.id("block/space_elevator_mechanical_casing"),
            GTCEu.id("block/multiblock/gcym/large_assembler")
        )
        .register()

    @JvmField
    val NEBULA_REAPER: MultiblockMachineDefinition = REGISTRATE.multiblock("nebula_reaper",
        Function { AdvancedSpaceElevatorModuleMachine(it!!) })
        .nonYAxisRotation()
        .tooltipText("最大并行数：8^(动力模块等级-1)")
        .tooltipTextLaser()
        .tooltipTextMultiRecipes()
        .tooltipText("可用配方类型：太空采矿、太空钻井")
        .tooltipBuilder(GTLAddMachines.GTLAdd_TOOLTIP)
        .recipeType(GTLRecipeTypes.MINER_MODULE_RECIPES) // 太空采矿
        .recipeType(GTLRecipeTypes.DRILLING_MODULE_RECIPES) // 太空钻井
        .appearanceBlock(SPACE_ELEVATOR_MECHANICAL_CASING)
        .pattern { definition: MultiblockMachineDefinition? ->
            FactoryBlockPattern.start()
                .aisle("aaa", "bcb", "bbb", "bbb", "bbb")
                .aisle("aaa", "bbb", "bbb", "bbb", "bbb")
                .aisle("aaa", "bbb", "bbb", "b~b", "bbb")
                .where("~", Predicates.controller(Predicates.blocks(definition!!.get())))
                .where("b", Predicates.blocks(SPACE_ELEVATOR_MECHANICAL_CASING.get())
                        .or(Predicates.autoAbilities(*definition.recipeTypes))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1)))
                .where("a", Predicates.blocks(getBlock("kubejs:module_base")))
                .where("c", Predicates.blocks(getBlock("kubejs:module_connector")))
                .build()
        }
        .workableCasingRenderer(
            GTLCore.id("block/space_elevator_mechanical_casing"),
            GTCEu.id("block/multiblock/gcym/large_assembler")
        )
        .register()

    @JvmField
    val ARCANIC_ASTROGRAPH: MultiblockMachineDefinition = REGISTRATE.multiblock("arcanic_astrograph",
        Function { ArcanicAstrograph(it!!) })
        .nonYAxisRotation()
        .recipeType(GTLRecipeTypes.COSMOS_SIMULATION_RECIPES)
        .recipeModifier { machine: MetaMachine?, recipe: GTRecipe?, params: OCParams?, result: OCResult? ->
            ArcanicAstrograph.recipeModifier(machine, recipe!!, params!!, result!!)
        }
        .tooltips(*arrayOf<Component>(Component.literal("最大并行数：2048")))
        .tooltips(*arrayOf<Component>(Component.literal("仅量子爆弹和宇宙素的消耗量增加，其他资源消耗量不变")))
        .tooltips(*arrayOf<Component>(Component.translatable("gtceu.machine.eye_of_harmony.tooltip.0")))
        .tooltips(*arrayOf<Component>(Component.translatable("gtceu.machine.eye_of_harmony.tooltip.1")))
        .tooltips(*arrayOf<Component>(Component.translatable("gtceu.machine.eye_of_harmony.tooltip.2")))
        .tooltips(*arrayOf<Component>(Component.translatable("gtceu.machine.eye_of_harmony.tooltip.3")))
        .tooltips(*arrayOf<Component>(Component.translatable("gtceu.machine.eye_of_harmony.tooltip.4")))
        .tooltips(*arrayOf<Component>(Component.translatable("gtceu.machine.eye_of_harmony.tooltip.5")))
        .tooltips(*arrayOf<Component>(Component.translatable("gtceu.machine.eye_of_harmony.tooltip.6")))
        .tooltips(
            *arrayOf<Component>(
                Component.translatable(
                    "gtceu.machine.available_recipe_map_1.tooltip",
                    Component.translatable("gtceu.cosmos_simulation")
                )
            )
        )
        .tooltipBuilder(GTLAddMachines.GTLAdd_TOOLTIP)
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

    @JvmField
    val ARCANE_CACHE_VAULT: MultiblockMachineDefinition = REGISTRATE.multiblock("arcane_cache_vault",
        Function { GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(it!!)})
        .allRotation()
        .tooltipTextCoilParallel()
        .tooltipTextLaser()
        .tooltipTextMultiRecipes()
        .tooltipText("可用配方类型：打包机")
        .coilparalleldisplay()
        .tooltipBuilder(GTLAddMachines.GTLAdd_TOOLTIP)
        .recipeType(GTRecipeTypes.PACKER_RECIPES)
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
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1)))
                .where("B", Predicates.heatingCoils())
                .build()
        }
        .additionalDisplay(GTLAddMultiBlockMachineModifier.MULTIPLERECIPES_COIL_PARALLEL)
        .workableCasingRenderer(
            GTLCore.id("block/casings/pikyonium_machine_casing"),
            GTCEu.id("block/multiblock/gcym/large_packer")
        )
        .register()

    @JvmField
    val DRACONIC_COLLAPSE_CORE: MultiblockMachineDefinition = REGISTRATE.multiblock("draconic_collapse_core",
        Function { WorkableElectricMultiblockMachine(it!!) })
        .nonYAxisRotation()
        .tooltipText("电压等级每高出UEV一级最大并行数X8")
        .tooltipText("只能使用激光仓")
        .tooltipTextPerfectOverclock()
        .tooltipText("可用配方类型：聚合装置")
        .tooltipBuilder(GTLAddMachines.GTLAdd_TOOLTIP)
        .recipeType(GTLRecipeTypes.AGGREGATION_DEVICE_RECIPES)
        .recipeModifiers(*GTLAddMultiBlockMachineModifier.DRACONIC_COLLAPSE_CORE_MODIFIER)
        .appearanceBlock(FUSION_CASING)
        .pattern { definition: MultiblockMachineDefinition? ->
            MultiBlockStructure.DRACONIC_COLLAPSE_CORE_STRUCTURE!!
                .where("E", Predicates.controller(Predicates.blocks(definition!!.get())))
                .where("D", Predicates.blocks(GTLFusionCasingBlock.getCasingState(10))
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(2))
                        .setMinGlobalLimited(1))
                .where("L", Predicates.blocks(GTLFusionCasingBlock.getCasingState(10))
                        .or(Predicates.blocks(GTMachines.ITEM_IMPORT_BUS[0].get()))
                        .or(Predicates.blocks(CustomMachines.HUGE_ITEM_IMPORT_BUS[0].get())))
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
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS)))
                .where(" ", Predicates.any())
                .build()
        }
        .additionalDisplay(GTLAddMultiBlockMachineModifier.DRACONIC_COLLAPSE_CORE_ADDTEXT)
        .workableCasingRenderer(
            GTLFusionCasingBlock.getCasingType(10).texture,
            GTCEu.id("block/multiblock/fusion_reactor")
        )
        .register()

    @JvmField
    val TITAN_CRIP_EARTHBORE: MultiblockMachineDefinition = REGISTRATE.multiblock("titan_crip_earthbore",
        Function { WorkableElectricMultiblockMachine(it!!) })
        .noneRotation()
        .tooltipText("电压每高出LuV一级最大并行数X2")
        .tooltipTextPerfectOverclock()
        .tooltipText("可用配方类型：地脉断层发生器")
        .tooltipBuilder(GTLAddMachines.GTLAdd_TOOLTIP)
        .recipeType(GTLAddRecipesTypes.TECTONIC_FAULT_GENERATOR)
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
                        .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1)))
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

    @JvmField
    val BIOLOGICAL_SIMULATION_LABORATORY: MultiblockMachineDefinition = REGISTRATE.multiblock("biological_simulation_laboratory",
        Function { BiologicalSimulationLaboratory(it!!) })
        .allRotation()
        .tooltipText("更高效的获取生物掉落物")
        .tooltipText("初始最大并行数为64")
        .tooltipText("可以在主机中放入不同纳米蜂群获得不同的加成")
        .tooltipText("§c铼纳米蜂群§r（§o并行§r：2048，§o耗电§r：0.9，§o耗时§r：0.9）")
        .tooltipText("§c山铜纳米蜂群§r（§o并行§r：16384，§o耗电§r：0.8，§o耗时§r：0.6）")
        .tooltipText("§c魔金纳米蜂群§r（§o并行§r：262144，§o耗电§r：0.6，§o耗时§r：0.4）")
        .tooltipText("§c不再是菜鸟的证明§r（§o并行§r：4194304，§o耗电§r：0.25，§o耗时§r：0.1）")
        .tooltipText("放入§c不再是菜鸟的证明§r时解锁寰宇支配之剑的配方")
        .tooltipText("放入§c不再是菜鸟的证明§r时解锁跨配方")
        .tooltipText("允许使用激光仓，但只能使用UXV以上的激光仓")
        .tooltipText("可用配方类型：生物数据模拟")
        .tooltipBuilder(GTLAddMachines.GTLAdd_TOOLTIP)
        .recipeType(GTLAddRecipesTypes.BIOLOGICAL_SIMULATION)
        .appearanceBlock(NAQUADAH_ALLOY_CASING)
        .pattern { definition: MultiblockMachineDefinition? ->
            MultiBlockStructure.BIOLOGICAL_SIMULATION_LABORATORY_STRUCTURE!!
                .where("~", Predicates.controller(Predicates.blocks(definition!!.get())))
                .where("A", Predicates.blocks(NAQUADAH_ALLOY_CASING.get())
                        .or(Predicates.autoAbilities(*definition.recipeTypes))
                        .or(Predicates.blocks(*PartAbility.INPUT_LASER.getBlockRange(12, 14).toTypedArray<Block?>()).setMaxGlobalLimited(1)))
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

    @JvmField
    val DIMENSIONALLY_TRANSCENDENT_CHEMICAL_PLANT: MultiblockMachineDefinition = REGISTRATE.multiblock("dimensionally_transcendent_chemical_plant",
        Function { GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(it!!) })
        .nonYAxisRotation()
        .tooltipText("高效的化学反应堆")
        .tooltipTextCoilParallel()
        .tooltipTextLaser()
        .tooltipTextMultiRecipes()
        .tooltipText("可用配方类型：大型化学反应釜")
        .tooltipBuilder(GTLAddMachines.GTLAdd_TOOLTIP)
        .recipeType(GTRecipeTypes.LARGE_CHEMICAL_RECIPES)
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
                        .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1)))
                .where("b", Predicates.blocks(HIGH_POWER_CASING.get()))
                .where("C", Predicates.heatingCoils())
                .where("d", Predicates.blocks(CASING_PTFE_INERT.get()))
                .where("s", Predicates.blocks(getBlock("gtceu:ptfe_pipe_casing")))
                .where(" ", Predicates.any())
                .build()
        }
        .additionalDisplay(GTLAddMultiBlockMachineModifier.MULTIPLERECIPES_COIL_PARALLEL)
        .workableCasingRenderer(
            GTCEu.id("block/casings/solid/machine_casing_inert_ptfe"),
            GTCEu.id("block/machines/chemical_reactor")
        )
        .register()

    @JvmField
    val QUANTUM_SYPHON_MATRIX: MultiblockMachineDefinition = REGISTRATE.multiblock("quantum_syphon_matrix",
        Function { GTLAddWorkableElectricParallelHatchMultipleRecipesMachine(it!!)})
        .noneRotation()
        .tooltipTextParallelHatch()
        .tooltipTextLaser()
        .tooltipTextMultiRecipes()
        .tooltipText("可用配方类型：虚空聚流反应")
        .tooltipBuilder(GTLAddMachines.GTLAdd_TOOLTIP)
        .recipeType(GTLAddRecipesTypes.VOIDFLUX_REACTION)
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
                        .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1)))
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

    @JvmField
    val FUXI_BAGUA_HEAVEN_FORGING_FURNACE: MultiblockMachineDefinition = REGISTRATE.multiblock("fuxi_bagua_heaven_forging_furnace",
        Function { GTLAddCoilWorkableElectricParallelHatchMultipleRecipesMachine(it!!) })
        .nonYAxisRotation()
        .tooltipTextParallelHatch()
        .tooltipText("只能使用激光仓")
        .tooltipTextMultiRecipes()
        .tooltipText("可用配方类型：星焰跃迁、混沌炼金、分子解构、终极物质锻造")
        .tooltipBuilder(GTLAddMachines.GTLAdd_TOOLTIP)
        .recipeType(GTLAddRecipesTypes.STELLAR_LGNITION)
        .recipeType(GTLAddRecipesTypes.CHAOTIC_ALCHEMY)
        .recipeType(GTLAddRecipesTypes.MOLECULAR_DECONSTRUCTION)
        .recipeType(GTLRecipeTypes.ULTIMATE_MATERIAL_FORGE_RECIPES)
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
                        .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1)))
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
                                FormattingUtil.formatNumbers(controller.coilType!!.coilTemperature) + "K")
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

    @JvmField
    val ANTIENTROPY_CONDENSATION_CENTER: MultiblockMachineDefinition = REGISTRATE.multiblock("antientropy_condensation_center",
        Function { AntientropyCondensationCenter(it!!) })
        .allRotation()
        .tooltipText("每次工作前需要提供凛冰粉")
        .tooltipText("电压每高一级，消耗的凛冰粉数量/2")
        .tooltipTextParallelHatch()
        .tooltipText("只能使用激光仓")
        .tooltipTextMultiRecipes()
        .tooltipText("可用配方类型：反熵冷凝")
        .tooltipBuilder(GTLAddMachines.GTLAdd_TOOLTIP)
        .recipeType(GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION)
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
                        .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1)))
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

    @JvmField
    val TAIXU_TURBID_ARRAY: MultiblockMachineDefinition = REGISTRATE.multiblock("taixu_turbid_array",
        Function { TaixuTurbidArray(it!!) })
        .rotationState(RotationState.Y_AXIS)
        .tooltips(
            Component.translatable("gtceu.machine.taixuturbidarray.tooltip.0"),
            Component.translatable("gtceu.machine.taixuturbidarray.tooltip.1"),
            Component.translatable("gtceu.machine.taixuturbidarray.tooltip.12"),
            Component.translatable("gtceu.machine.taixuturbidarray.tooltip.2"),
            Component.translatable("gtceu.machine.taixuturbidarray.tooltip.3"),
            Component.translatable("gtceu.machine.taixuturbidarray.tooltip.13"),
            Component.translatable("gtceu.machine.taixuturbidarray.tooltip.4"),
            Component.translatable("gtceu.machine.taixuturbidarray.tooltip.5"),
            Component.translatable("gtceu.machine.taixuturbidarray.tooltip.6"),
            Component.translatable("gtceu.machine.taixuturbidarray.tooltip.8"),
            Component.translatable("gtceu.machine.taixuturbidarray.tooltip.9"),
            Component.translatable("gtceu.machine.taixuturbidarray.tooltip.14"),
            Component.translatable("gtceu.machine.taixuturbidarray.tooltip.15"),
            Component.translatable("gtceu.machine.taixuturbidarray.tooltip.7"),
            Component.translatable("gtceu.machine.taixuturbidarray.tooltip.11"),
            Component.translatable("gtceu.machine.taixuturbidarray.tooltip.10")
        )
        .tooltipBuilder(GTLAddMachines.GTLAdd_TOOLTIP)
        .recipeType(GTLAddRecipesTypes.CHAOS_WEAVE)
        .recipeModifier { machine: MetaMachine?, recipe: GTRecipe?, params: OCParams?, result: OCResult? ->
            TaixuTurbidArray.recipeModifier(machine!!, recipe!!, params!!, result!!)
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
                .where("M", GTLPredicates.tierCasings(scmap, "SCTier"))
                .where("A", Predicates.blocks(IRIDIUM_CASING.get()))
                .where("L", Predicates.blocks(getBlock("kubejs:containment_field_generator")))
                .where("Q", Predicates.blocks(getBlock("kubejs:dimensional_bridge_casing")))
                .where("C", Predicates.blocks(getBlock("gtceu:atomic_casing")))
                .where("D", Predicates.blocks(ChemicalHelper.getBlock(frameGt, Mithril)))
                .where("O", Predicates.heatingCoils())
                .build()
        }
        .workableCasingRenderer(
            GTCEu.id("block/casings/voltage/uhv/side"),
            GTCEu.id("block/multiblock/fusion_reactor")
        )
        .register()

    @JvmField
    val INFERNO_CLEFT_SMELTING_VAULT: MultiblockMachineDefinition = REGISTRATE.multiblock("inferno_cleft_smelting_vault",
        Function { GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(it!!) })
        .nonYAxisRotation()
        .tooltipTextCoilParallel()
        .tooltipTextLaser()
        .tooltipTextMultiRecipes()
        .tooltipText("可用配方类型：热解机, 裂化机")
        .tooltipBuilder(GTLAddMachines.GTLAdd_TOOLTIP)
        .recipeTypes(GTRecipeTypes.PYROLYSE_RECIPES, GTRecipeTypes.CRACKING_RECIPES)
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
                    .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1)))
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
        .additionalDisplay(GTLAddMultiBlockMachineModifier.MULTIPLERECIPES_COIL_PARALLEL)
        .workableCasingRenderer(
            GTLCore.id("block/casings/iridium_casing"),
            GTCEu.id("block/multiblock/pyrolyse_oven")
        )
        .register()

    @JvmField
    val SKELETON_SHIFT_RIFT_ENGINE: MultiblockMachineDefinition = REGISTRATE.multiblock("skeleton_shift_rift_engine",
        Function {  SkeletonShiftRiftEngine(it!!) })
        .nonYAxisRotation()
        .tooltipText("线圈温度每高出1200K, 并行数X2")
        .tooltipText("耗时倍速: 1/恒星热力容器等级")
        .tooltipTextLaser()
        .tooltipTextPerfectOverclock()
        .tooltipText("可用配方类型：衰变加速器")
        .tooltipBuilder(GTLAddMachines.GTLAdd_TOOLTIP)
        .recipeTypes(GTLRecipeTypes.DECAY_HASTENER_RECIPES)
        .recipeModifier(SkeletonShiftRiftEngine::recipeModifier)
        .appearanceBlock(HYPER_MECHANICAL_CASING)
        .pattern { definition: MultiblockMachineDefinition? ->
            MultiBlockStructure.SKELETON_SHIFT_RIFT_ENGINE!!
                .where("Q", Predicates.controller(Predicates.blocks(definition!!.get())))
                .where("P", GTLPredicates.tierCasings(scmap, "SCTier"))
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
                    .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1)))
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

    @JvmStatic
    fun init() {}
}
