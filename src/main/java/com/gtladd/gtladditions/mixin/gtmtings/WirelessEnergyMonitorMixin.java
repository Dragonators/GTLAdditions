package com.gtladd.gtladditions.mixin.gtmtings;

import org.gtlcore.gtlcore.integration.gtmt.NewGTValues;
import org.gtlcore.gtlcore.utils.NumberUtils;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;

import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import com.gtladd.gtladditions.common.data.MachineEnergyData;
import com.gtladd.gtladditions.utils.CommonUtils;
import com.hepdd.gtmthings.common.block.machine.electric.WirelessEnergyMonitor;
import com.hepdd.gtmthings.utils.TeamUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import static com.gtladd.gtladditions.utils.WirelessEnergyManagerData.BIG_MACHINE_DATA;
import static com.hepdd.gtmthings.api.misc.WirelessEnergyManager.getUserEU;
import static com.hepdd.gtmthings.utils.TeamUtil.GetName;

@Mixin(WirelessEnergyMonitor.class)
public abstract class WirelessEnergyMonitorMixin extends MetaMachine {

    @Unique
    private List<MachineEnergyData> gTLAdditions$cachedSortedBigEntries = null;
    @Shadow(remap = false)
    private UUID userid;
    @Shadow(remap = false)
    private boolean all;

    @Shadow(remap = false)
    private BigDecimal getAvgUsage(BigInteger now) {
        throw new AssertionError();
    }

    @Shadow(remap = false)
    private static Component getTimeToFillDrainText(BigInteger timeToFillSeconds) {
        throw new AssertionError();
    }

    public WirelessEnergyMonitorMixin(IMachineBlockEntity holder) {
        super(holder);
    }

    @Unique
    private List<MachineEnergyData> gTLAdditions$getSortedBigEntries() {
        if (gTLAdditions$cachedSortedBigEntries == null || getOffsetTimer() % 20 == 0) {
            List<MachineEnergyData> dataList = new ObjectArrayList<>();

            for (var entry : BIG_MACHINE_DATA.asMap().entrySet()) {
                MetaMachine machine = entry.getKey();
                var data = entry.getValue();
                dataList.add(new MachineEnergyData(data.left(), machine, data.right()));
            }

            dataList.sort(null);
            gTLAdditions$cachedSortedBigEntries = dataList;

            BIG_MACHINE_DATA.invalidateAll();
        }
        return gTLAdditions$cachedSortedBigEntries;
    }

    /**
     * @author Dragons
     * @reason Format
     */
    @Overwrite(remap = false)
    private void addDisplayText(@NotNull List<Component> textList) {
        BigInteger energyTotal = getUserEU(this.userid);
        textList.add(Component.translatable("gtmthings.machine.wireless_energy_monitor.tooltip.0",
                GetName(this.holder.level(), this.userid)).withStyle(ChatFormatting.AQUA));
        textList.add(Component.translatable("gtmthings.machine.wireless_energy_monitor.tooltip.1",
                Component.literal(CommonUtils.formatBigIntegerFixed(energyTotal)).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY));

        BigDecimal avgEnergy = getAvgUsage(energyTotal);
        BigDecimal absAvgEnergy = avgEnergy.abs();
        long absAvgLongEut = NumberUtils.getLongValue(absAvgEnergy.toBigInteger());
        int avgEnergyTier = absAvgLongEut == Long.MAX_VALUE ? GTValues.MAX_TRUE : NumberUtils.getFakeVoltageTier(absAvgLongEut);
        Component voltageName = Component.literal(NewGTValues.VNF[avgEnergyTier]);
        BigDecimal voltageAmperage = absAvgEnergy.divide(BigDecimal.valueOf(GTValues.VEX[avgEnergyTier]), 2, RoundingMode.FLOOR);

        if (avgEnergy.compareTo(BigDecimal.valueOf(0)) >= 0) {
            textList.add(Component.translatable("gtmthings.machine.wireless_energy_monitor.tooltip.input",
                    Component.literal(CommonUtils.formatFixedBigDecimal(absAvgEnergy)).withStyle(ChatFormatting.BLUE), CommonUtils.format2BigDecimal(voltageAmperage), voltageName).withStyle(ChatFormatting.GRAY));
            textList.add(Component.translatable("gtceu.multiblock.power_substation.time_to_fill", Component.translatable("gtmthings.machine.wireless_energy_monitor.tooltip.time_to_fill")).withStyle(ChatFormatting.GRAY));
        } else {
            textList.add(Component.translatable("gtmthings.machine.wireless_energy_monitor.tooltip.output",
                    Component.literal(CommonUtils.formatFixedBigDecimal(absAvgEnergy)).withStyle(ChatFormatting.BLUE), CommonUtils.format2BigDecimal(voltageAmperage), voltageName).withStyle(ChatFormatting.GRAY));
            textList.add(Component.translatable("gtceu.multiblock.power_substation.time_to_drain",
                    getTimeToFillDrainText(energyTotal.divide(absAvgEnergy.toBigInteger().multiply(BigInteger.valueOf(20))))).withStyle(ChatFormatting.GRAY));
        }

        textList.add(Component.translatable("gtmthings.machine.wireless_energy_monitor.tooltip.statistics")
                .append(ComponentPanelWidget.withButton(all ? Component.translatable("gtmthings.machine.wireless_energy_monitor.tooltip.all") : Component.translatable("gtmthings.machine.wireless_energy_monitor.tooltip.team"), "all")));

        for (var entry : gTLAdditions$getSortedBigEntries()) {
            UUID uuid = entry.userId;
            if (all || TeamUtil.getTeamUUID(uuid) == TeamUtil.getTeamUUID(this.userid)) {
                MetaMachine machine = entry.machine;
                BigInteger eut = entry.euPerTick;
                long absLongEut = NumberUtils.getLongValue(eut.abs());
                int energyTier = absLongEut == Long.MAX_VALUE ? GTValues.MAX_TRUE : NumberUtils.getFakeVoltageTier(absLongEut);
                String pos = machine.getPos().toShortString();
                textList.add(Component.translatable(machine.getBlockState().getBlock().getDescriptionId())
                        .withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("recipe.condition.dimension.tooltip", machine.getLevel()
                                .dimension().location()).append(" [").append(pos).append("] ")
                                .append(Component.translatable("gtmthings.machine.wireless_energy_monitor.tooltip.0", GetName(this.holder.level(), uuid))))))
                        .append(" ").append(CommonUtils.formatSignBigInteger(eut)).append(" EU/t (")
                        .append(NewGTValues.VNF[energyTier]).append(")")
                        .append(ComponentPanelWidget.withButton(Component.literal("[").append(pos)
                                .append("]"), "none")));
            }
        }
    }
}
