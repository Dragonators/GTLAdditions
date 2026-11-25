package com.gtladd.gtladditions.client.render.machine.condensation

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

object CondensationCoreGeometry {
    // The containment generator is at (13, 15, 14), controller B at (13, 15, 26).
    // Keep the outer shell inside the first remaining structure blocks, 9 blocks from the center.
    const val CORE_SCALE = 5.4f
    const val SHELL_RADIUS = 5.5f

    fun center(controller: BlockPos, front: Direction): Vec3 =
        Vec3.atCenterOf(controller.relative(front.opposite, 12))

    fun bounds(center: Vec3): AABB = AABB(center, center).inflate(SHELL_RADIUS.toDouble())
}
