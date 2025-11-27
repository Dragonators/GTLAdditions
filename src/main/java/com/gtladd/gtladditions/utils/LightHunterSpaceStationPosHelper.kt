package com.gtladd.gtladditions.utils

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

object LightHunterSpaceStationPosHelper {

    private val BASE_OFFSETS = arrayOf(
        intArrayOf(-41, 0, -9),
        intArrayOf(-49, 0, -9),
        intArrayOf(-57, 0, -9),
        intArrayOf(-65, 0, -9),
        intArrayOf(-73, 0, -9),
        intArrayOf(-81, 0, -9),
        intArrayOf(-89, 0, -9),
        intArrayOf(-97, 0, -9),
        intArrayOf(-105, 0, -9),
        intArrayOf(-113, 0, -9),
        intArrayOf(-113, 0, 9),
        intArrayOf(-105, 0, 9),
        intArrayOf(-97, 0, 9),
        intArrayOf(-89, 0, 9),
        intArrayOf(-81, 0, 9),
        intArrayOf(-73, 0, 9),
        intArrayOf(-65, 0, 9),
        intArrayOf(-57, 0, 9),
        intArrayOf(-49, 0, 9),
        intArrayOf(-41, 0, 9)
    )

    private val offsetsByDirection by lazy {
        mapOf(
            Direction.EAST to BASE_OFFSETS,
            Direction.SOUTH to rotateOffsets(BASE_OFFSETS, 1),
            Direction.WEST to rotateOffsets(BASE_OFFSETS, 2),
            Direction.NORTH to rotateOffsets(BASE_OFFSETS, 3)
        )
    }

    fun calculateModulePositions(hostPos: BlockPos, hostFacing: Direction): Array<BlockPos> {
        require(hostFacing.axis != Direction.Axis.Y) {
            "Host facing must be horizontal (NORTH, SOUTH, EAST, WEST), got: $hostFacing"
        }

        val offsets = offsetsByDirection[hostFacing]
            ?: throw IllegalArgumentException("Unsupported direction: $hostFacing")

        return Array(offsets.size) { i ->
            val offset = offsets[i]
            hostPos.offset(offset[0], offset[1], offset[2])
        }
    }

    fun calculatePossibleHostPositions(modulePos: BlockPos): Array<BlockPos> {
        val result = mutableListOf<BlockPos>()

        for ((_, offsets) in offsetsByDirection) {
            for (offset in offsets) {
                val hostPos = modulePos.offset(-offset[0], -offset[1], -offset[2])
                result.add(hostPos)
            }
        }

        return result.toTypedArray()
    }

    @Suppress("SameParameterValue")
    private fun rotateOffsets(offsets: Array<IntArray>, rotation: Int): Array<IntArray> {
        return Array(offsets.size) { i ->
            val (x, y, z) = offsets[i]
            when (rotation % 4) {
                0 -> intArrayOf(x, y, z)       // EAST: no rotation
                1 -> intArrayOf(-z, y, x)      // SOUTH: 90° clockwise
                2 -> intArrayOf(-x, y, -z)     // WEST: 180°
                3 -> intArrayOf(z, y, -x)      // NORTH: 270° clockwise
                else -> intArrayOf(x, y, z)
            }
        }
    }
}