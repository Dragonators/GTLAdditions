package com.gtladd.gtladditions.utils.antichrist

import com.gtladd.gtladditions.common.data.MachineInfo
import com.gtladd.gtladditions.network.GTLAddNetworking
import com.gtladd.gtladditions.network.MachineDeltaPacket
import com.gtladd.gtladditions.network.SyncDimensionMachinesPacket
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.minecraftforge.network.PacketDistributor

object ServerMachineManager {
    private val DIMENSION_MACHINES = Object2ReferenceOpenHashMap<ResourceKey<Level>, Long2ReferenceMap<Direction>>()

    fun registerMachine(level: ServerLevel, pos: BlockPos, facing: Direction) {
        val machines = DIMENSION_MACHINES.computeIfAbsent(level.dimension()) { Long2ReferenceOpenHashMap() }
        machines[pos.asLong()] = facing
        broadcastMachineDelta(level, pos, facing, add = true)
    }

    fun unregisterMachine(level: ServerLevel, pos: BlockPos) {
        val machines = DIMENSION_MACHINES[level.dimension()] ?: return
        val facing = machines.remove(pos.asLong()) ?: return

        if (machines.isEmpty()) DIMENSION_MACHINES.remove(level.dimension())

        broadcastMachineDelta(level, pos, facing, add = false)
    }

    fun syncAllToPlayer(player: ServerPlayer) {
        val dimensionKey = player.serverLevel().dimension()
        val machines = DIMENSION_MACHINES[dimensionKey] ?: return

        val machineArray = machines.long2ReferenceEntrySet().map { entry ->
            MachineInfo(entry.longKey, entry.value)
        }.toTypedArray()

        GTLAddNetworking.CHANNEL.send(
            PacketDistributor.PLAYER.with { player },
            SyncDimensionMachinesPacket(dimensionKey, machineArray)
        )
    }

    private fun broadcastMachineDelta(level: ServerLevel, pos: BlockPos, facing: Direction, add: Boolean) {
        val packet = MachineDeltaPacket(level.dimension(), pos, facing, add)
        for (player in level.players()) {
            if (player is ServerPlayer) {
                GTLAddNetworking.CHANNEL.send(
                    PacketDistributor.PLAYER.with { player },
                    packet
                )
            }
        }
    }

    internal fun clearDimension(dimensionKey: ResourceKey<Level>) {
        DIMENSION_MACHINES.remove(dimensionKey)
    }

    internal fun clearAll() {
        DIMENSION_MACHINES.clear()
    }
}