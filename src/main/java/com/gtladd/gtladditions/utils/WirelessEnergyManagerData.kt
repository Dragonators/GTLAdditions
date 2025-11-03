package com.gtladd.gtladditions.utils

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.gregtechceu.gtceu.api.machine.MetaMachine
import it.unimi.dsi.fastutil.Pair
import java.math.BigInteger
import java.time.Duration
import java.util.*

object WirelessEnergyManagerData {
    @JvmField
    val BIG_MACHINE_DATA: Cache<MetaMachine, Pair<UUID, BigInteger>> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(5))
        .maximumSize(10000)
        .weakKeys()
        .build<MetaMachine, Pair<UUID, BigInteger>>()
}
