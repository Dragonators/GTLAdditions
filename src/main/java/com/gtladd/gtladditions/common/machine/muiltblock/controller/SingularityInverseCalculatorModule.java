package com.gtladd.gtladditions.common.machine.muiltblock.controller;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.logic.OCParams;
import com.gregtechceu.gtceu.api.recipe.logic.OCResult;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.gtlcore.gtlcore.GTLCore;
import org.gtlcore.gtlcore.api.machine.multiblock.NoEnergyMultiblockMachine;
import org.gtlcore.gtlcore.utils.MachineIO;
import org.gtlcore.gtlcore.utils.Registries;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SingularityInverseCalculatorModule extends NoEnergyMultiblockMachine {
    private int tier;
    private boolean isLinked;
    private final ConditionalSubscriptionHandler StartupSubs = new ConditionalSubscriptionHandler(this, this::StartupUpdate, this::isFormed);
    public SingularityInverseCalculatorModule(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }
    private BlockPos getCorePos(BlockPos pos, Level level) {
        BlockPos[] coordinates = new BlockPos[]{
                pos.offset(9, 10, 0),
                pos.offset(-9, 10, 0),
                pos.offset(0, 10, -9),
                pos.offset(0, 10, 9),
        };
        for (BlockPos coordinate : coordinates) {
            if (level.getBlockState(coordinate).getBlock().equals(Registries.getBlock("kubejs:annihilate_core"))) {
                return coordinate;
            }
        }
        return null;
    }
    private void getLinkedHost() {
        Level level = this.getLevel();
        BlockPos blockPos = this.getCorePos(this.getPos(), level);
        if (blockPos!= null && level != null) {
            BlockPos[] coordinatess = new BlockPos[] {
                    blockPos.offset(9, -10, 0),
                    blockPos.offset(-9, -10, 0),
                    blockPos.offset(0, -10, -9),
                    blockPos.offset(0, -10, 9)
            };
            for (BlockPos j : coordinatess) {
                RecipeLogic logic = GTCapabilityHelper.getRecipeLogic(level, j, null);
                if (logic != null && logic.getMachine().getDefinition() == MultiBlockMachine.SINGULARITU_INVERSE_CALCULATOR) {
                       if (logic.isWorking()) this.isLinked = true;
                       else if (!logic.isWorking()) this.isLinked = false;
                }
            }
        }

    }
    public void onStructureFormed() {
        super.onStructureFormed();
        this.StartupSubs.initialize(this.getLevel());
    }
    private void StartupUpdate() {
        if (this.getOffsetTimer() % 20L == 0L) {
            tier = 0;
            if (MachineIO.notConsumableCircuit(this, 1)) this.tier = 1;
            if (MachineIO.notConsumableCircuit(this, 2)) this.tier = 2;
            if (MachineIO.notConsumableCircuit(this, 3)) this.tier = 3;
            if (MachineIO.notConsumableCircuit(this, 4)) this.tier = 4;
            if (MachineIO.notConsumableCircuit(this, 5)) this.tier = 5;
            if (MachineIO.notConsumableCircuit(this, 6)) this.tier = 6;
            if (MachineIO.notConsumableCircuit(this, 7)) this.tier = 7;
            if (MachineIO.notConsumableCircuit(this, 8)) this.tier = 8;
            if (MachineIO.notConsumableCircuit(this, 9)) this.tier = 9;
            if (MachineIO.notConsumableCircuit(this, 10)) this.tier = 10;
            if (MachineIO.notConsumableCircuit(this, 11)) this.tier = 11;
            if (MachineIO.notConsumableCircuit(this, 12)) this.tier = 12;
            if (MachineIO.notConsumableCircuit(this, 13)) this.tier = 13;
            if (MachineIO.notConsumableCircuit(this, 14)) this.tier = 14;
            if (MachineIO.notConsumableCircuit(this, 15)) this.tier = 15;
            if (MachineIO.notConsumableCircuit(this, 16)) this.tier = 16;
            if (MachineIO.notConsumableCircuit(this, 17)) this.tier = 17;
            if (MachineIO.notConsumableCircuit(this, 18)) this.tier = 18;
            if (MachineIO.notConsumableCircuit(this, 19)) this.tier = 19;
            if (MachineIO.notConsumableCircuit(this, 20)) this.tier = 20;
            if (MachineIO.notConsumableCircuit(this, 21)) this.tier = 21;
            if (MachineIO.notConsumableCircuit(this, 22)) this.tier = 22;
            if (MachineIO.notConsumableCircuit(this, 23)) this.tier = 23;
            if (MachineIO.notConsumableCircuit(this, 24)) this.tier = 24;
            if (MachineIO.notConsumableCircuit(this, 25)) this.tier = 25;
            if (MachineIO.notConsumableCircuit(this, 26)) this.tier = 26;
            if (MachineIO.notConsumableCircuit(this, 27)) this.tier = 27;
            if (MachineIO.notConsumableCircuit(this, 28)) this.tier = 28;
            if (MachineIO.notConsumableCircuit(this, 29)) this.tier = 29;
            if (MachineIO.notConsumableCircuit(this, 30)) this.tier = 30;
            if (MachineIO.notConsumableCircuit(this, 31)) this.tier = 31;
            if (MachineIO.notConsumableCircuit(this, 32)) this.tier = 32;
        }
    }
    private long getStartupEnergy() {
        return this.tier == 0 ? 0L : (long)(GTValues.VA[GTValues.MAX] * 4096L * Math.pow(4.0, this.tier - 1));
    }
    private int getStartupDuration() {
        return this.tier == 0 ? 0 : 480 / (int)Math.pow(2, this.tier);
    }
    public static GTRecipe recipeModifier(MetaMachine machine, @NotNull GTRecipe recipe, @NotNull OCParams ocParams, @NotNull OCResult ocResult) {
        if (machine instanceof SingularityInverseCalculatorModule SingularityInverseCalculatorModule){
            GTRecipe recipe1 = recipe.copy();
            recipe1.duration = SingularityInverseCalculatorModule.getStartupDuration();
            RecipeHelper.setInputEUt(recipe1, SingularityInverseCalculatorModule.getStartupEnergy());
            return GTRecipeModifiers.accurateParallel(machine, recipe, Integer.MAX_VALUE, false).getFirst();
        }
        return null;
    }
    public void addDisplayText(@NotNull List<Component> textList) {
        super.addDisplayText(textList);
        if (this.isFormed()){
            if (this.getOffsetTimer() % 20L == 0L) this.getLinkedHost();
            textList.add(Component.literal((this.isLinked ? "已" : "未") + "连接到正在运行的主机" ));
            textList.add(Component.literal("启动耗能：" + FormattingUtil.formatNumbers(this.getStartupEnergy() / GTValues.VA[GTValues.MAX]) + "安MAX"));
            textList.add(Component.literal("配方耗时：" + (this.getStartupDuration() / 20) + "秒"));
        }
    }
}
