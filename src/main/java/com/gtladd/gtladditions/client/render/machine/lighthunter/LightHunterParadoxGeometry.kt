package com.gtladd.gtladditions.client.render.machine.lighthunter

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
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

    /** Relative to the controller: on the chart plane, shifted by the depth bias toward the eye. */
    fun bounds(facing: Direction): AABB {
        val matrix = LightHunterVfxGeometry.transform(facing)
        val x = LightHunterParadoxTiming.WINDOW_X
        val bias = LightHunterParadoxTiming.WINDOW_DEPTH_BIAS
        val y = LightHunterParadoxTiming.WINDOW_Y
        val z = LightHunterParadoxTiming.WINDOW_Z
        val a = matrix.transformPosition(Vector3f(x - bias, y - 14f, z - 14f))
        val b = matrix.transformPosition(Vector3f(x + bias, y + 14f, z + 14f))

        return AABB(a.x.toDouble(), a.y.toDouble(), a.z.toDouble(), b.x.toDouble(), b.y.toDouble(), b.z.toDouble())
    }
}