package com.gtladd.gtladditions.utils

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gtladd.gtladditions.mixin.gtlcore.machine.ComputationProviderMachineAccessor
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachine
import org.gtlcore.gtlcore.common.machine.multiblock.electric.ComputationProviderMachine

private val energyProbeRecipes = mutableMapOf<Long, GTRecipe>()
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
    return if (this is ComputationProviderMachine && simulate) {
        simulateAvailableCWU(requested).toLong()
    } else {
        requestCWUt(requested, simulate, ignoredSeenProviders).toLong()
    }
}

internal fun IOpticalComputationProvider.getMaxCloudCWU(): Long {
    if (isUnlimitedCloudProvider()) return Long.MAX_VALUE
    if (this is ComputationProviderMachine && !canProvideCloudCWU()) return 0L
    return getMaxCWUt(ignoredSeenProviders).toLong().coerceAtLeast(0L)
}

internal fun IOpticalComputationProvider.getRemainingCloudCWU(): Long {
    if (isUnlimitedCloudProvider()) return Long.MAX_VALUE
    return if (this is ComputationProviderMachine) {
        simulateAvailableCWU(Int.MAX_VALUE).toLong()
    } else {
        requestCWUt(Int.MAX_VALUE, true, ignoredSeenProviders).toLong().coerceAtLeast(0L)
    }
}

internal fun IOpticalComputationProvider.isUnlimitedCloudProvider(): Boolean =
    this is MetaMachine && definition === AdvancedMultiBlockMachine.CREATE_COMPUTATION

private fun ComputationProviderMachine.simulateAvailableCWU(requested: Int): Int {
    if (!isFormed || !canProvideCloudCWU()) return 0
    val maximum = getMaxCWUt(ignoredSeenProviders)
    val availableCapacity = (maximum - allocatedCWUt).coerceAtLeast(0)
    var projectedCWU = totalCWU
    val generationCost = GTValues.VA[tier].toLong()
    if (projectedCWU < maximum && canInputEUWithoutConsuming(generationCost)) {
        projectedCWU = minOf(maximum.toLong(), projectedCWU + (1L shl tier))
    }
    return minOf(requested.toLong(), availableCapacity.toLong(), projectedCWU)
        .coerceAtLeast(0L)
        .toInt()
}

private fun ComputationProviderMachine.canProvideCloudCWU(): Boolean =
    (this as ComputationProviderMachineAccessor).canProvideCWUt()

private fun ComputationProviderMachine.canInputEUWithoutConsuming(eut: Long): Boolean {
    val recipe = energyProbeRecipes.getOrPut(eut) {
        GTRecipeBuilder.ofRaw().EUt(eut).buildRawRecipe()
    }
    return recipe.matchTickRecipe(this).isSuccess
}