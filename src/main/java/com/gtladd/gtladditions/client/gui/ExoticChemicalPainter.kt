package com.gtladd.gtladditions.client.gui

import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gtladd.gtladditions.GTLAdditions
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions
import net.minecraftforge.fluids.FluidStack
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OnlyIn(Dist.CLIENT)
object ExoticChemicalPainter {

    // ========================================
    // TEXTURES & PALETTE
    // ========================================

    private val PAGE = GTLAdditions.id("textures/gui/exotic_chemical/page.png")
    private val BLUEPRINT = GTLAdditions.id("textures/gui/exotic_chemical/blueprint.png")

    const val SWITCH_W = 24

    const val PANEL_A = 0xFF2B2E31.toInt()
    const val PANEL_B = 0xFF222528.toInt()
    const val PANEL_HI = 0xFF44494E.toInt()
    const val PANEL_LO = 0xFF151719.toInt()
    const val PANEL_BORDER = 0xFF121416.toInt()

    const val WELL_DARK = 0xFF0D0F11.toInt()
    const val WELL_LIGHT = 0xFF3D4247.toInt()
    const val WELL_BG = 0xFF17191C.toInt()
    const val SUB_WELL = 0xFF1E2124.toInt()

    const val BTN_A = 0xFF3E4348.toInt()
    const val BTN_B = 0xFF2E3236.toInt()
    const val BTN_HI = 0xFF575E65.toInt()
    const val BTN_LO = 0xFF1A1D20.toInt()
    const val BTN_BORDER = 0xFF131517.toInt()
    const val BTN_ACT_A = 0xFF174B44.toInt()
    const val BTN_ACT_B = 0xFF103530.toInt()
    const val BTN_ACT_HI = 0xFF38D9BD.toInt()
    const val BTN_ACT_LO = 0xFF09211E.toInt()
    const val BTN_DISABLED = 0xFF1F2226.toInt()

    const val ROW_BG = 0xFF202326.toInt()
    const val ROW_BORDER = 0xFF353A3F.toInt()
    const val ROW_HI = 0xFF2C3035.toInt()

    const val TEXT = 0xFFF2F4F6.toInt()
    const val TEXT_DIM = 0xFFA8B0B8.toInt()
    const val TEXT_MUTED = 0xFF707880.toInt()
    const val LCD_TEXT = 0xFFF2F6FA.toInt()

    const val ACCENT = 0xFF34D5B8.toInt()
    const val ACCENT_SOFT = 0xFF143832.toInt()
    const val ACCENT_BORDER = 0xFF2A8D7B.toInt()
    const val ACCENT_TEXT = 0xFF9CF6E4.toInt()
    const val DISTORT_ACCENT = 0xFFE2AD55.toInt()
    const val DISTORT_SOFT = 0xFF382B1B.toInt()
    const val DISTORT_TEXT = 0xFFFFDE9E.toInt()
    const val NEXUS_BASE = 0xFF27726B.toInt()

    private const val KNOB_HI_ON = 0xFFD6FFF6.toInt()
    private const val KNOB_HI_OFF = 0xFFC4CCD4.toInt()
    private const val GLASS_TINT = 0x6B0A1314
    private const val BLUEPRINT_TINT = 0xFF6AD4C2.toInt()

