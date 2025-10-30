package com.gtladd.gtladditions.utils

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

object AntichristPosHelper {
    private const val BASE_DEPTH = 13
    private const val LAYER_DEPTH = 12
    private const val SIDE_OFFSET = 14
    private const val LAYER_COUNT = 4

    fun calculateModulePositions(hostPos: BlockPos, hostFacing: Direction): Array<BlockPos> {
        val result = mutableListOf<BlockPos>()

        val backward = hostFacing.opposite.normal

        val (rightDir, upDir) = getPerpendicularDirections(hostFacing)

        for (layer in 0 until LAYER_COUNT) {
            val depthOffset = BASE_DEPTH + layer * LAYER_DEPTH

            val layerCenter = hostPos.offset(
                backward.x * depthOffset,
                backward.y * depthOffset,
                backward.z * depthOffset
            )

            result.add(layerCenter.offset(rightDir.x * SIDE_OFFSET, rightDir.y * SIDE_OFFSET, rightDir.z * SIDE_OFFSET))
            result.add(layerCenter.offset(-rightDir.x * SIDE_OFFSET, -rightDir.y * SIDE_OFFSET, -rightDir.z * SIDE_OFFSET))
            result.add(layerCenter.offset(upDir.x * SIDE_OFFSET, upDir.y * SIDE_OFFSET, upDir.z * SIDE_OFFSET))
            result.add(layerCenter.offset(-upDir.x * SIDE_OFFSET, -upDir.y * SIDE_OFFSET, -upDir.z * SIDE_OFFSET))
        }

        return result.toTypedArray()
    }

    fun calculatePossibleHostPositions(modulePos: BlockPos, moduleFacing: Direction): Array<BlockPos> {
        val result = mutableListOf<BlockPos>()

        val moduleNormal = moduleFacing.normal
        val offsetToCenter = BlockPos(-moduleNormal.x * SIDE_OFFSET, -moduleNormal.y * SIDE_OFFSET, -moduleNormal.z * SIDE_OFFSET)

        for (hostFacing in arrayOf(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
            val backward = hostFacing.opposite.normal

            for (layer in 0 until LAYER_COUNT) {
                val depthOffset = BASE_DEPTH + layer * LAYER_DEPTH

                val layerCenter = modulePos.offset(offsetToCenter)

                val hostPos = layerCenter.offset(
                    -backward.x * depthOffset,
                    -backward.y * depthOffset,
                    -backward.z * depthOffset
                )

                result.add(hostPos)
            }
        }

        return result.toTypedArray()
    }

    private fun getPerpendicularDirections(facing: Direction): Pair<BlockPos, BlockPos> {
        return when (facing) {
            Direction.NORTH -> Pair(BlockPos(1, 0, 0), BlockPos(0, 1, 0))
            Direction.SOUTH -> Pair(BlockPos(-1, 0, 0), BlockPos(0, 1, 0))
            Direction.EAST -> Pair(BlockPos(0, 0, 1), BlockPos(0, 1, 0))
            Direction.WEST -> Pair(BlockPos(0, 0, -1), BlockPos(0, 1, 0))
            else -> Pair(BlockPos(1, 0, 0), BlockPos(0, 1, 0))
        }
    }
}