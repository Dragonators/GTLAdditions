package com.gtladd.gtladditions.utils.animation

class ClientAnimationClock {
    private var clock: ContinuousAnimationClock? = null
    private var lastTick = Double.NEGATIVE_INFINITY
    private var smoothAnimations: Boolean? = null

    fun clear() {
        clock = null
        lastTick = Double.NEGATIVE_INFINITY
        smoothAnimations = null
    }

    fun start(elapsedTicks: Double, renderTick: Double, smooth: Boolean, running: Boolean) {
        clock = ContinuousAnimationClock().also { it.restore(elapsedTicks, sourceTime(renderTick, smooth), running) }
    }

    fun setRunning(running: Boolean, renderTick: Double, smooth: Boolean) {
        clock?.setRunning(running, sourceTime(renderTick, smooth))
    }

    fun sample(renderTick: Double, smooth: Boolean): Double? = clock?.sample(sourceTime(renderTick, smooth))

    private fun sourceTime(tick: Double, smooth: Boolean): Double {
        if (smoothAnimations != null && smoothAnimations != smooth) {
            // Both sources advance in ticks, but their origins are unrelated.
            clock?.rebase(lastTick, tick)
            lastTick = tick
        }

        smoothAnimations = smooth
        lastTick = maxOf(lastTick, tick)
        return lastTick
    }
}
