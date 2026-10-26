package com.gtladd.gtladditions.events

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.integration.stargate.StargateContent
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
object StargateCreativeTabEvents {
    @JvmStatic
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun hideDisabledContent(event: BuildCreativeModeTabContentsEvent) {
        val hidden = event.entries.filter { StargateContent.isDisabledItem(it.key.item) }.map { it.key }
        hidden.forEach { event.entries.remove(it) }
    }
}