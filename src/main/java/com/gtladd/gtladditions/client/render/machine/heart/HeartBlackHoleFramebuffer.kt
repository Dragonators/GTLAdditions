package com.gtladd.gtladditions.client.render.machine.heart

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL30C

internal object HeartBlackHoleFramebuffer {
    fun restoreFramebuffer(framebuffer: Int, width: Int, height: Int) {
        RenderSystem.defaultBlendFunc()
        RenderSystem.disableBlend()
        RenderSystem.enableCull()
        RenderSystem.colorMask(true, true, true, true)
        RenderSystem.depthMask(true)
        RenderSystem.enableDepthTest()
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, framebuffer)
        RenderSystem.viewport(0, 0, width, height)
    }

    fun prepareFramebufferWrite(writeDepth: Boolean) {
        RenderSystem.colorMask(true, true, true, true)
        RenderSystem.depthMask(writeDepth)
    }

    fun queryDepthAttachments(framebuffer: Int): DepthAttachments? {
        val previousReadFramebuffer = GlStateManager._getInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING)
        val previousDrawFramebuffer = GlStateManager._getInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING)
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, framebuffer)
        val attachments = queryBoundDepthAttachments()
        GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, previousReadFramebuffer)
        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, previousDrawFramebuffer)
        return attachments.takeIf { it.hasAttachment }
    }

    fun replaceDepthAttachments(framebuffer: Int, sharedDepth: DepthAttachments): DepthAttachments {
        val previousReadFramebuffer = GlStateManager._getInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING)
        val previousDrawFramebuffer = GlStateManager._getInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING)
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, framebuffer)

        val originalDepth = queryBoundDepthAttachments()
        detachDepthAttachment(GL30C.GL_DEPTH_ATTACHMENT)
        detachDepthAttachment(GL30C.GL_DEPTH_STENCIL_ATTACHMENT)
        attachDepthAttachment(GL30C.GL_DEPTH_ATTACHMENT, sharedDepth.depth)
        attachDepthAttachment(GL30C.GL_DEPTH_STENCIL_ATTACHMENT, sharedDepth.depthStencil)

        GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, previousReadFramebuffer)
        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, previousDrawFramebuffer)
        return originalDepth
    }

    fun restoreDepthAttachments(framebuffer: Int, depthAttachments: DepthAttachments) {
        val previousReadFramebuffer = GlStateManager._getInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING)
        val previousDrawFramebuffer = GlStateManager._getInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING)
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, framebuffer)

        detachDepthAttachment(GL30C.GL_DEPTH_ATTACHMENT)
        detachDepthAttachment(GL30C.GL_DEPTH_STENCIL_ATTACHMENT)
        attachDepthAttachment(GL30C.GL_DEPTH_ATTACHMENT, depthAttachments.depth)
        attachDepthAttachment(GL30C.GL_DEPTH_STENCIL_ATTACHMENT, depthAttachments.depthStencil)

        GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, previousReadFramebuffer)
        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, previousDrawFramebuffer)
    }

    fun copyDepthFromFramebuffer(sourceFramebuffer: Int, width: Int, height: Int, target: RenderTarget) {
        GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, sourceFramebuffer)
        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, target.frameBufferId)
        GlStateManager._glBlitFrameBuffer(
            0,
            0,
            width,
            height,
            0,
            0,
            target.width,
            target.height,
            GL11C.GL_DEPTH_BUFFER_BIT,
            GL11C.GL_NEAREST
        )
    }

    private fun queryBoundDepthAttachments(): DepthAttachments =
        DepthAttachments(
            queryBoundDepthAttachment(GL30C.GL_DEPTH_ATTACHMENT),
            queryBoundDepthAttachment(GL30C.GL_DEPTH_STENCIL_ATTACHMENT)
        )

    private fun queryBoundDepthAttachment(attachment: Int): DepthAttachment =
        DepthAttachment(
            GL30C.glGetFramebufferAttachmentParameteri(
                GL30C.GL_FRAMEBUFFER,
                attachment,
                GL30C.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE
            ),
            GL30C.glGetFramebufferAttachmentParameteri(
                GL30C.GL_FRAMEBUFFER,
                attachment,
                GL30C.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME
            )
        )

    private fun attachDepthAttachment(attachment: Int, depthAttachment: DepthAttachment) {
        when (depthAttachment.type) {
            GL11C.GL_TEXTURE -> GL30C.glFramebufferTexture2D(
                GL30C.GL_FRAMEBUFFER,
                attachment,
                GL11C.GL_TEXTURE_2D,
                depthAttachment.name,
                0
            )

            GL30C.GL_RENDERBUFFER -> GL30C.glFramebufferRenderbuffer(
                GL30C.GL_FRAMEBUFFER,
                attachment,
                GL30C.GL_RENDERBUFFER,
                depthAttachment.name
            )
        }
    }

    private fun detachDepthAttachment(attachment: Int) {
        GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, attachment, GL11C.GL_TEXTURE_2D, 0, 0)
        GL30C.glFramebufferRenderbuffer(GL30C.GL_FRAMEBUFFER, attachment, GL30C.GL_RENDERBUFFER, 0)
    }

    internal data class DepthAttachment(
        val type: Int,
        val name: Int
    ) {
        val hasAttachment: Boolean
            get() = type != GL11C.GL_NONE && name != 0
    }

    internal data class DepthAttachments(
        val depth: DepthAttachment,
        val depthStencil: DepthAttachment
    ) {
        val hasAttachment: Boolean
            get() = depth.hasAttachment || depthStencil.hasAttachment
    }
}