    enum class State(
        val main: Int,
        val bar: Int,
        val fillA: Int,
        val fillB: Int,
        val badge: Int,
        val text: Int,
        val key: String
    ) {
        RUN(
            0xFF4CE07F.toInt(),
            0xFF2FA658.toInt(),
            0xFF1E5E3A.toInt(),
            0xFF113822.toInt(),
            0xFF153022.toInt(),
            0xFFA1F5BE.toInt(),
            "run"
        ),
        WAIT(
            0xFFF6B83B.toInt(),
            0xFFCC8814.toInt(),
            0xFF664514.toInt(),
            0xFF3E290A.toInt(),
            0xFF33250F.toInt(),
            0xFFFFE299.toInt(),
            "wait"
        ),
        PAUSE(
            0xFFB098FA.toInt(),
            0xFF7D64C4.toInt(),
            0xFF44356B.toInt(),
            0xFF292042.toInt(),
            0xFF261F3B.toInt(),
            0xFFDFD4FF.toInt(),
            "pause"
        ),
        IDLE(
            0xFF9AA2AA.toInt(),
            0xFF5E666E.toInt(),
            0xFF363A3F.toInt(),
            0xFF23262A.toInt(),
            0xFF222529.toInt(),
            0xFFCBD1D8.toInt(),
            "idle"
        ),
        LOCKED(
            0xFF596169.toInt(),
            0xFF32373C.toInt(),
            0xFF191C20.toInt(),
            0xFF111316.toInt(),
            0xFF1C2024.toInt(),
            0xFF939AA2.toInt(),
            "locked"
        )
    }

    private val ICONS = mapOf(
        "pause" to arrayOf(".......", ".##.##.", ".##.##.", ".##.##.", ".##.##.", ".##.##.", "......."),
        "play" to arrayOf(".#.....", ".##....", ".###...", ".####..", ".###...", ".##....", ".#....."),
        "clock" to arrayOf("..###..", ".#...#.", "#..#..#", "#..##.#", "#.....#", ".#...#.", "..###.."),
        "back" to arrayOf(".......", "..#....", ".##....", "#######", ".##....", "..#....", "......."),
        "chev" to arrayOf(".......", "..#....", "...#...", "....#..", "...#...", "..#....", "......."),
        "bolt" to arrayOf(
            "....##..",
            "...##...",
            "..##....",
            ".######.",
            "....##..",
            "...##...",
            "..##....",
            ".##....."
        )
    )

    // ========================================
    // COLOR & TEXT UTILITIES
    // ========================================

    val font: Font
        get() = Minecraft.getInstance().font

    fun alpha(color: Int, a: Float): Int = (color and 0xFFFFFF) or ((a * 255).roundToInt().coerceIn(0, 255) shl 24)

    // HSL lightness scale, so tinted shading keeps the hue.
    fun lightness(color: Int, factor: Float): Int {
        val r = (color shr 16 and 255) / 255f
        val g = (color shr 8 and 255) / 255f
        val b = (color and 255) / 255f
        val hi = max(r, max(g, b))
        val lo = min(r, min(g, b))

        var h = 0f
        var s = 0f
        val l = (hi + lo) / 2f
        if (hi != lo) {
            val d = hi - lo
            s = if (l > 0.5f) d / (2f - hi - lo) else d / (hi + lo)
            h = when (hi) {
                r -> (g - b) / d + if (g < b) 6f else 0f
                g -> (b - r) / d + 2f
                else -> (r - g) / d + 4f
            } / 6f
        }

        val nl = (l * factor).coerceIn(0f, 1f)
        fun hue(p: Float, q: Float, t0: Float): Float {
            val t = when {
                t0 < 0f -> t0 + 1f
                t0 > 1f -> t0 - 1f
                else -> t0
            }
            return when {
                t < 1f / 6f -> p + (q - p) * 6f * t
                t < 0.5f -> q
                t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
                else -> p
            }
        }

        val (rr, gg, bb) = if (s == 0f) {
            Triple(nl, nl, nl)
        } else {
            val q = if (nl < 0.5f) nl * (1f + s) else nl + s - nl * s
            val p = 2f * nl - q
            Triple(hue(p, q, h + 1f / 3f), hue(p, q, h), hue(p, q, h - 1f / 3f))
        }

        return (color and -0x1000000) or
            ((rr * 255).roundToInt() shl 16) or
            ((gg * 255).roundToInt() shl 8) or
            (bb * 255).roundToInt()
    }

