package com.gtladd.gtladditions.client.render.machine.deferred

import net.minecraft.client.renderer.ShaderInstance
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
interface DeferredIrisPipelineBridge {
    val foaBatchShader: ShaderInstance?

    fun beginDeferredTarget()

    fun endDeferredTarget()
}