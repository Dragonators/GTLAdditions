package com.gtladd.gtladditions.integration.jade.provider

import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.hepdd.gtmthings.utils.TeamUtil
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.Level
import snownee.jade.api.ITooltip
import java.util.UUID

object JadeTeamBindingHelper {
    private const val UUID_KEY = "uuid"

    fun writeBinding(tag: CompoundTag, uuid: UUID?) {
        uuid?.let { tag.putUUID(UUID_KEY, it) }
    }

    fun readBinding(tag: CompoundTag): UUID? = if (tag.hasUUID(UUID_KEY)) tag.getUUID(UUID_KEY) else null

    fun appendTooltip(tooltip: ITooltip, level: Level, tag: CompoundTag) {
        val uuid = readBinding(tag)
        if (uuid == null) {
            tooltip.add("gtmthings.machine.wireless_energy_hatch.tooltip.1".toComponent)
            return
        }

        if (TeamUtil.hasOwner(level, uuid)) {
            tooltip.add(
                "gtmthings.machine.wireless_energy_hatch.tooltip.2".toComponent(
                    TeamUtil.GetName(level, uuid)
                )
            )
        } else {
            tooltip.add("gtmthings.machine.wireless_energy_hatch.tooltip.3".toComponent(uuid))
        }
    }
}