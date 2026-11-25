package com.gtladd.gtladditions.mixin.gtmtings;

import net.minecraft.network.chat.Component;

import com.hepdd.gtmthings.common.block.machine.electric.WirelessEnergyMonitor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.math.BigInteger;

@Mixin(WirelessEnergyMonitor.class)
public interface WirelessEnergyMonitorAccessor {

    @Invoker(value = "getTimeToFillDrainText", remap = false)
    static Component invokeGetTimeToFillDrainText(BigInteger timeToFillSeconds) {
        throw new AssertionError();
    }
}
