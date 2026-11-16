package com.gtladd.gtladditions.client.render.machine.matrix

import kotlin.math.roundToInt

object MatrixEffectGeometry {
    const val CUBE_CENTER_Y = 58.5f
    const val CENTER_OFFSET_Z = -15.0

    // Keep all four layers in the transform/hide-mask data; only the first three are solid frames.
    const val ENERGY_CORE_LAYER_INDEX = 3
    const val BEAM_RADIUS = 1.25f
    const val BEAM_HEIGHT = 1024.0f

    class Layer(
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

    // Rune strokes in local tangent/radial coordinates.
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
}