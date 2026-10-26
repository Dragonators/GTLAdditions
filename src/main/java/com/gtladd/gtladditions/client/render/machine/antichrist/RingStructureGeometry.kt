package com.gtladd.gtladditions.client.render.machine.antichrist

object RingStructureGeometry {
    const val ALL_FACES = 63

    // Bits in WEST, EAST, UP, DOWN, NORTH, SOUTH order.
    data class Cell(val x: Int, val y: Int, val z: Int, val letter: Char, val exposedFaces: Int) {
        fun visibleFaces(solid: Boolean): Int = if (solid) exposedFaces else ALL_FACES
    }

    fun collect(structure: Array<Array<String>>): List<Cell> {
        val cells = ArrayList<Cell>()

        for (x in structure.indices) {
            for (y in structure[x].indices) {
                for (z in structure[x][y].indices) {
                    val letter = structure[x][y][z]
                    if (letter == ' ') continue
                    var faces = 0
                    if (x == 0 || structure[x - 1][y][z] != letter) faces = faces or 1
                    if (x == structure.lastIndex || structure[x + 1][y][z] != letter) faces = faces or 2
                    if (y == 0 || structure[x][y - 1][z] != letter) faces = faces or 4
                    if (y == structure[x].lastIndex || structure[x][y + 1][z] != letter) faces = faces or 8
                    if (z == 0 || structure[x][y][z - 1] != letter) faces = faces or 16
                    if (z == structure[x][y].lastIndex || structure[x][y][z + 1] != letter) faces = faces or 32
                    cells.add(Cell(x, y, z, letter, faces))
                }
            }
        }

        return cells
    }
}