package com.gtladd.gtladditions.api.registry;

import org.gtlcore.gtlcore.common.data.GTLMachines;

import com.gregtechceu.gtceu.api.block.IMachineBlock;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.item.MetaMachineItem;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.registry.registrate.MultiblockMachineBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import org.apache.commons.lang3.function.TriFunction;

import java.util.function.BiFunction;
import java.util.function.Function;

public class GTLAddMultiBlockMachineBuilder extends MultiblockMachineBuilder {

    private GTLAddMultiBlockMachineBuilder(String name, Function<IMachineBlockEntity, ? extends MultiblockControllerMachine> metaMachine,
                                           BiFunction<BlockBehaviour.Properties, MultiblockMachineDefinition, IMachineBlock> blockFactory,
                                           BiFunction<IMachineBlock, Item.Properties, MetaMachineItem> itemFactory,
                                           TriFunction<BlockEntityType<?>, BlockPos, BlockState, IMachineBlockEntity> blockEntityFactory) {
        super(GTLAddRegistration.REGISTRATE, name, metaMachine, blockFactory, itemFactory, blockEntityFactory);
    }

    public static GTLAddMultiBlockMachineBuilder createMulti(String name, Function<IMachineBlockEntity, ? extends MultiblockControllerMachine> metaMachine,
                                                             BiFunction<BlockBehaviour.Properties, MultiblockMachineDefinition, IMachineBlock> blockFactory,
                                                             BiFunction<IMachineBlock, Item.Properties, MetaMachineItem> itemFactory,
                                                             TriFunction<BlockEntityType<?>, BlockPos, BlockState, IMachineBlockEntity> blockEntityFactory) {
        return new GTLAddMultiBlockMachineBuilder(name, metaMachine, blockFactory, itemFactory, blockEntityFactory);
    }

    public GTLAddMultiBlockMachineBuilder allRotation() {
        return (GTLAddMultiBlockMachineBuilder) super.rotationState(RotationState.ALL);
    }

    public GTLAddMultiBlockMachineBuilder nonYAxisRotation() {
        return (GTLAddMultiBlockMachineBuilder) super.rotationState(RotationState.NON_Y_AXIS).allowExtendedFacing(false);
    }

    public GTLAddMultiBlockMachineBuilder noneRotation() {
        return (GTLAddMultiBlockMachineBuilder) super.rotationState(RotationState.NONE).allowExtendedFacing(false).allowFlip(false);
    }

    public GTLAddMultiBlockMachineBuilder tooltipText(String string) {
        return (GTLAddMultiBlockMachineBuilder) super.tooltips(new Component[] { Component.literal(string) });
    }

    public GTLAddMultiBlockMachineBuilder tooltipTextCoilParallel() {
        return (GTLAddMultiBlockMachineBuilder) super.tooltips(new Component[] { Component.translatable("gtceu.multiblock.coil_parallel") });
    }

    public GTLAddMultiBlockMachineBuilder tooltipTextLaser() {
        return (GTLAddMultiBlockMachineBuilder) super.tooltips(new Component[] { Component.translatable("gtceu.multiblock.laser.tooltip") });
    }

    public GTLAddMultiBlockMachineBuilder tooltipTextMultiRecipes() {
        return (GTLAddMultiBlockMachineBuilder) super.tooltips(new Component[] { Component.translatable("gtceu.machine.multiple_recipes.tooltip") });
    }

    public GTLAddMultiBlockMachineBuilder tooltipTextParallelHatch() {
        return (GTLAddMultiBlockMachineBuilder) super.tooltips(new Component[] { Component.translatable("gtceu.multiblock.parallelizable.tooltip") });
    }

    public GTLAddMultiBlockMachineBuilder tooltipTextPerfectOverclock() {
        return (GTLAddMultiBlockMachineBuilder) super.tooltips(new Component[] { Component.translatable("gtceu.machine.perfect_oc") });
    }

    public GTLAddMultiBlockMachineBuilder coilparalleldisplay() {
        return (GTLAddMultiBlockMachineBuilder) super.additionalDisplay(GTLMachines.MULTIPLERECIPES_COIL_PARALLEL);
    }
}
