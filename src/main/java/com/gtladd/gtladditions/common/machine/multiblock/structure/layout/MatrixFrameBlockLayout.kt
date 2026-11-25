package com.gtladd.gtladditions.common.machine.multiblock.structure.layout

import com.gtladd.gtladditions.common.machine.multiblock.structure.loadStructureOffsets
import com.gtladd.gtladditions.common.machine.multiblock.structure.rotateStructureOffset
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

object MatrixFrameBlockLayout {
    private val frameOffsets by lazy {
        loadStructureOffsets(
            "masks/apocalyptic_matrix_frames.bin",
            "apocalyptic_matrix_frames",
            BlockPos(29, -29, 47)
        ) { _, symbol -> symbol != ' ' }
    }

    fun positions(controller: BlockPos, facing: Direction, flipped: Boolean): Set<BlockPos> = frameOffsets.mapTo(LinkedHashSet()) {
        controller.offset(rotateStructureOffset(it, facing, flipped))
    }
}
