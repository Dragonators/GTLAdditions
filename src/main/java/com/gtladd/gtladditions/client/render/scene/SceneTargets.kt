package com.gtladd.gtladditions.client.render.scene

import com.gtladd.gtladditions.api.client.scene.SceneDepth
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleRenderState
import com.gtladd.gtladditions.client.render.machine.heart.HeartLayerImages
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.ShaderInstance
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL12C
import org.lwjgl.opengl.GL30C
import org.lwjgl.system.MemoryStack
import java.nio.ByteBuffer

/** The main colour and depth attachments are borrowed; this object never deletes them. */
internal object SceneTargets {
    private var width = 0
    private var height = 0
    private var composeFramebuffer = 0
    private var targetFramebuffer = 0
    private var targetColor = 0
    private var snapshotFormat = 0
    private var stencil = false

    /** `D_o`, `D_t`: the main depth's own format, so a copy samples exactly like the live buffer it is compared with. */
    var opaqueDepth = 0
        private set
    var translucentDepth = 0
        private set

    var composedDepth = 0
        private set

    var sampledDepth = 0
        private set
    var generation = 0
        private set

    /** [source] is the main depth texture. Its format is queried each time: a reallocation may keep the texture name. */
    fun ensure(width: Int, height: Int, source: Int, stencil: Boolean) {
        val format = internalFormat(source)
        if (composedDepth != 0 &&
            width == this.width &&
            height == this.height &&
            format == snapshotFormat &&
            stencil == this.stencil
        ) {
            return
        }

        close()
        try {
            this.width = width
            this.height = height
            snapshotFormat = format
            this.stencil = stencil
            opaqueDepth = depthTexture(format)
            translucentDepth = depthTexture(format)
            composedDepth = depthTexture(if (stencil) GL30C.GL_DEPTH32F_STENCIL8 else GL30C.GL_DEPTH_COMPONENT32F)
            sampledDepth = texture(GL30C.GL_R32F, GL11C.GL_RED, GL11C.GL_FLOAT)
            composeFramebuffer = GL30C.glGenFramebuffers()
            targetFramebuffer = GL30C.glGenFramebuffers()
            withFramebuffer(composeFramebuffer) {
                GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, GL11C.GL_TEXTURE_2D, sampledDepth, 0)
                GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, depthAttachment(), GL11C.GL_TEXTURE_2D, composedDepth, 0)
                check(GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER) == GL30C.GL_FRAMEBUFFER_COMPLETE) {
                    "Incomplete scene depth framebuffer"
                }
            }
        } catch (error: Exception) {
            close()
            throw error
        }

        generation++
    }

    /** A same-format copy is exact; glCopyTexSubImage does not draw, so no write or blend lock applies. */
    fun copyDepth(framebuffer: Int, texture: Int) {
        val read = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING)
        val bound = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D)
        try {
            GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, framebuffer)
            GlStateManager._bindTexture(texture)
            GL11C.glCopyTexSubImage2D(GL11C.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height)
        } finally {
            GlStateManager._bindTexture(bound)
            GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, read)
        }
    }

    /**
     * Without [excludeTranslucent] (Fabulous) `D_comp = D_m`. Nothing writes depth between this pass and the ordered
     * draws, so `D_comp` and its R32F copy agree.
     */
    fun compose(
        shader: ShaderInstance,
        scene: Int,
        opaque: Int,
        translucent: Int,
        excludeTranslucent: Boolean,
        projection: Matrix4fc,
        jittered: Boolean
    ): SceneDepth {
        MemoryStack.stackPush().use { stack ->
            val viewport = stack.mallocInt(4)
            val colorMask = stack.malloc(4)
            GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, viewport)
            GL11C.glGetBooleanv(GL11C.GL_COLOR_WRITEMASK, colorMask)
            RenderUtils.withSavedRenderState {
                withFramebuffer(composeFramebuffer) {
                    RenderSystem.viewport(0, 0, width, height)
                    RenderSystem.disableBlend()
                    RenderSystem.disableCull()
                    // Depth writes need the test enabled.
                    RenderSystem.enableDepthTest()
                    RenderSystem.depthFunc(GL11C.GL_ALWAYS)
                    RenderSystem.depthMask(true)
                    RenderSystem.colorMask(true, true, true, true)
                    shader.setSampler("SceneDepthIn", scene)
                    shader.setSampler("OpaqueDepth", opaque)
                    shader.setSampler("TranslucentDepth", translucent)
                    shader.getUniform("ExcludeTranslucent")?.set(if (excludeTranslucent) 1 else 0)
                    HeartLayerImages.drawQuad(shader)
                }
            }

            RenderSystem.colorMask(
                colorMask[0] != 0.toByte(),
                colorMask[1] != 0.toByte(),
                colorMask[2] != 0.toByte(),
                colorMask[3] != 0.toByte()
            )
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3])
        }

        return SceneDepthState(sampledDepth, width, height, projection, jittered, generation)
    }

    /**
     * [mask] is the completed hand mask; with 0 the stencil is only cleared. The stencil state is left to the caller's
     * scope, set through RenderSystem like its restore.
     */
    fun writeHandStencil(mask: Int, program: ShaderInstance?) {
        MemoryStack.stackPush().use { stack ->
            val viewport = stack.mallocInt(4)
            val scissorBox = stack.mallocInt(4)
            val colorMask = stack.malloc(4)
            GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, viewport)
            GL11C.glGetIntegerv(GL11C.GL_SCISSOR_BOX, scissorBox)
            GL11C.glGetBooleanv(GL11C.GL_COLOR_WRITEMASK, colorMask)
            val scissor = GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST)
            RenderUtils.withSavedRenderState {
                withFramebuffer(composeFramebuffer) {
                    RenderSystem.viewport(0, 0, width, height)
                    HeartBlackHoleRenderState.disableScissor()
                    RenderSystem.stencilMask(0xFF)
                    RenderSystem.clearStencil(0)
                    GlStateManager._clear(GL11C.GL_STENCIL_BUFFER_BIT, false)
                    if (mask != 0 && program != null) {
                        GL11C.glEnable(GL11C.GL_STENCIL_TEST)
                        RenderSystem.stencilFunc(GL11C.GL_ALWAYS, 1, 0xFF)
                        RenderSystem.stencilOp(GL11C.GL_KEEP, GL11C.GL_KEEP, GL11C.GL_REPLACE)
                        RenderSystem.disableDepthTest()
                        RenderSystem.depthMask(false)
                        RenderSystem.colorMask(false, false, false, false)
                        RenderSystem.disableBlend()
                        RenderSystem.disableCull()
                        program.setSampler("SceneHandMask", mask)
                        HeartLayerImages.drawQuad(program)
                    }
                }
            }

            RenderSystem.colorMask(
                colorMask[0] != 0.toByte(),
                colorMask[1] != 0.toByte(),
                colorMask[2] != 0.toByte(),
                colorMask[3] != 0.toByte()
            )
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3])
            GlStateManager._scissorBox(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3])
            if (scissor) GlStateManager._enableScissorTest() else GlStateManager._disableScissorTest()
        }
    }

    fun testHandStencil(written: Boolean) {
        if (!written) return GL11C.glDisable(GL11C.GL_STENCIL_TEST)
        GL11C.glEnable(GL11C.GL_STENCIL_TEST)
        RenderSystem.stencilFunc(GL11C.GL_EQUAL, 0, 0xFF)
        RenderSystem.stencilOp(GL11C.GL_KEEP, GL11C.GL_KEEP, GL11C.GL_KEEP)
        RenderSystem.stencilMask(0)
    }

    /**
     * The colour is attached on every call: a GUI-scale change recreates the main target at the same size and may hand the
     * new texture the old name.
     */
    fun bindTarget(color: Int) {
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, targetFramebuffer)
        GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, GL11C.GL_TEXTURE_2D, color, 0)
        if (targetColor != color) {
            GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, depthAttachment(), GL11C.GL_TEXTURE_2D, composedDepth, 0)
            check(GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER) == GL30C.GL_FRAMEBUFFER_COMPLETE) {
                "Incomplete scene composition framebuffer"
            }

            targetColor = color
        }

        RenderSystem.viewport(0, 0, width, height)
    }

    /** A borrowed view that owns nothing and is never destroyed. */
    fun composition(color: Int): RenderTarget = compositionView.also { view ->
        view.color = color
        view.width = width
        view.height = height
        view.viewWidth = width
        view.viewHeight = height
        view.frameBufferId = targetFramebuffer
    }

    private val compositionView = CompositionView()

    private class CompositionView : RenderTarget(false) {
        var color = 0

        override fun bindWrite(setViewport: Boolean) = bindTarget(color)
    }

    fun close() {
        for (texture in intArrayOf(opaqueDepth, translucentDepth, composedDepth, sampledDepth)) {
            if (texture != 0) GlStateManager._deleteTexture(texture)
        }

        if (composeFramebuffer != 0) GL30C.glDeleteFramebuffers(composeFramebuffer)
        if (targetFramebuffer != 0) GL30C.glDeleteFramebuffers(targetFramebuffer)
        opaqueDepth = 0
        translucentDepth = 0
        composedDepth = 0
        sampledDepth = 0
        composeFramebuffer = 0
        targetFramebuffer = 0
        targetColor = 0
        snapshotFormat = 0
        stencil = false
        width = 0
        height = 0
    }

    private fun depthAttachment() = if (stencil) GL30C.GL_DEPTH_STENCIL_ATTACHMENT else GL30C.GL_DEPTH_ATTACHMENT

    private fun internalFormat(texture: Int): Int {
        val bound = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D)
        GlStateManager._bindTexture(texture)
        val format = GL11C.glGetTexLevelParameteri(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_TEXTURE_INTERNAL_FORMAT)
        GlStateManager._bindTexture(bound)
        return format
    }

    private fun depthTexture(internalFormat: Int): Int = when (internalFormat) {
        GL30C.GL_DEPTH24_STENCIL8, GL30C.GL_DEPTH_STENCIL -> texture(internalFormat, GL30C.GL_DEPTH_STENCIL, GL30C.GL_UNSIGNED_INT_24_8)
        GL30C.GL_DEPTH32F_STENCIL8 -> texture(internalFormat, GL30C.GL_DEPTH_STENCIL, GL30C.GL_FLOAT_32_UNSIGNED_INT_24_8_REV)
        else -> texture(internalFormat, GL11C.GL_DEPTH_COMPONENT, GL11C.GL_FLOAT)
    }

    private fun texture(internalFormat: Int, format: Int, type: Int): Int {
        val texture = GlStateManager._genTexture()
        val bound = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D)
        GlStateManager._bindTexture(texture)
        RenderUtils.withTightlyPackedPixels {
            GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, null as ByteBuffer?)
        }

        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_NEAREST)
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_NEAREST)
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL12C.GL_CLAMP_TO_EDGE)
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE)
        GlStateManager._bindTexture(bound)

        return texture
    }

    private inline fun withFramebuffer(framebuffer: Int, action: () -> Unit) {
        val read = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING)
        val draw = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING)
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, framebuffer)
        try {
            action()
        } finally {
            GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, read)
            GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, draw)
        }
    }
}

internal class SceneDepthState(
    override val texture: Int,
    override val width: Int,
    override val height: Int,
    projection: Matrix4fc,
    override val jittered: Boolean,
    override val generation: Int
) : SceneDepth {
    override val originX = 0
    override val originY = 0
    override val inverseProjection: Matrix4f = Matrix4f(projection).invert()
    override val translucentTerrain = false

    override fun bind(shader: ShaderInstance) {
        shader.setSampler("SceneDepth", texture)
        shader.getUniform("SceneSize")?.set(width.toFloat(), height.toFloat())
        shader.getUniform("SceneViewport")?.set(originX.toFloat(), originY.toFloat())
        shader.getUniform("SceneInverseProjection")?.set(inverseProjection)
    }
}