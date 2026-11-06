package com.gtladd.gtladditions.mixin.gtceu.common.machine;

import net.minecraft.world.item.ItemStack;

import com.gtladd.gtladditions.utils.ObjectHolderSlotRules;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "com.gregtechceu.gtceu.common.machine.multiblock.part.ObjectHolderMachine$ObjectHolderHandler$1", remap = false)
public class ObjectHolderTransferMixin {

    // This anonymous transfer inherits the unrestricted implementation from ItemStackTransfer.
    public boolean isItemValid(int slot, ItemStack stack) {
        return ObjectHolderSlotRules.isItemValid(slot, stack);
    }
}