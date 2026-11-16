package com.gtladd.gtladditions.client.render.scene

import com.mojang.blaze3d.systems.RenderSystem
import net.irisshaders.iris.gl.IrisRenderSystem
import net.irisshaders.iris.gl.blending.BlendModeStorage
import net.irisshaders.iris.gl.blending.DepthColorStorage
import net.minecraft.client.Minecraft
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL20C

@OnlyIn(Dist.CLIENT)
object OculusCompat {
    private const val IRIS_RENDER_TARGET_LIMIT = 16
    private val maxDrawBuffers = IntArray(1)

    private fun bindOutput() = Minecraft.getInstance().mainRenderTarget.bindWrite(false)

    fun withDirectMainTargetPass(draw: () -> Unit) {
        bindOutput()
        resetIndependentBufferBlendState()
        unlockDepthColorIfLocked()
        try {
            draw()
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
            RenderSystem.colorMask(true, true, true, true)
            RenderSystem.depthMask(true)
            RenderSystem.disableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.enableCull()
            bindOutput()
        }
    }

    private fun unlockDepthColorIfLocked() {
        if (DepthColorStorage.isDepthColorLocked()) {
            DepthColorStorage.unlockDepthColor()
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
}