package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.GTLAdditions
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL30C
import org.lwjgl.opengl.GL32C

internal object HeartBlackHoleFramebuffer {
    private val reportedProblems = HashSet<String>()

    fun querySceneDepth(framebuffer: Int): DepthAttachment? {
        if (framebuffer == 0) {
            reportProblem("the scene uses the default framebuffer, whose depth attachment cannot be shared")
            return null
        }

        val previousFramebuffer = GlStateManager._getInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING)
        try {
            GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, framebuffer)
            if (!isComplete("scene framebuffer")) return null

            // A packed depth/stencil image is also visible at GL_DEPTH_ATTACHMENT.
            // Querying GL_DEPTH_STENCIL_ATTACHMENT is invalid on depth-only framebuffers.
            val type = depthParameter(GL30C.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE)
            if (type == GL11C.GL_NONE) {
                reportProblem("the scene framebuffer has no depth attachment")
                return null
            }
            val name = depthParameter(GL30C.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME)
            return when (type) {
                GL11C.GL_TEXTURE -> DepthAttachment.Texture(
                    name,
                    depthParameter(GL30C.GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL)
                )

                GL30C.GL_RENDERBUFFER -> DepthAttachment.Renderbuffer(name)
                else -> {
                    reportProblem("unsupported scene depth attachment type: 0x${type.toString(16)}")
                    null
                }
            }
        } finally {
            GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, previousFramebuffer)
        }
    }

    fun withSceneDepth(target: RenderTarget, sceneDepth: DepthAttachment, draw: () -> Unit): Boolean {
        check(!target.useDepth) { "Heart effect targets must not own a depth attachment" }
        if (!bindForDraw(target)) return false

        try {
            sceneDepth.attach()
            if (!isComplete("effect framebuffer with scene depth")) return false
            draw()
            return true
        } finally {
            // The target owns only its color image. Always release the borrowed depth,
            // including when validation or rendering fails, without touching scene stencil.
            GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, target.frameBufferId)
            GL32C.glFramebufferTexture(GL30C.GL_DRAW_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT, 0, 0)
        }
    }

    fun bindForDraw(target: RenderTarget): Boolean {
        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, target.frameBufferId)
        RenderSystem.viewport(0, 0, target.viewWidth, target.viewHeight)
        return isComplete("effect framebuffer")
    }

    fun clearColor(target: RenderTarget): Boolean {
        if (!bindForDraw(target)) return false
        HeartBlackHoleRenderState.disableScissor()
        HeartBlackHoleRenderState.prepareColorWrite()
        GlStateManager._clearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GlStateManager._clear(GL11C.GL_COLOR_BUFFER_BIT, false)

        return true
    }

    fun isComplete(description: String): Boolean {
        val status = GL30C.glCheckFramebufferStatus(GL30C.GL_DRAW_FRAMEBUFFER)
        if (status == GL30C.GL_FRAMEBUFFER_COMPLETE) return true
        reportProblem("$description is incomplete: 0x${status.toString(16)}")
        return false
    }

    private fun depthParameter(parameter: Int): Int =
        GL30C.glGetFramebufferAttachmentParameteri(
            GL30C.GL_DRAW_FRAMEBUFFER,
            GL30C.GL_DEPTH_ATTACHMENT,
            parameter
        )

    private fun reportProblem(problem: String) {
        if (reportedProblems.add(problem)) {
            GTLAdditions.LOGGER.warn("Skipping Heart of the Universe effect: {}", problem)
        }
    }

    sealed interface DepthAttachment {
        fun attach()

        data class Texture(val name: Int, val level: Int) : DepthAttachment {
            override fun attach() {
                GL32C.glFramebufferTexture(GL30C.GL_DRAW_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT, name, level)
            }
        }

        data class Renderbuffer(val name: Int) : DepthAttachment {
            override fun attach() {
                GL30C.glFramebufferRenderbuffer(
                    GL30C.GL_DRAW_FRAMEBUFFER,
                    GL30C.GL_DEPTH_ATTACHMENT,
                    GL30C.GL_RENDERBUFFER,
                    name
                )
            }
        }
    }
}