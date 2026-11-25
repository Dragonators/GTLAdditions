package com.gtladd.gtladditions.client.render.machine.matrix

import kotlin.math.sqrt

object MatrixFrameDetails {
    class SolidQuad(val vertices: FloatArray, val normal: FloatArray, val color: FloatArray)

    class GlowQuad(val vertices: FloatArray, val color: FloatArray, val alpha: Float)

    data class Details(val panels: List<SolidQuad>, val lights: List<GlowQuad>)

    private val details by lazy { MatrixEffectGeometry.layers.mapIndexed(::build) }

    fun layer(index: Int): Details = details[index]

    private fun build(index: Int, layer: MatrixEffectGeometry.Layer): Details {
        val panels = ArrayList<SolidQuad>()
        val lights = ArrayList<GlowQuad>()
        val half = layer.side / 2f
        val thickness = layer.thickness
        val plate = floatArrayOf(0.13f, 0.16f, 0.20f)
        val bed = floatArrayOf(0.055f, 0.07f, 0.09f)
        val rim = floatArrayOf(0.24f, 0.28f, 0.33f)
        val blue = floatArrayOf(0.38f, 0.8f, 1f)
        val violet = floatArrayOf(0.67f, 0.42f, 1f)
        val color = if (index % 2 == 0) blue else violet

        fun point(normal: Int, sign: Int, along: Int, u: Float, v: Float, height: Float, inward: Boolean): FloatArray =
            FloatArray(3).also {
                it[normal] = sign * if (inward) half - thickness - height else half + height
                it[along] = u
                it[3 - normal - along] = v
            }

        fun rectangle(
            normal: Int,
            sign: Int,
            along: Int,
            u0: Float,
            u1: Float,
            v0: Float,
            v1: Float,
            height: Float,
            inward: Boolean
        ): FloatArray {
            val points = arrayOf(
                point(normal, sign, along, u0, v0, height, inward),
                point(normal, sign, along, u1, v0, height, inward),
                point(normal, sign, along, u1, v1, height, inward),
                point(normal, sign, along, u0, v1, height, inward)
            )
            val cross = if ((along + 1) % 3 == 3 - normal - along) 1 else -1
            val outward = if (inward) -sign else sign
            val order = if (cross == outward) intArrayOf(0, 1, 2, 3) else intArrayOf(3, 2, 1, 0)
            return FloatArray(12) { points[order[it / 3]][it % 3] }
        }

        fun face(vertices: FloatArray, tint: FloatArray) {
            val a = FloatArray(3) { vertices[3 + it] - vertices[it] }
            val b = FloatArray(3) { vertices[6 + it] - vertices[it] }
            val normal = floatArrayOf(a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])
            val length = sqrt(normal.sumOf { (it * it).toDouble() }).toFloat()
            for (axis in 0..2) normal[axis] /= length
            panels.add(SolidQuad(vertices, normal, tint))
        }

        // No back face: it is embedded in its support.
        fun slab(
            normal: Int,
            sign: Int,
            along: Int,
            u0: Float,
            u1: Float,
            v0: Float,
            v1: Float,
            low: Float,
            high: Float,
            tint: FloatArray,
            inward: Boolean
        ) {
            val back = rectangle(normal, sign, along, u0, u1, v0, v1, low * thickness, inward)
            val front = rectangle(normal, sign, along, u0, u1, v0, v1, high * thickness, inward)
            face(front, tint)

            for (edge in 0..3) {
                val next = (edge + 1) % 4
                face(
                    FloatArray(12) { i ->
                        val vertex = if (i / 3 == 0 || i / 3 == 3) edge else next
                        (if (i / 3 < 2) back else front)[vertex * 3 + i % 3]
                    },
                    tint
                )
            }
        }

        fun backing(normal: Int, sign: Int, along: Int, u0: Float, u1: Float, v0: Float, v1: Float, channel: Boolean, inward: Boolean) {
            val floor = if (channel) 0.025f else 0.05f
            val top = if (channel) 0.06f else 0.08f
            val edge = thickness * if (channel) 0.022f else 0.03f
            slab(normal, sign, along, u0, u1, v0, v1, 0.004f, floor, if (channel) bed else plate, inward)
            slab(normal, sign, along, u0, u1, v0, v0 + edge, floor, top, rim, inward)
            slab(normal, sign, along, u0, u1, v1 - edge, v1, floor, top, rim, inward)
            slab(normal, sign, along, u0, u0 + edge, v0 + edge, v1 - edge, floor, top, rim, inward)
            slab(normal, sign, along, u1 - edge, u1, v0 + edge, v1 - edge, floor, top, rim, inward)
        }

