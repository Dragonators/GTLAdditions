package com.gtladd.gtladditions.client.render.machine.heart

import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal object HeartBlackHoleQuadBuffers {
    private const val SPHERE_LONGITUDE_SEGMENTS = 48
    private const val SPHERE_LATITUDE_SEGMENTS = 24

    val heartVolumeBuffer: VertexBuffer by lazy {
        buildSphereVolumeBuffer()
    }

    val screenQuadBuffer: VertexBuffer by lazy {
        buildScreenQuadBuffer()
    }

    private fun buildSphereVolumeBuffer(): VertexBuffer {
        val buffer = VertexBuffer(VertexBuffer.Usage.STATIC)
        val builder = Tesselator.getInstance().builder
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION)

        for (latitude in 0 until SPHERE_LATITUDE_SEGMENTS) {
            val latitude0 = -PI / 2.0 + PI * latitude / SPHERE_LATITUDE_SEGMENTS
            val latitude1 = -PI / 2.0 + PI * (latitude + 1) / SPHERE_LATITUDE_SEGMENTS

            for (longitude in 0 until SPHERE_LONGITUDE_SEGMENTS) {
                val longitude0 = 2.0 * PI * longitude / SPHERE_LONGITUDE_SEGMENTS
                val longitude1 = 2.0 * PI * (longitude + 1) / SPHERE_LONGITUDE_SEGMENTS

                val southWest = spherePoint(latitude0, longitude0)
                val northWest = spherePoint(latitude1, longitude0)
                val northEast = spherePoint(latitude1, longitude1)
                val southEast = spherePoint(latitude0, longitude1)

                addVertex(builder, southWest)
                addVertex(builder, northWest)
                addVertex(builder, northEast)
                addVertex(builder, southWest)
                addVertex(builder, northEast)
                addVertex(builder, southEast)
            }
        }

        buffer.bind()
        buffer.upload(builder.end())
        VertexBuffer.unbind()
        return buffer
    }

    private fun spherePoint(latitude: Double, longitude: Double): SphereVertex {
        val y = sin(latitude)
        val horizontalRadius = cos(latitude)
        return SphereVertex(
            (horizontalRadius * cos(longitude)).toFloat(),
            y.toFloat(),
            (horizontalRadius * sin(longitude)).toFloat()
        )
    }

    private fun addVertex(builder: BufferBuilder, vertex: SphereVertex) {
        builder.vertex(vertex.x.toDouble(), vertex.y.toDouble(), vertex.z.toDouble()).endVertex()
    }

    private fun buildScreenQuadBuffer(): VertexBuffer {
        val buffer = VertexBuffer(VertexBuffer.Usage.STATIC)
        val builder = Tesselator.getInstance().builder
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX)

        addScreenVertex(builder, -1.0f, -1.0f, 0.0f, 0.0f)
        addScreenVertex(builder, 1.0f, -1.0f, 1.0f, 0.0f)
        addScreenVertex(builder, 1.0f, 1.0f, 1.0f, 1.0f)
        addScreenVertex(builder, -1.0f, -1.0f, 0.0f, 0.0f)
        addScreenVertex(builder, 1.0f, 1.0f, 1.0f, 1.0f)
        addScreenVertex(builder, -1.0f, 1.0f, 0.0f, 1.0f)

        buffer.bind()
        buffer.upload(builder.end())
        VertexBuffer.unbind()
        return buffer
    }

    private fun addScreenVertex(builder: BufferBuilder, x: Float, y: Float, u: Float, v: Float) {
        builder.vertex(x.toDouble(), y.toDouble(), 0.0).uv(u, v).endVertex()
    }

    private data class SphereVertex(
        val x: Float,
        val y: Float,
        val z: Float
    )
}