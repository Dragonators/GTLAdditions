package com.gtladd.gtladditions.common.machine.muiltblock.controller;

import org.gtlcore.gtlcore.api.machine.multiblock.NoEnergyMultiblockMachine;
import org.gtlcore.gtlcore.utils.MachineIO;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineModifyDrops;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.logic.OCParams;
import com.gregtechceu.gtceu.api.recipe.logic.OCResult;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

import com.hepdd.gtmthings.api.misc.WirelessEnergyManager;
import com.hepdd.gtmthings.utils.TeamUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class SingularityInverseCalculator extends NoEnergyMultiblockMachine implements IMachineModifyDrops {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER;
    @Persisted
    public final NotifiableItemStackHandler machineStorage;
    @Persisted
    private int tier;
    @Persisted
    private UUID userid;
    private final ConditionalSubscriptionHandler StartupSubs = new ConditionalSubscriptionHandler(this, this::StartupUpdate, this::isFormed);

    public SingularityInverseCalculator(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
        this.machineStorage = createMachineStorage();
    }

    protected NotifiableItemStackHandler createMachineStorage() {
        NotifiableItemStackHandler handler = new NotifiableItemStackHandler(this, 1, IO.NONE, IO.BOTH, (slots) -> new ItemStackTransfer(1) {

            public int getSlotLimit(int slot) {
                return 64;
            }
        });
        handler.setFilter(this::filter);
        return handler;
    }

    @Override
    public @NotNull Widget createUIWidget() {
        Widget widget = super.createUIWidget();
        if (widget instanceof WidgetGroup group) {
            Size size = group.getSize();
            group.addWidget((new SlotWidget(this.machineStorage.storage, 0, size.width - 30, size.height - 30, true, true))
                    .setBackground(GuiTextures.SLOT));
        }
        return widget;
    }

    protected boolean filter(@NotNull ItemStack itemStack) {
        Item item = itemStack.getItem();
        return true;
    }

    public void onDrops(List<ItemStack> drops) {
        this.clearInventory(this.machineStorage.storage);
    }

    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public static GTRecipe recipeModifier(MetaMachine machine, @NotNull GTRecipe recipe, @NotNull OCParams ocParams, @NotNull OCResult ocResult) {
        if (machine instanceof SingularityInverseCalculator singularityInverseCalculator) {
            if (singularityInverseCalculator.userid != null && singularityInverseCalculator.tier > 0) {
                if ((int) (Math.random() * 100) <= 50) {
                    if (WirelessEnergyManager.addEUToGlobalEnergyMap(singularityInverseCalculator.userid, -singularityInverseCalculator.getStartupEnergy(), machine)) {
                        GTRecipe recipe1 = recipe.copy();
                        recipe1.duration = (int) (4800.0 / Math.pow(2.0, singularityInverseCalculator.tier));
                        if (!singularityInverseCalculator.machineStorage.isEmpty()) {
                            int count = singularityInverseCalculator.machineStorage.storage.getStackInSlot(0).getCount();
                            return GTRecipeModifiers.accurateParallel(machine, recipe1, count * 27, false).getFirst();
                        }
                        return recipe1;
                    }
                } else if (WirelessEnergyManager.addEUToGlobalEnergyMap(singularityInverseCalculator.userid, singularityInverseCalculator.getOutputEnergy(), machine)) return null;
            }
        }
        return null;
    }

    private void StartupUpdate() {
        if (this.getOffsetTimer() % 20L == 0L) {
            tier = 0;
            if (MachineIO.notConsumableCircuit(this, 1)) this.tier = 1;
            if (MachineIO.notConsumableCircuit(this, 2)) this.tier = 2;
            if (MachineIO.notConsumableCircuit(this, 3)) this.tier = 3;
            if (MachineIO.notConsumableCircuit(this, 4)) this.tier = 4;
        }
    }

    public void onStructureFormed() {
        super.onStructureFormed();
        this.StartupSubs.initialize(this.getLevel());
    }

    private long getStartupEnergy() {
        return this.tier == 0 ? 0L : (long) (5.2776558108672E15 * Math.pow(8.0, this.tier - 1));
    }

    private long getOutputEnergy() {
        return this.tier == 0 ? 0L : (long) (1.0E15 * Math.pow(8.0, this.tier - 1));
    }

    public void addDisplayText(@NotNull List<Component> textList) {
        super.addDisplayText(textList);
        if (this.isFormed()) {
            if (this.userid != null) {
                textList.add(Component.translatable("gtmthings.machine.wireless_energy_monitor.tooltip.0", TeamUtil.GetName(this.getLevel(), this.userid)));
                textList.add(Component.translatable("gtmthings.machine.wireless_energy_monitor.tooltip.1", FormattingUtil.formatNumbers(WirelessEnergyManager.getUserEU(this.userid))));
            }
            textList.add(Component.literal("启动耗能：" + FormattingUtil.formatNumbers(this.getStartupEnergy()) + "EU"));
        }
    }

    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        if (this.userid == null || !this.userid.equals(player.getUUID())) this.userid = player.getUUID();
        return true;
    }

    static {
        MANAGED_FIELD_HOLDER = new ManagedFieldHolder(SingularityInverseCalculator.class, NoEnergyMultiblockMachine.MANAGED_FIELD_HOLDER);
    }
}
