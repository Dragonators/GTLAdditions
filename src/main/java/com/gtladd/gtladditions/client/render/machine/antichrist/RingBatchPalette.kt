package com.gtladd.gtladditions.client.render.machine.antichrist

import com.mojang.blaze3d.platform.GlStateManager
import net.minecraft.client.renderer.ShaderInstance
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.lwjgl.opengl.GL15C
import org.lwjgl.opengl.GL31C
import org.lwjgl.opengl.GL32C
import org.lwjgl.system.MemoryUtil
import java.util.WeakHashMap

class RingBatchPalette : AutoCloseable {
    val capacity = RingBatchShaders.capacity()
    val instancesPerBatch = capacity / 3
    private val buffers = ArrayList<Int>()
    private val data = MemoryUtil.memAllocFloat(capacity * RingBatchShaderSources.BYTES_PER_RING / 4)
    private val inverse = Matrix4f()
    private val normal = Matrix3f()
    private val normal4 = Matrix4f()
    private val bindings = WeakHashMap<ShaderInstance, Int>()
    private var closed = false

    fun binding(shader: ShaderInstance): Int = bindings.getOrPut(shader) {
        val block = GL31C.glGetUniformBlockIndex(shader.id, RingBatchShaderSources.BLOCK)
        check(block != GL31C.GL_INVALID_INDEX) { "FOA shader has no ring palette: ${shader.name}" }
        val used = HashSet<Int>()
        val count = GL31C.glGetProgrami(shader.id, GL31C.GL_ACTIVE_UNIFORM_BLOCKS)
        for (i in 0 until count) if (i != block) used.add(GL31C.glGetActiveUniformBlocki(shader.id, i, GL31C.GL_UNIFORM_BLOCK_BINDING))
        val slot = (0 until GL31C.glGetInteger(GL31C.GL_MAX_UNIFORM_BUFFER_BINDINGS)).last { it !in used }
        GL31C.glUniformBlockBinding(shader.id, block, slot)
        slot
    }

    fun upload(instances: List<RingTransparency.Instance>) {
        val previous = GL15C.glGetInteger(GL31C.GL_UNIFORM_BUFFER_BINDING)
        try {
            val batches = (instances.size + instancesPerBatch - 1) / instancesPerBatch
            while (buffers.size < batches) buffers.add(GlStateManager._glGenBuffers())

            repeat(batches) { batch ->
                data.clear()
                val end = minOf(instances.size, (batch + 1) * instancesPerBatch)

                for (instance in instances.subList(batch * instancesPerBatch, end)) {
                    // A std140 record is three mat4s and two vec4s (224 bytes).
                    for (ring in 0..2) {
                        val matrix = instance.transforms[ring]
                        putMatrix(matrix)
                        putMatrix(inverse.set(matrix).invert())
                        normal.set(matrix).invert().transpose()
                        putMatrix(normal4.identity().set3x3(normal))
                        data.put(instance.shades[ring]).put(0f)
                    }
                }

                data.flip()
                GlStateManager._glBindBuffer(GL31C.GL_UNIFORM_BUFFER, buffers[batch])
                GL15C.glBufferData(GL31C.GL_UNIFORM_BUFFER, capacity.toLong() * RingBatchShaderSources.BYTES_PER_RING, GL15C.GL_STREAM_DRAW)
                GL15C.glBufferSubData(GL31C.GL_UNIFORM_BUFFER, 0L, data)
            }
        } finally {
            GlStateManager._glBindBuffer(GL31C.GL_UNIFORM_BUFFER, previous)
        }
    }

    private fun putMatrix(matrix: Matrix4f) {
        matrix.get(data.position(), data)
        data.position(data.position() + 16)
    }

    fun bind(slot: Int, batch: Int) = GL31C.glBindBufferBase(GL31C.GL_UNIFORM_BUFFER, slot, buffers[batch])

    fun withBinding(slot: Int, draw: () -> Unit) {
        val generic = GL15C.glGetInteger(GL31C.GL_UNIFORM_BUFFER_BINDING)
        val previous = GL31C.glGetIntegeri(GL31C.GL_UNIFORM_BUFFER_BINDING, slot)
        val offset = if (previous == 0) 0L else GL32C.glGetInteger64i(GL31C.GL_UNIFORM_BUFFER_START, slot)
        val size = if (previous == 0) 0L else GL32C.glGetInteger64i(GL31C.GL_UNIFORM_BUFFER_SIZE, slot)
        try {
            draw()
        } finally {
            if (previous != 0 && size > 0) {
                GL31C.glBindBufferRange(GL31C.GL_UNIFORM_BUFFER, slot, previous, offset, size)
            } else {
                GL31C.glBindBufferBase(GL31C.GL_UNIFORM_BUFFER, slot, previous)
            }
            if (generic != previous) GlStateManager._glBindBuffer(GL31C.GL_UNIFORM_BUFFER, generic)
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        buffers.forEach(GlStateManager::_glDeleteBuffers)
        buffers.clear()
        MemoryUtil.memFree(data)
        bindings.clear()
    }
}