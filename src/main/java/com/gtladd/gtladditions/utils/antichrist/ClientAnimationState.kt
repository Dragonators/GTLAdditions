package com.gtladd.gtladditions.utils.antichrist

import com.gtladd.gtladditions.common.machine.trait.StarRitualTrait.Companion.CLIENT_COLLAPSE_DURATION_TICKS
import com.gtladd.gtladditions.common.machine.trait.StarRitualTrait.Companion.CLIENT_RECOVER_DURATION_TICKS
import com.gtladd.gtladditions.utils.RenderUtils
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.pow

@OnlyIn(Dist.CLIENT)
class ClientAnimationState() {
    private val collapseTimer = RenderUtils.SmoothAnimationTimer()
    private val recoverTimer = RenderUtils.SmoothAnimationTimer()
    private var isCollapsing: Boolean = false

    fun onStateChanged(newValue: Boolean, oldValue: Boolean) {
        if (newValue && !oldValue) {
            isCollapsing = true
            collapseTimer.reset()
        } else if (!newValue && oldValue) {
            isCollapsing = false
            recoverTimer.reset()
        }
    }

    fun getProgress(): Float {
        return if (isCollapsing) {
            val durationMillis = (CLIENT_COLLAPSE_DURATION_TICKS * 50).toLong()
            val rawProgress = collapseTimer.getProgress(durationMillis)
            smoothStep(rawProgress)
        } else {
            val durationMillis = (CLIENT_RECOVER_DURATION_TICKS * 50).toLong()
            val rawProgress = recoverTimer.getProgress(durationMillis)
            1.0f - smoothStep(rawProgress)
        }
    }

    fun getRenderColor(baseColor: Int): Int {
        val currentProgress = getProgress()

        if (currentProgress <= 0.001f) {
            return baseColor
        }

        val r = (baseColor shr 16) and 0xFF
        val g = (baseColor shr 8) and 0xFF
        val b = baseColor and 0xFF

        val newR = (r * (1f - currentProgress)).toInt().coerceIn(0, 255)
        val newG = (g * (1f - currentProgress)).toInt().coerceIn(0, 255)
        val newB = (b * (1f - currentProgress)).toInt().coerceIn(0, 255)

        return (0xFF shl 24) or (newR shl 16) or (newG shl 8) or newB
    }

    fun getRenderRadius(baseRadius: Float): Float {
        val currentProgress = getProgress()
        val smoothProgress = easeInOutCubic(currentProgress)
        return baseRadius * (1.0f - smoothProgress * 0.98f)
    }

    private fun smoothStep(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }

    private fun easeInOutCubic(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return if (x < 0.5f) {
            4f * x * x * x
        } else {
            1f - (-2f * x + 2f).pow(3) / 2f
        }
    }
}