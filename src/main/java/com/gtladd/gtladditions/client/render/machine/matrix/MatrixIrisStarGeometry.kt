package com.gtladd.gtladditions.client.render.machine.matrix

import org.joml.Vector2f
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Z-up model coordinates. */
object MatrixIrisStarGeometry {
    const val PLANE_Z = 2.674f
    const val BEAM_RING_RADIUS = 41.2f
    private const val TAU = PI * 2
    private const val DEG = PI / 180

    class Primitive(val a: FloatArray, val b: FloatArray, val c: FloatArray, val d: FloatArray)

    data class Beam(val x: Float, val y: Float, val group: Int)

    /** Primitives before [inner] are the inner group. */
    data class Design(val primitives: List<Primitive>, val beams: List<Beam>, val inner: Int) {
        fun group(inner: Boolean): List<Primitive> =
            if (inner) primitives.subList(0, this.inner) else primitives.subList(this.inner, primitives.size)
    }

    val design: Design by lazy { build() }

    private val reaches by lazy { floatArrayOf(reach(design.group(true)), reach(design.group(false))) }

    /** From the centre, before the vertex shader pads its quad. */
    fun reach(inner: Boolean): Float = reaches[if (inner) 0 else 1]

    private fun reach(primitives: List<Primitive>): Float = primitives.maxOf { p ->
        when (p.d[2].roundToInt()) {
            0 -> maxOf(hypot(p.a[0], p.a[1]), hypot(p.a[2], p.a[3]))
            // An arc piece's outer corners lie r * sqrt(1 + sin(h)^2) from its circle's centre, h being half its span.
            1 -> hypot(p.a[0], p.a[1]) + p.a[2] * sqrt(1 + sin(p.b[3] / 2).pow(2))
            else -> hypot(p.a[0], p.a[1]) + hypot(p.a[3], p.b[0])
        }
    }

    /** Group 0 (inner fibres) stays fixed; group 1 (rune band) and group 2 (star and beam nodes) counter-rotate. */
    fun rotation(group: Int, seconds: Double): Float = Math.toRadians(
        (
            seconds * when (group) {
                1 -> 2.5
                2 -> -2.0
                else -> 0.0
            }
            ) % 360.0
    ).toFloat()

    fun beamPosition(beam: Beam, seconds: Double): Vector2f {
        val a = rotation(beam.group, seconds)
        return Vector2f(cos(a) * beam.x - sin(a) * beam.y, sin(a) * beam.x + cos(a) * beam.y)
    }

    private data class Style(
        val width: Int = 1,
        val secondary: Boolean = false,
        val group: Int = 0,
        val flow: Boolean = false,
        val intensity: Float = 1f,
        val endIntensity: Float? = null,
        val rune: Boolean = false
    )

    private class Builder {
        val items = ArrayList<Primitive>()
        val beams = ArrayList<Beam>()

        fun push(type: Int, a: DoubleArray, b: DoubleArray, s: Style) {
            items.add(
                Primitive(
                    a.map(Double::toFloat).toFloatArray(),
                    b.map(Double::toFloat).toFloatArray(),
                    floatArrayOf(s.width.toFloat(), if (s.secondary) 1f else 0f, s.group.toFloat(), if (s.flow) 1f else 0f),
                    floatArrayOf(s.intensity, s.endIntensity ?: s.intensity, type.toFloat(), if (s.rune) 1f else 0f)
                )
            )
        }

        fun segment(x0: Double, y0: Double, x1: Double, y1: Double, s: Style) =
            push(0, doubleArrayOf(x0, y0, x1, y1), DoubleArray(4), s)

        fun arc(x: Double, y: Double, r: Double, start: Double, end: Double, s: Style) {
            val span = end - start
            val n = maxOf(1, ceil(span / (150 * DEG)).toInt(), ceil(minOf(span / (12 * DEG), span * r / 2.5)).toInt())

            for (i in 0 until n) {
                push(1, doubleArrayOf(x, y, r, 0.0), doubleArrayOf(start, span, start + span * i / n, span / n), s)
            }
        }

        fun ring(x: Double, y: Double, r: Double, s: Style) = arc(x, y, r, 0.0, TAU, s)

