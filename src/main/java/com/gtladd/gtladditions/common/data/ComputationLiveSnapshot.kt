package com.gtladd.gtladditions.common.data

class ComputationLiveSnapshot(
    providerCurrentCwu: LongArray,
    providerMaxCwu: LongArray,
    receiverRequestedCwu: IntArray
) {

    private val providerCurrentCwu = providerCurrentCwu.copyOf()
    private val providerMaxCwu = providerMaxCwu.copyOf()
    private val receiverRequestedCwu = receiverRequestedCwu.copyOf()

    init {
        require(this.providerCurrentCwu.size == this.providerMaxCwu.size)
    }

    val providerCount: Int
        get() = providerCurrentCwu.size

    val receiverCount: Int
        get() = receiverRequestedCwu.size

    fun getProviderCurrentCwu(index: Int): Long = providerCurrentCwu[index]

    fun getProviderMaxCwu(index: Int): Long = providerMaxCwu[index]

    fun getReceiverRequestedCwu(index: Int): Int = receiverRequestedCwu[index]
}