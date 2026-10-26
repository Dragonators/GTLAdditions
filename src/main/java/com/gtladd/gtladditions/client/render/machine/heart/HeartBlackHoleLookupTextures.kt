package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.platform.GlStateManager
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.server.packs.resources.ResourceProvider
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL12C
import org.lwjgl.opengl.GL30C
import org.lwjgl.system.MemoryUtil
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal object HeartBlackHoleLookupTextures {
    private val DEFLECTION_RESOURCE = GTLAdditions.id("shaders/lookup/heart_ray_deflection.dat")
    private val INVERSE_RADIUS_RESOURCE = GTLAdditions.id("shaders/lookup/heart_ray_inverse_radius.dat")
    private var textures: TexturePair? = null

    @Throws(IOException::class)
    fun readResources(resources: ResourceProvider): PreparedTables {
        val deflection = resources.getResourceOrThrow(DEFLECTION_RESOURCE).open().use {
            readTable(it, 512, 512)
        }

        val inverseRadius = resources.getResourceOrThrow(INVERSE_RADIUS_RESOURCE).open().use {
            readTable(it, 64, 32)
        }

        return PreparedTables(deflection, inverseRadius)
    }

    fun upload(tables: PreparedTables) {
        val next = HeartBlackHoleRenderState.withSavedState {
            val deflection = uploadTable(tables.deflection)
            try {
                TexturePair(deflection, uploadTable(tables.inverseRadius))
            } catch (exception: Exception) {
                GlStateManager._deleteTexture(deflection)
                throw exception
            }
        }
        val previous = textures
        textures = next
        previous?.close()
    }

    fun setSamplers(shader: ShaderInstance): Boolean {
        val current = textures ?: return false
        shader.setSampler("RayDeflectionSampler", current.deflection)
        shader.setSampler("RayInverseRadiusSampler", current.inverseRadius)
        return true
    }

    @Throws(IOException::class)
    fun readTable(input: InputStream, width: Int, height: Int): LookupTable {
        val bytes = HeartTextureCodec.read(input, width, height, 2)
        val data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        if (data.float != width.toFloat() || data.float != height.toFloat()) {
            throw IOException("Incorrect black-hole lookup table dimensions; expected ${width}x$height")
        }

        val values = FloatArray(width * height * 2) {
            data.float.also { value ->
                if (!value.isFinite()) throw IOException("Non-finite black-hole lookup table value")
            }
        }

        return LookupTable(width, height, values)
    }

    private fun uploadTable(table: LookupTable): Int {
        val pixels = MemoryUtil.memAllocFloat(table.values.size)
        val texture = GL11C.glGenTextures()
        try {
            pixels.put(table.values).flip()
            GlStateManager._bindTexture(texture)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_LINEAR)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_LINEAR)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL12C.GL_CLAMP_TO_EDGE)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL12C.GL_TEXTURE_BASE_LEVEL, 0)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL12C.GL_TEXTURE_MAX_LEVEL, 0)
            RenderUtils.withTightlyPackedPixels {
                GL11C.glTexImage2D(
                    GL11C.GL_TEXTURE_2D,
                    0,
                    GL30C.GL_RG32F,
                    table.width,
                    table.height,
                    0,
                    GL30C.GL_RG,
                    GL11C.GL_FLOAT,
                    pixels
                )
            }

            if (GL11C.glGetTexLevelParameteri(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_TEXTURE_WIDTH) != table.width) {
                throw IOException("Unable to allocate black-hole lookup texture")
            }

            return texture
        } catch (exception: Exception) {
            GlStateManager._deleteTexture(texture)
            throw exception
        } finally {
            MemoryUtil.memFree(pixels)
        }
    }

    class LookupTable(val width: Int, val height: Int, val values: FloatArray)

    class PreparedTables(val deflection: LookupTable, val inverseRadius: LookupTable)

    private class TexturePair(val deflection: Int, val inverseRadius: Int) : AutoCloseable {
        override fun close() {
            GlStateManager._deleteTexture(deflection)
            GlStateManager._deleteTexture(inverseRadius)
        }
    }
}