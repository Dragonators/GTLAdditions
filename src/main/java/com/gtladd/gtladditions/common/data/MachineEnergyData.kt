package com.gtladd.gtladditions.common.data

import com.gregtechceu.gtceu.api.machine.MetaMachine
import java.math.BigInteger
import java.util.*

@JvmRecord
data class MachineEnergyData(@JvmField val userId: UUID, @JvmField val machine: MetaMachine, @JvmField val euPerTick: BigInteger) :
    Comparable<MachineEnergyData> {
    override fun compareTo(other: MachineEnergyData): Int {
        return this.euPerTick.compareTo(other.euPerTick)
    }
}
