package com.gtladd.gtladditions.client.render.machine.dimensionfocus

import com.gtladd.gtladditions.client.render.machine.DimensionFocusInfinityCraftingArrayRenderer.Companion.ORBIT_PERIOD_SECONDS
import com.gtladd.gtladditions.common.machine.multiblock.structure.layout.DimensionFocusBlockLayout
import com.gtladd.gtladditions.common.machine.multiblock.structure.layout.DimensionFocusBlockLayout.layers
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow

object DimensionFocusOrbitGeometry {
    private val orbitBounds: AABB by lazy {
        val radius = layers.maxOf { layer -> layer.maxOf { hypot(abs(it.position.x) + 0.5, abs(it.position.z) + 0.5) } }
        val minY = layers.minOf { layer -> layer.minOf { it.position.y } }
        val maxY = layers.maxOf { layer -> layer.maxOf { it.position.y } }
        AABB(0.5 - radius, minY.toDouble(), 0.5 - radius, 0.5 + radius, maxY + 1.0, 0.5 + radius)
    }

    fun yaw(facing: Direction): Float = when (facing) {
        Direction.WEST -> -90f
        Direction.NORTH -> 180f
        Direction.EAST -> 90f
        else -> 0f
    }

    fun angleDegrees(layer: Int, ticks: Double): Float {
        val periodTicks = ORBIT_PERIOD_SECONDS * 20.0
        val degrees = (ticks % periodTicks) / periodTicks * 360.0
        // Negative Y rotation is clockwise viewed from above.
        return (if (layer == 0) -degrees else degrees).toFloat()
    }

    /** The petals line up every 45 degrees of turn. */
    fun alignment(ticks: Double): Float {
        val relative = Math.toRadians((angleDegrees(1, ticks) - angleDegrees(0, ticks)).toDouble())
        return ((1.0 + cos(4.0 * relative)) * 0.5).pow(6).toFloat()
    }

    fun bounds(controller: BlockPos, facing: Direction): AABB =
        orbitBounds.move(DimensionFocusBlockLayout.center(controller, facing))
}