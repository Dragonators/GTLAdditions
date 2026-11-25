package com.gtladd.gtladditions.client.render.machine.heart

import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
class HeartMotion {
    var power = 0.0
        private set
    val angles = DoubleArray(4)
    var time = 0.0
        private set
    var orbitTime = 0.0
        private set
    private var last = Double.NaN
    private var lastSmoothAnimations = false

    val ease: Double
        get() = power * power * (3 - 2 * power)

    fun advance(seconds: Double, working: Boolean, smoothAnimations: Boolean) {
        if (last.isNaN()) {
            power = if (working) 1.0 else 0.0
            lastSmoothAnimations = smoothAnimations
            last = seconds
            time = seconds
            orbitTime = seconds
            return
        }

        if (lastSmoothAnimations != smoothAnimations) {
            lastSmoothAnimations = smoothAnimations
            last = seconds
        }

        val dt = (seconds - last).coerceIn(0.0, 0.25)
        last = maxOf(last, seconds)
        if (dt == 0.0) return
        val step = dt / RAMP
        power += ((if (working) 1.0 else 0.0) - power).coerceIn(-step, step)
        val ease = ease
        time += dt
        orbitTime += dt * (0.25 + 0.75 * ease)
        for (i in angles.indices) angles[i] = (angles[i] + TURN[i] * dt * ease) % (2 * Math.PI)
    }

    companion object {
        const val RAMP = 9.0

        /** Radians a second, inner to outer: one turn in 60 / 75 / 100 / 150 s, neighbours opposite. */
        val TURN = doubleArrayOf(0.10472, -0.083776, 0.062832, -0.041888)
    }
}
