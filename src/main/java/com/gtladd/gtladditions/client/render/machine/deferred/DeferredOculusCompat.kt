package com.gtladd.gtladditions.client.render.machine.deferred

import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristIrisPipelineBridge
import com.gtladd.gtladditions.mixin.oculus.DepthColorStorageAccessor
import com.mojang.blaze3d.systems.RenderSystem
import net.irisshaders.iris.Iris
import net.irisshaders.iris.gl.IrisRenderSystem
import net.irisshaders.iris.gl.blending.BlendModeStorage
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline
import net.minecraft.client.Minecraft
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL20C

@OnlyIn(Dist.CLIENT)
object DeferredOculusCompat {
    private const val IRIS_RENDER_TARGET_LIMIT = 16
    private val maxDrawBuffers = IntArray(1)
    private var directMainTargetDepth = 0

    fun withDeferredShaderPass(draw: () -> Unit) {
        beginDeferredShaderPass()
        try {
            draw()
        } finally {
            endDeferredShaderPass()
        }
    }

    private fun beginDeferredShaderPass() {
        if (directMainTargetDepth > 0) {
            Minecraft.getInstance().mainRenderTarget.bindWrite(false)
            unlockDepthColorForDeferredShader()
            return
        }

        val pipeline = Iris.getPipelineManager().pipelineNullable
        if (shouldUseShaderpackCompatPath() && pipeline is AntichristIrisPipelineBridge) {
            pipeline.beginAntichristFallbackTarget()
        } else {
            Minecraft.getInstance().mainRenderTarget.bindWrite(false)
        }
        unlockDepthColorForDeferredShader()
    }

    private fun endDeferredShaderPass() {
        if (directMainTargetDepth > 0) {
            Minecraft.getInstance().mainRenderTarget.bindWrite(false)
            return
        }

        val pipeline = Iris.getPipelineManager().pipelineNullable
        if (shouldUseShaderpackCompatPath() && pipeline is AntichristIrisPipelineBridge) {
            pipeline.endAntichristFallbackTarget()
        } else {
            Minecraft.getInstance().mainRenderTarget.bindWrite(false)
        }
    }

    fun beginDirectMainTargetPass() {
        directMainTargetDepth++
        Minecraft.getInstance().mainRenderTarget.bindWrite(false)
        resetIndependentBufferBlendState()
        unlockDepthColorForDeferredShader()
    }

    fun endDirectMainTargetPass() {
        if (directMainTargetDepth > 0) {
            directMainTargetDepth--
        }
        Minecraft.getInstance().mainRenderTarget.bindWrite(false)
    }

    fun shouldRenderAfterShaderpackFinal(): Boolean = shouldUseShaderpackCompatPath()

    private fun unlockDepthColorForDeferredShader() {
        if (DepthColorStorageAccessor.isDepthColorLocked()) {
            DepthColorStorageAccessor.unlockDepthColor()
        }
    }

    private fun resetIndependentBufferBlendState() {
        if (BlendModeStorage.isBlendLocked()) {
            BlendModeStorage.restoreBlend()
        }
        RenderSystem.defaultBlendFunc()
        if (!IrisRenderSystem.supportsBufferBlending()) return

        IrisRenderSystem.getIntegerv(GL20C.GL_MAX_DRAW_BUFFERS, maxDrawBuffers)
        val bufferCount = maxDrawBuffers[0].coerceAtMost(IRIS_RENDER_TARGET_LIMIT)
        for (buffer in 0 until bufferCount) {
            IrisRenderSystem.disableBufferBlend(buffer)
            IrisRenderSystem.blendFuncSeparatei(
                buffer,
                GL11C.GL_SRC_ALPHA,
                GL11C.GL_ONE_MINUS_SRC_ALPHA,
                GL11C.GL_ONE,
                GL11C.GL_ZERO
            )
        }
    }

    private fun shouldUseShaderpackCompatPath(): Boolean {
        val pipeline = Iris.getPipelineManager().pipelineNullable
        return pipeline is AntichristIrisPipelineBridge &&
            pipeline is ShaderRenderingPipeline &&
            pipeline.shouldOverrideShaders()
    }
}