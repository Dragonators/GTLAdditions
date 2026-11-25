package com.gtladd.gtladditions.client.render.machine.nexus

/** Reduce each animated coordinate in double precision, before uploading shader floats. */
internal object NexusBeamClock {
    const val NOISE_PERIOD = 1024.0

    private fun wrap(value: Double, period: Double): Double {
        val remainder = value % period
        return if (remainder < 0) remainder + period else remainder
    }

    fun flowPhase(time: Double, speed: Double, spacing: Double): Double = wrap(time * speed / spacing, 1.0)

    fun huePhase(time: Double): Double = wrap(time * 0.08, 1.0)

    fun noisePhases(time: Double, speed: Double, output: FloatArray) {
        for (layer in 0..1) {
            var y = -speed * time / if (layer == 0) 16.0 else 6.0
            var z = time * (if (layer == 0) 0.3 else 0.55) + if (layer == 0) 0.0 else 3.0

            for (octave in 0..2) {
                val index = (layer * 3 + octave) * 2
                output[index] = wrap(y, NOISE_PERIOD).toFloat()
                output[index + 1] = wrap(z, NOISE_PERIOD).toFloat()
                y = y * 2.07 + 1.1
                z = z * 2.07 + 7.7
            }
        }
    }
}
