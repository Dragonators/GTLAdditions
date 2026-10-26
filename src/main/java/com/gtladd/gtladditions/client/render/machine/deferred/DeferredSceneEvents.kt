package com.gtladd.gtladditions.client.render.machine.deferred

import com.gtladd.gtladditions.GTLAdditions
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/**
 * Scene composition order:
 * - Render tick START clears submissions and hand-mask state.
 * - AFTER_BLOCK_ENTITIES collects visible black holes before Oculus renders the hand.
 * - DeferredMachineRenderer draws ordinary VFX; overlapping transparent effects join the prepared scene.
 * - AFTER_LEVEL composites that scene over the finished world using the completed hand mask.
 *
 * LOWEST keeps preparation after terrain entries. Do not move terrain geometry into these passes.
 */
@Suppress("unused")
@Mod.EventBusSubscriber(
    modid = GTLAdditions.MOD_ID,
    bus = Mod.EventBusSubscriber.Bus.FORGE,
    value = [Dist.CLIENT]
)
internal object DeferredSceneEvents {
    @SubscribeEvent
    @JvmStatic
    fun onRenderTick(event: TickEvent.RenderTickEvent) {
        if (event.phase == TickEvent.Phase.START) {
            DeferredSceneRenderer.beginFrame()
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    @JvmStatic
    fun onRenderLevelStage(event: RenderLevelStageEvent) {
        when (event.stage) {
            RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES -> DeferredSceneRenderer.prepareBlackHoles(event)
            RenderLevelStageEvent.Stage.AFTER_LEVEL -> DeferredSceneRenderer.renderPreparedScene()
            else -> Unit
        }
    }
}