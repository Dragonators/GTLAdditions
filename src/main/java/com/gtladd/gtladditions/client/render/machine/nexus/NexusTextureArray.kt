package com.gtladd.gtladditions.client.render.machine.nexus

import com.google.gson.JsonObject
import com.mojang.blaze3d.platform.NativeImage
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.*
import org.lwjgl.opengl.GL14.GL_MIRRORED_REPEAT
import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryUtil
import java.io.IOException

/** File-backed arrays keep each shape's mip chain isolated, including its longitude seam. */
internal class NexusTextureArray(spec: JsonObject, layers: Layers, ledger: NexusGpu.Ledger) {
    val id = ledger.texture(glGenTextures())
    var bytes = 0L
        private set

    init {
        val format = if (layers.channels == 1) {
            GL_RED
        } else if (layers.channels == 3) {
            GL_RGB
        } else {
            GL_RGBA
        }

        val internal = if (layers.channels == 1) {
            GL_R8
        } else if (layers.channels == 3) {
            GL_RGB8
        } else {
            GL_RGBA8
        }

        glBindTexture(GL_TEXTURE_2D_ARRAY, id)
        glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, internal, layers.width, layers.height, layers.pixels.size, 0, format, GL_UNSIGNED_BYTE, 0L)
        var w = layers.width
        var h = layers.height

        while (true) {
            bytes += w.toLong() * h * layers.pixels.size * layers.channels
            if (w == 1 && h == 1) break
            w = maxOf(1, w / 2)
            h = maxOf(1, h / 2)
        }

        val pixels = MemoryUtil.memAlloc(layers.width * layers.height * layers.channels)
        try {
            for ((layer, data) in layers.pixels.withIndex()) {
                pixels.clear()
                pixels.put(data).flip()
                glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, layer, layers.width, layers.height, 1, format, GL_UNSIGNED_BYTE, pixels)
            }
        } finally {
            MemoryUtil.memFree(pixels)
        }

        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

        fun wrap(name: String) = when (spec.get(name)?.asInt) {
            1000 -> GL_REPEAT
            1002 -> GL_MIRRORED_REPEAT
            else -> GL_CLAMP_TO_EDGE
        }

        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, wrap("wrapS"))
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, wrap("wrapT"))
        glGenerateMipmap(GL_TEXTURE_2D_ARRAY)
    }

    /** Tightly packed rows of every layer, [channels] bytes per pixel. */
    class Layers(val width: Int, val height: Int, val channels: Int, val pixels: List<ByteArray>)

    companion object {
        @Throws(IOException::class)
        fun read(spec: JsonObject, file: (String) -> ByteArray): Layers {
            val files = spec.getAsJsonArray("layers")
            require(files.size() > 0)
            val channels = spec.get("channels")?.asInt ?: 4
            require(channels == 1 || channels == 3 || channels == 4)
            var width = 0
            var height = 0
            val pixels = files.mapIndexed { layer, name ->
                NativeImage.read(file(name.asString).inputStream()).use { image ->
                    if (layer == 0) {
                        width = image.width
                        height = image.height
                    }

                    require(image.width == width && image.height == height) { "Inconsistent Nexus array layer: $name" }
                    val data = ByteArray(width * height * channels)
                    var index = 0

                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            val rgba = image.getPixelRGBA(x, y)
                            data[index++] = rgba.toByte()
                            if (channels >= 3) {
                                data[index++] = (rgba ushr 8).toByte()
                                data[index++] = (rgba ushr 16).toByte()
                            }

                            if (channels == 4) data[index++] = (rgba ushr 24).toByte()
                        }
                    }

                    data
                }
            }

            return Layers(width, height, channels, pixels)
        }
    }
}
