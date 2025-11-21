package com.gtladd.gtladditions.integration.jade

import com.gtladd.gtladditions.integration.jade.provider.WirelessEnergyNetworkTerminalProvider
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import snownee.jade.api.IWailaClientRegistration
import snownee.jade.api.IWailaCommonRegistration
import snownee.jade.api.IWailaPlugin
import snownee.jade.api.WailaPlugin

@Suppress("unused")
@WailaPlugin
class GTLAddJadePlugin : IWailaPlugin {
    override fun register(registration: IWailaCommonRegistration) {
        registration.registerBlockDataProvider(WirelessEnergyNetworkTerminalProvider(), BlockEntity::class.java)
    }

    override fun registerClient(registration: IWailaClientRegistration) {
        registration.registerBlockComponent(WirelessEnergyNetworkTerminalProvider(), Block::class.java)
    }
}