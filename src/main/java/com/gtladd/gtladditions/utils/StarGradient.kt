package com.gtladd.gtladditions.utils

import kotlin.math.pow
import kotlin.math.roundToInt

object StarGradient {
    fun getRGBFromTime(ratio: Double): Int {
        val orange = 0xFF8C2A
        val blueWh = 0xBFD8FF
        val white = 0xFFFFFF

        val result: Int
        if (ratio <= 0.5) {
            val p = ratio / 0.5
            result = lerpSRGB(orange, blueWh, p)
        } else {
            val p = (ratio - 0.5) / 0.5
            result = lerpSRGB(blueWh, white, p)
        }

        return result
    }

    private fun lerpSRGB(colorA: Int, colorB: Int, t: Double): Int {
        val rA = (colorA shr 16) and 0xFF
        val gA = (colorA shr 8) and 0xFF
        val bA = colorA and 0xFF

        val rB = (colorB shr 16) and 0xFF
        val gB = (colorB shr 8) and 0xFF
        val bB = colorB and 0xFF

        val lrA = sRGBToLinear(rA / 255.0)
        val lgA = sRGBToLinear(gA / 255.0)
        val lbA = sRGBToLinear(bA / 255.0)

        val lrB = sRGBToLinear(rB / 255.0)
        val lgB = sRGBToLinear(gB / 255.0)
        val lbB = sRGBToLinear(bB / 255.0)

        val lr = lerp(lrA, lrB, t)
        val lg = lerp(lgA, lgB, t)
        val lb = lerp(lbA, lbB, t)

        val r = (clamp01(linearToSRGB(lr)) * 255).roundToInt()
        val g = (clamp01(linearToSRGB(lg)) * 255).roundToInt()
        val b = (clamp01(linearToSRGB(lb)) * 255).roundToInt()

        return (r shl 16) or (g shl 8) or b
    }

    private fun lerp(x: Double, y: Double, t: Double): Double {
        return x + (y - x) * t
    }

    private fun sRGBToLinear(c: Double): Double {
        return if (c <= 0.04045) (c / 12.92) else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun linearToSRGB(c: Double): Double {
        return if (c <= 0.0031308) (c * 12.92) else (1.055 * c.pow(1.0 / 2.4) - 0.055)
    }

    private fun clamp01(x: Double): Double {
        return if (x < 0) 0.0 else (if (x > 1) 1.0 else x)
    }
}
