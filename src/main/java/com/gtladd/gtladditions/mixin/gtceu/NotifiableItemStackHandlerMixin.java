package com.gtladd.gtladditions.mixin.gtceu;

import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;
import dev.architectury.patchedmixin.staticmixin.spongepowered.asm.mixin.Overwrite;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NotifiableItemStackHandler.class)
public class NotifiableItemStackHandlerMixin implements IItemTransfer {
    @Mutable
    @Final
    @Shadow(remap = false) public final ItemStackTransfer storage;

    public NotifiableItemStackHandlerMixin(ItemStackTransfer storage) {
        this.storage = storage;
    }

    @Overwrite
    public double getTotalContentAmount() {
        long amount = 0L;
        int slots = this.storage.getSlots();
        for(int i = 0; i < slots; ++i) {
            ItemStack stack = this.storage.getStackInSlot(i);
            if (!stack.isEmpty()) {
                amount += stack.getCount();
            }
        }
        return (double)amount;
    }

    @Override
    public int getSlots() {
        return this.storage.getSlots();
    }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.storage.getStackInSlot(slot);
    }

    @NotNull
    @Override
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate, boolean notifyChanges) {
        return null;
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate, boolean notifyChanges) {
        return null;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 0;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return false;
    }

    @NotNull
    @Override
    public Object createSnapshot() {
        return null;
    }

    @Override
    public void restoreFromSnapshot(Object snapshot) {

    }
}
