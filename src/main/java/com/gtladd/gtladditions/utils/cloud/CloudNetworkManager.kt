package com.gtladd.gtladditions.utils.cloud

import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.common.machine.multiblock.electric.research.NetworkSwitchMachine
import com.gtladd.gtladditions.common.data.CloudMachineSnapshot
import com.gtladd.gtladditions.common.data.ComputationLiveSnapshot
import com.gtladd.gtladditions.common.data.ComputationTopologySnapshot
import com.gtladd.gtladditions.common.machine.CloudOpticalDataMachine
import com.gtladd.gtladditions.common.machine.hatch.CloudOpticalComputationHatchMachine
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.minecraft.core.GlobalPos
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import org.gtlcore.gtlcore.api.machine.computation.ComputationNetwork
import org.gtlcore.gtlcore.api.machine.computation.ComputationUsage
import java.util.UUID

object CloudNetworkManager {

    private class TeamState {

        val providers: MutableSet<IOpticalComputationProvider> = ObjectOpenHashSet()
        val receiverControllers: MutableSet<MetaMachine> = ObjectOpenHashSet()
    }

    private val emptyTeamState = TeamState()
    private val computationProviders =
        Object2ObjectOpenHashMap<GlobalPos, CloudOpticalComputationHatchMachine>()
    private val computationReceivers =
        Object2ObjectOpenHashMap<GlobalPos, CloudOpticalComputationHatchMachine>()
    private val dataMachines = Object2ObjectOpenHashMap<GlobalPos, CloudOpticalDataMachine>()
    private val teamStates = Object2ObjectOpenHashMap<UUID, TeamState>()

    private val machineLocationOrder =
        compareBy<MetaMachine> { getDimensionId(it) }
            .thenBy { it.pos.x }
            .thenBy { it.pos.y }
            .thenBy { it.pos.z }

    private val providerLocationOrder = Comparator<IOpticalComputationProvider> { first, second ->
        machineLocationOrder.compare(first as MetaMachine, second as MetaMachine)
    }

    private var topologyDirty = true
    private var topologyVersion = 0L

    fun registerComputationHatch(hatch: CloudOpticalComputationHatchMachine) {
        val key = keyOf(hatch) ?: return
        val changed = if (hatch.isTransmitter()) {
            val removedOpposite = computationReceivers.remove(key) != null
            val replaced = computationProviders.put(key, hatch) !== hatch
            removedOpposite || replaced
        } else {
            val removedOpposite = computationProviders.remove(key) != null
            val replaced = computationReceivers.put(key, hatch) !== hatch
            removedOpposite || replaced
        }
        if (changed) invalidateComputationTopology()
    }

    fun unregisterComputationHatch(hatch: CloudOpticalComputationHatchMachine) {
        val key = keyOf(hatch) ?: return
        val machines = if (hatch.isTransmitter()) computationProviders else computationReceivers
        if (machines.remove(key, hatch)) invalidateComputationTopology()
    }

    fun registerDataMachine(machine: CloudOpticalDataMachine) {
        keyOf(machine)?.let { dataMachines[it] = machine }
    }

    fun unregisterDataMachine(machine: CloudOpticalDataMachine) {
        val key = keyOf(machine) ?: return
        dataMachines.remove(key, machine)
    }

    fun invalidateComputationTopology() {
        topologyDirty = true
        topologyVersion++
        ComputationNetwork.invalidate()
    }

    fun getTopologyVersion(): Long = topologyVersion

    fun getProviderCount(teamId: UUID?): Int = getTeamState(teamId).providers.count { it.canBridgeCloud() }

    fun getMaxCWU(teamId: UUID?): Long = ComputationNetwork.capacity(computationLinks(teamId))

    fun getRemainingCWU(teamId: UUID?): Long = ComputationNetwork.request(computationLinks(teamId), Long.MAX_VALUE, true)

    fun requestCWU(teamId: UUID?, cwu: Long, simulate: Boolean): Long =
        ComputationNetwork.request(computationLinks(teamId), cwu, simulate)

    fun isRecipeAvailableInCloud(recipe: GTRecipe, teamId: UUID?): Boolean {
        val normalizedTeamId = CloudTeamUtil.normalize(teamId) ?: return false
        for (machine in dataMachines.values) {
            if (normalizedTeamId != CloudTeamUtil.normalize(machine.getUUID())) continue
            if (machine.providesRecipe(recipe)) return true
        }
        return false
    }

    fun tryUploadResearchData(dataStack: ItemStack, teamId: UUID?): Boolean {
        val normalizedTeamId = CloudTeamUtil.normalize(teamId) ?: return false
        if (dataStack.isEmpty) return false
        for (machine in dataMachines.values) {
            if (normalizedTeamId != CloudTeamUtil.normalize(machine.uuid)) continue
            if (machine.tryStoreResearchData(dataStack)) return true
        }
        return false
    }

    fun getLoadedDataMachineCount(teamId: UUID?): Int {
        val normalizedTeamId = CloudTeamUtil.normalize(teamId) ?: return 0
        return dataMachines.values.count { machine ->
            normalizedTeamId == CloudTeamUtil.normalize(machine.uuid)
        }
    }

