package com.gtladd.gtladditions.mixin.gtlcore.machine.part;

import org.gtlcore.gtlcore.common.machine.multiblock.part.maintenance.*;
import org.gtlcore.gtlcore.utils.Registries;

import com.gregtechceu.gtceu.api.capability.ICleanroomReceiver;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;

import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.gtladd.gtladditions.common.machine.GTLAddMachines;
import dev.architectury.patchedmixin.staticmixin.spongepowered.asm.mixin.Overwrite;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;
import java.util.function.Supplier;

import static org.gtlcore.gtlcore.common.machine.multiblock.part.maintenance.ICleaningRoom.DUMMY_CLEANROOM;

@Mixin(AutoConfigurationMaintenanceHatchPartMachine.class)
public class AutoConfigurationMaintenanceHatchPartMachineMixin extends TieredPartMachine implements IMachineLife, IAutoConfigurationMaintenanceHatch {

    private float MAX_DURATION = getMax();
    private float MIN_DURATION = getMin();
    private static final ItemStack BIOWARE_MAINFRAME = Registries.getItemStack("kubejs:bioware_mainframe");
    private static final ItemStack COSMIC_MAINFRAME = Registries.getItemStack("kubejs:cosmic_mainframe");
    private static final ItemStack CREATIVE_MAINFRAME = Registries.getItemStack("kubejs:suprachronal_mainframe_complex");
    @Persisted
    private final NotifiableItemStackHandler gtladditions$max = this.createMachineStorage();

    private NotifiableItemStackHandler createMachineStorage() {
        return new NotifiableItemStackHandler(this, 1, IO.NONE, IO.BOTH, (slots) -> new ItemStackTransfer(1) {

            public int getSlotLimit(int slot) {
                return 1;
            }
        });
    }

    @Shadow(remap = false)
    private float durationMultiplier;

    @Shadow(remap = false)
    private static Component getTextWidgetText(Supplier<Float> multiplier) {
        return null;
    }

    @Shadow(remap = false)
    public float getDurationMultiplier() {
        return 0;
    }

    @Override
    public void setDurationMultiplier(float count) {
        if (count > getMax()) this.durationMultiplier = getMax();
        else this.durationMultiplier = Math.max(count, getMin());
    }

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

    @Overwrite(remap = false)
    public Widget createUIWidget() {
        WidgetGroup group = new WidgetGroup(0, 0, 150, 70);
        group.addWidget((new DraggableScrollableWidgetGroup(4, 4, 142, 62)).setBackground(GuiTextures.DISPLAY).addWidget((new ComponentPanelWidget(4, 5, (list) -> {
            list.add(getTextWidgetText(this::getDurationMultiplier));
            MutableComponent buttonText = Component.translatable("gtceu.maintenance.configurable_duration.modify");
            buttonText.append(" ");
            buttonText.append(ComponentPanelWidget.withButton(Component.literal("[-]"), "sub"));
            buttonText.append(" ");
            buttonText.append(ComponentPanelWidget.withButton(Component.literal("[+]"), "add"));
            list.add(buttonText);
        })).setMaxWidthLimit(130).clickHandler((componentData, clickData) -> {
            if (!clickData.isRemote) {
                int multiplier = clickData.isCtrlClick ? 100 : (clickData.isShiftClick ? 10 : 1);
                if (componentData.equals("sub")) this.setSubDuration(multiplier);
                else if (componentData.equals("add")) this.setAddDuration(multiplier);
            }
        }))).setBackground(GuiTextures.BACKGROUND_INVERSE);
        group.addWidget((new SlotWidget(gtladditions$max.storage, 0, 120, 40, true, true))
                .setBackground(GuiTextures.SLOT).setHoverTooltips(gtladditions$setMaxTooltips()));
        this.setSubDuration(0);
        this.setAddDuration(0);
        return group;
    }

    private void setDurationMultiplier() {
        MAX_DURATION = getMax();
        MIN_DURATION = getMin();
    }

    private void setSubDuration(int multiplier) {
        this.setDurationMultiplier();
        this.decInternalMultiplier(multiplier);
    }

    private void setAddDuration(int multiplier) {
        this.setDurationMultiplier();
        this.incInternalMultiplier(multiplier);
    }

    @Unique
    public void onMachineRemoved() {
        this.clearInventory(this.gtladditions$max.storage);
    }

    private @NotNull List<Component> gtladditions$setMaxTooltips() {
        List<Component> gtladditions$tooltips = new ArrayList<>();
        gtladditions$tooltips.add(Component.translatable("gtceu.universal.enabled"));
        gtladditions$tooltips.add(Component.translatable("gtceu.multiblock.use_different_mainframe"));
        gtladditions$tooltips.add(Component.translatable("gtceu.multiblock.use_bioware_mainframe", 3.0, 0.15));
        gtladditions$tooltips.add(Component.translatable("gtceu.multiblock.use_cosmic_mainframe", 7.5, 0.1));
        gtladditions$tooltips.add(Component.translatable("gtceu.multiblock.use_suprachronal_mainframe_complex", 25.0, 0.05));
        gtladditions$tooltips.add(GTLAddMachines.INSTANCE.getGTLAdd_MODIFY());
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
        Item stack = gtladditions$max != null ? gtladditions$max.storage.getStackInSlot(0).getItem() : null;
        if (stack != null) {
            if (BIOWARE_MAINFRAME.is(stack)) return 0.15F;
            else if (COSMIC_MAINFRAME.is(stack)) return 0.1F;
            else if (CREATIVE_MAINFRAME.is(stack)) return 0.05F;
        }
        return 0.2F;
    }

    @Unique
    public void addedToController(@NotNull IMultiController controller) {
        super.addedToController(controller);
        if (controller instanceof ICleanroomReceiver receiver) receiver.setCleanroom(null);
    }

    @Unique
    public void removedFromController(@NotNull IMultiController controller) {
        super.removedFromController(controller);
        if (controller instanceof ICleanroomReceiver receiver) if (receiver.getCleanroom() == DUMMY_CLEANROOM) receiver.setCleanroom(null);
    }
}
