package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.api.client.scene.PartBounds
import com.gtladd.gtladditions.common.machine.multiblock.controller.HeartOfTheCosmos
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector3d
import org.joml.Vector3f
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL15C
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.util.WeakHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal class HeartFleetLines(plan: HeartFleetPlan) : AutoCloseable {
    class Arc(val first: Int, val count: Int, val min: Vector3f, val max: Vector3f)

    val arcs = ArrayList<Arc>()
    private val positions = FloatArray(plan.lines.size * 360 * 2 * 3)
    private var mesh: HeartMesh? = null

    private class Elements(val buffer: Int, val count: Int)
    private val selections = object : LinkedHashMap<Long, Elements>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Elements>): Boolean {
            if (size <= 32) return false
            GlStateManager._glDeleteBuffers(eldest.value.buffer)
            return true
        }
    }

    private class BoundsCache(val position: BlockPos, val facing: Direction, val boxes: List<DoubleArray>)
    private val boundsCache = WeakHashMap<HeartOfTheCosmos, BoundsCache>()

    init {
        var vertex = 0

        for (line in plan.lines) {
            for (segment in 0 until 12) {
                val first = vertex
                val min = Vector3f(Float.POSITIVE_INFINITY)
                val max = Vector3f(Float.NEGATIVE_INFINITY)

                for (k in segment * 30 until (segment + 1) * 30) {
                    for (j in k..k + 1) {
                        val angle = j * 2 * PI / 360
                        val p = Vector3f(
                            ((line.plane.u.x * cos(angle) + line.plane.v.x * sin(angle)) * line.radius).toFloat(),
                            ((line.plane.u.y * cos(angle) + line.plane.v.y * sin(angle)) * line.radius).toFloat(),
                            ((line.plane.u.z * cos(angle) + line.plane.v.z * sin(angle)) * line.radius).toFloat()
                        )
                        positions[vertex * 3] = p.x
                        positions[vertex * 3 + 1] = p.y
                        positions[vertex * 3 + 2] = p.z
                        min.min(p)
                        max.max(p)
                        vertex++
                    }
                }

                arcs.add(Arc(first, vertex - first, min, max))
            }
        }
    }

    fun bounds(index: Int, machine: HeartOfTheCosmos, study: Matrix4fc, heart: Vector3d, padding: Double): PartBounds {
        val cached = boundsCache[machine]
        val boxes = if (cached != null && cached.position == machine.pos && cached.facing == machine.frontFacing) {
            cached.boxes
        } else {
            val rotation = Matrix4f(study).setTranslation(0f, 0f, 0f)
            arcs.map { arc ->
                val min = Vector3f(Float.POSITIVE_INFINITY)
                val max = Vector3f(Float.NEGATIVE_INFINITY)
                val p = Vector3f()

                for (corner in 0..7) {
                    rotation.transformPosition(
                        if (corner and 1 == 0) arc.min.x else arc.max.x,
                        if (corner and 2 == 0) arc.min.y else arc.max.y,
                        if (corner and 4 == 0) arc.min.z else arc.max.z,
                        p
                    )
                    min.min(p)
                    max.max(p)
                }

                doubleArrayOf(heart.x + min.x, heart.y + min.y, heart.z + min.z, heart.x + max.x, heart.y + max.y, heart.z + max.z)
            }.also { boundsCache[machine] = BoundsCache(machine.pos, machine.frontFacing, it) }
        }

        val b = boxes[index]
        return PartBounds.box(b[0] - padding, b[1] - padding, b[2] - padding, b[3] + padding, b[4] + padding, b[5] + padding)
    }

    fun mesh(): HeartMesh {
        mesh?.let { return it }
        val count = positions.size / 3
        val vertices = MemoryUtil.memAlloc(count * DefaultVertexFormat.POSITION_COLOR.vertexSize)
        var indices: ByteBuffer? = null
        try {
            val elements = MemoryUtil.memAlloc(count * 4).also { indices = it }

            for (i in 0 until count) {
                vertices.putFloat(positions[i * 3]).putFloat(positions[i * 3 + 1]).putFloat(positions[i * 3 + 2]).putInt(-1)
                elements.putInt(i)
            }

            vertices.flip()
            elements.flip()
            return HeartMesh.upload(DefaultVertexFormat.POSITION_COLOR, vertices, elements, count, GL11C.GL_LINES).also { mesh = it }
        } finally {
            MemoryUtil.memFree(vertices)
            indices?.let(MemoryUtil::memFree)
        }
    }

    fun draw(selected: Long) {
        val mesh = mesh()
        val elements = selections.getOrPut(selected) {
            val count = arcs.indices.sumOf { if (selected and (1L shl it) != 0L) arcs[it].count else 0 }
            val data = MemoryUtil.memAllocInt(count)
            val previous = GL11C.glGetInteger(GL15C.GL_ARRAY_BUFFER_BINDING)
            val buffer = GlStateManager._glGenBuffers()
            try {
                for (i in arcs.indices) {
                    if (selected and (1L shl i) == 0L) continue
                    val arc = arcs[i]
                    for (index in arc.first until arc.first + arc.count) data.put(index)
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

        mesh.drawElements(elements.buffer, elements.count)
    }

    override fun close() {
        selections.values.forEach { GlStateManager._glDeleteBuffers(it.buffer) }
        selections.clear()
        mesh?.close()
        mesh = null
    }
}