package com.gtladd.gtladditions.client.render.machine.lighthunter

import com.github.benmanes.caffeine.cache.Caffeine
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.common.machine.multiblock.controller.LightHunterSpaceStation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object LightHunterParadoxAnimation {
    private val animations = Caffeine.newBuilder().weakKeys().build<LightHunterSpaceStation, RenderUtils.SmoothAnimationTimer>()

    fun isUnlockTransition(newValue: Int, oldValue: Int): Boolean = oldValue == 0 && newValue == 1

    fun onStateChanged(machine: LightHunterSpaceStation, newValue: Int, oldValue: Int) {
        if (isUnlockTransition(newValue, oldValue)) {
            animations.put(machine, RenderUtils.SmoothAnimationTimer())
        } else if (newValue != 1) {
            animations.invalidate(machine)
        }
    }

    fun seconds(machine: LightHunterSpaceStation): Float =
        animations.getIfPresent(machine)?.getProgress(6000L)?.times(LightHunterParadoxTiming.DURATION) ?: LightHunterParadoxTiming.DURATION

    fun remove(machine: LightHunterSpaceStation) = animations.invalidate(machine)

    fun clear() = animations.invalidateAll()
}