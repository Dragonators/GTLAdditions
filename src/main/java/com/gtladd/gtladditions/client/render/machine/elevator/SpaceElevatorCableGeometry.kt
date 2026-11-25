package com.gtladd.gtladditions.client.render.machine.elevator

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

// GTNH Intergalactic's spacecable.vert.glsl: four ribbons, a quarter turn apart, wind around one regular octagon.
object SpaceElevatorCableGeometry {
    private const val SCALE = 5.4f
    private const val SHORT = 1f / SCALE
    private val long = (1f + sqrt(2f)) / SCALE
    private const val SIDE = 2f / SCALE
    const val SECTION_HEIGHT = 8f * SIDE
    private const val RIBBON_HEIGHT = 0.75f
    const val STRANDS = 4
    const val PARTS = 8
    private val edgeX = floatArrayOf(long, long, SHORT, -SHORT, -long, -long, -SHORT, SHORT)
    private val edgeZ = floatArrayOf(SHORT, -SHORT, -long, -long, -SHORT, SHORT, long, long)
    private val strandCos = floatArrayOf(1f, 0f, -1f, 0f)
    private val strandSin = floatArrayOf(0f, 1f, 0f, -1f)

    // GTNH measures the light front against its 512-block cable, whatever the cable length.
    private const val PULSE_TICKS = 60.0
    private const val PULSE_SPAN = 512f
    private const val PULSE_HALF_WIDTH = 0.03f

    /** Four corners of (x, y, z, u, v), then the outward normal's x and z. */
    const val QUAD_FLOATS = 22

    enum class Facing { X, Z, DIAGONAL }

    fun sections(length: Int): Int = ceil(length / SECTION_HEIGHT).toInt()

    // Odd parts are the octagon's diagonal sides; each strand turns the even ones by 90 degrees.
    fun facing(strand: Int, part: Int): Facing = when {
        part and 1 == 1 -> Facing.DIAGONAL
        (part / 2 + strand) and 1 == 0 -> Facing.X
        else -> Facing.Z
    }

    /**
     * Corners go counter-clockwise seen from outside, in GTNH's order: next edge bottom, next edge top, edge top, edge
     * bottom. v = 0 is the top of the texture.
     */
    fun quad(section: Int, strand: Int, part: Int, radiusScale: Float, out: FloatArray) {
        val next = (part + 1) and 7
        val bottom = SECTION_HEIGHT * section + SIDE * part
        corner(out, 0, strand, next, bottom + SIDE, 0f, 1f, radiusScale)
        corner(out, 1, strand, next, bottom + SIDE + RIBBON_HEIGHT, 0f, 0f, radiusScale)
        corner(out, 2, strand, part, bottom + RIBBON_HEIGHT, 1f, 0f, radiusScale)
        corner(out, 3, strand, part, bottom, 1f, 1f, radiusScale)
        val midX = out[0] + out[15]
        val midZ = out[2] + out[17]
        val length = sqrt(midX * midX + midZ * midZ)
        out[20] = midX / length
        out[21] = midZ / length
    }

    private fun corner(out: FloatArray, index: Int, strand: Int, edge: Int, y: Float, u: Float, v: Float, radiusScale: Float) {
        val x = edgeX[edge] * radiusScale
        val z = edgeZ[edge] * radiusScale
        val cos = strandCos[strand]
        val sin = strandSin[strand]
        val offset = index * 5
        out[offset] = cos * x + sin * z
        out[offset + 1] = y
        out[offset + 2] = -sin * x + cos * z
        out[offset + 3] = u
        out[offset + 4] = v
    }

    fun pulseSections(ticks: Double, sections: Int): IntRange {
        val front = pulseFront(ticks) * PULSE_SPAN
        val reach = PULSE_HALF_WIDTH * PULSE_SPAN
        val first = ceil((front - reach) / SECTION_HEIGHT).toInt().coerceAtLeast(0)
        val last = floor((front + reach) / SECTION_HEIGHT).toInt().coerceAtMost(sections - 1)
        return first..last
    }

    // Whole sections light up together, keyed to their bottom edge.
    fun pulse(section: Int, ticks: Double): Float {
        val distance = abs(pulseFront(ticks) - section * SECTION_HEIGHT / PULSE_SPAN)
        if (distance > PULSE_HALF_WIDTH) return 0f
        val wave = sin(1.57f + distance * 33f * 1.57f)
        return wave * wave
    }

    private fun pulseFront(ticks: Double): Float = (ticks / PULSE_TICKS % 1.0).toFloat()
}
