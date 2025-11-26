package com.gtladd.gtladditions.events

import com.gtladd.gtladditions.GTLAdditions.Companion.MOD_ID
import com.gtladd.gtladditions.utils.antichrist.ServerMachineManager
import net.minecraft.server.level.ServerLevel
import net.minecraftforge.event.level.LevelEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Suppress("unused")
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object ServerLifecycleHandler {

    @SubscribeEvent
    @JvmStatic
    fun onLevelUnload(event: LevelEvent.Unload) {
        (event.level as? ServerLevel)?.let {
            ServerMachineManager.clearDimension(it.dimension())
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun onServerStopping(event: ServerStoppingEvent) {
        ServerMachineManager.clearAll()
    }
}