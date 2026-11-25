package com.gtladd.gtladditions.client.render.machine.heart

import com.google.common.collect.ImmutableMap
import com.gtladd.gtladditions.GTLAdditions
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.platform.TextureUtil
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexFormatElement
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.server.packs.resources.ResourceProvider
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Vector3f
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL12C
import org.lwjgl.opengl.GL13C
import org.lwjgl.opengl.GL30C
import org.lwjgl.system.MemoryUtil
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.zip.GZIPInputStream

@OnlyIn(Dist.CLIENT)
object HeartFleetAssets {
    val CRAFT_FORMAT: VertexFormat = DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL

    /** Reserves the instance attributes at shader link time; the actual mesh still uses CRAFT_FORMAT. */
    val CRAFT_SHADER_FORMAT = VertexFormat(
        ImmutableMap.builder<String, VertexFormatElement>()
            .put("Position", DefaultVertexFormat.ELEMENT_POSITION)
            .put("UV0", DefaultVertexFormat.ELEMENT_UV0)
            .put("Color", DefaultVertexFormat.ELEMENT_COLOR)
            .put("Normal", DefaultVertexFormat.ELEMENT_NORMAL)
            .apply {
                for (i in 0..7) {
                    put(
                        if (i < 4) "InstanceModel$i" else "InstanceView${i - 4}",
                        VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 4)
                    )
                }
            }.build()
    )
    val STARKILLER_FORMAT: VertexFormat = DefaultVertexFormat.POSITION_COLOR_NORMAL
    private val location = GTLAdditions.id("models/effect/heart/fleet.bin.gz")
    private val iceLuminance = GTLAdditions.id("textures/effect/heart/europa_luminance.jpg")
    private val iceColour = GTLAdditions.id("textures/effect/heart/europa_colour.jpg")

    /** Maps are indices into [Loaded.textures], -1 for none; [metalMap] holds roughness in green and metal in blue. */
    class Material(
        val color: Vector3f,
        val map: Int,
        val emissive: Vector3f,
        val emap: Int,
        val cut: Float,
        val normalMap: Int,
        val normalScale: Float,
        val metalMap: Int,
        val metal: Float,
        val rough: Float,
        val worn: Boolean
    )

    class Group(val material: Int, val mesh: HeartMesh)

    class Model(
        val name: String,
        val length: Float,
        val box: FloatArray,
        val materials: List<Material>,
        val groups: List<Group>,
        val points: List<Vector3f>,
        val pointColor: Vector3f
    )

    class StarkillerPart(val name: String, val metal: Float, val rough: Float, val emission: Float, val mesh: HeartMesh)

    /** [wear] and [wearNormal] are laid by object-space projection, not by UV. */
    class Loaded(
        val textures: IntArray,
        val wear: Int,
        val wearNormal: Int,
        val models: Map<String, Model>,
        val starkiller: List<StarkillerPart>,
        val ice: Int,
        val iceColor: Int,
        val white: Int
    ) : AutoCloseable {
        override fun close() {
            GlStateManager._deleteTexture(white)
            models.values.forEach { model -> model.groups.forEach { it.mesh.close() } }
            starkiller.forEach { it.mesh.close() }
            textures.forEach(GlStateManager::_deleteTexture)
            GlStateManager._deleteTexture(ice)
            GlStateManager._deleteTexture(iceColor)
        }
    }

    private class PreparedMesh(val vertices: ByteBuffer, val indices: ByteBuffer, val indexCount: Int) {
        fun free() {
            MemoryUtil.memFree(vertices)
            MemoryUtil.memFree(indices)
        }
    }

    private class PreparedModel(
        val name: String,
        val length: Float,
        val box: FloatArray,
        val materials: List<Material>,
        val groups: List<Pair<Int, PreparedMesh>>,
        val points: List<Vector3f>,
        val pointColor: Vector3f
    )

    private class PreparedPart(val name: String, val metal: Float, val rough: Float, val emission: Float, val mesh: PreparedMesh)

    private class Prepared(
        val images: List<NativeImage>,
        val wear: IntArray,
        val models: List<PreparedModel>,
        val parts: List<PreparedPart>,
        val ice: NativeImage,
        val iceColor: NativeImage
    ) {
        fun free() {
            images.forEach(NativeImage::close)
            models.forEach { m -> m.groups.forEach { it.second.free() } }
            parts.forEach { it.mesh.free() }
            ice.close()
            iceColor.close()
        }
    }

    private var job: CompletableFuture<Prepared>? = null
    private var loaded: Loaded? = null
    private var failed = false

    fun current(): Loaded? {
        loaded?.let { return it }
        if (failed) return null
        val pending = job
            ?: CompletableFuture
                .supplyAsync({ read(Minecraft.getInstance().resourceManager) }, Util.backgroundExecutor())
                .also { job = it }
        if (!pending.isDone) return null
        job = null
        val prepared = try {
            pending.join()
        } catch (error: Exception) {
            GTLAdditions.LOGGER.error("Unable to read the Heart fleet", error)
            failed = true
            return null
        }

        try {
            loaded = upload(prepared)
        } catch (error: Exception) {
            GTLAdditions.LOGGER.error("Unable to upload the Heart fleet", error)
            failed = true
        } finally {
            prepared.free()
        }

        return loaded
    }

    fun release() {
        job?.thenAccept { it.free() }
        job = null
        loaded?.close()
        loaded = null
        failed = false
    }

    private fun read(resources: ResourceProvider): Prepared {
        val images = ArrayList<NativeImage>()
        val models = ArrayList<PreparedModel>()
        val parts = ArrayList<PreparedPart>()
        var wear = IntArray(0)
        try {
            DataInputStream(GZIPInputStream(resources.getResourceOrThrow(location).open()).buffered(1 shl 16)).use { input ->
                val magic = ByteArray(7)
                input.readFully(magic)
                check(String(magic, Charsets.US_ASCII) == "GTLAHF2") { "Invalid Heart fleet data: $location" }

                repeat(input.readUnsignedByte()) {
                    val bytes = ByteArray(input.readInt())
                    input.readFully(bytes)
                    images.add(NativeImage.read(ByteArrayInputStream(bytes)))
                }

                wear = IntArray(2) { input.readShort().toInt() }
                repeat(input.readUnsignedByte()) { models.add(readModel(input)) }
                repeat(input.readUnsignedByte()) { parts.add(readPart(input)) }
                check(input.read() == -1) { "Trailing bytes in $location" }
                val names = models.map { it.name }.toSet()
                check(
                    names.containsAll(listOf("tie", "interceptor", "advanced", "phantom", "decimator", "gozanti", "destroyer"))
                ) { "Missing Heart model" }

                for (model in models) {
                    for (material in model.materials) {
                        check(
                            listOf(material.map, material.emap, material.normalMap, material.metalMap)
                                .all { it == -1 || it in images.indices }
                        ) {
                            "Invalid Heart texture index in $location"
                        }
                    }
                }
            }

            val ice = resources.getResourceOrThrow(iceLuminance).open().use { NativeImage.read(it) }
            val color = try {
                resources.getResourceOrThrow(iceColour).open().use { NativeImage.read(it) }
            } catch (error: Exception) {
                ice.close()
                throw error
            }

            return Prepared(images, wear, models, parts, ice, color)
        } catch (error: Throwable) {
            images.forEach(NativeImage::close)
            models.forEach { m -> m.groups.forEach { it.second.free() } }
            parts.forEach { it.mesh.free() }
            throw error
        }
    }

    private fun DataInputStream.readVector() = Vector3f(readFloat(), readFloat(), readFloat())

    private fun readModel(input: DataInputStream): PreparedModel {
        val name = input.readUTF()
        val length = input.readFloat()
        val box = floatArrayOf(input.readFloat(), input.readFloat(), input.readFloat())
        val materials = List(input.readUnsignedByte()) {
            Material(
                input.readVector(),
                input.readShort().toInt(),
                input.readVector(),
                input.readShort().toInt(),
                input.readFloat(),
                input.readShort().toInt(),
                input.readFloat(),
                input.readShort().toInt(),
                input.readFloat(),
                input.readFloat(),
                input.readUnsignedByte() != 0
            )
        }

        val groups = ArrayList<Pair<Int, PreparedMesh>>()
        try {
            repeat(input.readUnsignedByte()) {
                val material = input.readUnsignedByte()
                check(material in materials.indices) { "Invalid Heart material index: $material" }
                val verts = input.readInt()
                val tris = input.readInt()
                val pos = FloatArray(verts * 3) { input.readFloat() }
                val nrm = ByteArray(verts * 3).also(input::readFully)
                val uv = FloatArray(verts * 2) { input.readFloat() }
                val rawIndices = IntArray(tris * 3) {
                    input.readInt().also { index -> check(index in 0 until verts) { "Invalid Heart vertex index: $index" } }
                }
                val stride = CRAFT_FORMAT.vertexSize
                val vertices = MemoryUtil.memAlloc(verts * stride)
                var indices: ByteBuffer? = null
                try {
                    for (v in 0 until verts) {
                        vertices.putFloat(pos[v * 3]).putFloat(pos[v * 3 + 1]).putFloat(pos[v * 3 + 2])
                        vertices.putFloat(uv[v * 2]).putFloat(uv[v * 2 + 1])
                        vertices.put(-1).put(-1).put(-1).put(-1)
                        vertices.put(nrm[v * 3]).put(nrm[v * 3 + 1]).put(nrm[v * 3 + 2]).put(0)
                    }

                    vertices.flip()
                    val elementData = MemoryUtil.memAlloc(rawIndices.size * 4).also { indices = it }
                    for (index in rawIndices) elementData.putInt(index)
                    elementData.flip()
                    groups.add(material to PreparedMesh(vertices, elementData, rawIndices.size))
                } catch (error: Throwable) {
                    MemoryUtil.memFree(vertices)
                    indices?.let(MemoryUtil::memFree)
                    throw error
                }
            }

            val points = List(input.readUnsignedShort()) { input.readVector() }
            return PreparedModel(name, length, box, materials, groups, points, input.readVector())
        } catch (error: Throwable) {
            groups.forEach { it.second.free() }
            throw error
        }
    }

    private fun readPart(input: DataInputStream): PreparedPart {
        val name = input.readUTF()
        val metal = input.readFloat()
        val rough = input.readFloat()
        val emission = input.readFloat()
        val verts = input.readInt()
        val count = input.readInt()
        val pos = ShortArray(verts * 3) { input.readShort() }
        val nrm = ByteArray(verts * 3).also(input::readFully)
        val col = ByteArray(verts * 4).also(input::readFully)
        val rawIndices = IntArray(count) {
            input.readInt().also { index -> check(index in 0 until verts) { "Invalid Heart vertex index: $index" } }
        }
        val vertices = MemoryUtil.memAlloc(verts * STARKILLER_FORMAT.vertexSize)
        var indices: ByteBuffer? = null
        try {
            val elements = MemoryUtil.memAlloc(count * 4).also { indices = it }
            val scale = 1.05f / 32767f

            for (v in 0 until verts) {
                vertices.putFloat(pos[v * 3] * scale).putFloat(pos[v * 3 + 1] * scale).putFloat(pos[v * 3 + 2] * scale)
                vertices.put(col[v * 4]).put(col[v * 4 + 1]).put(col[v * 4 + 2]).put(col[v * 4 + 3])
                vertices.put(nrm[v * 3]).put(nrm[v * 3 + 1]).put(nrm[v * 3 + 2]).put(0)
            }

            vertices.flip()
            for (index in rawIndices) elements.putInt(index)
            elements.flip()
            return PreparedPart(name, metal, rough, emission, PreparedMesh(vertices, elements, count))
        } catch (error: Throwable) {
            MemoryUtil.memFree(vertices)
            indices?.let(MemoryUtil::memFree)
            throw error
        }
    }

    private fun upload(prepared: Prepared): Loaded {
        RenderSystem.assertOnRenderThread()
        val textures = ArrayList<Int>()
        val meshes = ArrayList<HeartMesh>()
        try {
            prepared.images.forEach { textures.add(texture(it, repeat = true)) }
            val models = prepared.models.associate { m ->
                m.name to Model(
                    m.name,
                    m.length,
                    m.box,
                    m.materials,
                    m.groups.map { (material, mesh) ->
                        Group(
                            material,
                            HeartMesh.upload(CRAFT_FORMAT, mesh.vertices, mesh.indices, mesh.indexCount).also(meshes::add)
                        )
                    },
                    m.points,
                    m.pointColor
                )
            }

            val parts = prepared.parts.map { p ->
                StarkillerPart(
                    p.name,
                    p.metal,
                    p.rough,
                    p.emission,
                    HeartMesh.upload(STARKILLER_FORMAT, p.mesh.vertices, p.mesh.indices, p.mesh.indexCount).also(meshes::add)
                )
            }

            val ice = texture(prepared.ice, repeat = true).also(textures::add)
            val color = texture(prepared.iceColor, repeat = true).also(textures::add)
            val white = NativeImage(1, 1, false).use { image ->
                image.setPixelRGBA(0, 0, -1)
                texture(image, repeat = true)
            }.also(textures::add)

            val pictures = textures.dropLast(3).toIntArray()
            return Loaded(pictures, pictures[prepared.wear[0]], pictures[prepared.wear[1]], models, parts, ice, color, white)
        } catch (error: Throwable) {
            meshes.forEach(HeartMesh::close)
            textures.forEach(GlStateManager::_deleteTexture)
            throw error
        }
    }

    private fun texture(image: NativeImage, repeat: Boolean): Int {
        val previousActive = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE)
        GlStateManager._activeTexture(GL13C.GL_TEXTURE0)
        val previous = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D)
        val id = TextureUtil.generateTextureId()
        try {
            TextureUtil.prepareImage(id, 0, image.width, image.height)
            image.upload(0, 0, 0, 0, 0, image.width, image.height, false, false, false, false)
            GlStateManager._bindTexture(id)
            // prepareImage limits the texture to level 0; lift that before generating the chain.
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL12C.GL_TEXTURE_MAX_LEVEL, 1000)
            GL11C.glTexParameterf(GL11C.GL_TEXTURE_2D, GL12C.GL_TEXTURE_MAX_LOD, 1000f)
            GL30C.glGenerateMipmap(GL11C.GL_TEXTURE_2D)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_LINEAR_MIPMAP_LINEAR)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_LINEAR)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, if (repeat) GL11C.GL_REPEAT else GL12C.GL_CLAMP_TO_EDGE)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, if (repeat) GL11C.GL_REPEAT else GL12C.GL_CLAMP_TO_EDGE)
            return id
        } catch (error: Throwable) {
            GlStateManager._deleteTexture(id)
            throw error
        } finally {
            GlStateManager._bindTexture(previous)
            GlStateManager._activeTexture(previousActive)
        }
    }
}