        fun polygon(points: List<Pair<Double, Double>>, s: Style) {
            for (i in points.indices) {
                val a = points[i]
                val b = points[(i + 1) % points.size]
                segment(a.first, a.second, b.first, b.second, s)
            }
        }

        fun glyph(cx: Double, cy: Double, tangent: Double, size: Double, id: Int, style: Style) {
            val tx = cos(tangent)
            val ty = sin(tangent)
            fun point(h: Double): Pair<Double, Double> {
                val x = (floor(h * 3) - 1) * 0.42
                val y = (floor(fract(h * 7) * 3) - 1) * 0.42
                return cx + (tx * x - ty * y) * size to cy + (ty * x + tx * y) * size
            }

            for (k in 0..3) {
                val h1 = hash(id.toDouble(), k * 7.3)
                val h2 = hash(id * 1.7, k * 3.1 + 9)
                val p = point(h1)
                val q = if (k == 3 && h1 < 0.5) p else point(h2)
                segment(p.first, p.second, q.first, q.second, style.copy(rune = true))
            }
        }

        fun runeBand(inner: Double, outer: Double, glyph: Style, frame: Style, seed: Int) {
            val width = outer - inner
            val middle = (inner + outer) / 2
            val count = (TAU * middle / width).roundToInt()
            ring(0.0, 0.0, inner + 0.12 * width, frame)
            ring(0.0, 0.0, inner + 0.88 * width, frame)

            for (i in 0 until count) {
                val a = i * TAU / count
                glyph(cos(a) * middle, sin(a) * middle, a + PI / 2, width * 0.62, i + seed, glyph)
            }
        }

        fun planet(index: Int, cx: Double, cy: Double, angle: Double, s: Double, style: Style) {
            val ux = cos(angle)
            val uy = sin(angle)
            fun p(a: Double, b: Double) = cx + ux * a - uy * b to cy + uy * a + ux * b
            fun line(a: Double, b: Double, c: Double, d: Double) {
                val v = p(a * s, b * s)
                val w = p(c * s, d * s)
                segment(v.first, v.second, w.first, w.second, style)
            }

            fun ringAt(a: Double, b: Double, r: Double) {
                val c = p(a * s, b * s)
                ring(c.first, c.second, r * s, style)
            }

            fun arcAt(a: Double, b: Double, r: Double, from: Double, to: Double) {
                val c = p(a * s, b * s)
                arc(c.first, c.second, r * s, angle + from, angle + to, style)
            }

            when (index) {
                0 -> { // Jupiter
                    arcAt(0.35, -0.3, 0.4, -PI, 0.0)
                    line(-0.25, -0.75, -0.25, 0.7)
                    line(0.85, 0.35, -0.9, 0.35)
                }
                1 -> { // Saturn
                    line(0.95, -0.2, -0.25, -0.2)
                    line(0.55, -0.55, 0.55, 0.15)
                    arcAt(-0.25, 0.15, 0.35, PI / 2, 3 * PI / 2)
                    line(-0.25, 0.5, -0.75, 0.35)
                }
                2 -> { // Mercury
                    ringAt(0.1, 0.0, 0.42)
                    arcAt(0.62, 0.0, 0.36, PI + 0.5, TAU - 0.5)
                    line(-0.32, 0.0, -1.0, 0.0)
                    line(-0.7, -0.3, -0.7, 0.3)
                }
                3 -> { // Venus
                    ringAt(0.28, 0.0, 0.42)
                    line(-0.14, 0.0, -1.0, 0.0)
                    line(-0.6, -0.32, -0.6, 0.32)
                }
                4 -> { // Moon
                    arc(cx, cy, s, angle + PI / 2 + PI / 3, angle + PI / 2 + 5 * PI / 3, style)
                    arcAt(0.0, 0.45, 0.8675, PI / 2 + 86.7 * DEG, PI / 2 + TAU - 86.7 * DEG)
                }
                5 -> { // Mars
                    ringAt(-0.22, -0.22, 0.48)
                    line(0.12, 0.12, 0.78, 0.78)
                    line(0.78, 0.78, 0.3, 0.78)
                    line(0.78, 0.78, 0.78, 0.3)
                }
            }
        }
    }

