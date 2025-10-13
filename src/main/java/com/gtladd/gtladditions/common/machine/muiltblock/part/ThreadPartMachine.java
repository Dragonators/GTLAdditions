package com.gtladd.gtladditions.common.machine.muiltblock.part;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;

import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

import com.google.common.primitives.Ints;
import com.gtladd.gtladditions.api.machine.IThreadModifierMachine;
import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart;
import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic;
import com.gtladd.gtladditions.common.items.GTLAddItems;
import com.gtladd.gtladditions.utils.ThreadMultiplierStrategy;
import org.jetbrains.annotations.NotNull;

public class ThreadPartMachine extends MultiblockPartMachine implements IThreadModifierPart, IMachineLife {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            ThreadPartMachine.class, MultiblockPartMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    private final ItemStackTransfer astralArrayInventory;

    private int threadMultiplier = 0;
    private int threadCount = 0;

    public ThreadPartMachine(IMachineBlockEntity holder) {
        super(holder);
        astralArrayInventory = new ItemStackTransfer(1);
        astralArrayInventory.setFilter(ThreadPartMachine::astralArrayFilter);
    }

    private static boolean astralArrayFilter(ItemStack stack) {
        return stack.is(GTLAddItems.ASTRAL_ARRAY.asItem());
    }

    private void reCalculateThreadCount() {
        threadCount = Ints.saturatedCast(astralArrayInventory.getStackInSlot(0).getCount() * 64L * threadMultiplier);
    }

    @Override
    public Widget createUIWidget() {
        WidgetGroup group = new WidgetGroup(0, 0, 150, 70);
        group.addWidget(new DraggableScrollableWidgetGroup(4, 4, 142, 62).setBackground(GuiTextures.DISPLAY).addWidget(new ComponentPanelWidget(4, 5, list -> {
            list.add(Component.translatable("gtladditions.thread_modifier_hatch.thread_multiplier", Component.literal(String.valueOf(threadMultiplier)).withStyle(ChatFormatting.GOLD))
                    .setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("gtladditions.thread_modifier_hatch.hover")))));
            list.add(Component.translatable("gtladditions.thread_modifier_hatch.thread_count", Component.literal(String.valueOf(threadCount))
                    .withStyle(ChatFormatting.GOLD)));
        }))).setBackground(GuiTextures.BACKGROUND_INVERSE);
        group.addWidget(new SlotWidget(astralArrayInventory, 0, 120, 40, true, true)
                .setChangeListener(this::reCalculateThreadCount)
                .setBackground(GuiTextures.SLOT)
                .setHoverTooltips(Component.translatable("gtladditions.thread_modifier_hatch.base_thread")));
        return group;
    }

    @Override
    public void onMachineRemoved() {
        clearInventory(astralArrayInventory);
    }

    @Override
    public void addedToController(@NotNull IMultiController controller) {
        super.addedToController(controller);
        if (controller instanceof IThreadModifierMachine machine) {
            machine.setThreadPartMachine(this);
            if (controller instanceof IRecipeLogicMachine recipeLogicMachine) {
                if (recipeLogicMachine.getRecipeLogic() instanceof MutableRecipesLogic<?> logic) {
                    logic.setUseMultipleRecipes(true);
                }
                this.threadMultiplier = ThreadMultiplierStrategy.getAdditionalMultiplier(controller.self().getDefinition());
            }
        } else this.threadMultiplier = 0;
        reCalculateThreadCount();
    }

    @Override
    public void removedFromController(@NotNull IMultiController controller) {
        super.removedFromController(controller);
        threadMultiplier = 0;
        threadCount = 0;
    }

    @Override
    public boolean canShared() {
        return false;
    }

    @Override
    public int getThreadCount() {
        return threadCount;
    }

    @Override
    @NotNull
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }
}
