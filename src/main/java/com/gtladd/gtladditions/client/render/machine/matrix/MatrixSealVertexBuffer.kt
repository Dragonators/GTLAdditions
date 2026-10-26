package com.gtladd.gtladditions.client.render.machine.matrix

import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.VertexBuffer
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL15C
import org.lwjgl.system.MemoryUtil
import java.nio.ByteOrder

/** The seal owns its element buffer; Minecraft's shared sequential indices are never changed. */
@OnlyIn(Dist.CLIENT)
internal class MatrixSealVertexBuffer(builder: BufferBuilder, indices: IntArray) : VertexBuffer(Usage.STATIC) {
    private val elements = GlStateManager._glGenBuffers()
    val elementCount = indices.size
    val elementType = if ((indices.maxOrNull() ?: 0) <= 0xffff) GL11C.GL_UNSIGNED_SHORT else GL11C.GL_UNSIGNED_INT
    private var closed = false

    init {
        try {
            val bytesPerIndex = if (elementType == GL11C.GL_UNSIGNED_SHORT) 2 else 4
            val data = MemoryUtil.memAlloc(elementCount * bytesPerIndex).order(ByteOrder.nativeOrder())
            try {
                for (index in indices) {
                    if (bytesPerIndex == 2) data.putShort(index.toShort()) else data.putInt(index)
                }
                data.flip()
                RenderUtils.withBoundBuffer(this) {
                    upload(builder.end())
                    // Replacing this VAO's binding does not modify the shared buffer used by upload.
                    GlStateManager._glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, elements)
                    GL15C.glBufferData(GL15C.GL_ELEMENT_ARRAY_BUFFER, data, GL15C.GL_STATIC_DRAW)
                }
            } finally {
                MemoryUtil.memFree(data)
            }
        } catch (error: Throwable) {
            close()
            throw error
        }
    }

    override fun draw() {
        GlStateManager._drawElements(GL11C.GL_TRIANGLES, elementCount, elementType, 0L)
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