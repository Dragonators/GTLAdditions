package com.gtladd.gtladditions.api.machine

interface IGTLAddMultiRecipeMachine : IWirelessThreadModifierParallelMachine {
    fun getLimitedDuration(): Int = 20

    fun setLimitedDuration(duration: Int) {}
}