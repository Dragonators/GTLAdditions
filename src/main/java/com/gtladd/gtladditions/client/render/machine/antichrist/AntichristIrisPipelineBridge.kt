package com.gtladd.gtladditions.client.render.machine.antichrist

import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
interface AntichristIrisPipelineBridge {
    fun beginAntichristFallbackTarget()

    fun endAntichristFallbackTarget()
}