    fun mix(a: Int, b: Int, t: Float): Int {
        fun ch(shift: Int): Int =
            ((a shr shift and 255) * (1f - t) + (b shr shift and 255) * t).roundToInt() shl shift

        return ch(24) or ch(16) or ch(8) or ch(0)
    }

    // CJK glyphs fill 8 rows below y in both Unifont and Modern UI's font; centring 7 rows left labels 1 px low.
    fun ty(y: Int, h: Int): Int = y + ((h - 8) / 2.0).roundToInt()

    fun text(g: GuiGraphics, text: Component, x: Int, y: Int, color: Int): Int {
        g.drawString(font, text, x, y, color, false)
        return font.width(text)
    }

    fun text(g: GuiGraphics, text: String, x: Int, y: Int, color: Int): Int {
        g.drawString(font, text, x, y, color, false)
        return font.width(text)
    }

    fun textCenter(g: GuiGraphics, text: Component, cx: Int, y: Int, color: Int): Int =
        text(g, text, cx - font.width(text) / 2, y, color)

    // Squeezes captions such as "+512" into a 22 px key.
    fun textFit(g: GuiGraphics, text: String, x: Int, y: Int, w: Int, h: Int, color: Int) {
        val tw = font.width(text)
        if (tw <= w - 4) {
            text(g, text, x + (w - tw + 1) / 2, ty(y, h), color)
            return
        }

        val k = (w - 4f) / tw
        g.pose().pushPose()
        g.pose().translate(x + 2f, y + h / 2f - 4f * k, 0f)
        g.pose().scale(k, k, 1f)
        g.drawString(font, text, 0, 0, color, false)
        g.pose().popPose()
    }

    fun icon(g: GuiGraphics, name: String, x: Int, y: Int, color: Int) {
        val rows = ICONS.getValue(name)
        for ((dy, row) in rows.withIndex()) {
            for ((dx, c) in row.withIndex()) {
                if (c == '#') rect(g, x + dx, y + dy, 1, 1, color)
            }
        }
    }

    // ========================================
    // PRIMITIVES & SURFACES
    // ========================================

    fun rect(g: GuiGraphics, x: Int, y: Int, w: Int, h: Int, color: Int) {
        if (w > 0 && h > 0) g.fill(x, y, x + w, y + h, color)
    }

    fun frame(g: GuiGraphics, x: Int, y: Int, w: Int, h: Int, color: Int) {
        rect(g, x, y, w, 1, color)
        rect(g, x, y + h - 1, w, 1, color)
        rect(g, x, y + 1, 1, h - 2, color)
        rect(g, x + w - 1, y + 1, 1, h - 2, color)
    }

    fun groove(g: GuiGraphics, x: Int, y: Int, w: Int) {
        rect(g, x, y, w, 1, PANEL_LO)
        rect(g, x, y + 1, w, 1, alpha(PANEL_HI, 0.35f))
    }

    fun tag(g: GuiGraphics, x: Int, y: Int, w: Int, h: Int, bg: Int, border: Int) {
        rect(g, x, y, w, h, bg)
        frame(g, x, y, w, h, border)
    }

    // Jade-like cylinder: dark -> bright at 45 % -> darker, with a faint top catchlight.
    fun cylinder(g: GuiGraphics, x: Int, y: Int, w: Int, h: Int, base: Int) {
        if (w <= 0 || h <= 0) return

        val split = (h * 0.45f).roundToInt().coerceIn(1, h)
        g.fillGradient(x, y, x + w, y + split, lightness(base, 0.78f), lightness(base, 1.08f))
        g.fillGradient(x, y + split, x + w, y + h, lightness(base, 1.08f), lightness(base, 0.66f))
        rect(g, x, y, w, 1, 0x24FFFFFF)
    }

