package com.gtladd.gtladditions.events

import com.gtladd.gtladditions.GTLAdditions
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.awt.Color

@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, value = [Dist.CLIENT])
object ClientCloudHighlighter {

    private data class Entry(val pos: BlockPos, val dimensionId: String, val expireTick: Long)

    private const val HIGHLIGHT_TICKS = 300L
    private const val COLOR_CYCLE_MS = 4000L
    private const val LINE_WIDTH = 5.0f

    private val entries = mutableListOf<Entry>()

    @Suppress("INFERRED_INVISIBLE_RETURN_TYPE_WARNING")
    private val highlightLines = RenderType.create(
        "gtladditions_highlight_lines",
        DefaultVertexFormat.POSITION_COLOR_NORMAL,
        VertexFormat.Mode.LINES,
        65536,
        false,
        false,
        RenderType.CompositeState.builder()
            .setTransparencyState(
                RenderStateShard.TransparencyStateShard(
                    "gtl_glint_transparency",
                    {
                        RenderSystem.enableBlend()
                        RenderSystem.defaultBlendFunc()
                        RenderSystem.lineWidth(LINE_WIDTH)
                    },
                    {
                        RenderSystem.disableBlend()
                        RenderSystem.lineWidth(1.0f)
                    }
                )
            )
            .setTextureState(RenderStateShard.EmptyTextureStateShard({}, {}))
            .setDepthTestState(RenderStateShard.DepthTestStateShard("gtl_no_depth_test", 519))
            .setCullState(RenderStateShard.CullStateShard(false))
            .setLightmapState(RenderStateShard.LightmapStateShard(false))
            .setWriteMaskState(RenderStateShard.WriteMaskStateShard(true, true))
            .setShaderState(RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeLinesShader))
            .createCompositeState(false)
    )

    @JvmStatic
    fun highlight(pos: BlockPos, dimensionId: String) {
        val level = Minecraft.getInstance().level ?: return
        val expireTick = level.gameTime + HIGHLIGHT_TICKS
        entries.removeIf { it.pos == pos && it.dimensionId == dimensionId }
        entries += Entry(pos.immutable(), dimensionId, expireTick)
    }

    @Suppress("unused")
    @JvmStatic
    @SubscribeEvent
    fun onLoggingOut(event: ClientPlayerNetworkEvent.LoggingOut) {
        entries.clear()
    }

    @JvmStatic
    @SubscribeEvent
    fun onRenderLevel(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return
        val minecraft = Minecraft.getInstance()
        val level = minecraft.level ?: return

        entries.removeIf { it.expireTick < level.gameTime }
        if (entries.isEmpty()) return

        val hue = (System.currentTimeMillis() % COLOR_CYCLE_MS) / COLOR_CYCLE_MS.toFloat()
        val color = Color.getHSBColor(hue, 1.0f, 1.0f)
        val bufferSource = minecraft.renderBuffers().bufferSource()
        val consumer = bufferSource.getBuffer(highlightLines)
        val cameraPosition = event.camera.position
        val poseStack = event.poseStack

        poseStack.pushPose()
        for (entry in entries) {
            if (entry.dimensionId != level.dimension().location().toString()) continue
            val box = AABB(entry.pos).move(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z)
            LevelRenderer.renderLineBox(
                poseStack,
                consumer,
                box,
                color.red / 255.0f,
                color.green / 255.0f,
                color.blue / 255.0f,
                0.95f
            )
        }
        poseStack.popPose()
        RenderSystem.lineWidth(LINE_WIDTH)
        bufferSource.endBatch(highlightLines)
        RenderSystem.lineWidth(1.0f)
    }
}