package com.gtladd.gtladditions.common.machine.multiblock.structure.layout

import com.gtladd.gtladditions.api.pattern.StructureTierLayout
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine
import com.gtladd.gtladditions.common.machine.multiblock.structure.StructureResourceLoader
import com.gtladd.gtladditions.common.machine.multiblock.structure.rotateStructureOffset
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

object HeartBlockLayout {
    private const val RESOURCE = "multiblock/heart_of_the_cosmos.bin"
    private const val STRUCTURE_ID = "heart_of_the_cosmos"
    const val SHELL_SYMBOLS = "WwFSBATUGLDRQPMO"
    const val FILLER = '.'

    /** Console: 'c' layer under the controller (takes the parts), 'r' layer below, 'n' core, 'g' hollow, 't' rim, 'l' seam, 'e' face. */
    val BLOCK_IDS: Map<Char, String> = mapOf(
        'W' to "gtladditions:gravity_stabilization_casing",
        'w' to "gtladditions:temporal_anchor_field_casing",
        'F' to "gtlcore:enhance_hyper_mechanical_casing",
        'S' to "gtlcore:qft_coil",
        'A' to "gtlcore:dimensionally_transcendent_casing",
        'B' to "gtlcore:dimensionally_transcendent_casing",
        'T' to "gtlcore:dimensionally_transcendent_casing",
        'U' to "gtlcore:dimensionally_transcendent_casing",
        'D' to "gtlcore:dimensionally_transcendent_casing",
        'G' to "gtlcore:dimension_connection_casing",
        'M' to "gtlcore:dimension_connection_casing",
        'L' to "kubejs:dimensional_bridge_casing",
        'R' to "gtlcore:iridium_casing",
        'P' to "gtlcore:iridium_casing",
        'Q' to "kubejs:dimensional_stability_casing",
        'O' to "kubejs:dimensional_stability_casing",
        'c' to "gtlcore:iridium_casing",
        'r' to "gtlcore:iridium_casing",
        't' to "gtlcore:enhance_hyper_mechanical_casing",
        'l' to "kubejs:dimensional_bridge_casing",
        'e' to "gtladditions:gravity_stabilization_casing",
        'n' to "gtlcore:dimension_connection_casing",
        'g' to "gtladditions:spatially_transcendent_gravitational_lens"
    )

    /** Pattern column, row and aisle of the controller; aisles run toward its front. */
    private val CONTROLLER = intArrayOf(82, 3, 220)

    /** Controller-relative, for a controller facing south. */
    val centerOffset: BlockPos = BlockPos(0, 115, -105)

    /** [position] from the black hole's centre cell, for a controller facing south; pattern order (aisle, row, column). */
    data class Cell(val position: BlockPos, val symbol: Char)

    val cells: List<Cell> by lazy { collect { it in SHELL_SYMBOLS } }

    /** The shells lie at radii 39–42, 55–58, 79–83 and 110–115, so 68 and 96 split them into tiers 1 to 3. */
    val tiers = StructureTierLayout(1, 3, { MultiBlockMachine.HEART_OF_THE_COSMOS.patternFactory.get() }) { aisle, row, column ->
        if (StructureResourceLoader.loadAisles(RESOURCE, STRUCTURE_ID)[aisle][row][column] !in SHELL_SYMBOLS) return@StructureTierLayout 0
        val x = column - CONTROLLER[0] - centerOffset.x
        val y = row - CONTROLLER[1] - centerOffset.y
        val z = aisle - CONTROLLER[2] - centerOffset.z
        val distance = x * x + y * y + z * z
        if (distance < 68 * 68) {
            1
        } else if (distance < 96 * 96) {
            2
        } else {
            3
        }
    }

    fun shells(tier: Int): Int = tier + 1

    val fillers: List<BlockPos> by lazy { collect { it == FILLER }.map(Cell::position) }

    private fun collect(include: (Char) -> Boolean): List<Cell> = buildList {
        StructureResourceLoader.loadAisles(RESOURCE, STRUCTURE_ID).forEachIndexed { aisle, rows ->
            rows.forEachIndexed { row, line ->
                line.forEachIndexed { column, symbol ->
                    if (include(symbol)) {
                        add(
                            Cell(
                                BlockPos(
                                    column - CONTROLLER[0] - centerOffset.x,
                                    row - CONTROLLER[1] - centerOffset.y,
                                    aisle - CONTROLLER[2] - centerOffset.z
                                ),
                                symbol
                            )
                        )
                    }
                }
            }
        }
    }

    fun center(controller: BlockPos, facing: Direction, flipped: Boolean = false): BlockPos =
        controller.offset(rotateStructureOffset(centerOffset, facing, flipped))

    fun positions(controller: BlockPos, facing: Direction, flipped: Boolean): Set<BlockPos> {
        val center = center(controller, facing, flipped)
        return cells.mapTo(LinkedHashSet(cells.size * 2)) { center.offset(rotateStructureOffset(it.position, facing, flipped)) }
    }
}