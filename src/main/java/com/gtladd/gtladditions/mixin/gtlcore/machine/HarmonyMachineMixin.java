package com.gtladd.gtladditions.mixin.gtlcore.machine;

import org.gtlcore.gtlcore.api.machine.multiblock.NoEnergyMultiblockMachine;
import org.gtlcore.gtlcore.common.machine.multiblock.electric.HarmonyMachine;
import org.gtlcore.gtlcore.utils.MachineIO;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import com.gtladd.gtladditions.utils.CommonUtils;
import com.hepdd.gtmthings.api.misc.WirelessEnergyManager;
import com.hepdd.gtmthings.utils.TeamUtil;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.UUID;

@Mixin(HarmonyMachine.class)
public abstract class HarmonyMachineMixin extends NoEnergyMultiblockMachine {

    @Shadow(remap = false)
    private int oc = 0;
    @Shadow(remap = false)
    private long hydrogen = 0L;
    @Shadow(remap = false)
    private long helium = 0L;
    @Shadow(remap = false)
    private UUID userid;

    @Shadow(remap = false)
    private long getStartupEnergy() {
        throw new AssertionError();
    }

    public HarmonyMachineMixin(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    protected void StartupUpdate() {
        if (this.getOffsetTimer() % 20L == 0L) {
            this.oc = 0;
            if (this.hydrogen < 10000000000L && MachineIO.inputFluid(this, GTMaterials.Hydrogen.getFluid(100000000L))) this.hydrogen += 100000000L;
            if (this.helium < 10000000000L && MachineIO.inputFluid(this, GTMaterials.Helium.getFluid(100000000L))) this.helium += 100000000L;
            if (MachineIO.notConsumableCircuit(this, 4)) this.oc = 4;
            if (MachineIO.notConsumableCircuit(this, 3)) this.oc = 3;
            if (MachineIO.notConsumableCircuit(this, 2)) this.oc = 2;
            if (MachineIO.notConsumableCircuit(this, 1)) this.oc = 1;
        }
    }

    @Override
    public void addDisplayText(@NotNull List<Component> textList) {
        super.addDisplayText(textList);
        if (this.isFormed()) {
            if (userid != null) {
                var totalEu = WirelessEnergyManager.getUserEU(userid);
                textList.add(Component.translatable("gtmthings.machine.wireless_energy_monitor.tooltip.0",
                        TeamUtil.GetName(getLevel(), userid)));
                textList.add(Component.translatable("gtmthings.machine.wireless_energy_monitor.tooltip.1",
                        Component.literal(CommonUtils.formatBigIntegerFixed(totalEu)).withStyle(ChatFormatting.RED)));
            }
            textList.add(Component.translatable("tooltip.gtlcore.startup_energy_cost", FormattingUtil.formatNumbers(getStartupEnergy())));
            textList.add(Component.translatable("tooltip.gtlcore.hydrogen_storage", FormattingUtil.formatNumbers(hydrogen)));
            textList.add(Component.translatable("tooltip.gtlcore.helium_storage", FormattingUtil.formatNumbers(helium)));
        }
    }
}
