package com.gtladd.gtladditions.client.render.machine.lighthunter

/** Seconds, not ticks. */
object LightHunterParadoxTiming {
    const val DURATION = 6f
    const val WINDOW_START = 4.5f
    const val WINDOW_END = 5.8f
    const val ROTATION_DEGREES_PER_SECOND = 1.5f
    const val WINDOW_RADIUS = 12.2f
    const val WINDOW_X = LightHunterVfxGeometry.CHART_X
    const val WINDOW_DEPTH_BIAS = 0.02f
    const val WINDOW_Y = 6f
    const val WINDOW_Z = 12f

    fun smooth(start: Float, end: Float, value: Float): Float {
        val p = ((value - start) / (end - start)).coerceIn(0f, 1f)
        return p * p * (3f - 2f * p)
    }

    fun window(seconds: Float): Window {
        if (seconds < WINDOW_START) return Window(0f, 0f, 0f, 1f)
        if (seconds >= WINDOW_END) return Window(1f, 0f, 0f, 1f)
        val p = ((seconds - WINDOW_START) / (WINDOW_END - WINDOW_START)).coerceIn(0f, 1f)
        val reveal = when {
            p < 0.18f -> 0.9f
            p < 0.30f -> 0f
            p < 0.54f -> 1f
            p < 0.64f -> 0f
            else -> 1f
        }

        val glitch = when {
            p < 0.30f -> 1f
            p < 0.64f -> 0.72f
            else -> 0.5f * (1f - smooth(0.64f, 1f, p))
        }

        return Window(reveal, glitch, kotlin.math.floor((seconds - WINDOW_START) * 14f), if (reveal == 0f) 0.1f else 1f - 0.35f * glitch)
    }

    data class Window(val reveal: Float, val glitch: Float, val frame: Float, val rim: Float)
}