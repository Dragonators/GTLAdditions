package com.gtladd.gtladditions.utils;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.unimi.dsi.fastutil.Pair;

import java.math.BigInteger;
import java.time.Duration;
import java.util.UUID;

public class WirelessEnergyManagerData {

    public static final Cache<MetaMachine, Pair<UUID, BigInteger>> BIG_MACHINE_DATA = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(5))
            .maximumSize(10_000)
            .weakKeys()
            .build();
}
