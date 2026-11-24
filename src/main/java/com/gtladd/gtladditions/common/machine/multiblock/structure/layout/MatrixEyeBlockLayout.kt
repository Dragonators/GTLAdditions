package com.gtladd.gtladditions.common.machine.multiblock.structure.layout

import com.gtladd.gtladditions.common.machine.multiblock.structure.StructureResourceLoader
import com.gtladd.gtladditions.common.machine.multiblock.structure.rotateStructureOffset
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

object MatrixEyeBlockLayout {
    private val aisles by lazy {
        StructureResourceLoader.loadAisles("multiblock/apocalyptic_quantum_warp_matrix.bin", "apocalyptic_quantum_warp_matrix")
    }

    /** Full pattern at a south-facing controller-relative offset. Its controller is at column 81, row 73, aisle 126. */
    internal fun structureSymbol(pos: BlockPos): Char = aisles[126 - pos.y][73 - pos.z][81 + pos.x]

    internal val offsets by lazy {
        buildList {
            for (z in -9..9) {
                for (x in -9..9) {
                    val pos = BlockPos(x, 1, z - 15)
                    if (x * x + z * z < 9.5 * 9.5 && structureSymbol(pos) == 'X') add(pos)
                }
            }
        }
    }

    fun positions(controller: BlockPos, facing: Direction, flipped: Boolean): Set<BlockPos> = offsets.mapTo(LinkedHashSet()) {
        controller.offset(rotateStructureOffset(it, facing, flipped))
    }
}