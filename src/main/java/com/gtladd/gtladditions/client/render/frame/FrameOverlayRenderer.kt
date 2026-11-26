package com.gtladd.gtladditions.client.render.frame

import com.gtladd.gtladditions.GTLAdditions
import net.irisshaders.iris.api.v0.IrisApi
import net.minecraft.client.Minecraft
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, value = [Dist.CLIENT])
object FrameOverlayRenderer {
    @Volatile
    var enabled = false
        private set

    @SubscribeEvent
    @JvmStatic
    fun onRenderTick(event: TickEvent.RenderTickEvent) {
        if (event.phase != TickEvent.Phase.START) return

        val mc = Minecraft.getInstance()
        val level = mc.level
        val shouldEnable = level != null && !IrisApi.getInstance().isShaderPackInUse
        if (enabled == shouldEnable) return

        enabled = shouldEnable
        // Mesh routing changes when a shader pack is toggled.
        if (level != null) mc.levelRenderer.allChanged()
    }

    @SubscribeEvent
    @JvmStatic
    fun onRenderLevelStage(event: RenderLevelStageEvent) {
        if (enabled && event.stage == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            FrameOverlayTerrainPass.draw(event)
        }
    }
}
