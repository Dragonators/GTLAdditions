package com.gtladd.gtladditions.client.render.machine.matrix

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.server.packs.resources.ResourceProvider
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL12C
import org.lwjgl.opengl.GL13C
import org.lwjgl.opengl.GL30C
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** RGBA8 keeps data/normal maps linear; the base map is decoded explicitly by the eye shader. */
internal class MatrixEyeTextures : AutoCloseable {
    private val textures = HashMap<String, Int>()
    private var attempted = false

    fun prepare(resources: ResourceProvider): Boolean {
        if (attempted) return textures.isNotEmpty()
        attempted = true
        try {
            for (name in listOf("gold_base", "data", "normal", "mist_noise")
        ) {
                val id = GTLAdditions.id("textures/effect/matrix/eye/$name.png")
                resources.getResourceOrThrow(id).open().use { input ->
                    NativeImage.read(NativeImage.Format.RGBA, input).use { image ->
                        val pixels = MemoryUtil.memAlloc(image.width * image.height * 4).order(ByteOrder.LITTLE_ENDIAN)
                        try {
                            for (y in 0 until image.height) {
                                for (x in 0 until image.width) {
                                    pixels.putInt(image.getPixelRGBA(x, y))
                                }
                            }
                            pixels.flip()
                            textures[name] = upload(image.width, image.height, pixels, name != "mist_noise")
                        } finally {
                            MemoryUtil.memFree(pixels)
                        }
                    }
                }
            }
            return true
        } catch (exception: Exception) {
            textures.values.forEach(GlStateManager::_deleteTexture)
            textures.clear()
            GTLAdditions.LOGGER.warn("Unable to load matrix eye textures; retrying on resource reload", exception)
            return false
        }
    }

    operator fun get(name: String): Int = textures.getValue(name)

    private fun upload(width: Int, height: Int, pixels: ByteBuffer, mipmap: Boolean): Int {
        val texture = GlStateManager._genTexture()
        try {
            GlStateManager._bindTexture(texture)
            RenderUtils.withTightlyPackedPixels {
                // RGBA8 keeps data/normal maps linear; the base map is decoded explicitly by the eye shader.
                GL11C.glTexImage2D(
                    GL11C.GL_TEXTURE_2D,
                    0,
                    GL11C.GL_RGBA8, width, height,
                    0,
                    GL11C.GL_RGBA,
                    GL11C.GL_UNSIGNED_BYTE,
                    pixels
                )
            }

            GL11C.glTexParameteri(
                GL11C.GL_TEXTURE_2D,
                GL11C.GL_TEXTURE_MIN_FILTER,
                if (mipmap) GL11C.GL_LINEAR_MIPMAP_LINEAR else GL11C.GL_LINEAR
            )
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_LINEAR)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL12C.GL_CLAMP_TO_EDGE)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE)
            if (mipmap) GL30C.glGenerateMipmap(GL11C.GL_TEXTURE_2D)
            return texture
        } catch (exception: Exception) {
            GlStateManager._deleteTexture(texture)
            throw exception
        }
    }

    override fun close() {
        textures.values.forEach(GlStateManager::_deleteTexture)
        textures.clear()
        attempted = false
    }
}

/** Sampling a copy avoids framebuffer feedback on the bound draw target. */
internal class MatrixEyeScene : AutoCloseable {
    private var color = 0
    private var width = 0
    private var height = 0

    /** [depth] is this frame's R32F scene depth, which matches the composition target's depth bit for bit. */
    fun capture(shader: ShaderInstance, depth: Int): Boolean = MemoryStack.stackPush().use { stack ->
        val viewport = stack.mallocInt(4)
        GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, viewport)
        if (viewport[2] <= 0 || viewport[3] <= 0) return@use false
        val resize = width != viewport[2] || height != viewport[3]
        width = viewport[2]
        height = viewport[3]
        if (color == 0) color = GlStateManager._genTexture()
        RenderSystem.activeTexture(GL13C.GL_TEXTURE0)
        val read = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING)
        val draw = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING)
        val readBuffer = GL11C.glGetInteger(GL11C.GL_READ_BUFFER)
        try {
            GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, draw)
            val drawReadBuffer = GL11C.glGetInteger(GL11C.GL_READ_BUFFER)
            try {
                GL11C.glReadBuffer(GL11C.glGetInteger(GL30C.GL_DRAW_BUFFER0))
                GlStateManager._bindTexture(color)
                if (resize) {
                    RenderUtils.withTightlyPackedPixels {
                        GL11C.glTexImage2D(
                            GL11C.GL_TEXTURE_2D,
                            0,
                            GL11C.GL_RGBA8,
                            width,
                            height,
                            0,
                            GL11C.GL_RGBA,
                            GL11C.GL_UNSIGNED_BYTE,
                            null as ByteBuffer?
                        )
                    }

                    GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_NEAREST)
                    GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_NEAREST)
                    GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL12C.GL_CLAMP_TO_EDGE)
                    GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE)
                }

                GL11C.glCopyTexSubImage2D(GL11C.GL_TEXTURE_2D, 0, 0, 0, viewport[0], viewport[1], width, height)
            } finally {
                GL11C.glReadBuffer(drawReadBuffer)
            }
        } finally {
            GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, read)
            GL11C.glReadBuffer(readBuffer)
        }

        shader.setSampler("tColor", color)
        shader.setSampler("tDepth", depth)
        shader.getUniform("uViewport")?.set(viewport[0].toFloat(), viewport[1].toFloat(), width.toFloat(), height.toFloat())
        shader.getUniform("uResolution")?.set(width.toFloat(), height.toFloat())
        true
    }

    override fun close() {
        if (color != 0) GlStateManager._deleteTexture(color)
        color = 0
        width = 0
        height = 0
    }
}