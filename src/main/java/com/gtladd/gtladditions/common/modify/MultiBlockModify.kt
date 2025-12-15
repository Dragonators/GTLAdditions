package com.gtladd.gtladditions.common.modify

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic
import com.gregtechceu.gtceu.api.recipe.logic.OCParams
import com.gregtechceu.gtceu.api.recipe.logic.OCResult
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifierList
import com.gregtechceu.gtceu.common.data.GTMachines
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers
import com.gregtechceu.gtceu.utils.SupplierMemoizer
import com.gtladd.gtladditions.api.machine.IThreadModifierMachine
import com.gtladd.gtladditions.common.machine.muiltblock.controller.mutable.CreateAggregation
import com.gtladd.gtladditions.common.machine.muiltblock.controller.mutable.DoorOfCreate
import com.gtladd.gtladditions.common.machine.muiltblock.controller.MolecularAssemblerMultiblockMachine
import com.gtladd.gtladditions.common.modify.multiblockMachine.WorkableMultiBlock
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import org.gtlcore.gtlcore.common.data.machines.AdditionalMultiBlockMachine
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachine
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
                    Entity::class.java, AABB(
                        pos.x - 10.0, pos.y - 10.0, pos.z - 10.0, pos.x + 10.0, pos.y + 10.0, pos.z + 10.0
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
                                    0.0, 1.0, 0.0,
                                    entity.yRot,
                                    entity.xRot,
                                )
                            }
                        } else {
                            entity.`kjs$setStatusMessage`(
                                Component.translatable(
                                    "message.gtlcore.equipment_incompatible_dimension"
                                )
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
                    blockId == "kubejs:command_block_broken" && MachineIO.inputItem(
                        machine as WorkableMultiblockMachine,
                        Registries.getItemStack("kubejs:chain_command_block_core")
                    ) -> level.setBlockAndUpdate(pos, Blocks.CHAIN_COMMAND_BLOCK.defaultBlockState())

                    blockId == "kubejs:chain_command_block_broken" && MachineIO.inputItem(
                        machine as WorkableMultiblockMachine,
                        Registries.getItemStack("kubejs:repeating_command_block_core")
                    ) -> level.setBlockAndUpdate(pos, Blocks.REPEATING_COMMAND_BLOCK.defaultBlockState())
                }
            }
        }
        true
    }

    private val recipeModifierList = RecipeModifierList(
        { machine: MetaMachine?, recipe: GTRecipe?, params: OCParams?, result: OCResult? ->
            GTRecipeModifiers.accurateParallel(
                machine,
                recipe!!,
                Ints.saturatedCast(1L + (machine as IThreadModifierMachine).getAdditionalThread()),
                false
            ).getFirst()
        }, GTRecipeModifiers.ELECTRIC_OVERCLOCK.apply(OverclockingLogic(1.0, 1.0, false))
    )

    fun init() {
        AdvancedMultiBlockMachine.DOOR_OF_CREATE.patternFactory = SupplierMemoizer.memoize {
            (WorkableMultiBlock.doorOfCreatePattern).apply(AdvancedMultiBlockMachine.DOOR_OF_CREATE)
        }
        AdvancedMultiBlockMachine.DOOR_OF_CREATE.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            DoorOfCreate(blockEntity)
        }
        AdvancedMultiBlockMachine.DOOR_OF_CREATE.onWorking = doorOfCreateOnWorking
        AdvancedMultiBlockMachine.DOOR_OF_CREATE.recipeModifier = recipeModifierList


        AdvancedMultiBlockMachine.CREATE_AGGREGATION.patternFactory = SupplierMemoizer.memoize {
            (WorkableMultiBlock.createAggregation).apply(AdvancedMultiBlockMachine.CREATE_AGGREGATION)
        }
        AdvancedMultiBlockMachine.CREATE_AGGREGATION.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            CreateAggregation(blockEntity)
        }
        AdvancedMultiBlockMachine.CREATE_AGGREGATION.onWorking = createAggregationOnWorking
        AdvancedMultiBlockMachine.CREATE_AGGREGATION.recipeModifier = recipeModifierList


        GTMachines.ACTIVE_TRANSFORMER.patternFactory = SupplierMemoizer.memoize {
            (WorkableMultiBlock.activeTransformer).apply(GTMachines.ACTIVE_TRANSFORMER)
        }

        AdditionalMultiBlockMachine.MOLECULAR_ASSEMBLER_MATRIX.setMachineSupplier { blockEntity: IMachineBlockEntity ->
            MolecularAssemblerMultiblockMachine(blockEntity)
        }
    }
}
