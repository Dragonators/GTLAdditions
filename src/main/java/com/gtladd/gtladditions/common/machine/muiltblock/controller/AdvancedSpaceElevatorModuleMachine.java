package com.gtladd.gtladditions.common.machine.muiltblock.controller;

import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine;
import org.gtlcore.gtlcore.common.data.GTLBlocks;
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachine;
import org.gtlcore.gtlcore.common.machine.multiblock.electric.SpaceElevatorMachine;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import com.gtladd.gtladditions.api.recipeslogic.GTLAddMultipleRecipesLogic;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AdvancedSpaceElevatorModuleMachine extends WorkableElectricMultiblockMachine implements ParallelMachine {

    private int SpaceElevatorTier = 0;
    private int ModuleTier = 0;
    private final boolean SEPMTier;

    public AdvancedSpaceElevatorModuleMachine(IMachineBlockEntity holder, boolean SEPMTier) {
        super(holder);
        this.SEPMTier = SEPMTier;
    }

    public @NotNull RecipeLogic createRecipeLogic(@NotNull Object @NotNull... args) {
        return new GTLAddMultipleRecipesLogic(this);
    }

    private void getSpaceElevatorTier() {
        Level level = this.getLevel();
        BlockPos pos = this.getPos();
        BlockPos[] coordinates = new BlockPos[] {
                pos.offset(8, -2, 3),
                pos.offset(8, -2, -3),
                pos.offset(-8, -2, 3),
                pos.offset(-8, -2, -3),
                pos.offset(3, -2, 8),
                pos.offset(-3, -2, 8),
                pos.offset(3, -2, -8),
                pos.offset(-3, -2, -8)
        };
        for (BlockPos i : coordinates) {
            if (level != null && level.getBlockState(i).getBlock() == GTLBlocks.POWER_CORE.get()) {
                BlockPos[] coordinatess = new BlockPos[] {
                        i.offset(3, 2, 0),
                        i.offset(-3, 2, 0),
                        i.offset(0, 2, 3),
                        i.offset(0, 2, -3)
                };
                for (BlockPos j : coordinatess) {
                    RecipeLogic logic = GTCapabilityHelper.getRecipeLogic(level, j, null);
                    if (logic != null && logic.getMachine().getDefinition() == AdvancedMultiBlockMachine.SPACE_ELEVATOR) {
                        if (logic.isWorking() && logic.getProgress() > 80) {
                            this.SpaceElevatorTier = ((SpaceElevatorMachine) logic.machine).getTier() - 7;
                            this.ModuleTier = ((SpaceElevatorMachine) logic.machine).getCasingTier();
                        } else if (!logic.isWorking()) {
                            this.SpaceElevatorTier = 0;
                            this.ModuleTier = 0;
                        }
                    }
                }
            }
        }
    }

    public static boolean beforeWorking(IRecipeLogicMachine machine, @NotNull GTRecipe recipe) {
        if (machine instanceof AdvancedSpaceElevatorModuleMachine spaceElevatorModuleMachine) {
            spaceElevatorModuleMachine.getSpaceElevatorTier();
            if (spaceElevatorModuleMachine.SpaceElevatorTier < 1) return false;
            return !spaceElevatorModuleMachine.SEPMTier || recipe.data.getInt("SEPMTier") <= spaceElevatorModuleMachine.ModuleTier;
        }
        return false;
    }

    public boolean onWorking() {
        boolean value = super.onWorking();
        if (this.getOffsetTimer() % 10L == 0L) {
            this.getSpaceElevatorTier();
            if (this.SpaceElevatorTier < 1) {
                this.getRecipeLogic().interruptRecipe();
                return false;
            }
        }
        return value;
    }

    public void addDisplayText(@NotNull List<Component> textList) {
        super.addDisplayText(textList);
        if (this.isFormed) {
            if (this.getOffsetTimer() % 10L == 0L) {
                this.getSpaceElevatorTier();
            }
            textList.add(Component.translatable("gtceu.multiblock.parallel",
                    Component.translatable(FormattingUtil.formatNumbers(getMaxParallel())).withStyle(ChatFormatting.DARK_PURPLE))
                    .withStyle(ChatFormatting.GRAY));
            textList.add(Component.translatable((this.SpaceElevatorTier < 1 ? "未" : "已") + "连接正在运行的太空电梯"));
        }
    }

    public int getMaxParallel() {
        return (2 << 3) << (this.ModuleTier - 1);
    }
}
