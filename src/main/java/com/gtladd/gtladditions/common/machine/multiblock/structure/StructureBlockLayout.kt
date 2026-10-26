package com.gtladd.gtladditions.common.machine.multiblock.structure

import com.gtladd.gtladditions.common.data.MachineInfo
import com.gtladd.gtladditions.common.data.StructureRenderType
import com.gtladd.gtladditions.common.machine.multiblock.structure.layout.AntichristBlockLayout
import com.gtladd.gtladditions.common.machine.multiblock.structure.layout.CondensationCoreBlockLayout
import com.gtladd.gtladditions.common.machine.multiblock.structure.layout.DimensionFocusBlockLayout
import com.gtladd.gtladditions.common.machine.multiblock.structure.layout.MatrixEyeBlockLayout
import com.gtladd.gtladditions.common.machine.multiblock.structure.layout.MatrixFrameBlockLayout
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

object StructureBlockLayout {
    fun positions(machine: MachineInfo): Set<BlockPos> {
        val controller = BlockPos.of(machine.posLong)
        return when (machine.type) {
            StructureRenderType.ANTICHRIST -> AntichristBlockLayout.positions(controller, machine.facing, machine.flipped)
            StructureRenderType.APOCALYPTIC_MATRIX -> MatrixFrameBlockLayout.positions(controller, machine.facing, machine.flipped) +
                MatrixEyeBlockLayout.positions(controller, machine.facing, machine.flipped)
            StructureRenderType.ANTIENTROPY_CONDENSATION -> CondensationCoreBlockLayout.positions(
                controller,
                machine.facing,
                machine.upwardsFacing,
                machine.flipped
            )
            StructureRenderType.DIMENSION_FOCUS -> DimensionFocusBlockLayout.positions(controller, machine.facing, machine.flipped)
        }
    }
}

internal fun loadStructureOffsets(
    resource: String,
    id: String,
    controller: BlockPos,
    include: (Int, Char) -> Boolean
): List<BlockPos> = buildList {
    StructureResourceLoader.loadAisles(resource, id).forEachIndexed { z, rows ->
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, symbol ->
                if (include(y, symbol)) add(BlockPos(x, y, z).subtract(controller))
            }
        }
    }
}

internal fun rotateStructureOffset(pos: BlockPos, facing: Direction, flipped: Boolean): BlockPos {
    val x = if (flipped) -pos.x else pos.x
    return when (facing) {
        Direction.WEST -> BlockPos(-pos.z, pos.y, x)
        Direction.NORTH -> BlockPos(-x, pos.y, -pos.z)
        Direction.EAST -> BlockPos(pos.z, pos.y, -x)
        else -> BlockPos(x, pos.y, pos.z)
    }
}