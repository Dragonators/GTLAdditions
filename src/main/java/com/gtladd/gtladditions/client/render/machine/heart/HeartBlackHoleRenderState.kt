package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.mixin.minecraft.client.BlendModeAccessor
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexBuffer
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL13C
import org.lwjgl.opengl.GL14C
import org.lwjgl.opengl.GL15C
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL30C

internal class HeartBlackHoleRenderState private constructor() {
    private val readFramebuffer = integer(GL30C.GL_READ_FRAMEBUFFER_BINDING)
    private val drawFramebuffer = integer(GL30C.GL_DRAW_FRAMEBUFFER_BINDING)
    private val viewport = integers(GL11C.GL_VIEWPORT)
    private val scissorEnabled = GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST)
    private val scissorBox = integers(GL11C.GL_SCISSOR_BOX)
    private val stencilEnabled = GL11C.glIsEnabled(GL11C.GL_STENCIL_TEST)
    private val colorLogicEnabled = GL11C.glIsEnabled(GL11C.GL_COLOR_LOGIC_OP)
    private val colorMask = integers(GL11C.GL_COLOR_WRITEMASK)
    private val clearColor = FloatArray(4).also { GL11C.glGetFloatv(GL11C.GL_COLOR_CLEAR_VALUE, it) }
    private val depthTestEnabled = GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST)
    private val depthMask = GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK)
    private val depthFunction = integer(GL11C.GL_DEPTH_FUNC)
    private val cullEnabled = GL11C.glIsEnabled(GL11C.GL_CULL_FACE)
    private val cullFaceMode = integer(GL11C.GL_CULL_FACE_MODE)
    private val frontFace = integer(GL11C.GL_FRONT_FACE)
    private val shaderBlendMode = BlendModeAccessor.getLastApplied()
    private val blendEnabled = GL11C.glIsEnabled(GL11C.GL_BLEND)
    private val blendSrcRgb = integer(GL14C.GL_BLEND_SRC_RGB)
    private val blendDstRgb = integer(GL14C.GL_BLEND_DST_RGB)
    private val blendSrcAlpha = integer(GL14C.GL_BLEND_SRC_ALPHA)
    private val blendDstAlpha = integer(GL14C.GL_BLEND_DST_ALPHA)
    private val blendEquationRgb = integer(GL20C.GL_BLEND_EQUATION_RGB)
    private val blendEquationAlpha = integer(GL20C.GL_BLEND_EQUATION_ALPHA)
    private val program = integer(GL20C.GL_CURRENT_PROGRAM)
    private val vertexArray = integer(GL30C.GL_VERTEX_ARRAY_BINDING)
    private val arrayBuffer = integer(GL15C.GL_ARRAY_BUFFER_BINDING)
    private val activeTexture = integer(GL13C.GL_ACTIVE_TEXTURE)
    private val shaderTexture = RenderSystem.getShaderTexture(0)
    private val textureBindings = captureTextureBindings()

    private fun captureTextureBindings(): IntArray =
        try {
            RenderSystem.activeTexture(activeTexture)
            IntArray(TEXTURE_UNIT_COUNT) { unit ->
                RenderSystem.activeTexture(GL13C.GL_TEXTURE0 + unit)
                integer(GL11C.GL_TEXTURE_BINDING_2D).also { GlStateManager._bindTexture(it) }
            }
        } finally {
            RenderSystem.activeTexture(activeTexture)
        }

    private fun prepare() {
        RenderSystem.activeTexture(GL13C.GL_TEXTURE0)
        disableScissor()
        RenderSystem.disableColorLogicOp()
        GL11C.glDisable(GL11C.GL_COLOR_LOGIC_OP)
    }

    private fun restore() {
        restoreBindings()
        restoreRasterState()
        GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, readFramebuffer)
        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, drawFramebuffer)
        RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3])
    }

    private fun restoreBindings() {
        VertexBuffer.unbind()
        GlStateManager._glBindVertexArray(vertexArray)
        GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, arrayBuffer)
        GlStateManager._glUseProgram(program)
        textureBindings.forEachIndexed { unit, texture ->
            RenderSystem.activeTexture(GL13C.GL_TEXTURE0 + unit)
            GlStateManager._bindTexture(texture)
        }

        RenderSystem.setShaderTexture(0, shaderTexture)
        RenderSystem.activeTexture(activeTexture)
    }

    private fun restoreRasterState() {
        RenderSystem.blendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha)
        GL20C.glBlendEquationSeparate(blendEquationRgb, blendEquationAlpha)
        // Offscreen and GUI composites must restore the shader cache as well as the actual GL state.
        BlendModeAccessor.setLastApplied(shaderBlendMode)
        if (blendEnabled) RenderSystem.enableBlend() else RenderSystem.disableBlend()
        if (depthTestEnabled) RenderSystem.enableDepthTest() else RenderSystem.disableDepthTest()
        if (cullEnabled) RenderSystem.enableCull() else RenderSystem.disableCull()
        GL11C.glCullFace(cullFaceMode)
        GL11C.glFrontFace(frontFace)
        if (colorLogicEnabled) RenderSystem.enableColorLogicOp() else RenderSystem.disableColorLogicOp()
        if (stencilEnabled) GL11C.glEnable(GL11C.GL_STENCIL_TEST) else GL11C.glDisable(GL11C.GL_STENCIL_TEST)
        RenderSystem.depthFunc(depthFunction)
        RenderSystem.depthMask(depthMask)
        RenderSystem.colorMask(colorMask[0] != 0, colorMask[1] != 0, colorMask[2] != 0, colorMask[3] != 0)
        GlStateManager._clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3])
        GlStateManager._scissorBox(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3])
        if (scissorEnabled) GlStateManager._enableScissorTest() else GlStateManager._disableScissorTest()
    }

    companion object {
        // Four effect/mask samplers, five bloom levels and the first-person coverage mask.
        private const val TEXTURE_UNIT_COUNT = 10

        fun disableScissor() {
            RenderSystem.disableScissor()
            GL11C.glDisable(GL11C.GL_SCISSOR_TEST)
        }

        fun prepareColorWrite(writeAlpha: Boolean = true) {
            // Other renderers may use raw GL, bypassing GlStateManager's cache.
            // Synchronize both before clearing or writing persistent color targets.
            RenderSystem.colorMask(true, true, true, writeAlpha)
            GL11C.glColorMask(true, true, true, writeAlpha)
            RenderSystem.depthMask(false)
            GL11C.glDepthMask(false)
        }

        fun <T> withSavedState(draw: () -> T): T {
            val state = HeartBlackHoleRenderState()
            try {
                state.prepare()
                return draw()
            } finally {
                state.restore()
            }
        }

        private fun integer(parameter: Int): Int = GlStateManager._getInteger(parameter)

        private fun integers(parameter: Int): IntArray =
            IntArray(4).also { GL11C.glGetIntegerv(parameter, it) }
    }
}