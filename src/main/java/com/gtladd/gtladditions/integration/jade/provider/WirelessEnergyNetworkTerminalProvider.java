package com.gtladd.gtladditions.integration.jade.provider;

import org.gtlcore.gtlcore.integration.gtmt.NewGTValues;
import org.gtlcore.gtlcore.utils.NumberUtils;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.gtladd.gtladditions.GTLAdditions;
import com.gtladd.gtladditions.common.machine.muiltblock.part.WirelessEnergyNetworkTerminalPartMachineBase;
import com.gtladd.gtladditions.utils.CommonUtils;
import com.hepdd.gtmthings.api.misc.WirelessEnergyManager;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import static com.hepdd.gtmthings.utils.TeamUtil.GetName;
import static com.hepdd.gtmthings.utils.TeamUtil.hasOwner;
import static net.minecraft.ChatFormatting.*;
import static org.gtlcore.gtlcore.utils.TextUtil.GTL_CORE$VC;

public class WirelessEnergyNetworkTerminalProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            if (blockEntity.getMetaMachine() instanceof WirelessEnergyNetworkTerminalPartMachineBase) {
                CompoundTag serverData = blockAccessor.getServerData();
                if (!serverData.hasUUID("uuid")) {
                    tooltip.add(Component.translatable("gtmthings.machine.wireless_energy_hatch.tooltip.1"));
                } else {
                    UUID uuid = serverData.getUUID("uuid");
                    if (hasOwner(blockAccessor.getLevel(), uuid)) {
                        tooltip.add(Component.translatable("gtmthings.machine.wireless_energy_hatch.tooltip.2", GetName(blockAccessor.getLevel(), uuid)));
                    } else {
                        tooltip.add(Component.translatable("gtmthings.machine.wireless_energy_hatch.tooltip.3", uuid));
                    }

                    var totalEu = WirelessEnergyManager.getUserEU(uuid);
                    var abs = totalEu.abs();
                    var longEu = NumberUtils.getLongValue(totalEu);
                    var energyTier = longEu == Long.MAX_VALUE ? GTValues.MAX_TRUE : NumberUtils.getFakeVoltageTier(longEu);
                    Component text = Component.literal(CommonUtils.format2Double(abs.doubleValue())).withStyle(RED)
                            .append(Component.literal(" EU").withStyle(RESET)
                                    .append(Component.literal(" (").withStyle(GREEN)
                                            .append(Component
                                                    .translatable("gtceu.top.electricity",
                                                            String.format("%.2e", new BigDecimal(abs).divide(BigDecimal.valueOf(GTValues.VEX[energyTier]), 3, RoundingMode.DOWN).doubleValue()),
                                                            NewGTValues.VNF[energyTier])
                                                    .withStyle(style -> style.withColor(GTL_CORE$VC[Math.min(energyTier, 14)])))
                                            .append(Component.literal(")").withStyle(GREEN))));

                    tooltip.add(Component.translatable("gtladditions.machine.wireless_energy_network_terminal.tooltips.1", text));
                }
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            if (blockEntity.getMetaMachine() instanceof WirelessEnergyNetworkTerminalPartMachineBase machine) {
                if (machine.getUUID() != null) compoundTag.putUUID("uuid", machine.getUUID());
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return GTLAdditions.id("wireless_energy_network_terminal");
    }
}
