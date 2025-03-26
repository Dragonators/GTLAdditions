package com.gtladd.gtladditions.common.machine.muiltblock.controller;

import org.gtlcore.gtlcore.utils.Registries;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SingularityInverseCalculator extends WorkableElectricMultiblockMachine {

    private int linkedModules = 0;

    public SingularityInverseCalculator(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    private BlockPos getCorePos(BlockPos pos, Level level) {
        BlockPos[] coordinates = new BlockPos[] {
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

    private int LinkedModules() {
        Level level = this.getLevel();
        BlockPos blockPos = this.getCorePos(this.getPos(), level);
        if (blockPos != null && level != null) {
            BlockPos[] coordinates = new BlockPos[] {
                    blockPos.offset(9, -10, 0),
                    blockPos.offset(-9, -10, 0),
                    blockPos.offset(0, -10, -9),
                    blockPos.offset(0, -10, 9),
            };
            for (BlockPos coordinate : coordinates) {
                MetaMachine metaMachine = MetaMachine.getMachine(level, coordinate);
                if (metaMachine instanceof WorkableElectricMultiblockMachine mbmachine) {
                    if (mbmachine.isFormed()) {
                        String bid = mbmachine.getBlockState().getBlock().kjs$getId();
                        if (bid.equals("gtladditions:universe_sandbox_module")) {
                            ++this.linkedModules;
                        }
                    }
                }
            }
            return this.linkedModules;
        }
        return this.linkedModules;
    }

    public void addDisplayText(@NotNull List<Component> textList) {
        super.addDisplayText(textList);
        if (this.isFormed) {
            textList.add(Component.translatable("gtceu.machine.module", this.LinkedModules()));
        }
    }
}
