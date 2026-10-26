package com.gtladd.gtladditions.common.machine.multiblock.structure.layout

import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection
import com.gtladd.gtladditions.common.machine.multiblock.structure.loadStructureOffsets
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

object CondensationCoreBlockLayout {
    private val coreOffsets by lazy {
        loadStructureOffsets(
            "multiblock/antientropy_condensation_center.bin",
            "antientropy_condensation_center",
            BlockPos(13, 15, 26)
        ) { _, symbol ->
            symbol == 'J' || symbol == 'M'
        }
    }

    fun positions(controller: BlockPos, facing: Direction, upwardsFacing: Direction, flipped: Boolean): Set<BlockPos> {
        val left = RelativeDirection.LEFT.getRelativeFacing(facing, upwardsFacing, flipped)
        val up = RelativeDirection.UP.getRelativeFacing(facing, upwardsFacing, flipped)
        return coreOffsets.mapTo(LinkedHashSet()) {
            controller.relative(left, it.x).relative(up, it.y).relative(facing, it.z)
        }
    }
}