    fun getComputationTopologySnapshot(teamId: UUID?): ComputationTopologySnapshot {
        val normalizedTeamId = CloudTeamUtil.normalize(teamId)
            ?: return ComputationTopologySnapshot(
                topologyVersion,
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList()
            )

        ensureTopologyCache()
        val state = teamStates[normalizedTeamId] ?: emptyTeamState
        val providers = sortedProviders(state).map { provider ->
            val machine = provider as MetaMachine
            val canBridge = provider.canBridgeCloud()
            machineSnapshot(
                machine,
                if (canBridge) provider.getRemainingCloudCWU() else 0L,
                if (canBridge) provider.getMaxCloudCWU() else 0L,
                0L
            )
        }

        val receivers = sortedReceivers(state)
            .map { machineSnapshot(it, 0L, 0L, getRequestedCWU(it)) }

        val unboundProviders = computationProviders.values
            .filter { it.getUUID() == null }
            .sortedWith(machineLocationOrder)
            .map { machineSnapshot(it, 0L, 0L, 0) }
        val unboundReceivers = computationReceivers.values
            .filter { it.getUUID() == null }
            .sortedWith(machineLocationOrder)
            .map { machineSnapshot(it, 0L, 0L, 0) }
        return ComputationTopologySnapshot(
            topologyVersion,
            providers,
            receivers,
            unboundProviders,
            unboundReceivers
        )
    }

    fun getComputationLiveSnapshot(teamId: UUID?): ComputationLiveSnapshot {
        val normalizedTeamId =
            CloudTeamUtil.normalize(teamId) ?: return ComputationLiveSnapshot(LongArray(0), LongArray(0), LongArray(0))
        ensureTopologyCache()
        val state = teamStates[normalizedTeamId] ?: emptyTeamState
        val providers = sortedProviders(state)
        val currentCwu = LongArray(providers.size)
        val maxCwu = LongArray(providers.size)
        providers.forEachIndexed { index, provider ->
            val canBridge = provider.canBridgeCloud()
            currentCwu[index] = if (canBridge) provider.getRemainingCloudCWU() else 0L
            maxCwu[index] = if (canBridge) provider.getMaxCloudCWU() else 0L
        }
        val receivers = sortedReceivers(state)
        val requestedCwu = LongArray(receivers.size) { getRequestedCWU(receivers[it]) }
        return ComputationLiveSnapshot(currentCwu, maxCwu, requestedCwu)
    }

    fun clearLevel(dimension: ResourceKey<Level>) {
        var computationRemoved = computationProviders.keys.removeIf { it.dimension() == dimension }
        computationRemoved = computationReceivers.keys.removeIf { it.dimension() == dimension } || computationRemoved
        dataMachines.keys.removeIf { it.dimension() == dimension }
        if (computationRemoved) invalidateComputationTopology()
    }

    fun clearAll() {
        val computationRemoved = computationProviders.isNotEmpty() || computationReceivers.isNotEmpty()
        computationProviders.clear()
        computationReceivers.clear()
        dataMachines.clear()
        teamStates.clear()
        topologyDirty = true
        if (computationRemoved) invalidateComputationTopology()
    }

    private fun ensureTopologyCache() {
        if (!topologyDirty) return
        teamStates.clear()
        for (hatch in computationProviders.values) {
            val teamId = CloudTeamUtil.normalize(hatch.getUUID()) ?: continue
            val state = teamStates.computeIfAbsent(teamId) { TeamState() }
            for (controller in hatch.controllers) {
                if (controller is NetworkSwitchMachine) continue
                val provider = controller as? IOpticalComputationProvider ?: continue
                state.providers += provider
            }
        }
        for (hatch in computationReceivers.values) {
            val teamId = CloudTeamUtil.normalize(hatch.getUUID()) ?: continue
            val state = teamStates.computeIfAbsent(teamId) { TeamState() }
            for (controller in hatch.controllers) {
                if (controller is MetaMachine) state.receiverControllers += controller
            }
        }
        topologyDirty = false
    }

    private fun getTeamState(teamId: UUID?): TeamState {
        val normalizedTeamId = CloudTeamUtil.normalize(teamId) ?: return emptyTeamState
        ensureTopologyCache()
        return teamStates[normalizedTeamId] ?: emptyTeamState
    }

    fun computationLinks(teamId: UUID?): List<IOpticalComputationProvider> = getTeamState(teamId).providers.toList()

    private fun sortedProviders(state: TeamState): List<IOpticalComputationProvider> =
        state.providers.filter { it is MetaMachine }.sortedWith(providerLocationOrder)

    private fun sortedReceivers(state: TeamState): List<MetaMachine> =
        state.receiverControllers.sortedWith(machineLocationOrder)

    private fun getDimensionId(machine: MetaMachine): String =
        machine.level?.dimension()?.location()?.toString().orEmpty()

    private fun machineSnapshot(
        machine: MetaMachine,
        currentCwu: Long,
        maxCwu: Long,
        requestedCwu: Long
    ): CloudMachineSnapshot {
        val level = machine.level
        val dimensionId = getDimensionId(machine)
        val frontPos = level?.let { machine.pos.relative(machine.frontFacing) }
        return CloudMachineSnapshot(
            dimensionId,
            machine.pos,
            frontPos,
            machine.definition.asStack(),
            currentCwu,
            maxCwu,
            requestedCwu
        )
    }

    private fun getRequestedCWU(machine: MetaMachine): Long {
        val recipeMachine = machine as? IRecipeLogicMachine ?: return 0L
        val recipeLogic = recipeMachine.recipeLogic
        if (!recipeLogic.isWorking) return 0L
        return (recipeLogic as? ComputationUsage)?.`gtlcore$usedComputation`() ?: 0L
    }

    private fun keyOf(machine: MetaMachine): GlobalPos? {
        val level = machine.level as? ServerLevel ?: return null
        return GlobalPos.of(level.dimension(), machine.pos.immutable())
    }
}