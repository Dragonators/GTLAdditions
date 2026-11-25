package com.gtladd.gtladditions.client.render.machine.matrix

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class MatrixEyeAnimation {
    data class Frame(
        val time: Double,
        val open: Float,
        val dilate: Float,
        val light: Float,
        val seam: Float,
        val shroud: Float,
        val stir: Float,
        val swirl: Float,
        val rise: Float,
        val gazeX: Float,
        val gazeY: Float,
        val sealReveal: Float,
        val sealFront: Float
    ) {
        val upperLid: Float get() = 1.62f + (-0.08f - 1.62f) * open
        val lowerLid: Float get() = (PI.toFloat() - 1.62f + 0.14f).let { it + (-0.08f - it) * open.pow(1.25f) }
    }

    private data class Curves(
        val open: Double,
        val dilate: Double,
        val light: Double,
        val seam: Double,
        val shroud: Double,
        val stir: Double,
        val reveal: Double,
        val front: Double
    ) {
        fun mix(other: Curves, k: Double) = Curves(
            open + (other.open - open) * k,
            dilate + (other.dilate - dilate) * k,
            light + (other.light - light) * k,
            seam + (other.seam - seam) * k,
            shroud + (other.shroud - shroud) * k,
            stir + (other.stir - stir) * k,
            reveal + (other.reveal - reveal) * k,
            front + (other.front - front) * k
        )
    }

    private var lastTime = Double.NaN
    private var lastSmoothAnimations = false
    private var elapsed = 0.0
    private var progress = 0.0
    private var running = false
    private var switchedAt = -1e9
    private var from: Curves? = null
    private var blinkAt = 9.0
    private var blinkStart = -1e9
    private var swirl = 0.0
    private var rise = 0.0

    fun sample(seconds: Double, active: Boolean, smoothAnimations: Boolean): Frame {
        require(seconds.isFinite())
        if (lastTime.isNaN() || lastSmoothAnimations != smoothAnimations) {
            // Render and machine clocks have unrelated origins; preserve the eye's local phase when switching.
            lastTime = seconds
        }

        lastSmoothAnimations = smoothAnimations
        val dt = (seconds - lastTime).coerceAtLeast(0.0)
        lastTime = maxOf(lastTime, seconds)
        elapsed += dt
        progress = (progress + dt * if (running) 1.0 / OPEN_SECONDS else -1.0 / CLOSE_SECONDS).coerceIn(0.0, 1.0)
        if (active != running) {
            from = current()
            running = active
            switchedAt = elapsed
            blinkStart = -1e9
            if (active) blinkAt = elapsed + 9.0
        }

        if (running && progress >= 1.0 && elapsed >= blinkAt) {
            blinkStart = elapsed
            blinkAt = elapsed + 7.0 + 7.0 * hash(floor(elapsed))
        }

        val c = current()
        swirl += dt * (0.035 + 0.30 * c.stir)
        rise += dt * (0.10 + 0.60 * c.stir)
        val gaze = gaze(elapsed)
        return Frame(
            elapsed,
            c.open.toFloat(),
            (c.dilate * (1.0 + 0.08 * sin(elapsed * 0.53))).toFloat(),
            c.light.toFloat(),
            (c.seam * (0.72 + 0.28 * sin(elapsed * 0.9))).toFloat(),
            c.shroud.toFloat(),
            c.stir.toFloat(),
            swirl.toFloat(),
            rise.toFloat(),
            gaze.first,
            gaze.second,
            c.reveal.toFloat(),
            c.front.toFloat()
        )
    }

    private fun current(): Curves {
        val target = if (running) {
            val t = progress * OPEN_SECONDS
            Curves(
                0.07 * bump((t - 0.4) / 0.45) + smooth((t - 0.95) / 1.65),
                1.0 + 1.2 * (1.0 - smooth((t - 1.7) / 1.3)),
                0.12 + 0.88 * smooth((t - 0.8) / 1.8),
                if (t < 1.0) 0.35 + 0.65 * smooth(t / 0.9) else 1.0 - smooth((t - 1.0) / 0.9),
                1.0 - smooth((t - 0.5) / 2.3),
                bump((t - 0.3) / 2.6),
                9.0 + 36.0 * smooth((t - 1.0) / 1.9),
                0.9 * bump((t - 1.0) / 1.9)
            )
        } else {
            val t = (1.0 - progress) * CLOSE_SECONDS
            Curves(
                1.0 - smooth(t / 1.4),
                1.0 + 0.5 * smooth(t / 1.2),
                1.0 - 0.88 * smooth((t - 0.1) / 1.5),
                0.35 * smooth((t - 1.1) / 0.6),
                smooth((t - 0.2) / 1.6),
                0.6 * bump(t / 1.8),
                45.0 - 36.0 * smooth(t / 1.5),
                0.0
            )
        }

        val c = from?.mix(target, smooth((elapsed - switchedAt) / 0.4)) ?: target
        val x = (elapsed - blinkStart) / 0.38
        val blink = if (x !in 0.0..1.0) {
            0.0
        } else if (x < 0.4) {
            smooth(x / 0.4)
        } else {
            1.0 - smooth((x - 0.4) / 0.6)
        }

        return c.copy(open = (c.open * (1.0 - blink)).coerceIn(0.0, 1.0))
    }

    private fun gaze(t: Double): Pair<Float, Float> {
        val k = floor(t / 3.7)
        fun target(i: Double): Pair<Double, Double> {
            val a = hash(i) * 6.2832
            val r = sqrt(hash(i + 0.5)) * 0.05
            return r * cos(a) to r * sin(a)
        }

        val p = target(k - 1.0)
        val q = target(k)
        val s = smooth((t - k * 3.7) / 0.22)
        return (p.first + (q.first - p.first) * s + 0.002 * sin(t * 13.1)).toFloat() to
            (p.second + (q.second - p.second) * s + 0.002 * sin(t * 11.7)).toFloat()
    }

    companion object {
        const val OPEN_SECONDS = 3.2
        const val CLOSE_SECONDS = 1.8

        private fun smooth(value: Double): Double = value.coerceIn(0.0, 1.0).let { it * it * (3.0 - 2.0 * it) }

        private fun bump(value: Double): Double = if (value <= 0.0 || value >= 1.0) 0.0 else sin(PI * value)

        private fun hash(value: Double): Double = (sin(value * 127.1 + 311.7) * 43758.5453).let { it - floor(it) }
    }
}
