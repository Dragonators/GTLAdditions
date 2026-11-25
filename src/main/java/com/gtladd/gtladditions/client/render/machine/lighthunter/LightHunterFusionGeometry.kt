package com.gtladd.gtladditions.client.render.machine.lighthunter

import com.mojang.blaze3d.vertex.VertexConsumer
import org.joml.Vector3f

object LightHunterFusionGeometry {
    const val MIN_X = -21.3f
    const val MAX_X = -14.7f
    const val EXTENT = 33.5f

    fun write(out: VertexConsumer) {
        val points = Array(8) { i ->
            Vector3f(if (i and 1 == 0) MIN_X else MAX_X, if (i and 2 == 0) -EXTENT else EXTENT, if (i and 4 == 0) -EXTENT else EXTENT)
        }

        val faces = arrayOf(
            intArrayOf(0, 4, 6, 2),
            intArrayOf(1, 3, 7, 5),
            intArrayOf(0, 1, 5, 4),
            intArrayOf(2, 6, 7, 3),
            intArrayOf(0, 2, 3, 1),
            intArrayOf(4, 5, 7, 6)
        )
        for (face in faces) {
            for (index in face) {
                val p = points[index]
                out.vertex(p.x.toDouble(), p.y.toDouble(), p.z.toDouble()).uv(0f, 0f).color(0.62f, 0.86f, 0.88f, 1f).endVertex()
            }
        }
    }
}
