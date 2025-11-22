package com.gtladd.gtladditions.events

import com.gtladd.gtladditions.GTLAdditions.Companion.MOD_ID
import com.gtladd.gtladditions.utils.CommonUtils.VALID_TAG
import com.gtladd.gtladditions.utils.CommonUtils.isTargetDimension
import com.gtladd.gtladditions.utils.antichrist.ClientRingBlockHelper
import com.gtladd.gtladditions.utils.antichrist.ServerMachineManager
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
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
            if (canPlayerChangeDimension(event.to, player))
                ServerMachineManager.syncAllToPlayer(player)
            else
                transportBackToOverWorld(player)
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun onPlayerLoggedIn(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity
        if (player is ServerPlayer) {
            ServerMachineManager.syncAllToPlayer(player)
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    @JvmStatic
    fun onPlayerLoggedOut(event: ClientPlayerNetworkEvent.LoggingOut) {
        ClientRingBlockHelper.clearAllData()
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
            Component.translatable("tooltip.gtladditions.dimension.forbidden")
        )
    }
}