package com.gtladd.gtladditions.client.render.machine.nexus

import com.gtladd.gtladditions.client.RenderUtils.rotationRadians
import kotlin.math.floor

/** Keep the screen's short glitch intervals precise before converting to GPU floats. */
internal object NexusScreenClock {
    const val SIZE = 18

    fun sample(time: Double, seed: Double, values: FloatArray) {
        val cycle = time * 0.3 + seed * 3
        values[0] = ((cycle - floor(cycle)) / 0.3).toFloat()
        values[1] = (floor(cycle) % 1024).toFloat()
        values[2] = (floor(time * 6) % 1024).toFloat()
        values[3] = (floor(time / 1.1) % 1024).toFloat()
        values[4] = (floor(time / 1.8 + 3) % 1024).toFloat()

        // Strip and flicker ids advance only at their original, discrete frame boundaries.
        values[5] = (floor(time * 11) % 1024).toFloat()
        values[6] = (floor(time * 13) % 1024).toFloat()
        values[7] = (floor(time * 5) % 1024).toFloat()
        values[8] = (floor(time * 17) % 1024).toFloat()
        values[9] = (floor(time * 2.7) % 1024).toFloat()

        values[10] = rotationRadians(time * 0.23)
        values[11] = rotationRadians(time * 6)
        values[12] = rotationRadians(time * 0.04)
        // Burst, two stellar noise layers, gold edge and rising sparks use a 1024-cell lattice.
        values[13] = (time * 0.85 % 1024.0).toFloat()
        values[14] = (time * 0.06 % 1024.0).toFloat()
        values[15] = (-time * 0.04 % 1024.0).toFloat()
        values[16] = (time * 4 % 1024.0).toFloat()
        values[17] = (time * 0.35 % 1024.0).toFloat()
    }
}
