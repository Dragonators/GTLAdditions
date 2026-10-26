package com.gtladd.gtladditions.client.render.machine.rrf

import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.world.phys.Vec3
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Render thread only. */
internal class RrfVertexWriter : AutoCloseable {
    private var scratch: ByteBuffer? = null
    private val littleEndian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN

    fun append(builder: BufferBuilder, mesh: RrfEffectMesh, placement: RrfEffectPlacement, relative: Vec3) {
        val vertexSize = DefaultVertexFormat.POSITION_TEX_COLOR.vertexSize
        val bytes = prepare(mesh.size / RrfEffectGeometry.STRIDE * vertexSize)
        var address = MemoryUtil.memAddress(bytes)
        val right = placement.right
        val up = placement.above
        val front = placement.forward
        val v = mesh.vertices

        for (i in 0 until mesh.size step RrfEffectGeometry.STRIDE) {
            val x = v[i].toDouble()
            val y = v[i + 1].toDouble()
            val z = v[i + 2].toDouble()
            MemoryUtil.memPutFloat(address, (relative.x + right.stepX * x + up.stepX * y + front.stepX * z).toFloat())
            MemoryUtil.memPutFloat(address + 4, (relative.y + right.stepY * x + up.stepY * y + front.stepY * z).toFloat())
            MemoryUtil.memPutFloat(address + 8, (relative.z + right.stepZ * x + up.stepZ * y + front.stepZ * z).toFloat())
            writeAttributes(address, v, i)
            address += vertexSize
        }

        builder.putBulkData(bytes)
    }

    fun appendArray(builder: BufferBuilder, mesh: RrfArrayMesh, relative: Vec3) {
        val vertexSize = DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL.vertexSize
        val bytes = prepare(mesh.size / RrfArrayGeometry.STRIDE * vertexSize)
        var address = MemoryUtil.memAddress(bytes)
        val v = mesh.vertices

        for (i in 0 until mesh.size step RrfArrayGeometry.STRIDE) {
            MemoryUtil.memPutFloat(address, (relative.x + v[i]).toFloat())
            MemoryUtil.memPutFloat(address + 4, (relative.y + v[i + 1]).toFloat())
            MemoryUtil.memPutFloat(address + 8, (relative.z + v[i + 2]).toFloat())
            writeAttributes(address, v, i)
            MemoryUtil.memPutInt(address + NORMAL_OFFSET, packBytes(normalByte(v[i + 9]), normalByte(v[i + 10]), normalByte(v[i + 11]), 0))
            address += vertexSize
        }

        builder.putBulkData(bytes)
    }

    private fun writeAttributes(address: Long, v: FloatArray, i: Int) {
        MemoryUtil.memPutFloat(address + UV_OFFSET, v[i + 7])
        MemoryUtil.memPutFloat(address + UV_OFFSET + Float.SIZE_BYTES, v[i + 8])
        // Match VertexConsumer.color(float...) truncation, including values outside 0..1.
        MemoryUtil.memPutInt(
            address + COLOR_OFFSET,
            packBytes((v[i + 3] * 255f).toInt(), (v[i + 4] * 255f).toInt(), (v[i + 5] * 255f).toInt(), (v[i + 6] * 255f).toInt())
        )
    }

    private fun normalByte(value: Float): Int = (value.coerceIn(-1f, 1f) * 127f).toInt()

    private fun packBytes(a: Int, b: Int, c: Int, d: Int): Int {
        val packed = (a and 255) or ((b and 255) shl 8) or ((c and 255) shl 16) or ((d and 255) shl 24)
        return if (littleEndian) packed else Integer.reverseBytes(packed)
    }

    private fun prepare(required: Int): ByteBuffer {
        var bytes = scratch
        if (bytes == null || bytes.capacity() < required) {
            bytes = MemoryUtil.memRealloc(bytes, maxOf(INITIAL_CAPACITY, required, (bytes?.capacity() ?: 0) * 2))
            scratch = bytes
        }

        bytes.clear()
        bytes.limit(required)
        return bytes
    }

    override fun close() {
        MemoryUtil.memFree(scratch)
        scratch = null
    }

    private companion object {
        const val INITIAL_CAPACITY = 1 shl 16
        const val UV_OFFSET = 12
        const val COLOR_OFFSET = 20
        const val NORMAL_OFFSET = 24
    }
}