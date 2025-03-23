package com.gtladd.gtladditions.mixin.gtlcore;

import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import org.gtlcore.gtlcore.common.machine.multiblock.part.maintenance.AutoConfigurationMaintenanceHatchPartMachine;
import org.gtlcore.gtlcore.utils.Registries;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;

import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.llamalad7.mixinextras.sugar.Local;
import dev.architectury.patchedmixin.staticmixin.spongepowered.asm.mixin.Overwrite;
import org.gtlcore.gtlcore.utils.TextUtil;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(AutoConfigurationMaintenanceHatchPartMachine.class)
public class AutoConfigurationMaintenanceHatchPartMachineMixin extends TieredPartMachine implements IMachineLife {

    private float MAX_DURATION = getMax();
    private float MIN_DURATION = getMin();
    private static final ItemStack BIOWARE_MAINFRAME = Registries.getItemStack("kubejs:bioware_mainframe", 16);
    private static final ItemStack COSMIC_MAINFRAME = Registries.getItemStack("kubejs:cosmic_mainframe", 16);
    private static final ItemStack CREATIVE_MAINFRAME = Registries.getItemStack("kubejs:suprachronal_mainframe_complex", 16);
    @Persisted
    private final NotifiableItemStackHandler gtladditions$max = new NotifiableItemStackHandler(this, 1, IO.NONE, IO.BOTH, (slots) -> new ItemStackTransfer(1) {
        public int getSlotLimit(int slot) {
            return 16;
        }
    });
    @Persisted
    private final NotifiableItemStackHandler gtladditions$min = new NotifiableItemStackHandler(this, 1, IO.NONE, IO.BOTH, (slots) -> new ItemStackTransfer(1) {
        public int getSlotLimit(int slot) {
            return 16;
        }
    });
    @Shadow(remap = false)
    private float durationMultiplier;

    public AutoConfigurationMaintenanceHatchPartMachineMixin(IMachineBlockEntity holder) {
        super(holder, 5);
    }

    @Unique
    public boolean canShared() {
        return true;
    }

    @Overwrite(remap = false)
    public void incInternalMultiplier(int multiplier) {
        durationMultiplier = Math.min(durationMultiplier + 0.01F * (float) multiplier, MAX_DURATION);
    }

    @Overwrite(remap = false)
    private void decInternalMultiplier(int multiplier) {
        durationMultiplier = Math.max(durationMultiplier - 0.01F * (float) multiplier, MIN_DURATION);
    }

    @Inject(method = "createUIWidget", at = @At(value = "RETURN", shift = At.Shift.BEFORE), remap = false)
    public void createUIWidget(CallbackInfoReturnable<Widget> cir, @Local(ordinal = 0) WidgetGroup group) {
        group.addWidget((new SlotWidget(gtladditions$min.storage, 0, 100, 40, true, true))
                .setBackground(GuiTextures.SLOT).setHoverTooltips(gtladditions$setMinTooltips()));
        group.addWidget((new SlotWidget(gtladditions$max.storage, 0, 120, 40, true, true))
                .setBackground(GuiTextures.SLOT).setHoverTooltips(gtladditions$setMaxTooltips()));
        MAX_DURATION = getMax();
        MIN_DURATION = getMin();
        incInternalMultiplier(0);
        decInternalMultiplier(0);
    }

    @Unique
    public void onMachineRemoved() {
        this.clearInventory(this.gtladditions$max.storage);
        this.clearInventory(this.gtladditions$min.storage);
    }

    private @NotNull List<Component> gtladditions$setMaxTooltips() {
        List<Component> gtladditions$tooltips = new ArrayList<>();
        gtladditions$tooltips.add(Component.literal("允许多方块结构共享"));
        gtladditions$tooltips.add(Component.literal("每个等级的加成需要不同的电路主机16个"));
        gtladditions$tooltips.add(Component.literal("生物活性处理器主机：3.0"));
        gtladditions$tooltips.add(Component.literal("寰宇处理器主机：7.5"));
        gtladditions$tooltips.add(Component.literal("创造主机：25.0"));
        gtladditions$tooltips.add(Component.literal("需要重新打开gui界面才能生效"));
        gtladditions$tooltips.add(Component.literal(TextUtil.full_color("由GTLAdditions修改")));
        return gtladditions$tooltips;
    }

    private @NotNull List<Component> gtladditions$setMinTooltips() {
        List<Component> gtladditions$tooltips = new ArrayList<>();
        gtladditions$tooltips.add(Component.literal("允许多方块结构共享"));
        gtladditions$tooltips.add(Component.literal("每个等级的加成需要不同的电路主机16个"));
        gtladditions$tooltips.add(Component.literal("生物活性处理器主机：0.15"));
        gtladditions$tooltips.add(Component.literal("寰宇处理器主机：0.1"));
        gtladditions$tooltips.add(Component.literal("创造主机：0.05"));
        gtladditions$tooltips.add(Component.literal("需要重新打开gui界面才能生效"));
        gtladditions$tooltips.add(Component.literal(TextUtil.full_color("由GTLAdditions修改")));
        return gtladditions$tooltips;
    }

    private float getMax() {
        Item stack = gtladditions$max != null ? gtladditions$max.storage.getStackInSlot(0).getItem() : null;
        if (stack != null) {
            if (BIOWARE_MAINFRAME.is(stack)) return 3.0F;
            else if (COSMIC_MAINFRAME.is(stack)) return 7.5F;
            else if (CREATIVE_MAINFRAME.is(stack)) return 25.0F;
        }
        return 1.2F;
    }

    private float getMin() {
        Item stack = gtladditions$min != null ? gtladditions$min.storage.getStackInSlot(0).getItem() : null;
        if (stack != null) {
            if (BIOWARE_MAINFRAME.is(stack)) return 0.15F;
            else if (COSMIC_MAINFRAME.is(stack)) return 0.1F;
            else if (CREATIVE_MAINFRAME.is(stack)) return 0.05F;
        }
        return 0.2F;
    }
}
