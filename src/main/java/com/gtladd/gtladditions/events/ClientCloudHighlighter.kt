package com.gtladd.gtladditions.events

import com.gtladd.gtladditions.client.RenderUtils
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.lwjgl.opengl.GL11C
import org.lwjgl.system.MemoryStack
import java.awt.Color

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
            // 519 leaves the depth test alone; [render] turns it off.
            .setDepthTestState(RenderStateShard.DepthTestStateShard("gtl_no_depth_test", 519))
            .setCullState(RenderStateShard.CullStateShard(false))
            .setLightmapState(RenderStateShard.LightmapStateShard(false))
            .setWriteMaskState(RenderStateShard.WriteMaskStateShard(true, false))
            .setShaderState(RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeLinesShader))
            .createCompositeState(false)
    )

    fun highlight(pos: BlockPos, dimensionId: String) {
        val level = Minecraft.getInstance().level ?: return
        val expireTick = level.gameTime + HIGHLIGHT_TICKS
        entries.removeIf { it.pos == pos && it.dimensionId == dimensionId }
        entries += Entry(pos.immutable(), dimensionId, expireTick)
    }

    fun clear() = entries.clear()

    /** Also drops expired entries. */
    fun hasEntries(level: ClientLevel): Boolean {
        entries.removeIf { it.expireTick < level.gameTime }
        return entries.any { it.dimensionId == level.dimension().location().toString() }
    }

    /** Into the target the scheduler binds, with the frame's [pose]. */
    fun render(pose: PoseStack, level: ClientLevel, camera: Vec3) {
        val minecraft = Minecraft.getInstance()
        val hue = (System.currentTimeMillis() % COLOR_CYCLE_MS) / COLOR_CYCLE_MS.toFloat()
        val color = Color.getHSBColor(hue, 1.0f, 1.0f)
        val bufferSource = minecraft.renderBuffers().bufferSource()
        val consumer = bufferSource.getBuffer(highlightLines)

        for (entry in entries) {
            if (entry.dimensionId != level.dimension().location().toString()) continue
            val box = AABB(entry.pos).move(-camera.x, -camera.y, -camera.z)
            LevelRenderer.renderLineBox(
                pose,
                consumer,
                box,
                color.red / 255.0f,
                color.green / 255.0f,
                color.blue / 255.0f,
                0.95f
            )
        }

        val lineWidth = RenderSystem.getShaderLineWidth()
        val shaderColor = RenderSystem.getShaderColor().copyOf()
        MemoryStack.stackPush().use { stack ->
            val colorMask = stack.malloc(4)
            GL11C.glGetBooleanv(GL11C.GL_COLOR_WRITEMASK, colorMask)
            RenderUtils.withSavedRenderState {
                try {
                    RenderSystem.disableDepthTest()
                    RenderSystem.colorMask(true, true, true, false)
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
                    bufferSource.endBatch(highlightLines)
                } finally {
                    RenderSystem.colorMask(
                        colorMask[0] != 0.toByte(),
                        colorMask[1] != 0.toByte(),
                        colorMask[2] != 0.toByte(),
                        colorMask[3] != 0.toByte()
                    )
                    RenderSystem.setShaderColor(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3])
                    RenderSystem.lineWidth(lineWidth)
                }
            }
        }
    }
}
