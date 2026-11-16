package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.RenderUtils
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.server.packs.resources.ResourceProvider
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL12C
import org.lwjgl.opengl.GL30C
import org.lwjgl.system.MemoryUtil
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal object HeartBlackHoleAppearanceTextures {
    private val NOISE_RESOURCE = GTLAdditions.id("textures/effect/heart_noise_deep.png")
    private val GALAXY_RESOURCE = GTLAdditions.id("shaders/lookup/heart_galaxy.dat")
    private const val GALAXY_WIDTH = 2048
    private const val GALAXY_HEIGHT = 1024

    fun readResources(resources: ResourceProvider): PreparedTextures {
        val noise = resources.getResourceOrThrow(NOISE_RESOURCE).open().use { input ->
            NativeImage.read(NativeImage.Format.RGBA, input).use { image ->
                if (image.width != 1024 || image.height != 1024) throw IOException("Incorrect black-hole noise dimensions")
                // Match Three.js TextureLoader's default vertical orientation.
                image.flipY()
                val pixels = ByteBuffer.allocate(image.width * image.height * 4).order(ByteOrder.LITTLE_ENDIAN)
                image.pixelsRGBA.forEach(pixels::putInt)
                TextureData(image.width, image.height, GL11C.GL_RGBA8, GL11C.GL_RGBA, GL11C.GL_UNSIGNED_BYTE, pixels.array(), true)
            }
        }

        val galaxy = resources.getResourceOrThrow(GALAXY_RESOURCE).open().use(::readGalaxy)
        return PreparedTextures(noise, galaxy)
    }

    private fun readGalaxy(input: InputStream): TextureData {
        val expected = 8 + GALAXY_WIDTH * GALAXY_HEIGHT * Int.SIZE_BYTES
        val bytes = HeartTextureCodec.read(input, GALAXY_WIDTH, GALAXY_HEIGHT, 1)
        val data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        if (data.int != GALAXY_WIDTH || data.int != GALAXY_HEIGHT) throw IOException("Incorrect black-hole galaxy dimensions")
        val pixels = ByteBuffer.allocate(expected - 8).order(ByteOrder.nativeOrder())
        while (data.hasRemaining()) pixels.putInt(data.int)

        return TextureData(
            GALAXY_WIDTH,
            GALAXY_HEIGHT,
            GL30C.GL_RGB9_E5,
            GL11C.GL_RGB,
            GL30C.GL_UNSIGNED_INT_5_9_9_9_REV,
            pixels.array(),
            false
        )
    }

    fun uploadTexture(data: TextureData): Int {
        val pixels = MemoryUtil.memAlloc(data.pixels.size)
        val texture = GL11C.glGenTextures()
        try {
            pixels.put(data.pixels).flip()
            GlStateManager._bindTexture(texture)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_LINEAR_MIPMAP_LINEAR)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_LINEAR)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL11C.GL_REPEAT)
            GL11C.glTexParameteri(
                GL11C.GL_TEXTURE_2D,
                GL11C.GL_TEXTURE_WRAP_T,
                if (data.repeatY) GL11C.GL_REPEAT else GL12C.GL_CLAMP_TO_EDGE
            )
            RenderUtils.withTightlyPackedPixels {
                GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, data.internalFormat, data.width, data.height, 0, data.format, data.type, pixels)
            }

            GL30C.glGenerateMipmap(GL11C.GL_TEXTURE_2D)
            if (GL11C.glGetTexLevelParameteri(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_TEXTURE_WIDTH) != data.width) {
                throw IOException("Unable to allocate black-hole appearance texture")
            }

            return texture
        } catch (exception: Exception) {
            GlStateManager._deleteTexture(texture)
            throw exception
        } finally {
            MemoryUtil.memFree(pixels)
        }
    }

    class TextureData(
        val width: Int,
        val height: Int,
        val internalFormat: Int,
        val format: Int,
        val type: Int,
        val pixels: ByteArray,
        val repeatY: Boolean
    )

    class PreparedTextures(val noise: TextureData, val galaxy: TextureData)
}