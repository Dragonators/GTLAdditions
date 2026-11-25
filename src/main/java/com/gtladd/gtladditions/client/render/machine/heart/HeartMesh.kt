package com.gtladd.gtladditions.client.render.machine.heart

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.ShaderInstance
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.lwjgl.opengl.ARBInstancedArrays
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL15C
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL30C
import org.lwjgl.opengl.GL31C
import org.lwjgl.opengl.GL33C
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

/** Built from direct buffers because a 1.20.1 BufferBuilder never frees its memory; attribute locations follow the format's order. */
@OnlyIn(Dist.CLIENT)
class HeartMesh private constructor(
    private val vao: Int,
    private val vbo: Int,
    private val ebo: Int,
    val indexCount: Int,
    private val primitive: Int
) : AutoCloseable {
    private var closed = false
    private var instanceSource = 0
    private var instanceOffset = -1
    private var instanceAttributes: IntArray? = null

    private fun clearInstances() {
        val attributes = instanceAttributes ?: return
        for (i in attributes) {
            if (GL.getCapabilities().OpenGL33) {
                GL33C.glVertexAttribDivisor(i, 0)
            } else {
                ARBInstancedArrays.glVertexAttribDivisorARB(i, 0)
            }
            GL20C.glDisableVertexAttribArray(i)
        }

        instanceAttributes = null
    }

    /** Expects the shader already applied. */
    fun drawRange(first: Int = 0, count: Int = indexCount) {
        if (count <= 0) return
        BufferUploader.invalidate()
        GlStateManager._glBindVertexArray(vao)
        clearInstances()
        GL11C.glDrawElements(primitive, count, GL11C.GL_UNSIGNED_INT, first.toLong() * 4)
        GlStateManager._glBindVertexArray(0)
    }

    /** [elements] stays owned by the caller. */
    fun drawElements(elements: Int, count: Int) {
        BufferUploader.invalidate()
        GlStateManager._glBindVertexArray(vao)
        clearInstances()
        try {
            GlStateManager._glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, elements)
            GL11C.glDrawElements(primitive, count, GL11C.GL_UNSIGNED_INT, 0L)
        } finally {
            GlStateManager._glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, ebo)
            GlStateManager._glBindVertexArray(0)
        }
    }

    /** Per instance two mat4 attributes, object-to-world then object-to-view (128 bytes). */
    fun drawInstanced(shader: ShaderInstance, instances: Int, offset: Int, count: Int, attributes: IntArray) {
        shader.apply()
        BufferUploader.invalidate()
        GlStateManager._glBindVertexArray(vao)
        try {
            if (instanceSource != instances || instanceOffset != offset || instanceAttributes !== attributes) {
                clearInstances()
                val previous = GL11C.glGetInteger(GL15C.GL_ARRAY_BUFFER_BINDING)
                try {
                    GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, instances)

                    for (i in 0..7) {
                        GL20C.glEnableVertexAttribArray(attributes[i])
                        GL20C.glVertexAttribPointer(attributes[i], 4, GL11C.GL_FLOAT, false, 128, offset.toLong() + i * 16)
                        if (GL.getCapabilities().OpenGL33) {
                            GL33C.glVertexAttribDivisor(attributes[i], 1)
                        } else {
                            ARBInstancedArrays.glVertexAttribDivisorARB(attributes[i], 1)
                        }
                    }

                    instanceSource = instances
                    instanceOffset = offset
                    instanceAttributes = attributes
                } finally {
                    GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, previous)
                }
            }
            GL31C.glDrawElementsInstanced(primitive, indexCount, GL11C.GL_UNSIGNED_INT, 0L, count)
        } finally {
            GlStateManager._glBindVertexArray(0)
            shader.clear()
        }
    }

    fun updateVertices(vertices: ByteBuffer) {
        val previous = GL11C.glGetInteger(GL15C.GL_ARRAY_BUFFER_BINDING)
        try {
            GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo)
            GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, vertices, GL15C.GL_STREAM_DRAW)
        } finally {
            GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, previous)
        }
    }

    fun draw(shader: ShaderInstance) {
        shader.apply()
        try {
            drawRange()
        } finally {
            shader.clear()
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        RenderSystem.assertOnRenderThread()
        GlStateManager._glDeleteBuffers(vbo)
        GlStateManager._glDeleteBuffers(ebo)
        GlStateManager._glDeleteVertexArrays(vao)
    }

    companion object {
        fun quads(format: VertexFormat, vertices: ByteBuffer, quads: Int): HeartMesh {
            val indices = MemoryUtil.memAllocInt(quads * 6)
            try {
                for (q in 0 until quads) {
                    val v = q * 4
                    indices.put(v).put(v + 1).put(v + 2).put(v + 2).put(v + 3).put(v)
                }

                indices.flip()
                return upload(format, vertices, MemoryUtil.memByteBuffer(indices), quads * 6)
            } finally {
                MemoryUtil.memFree(indices)
            }
        }

        /** [indices] are unsigned 32-bit ints. */
        fun upload(
            format: VertexFormat,
            vertices: ByteBuffer,
            indices: ByteBuffer,
            indexCount: Int,
            primitive: Int = GL11C.GL_TRIANGLES
        ): HeartMesh {
            RenderSystem.assertOnRenderThread()
            val previousVao = GL11C.glGetInteger(GL30C.GL_VERTEX_ARRAY_BINDING)
            val previousArray = GL11C.glGetInteger(GL15C.GL_ARRAY_BUFFER_BINDING)
            val vao = GlStateManager._glGenVertexArrays()
            val vbo = GlStateManager._glGenBuffers()
            val ebo = GlStateManager._glGenBuffers()
            try {
                BufferUploader.invalidate()
                GlStateManager._glBindVertexArray(vao)
                GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo)
                GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, vertices, GL15C.GL_STATIC_DRAW)
                format.setupBufferState()
                GlStateManager._glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, ebo)
                GL15C.glBufferData(GL15C.GL_ELEMENT_ARRAY_BUFFER, indices, GL15C.GL_STATIC_DRAW)
                GlStateManager._glBindVertexArray(0)
                GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0)
                return HeartMesh(vao, vbo, ebo, indexCount, primitive)
            } catch (error: Throwable) {
                GlStateManager._glBindVertexArray(0)
                GlStateManager._glDeleteBuffers(vbo)
                GlStateManager._glDeleteBuffers(ebo)
                GlStateManager._glDeleteVertexArrays(vao)
                throw error
            } finally {
                GlStateManager._glBindVertexArray(previousVao)
                GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, previousArray)
            }
        }
    }
}
