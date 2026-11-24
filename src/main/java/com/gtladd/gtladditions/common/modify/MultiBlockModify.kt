package com.gtladd.gtladditions.common.modify

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine
import com.gregtechceu.gtceu.api.pattern.Predicates
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic
import com.gregtechceu.gtceu.api.recipe.logic.OCParams
import com.gregtechceu.gtceu.api.recipe.logic.OCResult
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifierList
import com.gregtechceu.gtceu.common.data.GTBlocks
import com.gregtechceu.gtceu.common.data.GTMachines
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers
import com.gregtechceu.gtceu.common.data.machines.GTResearchMachines
import com.gregtechceu.gtceu.common.machine.multiblock.electric.ActiveTransformerMachine
import com.gtladd.gtladditions.api.machine.GTLAddPartAbility
import com.gtladd.gtladditions.api.machine.IThreadModifierMachine
import com.gtladd.gtladditions.api.pattern.PatternPredicateSelector
import com.gtladd.gtladditions.api.pattern.patchPatternPredicates
import com.gtladd.gtladditions.api.pattern.patternPredicateSelector
import com.gtladd.gtladditions.api.pattern.replacePatternPredicates
import com.gtladd.gtladditions.client.render.machine.QFTRenderer
import com.gtladd.gtladditions.client.render.machine.SpaceElevatorRenderer
import com.gtladd.gtladditions.common.machine.GTLAddMachines
import com.gtladd.gtladditions.common.machine.multiblock.controller.BasicOreProcessorMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.MolecularAssemblerMultiblockMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.WeatherControlMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.mutable.AdvancedInfiniteDrillMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.mutable.CreateAggregation
import com.gtladd.gtladditions.common.machine.multiblock.controller.mutable.DoorOfCreate
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.lowdragmc.lowdraglib.LDLib
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import org.gtlcore.gtlcore.api.machine.IGTLRenderDisabled
import org.gtlcore.gtlcore.common.data.GTLBlocks
import org.gtlcore.gtlcore.common.data.machines.AdditionalMultiBlockMachine
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachineA
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachineB
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineA
import org.gtlcore.gtlcore.utils.MachineIO
import org.gtlcore.gtlcore.utils.MachineUtil
import org.gtlcore.gtlcore.utils.Registries
import org.gtlcore.gtlcore.utils.Registries.getItem
import java.util.function.Predicate
import net.minecraft.core.registries.Registries as MinecraftRegistries

@Suppress("DuplicatedCode")
object MultiBlockModify {

    private val createDimension = ResourceKey.create(
        MinecraftRegistries.DIMENSION,
        ResourceLocation("kubejs", "create")
    )

    private val doorOfCreateOnWorking = Predicate { machine: IRecipeLogicMachine ->
        if (machine.recipeLogic.progress == 5 && machine is DoorOfCreate) {
            (machine.level as? ServerLevel)?.let { level ->
                val pos = machine.self().pos.offset(0, -13, 0)

                level.sendParticles(
                    ParticleTypes.DRAGON_BREATH,
                    pos.x.toDouble(),
                    pos.y.toDouble(),
                    pos.z.toDouble(),
                    1000,
                    4.0,
                    4.0,
                    4.0,
                    0.01
                )

                val entities = level.getEntitiesOfClass(
                    Entity::class.java,
                    AABB(
                        pos.x - 10.0,
                        pos.y - 10.0,
                        pos.z - 10.0,
                        pos.x + 10.0,
                        pos.y + 10.0,
                        pos.z + 10.0
                    )
                )

                for (entity in entities) {
                    if (entity is ItemEntity) {
                        when {
                            entity.item.`kjs$getId`() == "gtceu:magnetohydrodynamicallyconstrainedstarmatter_block" -> {
                                MachineUtil.createItemEntity(
                                    level,
                                    entity.x,
                                    entity.y,
                                    entity.z,
                                    ItemStack(Blocks.COMMAND_BLOCK, entity.item.count)
                                )
                                entity.discard()
                            }

                            entity.item.`kjs$getId`() == "gtceu:magmatter_ingot" && entity.item.count >= 64 -> {
                                MachineUtil.createItemEntity(
                                    level,
                                    entity.x,
                                    entity.y,
                                    entity.z,
                                    ItemStack(getItem("gtceu:magmatter_block"), entity.item.count / 64)
                                )
                                entity.discard()
                            }
                        }
                    } else if (entity is ServerPlayer) {
                        if (MachineUtil.hasFullArmorSet(entity)) {
                            level.server.getLevel(createDimension)?.let { targetLevel ->
                                entity.teleportTo(
                                    targetLevel,
                                    0.0,
                                    1.0,
                                    0.0,
                                    entity.yRot,
                                    entity.xRot
                                )
                            }
                        } else {
                            entity.`kjs$setStatusMessage`(
                                "message.gtlcore.equipment_incompatible_dimension".toComponent
                            )
                        }
                    }
                }
            }
        }
        true
    }

