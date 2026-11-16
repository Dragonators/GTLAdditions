package com.gtladd.gtladditions.client.render.machine.lighthunter

import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.scene.ResourceSlot
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceProvider
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.util.zip.GZIPInputStream

internal object LightHunterParadoxAssets {
    private val slot = ResourceSlot<Loaded>("Light Hunter paradox models")

    val current: Loaded?
        get() = slot.value

    val pending: Boolean
        get() = slot.state == ResourceSlot.State.EMPTY || slot.state == ResourceSlot.State.PENDING

    fun invalidate() = slot.invalidate()

    private fun read(resources: ResourceProvider) = Prepared(readModel(resources, "death_star"), readModel(resources, "kamino"))

    private fun upload(prepared: Prepared): Loaded {
        val created = ArrayList<Part>()
        try {
            return Loaded(
                prepared.deathStar.map { uploadPart(it).also(created::add) },
                prepared.kamino.map { uploadPart(it).also(created::add) }
            )
        } catch (error: Exception) {
            created.forEach(Part::close)
            throw error
        }
    }

    private fun load(prepared: Prepared) = slot.publish(upload(prepared))

    private fun readModel(resources: ResourceProvider, name: String): List<PreparedPart> {
        val location = ResourceLocation("gtladditions", "models/effect/lighthunter/$name.bin.gz")
        DataInputStream(GZIPInputStream(resources.getResourceOrThrow(location).open()).buffered(1 shl 16)).use { data ->
            check(data.readInt() == 0x4c485031) { "Invalid Light Hunter model: $location" }
            val count = data.readInt()
            require(count in 1..32)
            return List(count) {
                val textureName = data.readUTF()
                val color = IntArray(4) { data.readUnsignedByte() }
                val vertices = data.readInt()
                require(vertices in 3..500000 && vertices % 3 == 0)
                val texture = if (textureName.isEmpty()) {
                    null
                } else {
                    resources.getResourceOrThrow(ResourceLocation(textureName)).open().use { it.readBytes() }
                }

                // Position, UV and normal of each vertex, in file order.
                val values = FloatArray(vertices * 8) {
                    data.readFloat().also { value -> require(value.isFinite()) { "Non-finite vertex in $location" } }
                }

                PreparedPart(texture, color, values)
            }
        }
    }

    private fun uploadPart(part: PreparedPart): Part {
        // The image format follows the file's content, not its name.
        val texture = part.texture?.let { bytes ->
            DynamicTexture(NativeImage.read(ByteArrayInputStream(bytes))).also { it.setFilter(true, false) }
        }

        try {
            return RenderUtils.withStagingBuilder { builder ->
                builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL)
                val v = part.vertices

                for (i in v.indices step 8) {
                    builder.vertex(v[i].toDouble(), v[i + 1].toDouble(), v[i + 2].toDouble()).uv(v[i + 3], v[i + 4])
                        .color(part.color[0], part.color[1], part.color[2], part.color[3]).normal(v[i + 5], v[i + 6], v[i + 7]).endVertex()
                }

                Part(RenderUtils.uploadStaticBuffer(builder), texture)
            }
        } catch (error: Exception) {
            texture?.close()
            throw error
        }
    }

    /** [texture] holds the image file's bytes; [vertices] eight floats per vertex. */
    private class PreparedPart(val texture: ByteArray?, val color: IntArray, val vertices: FloatArray)

    private class Prepared(val deathStar: List<PreparedPart>, val kamino: List<PreparedPart>)

    class Part(val buffer: VertexBuffer, val texture: DynamicTexture?) : AutoCloseable {
        override fun close() {
            buffer.close()
            texture?.close()
        }
    }

    class Loaded(val deathStar: List<Part>, val kamino: List<Part>) : AutoCloseable {
        override fun close() {
            deathStar.forEach(Part::close)
            kamino.forEach(Part::close)
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