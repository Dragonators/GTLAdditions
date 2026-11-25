package com.gtladd.gtladditions.client.render.machine.rrf

import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/** Uses the same relative axes as the BACK / UP / RIGHT module pattern. */
class RrfEffectPlacement(front: Direction, up: Direction, flipped: Boolean, array: Boolean) {
    val right: Direction = if (array) Direction.EAST else RelativeDirection.RIGHT.getRelativeFacing(front, up, flipped)
    val above: Direction = if (array) Direction.UP else RelativeDirection.UP.getRelativeFacing(front, up, flipped)
    val forward: Direction = if (array) Direction.SOUTH else RelativeDirection.FRONT.getRelativeFacing(front, up, flipped)

    fun transform(x: Double, y: Double, z: Double): Vec3 = Vec3(
        right.stepX * x + above.stepX * y + forward.stepX * z,
        right.stepY * x + above.stepY * y + forward.stepY * z,
        right.stepZ * x + above.stepZ * y + forward.stepZ * z
    )

    fun bounds(position: BlockPos, array: Boolean): AABB {
        val min = if (array) doubleArrayOf(-31.0, 0.6, -31.0) else doubleArrayOf(-3.7, -3.7, -17.2)
        val max = if (array) doubleArrayOf(31.0, 3.0, 31.0) else doubleArrayOf(3.7, 3.7, 0.7)
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var minZ = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        var maxZ = Double.NEGATIVE_INFINITY

        for (corner in 0..7) {
            val lx = if (corner and 1 == 0) min[0] else max[0]
            val ly = if (corner and 2 == 0) min[1] else max[1]
            val lz = if (corner and 4 == 0) min[2] else max[2]
            val x = position.x + 0.5 + right.stepX * lx + above.stepX * ly + forward.stepX * lz
            val y = position.y + 0.5 + right.stepY * lx + above.stepY * ly + forward.stepY * lz
            val z = position.z + 0.5 + right.stepZ * lx + above.stepZ * ly + forward.stepZ * lz
            minX = minOf(minX, x)
            minY = minOf(minY, y)
            minZ = minOf(minZ, z)
            maxX = maxOf(maxX, x)
            maxY = maxOf(maxY, y)
            maxZ = maxOf(maxZ, z)
        }

        return AABB(minX, minY, minZ, maxX, maxY, maxZ)
    }
}