    private val createAggregationOnWorking = Predicate { machine: IRecipeLogicMachine ->
        if (machine.recipeLogic.getProgress() == 19 && machine is CreateAggregation) {
            machine.level?.let { level ->
                val pos = machine.self().pos.offset(0, -16, 0)
                val blockId = level.getBlockState(pos).block.`kjs$getId`()
                when {
                    blockId == "kubejs:command_block_broken" &&
                        MachineIO.inputItem(
                            machine as WorkableMultiblockMachine,
                            Registries.getItemStack("kubejs:chain_command_block_core")
                        ) -> level.setBlockAndUpdate(pos, Blocks.CHAIN_COMMAND_BLOCK.defaultBlockState())

                    blockId == "kubejs:chain_command_block_broken" &&
                        MachineIO.inputItem(
                            machine as WorkableMultiblockMachine,
                            Registries.getItemStack("kubejs:repeating_command_block_core")
                        ) -> level.setBlockAndUpdate(pos, Blocks.REPEATING_COMMAND_BLOCK.defaultBlockState())
                }
            }
        }
        true
    }

    private val recipeModifierList = RecipeModifierList(
        { machine: MetaMachine?, recipe: GTRecipe?, _: OCParams?, _: OCResult? ->
            GTRecipeModifiers.accurateParallel(
                machine,
                recipe!!,
                Ints.saturatedCast(1L + (machine as IThreadModifierMachine).getAdditionalThread()),
                false
            ).getFirst()
        },
        GTRecipeModifiers.ELECTRIC_OVERCLOCK.apply(OverclockingLogic(1.0, 1.0, false))
    )

    fun init() {
        if (LDLib.isClient()) {
            MultiBlockMachineA.QFT.renderer = QFTRenderer()
            AdvancedMultiBlockMachineB.SPACE_ELEVATOR.renderer = SpaceElevatorRenderer(climberHeight = 180.0, cableLength = 360, viewDistance = 512)
        }

        MultiBlockMachineA.QFT.setMachineSupplier { holder ->
            object : WorkableElectricMultiblockMachine(holder) {
                override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
                    super.attachConfigurators(configuratorPanel)
                    IGTLRenderDisabled.attachConfigurator(configuratorPanel, this as IGTLRenderDisabled)
                }
            }
        }

