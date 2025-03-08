package com.gtladd.gtladditions.Common.MuiltBlock;

import org.gtlcore.gtlcore.GTLCore;
import org.gtlcore.gtlcore.client.renderer.machine.EyeOfHarmonyRenderer;
import org.gtlcore.gtlcore.common.block.GTLFusionCasingBlock;
import org.gtlcore.gtlcore.common.data.GTLBlocks;
import org.gtlcore.gtlcore.common.data.GTLMachines;
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes;
import org.gtlcore.gtlcore.common.machine.multiblock.electric.CoilWorkableElectricMultipleRecipesMultiblockMachine;
import org.gtlcore.gtlcore.common.machine.multiblock.electric.StorageMachine;
import org.gtlcore.gtlcore.utils.Registries;
import org.gtlcore.gtlcore.utils.TextUtil;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

import com.gtladd.gtladditions.Common.MuiltBlock.Structure.MultiBlockStructure;
import com.gtladd.gtladditions.api.Machine.GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine;
import com.gtladd.gtladditions.api.Machine.GTLAddWorkableElectricMultipleRecipesMachine;
import com.gtladd.gtladditions.api.Machine.GTLAddWorkableElectricParallelHatchMultipleRecipesMachine;
import com.gtladd.gtladditions.api.Machine.Special.AdvancedHarmonyMachine;
import com.gtladd.gtladditions.api.Machine.Special.AdvancedSpaceElevatorModuleMachine;
import com.gtladd.gtladditions.api.Machine.Special.BiologicalSimulationLaboratory;
import com.gtladd.gtladditions.api.Recipe.GTLAddRecipesTypes;
import com.gtladd.gtladditions.api.Registry.GTLAddRegistration;
import com.hepdd.gtmthings.data.CustomMachines;

public class MultiBlockMachine {

    public static final MultiblockMachineDefinition SUPER_FACTORY_MKI;
    public static final MultiblockMachineDefinition SUPER_FACTORY_MKII;
    public static final MultiblockMachineDefinition SUPER_FACTORY_MKIII;
    public static final MultiblockMachineDefinition SUPER_FACTORY_MKIV;
    public static final MultiblockMachineDefinition LUCID_ETCHDREAMER;
    public static final MultiblockMachineDefinition ATOMIC_TRANSMUTATIOON_CORE;
    public static final MultiblockMachineDefinition ASTRAL_CONVERGENCE_NEXUS;
    public static final MultiblockMachineDefinition NEBULA_REAPER;
    public static final MultiblockMachineDefinition ARCANIC_ASTROGRAPH;
    public static final MultiblockMachineDefinition ARCANE_CACHE_VAULT;
    public static final MultiblockMachineDefinition DRACONIC_COLLAPSE_CORE;
    public static final MultiblockMachineDefinition TITAN_CRIP_EARTHBORE;
    public static final MultiblockMachineDefinition BIOLOGICAL_SIMULATION_LABORATORY;
    public static final MultiblockMachineDefinition DIMENSIONALLY_TRANSCENDENT_CHEMICAL_PLANT;
    public static final MultiblockMachineDefinition QUANTUM_SYPHON_MATRIX;
    public static final MultiblockMachineDefinition FUXI_BAGUA_HEAVEN_FORGING_FURNACE;

    public MultiBlockMachine() {}

    public static void init() {}

