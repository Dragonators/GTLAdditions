package com.gtladd.gtladditions.network

import com.gtladd.gtladditions.utils.antichrist.ClientRingBlockHelper
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.fml.DistExecutor
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

class MachineDeltaPacket(
    private val dimensionKey: ResourceKey<Level>,
    private val posLong: Long,
    private val facing: Direction,
    private val add: Boolean
) {
    constructor(
        dimensionKey: ResourceKey<Level>,
        pos: BlockPos,
        facing: Direction,
        add: Boolean
    ) : this(dimensionKey, pos.asLong(), facing, add)

    companion object {
        fun encode(packet: MachineDeltaPacket, buffer: FriendlyByteBuf) {
            buffer.writeResourceLocation(packet.dimensionKey.location())
            buffer.writeLong(packet.posLong)
            buffer.writeEnum(packet.facing)
            buffer.writeBoolean(packet.add)
        }

        fun decode(buffer: FriendlyByteBuf): MachineDeltaPacket {
            val dimensionLoc = buffer.readResourceLocation()
            val dimensionKey = ResourceKey.create(GTLAddNetworking.DIMENSION_REGISTRY_KEY, dimensionLoc)
            val posLong = buffer.readLong()
            val facing = buffer.readEnum(Direction::class.java)
            val add = buffer.readBoolean()
            return MachineDeltaPacket(dimensionKey, posLong, facing, add)
        }

        fun handle(packet: MachineDeltaPacket, contextSupplier: Supplier<NetworkEvent.Context>) {
            val context = contextSupplier.get()
            context.enqueueWork {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT) {
                    Runnable {
                        val level = Minecraft.getInstance().level ?: return@Runnable
                        if (level.dimension() == packet.dimensionKey) {
                            if (packet.add) {
                                ClientRingBlockHelper.hideRingsAtPosition(level, packet.posLong, packet.facing)
                            } else {
                                ClientRingBlockHelper.restoreRingsAtPosition(level, packet.posLong, packet.facing)
                            }
                        }
                    }
                }
            }
            context.packetHandled = true
        }
    }
}