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
        var bounds: AABB? = null

        for (corner in 0..7) {
            val point = transform(
                if (corner and 1 == 0) min[0] else max[0],
                if (corner and 2 == 0) min[1] else max[1],
                if (corner and 4 == 0) min[2] else max[2]
            ).add(position.x + 0.5, position.y + 0.5, position.z + 0.5)
            val single = AABB(point, point)
            bounds = bounds?.minmax(single) ?: single
        }

        return requireNotNull(bounds)
    }
}
