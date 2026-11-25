package com.gtladd.gtladditions.client.render.machine.heart

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL15C
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.util.BitSet

/** Ordered batches only select indices and never overwrite vertices still in use. */
internal class HeartFleetSprites : AutoCloseable {
    private class Elements(val buffer: Int, val count: Int)
    private var mesh: HeartMesh? = null
    private var capacity = 0
    private val selections = object : LinkedHashMap<BitSet, Elements>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<BitSet, Elements>): Boolean {
            if (size <= 32) return false
            GlStateManager._glDeleteBuffers(eldest.value.buffer)
            return true
        }
    }

    fun upload(vertices: ByteBuffer, quads: Int) {
        if (quads > capacity) {
            val next = HeartMesh.quads(DefaultVertexFormat.POSITION_TEX_COLOR, vertices, quads)
            mesh?.close()
            mesh = next
            capacity = quads
        } else {
            checkNotNull(mesh).updateVertices(vertices)
        }
    }

    /** [selected] becomes a cache key; the caller must not mutate it afterwards. */
    fun draw(selected: BitSet) {
        val elements = selections.getOrPut(selected) {
            val count = selected.cardinality() * 6
            val data = MemoryUtil.memAllocInt(count)
            val previous = GL11C.glGetInteger(GL15C.GL_ARRAY_BUFFER_BINDING)
            val buffer = GlStateManager._glGenBuffers()
            try {
                var q = selected.nextSetBit(0)

                while (q >= 0) {
                    val v = q * 4
                    data.put(v).put(v + 1).put(v + 2).put(v + 2).put(v + 3).put(v)
                    q = selected.nextSetBit(q + 1)
                }

                data.flip()
                GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, buffer)
                GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, data, GL15C.GL_STATIC_DRAW)
                Elements(buffer, count)
            } catch (error: Throwable) {
                GlStateManager._glDeleteBuffers(buffer)
                throw error
            } finally {
                GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, previous)
                MemoryUtil.memFree(data)
            }
        }
        checkNotNull(mesh).drawElements(elements.buffer, elements.count)
    }

    override fun close() {
        selections.values.forEach { GlStateManager._glDeleteBuffers(it.buffer) }
        selections.clear()
        mesh?.close()
        mesh = null
        capacity = 0
    }
}
