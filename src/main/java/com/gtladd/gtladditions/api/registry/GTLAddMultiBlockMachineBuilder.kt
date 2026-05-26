package com.gtladd.gtladditions.api.registry

import com.gregtechceu.gtceu.api.block.IMachineBlock
import com.gregtechceu.gtceu.api.data.RotationState
import com.gregtechceu.gtceu.api.item.MetaMachineItem
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.api.registry.registrate.MultiblockMachineBuilder
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
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

    fun allRotation(): GTLAddMultiBlockMachineBuilder = super.rotationState(RotationState.ALL) as GTLAddMultiBlockMachineBuilder

    fun nonYAxisRotation(): GTLAddMultiBlockMachineBuilder = super.rotationState(RotationState.NON_Y_AXIS).allowExtendedFacing(false) as GTLAddMultiBlockMachineBuilder

    fun noneRotation(): GTLAddMultiBlockMachineBuilder = super.rotationState(RotationState.NONE).allowExtendedFacing(false).allowFlip(false) as GTLAddMultiBlockMachineBuilder

    fun tooltipTextKey(vararg key: Component): GTLAddMultiBlockMachineBuilder = super.tooltips(*key) as GTLAddMultiBlockMachineBuilder

    fun tooltipTextMaxParallels(parallel: Any): GTLAddMultiBlockMachineBuilder = super.tooltips("gtceu.multiblock.max_parallel".toComponent(parallel)) as GTLAddMultiBlockMachineBuilder

    fun tooltipTextRecipeTypes(vararg recipeTypes: GTRecipeType): GTLAddMultiBlockMachineBuilder {
        val components = recipeTypes.map { it.registryName.toLanguageKey().toComponent }.toTypedArray()
        return super.tooltips("gtceu.machine.available_recipe_map_${recipeTypes.size}.tooltip".toComponent(*components)) as GTLAddMultiBlockMachineBuilder
    }

    fun tooltipTextCoilParallel(): GTLAddMultiBlockMachineBuilder = super.tooltips("gtceu.multiblock.coil_parallel".toComponent) as GTLAddMultiBlockMachineBuilder

    fun tooltipTextLaser(): GTLAddMultiBlockMachineBuilder = super.tooltips("gtceu.multiblock.laser.tooltip".toComponent) as GTLAddMultiBlockMachineBuilder

    fun tooltipOnlyTextLaser(): GTLAddMultiBlockMachineBuilder = super.tooltips("gtceu.multiblock.only.laser.tooltip".toComponent) as GTLAddMultiBlockMachineBuilder

    fun tooltipTextMultiRecipes(): GTLAddMultiBlockMachineBuilder = super.tooltips("gtceu.machine.multiple_recipes.tooltip".toComponent) as GTLAddMultiBlockMachineBuilder

    fun tooltipTextParallelHatch(): GTLAddMultiBlockMachineBuilder = super.tooltips("gtceu.multiblock.parallelizable.tooltip".toComponent) as GTLAddMultiBlockMachineBuilder

    fun tooltipTextPerfectOverclock(): GTLAddMultiBlockMachineBuilder = super.tooltips("gtceu.machine.perfect_oc".toComponent) as GTLAddMultiBlockMachineBuilder

    fun coilParallelDisplay(): GTLAddMultiBlockMachineBuilder = super.additionalDisplay(GTLMachines.MULTIPLERECIPES_COIL_PARALLEL) as GTLAddMultiBlockMachineBuilder

    companion object {
        fun createMulti(
            name: String,
            metaMachine: Function<IMachineBlockEntity, out MultiblockControllerMachine>,
            blockFactory: BiFunction<BlockBehaviour.Properties, MultiblockMachineDefinition, IMachineBlock>,
            itemFactory: BiFunction<IMachineBlock, Item.Properties, MetaMachineItem>,
            blockEntityFactory: TriFunction<BlockEntityType<*>, BlockPos, BlockState, IMachineBlockEntity>
        ): GTLAddMultiBlockMachineBuilder = GTLAddMultiBlockMachineBuilder(name, metaMachine, blockFactory, itemFactory, blockEntityFactory)
    }
}