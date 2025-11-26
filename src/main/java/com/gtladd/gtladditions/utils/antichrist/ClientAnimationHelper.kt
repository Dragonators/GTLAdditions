package com.gtladd.gtladditions.utils.antichrist

import com.github.benmanes.caffeine.cache.Caffeine
import com.gtladd.gtladditions.common.machine.muiltblock.controller.ForgeOfTheAntichrist
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object ClientAnimationHelper {
    private val ANIMATION_STATES = Caffeine.newBuilder()
        .weakKeys()
        .build<ForgeOfTheAntichrist, ClientAnimationState>()

    private fun getOrCreate(machine: ForgeOfTheAntichrist): ClientAnimationState =
        ANIMATION_STATES.get(machine) { ClientAnimationState() }

    fun onCollapseStateChanged(machine: ForgeOfTheAntichrist, newValue: Boolean, oldValue: Boolean) {
        getOrCreate(machine).onStateChanged(newValue, oldValue)
    }

    fun getClientRenderColor(machine: ForgeOfTheAntichrist, baseColor: Int): Int =
        getOrCreate(machine).getRenderColor(baseColor)

    fun getClientRenderRadius(machine: ForgeOfTheAntichrist, baseRadius: Float): Float =
        getOrCreate(machine).getRenderRadius(baseRadius)

}