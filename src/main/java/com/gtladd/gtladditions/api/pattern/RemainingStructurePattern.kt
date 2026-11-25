package com.gtladd.gtladditions.api.pattern

import com.gregtechceu.gtceu.api.pattern.BlockPattern
import com.gregtechceu.gtceu.api.pattern.Predicates
import com.gtladd.gtladditions.common.data.StructureRenderType
import com.gtladd.gtladditions.common.machine.multiblock.structure.StructureBlockLayout
import com.gtladd.gtladditions.mixin.gtceu.api.pattern.BlockPatternAccessor
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import it.unimi.dsi.fastutil.ints.IntSets
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import java.util.EnumMap
import java.util.WeakHashMap

object RemainingStructurePattern {

    private val shared = WeakHashMap<BlockPattern, EnumMap<StructureRenderType, BlockPattern>>()
    private val cells = WeakHashMap<BlockPattern, EnumMap<StructureRenderType, IntSet>>()

    fun create(pattern: BlockPattern, type: StructureRenderType): BlockPattern = synchronized(shared) {
        shared.getOrPut(pattern) { EnumMap(StructureRenderType::class.java) }.getOrPut(type) { build(pattern, type) }
    }

    fun virtualCells(pattern: BlockPattern, type: StructureRenderType): IntSet = synchronized(shared) {
        cells.getOrPut(pattern) { EnumMap(StructureRenderType::class.java) }.getOrPut(type) { collect(pattern, type) }
    }

    private fun build(pattern: BlockPattern, type: StructureRenderType): BlockPattern {
        val accessor = pattern as BlockPatternAccessor
        val original = accessor.blockMatches

        // Never modify the definition's full template; build/dismantle tools use it.
        val matches = original.copyOf()
        val any = Predicates.any()

        val iterator = virtualCells(pattern, type).iterator()
        while (iterator.hasNext()) {
            val cell = iterator.nextInt()
            val z = StructureTierLayout.aisle(cell)
            val y = StructureTierLayout.row(cell)
            val x = StructureTierLayout.column(cell)

            if (matches[z] === original[z]) matches[z] = original[z].copyOf()
            if (matches[z][y] === original[z][y]) matches[z][y] = original[z][y].copyOf()
            matches[z][y][x] = any
        }

        return BlockPattern(matches, pattern.structureDir, pattern.aisleRepetitions, accessor.centerOffset)
    }

    private fun collect(pattern: BlockPattern, type: StructureRenderType): IntSet {
        val accessor = pattern as BlockPatternAccessor
        val original = accessor.blockMatches
        val center = accessor.centerOffset

        require(pattern.aisleRepetitions.all { it[0] == 1 && it[1] == 1 }) {
            "Structure removal requires fixed aisles"
        }

        val axes = pattern.structureDir.map { it.getActualFacing(Direction.SOUTH) }
        val cells = IntOpenHashSet()

        for (pos in StructureBlockLayout.positions(type, BlockPos.ZERO, Direction.SOUTH, Direction.NORTH, false)) {
            val x = pos.x * axes[0].stepX + pos.y * axes[0].stepY + pos.z * axes[0].stepZ + center[0]
            val y = pos.x * axes[1].stepX + pos.y * axes[1].stepY + pos.z * axes[1].stepZ + center[1]
            val z = pos.x * axes[2].stepX + pos.y * axes[2].stepY + pos.z * axes[2].stepZ + center[4]

            val predicate = original[z][y][x]
            require(!predicate.isController && predicate.limited.isEmpty()) {
                "Removed cells cannot contain controllers or counted parts"
            }

            cells.add(StructureTierLayout.pack(z, y, x))
        }

        return IntSets.unmodifiable(cells)
    }
}
