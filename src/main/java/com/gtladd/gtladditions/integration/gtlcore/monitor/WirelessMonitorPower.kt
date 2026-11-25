package com.gtladd.gtladditions.integration.gtlcore.monitor

import com.gtladd.gtladditions.utils.CommonUtils
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import java.math.BigInteger

object WirelessMonitorPower {
    const val TAG = "wirelessTickInputs"

    @JvmStatic
    fun hasNonZeroWirelessTickInputs(state: CompoundTag): Boolean =
        state.contains(TAG, Tag.TAG_BYTE_ARRAY.toInt()) && state.getByteArray(TAG).any { it.toInt() != 0 }

    @JvmStatic
    fun energy(state: CompoundTag, compact: Boolean): Component? {
        if (state.getCompound("Recipe").getLong("EUt") > 0 ||
            !state.contains(TAG, Tag.TAG_BYTE_ARRAY.toInt())
        ) {
            return null
        }

        val bytes = state.getByteArray(TAG)
        if (bytes.all { it.toInt() == 0 }) return Component.literal("—")

        val signed = BigInteger(bytes)
        val magnitude = signed.abs()
        val input = signed.signum() < 0
        val power = if (compact) {
            CommonUtils.formatCompactBigInteger(magnitude)
        } else {
            CommonUtils.formatBigIntegerFixed(magnitude)
        }

        return Component.translatable(if (input) "monitor.gtlcore.machine.consume" else "monitor.gtlcore.machine.produce")
            .append(Component.literal("$power EU/t").withStyle(if (input) ChatFormatting.RED else ChatFormatting.GREEN))
            .append(Component.literal(" (").withStyle(ChatFormatting.GREEN))
            .append(CommonUtils.getEnergyTierComponent(magnitude))
            .append(Component.literal(")").withStyle(ChatFormatting.GREEN))
    }
}
