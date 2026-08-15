package com.gtladd.gtladditions.integration.jade.provider

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.machine.multiblock.controller.bs.BiosphereIIIController
import com.gtladd.gtladditions.utils.CommonUtils
import com.gtladd.gtladditions.utils.ComponentExtensions.literal
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IBlockComponentProvider
import snownee.jade.api.IServerDataProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.config.IPluginConfig
import java.math.BigInteger

class BiosphereIIIEnergyProvider :
    IBlockComponentProvider,
    IServerDataProvider<BlockAccessor> {

    override fun appendTooltip(tooltip: ITooltip, accessor: BlockAccessor, config: IPluginConfig) {
        val blockEntity = accessor.blockEntity as? IMachineBlockEntity ?: return
        if (blockEntity.metaMachine !is BiosphereIIIController) return

        val serverData = accessor.serverData
        if (!serverData.contains(AVERAGE_ENERGY_KEY, Tag.TAG_BYTE_ARRAY.toInt())) return

        val averageEnergy = BigInteger(serverData.getByteArray(AVERAGE_ENERGY_KEY))
        tooltip.add(
            "gui.gtladditions.biosphere_iii_average_eut".toComponent(
                CommonUtils.formatBigIntegerFixed(averageEnergy).literal.withStyle(ChatFormatting.GOLD),
                BiosphereIIIController.getEnergyTierComponent(averageEnergy)
            )
        )
    }

    override fun appendServerData(tag: CompoundTag, accessor: BlockAccessor) {
        val blockEntity = accessor.blockEntity as? IMachineBlockEntity ?: return
        val controller = blockEntity.metaMachine as? BiosphereIIIController ?: return
        tag.putByteArray(AVERAGE_ENERGY_KEY, controller.getAverageEnergyConsumption().toByteArray())
    }

    override fun getUid(): ResourceLocation = GTLAdditions.id("biosphere_iii_energy")

    companion object {
        private const val AVERAGE_ENERGY_KEY = "biosphereIIIAverageEnergy"
    }
}