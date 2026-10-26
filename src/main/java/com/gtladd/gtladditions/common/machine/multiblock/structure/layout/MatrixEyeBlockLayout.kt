package com.gtladd.gtladditions.common.machine.multiblock.structure.layout

import com.gtladd.gtladditions.common.machine.multiblock.structure.StructureResourceLoader
import com.gtladd.gtladditions.common.machine.multiblock.structure.rotateStructureOffset
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

object MatrixEyeBlockLayout {
    private val offsets by lazy {
        val aisles = StructureResourceLoader.loadAisles("multiblock/apocalyptic_quantum_warp_matrix.bin", "apocalyptic_quantum_warp_matrix")
        buildList {
            // Factory pattern: columns = x + 81, rows = 88 - z, aisles = 127 - y.
            val ring = aisles[125] // Model-local y=2; controller y=1.

            for (z in -9..9) {
                for (x in -9..9) {
                    if (x * x + z * z < 9.5 * 9.5 && ring[88 - z][x + 81] == 'X') {
                        add(BlockPos(x, 1, z - 15))
                    }
                }
            }
        }
    }

    fun positions(controller: BlockPos, facing: Direction, flipped: Boolean): Set<BlockPos> = offsets.mapTo(LinkedHashSet()) {
        controller.offset(rotateStructureOffset(it, facing, flipped))
    }
}