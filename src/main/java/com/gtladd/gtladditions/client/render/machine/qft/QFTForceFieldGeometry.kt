package com.gtladd.gtladditions.client.render.machine.qft

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

internal object QFTForceFieldGeometry {
    private val corners = arrayOf(
        intArrayOf(3, 7),
        intArrayOf(-3, 7),
        intArrayOf(-7, 3),
        intArrayOf(-7, -3),
        intArrayOf(-3, -7),
        intArrayOf(3, -7),
        intArrayOf(7, -3),
        intArrayOf(7, 3)
    )

    fun center(pos: BlockPos, facing: Direction): Vec3 = Vec3(
        pos.x + 0.5 - facing.stepX * 3.0,
        pos.y.toDouble(),
        pos.z + 0.5 - facing.stepZ * 3.0
    )

    fun bounds(center: Vec3): AABB = AABB(
        center.x - 7.0,
        center.y,
        center.z - 7.0,
        center.x + 7.0,
        center.y + 4.0,
        center.z + 7.0
    )

    fun animationFrame(elapsedMillis: Long): Float = (elapsedMillis % 6000L) * (119.0f / 6000.0f)

    fun write(consumer: VertexConsumer, center: Vec3) {
        for (index in corners.indices) {
            val current = corners[index]
            val next = corners[(index + 1) % corners.size]
            val x0 = center.x + current[0]
            val z0 = center.z + current[1]
            val x1 = center.x + next[0]
            val z1 = center.z + next[1]
            consumer.vertex(x0, center.y + 4.0, z0).uv(1.0f, 1.0f).endVertex()
            consumer.vertex(x0, center.y, z0).uv(1.0f, 0.0f).endVertex()
            consumer.vertex(x1, center.y, z1).uv(0.0f, 0.0f).endVertex()
            consumer.vertex(x1, center.y + 4.0, z1).uv(0.0f, 1.0f).endVertex()
        }
    }
}