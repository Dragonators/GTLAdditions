package com.gtladd.gtladditions.mixin.gtlcore.machine.part;

import org.gtlcore.gtlcore.common.machine.multiblock.part.maintenance.AutoConfigurationMaintenanceHatchPartMachine;
import org.gtlcore.gtlcore.common.machine.multiblock.part.maintenance.IAutoConfigurationMaintenanceHatch;
import org.gtlcore.gtlcore.utils.Registries;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.function.Supplier;

@Mixin(AutoConfigurationMaintenanceHatchPartMachine.class)
public abstract class AutoConfigurationMaintenanceHatchPartMachineMixin extends TieredPartMachine implements IMachineLife, IAutoConfigurationMaintenanceHatch {

    @Unique
    private static final ItemStack BIOWARE_MAINFRAME = Registries.getItemStack("kubejs:bioware_mainframe");
    @Unique
    private static final ItemStack COSMIC_MAINFRAME = Registries.getItemStack("kubejs:cosmic_mainframe");
    @Unique
    private static final ItemStack CREATIVE_MAINFRAME = Registries.getItemStack("kubejs:suprachronal_mainframe_complex");
    @Unique
    @Persisted(key = "gtladditions$max")
    private final NotifiableItemStackHandler gtladditions$max = this.gtladditions$createMachineStorage();

    @Shadow(remap = false)
    private float durationMultiplier;

    @Shadow(remap = false)
    private static Component getTextWidgetText(Supplier<Float> multiplier) {
        throw new AssertionError();
    }

    @Shadow(remap = false)
    public float getDurationMultiplier() {
        throw new AssertionError();
    }

    public AutoConfigurationMaintenanceHatchPartMachineMixin(IMachineBlockEntity holder) {
        super(holder, 5);
    }

    @Override
    public void setDurationMultiplier(float count) {
        if (count > gtladditions$getMax()) this.durationMultiplier = gtladditions$getMax();
        else this.durationMultiplier = Math.max(count, gtladditions$getMin());
    }

    @Override
    public boolean canShared() {
        return true;
    }

    @Override
    public void onMachineRemoved() {
        this.clearInventory(this.gtladditions$max.storage);
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    private void incInternalMultiplier(int multiplier) {
        durationMultiplier = Math.min(durationMultiplier + 0.01F * (float) multiplier, gtladditions$getMax());
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    private void decInternalMultiplier(int multiplier) {
        durationMultiplier = Math.max(durationMultiplier - 0.01F * (float) multiplier, gtladditions$getMin());
    }

    /**
     * @author .
     * @reason .
     */
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
                if (componentData.equals("sub")) this.decInternalMultiplier(multiplier);
                else if (componentData.equals("add")) this.incInternalMultiplier(multiplier);
            }
        }))).setBackground(GuiTextures.BACKGROUND_INVERSE);
        group.addWidget((new SlotWidget(gtladditions$max.storage, 0, 120, 40, true, true))
                .setBackground(GuiTextures.SLOT).setHoverTooltips(gtladditions$setMaxTooltips()));
        return group;
    }

    @Unique
    private NotifiableItemStackHandler gtladditions$createMachineStorage() {
        NotifiableItemStackHandler handler = new NotifiableItemStackHandler(this, 1, IO.NONE, IO.BOTH, (slots) -> new ItemStackTransfer(1) {

            public int getSlotLimit(int slot) {
                return 1;
            }
        });
        handler.addChangedListener(() -> setDurationMultiplier(durationMultiplier));
        return handler;
    }

    @Unique
    private @NotNull List<Component> gtladditions$setMaxTooltips() {
        List<Component> tooltips = new ObjectArrayList<>();
        tooltips.add(Component.translatable("gtceu.universal.enabled"));
        tooltips.add(Component.translatable("gtceu.multiblock.use_different_mainframe"));
        tooltips.add(Component.translatable("gtceu.multiblock.use_bioware_mainframe", 3.0, 0.15));
        tooltips.add(Component.translatable("gtceu.multiblock.use_cosmic_mainframe", 7.5, 0.1));
        tooltips.add(Component.translatable("gtceu.multiblock.use_suprachronal_mainframe_complex", 25.0, 0.05));
        tooltips.add(GTLAddMachines.INSTANCE.getGTLAdd_MODIFY());
        return tooltips;
    }

    @Unique
    private Item gtladditions$getCurrentMainframe() {
        return gtladditions$max.storage.getStackInSlot(0).getItem();
    }

    @Unique
    private float gtladditions$getMax() {
        Item mainframe = gtladditions$getCurrentMainframe();

        if (BIOWARE_MAINFRAME.is(mainframe)) return 3.0F;
        if (COSMIC_MAINFRAME.is(mainframe)) return 7.5F;
        if (CREATIVE_MAINFRAME.is(mainframe)) return 25.0F;
        return 1.2F;
    }

    @Unique
    private float gtladditions$getMin() {
        Item mainframe = gtladditions$getCurrentMainframe();

        if (BIOWARE_MAINFRAME.is(mainframe)) return 0.15F;
        if (COSMIC_MAINFRAME.is(mainframe)) return 0.1F;
        if (CREATIVE_MAINFRAME.is(mainframe)) return 0.05F;
        return 0.2F;
    }
}