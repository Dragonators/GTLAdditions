package com.gtladd.gtladditions.common.data

class ComputationTopologySnapshot(
    val topologyVersion: Long,
    providers: List<CloudMachineSnapshot>,
    receivers: List<CloudMachineSnapshot>,
    val otherProviderCount: Int,
    val otherReceiverCount: Int
) {

    val providers: List<CloudMachineSnapshot> = providers.toList()
    val receivers: List<CloudMachineSnapshot> = receivers.toList()
}