    static {
        SUPER_FACTORY_MKI = GTLAddRegistration.REGISTRATE.multiblock("super_factory_mk1", GTLAddWorkableElectricMultipleRecipesMachine::new)
                .allRotation()
                .tooltipText("最大并行数：2147483647")
                .tooltipTextLaser()
                .tooltipTextSuperMultiRecipes()
                .tooltipText("可用配方类型：车床，卷板机，压缩机，锻造锤，切割机，压模器，搅拌机，线材轧机，冲压车床，两极磁化机")
                .tooltipTextAdd()
                .recipeType(GTRecipeTypes.LATHE_RECIPES)// 车床
                .recipeType(GTRecipeTypes.BENDER_RECIPES)// 卷板机
                .recipeType(GTRecipeTypes.COMPRESSOR_RECIPES)// 压缩机
                .recipeType(GTRecipeTypes.FORGE_HAMMER_RECIPES)// 锻造锤
                .recipeType(GTRecipeTypes.CUTTER_RECIPES)// 切割机
                .recipeType(GTRecipeTypes.EXTRUDER_RECIPES)// 压模器
                .recipeType(GTRecipeTypes.MIXER_RECIPES)// 搅拌机
                .recipeType(GTRecipeTypes.WIREMILL_RECIPES)// 线材轧机
                .recipeType(GTRecipeTypes.FORMING_PRESS_RECIPES)// 冲压车床
                .recipeType(GTRecipeTypes.POLARIZER_RECIPES)// 两极磁化机
                .appearanceBlock(GTLBlocks.MULTI_FUNCTIONAL_CASING)
                .pattern((definition) -> MultiBlockStructure.ARCANIC_ASTROGRAPH_STRUCTURE
                        .where("~", Predicates.controller(Predicates.blocks(definition.get())))
                        .where("A", Predicates.blocks(GTLBlocks.MULTI_FUNCTIONAL_CASING.get()))
                        .where("B", Predicates.blocks(GTLBlocks.MULTI_FUNCTIONAL_CASING.get())
                                .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1)))
                        .where("D", Predicates.blocks(GTLBlocks.MULTI_FUNCTIONAL_CASING.get()))
                        .where("E", Predicates.blocks(Registries.getBlock("kubejs:neutronium_pipe_casing")))
                        .where("F", Predicates.blocks(Registries.getBlock("gtceu:bronze_pipe_casing")))
                        .where("G", Predicates.blocks(Registries.getBlock("gtceu:ptfe_pipe_casing")))
                        .where(" ", Predicates.any())
                        .build())
                .additionalDisplay(GTLAddMultiBlockMachineModifier.INT_MAX_PARALLEL)
                .workableCasingRenderer(GTLCore.id("block/multi_functional_casing"), GTCEu.id("block/multiblock/gcym/large_assembler"))
                .register();

        SUPER_FACTORY_MKII = GTLAddRegistration.REGISTRATE.multiblock("super_factory_mk2", GTLAddWorkableElectricMultipleRecipesMachine::new)
                .allRotation()
                .tooltipText("最大并行数：2147483647")
                .tooltipTextLaser()
                .tooltipTextSuperMultiRecipes()
                .tooltipText("可用配方类型：碎岩机, 洗矿机，离心机，电解机，筛选机，研磨机，脱水机，热力离心机，电磁选矿机")
                .tooltipTextAdd()
                .recipeType(GTRecipeTypes.ROCK_BREAKER_RECIPES)// 碎岩机
                .recipeType(GTRecipeTypes.ORE_WASHER_RECIPES)// 洗矿机
                .recipeType(GTRecipeTypes.CENTRIFUGE_RECIPES)// 离心机
                .recipeType(GTRecipeTypes.ELECTROLYZER_RECIPES)// 电解机
                .recipeType(GTRecipeTypes.SIFTER_RECIPES)// 筛选机
                .recipeType(GTRecipeTypes.MACERATOR_RECIPES)// 研磨机
                .recipeType(GTLRecipeTypes.DEHYDRATOR_RECIPES)// 脱水机
                .recipeType(GTRecipeTypes.THERMAL_CENTRIFUGE_RECIPES)// 热力离心机
                .recipeType(GTRecipeTypes.ELECTROMAGNETIC_SEPARATOR_RECIPES)// 电磁选矿机
                .appearanceBlock(GTLBlocks.MULTI_FUNCTIONAL_CASING)
                .pattern((definition) -> MultiBlockStructure.ARCANIC_ASTROGRAPH_STRUCTURE
                        .where("~", Predicates.controller(Predicates.blocks(definition.get())))
                        .where("A", Predicates.blocks(GTLBlocks.MULTI_FUNCTIONAL_CASING.get()))
                        .where("B", Predicates.blocks(GTLBlocks.MULTI_FUNCTIONAL_CASING.get())
                                .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1)))
                        .where("D", Predicates.blocks(GTLBlocks.MULTI_FUNCTIONAL_CASING.get()))
                        .where("E", Predicates.blocks(Registries.getBlock("kubejs:neutronium_pipe_casing")))
                        .where("F", Predicates.blocks(Registries.getBlock("gtceu:bronze_pipe_casing")))
                        .where("G", Predicates.blocks(Registries.getBlock("gtceu:ptfe_pipe_casing")))
                        .where(" ", Predicates.any())
                        .build())
                .additionalDisplay(GTLAddMultiBlockMachineModifier.INT_MAX_PARALLEL)
                .workableCasingRenderer(GTLCore.id("block/multi_functional_casing"), GTCEu.id("block/multiblock/gcym/large_assembler"))
                .register();

        SUPER_FACTORY_MKIII = GTLAddRegistration.REGISTRATE.multiblock("super_factory_mk3", GTLAddWorkableElectricMultipleRecipesMachine::new)
                .allRotation()
                .tooltipText("最大并行数：2147483647")
                .tooltipTextLaser()
                .tooltipTextSuperMultiRecipes()
                .tooltipText("可用配方类型：蒸发, 高压釜, 提取机, 酿造机, 发酵槽, 蒸馏室, 蒸馏塔, 流体加热机, 流体固化机, 化学浸洗机")
                .tooltipTextAdd()
                .recipeType(GTRecipeTypes.EVAPORATION_RECIPES)// 蒸发
                .recipeType(GTRecipeTypes.AUTOCLAVE_RECIPES)// 高压釜
                .recipeType(GTRecipeTypes.EXTRACTOR_RECIPES)// 提取机
                .recipeType(GTRecipeTypes.BREWING_RECIPES)// 酿造机
                .recipeType(GTRecipeTypes.FERMENTING_RECIPES)// 发酵槽
                .recipeType(GTRecipeTypes.DISTILLERY_RECIPES)// 蒸馏室
                .recipeType(GTRecipeTypes.DISTILLATION_RECIPES)// 蒸馏塔
                .recipeType(GTRecipeTypes.FLUID_HEATER_RECIPES)// 流体加热机
                .recipeType(GTRecipeTypes.FLUID_SOLIDFICATION_RECIPES)// 流体固化机
                .recipeType(GTRecipeTypes.CHEMICAL_BATH_RECIPES)// 化学浸洗机
                .appearanceBlock(GTLBlocks.MULTI_FUNCTIONAL_CASING)
                .pattern((definition) -> MultiBlockStructure.ARCANIC_ASTROGRAPH_STRUCTURE
                        .where("~", Predicates.controller(Predicates.blocks(definition.get())))
                        .where("A", Predicates.blocks(GTLBlocks.MULTI_FUNCTIONAL_CASING.get()))
                        .where("B", Predicates.blocks(GTLBlocks.MULTI_FUNCTIONAL_CASING.get())
                                .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1)))
                        .where("D", Predicates.blocks(GTLBlocks.MULTI_FUNCTIONAL_CASING.get()))
                        .where("E", Predicates.blocks(Registries.getBlock("kubejs:neutronium_pipe_casing")))
                        .where("F", Predicates.blocks(Registries.getBlock("gtceu:bronze_pipe_casing")))
                        .where("G", Predicates.blocks(Registries.getBlock("gtceu:ptfe_pipe_casing")))
                        .where(" ", Predicates.any())
                        .build())
                .additionalDisplay(GTLAddMultiBlockMachineModifier.INT_MAX_PARALLEL)
                .workableCasingRenderer(GTLCore.id("block/multi_functional_casing"), GTCEu.id("block/multiblock/gcym/large_assembler"))
                .register();

        SUPER_FACTORY_MKIV = GTLAddRegistration.REGISTRATE.multiblock("super_factory_mk4", GTLAddWorkableElectricMultipleRecipesMachine::new)
                .allRotation()
                .tooltipText("最大并行数：2147483647")
                .tooltipTextLaser()
                .tooltipTextSuperMultiRecipes()
                .tooltipText("可用配方类型：装罐机, 电弧炉, 闪电处理, 组装机, 精密组装, 电路组装机")
                .tooltipTextAdd()
                .recipeType(GTRecipeTypes.CANNER_RECIPES)// 装罐机
                .recipeType(GTRecipeTypes.ARC_FURNACE_RECIPES)// 电弧炉
                .recipeType(GTLRecipeTypes.LIGHTNING_PROCESSOR_RECIPES)// 闪电处理
                .recipeType(GTRecipeTypes.ASSEMBLER_RECIPES)// 组装机
                .recipeType(GTLRecipeTypes.PRECISION_ASSEMBLER_RECIPES)// 精密组装
                .recipeType(GTRecipeTypes.CIRCUIT_ASSEMBLER_RECIPES)// 电路组装机
                .appearanceBlock(GTLBlocks.MULTI_FUNCTIONAL_CASING)
                .pattern((definition) -> MultiBlockStructure.ARCANIC_ASTROGRAPH_STRUCTURE
                        .where("~", Predicates.controller(Predicates.blocks(definition.get())))
                        .where("A", Predicates.blocks(GTLBlocks.MULTI_FUNCTIONAL_CASING.get()))
                        .where("B", Predicates.blocks(GTLBlocks.MULTI_FUNCTIONAL_CASING.get())
                                .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1)))
                        .where("D", Predicates.blocks(GTLBlocks.MULTI_FUNCTIONAL_CASING.get()))
                        .where("E", Predicates.blocks(Registries.getBlock("kubejs:neutronium_pipe_casing")))
                        .where("F", Predicates.blocks(Registries.getBlock("gtceu:bronze_pipe_casing")))
                        .where("G", Predicates.blocks(Registries.getBlock("gtceu:ptfe_pipe_casing")))
                        .where(" ", Predicates.any())
                        .build())
                .additionalDisplay(GTLAddMultiBlockMachineModifier.INT_MAX_PARALLEL)
                .workableCasingRenderer(GTLCore.id("block/multi_functional_casing"), GTCEu.id("block/multiblock/gcym/large_assembler"))
                .register();

        ATOMIC_TRANSMUTATIOON_CORE = GTLAddRegistration.REGISTRATE.multiblock("atomic_transmutation_core", CoilWorkableElectricMultipleRecipesMultiblockMachine::new)
                .noneRotation()
                .tooltipTextCoilParallel()
                .tooltipTextLaser()
                .tooltipTextMultiRecipes()
                .tooltipText("可用配方类型：电磁共振转化场")
                .tooltipTextAdd()
                .coilparalleldisplay()
                .recipeType(GTLAddRecipesTypes.EM_RESONANCE_CONVERSION_FIELD)
                .appearanceBlock(GTLBlocks.ALUMINIUM_BRONZE_CASING)
                .pattern((definition) -> FactoryBlockPattern.start()
                        .aisle("AAAAAAAAA", "AAAAAAAAA", "ABBBBBBBA", "ABBBBBBBA", "ABBBBBBBA", "ABBBBBBBA", "AAAAAAAAA")
                        .aisle("AAAAAAAAA", "ACCCCCCCA", "B       B", "B       B", "B       B", "B       B", "AAAAAAAAA")
                        .aisle("AAAAAAAAA", "ACDDDDDCA", "B       B", "B       B", "B       B", "B       B", "AAAAAAAAA")
                        .aisle("AAAAAAAAA", "ACDDDDDCA", "B       B", "B       B", "B       B", "B       B", "AAAAAAAAA")
                        .aisle("AAAAAAAAA", "ACDDDDDCA", "B       B", "B       B", "B       B", "B       B", "AAAA~AAAA")
                        .aisle("AAAAAAAAA", "ACDDDDDCA", "B       B", "B       B", "B       B", "B       B", "AAAAAAAAA")
                        .aisle("AAAAAAAAA", "ACDDDDDCA", "B       B", "B       B", "B       B", "B       B", "AAAAAAAAA")
                        .aisle("AAAAAAAAA", "ACCCCCCCA", "B       B", "B       B", "B       B", "B       B", "AAAAAAAAA")
                        .aisle("AAAAAAAAA", "AAAAAAAAA", "ABBBBBBBA", "ABBBBBBBA", "ABBBBBBBA", "ABBBBBBBA", "AAAAAAAAA")
                        .where("~", Predicates.controller(Predicates.blocks(definition.get())))
                        .where("A", Predicates.blocks(Registries.getBlock("gtlcore:aluminium_bronze_casing"))
                                .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1)))
                        .where("C", Predicates.heatingCoils())
                        .where("D", Predicates.blocks(Registries.getBlock("kubejs:infused_obsidian")))
                        .where("B", Predicates.blocks(Registries.getBlock("gtceu:cleanroom_glass")))
                        .where(" ", Predicates.any())
                        .build())
                .workableCasingRenderer(GTLCore.id("block/casings/aluminium_bronze_casing"), GTCEu.id("block/multiblock/cleanroom"))
                .register();

        LUCID_ETCHDREAMER = GTLAddRegistration.REGISTRATE.multiblock("lucid_etchdreamer", GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine::new)
                .nonYAxisRotation()
                .tooltipTextCoilParallel()
                .tooltipTextLaser()
                .tooltipTextSuperMultiRecipes()
                .tooltipText("可用配方类型：光子晶阵蚀刻")
                .tooltipTextAdd()
                .coilparalleldisplay()
                .recipeType(GTLAddRecipesTypes.PHOTON_MATRIX_ETCH)
                .appearanceBlock(GTLBlocks.IRIDIUM_CASING)
                .pattern(definition -> MultiBlockStructure.LUCID_ETCHDREAMER_STRUCTURE
                        .where("I", Predicates.controller(Predicates.blocks(definition.get())))
                        .where("A", Predicates.blocks(Registries.getBlock("gtlcore:iridium_casing"))
                                .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1)))
                        .where("D", Predicates.heatingCoils())
                        .where("E", Predicates.blocks(Registries.getBlock("kubejs:neutronium_pipe_casing")))
                        .where("B", Predicates.blocks(Registries.getBlock("gtlcore:dimensionally_transcendent_casing")))
                        .where("C", Predicates.blocks(Registries.getBlock("gtlcore:dimension_injection_casing")))
                        .where("E", Predicates.blocks(Registries.getBlock("kubejs:neutronium_pipe_casing")))
                        .where("G", Predicates.blocks(Registries.getBlock("gtlcore:graviton_field_constraint_casing")))
                        .where("F", Predicates.blocks(Registries.getBlock("gtceu:cleanroom_glass")))
                        .where("H", Predicates.blocks(Registries.getBlock("kubejs:annihilate_core")))
                        .where(" ", Predicates.any())
                        .build())
                .additionalDisplay(GTLAddMultiBlockMachineModifier.MULTIPLERECIPES_COIL_PARALLEL)
                .workableCasingRenderer(GTLCore.id("block/casings/iridium_casing"), GTCEu.id("block/multiblock/gcym/large_engraving_laser"))
                .register();

        ASTRAL_CONVERGENCE_NEXUS = GTLAddRegistration.REGISTRATE.multiblock("astral_convergence_nexus", (holder) -> new AdvancedSpaceElevatorModuleMachine(holder, true))
                .nonYAxisRotation()
                .tooltipText("耗能倍数：0.75")
                .tooltipText("最大并行数：8^(动力模块等级-1)")
                .tooltipTextLaser()
                .tooltipTextSuperMultiRecipes()
                .tooltipText("可用配方类型：太空组装")
                .tooltipTextAdd()
                .recipeType(GTLRecipeTypes.ASSEMBLER_MODULE_RECIPES)// 太空组装
                .recipeModifier(AdvancedSpaceElevatorModuleMachine::recipeModifier)
                .appearanceBlock(GTLBlocks.SPACE_ELEVATOR_MECHANICAL_CASING).pattern((definition) -> FactoryBlockPattern.start()
                        .aisle("aaa", "bcb", "bbb", "bbb", "bbb")
                        .aisle("aaa", "bbb", "bbb", "bbb", "bbb")
                        .aisle("aaa", "bbb", "bbb", "b~b", "bbb")
                        .where("~", Predicates.controller(Predicates.blocks(definition.get())))
                        .where("b", Predicates.blocks(GTLBlocks.SPACE_ELEVATOR_MECHANICAL_CASING.get())
                                .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                                .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1)))
                        .where("a", Predicates.blocks(Registries.getBlock("kubejs:module_base")))
                        .where("c", Predicates.blocks(Registries.getBlock("kubejs:module_connector")))
                        .build())
                .beforeWorking(AdvancedSpaceElevatorModuleMachine::beforeWorking)
                .workableCasingRenderer(GTLCore.id("block/space_elevator_mechanical_casing"), GTCEu.id("block/multiblock/gcym/large_assembler"))
                .register();

        NEBULA_REAPER = GTLAddRegistration.REGISTRATE.multiblock("nebula_reaper", (holder) -> new AdvancedSpaceElevatorModuleMachine(holder, true))
                .nonYAxisRotation()
                .tooltipText("耗能倍数：0.75")
                .tooltipText("最大并行数：8^(动力模块等级-1)")
                .tooltipTextLaser()
                .tooltipTextSuperMultiRecipes()
                .tooltipText("可用配方类型：太空采矿、太空钻井")
                .tooltipTextAdd()
                .recipeType(GTLRecipeTypes.MINER_MODULE_RECIPES)// 太空采矿
                .recipeType(GTLRecipeTypes.DRILLING_MODULE_RECIPES)// 太空钻井
                .recipeModifier(AdvancedSpaceElevatorModuleMachine::recipeModifier)
                .appearanceBlock(GTLBlocks.SPACE_ELEVATOR_MECHANICAL_CASING).pattern((definition) -> FactoryBlockPattern.start()
                        .aisle("aaa", "bcb", "bbb", "bbb", "bbb")
                        .aisle("aaa", "bbb", "bbb", "bbb", "bbb")
                        .aisle("aaa", "bbb", "bbb", "b~b", "bbb")
                        .where("~", Predicates.controller(Predicates.blocks(definition.get())))
                        .where("b", Predicates.blocks(GTLBlocks.SPACE_ELEVATOR_MECHANICAL_CASING.get())
                                .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                                .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1)))
                        .where("a", Predicates.blocks(Registries.getBlock("kubejs:module_base")))
                        .where("c", Predicates.blocks(Registries.getBlock("kubejs:module_connector")))
                        .build())
                .beforeWorking(AdvancedSpaceElevatorModuleMachine::beforeWorking)
                .workableCasingRenderer(GTLCore.id("block/space_elevator_mechanical_casing"), GTCEu.id("block/multiblock/gcym/large_assembler"))
                .register();

        ARCANIC_ASTROGRAPH = GTLAddRegistration.REGISTRATE.multiblock("arcanic_astrograph", AdvancedHarmonyMachine::new)
                .nonYAxisRotation()
                .recipeType(GTLRecipeTypes.COSMOS_SIMULATION_RECIPES)
                .recipeModifier(AdvancedHarmonyMachine::recipeModifier)
                .tooltips(new Component[] { Component.literal("最大并行数：256") })
                .tooltips(new Component[] { Component.translatable("gtceu.machine.eye_of_harmony.tooltip.0") })
                .tooltips(new Component[] { Component.translatable("gtceu.machine.eye_of_harmony.tooltip.1") })
                .tooltips(new Component[] { Component.translatable("gtceu.machine.eye_of_harmony.tooltip.2") })
                .tooltips(new Component[] { Component.translatable("gtceu.machine.eye_of_harmony.tooltip.3") })
                .tooltips(new Component[] { Component.translatable("gtceu.machine.eye_of_harmony.tooltip.4") })
                .tooltips(new Component[] { Component.translatable("gtceu.machine.eye_of_harmony.tooltip.5") })
                .tooltips(new Component[] { Component.translatable("gtceu.machine.eye_of_harmony.tooltip.6") })
                .tooltips(new Component[] { Component.translatable("gtceu.machine.available_recipe_map_1.tooltip", Component.translatable("gtceu.cosmos_simulation")) })
                .tooltips(Component.literal(TextUtil.full_color("由GTLADDitions添加")))
                .appearanceBlock(GTBlocks.HIGH_POWER_CASING)
                .pattern((definition) -> MultiBlockStructure.ARCANIC_ASTROGRAPH_STRUCTURE
                        .where('~', Predicates.controller(Predicates.blocks(definition.get())))
                        .where('A', Predicates.blocks(GTLBlocks.CREATE_CASING.get()))
                        .where('B', Predicates.blocks(GTBlocks.HIGH_POWER_CASING.get())
                                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1))
                                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMaxGlobalLimited(1))
                                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setPreviewCount(1))
                                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1)))
                        .where('D', Predicates.blocks(GTLBlocks.DIMENSION_INJECTION_CASING.get()))
                        .where('E', Predicates.blocks(Registries.getBlock("kubejs:dimension_creation_casing")))
                        .where('F', Predicates.blocks(Registries.getBlock("kubejs:spacetime_compression_field_generator")))
                        .where('G', Predicates.blocks(Registries.getBlock("kubejs:dimensional_stability_casing")))
                        .where(" ", Predicates.any())
                        .build())
                .renderer(EyeOfHarmonyRenderer::new)
                .hasTESR(true)
                .register();

        ARCANE_CACHE_VAULT = GTLAddRegistration.REGISTRATE.multiblock("arcane_cache_vault", CoilWorkableElectricMultipleRecipesMultiblockMachine::new)
                .allRotation()
                .tooltipTextCoilParallel()
                .tooltipTextLaser()
                .tooltipTextMultiRecipes()
                .tooltipText("可用配方类型：打包机")
                .tooltipTextAdd()
                .coilparalleldisplay()
                .recipeType(GTRecipeTypes.PACKER_RECIPES)
                .appearanceBlock(GTLBlocks.PIKYONIUM_MACHINE_CASING)
                .pattern(definition -> FactoryBlockPattern.start()
                        .aisle("AAA", "AAA", "AAA")
                        .aisle("AAA", "ABA", "AAA")
                        .aisle("AAA", "ABA", "AAA")
                        .aisle("AAA", "ABA", "AAA")
                        .aisle("AAA", "ABA", "AAA")
                        .aisle("AAA", "A~A", "AAA")
                        .where("~", Predicates.controller(Predicates.blocks(definition.get())))
                        .where("A", Predicates.blocks(GTLBlocks.PIKYONIUM_MACHINE_CASING.get())
                                .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1)))
                        .where("B", Predicates.heatingCoils())
                        .build())
                .workableCasingRenderer(GTLCore.id("block/casings/pikyonium_machine_casing"), GTCEu.id("block/multiblock/gcym/large_packer"))
                .register();

        DRACONIC_COLLAPSE_CORE = GTLAddRegistration.REGISTRATE.multiblock("draconic_collapse_core", WorkableElectricMultiblockMachine::new)
                .nonYAxisRotation()
                .tooltipText("电压等级每高出UEV一级最大并行数X8")
                .tooltipTextLaser()
                .tooltipTextPerfectOverclock()
                .tooltipText("可用配方类型：聚合装置")
                .tooltipTextAdd()
                .recipeType(GTLRecipeTypes.AGGREGATION_DEVICE_RECIPES)
                .recipeModifiers(GTLAddMultiBlockMachineModifier.DRACONIC_COLLAPSE_CORE_MODIFIER)
                .appearanceBlock(GTBlocks.FUSION_CASING)
                .pattern(definition -> MultiBlockStructure.DRACONIC_COLLAPSE_CORE_STRUCTURE
                        .where("E", Predicates.controller(Predicates.blocks(definition.get())))
                        .where("D", Predicates.blocks(GTLFusionCasingBlock.getCasingState(10))
                                .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(2)).setMinGlobalLimited(1))
                        .where("L", Predicates.blocks(GTLFusionCasingBlock.getCasingState(10))
                                .or(Predicates.blocks(GTMachines.ITEM_IMPORT_BUS[0].get()))
                                .or(Predicates.blocks(CustomMachines.HUGE_ITEM_IMPORT_BUS[0].get())))
                        .where("I", Predicates.blocks(Registries.getBlock("gtlcore:molecular_casing")))
                        .where("K", Predicates.blocks(Registries.getBlock("kubejs:annihilate_core")))
                        .where("J", Predicates.blocks(Registries.getBlock("kubejs:aggregatione_core")))
                        .where("F", Predicates.blocks(Registries.getBlock("gtlcore:dimensionally_transcendent_casing")))
                        .where("B", Predicates.blocks(Registries.getBlock("gtceu:neutronium_frame")))
                        .where("A", Predicates.blocks(Registries.getBlock("gtlcore:dimension_injection_casing")))
                        .where("C", Predicates.blocks(GTLFusionCasingBlock.getCasingState(10)))
                        .where("H", Predicates.blocks(Registries.getBlock("kubejs:hollow_casing")))
                        .where("G", Predicates.blocks(GTLFusionCasingBlock.getCompressedCoilState(10)))
                        .where("O", Predicates.blocks(GTLFusionCasingBlock.getCasingState(10))
                                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS)))
                        .where(" ", Predicates.any())
                        .build())
                .additionalDisplay(GTLAddMultiBlockMachineModifier.DRACONIC_COLLAPSE_CORE_ADDTEXT)
                .workableCasingRenderer(GTLFusionCasingBlock.getCasingType(10).getTexture(), GTCEu.id("block/multiblock/fusion_reactor"))
                .register();

        TITAN_CRIP_EARTHBORE = GTLAddRegistration.REGISTRATE.multiblock("titan_crip_earthbore", (holder) -> new StorageMachine(holder, 64))
                .noneRotation()
                .tooltipText("在主机中放入基岩钻头可获得对应数量X4的并行")
                .tooltipTextPerfectOverclock()
                .tooltipText("可用配方类型：地脉断层发生器")
                .tooltipTextAdd()
                .recipeType(GTLAddRecipesTypes.TECTONIC_FAULT_GENERATOR)
                .recipeModifiers(GTLAddMultiBlockMachineModifier.TITAN_CRIP_EARTHBORE_MODIFIER)
                .appearanceBlock(GTLBlocks.ECHO_CASING)
                .pattern(definition -> MultiBlockStructure.TITAN_CRIP_EARTHBORE_STRUCTURE
                        .where("~", Predicates.controller(Predicates.blocks(definition.get())))
                        .where("I", Predicates.blocks(Registries.getBlock("kubejs:neutronium_gearbox")))
                        .where("H", Predicates.blocks(Registries.getBlock("kubejs:neutronium_pipe_casing")))
                        .where("G", Predicates.blocks(Registries.getBlock("kubejs:machine_casing_grinding_head")))
                        .where("B", Predicates.blocks(Registries.getBlock("gtceu:neutronium_frame")))
                        .where("C", Predicates.blocks(Registries.getBlock("gtlcore:echo_casing")))
                        .where("A", Predicates.blocks(Registries.getBlock("gtlcore:molecular_casing")))
                        .where("F", Predicates.blocks(Registries.getBlock("minecraft:bedrock")))
                        .where("D", Predicates.blocks(Registries.getBlock("kubejs:molecular_coil")))
                        .where("E", Predicates.blocks(Registries.getBlock("gtlcore:echo_casing"))
                                .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1)))
                        .build())
                .workableCasingRenderer(GTLCore.id("block/casings/echo_casing"), GTCEu.id("block/multiblock/cleanroom"))
                .register();

        BIOLOGICAL_SIMULATION_LABORATORY = GTLAddRegistration.REGISTRATE.multiblock("biological_simulation_laboratory", BiologicalSimulationLaboratory::new)
                .allRotation()
                .tooltipText("更高效的获取生物掉落物")
                .tooltipText("初始最大并行数为64")
                .tooltipText("可以在主机中放入不同物品获得不同的加成")
                .tooltipText("§c铼纳米蜂群§r（§o并行§r：512，§o耗电§r：0.9，§o耗时§r：0.9）")
                .tooltipText("§c山铜纳米蜂群§r（§o并行§r：4096，§o耗电§r：0.8，§o耗时§r：0.6）")
                .tooltipText("§c魔金纳米蜂群§r（§o并行§r：16384，§o耗电§r：0.6，§o耗时§r：0.4）")
                .tooltipText("§c不再是菜鸟的证明§r（§o并行§r：4194304，§o耗电§r：0.25，§o耗时§r：0.1）")
                .tooltipText("放入§c不再是菜鸟的证明§r时解锁寰宇支配之剑的配方")
                .tooltipText("放入§c不再是菜鸟的证明§r时解锁跨配方·改")
                .tooltipText("只能使用UXV以上的激光仓")
                .tooltipText("可用配方类型：生物数据模拟")
                .tooltipTextAdd()
                .recipeType(GTLAddRecipesTypes.BIOLOGICAL_SIMULATION)
                .recipeModifier(BiologicalSimulationLaboratory::recipeModifier)
                .appearanceBlock(GTLBlocks.NAQUADAH_ALLOY_CASING)
                .pattern(definition -> MultiBlockStructure.BIOLOGICAL_SIMULATION_LABORATORY_STRUCTURE
                        .where("~", Predicates.controller(Predicates.blocks(definition.get())))
                        .where("A", Predicates.blocks(Registries.getBlock("gtlcore:naquadah_alloy_casing"))
                                .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(Predicates.blocks(PartAbility.INPUT_LASER.getBlockRange(12, 14).toArray(new Block[0])).setMaxGlobalLimited(1)))
                        .where("B", Predicates.blocks(Registries.getBlock("gtceu:naquadah_alloy_frame")))
                        .where("C", Predicates.blocks(Registries.getBlock("gtceu:luv_hermetic_casing")))
                        .where("E", Predicates.blocks(Registries.getBlock("gtceu:fusion_glass")))
                        .where("G", Predicates.blocks(Registries.getBlock("gtceu:computer_heat_vent")))
                        .where("D", Predicates.blocks(Registries.getBlock("gtceu:advanced_computer_casing")))
                        .where("H", Predicates.blocks(Registries.getBlock("gtceu:sterilizing_filter_casing")))
                        .where("F", Predicates.blocks(Registries.getBlock("gtceu:zpm_hermetic_casing")))
                        .build())
                .beforeWorking(BiologicalSimulationLaboratory::beforeWorking)
                .workableCasingRenderer(GTLCore.id("block/casings/hyper_mechanical_casing"), GTCEu.id("block/multiblock/fusion_reactor"))
                .register();

        DIMENSIONALLY_TRANSCENDENT_CHEMICAL_PLANT = GTLAddRegistration.REGISTRATE.multiblock("dimensionally_transcendent_chemical_plant", GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine::new)
                .nonYAxisRotation()
                .tooltipText("高效的化学反应堆")
                .tooltipTextCoilParallel()
                .tooltipTextLaser()
                .tooltipTextSuperMultiRecipes()
                .tooltipText("可用配方类型：大型化学反应釜")
                .tooltipTextAdd()
                .recipeType(GTRecipeTypes.LARGE_CHEMICAL_RECIPES)
                .appearanceBlock(GTBlocks.CASING_PTFE_INERT)
                .pattern((definition) -> GTLMachines.DTPF
                        .where("a", Predicates.controller(Predicates.blocks(definition.get())))
                        .where("e", Predicates.blocks(GTBlocks.CASING_PTFE_INERT.get())
                                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1))
                                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1))
                                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setPreviewCount(1))
                                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1))
                                .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMaxGlobalLimited(2))
                                .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1)))
                        .where("b", Predicates.blocks(GTBlocks.HIGH_POWER_CASING.get()))
                        .where("C", Predicates.heatingCoils())
                        .where("d", Predicates.blocks(GTBlocks.CASING_PTFE_INERT.get()))
                        .where("s", Predicates.blocks(Registries.getBlock("gtceu:ptfe_pipe_casing")))
                        .where(" ", Predicates.any())
                        .build())
                .additionalDisplay(GTLAddMultiBlockMachineModifier.MULTIPLERECIPES_COIL_PARALLEL)
                .workableCasingRenderer(GTCEu.id("block/casings/solid/machine_casing_inert_ptfe"), GTCEu.id("block/machines/chemical_reactor"))
                .register();

        QUANTUM_SYPHON_MATRIX = GTLAddRegistration.REGISTRATE.multiblock("quantum_syphon_matrix", GTLAddWorkableElectricParallelHatchMultipleRecipesMachine::new)
                .noneRotation()
                .tooltipTextParallelHatch()
                .tooltipTextLaser()
                .tooltipTextSuperMultiRecipes()
                .tooltipText("可用配方类型：虚空聚流反应")
                .tooltipTextAdd()
                .recipeType(GTLAddRecipesTypes.VOIDFLUX_REACTION)
                .recipeModifier(GTRecipeModifiers.PARALLEL_HATCH)
                .appearanceBlock(GTBlocks.HIGH_POWER_CASING)
                .pattern(definition -> MultiBlockStructure.QUANTUM_SYPHON_MATRIX_STRUCTURE
                        .where("~", Predicates.controller(Predicates.blocks(definition.get())))
                        .where("C", Predicates.blocks(Registries.getBlock("gtceu:neutronium_frame")))
                        .where("G", Predicates.blocks(Registries.getBlock("kubejs:accelerated_pipeline")))
                        .where("D", Predicates.blocks(Registries.getBlock("gtlcore:molecular_casing")))
                        .where("H", Predicates.blocks(Registries.getBlock("kubejs:neutronium_gearbox")))
                        .where("F", Predicates.blocks(GTBlocks.HIGH_POWER_CASING.get())
                                .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                                .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(1))
                                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1)))
                        .where("J", Predicates.blocks(Registries.getBlock("kubejs:neutronium_pipe_casing")))
                        .where("A", Predicates.blocks(Registries.getBlock("gtlcore:naquadah_alloy_casing")))
                        .where("B", Predicates.blocks(Registries.getBlock("gtceu:assembly_line_grating")))
                        .where("I", Predicates.blocks(Registries.getBlock("gtceu:uhv_hermetic_casing")))
                        .where("E", Predicates.blocks(Registries.getBlock("kubejs:hollow_casing")))
                        .where(" ", Predicates.any())
                        .build())
                .workableCasingRenderer(GTCEu.id("block/casings/hpca/high_power_casing"), GTCEu.id("block/machines/gas_collector"))
                .register();

        FUXI_BAGUA_HEAVEN_FORGING_FURNACE = GTLAddRegistration.REGISTRATE.multiblock("fuxi_bagua_heaven_forging_furnace", GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine::new)
                .nonYAxisRotation()
                .tooltipTextCoilParallel()
                .tooltipTextLaser()
                .tooltipTextSuperMultiRecipes()
                .tooltipText("可用配方类型：星焰跃迁")
                .tooltipTextAdd()
                .recipeType(GTLAddRecipesTypes.STELLAR_LGNITION)
                .appearanceBlock(GTLBlocks.DIMENSION_INJECTION_CASING)
                .pattern(definition -> MultiBlockStructure.FUXI_BAGUA_HEAVEN_FORGING_FURNACE_STRUCTURE
                        .where("D", Predicates.controller(Predicates.blocks(definition.get())))
                        .where("K", Predicates.blocks(Registries.getBlock("kubejs:neutronium_pipe_casing")))
                        .where("C", Predicates.blocks(GTLBlocks.DIMENSION_INJECTION_CASING.get())
                                .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1))
                                .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1))
                                .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setPreviewCount(1))
                                .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1))
                                .or(Predicates.abilities(PartAbility.INPUT_LASER).setMaxGlobalLimited(2)))
                        .where("X", Predicates.heatingCoils())
                        .where("J", Predicates.blocks(Registries.getBlock("kubejs:dimensional_bridge_casing")))
                        .where("F", Predicates.blocks(Registries.getBlock("gtlcore:graviton_field_constraint_casing")))
                        .where("I", Predicates.blocks(Registries.getBlock("kubejs:molecular_coil")))
                        .where("A", Predicates.blocks(Registries.getBlock("gtceu:atomic_casing")))
                        .where("G", Predicates.blocks(Registries.getBlock("gtlcore:degenerate_rhenium_constrained_casing")))
                        .where("N", Predicates.blocks(Registries.getBlock("gtlcore:ultimate_stellar_containment_casing")))
                        .where("B", Predicates.blocks(Registries.getBlock("gtlcore:dimension_injection_casing")))
                        .where("E", Predicates.blocks(Registries.getBlock("kubejs:dimension_creation_casing")))
                        .where("H", Predicates.blocks(Registries.getBlock("kubejs:spacetime_compression_field_generator")))
                        .where("L", Predicates.blocks(Registries.getBlock("gtlcore:compressed_fusion_coil_mk2_prototype")))
                        .where("M", Predicates.blocks(Registries.getBlock("kubejs:dimensional_stability_casing")))
                        .where("O", Predicates.blocks(Registries.getBlock("kubejs:restraint_device")))
                        .build())
                .additionalDisplay(GTLAddMultiBlockMachineModifier.MULTIPLERECIPES_COIL_PARALLEL)
                .workableCasingRenderer(GTLCore.id("block/casings/dimension_injection_casing"), GTCEu.id("block/multiblock/fusion_reactor"))
                .register();

    }
}
