package com.gtladd.gtladditions.client.gui

import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Matrix4f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@OnlyIn(Dist.CLIENT)
object WirelessMonitorPainter {

    // ========================================
    // PALETTE (Fluent dark)
    // ========================================

    const val PAGE = 0xFF202020.toInt()
    const val PAGE_STROKE = 0xFF3A3A3A.toInt()
    const val CARD = 0xFF2B2B2B.toInt()
    const val CARD_STROKE = 0xFF383838.toInt()
    const val SUNKEN = 0xFF1C1C1C.toInt()
    const val SUNKEN_STROKE = 0xFF333333.toInt()
    const val CHART = 0xFF232323.toInt()
    const val SELECTED = 0xFF3A3A3A.toInt()
    const val HOVER = 0xFF2F2F2F.toInt()
    const val ROW_HOVER = 0xFF333333.toInt()

    const val TEXT = 0xFFFFFFFF.toInt()
    const val LABEL = 0xFF9E9E9E.toInt()
    const val MUTED = 0xFF8B8B8B.toInt()
    const val SEGMENT_TEXT = 0xFFA0A0A0.toInt()

    const val ACCENT = 0xFF60CDFF.toInt()
    const val GEN = 0xFF6CCB5F.toInt()
    const val CON = 0xFFFF99A4.toInt()
    const val CAUTION = 0xFFFFC83D.toInt()

    val font: Font
        get() = Minecraft.getInstance().font

    fun alpha(color: Int, a: Float): Int = (color and 0xFFFFFF) or ((a * 255).roundToInt().coerceIn(0, 255) shl 24)

    // ========================================
    // SHAPES
    // ========================================

    // Plain pixel-aligned rectangles need no anti-aliasing.
    fun rect(g: GuiGraphics, x: Int, y: Int, w: Int, h: Int, color: Int) {
        if (w > 0 && h > 0) g.fill(x, y, x + w, y + h, color)
    }

