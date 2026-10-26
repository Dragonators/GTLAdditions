package com.gtladd.gtladditions.api.pattern

import com.gregtechceu.gtceu.api.pattern.BlockPattern
import com.gregtechceu.gtceu.api.pattern.Predicates
import com.gtladd.gtladditions.common.data.MachineInfo
import com.gtladd.gtladditions.common.data.StructureRenderType
import com.gtladd.gtladditions.common.machine.multiblock.structure.StructureBlockLayout
import com.gtladd.gtladditions.mixin.gtceu.api.pattern.BlockPatternAccessor
import net.minecraft.core.Direction

/** A validation template whose removed cells are skipped by the native matcher. */
object RemainingStructurePattern {
    fun create(pattern: BlockPattern, type: StructureRenderType): BlockPattern {
        val accessor = pattern as BlockPatternAccessor
        val original = accessor.blockMatches
        val center = accessor.centerOffset
        require(pattern.aisleRepetitions.all { it[0] == 1 && it[1] == 1 }) { "Structure removal requires fixed aisles" }

        // Never modify the definition's full template; build/dismantle tools use it.
        val matches = original.copyOf()
        val axes = pattern.structureDir.map { it.getActualFacing(Direction.SOUTH) }
        val any = Predicates.any()
        for (pos in StructureBlockLayout.positions(MachineInfo(0L, Direction.SOUTH, type, Direction.NORTH, false))) {
            val x = pos.x * axes[0].stepX + pos.y * axes[0].stepY + pos.z * axes[0].stepZ + center[0]
            val y = pos.x * axes[1].stepX + pos.y * axes[1].stepY + pos.z * axes[1].stepZ + center[1]
            val z = pos.x * axes[2].stepX + pos.y * axes[2].stepY + pos.z * axes[2].stepZ + center[4]
            val predicate = original[z][y][x]
            require(!predicate.isController && predicate.limited.isEmpty()) { "Removed cells cannot contain controllers or counted parts" }
            if (matches[z] === original[z]) matches[z] = original[z].copyOf()
            if (matches[z][y] === original[z][y]) matches[z][y] = original[z][y].copyOf()
            matches[z][y][x] = any
        }

        return BlockPattern(matches, pattern.structureDir, pattern.aisleRepetitions, center)
    }
}