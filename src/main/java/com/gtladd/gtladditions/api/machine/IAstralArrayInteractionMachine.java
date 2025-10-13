package com.gtladd.gtladditions.api.machine;

import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.gtladd.gtladditions.common.items.GTLAddItems;

public interface IAstralArrayInteractionMachine extends IMachineLife {

    int increaseAstralArrayCount(int amount);

    int getAstralArrayCount();

    @Override
    default void onMachineRemoved() {
        IMachineLife.super.onMachineRemoved();
        var count = getAstralArrayCount();
        var level = getLevel();
        var pos = getPos();
        if (count > 0) {
            for (int i = 0; i < count; i += 64) {
                int stackSize = Math.min(64, count - i);
                ItemStack stack = new ItemStack(GTLAddItems.ASTRAL_ARRAY.asItem(), stackSize);
                ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                level.addFreshEntity(itemEntity);
            }
        }
    }

    Level getLevel();

    BlockPos getPos();
}
