package com.gtladd.gtladditions.common.data

class ComputationTopologySnapshot(
    val topologyVersion: Long,
    providers: List<CloudMachineSnapshot>,
    receivers: List<CloudMachineSnapshot>,
    unboundProviders: List<CloudMachineSnapshot>,
    unboundReceivers: List<CloudMachineSnapshot>
) {

    val providers: List<CloudMachineSnapshot> = providers.toList()
    val receivers: List<CloudMachineSnapshot> = receivers.toList()
    val unboundProviders: List<CloudMachineSnapshot> = unboundProviders.toList()
    val unboundReceivers: List<CloudMachineSnapshot> = unboundReceivers.toList()
}