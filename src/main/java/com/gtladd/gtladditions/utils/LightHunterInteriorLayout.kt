package com.gtladd.gtladditions.utils

import com.gtladd.gtladditions.utils.module.LightHunterSpaceStationPosHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Rotation

/** Existing light slot numbers are a saved-world contract. */
object LightHunterInteriorLayout {
    const val CEILING_LIGHT_COUNT = 20
    const val LEGACY_LIGHT_COUNT = 40
    const val LIGHT_COUNT = 54
    const val FACTORY_MIN_X = -116.5f
    const val FACTORY_MAX_X = -37.5f
    const val CORRIDOR_MIN_X = -166.5f
    const val CORRIDOR_MAX_X = -36.5f
    const val RAIL_WIDTH = 0.0625f

    private val railExtensionX = intArrayOf(-121, -129, -137, -145, -153, -161, -165)

    private val lightOffsets = Direction.Plane.HORIZONTAL.associateWith { facing ->
        val modules = LightHunterSpaceStationPosHelper.calculateModulePositions(BlockPos.ZERO, facing)
        check(modules.size == CEILING_LIGHT_COUNT) {
            "Existing interior light slots require an explicit migration when module layout changes"
        }

        List(LIGHT_COUNT) { slot ->
            if (slot < LEGACY_LIGHT_COUNT) {
                val module = slot % CEILING_LIGHT_COUNT
                val inward = if (module < CEILING_LIGHT_COUNT / 2) facing.clockWise else facing.counterClockWise
                val ceiling = slot < CEILING_LIGHT_COUNT
                modules[module].relative(inward, if (ceiling) 2 else 7).above(if (ceiling) 2 else -3)
            } else {
                val extra = slot - LEGACY_LIGHT_COUNT
                val pos = BlockPos(railExtensionX[extra / 2], -3, if (extra % 2 == 0) -2 else 2)
                pos.rotate(
                    when (facing) {
                        Direction.EAST -> Rotation.NONE
                        Direction.SOUTH -> Rotation.CLOCKWISE_90
                        Direction.WEST -> Rotation.CLOCKWISE_180
                        Direction.NORTH -> Rotation.COUNTERCLOCKWISE_90
                        else -> error("Interior sources require a horizontal facing")
                    }
                )
            }
        }
    }

    fun lightOffset(slot: Int, facing: Direction): BlockPos {
        require(slot in 0 until LIGHT_COUNT)
        return lightOffsets.getValue(facing)[slot]
    }

    fun lightLevel(slot: Int): Int = if (slot < CEILING_LIGHT_COUNT) 14 else 11
}
