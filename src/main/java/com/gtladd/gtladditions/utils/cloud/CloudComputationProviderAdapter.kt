package com.gtladd.gtladditions.utils.cloud

import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider
import org.gtlcore.gtlcore.api.machine.computation.ComputationNetwork

internal fun IOpticalComputationProvider.canBridgeCloud(): Boolean =
    ComputationNetwork.canBridge(listOf(this))

internal fun IOpticalComputationProvider.getMaxCloudCWU(): Long =
    ComputationNetwork.capacity(listOf(this))

internal fun IOpticalComputationProvider.getRemainingCloudCWU(): Long =
    ComputationNetwork.request(listOf(this), Long.MAX_VALUE, true)