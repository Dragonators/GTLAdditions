package com.gtladd.gtladditions.utils

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider
import com.gregtechceu.gtceu.api.machine.MetaMachine
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachineB
import org.gtlcore.gtlcore.common.machine.multiblock.electric.ComputationProviderMachine

private val ignoredSeenProviders: MutableCollection<IOpticalComputationProvider> = IgnoredSeenProviders

private object IgnoredSeenProviders : AbstractMutableCollection<IOpticalComputationProvider>() {

    override val size = 0

    override fun add(element: IOpticalComputationProvider): Boolean = true

    override fun iterator(): MutableIterator<IOpticalComputationProvider> = EmptyProviderIterator
}

private object EmptyProviderIterator : MutableIterator<IOpticalComputationProvider> {

    override fun hasNext(): Boolean = false

    override fun next(): IOpticalComputationProvider = throw NoSuchElementException()

    override fun remove(): Unit = throw IllegalStateException("Cloud seen-provider collection is not mutable")
}

internal fun IOpticalComputationProvider.canBridgeCloud(): Boolean {
    val canBridge = canBridge(ignoredSeenProviders)
    return if (this is ComputationProviderMachine) isFormed && canBridge else canBridge
}

internal fun IOpticalComputationProvider.requestCloudCWU(cwu: Long, simulate: Boolean): Long {
    if (cwu <= 0L) return 0L
    if (isUnlimitedCloudProvider()) return cwu
    val requested = Ints.saturatedCast(cwu)
    // GTLCore fix2 simulates without drawing energy or allocating CWU.
    return requestCWUt(requested, simulate, ignoredSeenProviders).toLong()
}

internal fun IOpticalComputationProvider.getMaxCloudCWU(): Long {
    if (this is ComputationProviderMachine && energyContainer.energyStored == 0L) return 0L
    if (isUnlimitedCloudProvider()) return Long.MAX_VALUE
    return getMaxCWUt(ignoredSeenProviders).toLong().coerceAtLeast(0L)
}

internal fun IOpticalComputationProvider.getRemainingCloudCWU(): Long {
    if (isUnlimitedCloudProvider()) return Long.MAX_VALUE
    return requestCWUt(Int.MAX_VALUE, true, ignoredSeenProviders).toLong().coerceAtLeast(0L)
}

internal fun IOpticalComputationProvider.isUnlimitedCloudProvider(): Boolean =
    this is MetaMachine && definition === AdvancedMultiBlockMachineB.CREATE_COMPUTATION