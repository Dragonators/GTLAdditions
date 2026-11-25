package com.gtladd.gtladditions.client.render.machine.antichrist

import com.gtladd.gtladditions.client.RenderUtils
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexSorting
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.ShaderInstance
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL15C
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL30C
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.Arrays

@OnlyIn(Dist.CLIENT)
object RingTransparency {
    class Instance(val transforms: Array<Matrix4f>, val shades: Array<FloatArray>)

    private var sources: List<Buffer> = emptyList()
    private var baseQuads = 0
    private var mesh: Mesh? = null
    private var palette: RingBatchPalette? = null
    private val order = Order()
    private val identity = Matrix4f()

    fun install(buffers: List<Buffer>) {
        release()
        sources = buffers

        for (buffer in sources) {
            buffer.firstQuad = baseQuads
            baseQuads += buffer.centers.size
        }
    }

    fun release() {
        mesh?.close()
        mesh = null
        palette?.close()
        palette = null
        sources = emptyList()
        baseQuads = 0
    }

    fun render(instances: List<Instance>) {
        if (instances.isEmpty() || baseQuads == 0) return
        RenderUtils.withRenderType(RenderType.translucent()) {
            val original = RenderSystem.getShader() ?: return@withRenderType
            val shader = RingBatchShaders.current()
            if (shader == null) {
                renderGroups(instances, original)
                return@withRenderType
            }

            val parameters = palette ?: RingBatchPalette().also { palette = it }
            val binding = parameters.binding(shader)
            val geometry = mesh?.takeIf { it.instances >= instances.size } ?: run {
                val slots = maxOf(instances.size, (mesh?.instances ?: 0) * 2)
                mesh?.close()
                mesh = null
                Mesh(sources, slots, parameters.instancesPerBatch).also { mesh = it }
            }

            parameters.upload(instances)
            order.sort(instances, sources, baseQuads)
            geometry.uploadOrder(order, baseQuads, parameters.instancesPerBatch)
            geometry.palette = parameters
            geometry.binding = binding
            RenderUtils.withBoundBuffer(geometry) {
                // The private shader reads object matrices from the palette, not this uniform.
                geometry.drawWithShader(identity, RenderSystem.getProjectionMatrix(), shader)
            }
        }
    }

