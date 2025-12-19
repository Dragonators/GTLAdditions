package com.gtladd.gtladditions.integration.ae2

import appeng.api.storage.MEStorage
import appeng.capabilities.Capabilities
import com.gtladd.gtladditions.common.machine.hatch.InfinityDualHatchPartMachine
import net.minecraft.core.Direction
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.util.LazyOptional

class MEStorageCapabilityProvider(private val storageSupplier: () -> InfinityDualHatchPartMachine) : ICapabilityProvider {
    private val storageCap: LazyOptional<MEStorage> = LazyOptional.of(storageSupplier)

    override fun <T : Any?> getCapability(capability: Capability<T?>, direction: Direction?): LazyOptional<T?> {
        return if (capability == Capabilities.STORAGE && storageSupplier.invoke().frontFacing == direction) storageCap.cast() else LazyOptional.empty()
    }
}