package com.gtladd.gtladditions.client.render.machine.dimensionfocus

import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockAndTintGetter
import kotlin.math.cos

object DimensionFocusOrbitLighting {
    // Opposite horizontal faces share Minecraft's diffuse brightness. Y-only rotation needs five batches.
    enum class FaceGroup {
        UNSHADED,
        UP,
        DOWN,
        X,
        Z;

        fun shade(level: BlockAndTintGetter, yawDegrees: Float): Float = when (this) {
            UNSHADED -> level.getShade(Direction.UP, false)
            UP -> level.getShade(Direction.UP, true)
            DOWN -> level.getShade(Direction.DOWN, true)
            X, Z -> {
                val cosine = cos(Math.toRadians(yawDegrees.toDouble())).toFloat()
                val xWeight = if (this == X) cosine * cosine else 1f - cosine * cosine
                // Continuous at quadrant boundaries; never quantize a rotating normal to a Direction.
                level.getShade(Direction.EAST, true) * xWeight + level.getShade(Direction.SOUTH, true) * (1f - xWeight)
            }
        }

        companion object {
            fun of(quad: BakedQuad): FaceGroup = when {
                !quad.isShade -> UNSHADED
                quad.direction == Direction.UP -> UP
                quad.direction == Direction.DOWN -> DOWN
                quad.direction.axis == Direction.Axis.X -> X
                else -> Z
            }
        }
    }
}
