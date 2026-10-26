package com.gtladd.gtladditions.common.machine.multiblock.structure.layout

import com.gtladd.gtladditions.common.machine.multiblock.structure.StructureResourceLoader
import com.gtladd.gtladditions.utils.CommonUtils
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

object AntichristBlockLayout {
    const val STAR_OFFSET_X = -122.0
    val BASE_DIRECTION: Direction = Direction.EAST

    val RINGS: Array<Array<Array<String>>> by lazy {
        StructureResourceLoader.loadRings("rings/forge_of_the_antichrist_rings.bin", "forge_of_the_antichrist_rings")
    }

    fun positions(controller: BlockPos, facing: Direction, flipped: Boolean): Set<BlockPos> = buildSet {
        for (ring in RINGS) {
            val starOffset = CommonUtils.getRotatedRenderPosition(
                BASE_DIRECTION,
                facing,
                if (ring.size == 94) STAR_OFFSET_X - 1 else STAR_OFFSET_X,
                0.0,
                0.0
            )

            for (x in ring.indices) {
                for (y in ring[x].indices) {
                    for (z in ring[x][y].indices) {
                        if (ring[x][y][z] !in 'C'..'K') continue
                        val zOffset = z - ring[0][0].length / 2
                        val offset = CommonUtils.getRotatedRenderPosition(
                            Direction.WEST,
                            facing,
                            (x - ring.size / 2).toDouble(),
                            (ring[0].size / 2 - y).toDouble(),
                            (if (flipped) -zOffset else zOffset).toDouble()
                        )
                        add(controller.offset(BlockPos.containing(starOffset)).offset(BlockPos.containing(offset)))
                    }
                }
            }
        }
    }
}