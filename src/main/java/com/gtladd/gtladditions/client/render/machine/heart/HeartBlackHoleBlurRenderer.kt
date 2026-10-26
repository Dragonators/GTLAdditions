package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.ShaderInstance
import org.joml.Matrix4f

internal object HeartBlackHoleBlurRenderer {
    private val IDENTITY_MATRIX = Matrix4f()

    @JvmOverloads
    fun render(
        input: RenderTarget,
        blurred: RenderTarget,
        scratch: RenderTarget,
        downsampleShader: ShaderInstance,
        blurShader: ShaderInstance,
        brightnessThreshold: Float = -1.0f,
        gaussianRadius: Int = 0
    ): Boolean {
        val inputStepX = 0.25f / blurred.viewWidth.coerceAtLeast(1)
        val inputStepY = 0.25f / blurred.viewHeight.coerceAtLeast(1)
        downsampleShader.getUniform("BrightnessThreshold")?.set(brightnessThreshold)
        blurShader.getUniform("KernelRadius")?.set(gaussianRadius)
        if (!renderFilter(input, blurred, downsampleShader, inputStepX, inputStepY)) return false

        val blurStepX = 1.0f / blurred.viewWidth.coerceAtLeast(1)
        val blurStepY = 1.0f / scratch.viewHeight.coerceAtLeast(1)
        return renderFilter(blurred, scratch, blurShader, blurStepX, 0.0f) &&
            renderFilter(scratch, blurred, blurShader, 0.0f, blurStepY)
    }

    fun renderBloom(
        input: RenderTarget,
        levels: List<HeartBlackHoleBloomLevel>,
        downsampleShader: ShaderInstance,
        blurShader: ShaderInstance
    ): Boolean {
        var source = input
        // Three.js r180 BloomNode: five Gaussian radii, zero threshold, radius setting zero.
        levels.forEachIndexed { index, level ->
            val threshold = if (index == 0) 0.0f else -1.0f
            if (!render(source, level.blurred, level.scratch, downsampleShader, blurShader, threshold, 6 + index * 4)) return false
            source = level.blurred
        }

        return true
    }

    private fun renderFilter(
        input: RenderTarget,
        output: RenderTarget,
        shader: ShaderInstance,
        texelStepX: Float,
        texelStepY: Float
    ): Boolean {
        if (!HeartBlackHoleFramebuffer.clearColor(output)) return false
        RenderSystem.disableBlend()
        RenderSystem.disableDepthTest()
        RenderSystem.disableCull()
        RenderSystem.setShaderTexture(0, input.colorTextureId)
        shader.getUniform("TexelStep")?.set(texelStepX, texelStepY)

        try {
            RenderUtils.withBoundBuffer(HeartBlackHoleQuadBuffers.screenQuadBuffer) {
                HeartBlackHoleQuadBuffers.screenQuadBuffer.drawWithShader(IDENTITY_MATRIX, IDENTITY_MATRIX, shader)
            }
            return true
        } finally {
            shader.clear()
        }
    }
}