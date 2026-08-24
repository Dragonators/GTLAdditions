package com.gtladd.gtladditions.utils

import com.hepdd.gtmthings.api.capability.IBindable
import com.hepdd.gtmthings.utils.TeamUtil
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import java.util.UUID

object CloudTeamUtil {

    fun normalize(uuid: UUID?): UUID? {
        if (uuid == null) return null
        return TeamUtil.getTeamUUID(uuid) ?: uuid
    }

    fun sameTeam(first: UUID?, second: UUID?): Boolean {
        val normalizedFirst = normalize(first) ?: return false
        return normalizedFirst == normalize(second)
    }

    fun bindOnPlacement(machine: IBindable, placer: LivingEntity?) {
        if (placer is Player && !placer.level().isClientSide) {
            machine.setUUID(placer.uuid)
        }
    }

    @Suppress("SameReturnValue")
    fun bindFromDataStick(machine: IBindable, player: Player): InteractionResult {
        if (player.level().isClientSide) return InteractionResult.SUCCESS
        machine.setUUID(player.uuid)
        if (player is ServerPlayer) {
            player.sendSystemMessage(
                Component.translatable("gui.gtladditions.cloud.bind_success", TeamUtil.GetName(player))
            )
        }
        return InteractionResult.SUCCESS
    }

    fun unbindFromDataStick(machine: IBindable, player: Player): Boolean {
        if (!player.level().isClientSide) {
            machine.uuid = null
            if (player is ServerPlayer) {
                player.sendSystemMessage(Component.translatable("gui.gtladditions.cloud.unbind_success"))
            }
        }
        return true
    }
}