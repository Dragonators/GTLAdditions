package com.gtladd.gtladditions.client.render.machine.nexus

import kotlin.math.floor

/** Keep the screen's short glitch intervals precise before converting to GPU floats. */
internal object NexusScreenClock {
    fun sample(time: Double, seed: Double, values: FloatArray) {
        val cycle = time * 0.3 + seed * 3
        values[0] = ((cycle - floor(cycle)) / 0.3).toFloat()
        values[1] = (floor(cycle) % 1024).toFloat()
        values[2] = (floor(time * 6) % 1024).toFloat()
        values[3] = (floor(time / 1.1) % 1024).toFloat()
        values[4] = (floor(time / 1.8 + 3) % 1024).toFloat()
    }
}
