package com.gtladd.gtladditions.integration.xei.structureupgrade

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.machine.feature.TierCost
import com.gtladd.gtladditions.client.gui.ExoticChemicalPainter.alpha
import com.gtladd.gtladditions.client.gui.ExoticChemicalPainter.font
import com.gtladd.gtladditions.client.gui.ExoticChemicalPainter.frame
import com.gtladd.gtladditions.client.gui.ExoticChemicalPainter.mix
import com.gtladd.gtladditions.client.gui.ExoticChemicalPainter.octagon
import com.gtladd.gtladditions.client.gui.ExoticChemicalPainter.rect
import com.gtladd.gtladditions.client.gui.ExoticChemicalPainter.text
import com.gtladd.gtladditions.client.gui.ExoticChemicalPainter.textCenter
import com.gtladd.gtladditions.client.gui.ExoticChemicalPainter.ty
import com.gtladd.gtladditions.integration.xei.structureupgrade.StructureUpgradeTheme.Motif
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.jei.IngredientIO
import com.lowdragmc.lowdraglib.side.item.IItemTransfer
import com.lowdragmc.lowdraglib.utils.CycleItemStackHandler
import com.lowdragmc.lowdraglib.utils.TagOrCycleItemStackTransfer
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.datafixers.util.Either
import com.mojang.datafixers.util.Pair
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

class StructureUpgradeWidget(private val entry: StructureUpgradeEntry, private val layout: Layout) : WidgetGroup(0, 0, WIDTH, layout.height) {

    private val theme = entry.theme
    private val machineName = Component.translatable(entry.machine.descriptionId)
    private val fromLabel = "$KEY.tier".toComponent(entry.fromTier)
    private val toLabel = "$KEY.tier".toComponent(entry.toTier)
    private val conditions = entry.requirement.conditions.map { it.describeRequirement() }
    private val coreY = GRID_Y + (layout.rows * STRIDE - (STRIDE - SLOT)) / 2

    private val stars: IntArray by lazy {
        val random = Random(entry.id.hashCode())
        IntArray(STARS * 3) { if (it % 3 == 2) random.nextInt(25, 55) else random.nextInt(24) }
    }

    init {
        addWidget(slot(CycleItemStackHandler(listOf(listOf(entry.machine.asStack()))), CORE_X - 9, coreY - 9, IngredientIO.CATALYST, IGuiTexture.EMPTY))
        entry.requirement.costs.forEachIndexed { i, cost ->
            addWidget(slot(transfer(cost), PAD + i % COLUMNS * STRIDE, GRID_Y + i / COLUMNS * STRIDE, IngredientIO.INPUT, GuiTextures.SLOT))
        }
    }

    // ========================================
    // SLOTS
    // ========================================

    private fun slot(transfer: IItemTransfer, x: Int, y: Int, io: IngredientIO, background: IGuiTexture): SlotWidget =
        SlotWidget(transfer, 0, x, y, false, false).apply {
            setBackgroundTexture(background)
            setIngredientIO(io)
        }

    private fun transfer(cost: TierCost): IItemTransfer {
        val amount = Ints.saturatedCast(cost.amount)
        val tag = cost.tag ?: return CycleItemStackHandler(listOf(cost.ingredient.items.map { it.copyWithCount(amount) }))
        return TagOrCycleItemStackTransfer(listOf(Either.left(listOf(Pair.of(tag, amount)))))
    }

    // ========================================
    // RENDERING
    // ========================================

    @OnlyIn(Dist.CLIENT)
    override fun drawInBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val pose = graphics.pose()
        pose.pushPose()
        pose.translate(position.x.toFloat(), position.y.toFloat(), 0f)
        drawHeader(graphics)
        drawChevrons(graphics)
        drawCore(graphics)
        drawConditions(graphics)
        pose.popPose()

