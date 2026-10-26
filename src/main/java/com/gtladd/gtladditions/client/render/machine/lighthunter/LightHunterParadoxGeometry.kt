package com.gtladd.gtladditions.client.render.machine.lighthunter

import com.mojang.blaze3d.vertex.VertexConsumer
import org.joml.Vector3f

object LightHunterParadoxGeometry {
    fun write(out: VertexConsumer) {
        val y = LightHunterParadoxTiming.WINDOW_Y
        val z = LightHunterParadoxTiming.WINDOW_Z
        // The shader places the aperture on the navigation chart's plane.
        val points = arrayOf(
            Vector3f(0f, y - 14f, z - 14f),
            Vector3f(0f, y + 14f, z - 14f),
            Vector3f(0f, y + 14f, z + 14f),
            Vector3f(0f, y - 14f, z + 14f)
        )

        for (p in points) {
            out.vertex(p.x.toDouble(), p.y.toDouble(), p.z.toDouble()).uv(0f, 0f).color(0.62f, 0.86f, 0.88f, 1f).endVertex()
        }
    }
}