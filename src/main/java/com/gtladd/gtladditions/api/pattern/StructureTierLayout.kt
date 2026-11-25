package com.gtladd.gtladditions.api.pattern

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import com.gregtechceu.gtceu.api.pattern.BlockPattern
import com.gregtechceu.gtceu.api.pattern.MultiblockState
import com.gregtechceu.gtceu.api.pattern.Predicates
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate
import com.gregtechceu.gtceu.api.pattern.error.PatternError
import com.gtladd.gtladditions.mixin.gtceu.api.pattern.BlockPatternAccessor
import com.gtladd.gtladditions.mixin.gtceu.api.pattern.MultiblockStateAccessor
import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import org.gtlcore.gtlcore.api.pattern.AdvancedBlockPattern
import java.util.IdentityHashMap

class StructureTierLayout(
    val minTier: Int,
    val maxTier: Int,
    pattern: () -> BlockPattern,
    private val classifier: (aisle: Int, row: Int, column: Int) -> Int
) {

    val pattern: BlockPattern by lazy(pattern)
    val base: AdvancedBlockPattern by lazy { template { it <= minTier } }

    private val tiers: Array<Pair<IntArray, IntArray>> by lazy(::collect)

    /** The base (0) includes the controller and parts. */
    fun cells(tier: Int): IntArray = tiers[tier].first

    /** One cell per distinct predicate in [tier], used to recover shared matchContext keys from virtualized blocks. */
    fun distinctPredicateCells(tier: Int): IntArray = tiers[tier].second

    fun predicate(cell: Int): TraceabilityPredicate =
        (pattern as BlockPatternAccessor).blockMatches[aisle(cell)][row(cell)][column(cell)]

    fun offset(cell: Int, facing: Direction, upwards: Direction, flipped: Boolean): BlockPos {
        val accessor = pattern as BlockPatternAccessor
        val center = accessor.centerOffset
        return accessor.invokeSetActualRelativeOffset(
            column(cell) - center[0],
            row(cell) - center[1],
            aisle(cell) - center[4],
            facing,
            upwards,
            flipped
        )
    }

    fun position(cell: Int, controller: MultiblockControllerMachine, flipped: Boolean = controller.isFlipped): BlockPos =
        offset(cell, controller.frontFacing, controller.upwardsFacing, flipped).offset(controller.pos)

    fun template(keep: (tier: Int) -> Boolean): AdvancedBlockPattern = templateCells { tier, _ -> keep(tier) }

    fun templateCells(keep: (tier: Int, cell: Int) -> Boolean): AdvancedBlockPattern {
        val original = (pattern as BlockPatternAccessor).blockMatches
        val matches = original.copyOf()
        val any = Predicates.any()

        for (tier in 0..maxTier) {
            for (cell in cells(tier)) {
                if (keep(tier, cell)) continue

                val a = aisle(cell)
                val r = row(cell)
                if (matches[a] === original[a]) matches[a] = original[a].copyOf()
                if (matches[a][r] === original[a][r]) matches[a][r] = original[a][r].copyOf()
                matches[a][r][column(cell)] = any
            }
        }

        return AdvancedBlockPattern(
            matches,
            pattern.structureDir,
            pattern.aisleRepetitions,
            (pattern as BlockPatternAccessor).centerOffset
        )
    }

    fun positions(tiers: IntRange, controller: MultiblockControllerMachine): List<BlockPos> =
        buildList(tiers.sumOf { cells(it).size }) {
            for (tier in tiers) {
                for (cell in cells(tier)) {
                    add(position(cell, controller))
                }
            }
        }

    fun matches(cells: IntArray, state: MultiblockState, controller: MultiblockControllerMachine): Boolean {
        val accessor = state as MultiblockStateAccessor

        for (cell in cells) {
            val predicate = predicate(cell)
            val pos = position(cell, controller, state.isNeededFlip)
            if (!accessor.invokeUpdate(pos, predicate)) return false

            if (!predicate.test(state)) {
                state.setError(PatternError())
                return false
            }
        }

        return true
    }

    private fun collect(): Array<Pair<IntArray, IntArray>> {
        val matches = (pattern as BlockPatternAccessor).blockMatches
        require(pattern.aisleRepetitions.all { it[0] == 1 && it[1] == 1 }) {
            "Structure tiers require fixed aisles"
        }

        val tiers = Array(maxTier + 1) { IntArrayList() }
        val distinctByPredicate = Array(maxTier + 1) { IdentityHashMap<TraceabilityPredicate, Int>() }

        for (aisle in matches.indices) {
            for (row in matches[aisle].indices) {
                for (column in matches[aisle][row].indices) {
                    val predicate = matches[aisle][row][column]
                    if (predicate.isAny) continue

                    val tier = classifier(aisle, row, column)
                    require(tier == 0 || !predicate.isController && predicate.limited.isEmpty()) {
                        "Tier cells cannot contain controllers or counted parts"
                    }

                    val cell = pack(aisle, row, column)
                    tiers[tier].add(cell)
                    distinctByPredicate[tier].putIfAbsent(predicate, cell)
                }
            }
        }

        return Array(maxTier + 1) { tiers[it].toIntArray() to distinctByPredicate[it].values.toIntArray() }
    }

    companion object {
        fun pack(aisle: Int, row: Int, column: Int): Int = aisle shl 20 or (row shl 10) or column

        fun aisle(cell: Int): Int = cell ushr 20

        fun row(cell: Int): Int = cell ushr 10 and 0x3ff

        fun column(cell: Int): Int = cell and 0x3ff
    }
}
