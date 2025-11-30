package com.gtladd.gtladditions.integration.jade.provider

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.machine.hatch.InfinityDualHatchPartMachine
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEPatternBufferPartMachine
import snownee.jade.addon.universal.FluidStorageProvider
import snownee.jade.api.Accessor
import snownee.jade.api.view.*

object GTFluidStorageProvider : IServerExtensionProvider<MetaMachineBlockEntity, CompoundTag>,
    IClientExtensionProvider<CompoundTag, FluidView> {
    override fun getGroups(
        serverPlayer: ServerPlayer,
        serverLevel: ServerLevel,
        mmbe: MetaMachineBlockEntity,
        b: Boolean
    ): List<ViewGroup<CompoundTag>>? {
        val machine = mmbe.getMetaMachine()
        return if (machine is InfinityDualHatchPartMachine || machine is MEPatternBufferPartMachine)
            listOf()
        else
            FluidStorageProvider.INSTANCE.getGroups(serverPlayer, serverLevel, mmbe, b)
    }

    override fun getUid(): ResourceLocation = GTLAdditions.id("ban_fluid_storage")

    override fun getClientGroups(
        accessor: Accessor<*>,
        groups: List<ViewGroup<CompoundTag>>
    ): List<ClientViewGroup<FluidView>> {
        return ClientViewGroup.map(
            groups,
            FluidView::readDefault,
            null
        )
    }
}