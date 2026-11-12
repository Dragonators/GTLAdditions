package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.client.RenderUtils
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.ShaderInstance

internal object HeartBlackHoleBlurRenderer {
    @JvmOverloads
    fun render(
        input: RenderTarget,
        blurred: RenderTarget,
        scratch: RenderTarget,
        downsampleShader: ShaderInstance,
        blurShader: ShaderInstance,
        brightnessThreshold: Float = -1.0f,
        gaussianRadius: Int = 0,
        inputBounds: IntArray? = null
    ): Boolean {
        val inputStepX = 0.25f / blurred.viewWidth.coerceAtLeast(1)
        val inputStepY = 0.25f / blurred.viewHeight.coerceAtLeast(1)
        downsampleShader.getUniform("BrightnessThreshold")?.set(brightnessThreshold)
        blurShader.getUniform("KernelRadius")?.set(gaussianRadius)
        val downBounds = filterBounds(inputBounds, input, blurred, 2, 2)
        if (!renderFilter(input, blurred, downsampleShader, inputStepX, inputStepY, downBounds)) return false

        val blurStepX = 1.0f / blurred.viewWidth.coerceAtLeast(1)
        val blurStepY = 1.0f / scratch.viewHeight.coerceAtLeast(1)
        val support = maxOf(4, gaussianRadius) + 1
        val horizontal = filterBounds(downBounds, blurred, scratch, support, 1)
        val vertical = filterBounds(horizontal, scratch, blurred, 1, support)

        return renderFilter(blurred, scratch, blurShader, blurStepX, 0.0f, horizontal) &&
            renderFilter(scratch, blurred, blurShader, 0.0f, blurStepY, vertical)
    }

    fun renderBloom(
        input: RenderTarget,
        levels: List<HeartBlackHoleBloomLevel>,
        downsampleShader: ShaderInstance,
        blurShader: ShaderInstance,
        inputBounds: IntArray? = null
    ): Boolean {
        var source = input
        var bounds = inputBounds
        // Three.js r180 BloomNode: five Gaussian radii, zero threshold, radius setting zero.
        levels.forEachIndexed { index, level ->
            val threshold = if (index == 0) 0.0f else -1.0f
            val radius = 6 + index * 4
            if (!render(source, level.blurred, level.scratch, downsampleShader, blurShader, threshold, radius, bounds)) return false
            bounds = filterBounds(bounds, source, level.blurred, 2, 2)
            bounds = filterBounds(bounds, level.blurred, level.scratch, radius + 1, 1)
            bounds = filterBounds(bounds, level.scratch, level.blurred, 1, radius + 1)
            source = level.blurred
        }

        return true
    }

    /** Viewports/UVs stay full-size; cleared pixels outside this rectangle remain exactly zero. */
    private fun filterBounds(bounds: IntArray?, input: RenderTarget, output: RenderTarget, padX: Int, padY: Int): IntArray? {
        bounds ?: return null
        if (bounds[2] <= 0 || bounds[3] <= 0) return intArrayOf(0, 0, 0, 0)

        val scaleX = output.viewWidth.toDouble() / input.viewWidth
        val scaleY = output.viewHeight.toDouble() / input.viewHeight
        val x0 = (kotlin.math.floor(bounds[0] * scaleX).toInt() - padX).coerceIn(0, output.viewWidth)
        val y0 = (kotlin.math.floor(bounds[1] * scaleY).toInt() - padY).coerceIn(0, output.viewHeight)
        val x1 = (kotlin.math.ceil((bounds[0] + bounds[2]) * scaleX).toInt() + padX).coerceIn(0, output.viewWidth)
        val y1 = (kotlin.math.ceil((bounds[1] + bounds[3]) * scaleY).toInt() + padY).coerceIn(0, output.viewHeight)

        return intArrayOf(x0, y0, x1 - x0, y1 - y0)
    }

    private fun renderFilter(
        input: RenderTarget,
        output: RenderTarget,
        shader: ShaderInstance,
        texelStepX: Float,
        texelStepY: Float,
        bounds: IntArray?
    ): Boolean {
        if (!HeartBlackHoleFramebuffer.clearColor(output)) return false
        RenderSystem.disableBlend()
        RenderSystem.disableDepthTest()
        RenderSystem.disableCull()
        RenderSystem.setShaderTexture(0, input.colorTextureId)
        shader.getUniform("TexelStep")?.set(texelStepX, texelStepY)
        if (bounds != null) RenderSystem.enableScissor(bounds[0], bounds[1], bounds[2], bounds[3])

        try {
            RenderUtils.withBoundBuffer(HeartBlackHoleQuadBuffers.screenQuadBuffer) {
                shader.setSampler("Sampler0", input.colorTextureId)
                shader.apply()
                HeartBlackHoleQuadBuffers.screenQuadBuffer.draw()
            }
            return true
        } finally {
            RenderSystem.disableScissor()
            shader.clear()
        }
    }
}