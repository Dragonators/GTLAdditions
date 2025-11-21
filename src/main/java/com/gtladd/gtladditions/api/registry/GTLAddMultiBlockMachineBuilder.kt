package com.gtladd.gtladditions.api.registry

import com.gregtechceu.gtceu.api.block.IMachineBlock
import com.gregtechceu.gtceu.api.data.RotationState
import com.gregtechceu.gtceu.api.item.MetaMachineItem
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.api.registry.registrate.MultiblockMachineBuilder
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import org.apache.commons.lang3.function.TriFunction
import org.gtlcore.gtlcore.common.data.GTLMachines
import java.util.function.BiFunction
import java.util.function.Function

class GTLAddMultiBlockMachineBuilder private constructor(
    name: String,
    metaMachine: Function<IMachineBlockEntity, out MultiblockControllerMachine>,
    blockFactory: BiFunction<BlockBehaviour.Properties, MultiblockMachineDefinition, IMachineBlock>,
    itemFactory: BiFunction<IMachineBlock, Item.Properties, MetaMachineItem>,
    blockEntityFactory: TriFunction<BlockEntityType<*>, BlockPos, BlockState, IMachineBlockEntity>
) : MultiblockMachineBuilder(
    GTLAddRegistration.REGISTRATE,
    name,
    metaMachine,
    blockFactory,
    itemFactory,
    blockEntityFactory
) {

    fun allRotation(): GTLAddMultiBlockMachineBuilder {
        return super.rotationState(RotationState.ALL) as GTLAddMultiBlockMachineBuilder
    }

    fun nonYAxisRotation(): GTLAddMultiBlockMachineBuilder {
        return super.rotationState(RotationState.NON_Y_AXIS).allowExtendedFacing(false) as GTLAddMultiBlockMachineBuilder
    }

    fun noneRotation(): GTLAddMultiBlockMachineBuilder {
        return super.rotationState(RotationState.NONE).allowExtendedFacing(false).allowFlip(false) as GTLAddMultiBlockMachineBuilder
    }

    fun tooltipTextKey(key: Component): GTLAddMultiBlockMachineBuilder {
        return super.tooltips(key) as GTLAddMultiBlockMachineBuilder
    }

    fun tooltipTextMaxParallels(parallel: Any): GTLAddMultiBlockMachineBuilder {
        return super.tooltips(Component.translatable("gtceu.multiblock.max_parallel", parallel)) as GTLAddMultiBlockMachineBuilder
    }

    fun tooltipTextRecipeTypes(vararg recipeTypes: GTRecipeType): GTLAddMultiBlockMachineBuilder {
        val components = recipeTypes.map { Component.translatable(it.registryName.toLanguageKey()) }.toTypedArray()
        return super.tooltips(Component.translatable("gtceu.machine.available_recipe_map_${recipeTypes.size}.tooltip", *components)) as GTLAddMultiBlockMachineBuilder
    }

    fun tooltipTextCoilParallel(): GTLAddMultiBlockMachineBuilder {
        return super.tooltips(Component.translatable("gtceu.multiblock.coil_parallel")) as GTLAddMultiBlockMachineBuilder
    }

    fun tooltipTextLaser(): GTLAddMultiBlockMachineBuilder {
        return super.tooltips(Component.translatable("gtceu.multiblock.laser.tooltip")) as GTLAddMultiBlockMachineBuilder
    }

    fun tooltipOnlyTextLaser(): GTLAddMultiBlockMachineBuilder {
        return super.tooltips(Component.translatable("gtceu.multiblock.only.laser.tooltip")) as GTLAddMultiBlockMachineBuilder
    }

    fun tooltipTextMultiRecipes(): GTLAddMultiBlockMachineBuilder {
        return super.tooltips(Component.translatable("gtceu.machine.multiple_recipes.tooltip")) as GTLAddMultiBlockMachineBuilder
    }

    fun tooltipTextParallelHatch(): GTLAddMultiBlockMachineBuilder {
        return super.tooltips(Component.translatable("gtceu.multiblock.parallelizable.tooltip")) as GTLAddMultiBlockMachineBuilder
    }

    fun tooltipTextPerfectOverclock(): GTLAddMultiBlockMachineBuilder {
        return super.tooltips(Component.translatable("gtceu.machine.perfect_oc")) as GTLAddMultiBlockMachineBuilder
    }

    fun coilParallelDisplay(): GTLAddMultiBlockMachineBuilder {
        return super.additionalDisplay(GTLMachines.MULTIPLERECIPES_COIL_PARALLEL) as GTLAddMultiBlockMachineBuilder
    }

    companion object {
        fun createMulti(
            name: String,
            metaMachine: Function<IMachineBlockEntity, out MultiblockControllerMachine>,
            blockFactory: BiFunction<BlockBehaviour.Properties, MultiblockMachineDefinition, IMachineBlock>,
            itemFactory: BiFunction<IMachineBlock, Item.Properties, MetaMachineItem>,
            blockEntityFactory: TriFunction<BlockEntityType<*>, BlockPos, BlockState, IMachineBlockEntity>
        ): GTLAddMultiBlockMachineBuilder {
            return GTLAddMultiBlockMachineBuilder(name, metaMachine, blockFactory, itemFactory, blockEntityFactory)
        }
    }
}