package com.gtladd.gtladditions.client.render.scene

import net.minecraft.client.renderer.ShaderInstance
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
interface IrisPipelineBridge {
    val foaBatchShader: ShaderInstance?

    /** depthtex1: copied before translucents, after the solid hand. */
    val opaqueDepthTexture: Int
}
