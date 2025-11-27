package com.gtladd.gtladditions.utils

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

object IndustrialArrayPosHelper {

    private val BASE_OFFSETS = arrayOf(
        intArrayOf(-1, -100, -29),
        intArrayOf(-7, -100, -29),
        intArrayOf(-2, -100, -31),
        intArrayOf(-8, -100, -31),
        intArrayOf(-3, -100, -33),
        intArrayOf(-9, -100, -33),
        intArrayOf(-4, -100, -35),
        intArrayOf(-10, -100, -35),
        intArrayOf(-4, -100, -37),
        intArrayOf(-10, -100, -37),
        intArrayOf(-5, -100, -39),
        intArrayOf(-11, -100, -39),
        intArrayOf(-6, -100, -41),
        intArrayOf(-12, -100, -41),
        intArrayOf(-7, -100, -43),
        intArrayOf(-13, -100, -43),
        intArrayOf(-45, -100, -89),
        intArrayOf(-45, -100, -87),
        intArrayOf(-46, -100, -85),
        intArrayOf(-49, -100, -87),
        intArrayOf(-49, -100, -89),
        intArrayOf(-48, -100, -91),
        intArrayOf(-52, -100, -93),
        intArrayOf(-52, -100, -91),
        intArrayOf(-53, -100, -89),
        intArrayOf(-56, -100, -91),
        intArrayOf(-56, -100, -93),
        intArrayOf(-56, -100, -95),
        intArrayOf(-59, -100, -97),
        intArrayOf(-60, -100, -95),
        intArrayOf(-60, -100, -93),
        intArrayOf(-116, -100, -107),
        intArrayOf(-116, -100, -105),
        intArrayOf(-116, -100, -103),
        intArrayOf(-120, -100, -102),
        intArrayOf(-120, -100, -104),
        intArrayOf(-120, -100, -106),
        intArrayOf(-124, -100, -106),
        intArrayOf(-124, -100, -104),
        intArrayOf(-124, -100, -102),
        intArrayOf(-128, -100, -101),
        intArrayOf(-128, -100, -103),
        intArrayOf(-128, -100, -105),
        intArrayOf(-132, -100, -104),
        intArrayOf(-132, -100, -102),
        intArrayOf(-132, -100, -100),
        intArrayOf(-178, -100, -73),
        intArrayOf(-184, -100, -73),
        intArrayOf(-185, -100, -71),
        intArrayOf(-179, -100, -71),
        intArrayOf(-181, -100, -69),
        intArrayOf(-187, -100, -69),
        intArrayOf(-189, -100, -67),
        intArrayOf(-183, -100, -67),
        intArrayOf(-184, -100, -65),
        intArrayOf(-190, -100, -65),
        intArrayOf(-192, -100, -63),
        intArrayOf(-186, -100, -63),
        intArrayOf(-188, -100, -61),
        intArrayOf(-213, -100, -9),
        intArrayOf(-207, -100, -9),
        intArrayOf(-207, -100, -7),
        intArrayOf(-213, -100, -7),
        intArrayOf(-213, -100, -5),
        intArrayOf(-207, -100, -5),
        intArrayOf(-207, -100, -3),
        intArrayOf(-213, -100, -3),
        intArrayOf(-213, -100, -1),
        intArrayOf(-207, -100, -1),
        intArrayOf(-207, -100, 1),
        intArrayOf(-213, -100, 1),
        intArrayOf(-213, -100, 3),
        intArrayOf(-207, -100, 3),
        intArrayOf(-207, -100, 5),
        intArrayOf(-213, -100, 5),
        intArrayOf(-213, -100, 7),
        intArrayOf(-207, -100, 7),
        intArrayOf(-207, -100, 9),
        intArrayOf(-213, -100, 9),
        intArrayOf(-192, -100, 63),
        intArrayOf(-188, -100, 61),
        intArrayOf(-186, -100, 63),
        intArrayOf(-190, -100, 65),
        intArrayOf(-184, -100, 65),
        intArrayOf(-189, -100, 67),
        intArrayOf(-183, -100, 67),
        intArrayOf(-187, -100, 69),
        intArrayOf(-181, -100, 69),
        intArrayOf(-185, -100, 71),
        intArrayOf(-179, -100, 71),
        intArrayOf(-184, -100, 73),
        intArrayOf(-178, -100, 73),
        intArrayOf(-132, -100, 104),
        intArrayOf(-132, -100, 102),
        intArrayOf(-132, -100, 100),
        intArrayOf(-128, -100, 101),
        intArrayOf(-128, -100, 103),
        intArrayOf(-128, -100, 105),
        intArrayOf(-124, -100, 106),
        intArrayOf(-124, -100, 104),
        intArrayOf(-124, -100, 102),
        intArrayOf(-120, -100, 102),
        intArrayOf(-120, -100, 104),
        intArrayOf(-120, -100, 106),
        intArrayOf(-116, -100, 107),
        intArrayOf(-116, -100, 105),
        intArrayOf(-116, -100, 103),
        intArrayOf(-59, -100, 97),
        intArrayOf(-60, -100, 95),
        intArrayOf(-60, -100, 93),
        intArrayOf(-56, -100, 95),
        intArrayOf(-56, -100, 93),
        intArrayOf(-56, -100, 91),
        intArrayOf(-52, -100, 93),
        intArrayOf(-52, -100, 91),
        intArrayOf(-53, -100, 89),
        intArrayOf(-48, -100, 91),
        intArrayOf(-49, -100, 89),
        intArrayOf(-49, -100, 87),
        intArrayOf(-45, -100, 89),
        intArrayOf(-45, -100, 87),
        intArrayOf(-46, -100, 85),
        intArrayOf(-7, -100, 43),
        intArrayOf(-13, -100, 43),
        intArrayOf(-12, -100, 41),
        intArrayOf(-6, -100, 41),
        intArrayOf(-5, -100, 39),
        intArrayOf(-11, -100, 39),
        intArrayOf(-10, -100, 37),
        intArrayOf(-4, -100, 37),
        intArrayOf(-4, -100, 35),
        intArrayOf(-10, -100, 35),
        intArrayOf(-9, -100, 33),
        intArrayOf(-3, -100, 33),
        intArrayOf(-2, -100, 31),
        intArrayOf(-8, -100, 31),
        intArrayOf(-7, -100, 29),
        intArrayOf(-1, -100, 29)
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
                0 -> intArrayOf(x, y, z)        // EAST: no rotation
                1 -> intArrayOf(-z, y, x)       // SOUTH: 90° clockwise
                2 -> intArrayOf(-x, y, -z)      // WEST: 180°
                3 -> intArrayOf(z, y, -x)       // NORTH: 270° clockwise
                else -> intArrayOf(x, y, z)
            }
        }
    }
}