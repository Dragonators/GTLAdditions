@file:Suppress("ktlint:standard:filename")

package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.GTLAdditions
import net.minecraft.client.Minecraft
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RenderGuiEvent
import net.minecraftforge.client.event.ScreenEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Suppress("unused")
@Mod.EventBusSubscriber(
    modid = GTLAdditions.MOD_ID,
    bus = Mod.EventBusSubscriber.Bus.FORGE,
    value = [Dist.CLIENT]
)
internal object HeartBlackHoleGuiCompositeEvents {
    @SubscribeEvent
    @JvmStatic
    fun onRenderGuiPre(event: RenderGuiEvent.Pre) {
        HeartBlackHoleRenderer.renderPendingComposite(
            event.window.width,
            event.window.height
        )
    }

    @SubscribeEvent
    @JvmStatic
    fun onScreenRenderPre(event: ScreenEvent.Render.Pre) {
        val window = Minecraft.getInstance().window
        HeartBlackHoleRenderer.renderPendingComposite(
            window.width,
            window.height
        )
    }
}