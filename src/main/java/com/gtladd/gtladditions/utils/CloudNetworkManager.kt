package com.gtladd.gtladditions.utils

import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider
import com.gregtechceu.gtceu.api.capability.recipe.CWURecipeCapability
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
import net.minecraft.world.level.Level
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
    }

    fun getTopologyVersion(): Long = topologyVersion

    fun getProviderCount(teamId: UUID?): Int = getTeamState(teamId).providers.count { it.canBridgeCloud() }

    fun getMaxCWU(teamId: UUID?): Long {
        var total = 0L
        for (provider in getTeamState(teamId).providers) {
            if (!provider.canBridgeCloud()) continue
            total = saturatedAdd(total, provider.getMaxCloudCWU())
            if (total == Long.MAX_VALUE) return total
        }
        return total
    }

    fun getRemainingCWU(teamId: UUID?): Long {
        var total = 0L
        for (provider in getTeamState(teamId).providers) {
            if (!provider.canBridgeCloud()) continue
            total = saturatedAdd(total, provider.getRemainingCloudCWU())
            if (total == Long.MAX_VALUE) return total
        }
        return total
    }

    fun requestCWU(teamId: UUID?, cwu: Long, simulate: Boolean): Long {
        if (cwu <= 0L || CloudTeamUtil.normalize(teamId) == null) return 0L
        val providers = getTeamState(teamId).providers
        if (providers.any { it.canBridgeCloud() && it.isUnlimitedCloudProvider() }) return cwu
        var drawn = 0L
        for (provider in providers) {
            if (!provider.canBridgeCloud()) continue
            val remaining = cwu - drawn
            drawn += provider.requestCloudCWU(remaining, simulate).coerceIn(0L, remaining)
            if (drawn >= cwu) break
        }
        return drawn
    }

    fun isRecipeAvailableInCloud(recipe: GTRecipe, teamId: UUID?): Boolean {
        val normalizedTeamId = CloudTeamUtil.normalize(teamId) ?: return false
        for (machine in dataMachines.values) {
            if (normalizedTeamId != CloudTeamUtil.normalize(machine.getUUID())) continue
            if (machine.providesRecipe(recipe)) return true
        }
        return false
    }

    fun getLoadedDataMachineCount(): Int = dataMachines.size

    fun getComputationTopologySnapshot(teamId: UUID?): ComputationTopologySnapshot {
        val normalizedTeamId = CloudTeamUtil.normalize(teamId)
            ?: return ComputationTopologySnapshot(topologyVersion, emptyList(), emptyList(), 0, 0)

        ensureTopologyCache()
        val state = teamStates[normalizedTeamId] ?: emptyTeamState
        val providers = sortedProviders(state).map { provider ->
            val machine = provider as MetaMachine
            val canBridge = provider.canBridgeCloud()
            val unlimited = canBridge && provider.isUnlimitedCloudProvider()
            machineSnapshot(
                machine,
                if (unlimited) {
                    Long.MAX_VALUE
                } else if (canBridge) {
                    provider.getRemainingCloudCWU()
                } else {
                    0L
                },
                if (unlimited) {
                    Long.MAX_VALUE
                } else if (canBridge) {
                    provider.getMaxCloudCWU()
                } else {
                    0L
                },
                0
            )
        }

        val receivers = sortedReceivers(state)
            .map { machineSnapshot(it, 0L, 0L, getRequestedCWU(it)) }

        val otherProviderCount = computationProviders.values.count { hatch ->
            hatch.getUUID() != null && !CloudTeamUtil.sameTeam(hatch.getUUID(), teamId)
        }
        val otherReceiverCount = computationReceivers.values.count { hatch ->
            hatch.getUUID() != null && !CloudTeamUtil.sameTeam(hatch.getUUID(), teamId)
        }
        return ComputationTopologySnapshot(
            topologyVersion,
            providers,
            receivers,
            otherProviderCount,
            otherReceiverCount
        )
    }

    fun getComputationLiveSnapshot(teamId: UUID?): ComputationLiveSnapshot {
        val normalizedTeamId =
            CloudTeamUtil.normalize(teamId) ?: return ComputationLiveSnapshot(LongArray(0), LongArray(0), IntArray(0))
        ensureTopologyCache()
        val state = teamStates[normalizedTeamId] ?: emptyTeamState
        val providers = sortedProviders(state)
        val currentCwu = LongArray(providers.size)
        val maxCwu = LongArray(providers.size)
        providers.forEachIndexed { index, provider ->
            val canBridge = provider.canBridgeCloud()
            val unlimited = canBridge && provider.isUnlimitedCloudProvider()
            currentCwu[index] = when {
                unlimited -> Long.MAX_VALUE
                canBridge -> provider.getRemainingCloudCWU()
                else -> 0L
            }
            maxCwu[index] = when {
                unlimited -> Long.MAX_VALUE
                canBridge -> provider.getMaxCloudCWU()
                else -> 0L
            }
        }
        val receivers = sortedReceivers(state)
        val requestedCwu = IntArray(receivers.size) { getRequestedCWU(receivers[it]) }
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
        if (computationRemoved) topologyVersion++
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

    private fun saturatedAdd(total: Long, value: Long): Long =
        if (Long.MAX_VALUE - total < value) Long.MAX_VALUE else total + value

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
        requestedCwu: Int
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

    private fun getRequestedCWU(machine: MetaMachine): Int {
        val recipeMachine = machine as? IRecipeLogicMachine ?: return 0
        val recipeLogic = recipeMachine.recipeLogic
        val recipe = recipeLogic.lastRecipe ?: return 0
        if (!recipeLogic.isWorking) return 0
        val cwuInputs = recipe.tickInputs[CWURecipeCapability.CAP] ?: return 0
        return cwuInputs.sumOf { CWURecipeCapability.CAP.of(it.content) }
    }

    private fun keyOf(machine: MetaMachine): GlobalPos? {
        val level = machine.level as? ServerLevel ?: return null
        return GlobalPos.of(level.dimension(), machine.pos.immutable())
    }
}