package com.gtladd.gtladditions.api.machine.data;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import java.math.BigInteger;
import java.util.UUID;

public record MachineEnergyData(UUID userId, MetaMachine machine, BigInteger euPerTick)
        implements Comparable<MachineEnergyData> {

    @Override
    public int compareTo(MachineEnergyData other) {
        return this.euPerTick.compareTo(other.euPerTick);
    }
}
