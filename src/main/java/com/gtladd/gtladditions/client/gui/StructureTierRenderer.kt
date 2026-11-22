package com.gtladd.gtladditions.client.gui

import com.glodblock.github.glodium.client.render.ColorData
import com.glodblock.github.glodium.client.render.highlight.HighlightHandler
import com.gtladd.gtladditions.GTLAdditions
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.AABB
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
internal object StructureTierRenderer {

    private const val HIGHLIGHT_MILLIS = 15_000L

    private var marked: Set<BlockPos> = emptySet()

    private val BUTTON_NORMAL = GTLAdditions.id("textures/gui/structure_tier/button_normal.png")
    private val BUTTON_HOVER = GTLAdditions.id("textures/gui/structure_tier/button_hover.png")
    private val BUTTON_PRESSED = GTLAdditions.id("textures/gui/structure_tier/button_pressed.png")
    private val BUTTON_DISABLED = GTLAdditions.id("textures/gui/structure_tier/button_disabled.png")
    private const val TEXT = 0xFF202020.toInt()
    private const val MUTED_TEXT = 0xFF6F6F6F.toInt()

    // The 18 x 14 button textures, nine-sliced with 3-pixel edges.
    private const val FRAME_WIDTH = 18
    private const val FRAME_HEIGHT = 14
    private const val EDGE = 3

    fun clear() {
        marked = emptySet()
    }

    fun highlight(buf: FriendlyByteBuf) {
        val dimension = Minecraft.getInstance().level?.dimension() ?: return
        val until = System.currentTimeMillis() + HIGHLIGHT_MILLIS

        val cells = List(buf.readVarInt()) { BlockPos.of(buf.readLong()) }
        val chunks = List(buf.readVarInt()) {
            AABB(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble()
            )
        }

        val corners = chunks.map { BlockPos.containing(it.minX, it.minY, it.minZ) }
        // A fixed block must not stay marked: only the latest press's marks are drawn.
        marked = (cells + corners).toSet()

        val problem = ColorData(1f, 0f, 0f)
        val unloaded = ColorData(1f, 0.8f, 0f)

        for (pos in cells) {
            HighlightHandler.highlight(pos, null, dimension, until, AABB(pos), problem) {
                pos in marked && System.currentTimeMillis() / 500 % 2 == 0L
            }
        }

        for ((pos, box) in corners.zip(chunks)) {
            HighlightHandler.highlight(pos, null, dimension, until, box, unloaded) { pos in marked }
        }
    }

    fun drawButton(
        graphics: GuiGraphics,
        label: String,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        enabled: Boolean,
        pending: Boolean,
        hovered: Boolean
    ) {
        val frame = when {
            pending -> BUTTON_PRESSED
            !enabled -> BUTTON_DISABLED
            hovered -> BUTTON_HOVER
            else -> BUTTON_NORMAL
        }

        drawFrame(graphics, frame, x, y, width, height)
        val font = Minecraft.getInstance().font
        val name = I18n.get(label)
        val textY = y + (height - 7) / 2 + if (pending) 1 else 0
        graphics.drawString(
            font,
            name,
            x + (width - font.width(name) + 1) / 2,
            textY,
            if (enabled || pending) TEXT else MUTED_TEXT,
            false
        )
    }

    private fun drawFrame(graphics: GuiGraphics, frame: ResourceLocation, x: Int, y: Int, width: Int, height: Int) {
        val right = width - EDGE
        val bottom = height - EDGE
        val middleU = FRAME_WIDTH - 2 * EDGE
        val middleV = FRAME_HEIGHT - 2 * EDGE
        fun part(dx: Int, dy: Int, w: Int, h: Int, u: Int, v: Int, uw: Int, vh: Int) =
            graphics.blit(frame, x + dx, y + dy, w, h, u.toFloat(), v.toFloat(), uw, vh, FRAME_WIDTH, FRAME_HEIGHT)
        part(0, 0, EDGE, EDGE, 0, 0, EDGE, EDGE)
        part(right, 0, EDGE, EDGE, FRAME_WIDTH - EDGE, 0, EDGE, EDGE)
        part(0, bottom, EDGE, EDGE, 0, FRAME_HEIGHT - EDGE, EDGE, EDGE)
        part(right, bottom, EDGE, EDGE, FRAME_WIDTH - EDGE, FRAME_HEIGHT - EDGE, EDGE, EDGE)
        part(EDGE, 0, right - EDGE, EDGE, EDGE, 0, middleU, EDGE)
        part(EDGE, bottom, right - EDGE, EDGE, EDGE, FRAME_HEIGHT - EDGE, middleU, EDGE)
        part(0, EDGE, EDGE, bottom - EDGE, 0, EDGE, EDGE, middleV)
        part(right, EDGE, EDGE, bottom - EDGE, FRAME_WIDTH - EDGE, EDGE, EDGE, middleV)
        part(EDGE, EDGE, right - EDGE, bottom - EDGE, EDGE, EDGE, middleU, middleV)
    }
}