        MultiBlockMachineA.INTEGRATED_ORE_PROCESSOR.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            BasicOreProcessorMachine(blockEntity)
        }
        MultiBlockMachineA.INTEGRATED_ORE_PROCESSOR.recipeModifier = RecipeModifierList(
            GTRecipeModifiers.PARALLEL_HATCH,
            BasicOreProcessorMachine::recipeModifier
        )
        MultiBlockMachineA.INTEGRATED_ORE_PROCESSOR.patchPatternPredicates(
            "ore_processor_hatch",
            patternPredicateSelector({ GTBlocks.CASING_STAINLESS_CLEAN.get() }, PartAbility.PARALLEL_HATCH, PartAbility.MAINTENANCE),
            { Predicates.blocks(GTLAddMachines.ORE_PROCESSOR_HATCH.get()).setMaxGlobalLimited(1) }
        )
        MultiBlockMachineA.ADVANCED_INTEGRATED_ORE_PROCESSOR.patchPatternPredicates(
            "ore_processor_hatch",
            patternPredicateSelector({ GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get() }, PartAbility.INPUT_LASER),
            { Predicates.blocks(GTLAddMachines.ORE_PROCESSOR_HATCH.get()).setMaxGlobalLimited(1) }
        )

        val threadModifierPredicate = { Predicates.abilities(GTLAddPartAbility.THREAD_MODIFIER).setMaxGlobalLimited(1) }
        AdvancedMultiBlockMachineA.DOOR_OF_CREATE.patchPatternPredicates(
            "thread_modifier",
            patternPredicateSelector(
                { GTLBlocks.DIMENSION_CONNECTION_CASING.get() },
                PartAbility.IMPORT_ITEMS,
                PartAbility.EXPORT_ITEMS,
                PartAbility.INPUT_ENERGY
            ),
            threadModifierPredicate
        )
        AdvancedMultiBlockMachineA.DOOR_OF_CREATE.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            DoorOfCreate(blockEntity)
        }
        AdvancedMultiBlockMachineA.DOOR_OF_CREATE.onWorking = doorOfCreateOnWorking
        AdvancedMultiBlockMachineA.DOOR_OF_CREATE.recipeModifier = recipeModifierList

        AdvancedMultiBlockMachineA.CREATE_AGGREGATION.patchPatternPredicates(
            "thread_modifier",
            patternPredicateSelector(
                { GTLBlocks.DIMENSION_CONNECTION_CASING.get() },
                PartAbility.IMPORT_ITEMS,
                PartAbility.EXPORT_ITEMS,
                PartAbility.INPUT_ENERGY,
                PartAbility.COMPUTATION_DATA_RECEPTION
            ),
            threadModifierPredicate
        )
        AdvancedMultiBlockMachineA.CREATE_AGGREGATION.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            CreateAggregation(blockEntity)
        }
        AdvancedMultiBlockMachineA.CREATE_AGGREGATION.onWorking = createAggregationOnWorking
        AdvancedMultiBlockMachineA.CREATE_AGGREGATION.recipeModifier = recipeModifierList

        GTMachines.ACTIVE_TRANSFORMER.replacePatternPredicates(
            "active_transformer_min_casing_limit",
            patternPredicateSelector({ GTBlocks.HIGH_POWER_CASING.get() }),
            {
                Predicates.blocks(GTBlocks.HIGH_POWER_CASING.get())
                    .or(ActiveTransformerMachine.getHatchPredicates())
            }
        )

        AdditionalMultiBlockMachine.MOLECULAR_ASSEMBLER_MATRIX.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            MolecularAssemblerMultiblockMachine(blockEntity)
        }

        AdvancedMultiBlockMachineB.ADVANCED_INFINITE_DRILLER.patchPatternPredicates(
            "thread_modifier",
            patternPredicateSelector(
                { Registries.getBlock("gtlcore:iridium_casing") },
                PartAbility.IMPORT_FLUIDS,
                PartAbility.EXPORT_FLUIDS,
                PartAbility.INPUT_ENERGY,
                PartAbility.INPUT_LASER
            ),
            threadModifierPredicate
        )
        AdvancedMultiBlockMachineB.ADVANCED_INFINITE_DRILLER.patchPatternPredicates(
            "spectral_analysis_hatch",
            patternPredicateSelector({ Registries.getBlock("gtlcore:iridium_casing") }, PartAbility.IMPORT_FLUIDS),
            { Predicates.blocks(GTLAddMachines.ORE_PROCESSOR_HATCH.get()).setMaxGlobalLimited(1) }
        )
        AdvancedMultiBlockMachineB.ADVANCED_INFINITE_DRILLER.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            AdvancedInfiniteDrillMachine(blockEntity)
        }

        val weatherControl = AdvancedMultiBlockMachineB.WEATHER_CONTROL
        weatherControl.setMachineSupplier(::WeatherControlMachine)
        val baseWeatherModifier = weatherControl.recipeModifier
        weatherControl.recipeModifier = RecipeModifier { machine, recipe, params, result ->
            val modified = (machine as WeatherControlMachine).modifyLightningRecipe(recipe) ?: return@RecipeModifier null
            baseWeatherModifier.apply(machine, modified, params, result)
        }
        weatherControl.patchPatternPredicates(
            "weather_radon_input",
            patternPredicateSelector({ GTBlocks.STEEL_HULL.get() }, PartAbility.IMPORT_ITEMS, PartAbility.INPUT_ENERGY),
            { Predicates.abilities(PartAbility.IMPORT_FLUIDS).setMaxGlobalLimited(1) }
        )
        weatherControl.replacePatternPredicates(
            "weather_iron_bars",
            patternPredicateSelector(Blocks.LIGHTNING_ROD),
            { Predicates.blocks(Blocks.IRON_BARS) }
        )
        // Generate the preview from the updated predicates, including the optional fluid hatch.
        weatherControl.setShapes { emptyList() }

        val dataBank = GTResearchMachines.DATA_BANK as MultiblockMachineDefinition
        dataBank.replacePatternPredicates(
            "cloud_data_hatch_exclusion",
            patternPredicateSelector({ GTBlocks.COMPUTER_CASING.get() }, PartAbility.OPTICAL_DATA_RECEPTION),
            {
                Predicates.blocks(GTBlocks.COMPUTER_CASING.get()).setMinGlobalLimited(3)
                    .or(Predicates.abilities(PartAbility.DATA_ACCESS).setPreviewCount(3))
                    .or(Predicates.abilities(PartAbility.OPTICAL_DATA_TRANSMISSION).setMinGlobalLimited(1, 1))
                    .or(
                        Predicates.blocks(
                            *PartAbility.OPTICAL_DATA_RECEPTION.allBlocks
                                .filter { it != GTLAddMachines.CLOUD_DATA_HATCH.block }
                                .toTypedArray()
                        ).setPreviewCount(1)
                    )
            }
        )

        val networkSwitch = GTResearchMachines.NETWORK_SWITCH as MultiblockMachineDefinition
        networkSwitch.replacePatternPredicates(
            "cloud_computation_hatch_exclusion",
            patternPredicateSelector({ GTBlocks.COMPUTER_CASING.get() }),
            {
                Predicates.blocks(GTBlocks.COMPUTER_CASING.get()).setMinGlobalLimited(7)
                    .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1, 1))
                    .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                    .or(
                        Predicates.blocks(
                            *PartAbility.COMPUTATION_DATA_RECEPTION.allBlocks.stream()
                                .filter { it != GTLAddMachines.CLOUD_COMPUTATION_HATCH_RECEIVER.block }
                                .toList()
                                .toTypedArray()
                        ).setMinGlobalLimited(1, 2)
                    )
                    .or(
                        Predicates.blocks(
                            *PartAbility.COMPUTATION_DATA_TRANSMISSION.allBlocks.stream()
                                .filter { it != GTLAddMachines.CLOUD_COMPUTATION_HATCH_TRANSMITTER.block }
                                .toList()
                                .toTypedArray()
                        ).setMinGlobalLimited(1, 1)
                    )
            }
        )

        val researchStation = GTResearchMachines.RESEARCH_STATION as MultiblockMachineDefinition
        researchStation.replacePatternPredicates(
            "research_station_internal_air_relaxation",
            PatternPredicateSelector.pureAir(),
            { Predicates.any() }
        )
        researchStation.tooltipBuilder = researchStation.tooltipBuilder.andThen { _, components ->
            components.add("tooltip.gtladditions.cloud_systems".toComponent)
            components.add("tooltip.gtladditions.research_station.cloud_upload".toComponent)
            components.add(GTLAddMachines.GTLAdd_MODIFY)
        }
    }
}