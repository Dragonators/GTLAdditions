package com.gtladd.gtladditions.client.render.machine.heart

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexBuffer
import net.minecraft.client.renderer.ShaderInstance
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL14C

internal object HeartBlackHoleCompositeRenderer {
    private val IDENTITY_MATRIX = Matrix4f()

    fun renderPendingComposite(width: Int, height: Int, pending: PendingHeartComposite) {
        val compositeShader = HeartBlackHoleShaders.compositeShader ?: return
        val previousState = PendingCompositeState.capture()

        try {
            RenderSystem.viewport(0, 0, width, height)
            HeartBlackHoleFramebuffer.prepareFramebufferWrite(false)
            RenderSystem.colorMask(true, true, true, false)
            RenderSystem.disableDepthTest()
            RenderSystem.depthMask(false)
            RenderSystem.disableCull()
            RenderSystem.enableBlend()
            RenderSystem.blendFuncSeparate(
                GL11C.GL_ONE,
                GL11C.GL_ONE_MINUS_SRC_ALPHA,
                GL11C.GL_ONE,
                GL11C.GL_ONE_MINUS_SRC_ALPHA
            )

            drawCompositeQuad(compositeShader, pending)
        } finally {
            VertexBuffer.unbind()
            compositeShader.clear()
            previousState.restore()
        }
    }

    private fun drawCompositeQuad(
        compositeShader: ShaderInstance,
        pending: PendingHeartComposite
    ) {
        compositeShader.setSampler("EffectSampler", pending.targets.effect.colorTextureId)
        compositeShader.setSampler("BlurredEffectSampler", pending.targets.effectBlurB.colorTextureId)
        compositeShader.setSampler("MaskSampler", pending.targets.mask.colorTextureId)
        compositeShader.setSampler("BlurredMaskSampler", pending.targets.maskBlurB.colorTextureId)
        compositeShader.getUniform("BlurInputScale")?.set(
            pending.width.toFloat() / pending.blurWidth.toFloat(),
            pending.height.toFloat() / pending.blurHeight.toFloat()
        )

        HeartBlackHoleQuadBuffers.screenQuadBuffer.bind()
        HeartBlackHoleQuadBuffers.screenQuadBuffer.drawWithShader(IDENTITY_MATRIX, IDENTITY_MATRIX, compositeShader)
    }

    @Suppress("ArrayInDataClass")
    private data class PendingCompositeState(
        val viewport: IntArray,
        val blendEnabled: Boolean,
        val depthTestEnabled: Boolean,
        val cullEnabled: Boolean,
        val depthMask: Boolean,
        val blendSrcRgb: Int,
        val blendDstRgb: Int,
        val blendSrcAlpha: Int,
        val blendDstAlpha: Int
    ) {
        fun restore() {
            RenderSystem.blendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha)
            if (blendEnabled) RenderSystem.enableBlend() else RenderSystem.disableBlend()
            if (depthTestEnabled) RenderSystem.enableDepthTest() else RenderSystem.disableDepthTest()
            if (cullEnabled) RenderSystem.enableCull() else RenderSystem.disableCull()
            RenderSystem.colorMask(true, true, true, true)
            RenderSystem.depthMask(depthMask)
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3])
        }

        companion object {
            fun capture(): PendingCompositeState {
                val viewport = IntArray(4)
                GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, viewport)
                return PendingCompositeState(
                    viewport,
                    GL11C.glIsEnabled(GL11C.GL_BLEND),
                    GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST),
                    GL11C.glIsEnabled(GL11C.GL_CULL_FACE),
                    GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK),
                    GlStateManager._getInteger(GL14C.GL_BLEND_SRC_RGB),
                    GlStateManager._getInteger(GL14C.GL_BLEND_DST_RGB),
                    GlStateManager._getInteger(GL14C.GL_BLEND_SRC_ALPHA),
                    GlStateManager._getInteger(GL14C.GL_BLEND_DST_ALPHA)
                )
            }
        }
    }
}

internal data class PendingHeartComposite(
    val targets: HeartBlackHoleTargets,
    val width: Int,
    val height: Int,
    val blurWidth: Int,
    val blurHeight: Int
)