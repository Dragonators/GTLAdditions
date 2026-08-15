package com.gtladd.gtladditions.common.machine.multiblock.controller.bs

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

object BiosphereIIIPosHelper {
    private val BASE_OFFSETS = arrayOf(
        intArrayOf(7, 0, 6),
        intArrayOf(7, 0, -6),
        intArrayOf(9, 0, 8),
        intArrayOf(9, 0, -8),
        intArrayOf(15, 0, 8),
        intArrayOf(15, 0, -8),
        intArrayOf(17, 0, 6),
        intArrayOf(17, 0, -6)
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
        val offsets = offsetsByDirection.getValue(hostFacing)
        return Array(offsets.size) { index ->
            val offset = offsets[index]
            hostPos.offset(offset[0], offset[1], offset[2])
        }
    }

    fun calculatePossibleHostPositions(modulePos: BlockPos): Array<BlockPos> {
        val result = mutableListOf<BlockPos>()
        for (offsets in offsetsByDirection.values) {
            for (offset in offsets) {
                result.add(modulePos.offset(-offset[0], -offset[1], -offset[2]))
            }
        }
        return result.toTypedArray()
    }

    private fun rotateOffsets(offsets: Array<IntArray>, rotation: Int): Array<IntArray> =
        Array(offsets.size) { index ->
            val (x, y, z) = offsets[index]
            when (rotation % 4) {
                0 -> intArrayOf(x, y, z)
                1 -> intArrayOf(-z, y, x)
                2 -> intArrayOf(-x, y, -z)
                else -> intArrayOf(z, y, -x)
            }
        }
}