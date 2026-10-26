package com.gtladd.gtladditions.client.render.machine.matrix

import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object MatrixEffectGeometry {
    const val CIRCLE_RADIUS = 42.0f
    const val CIRCLE_Y = 2.03f // Above model Y=2, relative to the Q2-B controller at (0, 1, 15).
    const val CUBE_CENTER_Y = 58.5f
    const val CENTER_OFFSET_Z = -15.0

    // Keep all four layers in the transform/hide-mask data; only the first three are solid frames.
    const val ENERGY_CORE_LAYER_INDEX = 3
    const val CIRCLE_DEGREES_PER_SECOND = 3.0
    const val BEAM_RADIUS = 1.25f
    const val BEAM_HEIGHT = 1024.0f
    const val IDLE_BEAM_ALPHA = 0.18f
    const val ACTIVE_BEAM_ALPHA = 0.8f

    data class Layer(
        val side: Float,
        val thickness: Float,
        val angles: FloatArray,
        val axis: FloatArray,
        val speed: Double,
        val jointLength: Float,
        val material: String,
        val jointMaterial: String
    )

    val layers = listOf(
        Layer(
            40f,
            2f,
            floatArrayOf(16f, -18f, 24f),
            floatArrayOf(0f, 0f, 1f),
            4.0,
            4f,
            "gtladditions:gravity_stabilization_casing",
            "gtladditions:god_forge_trim_casing"
        ),
        Layer(
            23.8f,
            1.4f,
            floatArrayOf(27f, -11f, 35f),
            floatArrayOf(0f, 1f, 0f),
            -7.0,
            2.8f,
            "gtlcore:degenerate_rhenium_constrained_casing",
            "gtceu:atomic_casing"
        ),
        Layer(
            13.2f,
            1.2f,
            floatArrayOf(39f, -4f, 49f),
            floatArrayOf(1f, 0f, 0f),
            11.0,
            2.4f,
            "gtladditions:gravity_stabilization_casing",
            "gtladditions:god_forge_trim_casing"
        ),
        Layer(
            6f,
            1f,
            floatArrayOf(52f, 4f, 65f),
            floatArrayOf(0f, 0f, 1f),
            -16.0,
            1f,
            "kubejs:dimensional_stability_casing",
            "kubejs:dimensional_stability_casing"
        )
    )

    data class Face(val axis: Int, val sign: Int)

    data class UnitBlock(val x: Int, val y: Int, val z: Int, val material: String, val faces: List<Face>)
    data class Line(val x0: Float, val z0: Float, val x1: Float, val z1: Float, val purple: Boolean = false, val fine: Boolean = false)
    data class SealRing(val x: Float, val z: Float, val radius: Float, val purple: Boolean = false, val fine: Boolean = false, val flowing: Boolean = false)

    // Centreline circles shared by the seal geometry and its volume renderer.
    val sealRings = buildList {
        add(SealRing(0f, 0f, 0.83f, flowing = true))
        add(SealRing(0f, 0f, 0.79f, flowing = true))
        add(SealRing(0f, 0f, 0.25f, flowing = true))
        for (i in 0 until 6) {
            val angle = i * Math.PI / 3
            add(SealRing((cos(angle) * 0.98).toFloat(), (sin(angle) * 0.98).toFloat(), 0.055f, true))
            add(SealRing((cos(angle) * 0.81).toFloat(), (sin(angle) * 0.81).toFloat(), 0.006f, fine = true))
        }
    }

    // Rune strokes in local tangent/radial coordinates; no font or glyph atlas needed.
    private val runes = listOf(
        floatArrayOf(-0.4f, -1f, -0.4f, 1f, -0.4f, 0.3f, 0.5f, 0.85f, -0.4f, -0.3f, 0.5f, 0.25f), // ᚠ
        floatArrayOf(-0.5f, -1f, -0.5f, 1f, -0.5f, 1f, 0.5f, 0.6f, 0.5f, 0.6f, 0.5f, -1f), // ᚢ
        floatArrayOf(-0.4f, -1f, -0.4f, 1f, -0.4f, 0.65f, 0.5f, 0.15f, 0.5f, 0.15f, -0.4f, -0.35f), // ᚦ
        floatArrayOf(-0.4f, -1f, -0.4f, 1f, -0.4f, 0.75f, 0.5f, 0.25f, -0.4f, 0.05f, 0.5f, -0.45f), // ᚨ
        floatArrayOf(-0.4f, -1f, -0.4f, 1f, -0.4f, 1f, 0.5f, 0.5f, 0.5f, 0.5f, -0.4f, 0f, -0.4f, 0f, 0.5f, -1f), // ᚱ
        floatArrayOf(-0.4f, -1f, -0.4f, 1f, -0.4f, 1f, 0.5f, 0.5f, 0.5f, 0.5f, -0.4f, 0f), // ᚹ
        floatArrayOf(-0.5f, -1f, -0.5f, 1f, 0.5f, -1f, 0.5f, 1f, -0.5f, 0.5f, 0.5f, -0.5f), // ᚺ
        floatArrayOf(0f, -1f, 0f, 1f, -0.55f, 0.4f, 0.55f, -0.4f), // ᚾ
        floatArrayOf(0.4f, 1f, -0.5f, 0.45f, -0.5f, 0.45f, 0.15f, 0.05f, -0.4f, -1f, 0.5f, -0.45f, 0.5f, -0.45f, -0.15f, -0.05f), // ᛃ
        floatArrayOf(0f, -1f, 0f, 1f, 0f, 1f, 0.5f, 0.5f, 0f, -1f, -0.5f, -0.5f), // ᛇ
        floatArrayOf(
            -0.5f, -1f, -0.5f, 1f,
            -0.5f, 1f, 0.5f, 0.5f,
            0.5f, 0.5f, 0.1f, 0f,
            -0.5f, -1f, 0.5f, -0.5f,
            0.5f, -0.5f, 0.1f, 0f
        ), // ᛈ
        floatArrayOf(0f, -1f, 0f, 1f, 0f, 0.1f, -0.6f, 0.75f, 0f, 0.1f, 0.6f, 0.75f), // ᛉ
        floatArrayOf(0.5f, 1f, -0.5f, 0.3f, -0.5f, 0.3f, 0.5f, -0.3f, 0.5f, -0.3f, -0.5f, -1f), // ᛋ
        floatArrayOf(
            -0.5f, -1f, -0.5f, 1f,
            -0.5f, 1f, 0.5f, 0.5f,
            0.5f, 0.5f, -0.5f, 0f,
            -0.5f, 0f, 0.5f, -0.5f,
            0.5f, -0.5f, -0.5f, -1f
        ), // ᛒ
        floatArrayOf(-0.5f, -1f, -0.5f, 1f, 0.5f, -1f, 0.5f, 1f, -0.5f, 1f, 0f, 0f, 0.5f, 1f, 0f, 0f), // ᛖ
        floatArrayOf(-0.5f, -1f, -0.5f, 1f, 0.5f, -1f, 0.5f, 1f, -0.5f, 1f, 0.5f, 0f, 0.5f, 1f, -0.5f, 0f), // ᛗ
        floatArrayOf(-0.5f, -1f, -0.5f, 1f, 0.5f, -1f, 0.5f, 1f, -0.5f, 1f, 0.5f, -1f, 0.5f, 1f, -0.5f, -1f), // ᛞ
        floatArrayOf(
            0f, 1f, -0.5f, 0f,
            -0.5f, 0f, 0f, -0.7f,
            0f, -0.7f, 0.5f, 0f,
            0.5f, 0f, 0f, 1f,
            -0.5f, -1f, 0f, -0.7f,
            0f, -0.7f, 0.5f, -1f
        ) // ᛟ
    )

    fun angle(ticks: Double, degreesPerSecond: Double): Float = ((ticks / 20.0 * degreesPerSecond) % 360.0).toFloat()

    internal fun runeStrokes(index: Int): FloatArray = runes[index % runes.size]

    fun unitSide(layer: Layer): Int = (layer.side / layer.thickness).roundToInt()

    /** Unscaled source frame. */
    fun unitBlocks(layer: Layer): List<UnitBlock> {
        val side = unitSide(layer)
        val joints = (layer.jointLength / layer.thickness).roundToInt().coerceAtLeast(1)
        fun occupied(x: Int, y: Int, z: Int): Boolean = x in 0 until side &&
            y in 0 until side &&
            z in 0 until side &&
            (if (x == 0 || x == side - 1) 1 else 0) + (if (y == 0 || y == side - 1) 1 else 0) + (if (z == 0 || z == side - 1) 1 else 0) >= 2
        val blocks = ArrayList<UnitBlock>()

        for (x in 0 until side) {
            for (y in 0 until side) {
                for (z in 0 until side) {
                    if (!occupied(x, y, z)) continue
                    val coordinates = intArrayOf(x, y, z)
                    val faces = ArrayList<Face>(6)

                    for (axis in 0..2) {
                        for (sign in intArrayOf(-1, 1)) {
                            val next = coordinates.copyOf()
                            next[axis] += sign
                            if (!occupied(next[0], next[1], next[2])) faces.add(Face(axis, sign))
                        }
                    }

                    val joint = coordinates.all { minOf(it, side - 1 - it) < joints }
                    blocks.add(UnitBlock(x, y, z, if (joint) layer.jointMaterial else layer.material, faces))
                }
            }
        }

        return blocks
    }

    /** Straight strokes only; circular strokes are described by [sealRings]. */
    fun circle(): List<Line> {
        val lines = ArrayList<Line>()
        fun line(x0: Double, z0: Double, x1: Double, z1: Double, purple: Boolean = false, fine: Boolean = false) {
            lines.add(Line(x0.toFloat(), z0.toFloat(), x1.toFloat(), z1.toFloat(), purple, fine))
        }
        fun polygon(radius: Double, count: Int, offset: Double = 0.0, purple: Boolean = false) {
            for (i in 0 until count) {
                val a = offset + i * Math.PI * 2 / count
                val b = offset + (i + 1) * Math.PI * 2 / count
                line(cos(a) * radius, sin(a) * radius, cos(b) * radius, sin(b) * radius, purple)
            }
        }
        fun diamond(angle: Double, tangentHalfWidth: Double, radialHalfHeight: Double, purple: Boolean) {
            val cosine = cos(angle)
            val sine = sin(angle)
            val vertices = arrayOf(
                0.0 to radialHalfHeight,
                tangentHalfWidth to 0.0,
                0.0 to -radialHalfHeight,
                -tangentHalfWidth to 0.0
            ).map { (tangent, radial) ->
                (cosine * (0.81 + radial) - sine * tangent) to
                    (sine * (0.81 + radial) + cosine * tangent)
            }
            for (i in vertices.indices) {
                val (x0, z0) = vertices[i]
                val (x1, z1) = vertices[(i + 1) % vertices.size]
                line(x0, z0, x1, z1, purple, fine = true)
            }
        }
        polygon(1.0, 6)
        polygon(0.967, 6)
        polygon(0.74, 3, purple = true)
        polygon(0.74, 3, Math.PI, true)
        polygon(0.30, 6)
        // Six principal seals align with the star, leaving the intervals undecorated.
        for (i in 0 until 6) {
            val a = i * Math.PI / 3
            diamond(a, 0.019, 0.028, purple = true)
        }
        for (i in 0 until 6) {
            val a = i * Math.PI / 3
            line(cos(a) * 0.3, sin(a) * 0.3, cos(a) * 0.68, sin(a) * 0.68)
            // Three runes share one outward orientation and a straight baseline along each edge.
            val b = a + Math.PI / 6
            val cosine = cos(b)
            val sine = sin(b)
            for (j in -1..1) {
                // Retain the former arc layout's tangential spacing without rotating each glyph.
                val tangentOffset = j * 0.895 * sin(0.15)
                val cx = cosine * 0.895 - sine * tangentOffset
                val cz = sine * 0.895 + cosine * tangentOffset
                val rune = runes[i * 3 + j + 1]
                for (stroke in rune.indices step 4) {
                    val x0 = rune[stroke] * 0.025
                    val y0 = rune[stroke + 1] * 0.025
                    val x1 = rune[stroke + 2] * 0.025
                    val y1 = rune[stroke + 3] * 0.025
                    line(
                        cx - sine * x0 + cosine * y0,
                        cz + cosine * x0 + sine * y0,
                        cx - sine * x1 + cosine * y1,
                        cz + cosine * x1 + sine * y1,
                        purple = true,
                        fine = true
                    )
                }
            }
        }
        return lines
    }
}