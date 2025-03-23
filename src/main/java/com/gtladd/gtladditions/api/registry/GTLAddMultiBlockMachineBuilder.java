package com.gtladd.gtladditions.api.registry;

import org.gtlcore.gtlcore.common.data.GTLMachines;
import org.gtlcore.gtlcore.utils.TextUtil;

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
        return tooltipText("线圈温度每增加900K，并行数X2");
    }

    public GTLAddMultiBlockMachineBuilder tooltipTextLaser() {
        return tooltipText("允许使用激光仓");
    }

    public GTLAddMultiBlockMachineBuilder tooltipTextMultiRecipes() {
        return tooltipText("支持跨配方并行");
    }

    public GTLAddMultiBlockMachineBuilder tooltipTextSuperMultiRecipes() {
        return tooltipText("拥有特殊的跨配方并行");
    }

    public GTLAddMultiBlockMachineBuilder tooltipTextParallelHatch() {
        return tooltipText("通过并行控制仓让机器同时处理多个相同配方");
    }

    public GTLAddMultiBlockMachineBuilder tooltipTextPerfectOverclock() {
        return tooltipText("超频不会损失能耗");
    }

    public GTLAddMultiBlockMachineBuilder tooltipTextAdd() {
        return (GTLAddMultiBlockMachineBuilder) super.tooltips(Component.literal(TextUtil.full_color("由GTLAdditions添加")));
    }

    public GTLAddMultiBlockMachineBuilder coilparalleldisplay() {
        return (GTLAddMultiBlockMachineBuilder) super.additionalDisplay(GTLMachines.MULTIPLERECIPES_COIL_PARALLEL);
    }
}
