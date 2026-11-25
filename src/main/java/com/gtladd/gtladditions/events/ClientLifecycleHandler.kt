package com.gtladd.gtladditions.events

import com.gtladd.gtladditions.GTLAdditions.Companion.MOD_ID
import com.gtladd.gtladditions.client.gui.StructureTierPreview
import com.gtladd.gtladditions.client.gui.StructureTierRenderer
import com.gtladd.gtladditions.client.render.machine.ArcanicAstrographOrbits
import com.gtladd.gtladditions.client.render.machine.ArcanicAstrographStarRenderer
import com.gtladd.gtladditions.client.render.machine.heart.HeartShellRenderer
import com.gtladd.gtladditions.client.render.machine.matrix.MatrixEyeRenderer
import com.gtladd.gtladditions.client.render.machine.matrix.MatrixIrisStarRenderer
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.event.level.LevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Suppress("unused")
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object ClientLifecycleHandler {

    @SubscribeEvent
    @JvmStatic
    fun onLevelUnload(event: LevelEvent.Unload) {
        if (!event.level.isClientSide) return

        StructureTierPreview.clear()
        StructureTierRenderer.clear()
        MatrixEyeRenderer.clearAnimations()

        val release = {
            ArcanicAstrographOrbits.clearCache()
            ArcanicAstrographStarRenderer.releaseBuffers()
            HeartShellRenderer.onLevelUnload()
            MatrixEyeRenderer.releaseBuffers()
            MatrixIrisStarRenderer.releaseBuffers()
        }

        if (RenderSystem.isOnRenderThread()) release() else RenderSystem.recordRenderCall { release() }
    }

    // Highlights outlive a dimension change: the monitor can mark a machine in another dimension.
    @SubscribeEvent
    @JvmStatic
    fun onLoggingOut(event: ClientPlayerNetworkEvent.LoggingOut) {
        ClientCloudHighlighter.clear()
        StructureTierPreview.clear()
        StructureTierRenderer.clear()
    }
}