    fun recessed(g: GuiGraphics, x: Int, y: Int, w: Int, h: Int, fill: Int) {
        rect(g, x, y, w, h, fill)
        rect(g, x, y, w, 1, WELL_DARK)
        rect(g, x, y, 1, h, WELL_DARK)
        rect(g, x + 1, y + h - 1, w - 1, 1, WELL_LIGHT)
        rect(g, x + w - 1, y + 1, 1, h - 1, WELL_LIGHT)
        rect(g, x + 1, y + 1, w - 2, 1, 0x47000000)
    }

    // clip = 1 cuts the four corner pixels like GT's own frames.
    fun well(g: GuiGraphics, x: Int, y: Int, w: Int, h: Int, fill: Int, clip: Int = 0) {
        if (clip > 0) {
            rect(g, x + 1, y, w - 2, h, fill)
            rect(g, x, y + 1, 1, h - 2, fill)
            rect(g, x + w - 1, y + 1, 1, h - 2, fill)
        } else {
            rect(g, x, y, w, h, fill)
        }

        rect(g, x + clip, y, w - 2 * clip, 1, WELL_DARK)
        rect(g, x, y + clip, 1, h - 2 * clip, WELL_DARK)
        rect(g, x + 1, y + 1, w - 2, 1, 0x38000000)
        rect(g, x + 1, y + h - 1, w - 1 - clip, 1, WELL_LIGHT)
        rect(g, x + w - 1, y + 1, 1, h - 1 - clip, alpha(WELL_LIGHT, 0.78f))
    }

    fun plate(
        g: GuiGraphics,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        colA: Int,
        colB: Int,
        border: Int,
        hi: Int,
        lo: Int,
        clip: Int = 1
    ) {
        g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, colA, colB)

        rect(g, x + clip, y, w - 2 * clip, 1, border)
        rect(g, x + clip, y + h - 1, w - 2 * clip, 1, border)
        rect(g, x, y + clip, 1, h - 2 * clip, border)
        rect(g, x + w - 1, y + clip, 1, h - 2 * clip, border)

