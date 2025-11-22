package com.gtladd.gtladditions.network

import com.gtladd.gtladditions.common.data.MachineInfo
import com.gtladd.gtladditions.utils.antichrist.ClientRingBlockHelper
import net.minecraft.client.Minecraft
import net.minecraft.core.Direction
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.fml.DistExecutor
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

class SyncDimensionMachinesPacket(
    private val dimensionKey: ResourceKey<Level>,
    private val machines: Array<MachineInfo>
) {

    companion object {
        fun encode(packet: SyncDimensionMachinesPacket, buffer: FriendlyByteBuf) {
            buffer.writeResourceLocation(packet.dimensionKey.location())
            buffer.writeVarInt(packet.machines.size)
            for (machine in packet.machines) {
                buffer.writeLong(machine.posLong)
                buffer.writeEnum(machine.facing)
            }
        }

        fun decode(buffer: FriendlyByteBuf): SyncDimensionMachinesPacket {
            val dimensionLoc = buffer.readResourceLocation()
            val dimensionKey = ResourceKey.create(GTLAddNetworking.DIMENSION_REGISTRY_KEY, dimensionLoc)
            val count = buffer.readVarInt()
            val machines = Array(count) {
                MachineInfo(
                    buffer.readLong(),
                    buffer.readEnum(Direction::class.java)
                )
            }
            return SyncDimensionMachinesPacket(dimensionKey, machines)
        }

        fun handle(packet: SyncDimensionMachinesPacket, contextSupplier: Supplier<NetworkEvent.Context>) {
            val context = contextSupplier.get()
            context.enqueueWork {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT) {
                    Runnable {
                        val level = Minecraft.getInstance().level ?: return@Runnable
                        if (level.dimension() == packet.dimensionKey) {
                            ClientRingBlockHelper.syncDimensionMachines(level, packet.machines)
                        }
                    }
                }
            }
            context.packetHandled = true
        }
    }
}