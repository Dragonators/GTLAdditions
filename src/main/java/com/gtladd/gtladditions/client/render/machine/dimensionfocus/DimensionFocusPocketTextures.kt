package com.gtladd.gtladditions.client.render.machine.dimensionfocus

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.scene.ResourceSlot
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceProvider
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.lwjgl.opengl.EXTTextureFilterAnisotropic
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL12C
import org.lwjgl.opengl.GL13C
import org.lwjgl.opengl.GL30C
import org.lwjgl.system.MemoryUtil
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream

@OnlyIn(Dist.CLIENT)
internal object DimensionFocusPocketTextures {
    private const val GAS_SIZE = 512
    private const val NOISE_SIZE = 64
    private const val MAX_ANISOTROPY = 8f

    // +X -X +Y -Y +Z -Z in GL face orientation.
    private val FACES = listOf("px", "nx", "py", "ny", "pz", "nz")
    private val NOISE = GTLAdditions.id("shaders/lookup/dimension_focus_noise.dat")
    private val slot = ResourceSlot<Loaded>("Dimension Focus pocket textures")

    val current: Loaded?
        get() = slot.value

    val pending: Boolean
        get() = slot.state == ResourceSlot.State.EMPTY || slot.state == ResourceSlot.State.PENDING

    fun invalidate() = slot.invalidate()

    private fun read(resources: ResourceProvider): Prepared =
        Prepared(readCube(resources, "veil_near"), readCube(resources, "veil_deep"), readNoise(resources))

    private fun load(prepared: Prepared) {
        val created = ArrayList<Int>(3)
        val next = try {
            val near = uploadCube(prepared.gasNear).also(created::add)
            val deep = uploadCube(prepared.gasDeep).also(created::add)
            Loaded(uploadNoise(prepared.noise).also(created::add), near, deep)
        } catch (error: Exception) {
            created.forEach(GlStateManager::_deleteTexture)
            throw error
        }

        slot.publish(next)
    }

    private fun readCube(resources: ResourceProvider, name: String): List<ByteArray> = FACES.map { face ->
        resources.getResourceOrThrow(GTLAdditions.id("textures/effect/dimension_focus/${name}_$face.png")).open().use { input ->
            NativeImage.read(NativeImage.Format.RGBA, input).use { image ->
                if (image.width != GAS_SIZE || image.height != GAS_SIZE) {
                    throw IOException("Incorrect Dimension Focus gas face size: ${name}_$face")
                }

                // Face rows are already in GL order (row 0 at t = -1), so no flip.
                val pixels = ByteBuffer.allocate(GAS_SIZE * GAS_SIZE * 4).order(ByteOrder.LITTLE_ENDIAN)
                image.pixelsRGBA.forEach(pixels::putInt)
                pixels.array()
            }
        }
    }

    private fun readNoise(resources: ResourceProvider): ByteArray {
        val bytes = resources.getResourceOrThrow(NOISE).open().use { GZIPInputStream(it).readAllBytes() }
        if (bytes.size != NOISE_SIZE * NOISE_SIZE * NOISE_SIZE * 3) throw IOException("Incorrect Dimension Focus noise size")
        return bytes
    }

    private fun uploadCube(faces: List<ByteArray>): Int = upload(GL13C.GL_TEXTURE_CUBE_MAP, GL13C.GL_TEXTURE_BINDING_CUBE_MAP) {
        val pixels = MemoryUtil.memAlloc(GAS_SIZE * GAS_SIZE * 4)
        try {
            faces.forEachIndexed { index, face ->
                pixels.clear()
                pixels.put(face).flip()
                GL11C.glTexImage2D(
                    GL13C.GL_TEXTURE_CUBE_MAP_POSITIVE_X + index,
                    0,
                    GL11C.GL_RGBA8,
                    GAS_SIZE,
                    GAS_SIZE,
                    0,
                    GL11C.GL_RGBA,
                    GL11C.GL_UNSIGNED_BYTE,
                    pixels
                )
            }
        } finally {
            MemoryUtil.memFree(pixels)
        }

        GL11C.glTexParameteri(GL13C.GL_TEXTURE_CUBE_MAP, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_LINEAR_MIPMAP_LINEAR)
        GL11C.glTexParameteri(GL13C.GL_TEXTURE_CUBE_MAP, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_LINEAR)

        for (wrap in intArrayOf(GL11C.GL_TEXTURE_WRAP_S, GL11C.GL_TEXTURE_WRAP_T, GL12C.GL_TEXTURE_WRAP_R)) {
            GL11C.glTexParameteri(GL13C.GL_TEXTURE_CUBE_MAP, wrap, GL12C.GL_CLAMP_TO_EDGE)
        }

        if (GL.getCapabilities().GL_EXT_texture_filter_anisotropic) {
            val limit = GL11C.glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT)
            GL11C.glTexParameterf(
                GL13C.GL_TEXTURE_CUBE_MAP,
                EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT,
                minOf(MAX_ANISOTROPY, limit)
            )
        }

        GL30C.glGenerateMipmap(GL13C.GL_TEXTURE_CUBE_MAP)
    }

    private fun uploadNoise(noise: ByteArray): Int = upload(GL12C.GL_TEXTURE_3D, GL12C.GL_TEXTURE_BINDING_3D) {
        val pixels = MemoryUtil.memAlloc(noise.size)
        try {
            pixels.put(noise).flip()
            GL12C.glTexImage3D(
                GL12C.GL_TEXTURE_3D,
                0,
                GL11C.GL_RGB8,
                NOISE_SIZE,
                NOISE_SIZE,
                NOISE_SIZE,
                0,
                GL11C.GL_RGB,
                GL11C.GL_UNSIGNED_BYTE,
                pixels
            )
        } finally {
            MemoryUtil.memFree(pixels)
        }

        GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_LINEAR)
        GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_LINEAR)
        GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL12C.GL_TEXTURE_MAX_LEVEL, 0)

        for (wrap in intArrayOf(GL11C.GL_TEXTURE_WRAP_S, GL11C.GL_TEXTURE_WRAP_T, GL12C.GL_TEXTURE_WRAP_R)) {
            GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, wrap, GL11C.GL_REPEAT)
        }
    }

    private inline fun upload(target: Int, binding: Int, fill: () -> Unit): Int {
        val previous = GL11C.glGetInteger(binding)
        val texture = GL11C.glGenTextures()
        try {
            GL11C.glBindTexture(target, texture)
            RenderUtils.withTightlyPackedPixels(fill)
            return texture
        } catch (exception: Exception) {
            GlStateManager._deleteTexture(texture)
            throw exception
        } finally {
            GL11C.glBindTexture(target, previous)
        }
    }

    private class Prepared(val gasNear: List<ByteArray>, val gasDeep: List<ByteArray>, val noise: ByteArray)

    class Loaded(val noise: Int, val gasNear: Int, val gasDeep: Int) : AutoCloseable {
        override fun close() {
            GlStateManager._deleteTexture(noise)
            GlStateManager._deleteTexture(gasNear)
            GlStateManager._deleteTexture(gasDeep)
        }
    }

    object Reload : SimplePreparableReloadListener<() -> Unit>() {
        override fun prepare(resources: ResourceManager, profiler: ProfilerFiller): () -> Unit {
            val prepared = try {
                read(resources)
            } catch (error: Exception) {
                return { throw error }
            }
            return { load(prepared) }
        }

        override fun apply(upload: () -> Unit, resources: ResourceManager, profiler: ProfilerFiller) {
            try {
                upload()
            } catch (error: Exception) {
                slot.fail(error)
            }
        }
    }
}