        rect(g, x + 1, y + 1, w - 2, 1, hi)
        rect(g, x + 1, y + 2, 1, h - 3, alpha(hi, 0.45f))
        rect(g, x + 1, y + h - 2, w - 2, 1, lo)
        rect(g, x + w - 2, y + 2, 1, h - 4, alpha(lo, 0.72f))
    }

    // ========================================
    // WIDGET CONTROLS
    // ========================================

    fun button(
        g: GuiGraphics,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        label: Component?,
        icon: String? = null,
        active: Boolean = false,
        disabled: Boolean = false,
        hovered: Boolean = false
    ) {
        when {
            disabled -> well(g, x, y, w, h, BTN_DISABLED, 1)
            active -> plate(g, x, y, w, h, BTN_ACT_A, BTN_ACT_B, BTN_BORDER, BTN_ACT_HI, BTN_ACT_LO)
            hovered -> plate(
                g,
                x,
                y,
                w,
                h,
                lightness(BTN_A, 1.12f),
                lightness(BTN_B, 1.12f),
                BTN_BORDER,
                BTN_HI,
                BTN_LO
            )

            else -> plate(g, x, y, w, h, BTN_A, BTN_B, BTN_BORDER, BTN_HI, BTN_LO)
        }

        val color = when {
            disabled -> TEXT_MUTED
            active -> ACCENT_TEXT
            else -> TEXT
        }
        val lw = label?.let { font.width(it) } ?: 0
        val withIcon = icon != null && lw + 9 <= w - 6
        var cx = x + (w - lw - (if (withIcon) 9 else 0) + 1) / 2

        if (withIcon) {
            icon(g, icon, cx, y + (h - 7) / 2, color)
            cx += 9
        }
        if (label != null) text(g, label, cx, ty(y, h), color)
    }

    fun flushLink(g: GuiGraphics, x: Int, y: Int, w: Int, h: Int, hovered: Boolean) {
        rect(g, x, y, w, h, if (hovered) ROW_HI else ROW_BG)
        rect(g, x + 1, y + 1, w - 2, 1, ROW_HI)
        frame(g, x, y, w, h, ROW_BORDER)
        icon(g, "chev", x + w - 10, y + (h - 7) / 2, TEXT_DIM)
    }

    fun switch(g: GuiGraphics, x: Int, y: Int, on: Boolean, disabled: Boolean) {
        val h = 11
        well(g, x, y, SWITCH_W, h, if (disabled) BTN_DISABLED else WELL_BG, 1)
        if (!disabled && on) {
            cylinder(g, x + 1, y + 1, SWITCH_W - 11, h - 2, ACCENT)
        }

        val kx = if (on) x + SWITCH_W - 10 else x + 1
        if (disabled) {
            rect(g, kx, y + 1, 9, h - 2, PANEL_B)
            frame(g, kx, y + 1, 9, h - 2, PANEL_BORDER)
        } else {
            val knobHi = if (on) KNOB_HI_ON else KNOB_HI_OFF
            plate(
                g,
                kx,
                y + 1,
                9,
                h - 2,
                BTN_HI,
                BTN_B,
                PANEL_BORDER,
                knobHi,
                BTN_LO,
                0
            )
            rect(g, kx + 4, y + 3, 1, h - 6, 0x52000000)
        }
    }

    fun progress(
        g: GuiGraphics,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        k: Int,
        d: Int,
        state: State,
        label: Component?
    ) {
        recessed(g, x, y, w, h, 0xFF10151C.toInt())

        if (d > 0) {
            val fw = ((w - 2) * k.coerceIn(0, d).toFloat() / d).roundToInt()
            cylinder(g, x + 1, y + 1, fw, h - 2, state.bar)
        }
        if (label != null) text(g, label, x + 4, ty(y, h), 0xFFFFFFFF.toInt())
    }

    // ========================================
    // BLUEPRINT & GEOMETRY
    // ========================================

    fun page(g: GuiGraphics, x: Int, y: Int) = g.blit(PAGE, x, y, 314, 218, 0f, 0f, 628, 436, 628, 436)

    // Replaces the baked header bar and two-column interior with brushed metal margins around a single recessed screen.
    fun unformedPage(g: GuiGraphics, x: Int, y: Int) {
        page(g, x, y)
        g.blit(PAGE, x + 9, y + 4, 296, 5, 18f, 408f, 592, 10, 628, 436)
        g.blit(PAGE, x + 4, y + 4, 5, 23, 8f, 362f, 10, 46, 628, 436)
        g.blit(PAGE, x + 305, y + 4, 5, 23, 610f, 362f, 10, 46, 628, 436)
        frame(g, x + 9, y + 9, 296, 200, 0xFF525252.toInt())
        recessed(g, x + 10, y + 10, 294, 198, WELL_BG)
        glass(g, x + 10, y + 10, 294, 198)
    }

    // The bezel is part of the page texture; this draws the glass, grid and silhouette inside it.
    fun blueprint(g: GuiGraphics, x: Int, y: Int, size: Int) = glass(g, x, y, size, size, silhouette = true)

    private fun glass(g: GuiGraphics, x: Int, y: Int, w: Int, h: Int, silhouette: Boolean = false) {
        GuiTextures.DISPLAY.draw(g, 0, 0, x + 1f, y + 1f, w - 2, h - 2)
        rect(g, x + 1, y + 1, w - 2, h - 2, GLASS_TINT)
        frame(g, x + 1, y + 1, w - 2, h - 2, 0x73000000)

        val grid = alpha(BLUEPRINT_TINT, 0.05f)
        var gx = 16
        while (gx < w - 2) {
            rect(g, x + gx, y + 2, 1, h - 4, grid)
            gx += 16
        }
        var gy = 16
        while (gy < h - 2) {
            rect(g, x + 2, y + gy, w - 4, 1, grid)
            gy += 16
        }

        if (silhouette) {
            // 304 px mask for 144 GUI px: sample it smoothly instead of nearest texels.
            Minecraft.getInstance().textureManager.getTexture(BLUEPRINT).setFilter(true, false)
            RenderSystem.enableBlend()
            RenderSystem.setShaderColor(0x6A / 255f, 0xD4 / 255f, 0xC2 / 255f, 0.82f)
            g.blit(BLUEPRINT, x + 2, y + 2, w - 4, h - 4, 0f, 0f, 304, 304, 304, 304)
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        }

        val tick = alpha(PANEL_HI, 0.7f)
        val far = x + w - 3
        val low = y + h - 3
        rect(g, x + 2, y + 2, 3, 1, tick)
        rect(g, x + 2, y + 3, 1, 2, tick)
        rect(g, far - 2, y + 2, 3, 1, tick)
        rect(g, far, y + 3, 1, 2, tick)
        rect(g, x + 2, low, 3, 1, tick)
        rect(g, x + 2, low - 2, 1, 2, tick)
        rect(g, far - 2, low, 3, 1, tick)
        rect(g, far, low - 2, 1, 2, tick)
    }

    fun octagon(
        g: GuiGraphics,
        x: Int,
        y: Int,
        s: Int,
        c: Int,
        top: Int,
        bottom: Int,
        stroke: Int,
        recessed: Boolean
    ) {
        fun inset(i: Int, n: Int, cut: Int): Int = max(0, max(cut - i, cut - (n - 1 - i)))

        for (i in 0 until s) {
            val a = inset(i, s, c)
            rect(g, x + a, y + i, s - 2 * a, 1, stroke)
        }
        for (i in 0 until s - 2) {
            val a = inset(i, s - 2, c - 1)
            val t = if (s > 3) i / (s - 3f) else 0f
            rect(g, x + 1 + a, y + 1 + i, s - 2 - 2 * a, 1, mix(top, bottom, t))
        }
        if (!recessed) rect(g, x + c, y + 1, s - 2 * c, 1, 0x38FFFFFF)
    }

    fun reticle(g: GuiGraphics, x: Int, y: Int, w: Int, h: Int, color: Int) {
        rect(g, x, y, 3, 1, color)
        rect(g, x, y, 1, 3, color)
        rect(g, x + w - 3, y, 3, 1, color)
        rect(g, x + w - 1, y, 1, 3, color)
        rect(g, x, y + h - 1, 3, 1, color)
        rect(g, x, y + h - 3, 1, 3, color)
        rect(g, x + w - 3, y + h - 1, 3, 1, color)
        rect(g, x + w - 1, y + h - 3, 1, 3, color)
    }

    // 2 px on, 2 px off, from (x0, y0) towards (x1, y1).
    fun dashed(g: GuiGraphics, x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
        val steps = max(abs(x1 - x0), abs(y1 - y0))
        for (i in 0..steps) {
            if (i % 4 >= 2) continue
            val t = if (steps == 0) 0f else i.toFloat() / steps
            rect(g, (x0 + (x1 - x0) * t).roundToInt(), (y0 + (y1 - y0) * t).roundToInt(), 1, 1, color)
        }
    }

    // ========================================
    // STACK RENDERING
    // ========================================

    fun item(g: GuiGraphics, stack: ItemStack, x: Int, y: Int) {
        g.pose().pushPose()
        g.pose().translate(x.toFloat(), y.toFloat(), 0f)
        g.pose().scale(0.5f, 0.5f, 1f)
        g.renderItem(stack, 0, 0)
        g.pose().popPose()
    }

    fun fluid(g: GuiGraphics, stack: FluidStack, x: Int, y: Int) {
        val ext = IClientFluidTypeExtensions.of(stack.fluid)
        val sprite =
            Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(stack))
        val tint = ext.getTintColor(stack)

        RenderSystem.setShaderColor(
            (tint shr 16 and 255) / 255f,
            (tint shr 8 and 255) / 255f,
            (tint and 255) / 255f,
            1f
        )
        g.blit(x, y, 0, 8, 8, sprite)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }
}