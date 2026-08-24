package com.gtladd.gtladditions.integration.jade

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity
import com.gtladd.gtladditions.integration.jade.provider.BiosphereIIIEnergyProvider
import com.gtladd.gtladditions.integration.jade.provider.CloudBindingProvider
import com.gtladd.gtladditions.integration.jade.provider.GTFluidStorageProvider
import com.gtladd.gtladditions.integration.jade.provider.GTItemStorageProvider
import com.gtladd.gtladditions.integration.jade.provider.InfinityDualHatchProvider
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
        registration.registerBlockDataProvider(BiosphereIIIEnergyProvider(), BlockEntity::class.java)
        registration.registerBlockDataProvider(CloudBindingProvider(), BlockEntity::class.java)
        registration.registerBlockDataProvider(WirelessEnergyNetworkTerminalProvider(), BlockEntity::class.java)
        registration.registerBlockDataProvider(InfinityDualHatchProvider(), BlockEntity::class.java)
        registration.registerItemStorage(GTItemStorageProvider, MetaMachineBlockEntity::class.java)
        registration.registerFluidStorage(GTFluidStorageProvider, MetaMachineBlockEntity::class.java)
    }

    override fun registerClient(registration: IWailaClientRegistration) {
        registration.registerBlockComponent(BiosphereIIIEnergyProvider(), Block::class.java)
        registration.registerBlockComponent(CloudBindingProvider(), Block::class.java)
        registration.registerBlockComponent(WirelessEnergyNetworkTerminalProvider(), Block::class.java)
        registration.registerBlockComponent(InfinityDualHatchProvider(), Block::class.java)
        registration.registerItemStorageClient(GTItemStorageProvider)
        registration.registerFluidStorageClient(GTFluidStorageProvider)
    }
}