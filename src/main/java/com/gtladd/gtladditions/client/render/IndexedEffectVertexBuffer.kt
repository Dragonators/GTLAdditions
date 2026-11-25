package com.gtladd.gtladditions.client.render

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.VertexBuffer
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL15C
import org.lwjgl.system.MemoryUtil

internal class IndexedEffectVertexBuffer : VertexBuffer(Usage.DYNAMIC) {
    private val elements = GlStateManager._glGenBuffers()
    private var uploaded = IntArray(0)
    private var count = 0
    var drawCount = 0
    private var closed = false

    fun uploadIndexed(vertices: BufferBuilder.RenderedBuffer, values: IntArray, size: Int = values.size) {
        upload(vertices)
        GlStateManager._glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, elements)

        if (size != count || (0 until size).any { uploaded[it] != values[it] }) {
            val data = MemoryUtil.memAllocInt(size)
            try {
                data.put(values, 0, size).flip()
                GL15C.glBufferData(GL15C.GL_ELEMENT_ARRAY_BUFFER, data, GL15C.GL_STATIC_DRAW)
            } finally {
                MemoryUtil.memFree(data)
            }

            uploaded = values.copyOf(size)
            count = size
        }

        drawCount = size
    }

    override fun draw() {
        GlStateManager._drawElements(GL11C.GL_TRIANGLES, drawCount, GL11C.GL_UNSIGNED_INT, 0L)
    }

    override fun close() {
        if (closed) return
        closed = true
        try {
            super.close()
        } finally {
            GlStateManager._glDeleteBuffers(elements)
        }
    }
}
