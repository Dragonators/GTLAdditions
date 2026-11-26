package com.gtladd.gtladditions.integration.xei.structureupgrade

import com.gtladd.gtladditions.client.gui.ExoticChemicalPainter.lightness
import com.gtladd.gtladditions.client.gui.ExoticChemicalPainter.mix
import kotlin.math.max
import kotlin.math.min

/**
 * - [glass]: four header-glass stops from shadow to highlight; [glass]`[1]` is also the tint of the dark surfaces.
 * - [accent]: tier badge, tier bar, chevrons and the core ring.
 * - [glow]: the crown's far end, the core's inner light and condition marks.
 */
class StructureUpgradeTheme(val glass: IntArray, val accent: Int, val glow: Int, val motif: Motif) {
    val base = glass[1]
    val deep = lightness(base, 0.5f)
    val cell = lightness(base, 0.25f)
    val plate = lightness(base, 0.35f)
    val accentText = mix(accent, WHITE, 0.4f)
    val accentDim = mix(accent, deep, 0.5f)

    // Readable on the recipe viewers' native grey.
    val ink = withLightness(accent, 0.3f)
    val glowInk = withLightness(glow, 0.4f)

    enum class Motif {
        LATTICE,
        STARFIELD
    }

    companion object {
        private const val WHITE = 0xFFFFFFFF.toInt()

        // Glass ramp sampled from GTCEu's chemically inert (PTFE) machine casing.
        val DEFAULT = StructureUpgradeTheme(
            intArrayOf(0xFF141414.toInt(), 0xFF282828.toInt(), 0xFF454545.toInt(), 0xFFA8A8A8.toInt()),
            0xFF34D5B8.toInt(),
            0xFFA6E35E.toInt(),
            Motif.LATTICE
        )

        // Glass ramp sampled from GTLCore's dimensionally transcendent machine casing.
        val COSMIC = StructureUpgradeTheme(
            intArrayOf(0xFF021932.toInt(), 0xFF0B3157.toInt(), 0xFF164E88.toInt(), 0xFF8DB7E6.toInt()),
            0xFF6AAEF0.toInt(),
            0xFF9FDCFF.toInt(),
            Motif.STARFIELD
        )

        private fun withLightness(color: Int, target: Float): Int {
            val r = color shr 16 and 255
            val g = color shr 8 and 255
            val b = color and 255
            val l = (max(r, max(g, b)) + min(r, min(g, b))) / 510f
            return lightness(color, target / l)
        }
    }
}
