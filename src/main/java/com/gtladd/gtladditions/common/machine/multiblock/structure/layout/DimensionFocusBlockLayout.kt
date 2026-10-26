package com.gtladd.gtladditions.common.machine.multiblock.structure.layout

import com.gtladd.gtladditions.common.machine.multiblock.structure.StructureResourceLoader
import com.gtladd.gtladditions.common.machine.multiblock.structure.rotateStructureOffset
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

object DimensionFocusBlockLayout {
    private const val RESOURCE = "multiblock/dimension_focus_infinity_crafting_array.bin"
    private const val STRUCTURE_ID = "dimension_focus_infinity_crafting_array"
    private const val CENTER = 20
    private val CONTROLLER = BlockPos(20, 22, 9)
    val centerOffset: BlockPos = BlockPos(CENTER - CONTROLLER.x, CENTER - CONTROLLER.y, CONTROLLER.z - CENTER)

    data class Cell(val position: BlockPos, val symbol: Char)

    val layers: List<List<Cell>> by lazy {
        val groups = List(2) { ArrayList<Cell>() }
        StructureResourceLoader.loadAisles(RESOURCE, STRUCTURE_ID).forEachIndexed { z, rows ->
            rows.forEachIndexed { y, row ->
                row.forEachIndexed { x, symbol ->
                    if (symbol in "ADKLM" && y !in 18..22) {
                        groups[if (y > CENTER) 0 else 1].add(
                            Cell(BlockPos(x - CENTER, y - CENTER, CENTER - z), symbol)
                        )
                    }
                }
            }
        }

        groups.map { it.toList() }
    }

    fun center(controller: BlockPos, facing: Direction, flipped: Boolean = false): BlockPos =
        controller.offset(rotateStructureOffset(centerOffset, facing, flipped))

    fun positions(controller: BlockPos, facing: Direction, flipped: Boolean): Set<BlockPos> = buildSet {
        val center = center(controller, facing, flipped)

        for (layer in layers) {
            for ((position) in layer) {
                add(center.offset(rotateStructureOffset(position, facing, flipped)))
            }
        }
    }
}