    fun round(g: GuiGraphics, x: Int, y: Int, w: Int, h: Int, r: Int, color: Int) =
        round(g, x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), r.toFloat(), color)

    // Anti-aliased rounded rect; r is clamped to half the shorter side, so it turns into a pill or a disc.
    fun round(g: GuiGraphics, x: Float, y: Float, w: Float, h: Float, r: Float, color: Int) {
        if (w <= 0f || h <= 0f) return
        shape(g.bufferSource().getBuffer(RenderType.gui()), g.pose().last().pose(), x, y, w, h, r, color)
        g.flush()
    }

    fun capsule(g: GuiGraphics, x: Float, y: Float, w: Float, h: Float, color: Int) = round(g, x, y, w, h, min(w, h) / 2, color)

    fun disc(b: VertexConsumer, m: Matrix4f, cx: Float, cy: Float, r: Float, color: Int) = shape(b, m, cx - r, cy - r, 2 * r, 2 * r, r, color)

    // A hairline stroke round a fill: both end on one shared outline, so even translucent colours neither overlap nor leave a gap.
    fun card(g: GuiGraphics, x: Int, y: Int, w: Int, h: Int, r: Int, fill: Int, stroke: Int) {
        val b = g.bufferSource().getBuffer(RenderType.gui())
        val m = g.pose().last().pose()
        val fx = x.toFloat()
        val fy = y.toFloat()
        val fw = w.toFloat()
        val fh = h.toFloat()
        val rr = min(r.toFloat(), min(fw, fh) / 2)
        val inner = max(rr - hairline(), 0f)
        val half = pixel() / 2
        fan(b, m, fx, fy, fw, fh, rr, inner, fill)
        band(b, m, fx, fy, fw, fh, rr, inner, rr - half, stroke, stroke)
        band(b, m, fx, fy, fw, fh, rr, rr - half, rr + half, stroke, stroke and 0xFFFFFF)
        g.flush()
    }

    // Magnifier in a 7 × 7 box: an anti-aliased ring and a round-ended handle at 45°.
    fun searchIcon(g: GuiGraphics, x: Int, y: Int, color: Int) {
        val b = g.bufferSource().getBuffer(RenderType.gui())
        val m = g.pose().last().pose()
        val cx = x + 2.75f
        val cy = y + 2.75f
        val half = pixel() / 2
        val clear = color and 0xFFFFFF
        // A disc-sized box puts all four corner centres on the ring's centre; the ring runs from radius 1.75 to 2.75.
        band(b, m, cx - 2.75f, cy - 2.75f, 5.5f, 5.5f, 2.75f, 1.75f - half, 1.75f + half, clear, color)
        band(b, m, cx - 2.75f, cy - 2.75f, 5.5f, 5.5f, 2.75f, 1.75f + half, 2.75f - half, color, color)
        band(b, m, cx - 2.75f, cy - 2.75f, 5.5f, 5.5f, 2.75f, 2.75f - half, 2.75f + half, color, clear)

        g.pose().pushPose()
        g.pose().translate(cx, cy, 0f)
        g.pose().mulPose(Axis.ZP.rotationDegrees(45f))
        shape(b, g.pose().last().pose(), 2.4f, -0.55f, 3.5f, 1.1f, 0.55f, color)
        g.pose().popPose()
        g.flush()
    }

    // ========================================
    // SUB-PIXEL GEOMETRY
    // ========================================

    // One physical pixel in GUI units, the width of anti-aliasing feathers.
    fun pixel(): Float = 1f / Minecraft.getInstance().window.guiScale.toFloat()

    // Fine lines and borders: half a GUI pixel, rounded to whole physical pixels so straight lines stay crisp.
    fun hairline(): Float {
        val scale = Minecraft.getInstance().window.guiScale.toFloat()
        return max(1, (scale / 2).roundToInt()) / scale
    }

    // Quads run down their left side first, like GuiGraphics.fill: the GUI render type culls the other winding.
    fun vertex(b: VertexConsumer, m: Matrix4f, x: Float, y: Float, color: Int) {
        b.vertex(m, x, y, 0f).color(color shr 16 and 0xFF, color shr 8 and 0xFF, color and 0xFF, color ushr 24).endVertex()
    }

    fun rect(b: VertexConsumer, m: Matrix4f, x: Float, y: Float, w: Float, h: Float, color: Int) {
        vertex(b, m, x, y, color)
        vertex(b, m, x, y + h, color)
        vertex(b, m, x + w, y + h, color)
        vertex(b, m, x + w, y, color)
    }

    fun rect(g: GuiGraphics, x: Float, y: Float, w: Float, h: Float, color: Int) {
        rect(g.bufferSource().getBuffer(RenderType.gui()), g.pose().last().pose(), x, y, w, h, color)
        g.flush()
    }

    // Solid to half a pixel inside the outline, then a feather to half a pixel outside: whole-pixel straight edges stay crisp, arcs come out smooth.
    private fun shape(b: VertexConsumer, m: Matrix4f, x: Float, y: Float, w: Float, h: Float, r: Float, color: Int) {
        val rr = min(r, min(w, h) / 2)
        val half = pixel() / 2
        val inner = max(rr - half, 0f)
        fan(b, m, x, y, w, h, rr, inner, color)
        band(b, m, x, y, w, h, rr, inner, rr + half, color, color and 0xFFFFFF)
    }

    // Fills the outline `radius` out from the corner centres, as a fan from the middle of the rect.
    private fun fan(b: VertexConsumer, m: Matrix4f, x: Float, y: Float, w: Float, h: Float, r: Float, radius: Float, color: Int) {
        var lastX = 0f
        var lastY = 0f
        var started = false
        outline(x, y, w, h, r) { cx, cy, c, s ->
            val px = cx + c * radius
            val py = cy + s * radius
            if (started) {
                vertex(b, m, x + w / 2, y + h / 2, color)
                vertex(b, m, lastX, lastY, color)
                vertex(b, m, px, py, color)
                vertex(b, m, px, py, color)
            }
            lastX = px
            lastY = py
            started = true
        }
    }

    // Shades the ring between the outlines `inner` and `outer` out from the corner centres, from one colour to the other.
    private fun band(
        b: VertexConsumer,
        m: Matrix4f,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        r: Float,
        inner: Float,
        outer: Float,
        innerColor: Int,
        outerColor: Int
    ) {
        var lastIx = 0f
        var lastIy = 0f
        var lastOx = 0f
        var lastOy = 0f
        var started = false
        outline(x, y, w, h, r) { cx, cy, c, s ->
            val ix = cx + c * inner
            val iy = cy + s * inner
            val ox = cx + c * outer
            val oy = cy + s * outer
            if (started) {
                vertex(b, m, lastIx, lastIy, innerColor)
                vertex(b, m, lastOx, lastOy, outerColor)
                vertex(b, m, ox, oy, outerColor)
                vertex(b, m, ix, iy, innerColor)
            }
            lastIx = ix
            lastIy = iy
            lastOx = ox
            lastOy = oy
            started = true
        }
    }

    /**
     * Walks a rounded rect's outline once round and back to its start, giving each point's corner centre and direction.
     *
     * - Corners go bottom-right, top-right, top-left, bottom-left, CORNER_SEGMENTS arcs each; straight sides join them.
     * - Angles decrease, which runs counter-clockwise on the y-down screen: the winding the GUI render type keeps.
     */
    private inline fun outline(x: Float, y: Float, w: Float, h: Float, r: Float, point: (cx: Float, cy: Float, c: Float, s: Float) -> Unit) {
        val points = 4 * (CORNER_SEGMENTS + 1)
        for (k in 0..points) {
            val j = k % points
            val corner = j / (CORNER_SEGMENTS + 1)
            val a = PI / 2 * (1 - corner) - PI / 2 * (j % (CORNER_SEGMENTS + 1)) / CORNER_SEGMENTS
            point(
                if (corner < 2) x + w - r else x + r,
                if (corner == 0 || corner == 3) y + h - r else y + r,
                cos(a).toFloat(),
                sin(a).toFloat()
            )
        }
    }

    // ========================================
    // TEXT
    // ========================================

    // CJK glyphs fill 8 rows below y in both Unifont and Modern UI's font.
    fun ty(y: Int, h: Int): Int = y + ((h - 8) / 2.0).roundToInt()

    fun text(g: GuiGraphics, text: Component, x: Int, y: Int, color: Int): Int {
        g.drawString(font, text, x, y, color, false)
        return font.width(text)
    }

    fun text(g: GuiGraphics, text: String, x: Int, y: Int, color: Int): Int {
        g.drawString(font, text, x, y, color, false)
        return font.width(text)
    }

    // Draws at normal size, or squeezed to fit `width` when longer.
    fun fit(g: GuiGraphics, text: Component, x: Int, y: Int, width: Int, color: Int) {
        val tw = font.width(text)
        if (tw <= width) {
            text(g, text, x, y, color)
            return
        }

        val k = width.toFloat() / tw
        g.pose().pushPose()
        g.pose().translate(x.toFloat(), y + 4f * (1f - k), 0f)
        g.pose().scale(k, k, 1f)
        g.drawString(font, text, 0, 0, color, false)
        g.pose().popPose()
    }

    fun fitRight(g: GuiGraphics, text: Component, right: Int, y: Int, width: Int, color: Int) =
        fit(g, text, right - min(font.width(text), width), y, width, color)

    fun cut(text: String, width: Int): String {
        if (font.width(text) <= width) return text
        return font.plainSubstrByWidth(text, maxOf(0, width - font.width(ELLIPSIS))) + ELLIPSIS
    }

    // ========================================
    // CONTROLS
    // ========================================

    // Fluent segmented control: sunken track, raised selected pill with a short accent underline.
    fun segments(g: GuiGraphics, x: Int, y: Int, w: Int, h: Int, labels: List<Component>, selected: Int, hovered: Int) {
        card(g, x, y, w, h, 4, SUNKEN, SUNKEN_STROKE)
        for ((i, label) in labels.withIndex()) {
            val sx = x + w * i / labels.size
            val sw = x + w * (i + 1) / labels.size - sx
            if (i == selected) {
                round(g, sx + 1, y + 1, sw - 2, h - 2, 3, SELECTED)
                capsule(g, sx + sw / 2f - 5f, y + h - 2.75f, 10f, 1.5f, ACCENT)
            } else if (i == hovered) {
                round(g, sx + 1, y + 1, sw - 2, h - 2, 3, HOVER)
            }

            val color = if (i == selected) TEXT else SEGMENT_TEXT
            val tw = min(font.width(label), sw - 4)
            fit(g, label, sx + (sw - tw + 1) / 2, ty(y, h) - if (i == selected) 1 else 0, sw - 4, color)
        }
    }

    private const val ELLIPSIS = "…"

    // Arc segments per corner: 15° steps stay smooth at 4K with a GUI scale of 4.
    private const val CORNER_SEGMENTS = 6
}