        super.drawInBackground(graphics, mouseX, mouseY, partialTicks)
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawHeader(g: GuiGraphics) {
        g.blit(glassTexture(theme), 0, 0, 0f, 0f, WIDTH, HEADER_H, WIDTH, HEADER_H)

        val badgeW = 2 * BADGE_PAD + font.width(fromLabel) + font.width(toLabel) + CHEVRONS_W + 6
        val badgeX = WIDTH - PAD - badgeW
        val railX = badgeX - 6 - RAIL_W
        val nameW = railX - 6 - NAME_X

        val scrimW = NAME_X + min(font.width(machineName), nameW) + 10
        rect(g, 0, 2, scrimW, HEADER_H - 3, alpha(theme.plate, SCRIM))
        for (i in 0 until SCRIM_FADE) {
            rect(g, scrimW + i, 2, 1, HEADER_H - 3, alpha(theme.plate, SCRIM * (1f - i / SCRIM_FADE.toFloat())))
        }

        fitText(g, machineName, NAME_X, ty(0, HEADER_H), nameW, NAME_COLOR, true)
        drawBadge(g, badgeX, badgeW)
        drawRail(g, railX)
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawBadge(g: GuiGraphics, x: Int, w: Int) {
        val y = BADGE_Y
        val border = alpha(theme.accent, 0.9f)
        rect(g, x + 1, y + 1, w - 2, BADGE_H - 2, alpha(theme.plate, 0.85f))
        rect(g, x + 1, y, w - 2, 1, border)
        rect(g, x + 1, y + BADGE_H - 1, w - 2, 1, border)
        rect(g, x, y + 1, 1, BADGE_H - 2, border)
        rect(g, x + w - 1, y + 1, 1, BADGE_H - 2, border)

        val textY = ty(y, BADGE_H)
        val chevronX = x + BADGE_PAD + text(g, fromLabel, x + BADGE_PAD, textY, mix(theme.accentText, theme.deep, 0.35f)) + 3
        chevron(g, chevronX, textY, alpha(theme.accent, 0.55f))
        chevron(g, chevronX + CHEVRONS_W - 4, textY, theme.accent)
        text(g, toLabel, chevronX + CHEVRONS_W + 3, textY, theme.accentText)
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawRail(g: GuiGraphics, x: Int) {
        val y = RAIL_Y
        val span = entry.maxTier - 1f
        val reached = (RAIL_W * (entry.fromTier - 1) / span).roundToInt()
        val target = (RAIL_W * (entry.toTier - 1) / span).roundToInt()
        rect(g, x - 1, y - 1, RAIL_W + 2, 5, GROOVE_SHADOW)
        rect(g, x, y, RAIL_W, 3, alpha(theme.cell, 0.9f))
        rect(g, x, y, reached, 3, theme.accentDim)
        rect(g, x + reached, y, target - reached, 3, theme.accent)
        rect(g, x + reached, y, target - reached, 1, theme.accentText)
        for (tier in 1 until entry.maxTier - 1) {
            rect(g, x + (RAIL_W * tier / span).roundToInt(), y, 1, 3, TICK)
        }
        rect(g, x - 1, y + 3, RAIL_W + 2, 1, GROOVE_LIP)
    }

    @OnlyIn(Dist.CLIENT)
    private fun chevron(g: GuiGraphics, x: Int, y: Int, color: Int) {
        for (dy in 0..6) {
            rect(g, x + 3 - abs(dy - 3), y + dy, 1, 1, color)
        }
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawChevrons(g: GuiGraphics) {
        for (k in 0..2) {
            val color = alpha(theme.ink, 0.3f + 0.35f * k)
            for (dy in 0..8) {
                rect(g, CHEVRONS_X + k * 7 + 4 - abs(dy - 4), coreY - 4 + dy, 2, 1, color)
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawCore(g: GuiGraphics) {
        val cx = CORE_X
        val cy = coreY
        octagon(g, cx - 17, cy - 17, 34, 9, alpha(theme.glow, 0.1f), alpha(theme.glow, 0.03f), alpha(theme.glowInk, 0.45f), true)
        octagon(g, cx - 15, cy - 15, 30, 8, mix(theme.deep, theme.glow, 0.3f), mix(theme.deep, theme.glow, 0.06f), theme.accent, false)

        when (theme.motif) {
            Motif.LATTICE -> {
                val dot = alpha(theme.accent, 0.18f)
                for (y in cy - 11..cy + 11 step 4) {
                    for (x in cx - 11 + y / 4 % 2 * 2..cx + 11 step 4) {
                        rect(g, x, y, 1, 1, dot)
                    }
                }
            }

            Motif.STARFIELD -> {
                val star = mix(STAR, theme.glow, 0.35f)
                for (i in 0 until STARS) {
                    rect(g, cx - 12 + stars[3 * i], cy - 12 + stars[3 * i + 1], 1, 1, alpha(star, stars[3 * i + 2] / 100f))
                }
            }
        }

        rect(g, cx - 10, cy - 10, 20, 20, theme.cell)
        frame(g, cx - 10, cy - 10, 20, 20, alpha(theme.accent, 0.75f))
        textCenter(g, toLabel, cx, cy + 19, theme.ink)
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawConditions(g: GuiGraphics) {
        val y0 = layout.conditionY
        if (conditions.isEmpty()) {
            diamond(g, PAD + 1, y0 + 1, EMPTY_MARK, false)
            text(g, NO_CONDITION, PAD + 9, y0, MUTED)
            return
        }

        for (i in conditions.indices) {
            val y = y0 + i * LINE_H
            diamond(g, PAD + 1, y + 1, theme.glowInk, true)
            fitText(g, conditions[i], PAD + 9, y, WIDTH - 2 * PAD - 9, LABEL, false)
        }
    }

    @OnlyIn(Dist.CLIENT)
    private fun diamond(g: GuiGraphics, x: Int, y: Int, color: Int, filled: Boolean) {
        for (dy in 0..4) {
            val inset = abs(dy - 2)
            if (filled) {
                rect(g, x + inset, y + dy, 5 - 2 * inset, 1, color)
            } else {
                rect(g, x + inset, y + dy, 1, 1, color)
                rect(g, x + 4 - inset, y + dy, 1, 1, color)
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private fun fitText(g: GuiGraphics, label: Component, x: Int, y: Int, maxWidth: Int, color: Int, shadow: Boolean) {
        val width = font.width(label)
        if (width <= maxWidth) {
            g.drawString(font, label, x, y, color, shadow)
            return
        }

        val k = maxWidth / width.toFloat()
        val pose = g.pose()
        pose.pushPose()
        pose.translate(x.toFloat(), y + 4f * (1f - k), 0f)
        pose.scale(k, k, 1f)
        g.drawString(font, label, 0, 0, color, shadow)
        pose.popPose()
    }

    /** Shared by every page, so JEI and EMI show one card size for the whole category. */
    class Layout private constructor(val rows: Int, conditionLines: Int) {
        val conditionY = GRID_Y + rows * STRIDE
        val height = conditionY + conditionLines * LINE_H + 1

        companion object {
            fun of(entries: List<StructureUpgradeEntry>) = Layout(
                max(MIN_ROWS, entries.maxOf { (it.requirement.costs.size + COLUMNS - 1) / COLUMNS }),
                max(1, entries.maxOf { it.requirement.conditions.size })
            )
        }
    }

    companion object {
        const val WIDTH = 188

        private const val KEY = "gtladditions.structure_tier.xei"
        private val NO_CONDITION = "$KEY.no_condition".toComponent

        private const val PAD = 4
        private const val HEADER_H = 18
        private const val NAME_X = PAD + 2
        private const val BADGE_Y = 3
        private const val BADGE_H = 12
        private const val BADGE_PAD = 5
        private const val CHEVRONS_W = 9
        private const val RAIL_W = 30
        private const val RAIL_Y = 8
        private const val SCRIM = 0.26f
        private const val SCRIM_FADE = 24

        private const val COLUMNS = 4
        private const val MIN_ROWS = 4
        private const val SLOT = 18

        // EMI right-aligns amounts from 3 px into the icon, so "256" ends 4 px past its slot frame.
        private const val STRIDE = SLOT + 4
        private const val GRID_Y = HEADER_H + 4
        private const val CORE_X = WIDTH - 30
        private const val CHEVRONS_X = (PAD + (COLUMNS - 1) * STRIDE + SLOT + CORE_X - 17) / 2 - 10
        private const val STARS = 14
        private const val LINE_H = 10

        private const val NAME_COLOR = 0xFFF2F4F6.toInt()
        private const val STAR = 0xFFEDEFF5.toInt()
        private const val LABEL = 0xFF404040.toInt()
        private const val MUTED = 0xFF6E6E6E.toInt()
        private const val EMPTY_MARK = 0xFF8B8B8B.toInt()
        private const val GROOVE_SHADOW = 0x73000000
        private const val GROOVE_LIP = 0x1FFFFFFF
        private const val TICK = 0x88000000.toInt()
        private const val BLACK = 0xFF000000.toInt()
        private const val WHITE = 0xFFFFFFFF.toInt()

        // Diagonal glass reflections as (x at the top row, half-width, strength).
        private val REFLECTIONS = floatArrayOf(40f, 3f, 0.55f, 50f, 1.2f, 0.35f, 132f, 2.2f, 0.38f)

        private val GLASS = HashMap<StructureUpgradeTheme, ResourceLocation>()

        @OnlyIn(Dist.CLIENT)
        private fun glassTexture(theme: StructureUpgradeTheme): ResourceLocation = GLASS.getOrPut(theme) {
            val image = NativeImage(WIDTH, HEADER_H, false)
            for (y in 0 until HEADER_H) {
                for (x in 0 until WIDTH) {
                    val argb = glassPixel(theme, x, y)
                    image.setPixelRGBA(x, y, (argb and 0xFF00FF00.toInt()) or (argb shr 16 and 0xFF) or (argb and 0xFF shl 16))
                }
            }

            GTLAdditions.id("structure_upgrade/glass_${GLASS.size}").also {
                Minecraft.getInstance().textureManager.register(it, DynamicTexture(image))
            }
        }

        private fun glassPixel(theme: StructureUpgradeTheme, x: Int, y: Int): Int {
            var t = 0.3f - 0.12f * y / HEADER_H + 0.1f * (fbm(x / 30f, y / 12f) - 0.5f)
            for (i in REFLECTIONS.indices step 3) {
                val d = (x - 1.4f * y - REFLECTIONS[i]) / REFLECTIONS[i + 1]
                t += REFLECTIONS[i + 2] * exp(-d * d)
            }

            val c = mix(ramp(theme.glass, t), BLACK, 0.05f + 0.3f * y / (HEADER_H - 1))
            return when (y) {
                0 -> {
                    val f = x / WIDTH.toFloat()
                    val d = 2f * f - 1f
                    mix(c, mix(theme.accent, theme.glow, f), 0.35f + 0.6f * (1f - d * d))
                }

                1 -> mix(c, WHITE, 0.18f)

                HEADER_H - 1 -> mix(c, BLACK, 0.55f)

                else -> c
            }
        }

        private fun ramp(stops: IntArray, t: Float): Int {
            val s = t.coerceIn(0f, 1f) * (stops.size - 1)
            val i = min(s.toInt(), stops.size - 2)
            return mix(stops[i], stops[i + 1], s - i)
        }

        private fun fbm(x: Float, y: Float): Float = (valueNoise(x, y, 0) + 0.5f * valueNoise(2f * x, 2f * y, 17)) / 1.5f

        private fun valueNoise(x: Float, y: Float, seed: Int): Float {
            val ix = floor(x).toInt()
            val iy = floor(y).toInt()
            val fx = x - ix
            val fy = y - iy
            val sx = fx * fx * (3f - 2f * fx)
            val sy = fy * fy * (3f - 2f * fy)
            val top = hash(ix, iy, seed) + (hash(ix + 1, iy, seed) - hash(ix, iy, seed)) * sx
            val bottom = hash(ix, iy + 1, seed) + (hash(ix + 1, iy + 1, seed) - hash(ix, iy + 1, seed)) * sx
            return top + (bottom - top) * sy
        }

        private fun hash(x: Int, y: Int, seed: Int): Float {
            var h = x * 374761393 + y * 668265263 + seed * 1442695041
            h = (h xor (h ushr 13)) * 1274126177
            return ((h xor (h ushr 16)) and 0xFFFF) / 65535f
        }
    }
}
