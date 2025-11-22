package com.gtladd.gtladditions.network

import com.gtladd.gtladditions.GTLAdditions
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.Level
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.simple.SimpleChannel

object GTLAddNetworking {
    private const val PROTOCOL_VERSION = "1"

    val DIMENSION_REGISTRY_KEY: ResourceKey<Registry<Level>> = ResourceKey.createRegistryKey(
        ResourceLocation("minecraft", "dimension")
    )

    val CHANNEL: SimpleChannel = NetworkRegistry.newSimpleChannel(
        ResourceLocation(GTLAdditions.MOD_ID, "main"),
        { PROTOCOL_VERSION },
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    )

    private var messageId = 0

    fun init() {
        CHANNEL.registerMessage(
            messageId++,
            SyncDimensionMachinesPacket::class.java,
            SyncDimensionMachinesPacket::encode,
            SyncDimensionMachinesPacket::decode,
            SyncDimensionMachinesPacket::handle
        )

        CHANNEL.registerMessage(
            messageId++,
            MachineDeltaPacket::class.java,
            MachineDeltaPacket::encode,
            MachineDeltaPacket::decode,
            MachineDeltaPacket::handle
        )
    }
}