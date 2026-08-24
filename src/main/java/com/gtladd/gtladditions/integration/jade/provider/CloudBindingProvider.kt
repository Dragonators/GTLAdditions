package com.gtladd.gtladditions.integration.jade.provider

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.machine.CloudOpticalComputationMonitorMachine
import com.gtladd.gtladditions.common.machine.CloudOpticalDataMachine
import com.gtladd.gtladditions.common.machine.hatch.CloudOpticalComputationHatchMachine
import com.gtladd.gtladditions.common.machine.hatch.CloudOpticalDataHatchMachine
import com.hepdd.gtmthings.api.capability.IBindable
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IBlockComponentProvider
import snownee.jade.api.IServerDataProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.config.IPluginConfig

class CloudBindingProvider :
    IBlockComponentProvider,
    IServerDataProvider<BlockAccessor> {

    override fun appendTooltip(tooltip: ITooltip, accessor: BlockAccessor, config: IPluginConfig) {
        getCloudBindable(accessor)?.let {
            JadeTeamBindingHelper.appendTooltip(tooltip, accessor.level, accessor.serverData)
        }
    }

    override fun appendServerData(tag: CompoundTag, accessor: BlockAccessor) {
        JadeTeamBindingHelper.writeBinding(tag, getCloudBindable(accessor)?.uuid)
    }

    override fun getUid(): ResourceLocation = GTLAdditions.id("cloud_binding")

    private fun getCloudBindable(accessor: BlockAccessor): IBindable? {
        val blockEntity = accessor.blockEntity as? IMachineBlockEntity ?: return null
        return when (val machine = blockEntity.metaMachine) {
            is CloudOpticalComputationMonitorMachine,
            is CloudOpticalDataMachine,
            is CloudOpticalComputationHatchMachine,
            is CloudOpticalDataHatchMachine -> machine as IBindable
            else -> null
        }
    }
}