    private fun build(): Design {
        val b = Builder()
        val random = Random(7)
        val noise = RingNoise(11)
        b.ring(0.0, 0.0, 10.7, Style())
        b.ring(0.0, 0.0, 11.4, Style(width = 0, secondary = true, intensity = 0.8f))

        for (i in 0 until 240) {
            val angle = (i + (random.next() - 0.5) * 0.6) * TAU / 240
            val n = noise.sample(angle)
            // Fill the prototype's 10-degree gap from a separate stream so the shared stream, and every other fibre,
            // stays unchanged.
            val fibreRandom = if (n < 0.2 && random.next() < 0.85) Random(0xE1E + i) else random
            val r0 = 11.9 + fibreRandom.next() * 0.7
            val r1 = 13.4 + 2.6 * n.pow(1.2) * (0.75 + 0.5 * fibreRandom.next())
            val bold = n > 0.62 && fibreRandom.next() < 0.45
            b.segment(
                cos(angle) * r0,
                sin(angle) * r0,
                cos(angle) * r1,
                sin(angle) * r1,
                Style(
                    width = if (bold) 1 else 0,
                    secondary = fibreRandom.next() < 0.25,
                    intensity = if (bold) 1.1f else 1.3f,
                    endIntensity = 0.2f
                )
            )
        }

        b.polygon(
            List(96) { i ->
                val a = i * TAU / 96
                val r = 15 + (if (i % 2 == 1) 0.7 else -0.7) * (0.6 + 0.4 * noise.sample(a * 3 + 1)) + 0.8 * (noise.sample(a) - 0.5)
                cos(a) * r to sin(a) * r
            },
            Style(intensity = 0.85f)
        )
        val inner = b.items.size
        b.runeBand(26.6, 30.6, Style(0, true, 1, intensity = 0.95f), Style(0, group = 1, intensity = 0.8f), 900)

        for (offset in listOf(90, 30)) {
            b.polygon(
                List(3) { k ->
                    val a = (offset + 120 * k) * DEG
                    cos(a) * 37.9 to sin(a) * 37.9
                },
                Style(group = 2)
            )
        }

        for (k in 0 until 6) {
            val a = (30 + 60 * k) * DEG
            b.ring(cos(a) * 34, sin(a) * 34, 1.55, Style(0, group = 2, intensity = 0.9f))
            b.planet(k, cos(a) * 34, sin(a) * 34, a, 1.1, Style(0, true, 2, intensity = 1.1f))
        }

        for (k in 0 until 6) {
            val a = (30 + 60 * k) * DEG
            val x = cos(a) * 41.2
            val y = sin(a) * 41.2
            b.arc(0.0, 0.0, 41.2, a + 4.6 * DEG, a + 60 * DEG - 4.6 * DEG, Style(2, group = 2, flow = true))
            b.push(2, doubleArrayOf(x, y, a + PI / 2, 2.8), doubleArrayOf(1.25, 0.95, 0.17, 0.0), Style(group = 2))
            b.ring(x, y, 3.25, Style(0, true, 2, intensity = 0.8f))
            b.beams.add(Beam(x.toFloat(), y.toFloat(), 2))
        }

        b.ring(0.0, 0.0, 42.4, Style(0, true, 2, intensity = 0.7f))
        return Design(b.items.toList(), b.beams.toList(), inner)
    }

    private class Random(private var state: Int) {
        fun next(): Double {
            state += 0x6D2B79F5
            var t = (state xor (state ushr 15)) * (1 or state)
            t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
            return ((t xor (t ushr 14)).toLong() and 0xffffffffL) / 4294967296.0
        }
    }

    private class RingNoise(seed: Int) {
        private val frequencies = intArrayOf(3, 5, 7, 11, 17, 23)
        private val phases = Random(seed).let { random -> DoubleArray(6) { random.next() * TAU } }
        private val amplitudes = DoubleArray(6) { 1.0 / (1.0 + it * 0.55) }
        private val sum = amplitudes.sum()

        fun sample(angle: Double): Double =
            0.5 + 0.5 * frequencies.indices.sumOf { amplitudes[it] * sin(frequencies[it] * angle + phases[it]) } / sum
    }

    private fun fract(value: Double) = value - floor(value)

    private fun hash(x: Double, y: Double): Double {
        var px = fract(x * 123.34)
        var py = fract(y * 456.21)
        val d = px * (px + 45.32) + py * (py + 45.32)
        px += d
        py += d

        return fract(px * py)
    }
}