package com.gtladd.gtladditions.api.machine;

import com.gregtechceu.gtceu.api.machine.feature.IMachineModifyDrops;

import net.minecraft.world.item.ItemStack;

import com.gtladd.gtladditions.common.items.GTLAddItems;

import java.util.List;

public interface IAstralArrayInteractionMachine extends IMachineModifyDrops {

    int increaseAstralArrayCount(int amount);

    int getAstralArrayCount();

    @Override
    default void onDrops(List<ItemStack> list) {
        var count = getAstralArrayCount();
        if (count > 0) {
            for (int i = 0; i < count; i += 64) {
                int stackSize = Math.min(64, count - i);
                ItemStack stack = new ItemStack(GTLAddItems.ASTRAL_ARRAY.asItem(), stackSize);
                list.add(stack);
            }
        }
    }
}
