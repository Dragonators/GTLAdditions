package com.gtladd.gtladditions.integration.jade.provider

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.machine.multiblock.part.WirelessEnergyNetworkTerminalPartMachineBase
import com.gtladd.gtladditions.utils.CommonUtils
import com.gtladd.gtladditions.utils.ComponentExtensions.literal
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.hepdd.gtmthings.api.misc.WirelessEnergyManager
import com.hepdd.gtmthings.utils.TeamUtil
import net.minecraft.ChatFormatting.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import org.gtlcore.gtlcore.integration.gtmt.NewGTValues
import org.gtlcore.gtlcore.utils.NumberUtils
import org.gtlcore.gtlcore.utils.TextUtil
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IBlockComponentProvider
import snownee.jade.api.IServerDataProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.config.IPluginConfig
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

class WirelessEnergyNetworkTerminalProvider :
    IBlockComponentProvider,
    IServerDataProvider<BlockAccessor> {
    override fun appendTooltip(tooltip: ITooltip, blockAccessor: BlockAccessor, iPluginConfig: IPluginConfig) {
        val blockEntity = blockAccessor.blockEntity
        if (blockEntity is IMachineBlockEntity) {
            if (blockEntity.metaMachine is WirelessEnergyNetworkTerminalPartMachineBase) {
                val serverData = blockAccessor.serverData
                if (!serverData.hasUUID("uuid")) {
                    tooltip.add("gtmthings.machine.wireless_energy_hatch.tooltip.1".toComponent)
                } else {
                    val uuid = serverData.getUUID("uuid")
                    if (TeamUtil.hasOwner(blockAccessor.level, uuid)) {
                        tooltip.add(
                            "gtmthings.machine.wireless_energy_hatch.tooltip.2".toComponent(TeamUtil.GetName(blockAccessor.level, uuid))
                        )
                    } else {
                        tooltip.add("gtmthings.machine.wireless_energy_hatch.tooltip.3".toComponent(uuid))
                    }

                    val totalEu = BigInteger(serverData.getByteArray("totalEu"))
                    val abs = totalEu.abs()
                    val longEu = NumberUtils.getLongValue(totalEu)
                    val energyTier = if (longEu == Long.MAX_VALUE) GTValues.MAX_TRUE else NumberUtils.getFakeVoltageTier(longEu)

                    val text = (CommonUtils.format2Double(abs.toDouble())).literal.withStyle(RED)
                        .append(
                            " EU".literal.withStyle(RESET)
                                .append(
                                    " (".literal.withStyle(GREEN)
                                        .append(
                                            "gtceu.top.electricity".toComponent(
                                                String.format(
                                                    "%.2e",
                                                    BigDecimal(abs).divide(
                                                        BigDecimal.valueOf(GTValues.VEX[energyTier]),
                                                        3,
                                                        RoundingMode.DOWN
                                                    ).toDouble()
                                                ),
                                                NewGTValues.VNF[energyTier]
                                            ).withStyle { style ->
                                                style.withColor(
                                                    TextUtil.`GTL_CORE$VC`[
                                                        energyTier.coerceAtMost(
                                                            14
                                                        )
                                                    ]
                                                )
                                            }
                                        )
                                        .append(")".literal.withStyle(GREEN))
                                )
                        )

                    tooltip.add(
                        "gtladditions.machine.wireless_energy_network_terminal.tooltips.1".toComponent(text)
                    )
                }
            }
        }
    }

    override fun appendServerData(compoundTag: CompoundTag, blockAccessor: BlockAccessor) {
        val blockEntity = blockAccessor.blockEntity
        if (blockEntity is IMachineBlockEntity) {
            val machine = blockEntity.metaMachine
            if (machine is WirelessEnergyNetworkTerminalPartMachineBase) {
                machine.uuid?.let {
                    compoundTag.putUUID("uuid", it)
                    compoundTag.putByteArray("totalEu", WirelessEnergyManager.getUserEU(it).toByteArray())
                }
            }
        }
    }

    override fun getUid(): ResourceLocation = GTLAdditions.id("wireless_energy_network_terminal")
}