    private fun renderGroups(instances: List<Instance>, shader: ShaderInstance) {
        val color = RenderSystem.getShaderColor().copyOf()
        try {
            for (instance in instances) {
                for (source in sources) {
                    val shade = instance.shades[source.ring][source.group]
                    RenderSystem.setShaderColor(color[0] * shade, color[1] * shade, color[2] * shade, color[3])
                    RenderUtils.withBoundBuffer(source) {
                        source.drawWithShader(instance.transforms[source.ring], RenderSystem.getProjectionMatrix(), shader)
                    }
                }
            }
        } finally {
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3])
        }
    }

    class Order {
        private var keys = IntArray(0)
        private var scratch = IntArray(0)
        private val buckets = IntArray(256)
        private val position = Vector3f()
        var indices = IntArray(0)
            private set
        var size = 0
            private set

        fun sort(instances: List<Instance>, sources: List<Buffer>, baseQuads: Int) {
            size = instances.size * baseQuads
            if (indices.size < size) {
                val capacity = maxOf(size, indices.size * 2)
                indices = IntArray(capacity)
                scratch = IntArray(capacity)
                keys = IntArray(capacity)
            }

            var index = 0
            for (instance in instances) {
                for (source in sources) {
                    val transform = instance.transforms[source.ring]

                    for (center in source.centers) {
                        transform.transformPosition(center, position)
                        // Bits of a non-negative float sort like the float; MAX_VALUE minus them puts far quads first.
                        keys[index] = Int.MAX_VALUE - java.lang.Float.floatToIntBits(position.lengthSquared())
                        indices[index] = index
                        index++
                    }
                }
            }

            var from = indices
            var to = scratch

            for (shift in 0..24 step 8) {
                Arrays.fill(buckets, 0)
                for (i in 0 until size) buckets[(keys[from[i]] ushr shift) and 255]++
                var sum = 0

                for (i in buckets.indices) {
                    val count = buckets[i]
                    buckets[i] = sum
                    sum += count
                }

                for (i in 0 until size) {
                    val id = from[i]
                    to[buckets[(keys[id] ushr shift) and 255]++] = id
                }

                val swap = from
                from = to
                to = swap
            }

            indices = from
            scratch = to
        }
    }

    class Buffer private constructor(
        val ring: Int,
        val group: Int,
        val centers: Array<Vector3f>,
        val vertexFormat: VertexFormat,
        private var bytes: ByteBuffer?
    ) : VertexBuffer(Usage.STATIC) {
        var firstQuad = 0

        fun vertices(): ByteBuffer = checkNotNull(bytes).duplicate()

        override fun close() {
            try {
                super.close()
            } finally {
                bytes?.let(MemoryUtil::memFree)
                bytes = null
            }
        }

        companion object {
            fun upload(builder: BufferBuilder, ring: Int, group: Int): Buffer {
                val rendered = builder.end()
                var copy: ByteBuffer? = null
                var buffer: Buffer? = null
                var consumed = false
                try {
                    val state = rendered.drawState()
                    check(state.mode() == VertexFormat.Mode.QUADS && state.vertexCount() % 4 == 0)
                    val vertices = rendered.vertexBuffer()
                    val owned = MemoryUtil.memAlloc(vertices.remaining()).put(vertices).flip()
                    copy = owned
                    val stride = state.format().vertexSize
                    val centers = Array(state.vertexCount() / 4) { quad ->
                        val first = quad * 4 * stride
                        val opposite = first + 2 * stride
                        Vector3f(
                            (owned.getFloat(first) + owned.getFloat(opposite)) * 0.5f,
                            (owned.getFloat(first + 4) + owned.getFloat(opposite + 4)) * 0.5f,
                            (owned.getFloat(first + 8) + owned.getFloat(opposite + 8)) * 0.5f
                        )
                    }

                    val created = Buffer(ring, group, centers, state.format(), owned)
                    buffer = created
                    RenderUtils.withBoundBuffer(created) {
                        consumed = true
                        created.upload(rendered)
                    }

                    return created
                } catch (error: Throwable) {
                    if (buffer != null) buffer.close() else copy?.let(MemoryUtil::memFree)
                    throw error
                } finally {
                    if (!consumed) rendered.release()
                }
            }
        }
    }

    class Mesh(sources: List<Buffer>, val instances: Int, instancesPerBatch: Int) : VertexBuffer(Usage.STATIC) {
        private val metadata = GlStateManager._glGenBuffers()
        private val originalAttributes = HashMap<Int, AttributeState>()
        private var metadataShader: ShaderInstance? = null
        private var metadataLocation = -1
        private var indices: IntBuffer? = null
        private var runFirst = IntArray(0)
        private var runSize = IntArray(0)
        private var runBatch = IntArray(0)
        var runCount = 0
            private set
        var palette: RingBatchPalette? = null
        var binding = 0
        private var closed = false
        private val indexCapacity: Int

        init {
            val format = sources.first().vertexFormat
            check(sources.all { it.vertexFormat == format })
            val vertexCount = sources.sumOf { it.centers.size * 4 } * instances
            indexCapacity = vertexCount / 4 * 6
            val meta = MemoryUtil.memAllocFloat(vertexCount * 2)
            try {
                RenderUtils.withStagingBuilder { builder ->
                    builder.begin(VertexFormat.Mode.QUADS, format)

                    repeat(instances) { slot ->
                        for (source in sources) {
                            builder.putBulkData(source.vertices())

                            repeat(source.centers.size * 4) {
                                meta.put(((slot % instancesPerBatch) * 3 + source.ring).toFloat()).put(source.group.toFloat())
                            }
                        }
                    }

                    // The shared sequential index buffer must never be changed.
                    builder.setQuadSorting(VertexSorting.DISTANCE_TO_ORIGIN)
                    RenderUtils.withBoundBuffer(this) {
                        upload(builder.end())
                        meta.flip()
                        GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, metadata)
                        GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, meta, GL15C.GL_STATIC_DRAW)
                    }
                }
                indices = MemoryUtil.memAllocInt(indexCapacity)
            } catch (error: Throwable) {
                close()
                throw error
            } finally {
                MemoryUtil.memFree(meta)
            }
        }

        fun uploadOrder(order: Order, baseQuads: Int, instancesPerBatch: Int) {
            val singleBatch = order.size <= baseQuads * instancesPerBatch
            val capacity = if (singleBatch) 1 else order.size
            if (runFirst.size < capacity) {
                runFirst = IntArray(capacity)
                runSize = IntArray(capacity)
                runBatch = IntArray(capacity)
            }

            val data = checkNotNull(indices).clear()
            runCount = if (singleBatch && order.size > 0) 1 else 0
            if (runCount == 1) {
                runFirst[0] = 0
                runSize[0] = order.size * 6
                runBatch[0] = 0
            }

            for (index in 0 until order.size) {
                val quad = order.indices[index]
                if (!singleBatch) {
                    val batch = quad / baseQuads / instancesPerBatch
                    if (runCount == 0 || runBatch[runCount - 1] != batch) {
                        runFirst[runCount] = index * 6
                        runSize[runCount] = 0
                        runBatch[runCount] = batch
                        runCount++
                    }

                    runSize[runCount - 1] += 6
                }

                val vertex = quad * 4
                data.put(vertex).put(vertex + 1).put(vertex + 2).put(vertex + 2).put(vertex + 3).put(vertex)
            }

            data.flip()
            RenderUtils.withBoundBuffer(this) {
                GL15C.glBufferData(GL15C.GL_ELEMENT_ARRAY_BUFFER, indexCapacity * 4L, GL15C.GL_STREAM_DRAW)
                GL15C.glBufferSubData(GL15C.GL_ELEMENT_ARRAY_BUFFER, 0L, data)
            }
        }

        override fun drawWithShader(modelViewMatrix: Matrix4f, projectionMatrix: Matrix4f, shader: ShaderInstance) {
            if (!RenderSystem.isOnRenderThread()) {
                val modelView = Matrix4f(modelViewMatrix)
                val projection = Matrix4f(projectionMatrix)
                RenderSystem.recordRenderCall { drawWithShader(modelView, projection, shader) }
                return
            }

            if (metadataShader !== shader) {
                val location = GL20C.glGetAttribLocation(shader.id, RingBatchShaderSources.ATTRIBUTE)
                check(location >= 0) { "FOA shader has no ring metadata: ${shader.name}" }
                val previous = GL15C.glGetInteger(GL15C.GL_ARRAY_BUFFER_BINDING)
                try {
                    // Preserve the layout that native upload actually installed. Re-running
                    // format setup here could reinterpret Oculus's current global format flags.
                    if (metadataLocation >= 0) originalAttributes.getValue(metadataLocation).restore(metadataLocation)
                    originalAttributes.getOrPut(location) { AttributeState.capture(location) }
                    GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, metadata)
                    GlStateManager._enableVertexAttribArray(location)
                    GlStateManager._vertexAttribPointer(location, 2, GL11C.GL_FLOAT, false, 8, 0L)
                    metadataLocation = location
                    metadataShader = shader
                } finally {
                    GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, previous)
                }
            }

            super.drawWithShader(modelViewMatrix, projectionMatrix, shader)
        }

        private data class AttributeState(
            val buffer: Int,
            val size: Int,
            val type: Int,
            val normalized: Boolean,
            val integer: Boolean,
            val stride: Int,
            val offset: Long,
            val enabled: Boolean
        ) {
            fun restore(location: Int) {
                // Unused native slots have buffer 0. Disabling them preserves their generic
                // value without attempting an illegal client-array pointer in a core context.
                if (buffer != 0) {
                    GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, buffer)
                    if (integer) {
                        GlStateManager._vertexAttribIPointer(location, size, type, stride, offset)
                    } else {
                        GlStateManager._vertexAttribPointer(location, size, type, normalized, stride, offset)
                    }
                }

                if (enabled) {
                    GlStateManager._enableVertexAttribArray(location)
                } else {
                    GlStateManager._disableVertexAttribArray(location)
                }
            }

            companion object {
                fun capture(location: Int) = AttributeState(
                    GL20C.glGetVertexAttribi(location, GL20C.GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING),
                    GL20C.glGetVertexAttribi(location, GL20C.GL_VERTEX_ATTRIB_ARRAY_SIZE),
                    GL20C.glGetVertexAttribi(location, GL20C.GL_VERTEX_ATTRIB_ARRAY_TYPE),
                    GL20C.glGetVertexAttribi(location, GL20C.GL_VERTEX_ATTRIB_ARRAY_NORMALIZED) != 0,
                    GL20C.glGetVertexAttribi(location, GL30C.GL_VERTEX_ATTRIB_ARRAY_INTEGER) != 0,
                    GL20C.glGetVertexAttribi(location, GL20C.GL_VERTEX_ATTRIB_ARRAY_STRIDE),
                    GL20C.glGetVertexAttribPointer(location, GL20C.GL_VERTEX_ATTRIB_ARRAY_POINTER),
                    GL20C.glGetVertexAttribi(location, GL20C.GL_VERTEX_ATTRIB_ARRAY_ENABLED) != 0
                )
            }
        }

        override fun draw() {
            val parameters = checkNotNull(palette)
            parameters.withBinding(binding) {
                for (run in 0 until runCount) {
                    parameters.bind(binding, runBatch[run])
                    GlStateManager._drawElements(GL11C.GL_TRIANGLES, runSize[run], GL11C.GL_UNSIGNED_INT, runFirst[run] * 4L)
                }
            }
        }

        override fun close() {
            if (closed) return
            closed = true
            try {
                super.close()
            } finally {
                GlStateManager._glDeleteBuffers(metadata)
                metadataShader = null
                originalAttributes.clear()
                indices?.let(MemoryUtil::memFree)
                indices = null
            }
        }
    }
}
