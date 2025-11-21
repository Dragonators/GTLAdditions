package com.gtladd.gtladditions.api.machine.trait

import java.math.BigInteger

interface IWirelessNetworkEnergyHandler {
    fun consumeEnergy(energy: Int): Boolean

    fun consumeEnergy(energy: Long): Boolean

    fun consumeEnergy(energy: BigInteger): Boolean

    val maxAvailableEnergy: BigInteger

    val isOnline: Boolean
}
