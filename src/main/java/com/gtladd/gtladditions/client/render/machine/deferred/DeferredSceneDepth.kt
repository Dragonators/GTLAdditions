package com.gtladd.gtladditions.client.render.machine.deferred

import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.ShaderInstance
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL12C
import org.lwjgl.opengl.GL13C
import org.lwjgl.opengl.GL30C
import org.lwjgl.system.MemoryStack
import java.nio.ByteBuffer

/** A copied opaque depth buffer, safe to sample while drawing cropped color-only layers. */
internal class DeferredSceneDepth : AutoCloseable {
    private var texture = 0
    private var width = 0
    private var height = 0
    private var originX = 0
    private var originY = 0

    fun capture(shader: ShaderInstance): Boolean = MemoryStack.stackPush().use { stack ->
        val viewport = stack.mallocInt(4)
        GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, viewport)
        if (viewport[2] <= 0 || viewport[3] <= 0) return@use false
        if (texture == 0) texture = GlStateManager._genTexture()
        RenderSystem.activeTexture(GL13C.GL_TEXTURE0)
        GlStateManager._bindTexture(texture)
        if (width != viewport[2] || height != viewport[3]) {
            width = viewport[2]
            height = viewport[3]
            RenderUtils.withTightlyPackedPixels {
                GL11C.glTexImage2D(
                    GL11C.GL_TEXTURE_2D, 0, GL30C.GL_DEPTH_COMPONENT32F, width, height, 0,
                    GL11C.GL_DEPTH_COMPONENT, GL11C.GL_FLOAT, null as ByteBuffer?
                )
            }
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_NEAREST)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_NEAREST)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL12C.GL_CLAMP_TO_EDGE)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE)
        }
        val readFramebuffer = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING)
        val drawFramebuffer = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING)
        try {
            GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, drawFramebuffer)
            GL11C.glCopyTexSubImage2D(GL11C.GL_TEXTURE_2D, 0, 0, 0, viewport[0], viewport[1], width, height)
        } finally {
            GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, readFramebuffer)
        }
        originX = viewport[0]
        originY = viewport[1]
        bind(shader)
        true
    }

    fun bind(shader: ShaderInstance) {
        shader.setSampler("SceneDepth", texture)
        shader.getUniform("SceneViewport")?.set(originX.toFloat(), originY.toFloat())
    }

    override fun close() {
        if (texture != 0) GlStateManager._deleteTexture(texture)
        texture = 0
        width = 0
        height = 0
    }
}