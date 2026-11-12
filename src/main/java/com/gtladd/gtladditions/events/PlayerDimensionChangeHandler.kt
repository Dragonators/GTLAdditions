package com.gtladd.gtladditions.events

import com.gtladd.gtladditions.GTLAdditions.Companion.MOD_ID
import com.gtladd.gtladditions.integration.stargate.StargateDimensions.VALID_TAG
import com.gtladd.gtladditions.integration.stargate.StargateDimensions.isTargetDimension
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Suppress("unused")
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object PlayerDimensionChangeHandler {

    @SubscribeEvent
    @JvmStatic
    fun onPlayerChangedDimension(event: PlayerEvent.PlayerChangedDimensionEvent) {
        (event.entity as? ServerPlayer)?.let { player ->
            if (!canPlayerChangeDimension(event.to, player)) {
                transportBackToOverWorld(player)
            }
        }
    }

    private fun canPlayerChangeDimension(toDim: ResourceKey<Level>, player: ServerPlayer): Boolean {
        val toId = toDim.location()

        if (!isTargetDimension(toId)) return true

        if (player.tags.contains(VALID_TAG)) {
            player.removeTag(VALID_TAG)
            return true
        }
        return false
    }

    private fun transportBackToOverWorld(player: ServerPlayer) {
        player.teleportTo(player.server.overworld(), 0.0, 128.0, 0.0, player.yRot, player.xRot)
        player.sendSystemMessage(
            "tooltip.gtladditions.dimension.forbidden".toComponent
        )
    }
}