        fun glow(
            normal: Int,
            sign: Int,
            along: Int,
            u0: Float,
            u1: Float,
            v0: Float,
            v1: Float,
            height: Float,
            inward: Boolean,
            alpha: Float = 0.7f
        ) {
            lights.add(GlowQuad(rectangle(normal, sign, along, u0, u1, v0, v1, height * thickness, inward), color, alpha))
        }

        for (inward in listOf(false, true)) {
            for (along in 0..2) {
                val axisU = (along + 1) % 3
                val axisV = (along + 2) % 3

                for (signU in intArrayOf(-1, 1)) {
                    for (signV in intArrayOf(-1, 1)) {
                        for (normal in intArrayOf(axisU, axisV)) {
                            val sign = if (normal == axisU) signU else signV
                            val acrossSign = if (normal == axisU) signV else signU
                            val center = acrossSign * (half - thickness * 0.5f)
                            val end = half - layer.jointLength - 0.15f
                            val plaque = along == 0 && normal == axisU
                            val gap = thickness * 0.85f
                            val ranges = if (plaque) listOf(-end to -gap, gap to end) else listOf(-end to end)

                            for ((start, finish) in ranges.filter { it.second > it.first }) {
                                backing(
                                    normal,
                                    sign,
                                    along,
                                    start,
                                    finish,
                                    center - thickness * 0.11f,
                                    center + thickness * 0.11f,
                                    true,
                                    inward
                                )
                                glow(
                                    normal,
                                    sign,
                                    along,
                                    start + thickness * 0.03f,
                                    finish - thickness * 0.03f,
                                    center - thickness * 0.025f,
                                    center + thickness * 0.025f,
                                    0.033f,
                                    inward
                                )
                            }

                            if (plaque) {
                                backing(
                                    normal,
                                    sign,
                                    along,
                                    -gap + 0.08f,
                                    gap - 0.08f,
                                    center - thickness * 0.36f,
                                    center + thickness * 0.36f,
                                    false,
                                    inward
                                )
                                val rune = MatrixEffectGeometry.runeStrokes(
                                    index * 4 + (if (signU > 0) 2 else 0) + (if (signV > 0) 1 else 0)
                                )

                                for (stroke in rune.indices step 4) {
                                    val u0 = rune[stroke + 1] * thickness * 0.55f
                                    val v0 = center + rune[stroke] * thickness * 0.42f
                                    val u1 = rune[stroke + 3] * thickness * 0.55f
                                    val v1 = center + rune[stroke + 2] * thickness * 0.42f
                                    val du = u1 - u0
                                    val dv = v1 - v0
                                    val length = sqrt(du * du + dv * dv)
                                    val a = -dv / length * thickness * 0.014f
                                    val b = du / length * thickness * 0.014f
                                    val points = arrayOf(
                                        point(normal, sign, along, u0 + a, v0 + b, thickness * 0.058f, inward),
                                        point(normal, sign, along, u1 + a, v1 + b, thickness * 0.058f, inward),
                                        point(normal, sign, along, u1 - a, v1 - b, thickness * 0.058f, inward),
                                        point(normal, sign, along, u0 - a, v0 - b, thickness * 0.058f, inward)
                                    )
                                    lights.add(GlowQuad(FloatArray(12) { points[it / 3][it % 3] }, violet, 0.9f))
                                }
                            }
                        }
                    }
                }
            }
        }

        for (normal in 0..2) {
            for (sign in intArrayOf(-1, 1)) {
                val along = (normal + 1) % 3

                for (su in intArrayOf(-1, 1)) {
                    for (sv in intArrayOf(-1, 1)) {
                        val u = su * (half - thickness * 0.5f)
                        val v = sv * (half - thickness * 0.5f)
                        val radius = thickness * 0.32f
                        backing(normal, sign, along, u - radius, u + radius, v - radius, v + radius, false, false)
                        val r = radius * 0.75f
                        val w = thickness * 0.022f
                        glow(normal, sign, along, u - r, u + r, v - r - w, v - r + w, 0.058f, false, 0.42f)
                        glow(normal, sign, along, u - r, u + r, v + r - w, v + r + w, 0.058f, false, 0.42f)
                        glow(normal, sign, along, u - r - w, u - r + w, v - r, v + r, 0.058f, false, 0.42f)
                        glow(normal, sign, along, u + r - w, u + r + w, v - r, v + r, 0.058f, false, 0.42f)
                        glow(normal, sign, along, u - w * 2, u + w * 2, v - w * 2, v + w * 2, 0.058f, false, 0.8f)
                    }
                }
            }
        }

        return Details(panels, lights)
    }
}
