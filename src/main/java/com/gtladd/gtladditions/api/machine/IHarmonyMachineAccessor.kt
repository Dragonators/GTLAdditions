package com.gtladd.gtladditions.api.machine

interface IHarmonyMachineAccessor {

    fun consumeCosmosStartup(): Boolean = false

    fun consumeAstralStartup(): Boolean = false

    fun getHarmonyDuration(): Int = 0
}