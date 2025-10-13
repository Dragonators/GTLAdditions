package com.gtladd.gtladditions.api.machine;

import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler;
import org.jetbrains.annotations.Nullable;

public interface IWirelessElectricMultiblockMachine {

    @Nullable
    default IWirelessNetworkEnergyHandler getWirelessNetworkEnergyHandler() {
        return null;
    }

    default void setWirelessNetworkEnergyHandler(IWirelessNetworkEnergyHandler trait) {}
}
