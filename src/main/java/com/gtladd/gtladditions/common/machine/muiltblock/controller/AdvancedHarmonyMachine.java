package com.gtladd.gtladditions.common.machine.muiltblock.controller;

import org.gtlcore.gtlcore.common.machine.multiblock.electric.HarmonyMachine;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.logic.OCParams;
import com.gregtechceu.gtceu.api.recipe.logic.OCResult;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AdvancedHarmonyMachine extends HarmonyMachine {

    public final NotifiableItemStackHandler machineStorage;

    public AdvancedHarmonyMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
        this.machineStorage = this.createMachineStorage();
    }

    public static @Nullable GTRecipe recipeModifier(MetaMachine machine, @NotNull GTRecipe recipe, @NotNull OCParams params, @NotNull OCResult result) {
        GTRecipe recipe1 = HarmonyMachine.recipeModifier(machine, recipe, params, result);
        if (recipe1 != null) {
            return GTRecipeModifiers.accurateParallel(machine, recipe1, 2048, false).getFirst();
        }
        return null;
    }

    protected NotifiableItemStackHandler createMachineStorage() {
        NotifiableItemStackHandler handler = new NotifiableItemStackHandler(this, 1, IO.NONE, IO.BOTH, (slots) -> {
            return new ItemStackTransfer(1) {

                public int getSlotLimit(int slot) {
                    return 1;
                }
            };
        });
        handler.setFilter(this::filter);
        return handler;
    }

    public void onDrops(List<ItemStack> drops) {
        this.clearInventory(this.machineStorage.storage);
    }

    protected boolean filter(@NotNull ItemStack itemStack) {
        return true;
    }

    public @NotNull Widget createUIWidget() {
        Widget widget = super.createUIWidget();
        if (widget instanceof WidgetGroup group) {
            Size size = group.getSize();
            group.addWidget((new SlotWidget(this.machineStorage.storage, 0, size.width - 30, size.height - 30, true, true)).setBackground(new IGuiTexture[] { GuiTextures.SLOT }));
        }
        return widget;
    }

    public ItemStack getMachineStorageItem() {
        return this.machineStorage.getStackInSlot(0);
    }

    public void setMachineStorageItem(ItemStack item) {
        this.machineStorage.storage.setStackInSlot(0, item);
    }

    public boolean isEmpty() {
        return this.machineStorage.isEmpty();
    }
}
