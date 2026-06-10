package com.gtladd.gtladditions.client.render.machine.antichrist

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
object AntichristOculusCompat {
    private const val IRIS_RENDER_TARGET_LIMIT = 16
    private val maxDrawBuffers = IntArray(1)
    private var directMainTargetDepth = 0

    fun withAntichristShaderPass(draw: () -> Unit) {
        beginAntichristShaderPass()
        try {
            draw()
        } finally {
            endAntichristShaderPass()
        }
    }

    private fun beginAntichristShaderPass() {
        if (directMainTargetDepth > 0) {
            Minecraft.getInstance().mainRenderTarget.bindWrite(false)
            unlockDepthColorForAntichristShader()
            return
        }

        val pipeline = Iris.getPipelineManager().pipelineNullable
        if (pipeline is AntichristIrisPipelineBridge) {
            pipeline.beginAntichristFallbackTarget()
        } else {
            Minecraft.getInstance().mainRenderTarget.bindWrite(false)
        }
        unlockDepthColorForAntichristShader()
    }

    private fun endAntichristShaderPass() {
        if (directMainTargetDepth > 0) {
            Minecraft.getInstance().mainRenderTarget.bindWrite(false)
            return
        }

        val pipeline = Iris.getPipelineManager().pipelineNullable
        if (pipeline is AntichristIrisPipelineBridge) {
            pipeline.endAntichristFallbackTarget()
        } else {
            Minecraft.getInstance().mainRenderTarget.bindWrite(false)
        }
    }

    fun beginDirectMainTargetPass() {
        directMainTargetDepth++
        Minecraft.getInstance().mainRenderTarget.bindWrite(false)
        resetIndependentBufferBlendState()
        unlockDepthColorForAntichristShader()
    }

    fun endDirectMainTargetPass() {
        if (directMainTargetDepth > 0) {
            directMainTargetDepth--
        }
        Minecraft.getInstance().mainRenderTarget.bindWrite(false)
    }

    fun shouldRenderAfterShaderpackFinal(): Boolean = shouldUseShaderpackCompatPath()

    private fun unlockDepthColorForAntichristShader() {
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

        // Photon weather uses per-buffer premultiplied alpha on colortex targets;
        // FOA renders after final, so scrub indexed blend state before drawing to main.
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