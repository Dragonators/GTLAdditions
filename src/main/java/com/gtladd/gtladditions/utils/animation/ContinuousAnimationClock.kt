package com.gtladd.gtladditions.utils.animation

class ContinuousAnimationClock {
    private var elapsed = 0.0
    private var lastTime = 0.0
    private var running = false

    fun restore(elapsedTicks: Double, now: Double, isRunning: Boolean) {
        elapsed = elapsedTicks.coerceAtLeast(0.0)
        lastTime = now
        running = isRunning
    }

    fun sample(now: Double): Double = elapsed + if (running) maxOf(0.0, now - lastTime) else 0.0

    fun rebase(previousTime: Double, newTime: Double) {
        elapsed = sample(previousTime)
        lastTime = newTime
    }

    fun setRunning(isRunning: Boolean, now: Double) {
        elapsed = sample(now)
        lastTime = maxOf(now, lastTime)
        running = isRunning
    }
}
