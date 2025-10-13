package com.gtladd.gtladditions.api.machine.trait;

import java.math.BigInteger;

public interface IWirelessNetworkEnergyHandler {

    boolean consumeEnergy(int energy);

    boolean consumeEnergy(long energy);

    boolean consumeEnergy(BigInteger energy);

    BigInteger getMaxAvailableEnergy();

    boolean isOnline();
}
