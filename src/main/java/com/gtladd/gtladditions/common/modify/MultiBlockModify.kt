package com.gtladd.gtladditions.common.modify

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine
import com.gregtechceu.gtceu.api.pattern.Predicates
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic
import com.gregtechceu.gtceu.api.recipe.logic.OCParams
import com.gregtechceu.gtceu.api.recipe.logic.OCResult
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifierList
import com.gregtechceu.gtceu.common.data.GTBlocks
import com.gregtechceu.gtceu.common.data.GTMachines
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers
import com.gregtechceu.gtceu.common.machine.multiblock.electric.ActiveTransformerMachine
import com.gtladd.gtladditions.api.machine.GTLAddPartAbility
import com.gtladd.gtladditions.api.machine.IThreadModifierMachine
import com.gtladd.gtladditions.api.pattern.patchPatternPredicates
import com.gtladd.gtladditions.api.pattern.patternPredicateSelector
import com.gtladd.gtladditions.api.pattern.replacePatternPredicates
import com.gtladd.gtladditions.common.machine.GTLAddMachines
import com.gtladd.gtladditions.common.machine.multiblock.controller.BasicOreProcessorMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.MolecularAssemblerMultiblockMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.mutable.AdvancedInfiniteDrillMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.mutable.CreateAggregation
import com.gtladd.gtladditions.common.machine.multiblock.controller.mutable.DoorOfCreate
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
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
import org.gtlcore.gtlcore.common.data.GTLBlocks
import org.gtlcore.gtlcore.common.data.machines.AdditionalMultiBlockMachine
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachine
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
        AdvancedMultiBlockMachine.DOOR_OF_CREATE.patchPatternPredicates(
            "thread_modifier",
            patternPredicateSelector(
                { GTLBlocks.DIMENSION_CONNECTION_CASING.get() },
                PartAbility.IMPORT_ITEMS,
                PartAbility.EXPORT_ITEMS,
                PartAbility.INPUT_ENERGY
            ),
            threadModifierPredicate
        )
        AdvancedMultiBlockMachine.DOOR_OF_CREATE.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            DoorOfCreate(blockEntity)
        }
        AdvancedMultiBlockMachine.DOOR_OF_CREATE.onWorking = doorOfCreateOnWorking
        AdvancedMultiBlockMachine.DOOR_OF_CREATE.recipeModifier = recipeModifierList

        AdvancedMultiBlockMachine.CREATE_AGGREGATION.patchPatternPredicates(
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
        AdvancedMultiBlockMachine.CREATE_AGGREGATION.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            CreateAggregation(blockEntity)
        }
        AdvancedMultiBlockMachine.CREATE_AGGREGATION.onWorking = createAggregationOnWorking
        AdvancedMultiBlockMachine.CREATE_AGGREGATION.recipeModifier = recipeModifierList

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

        AdvancedMultiBlockMachine.ADVANCED_INFINITE_DRILLER.patchPatternPredicates(
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
        AdvancedMultiBlockMachine.ADVANCED_INFINITE_DRILLER.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            AdvancedInfiniteDrillMachine(blockEntity)
        }
    }
}