package com.gtladd.gtladditions.api.pattern

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import com.gregtechceu.gtceu.api.pattern.BlockPattern
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate
import com.gregtechceu.gtceu.utils.SupplierMemoizer
import com.gtladd.gtladditions.mixin.gtceu.api.pattern.BlockPatternAccessor
import net.minecraft.world.level.block.Block
import java.util.IdentityHashMap

class PatternPredicateSelector private constructor(
    private val blocks: Set<() -> Block>,
    private val abilities: Set<PartAbility>,
    private val skipController: Boolean
) {
    fun containsBlock(block: Block): PatternPredicateSelector = containsBlock { block }

    fun containsBlock(block: () -> Block): PatternPredicateSelector = PatternPredicateSelector(blocks + block, abilities, skipController)

    fun containsAbility(ability: PartAbility): PatternPredicateSelector = PatternPredicateSelector(blocks, abilities + ability, skipController)

    fun containsAbilities(vararg abilities: PartAbility): PatternPredicateSelector =
        PatternPredicateSelector(blocks, this.abilities + abilities, skipController)

    fun notController(): PatternPredicateSelector = PatternPredicateSelector(blocks, abilities, true)

    fun matches(predicate: TraceabilityPredicate): Boolean {
        if (skipController && predicate.isController) return false
        val candidateBlocks = predicate.candidateBlocks()
        return blocks.map { it() }.all(candidateBlocks::contains) &&
            abilities.all { ability -> ability.getAllBlocks().any(candidateBlocks::contains) }
    }

    companion object {
        fun any(): PatternPredicateSelector = PatternPredicateSelector(emptySet(), emptySet(), false)

        fun containsBlock(block: Block): PatternPredicateSelector = any().containsBlock(block)

        fun containsBlock(block: () -> Block): PatternPredicateSelector = any().containsBlock(block)

        fun containsAbility(ability: PartAbility): PatternPredicateSelector = any().containsAbility(ability)
    }
}

fun patternPredicateSelector(block: Block, vararg abilities: PartAbility): PatternPredicateSelector =
    PatternPredicateSelector.containsBlock(block)
        .containsAbilities(*abilities)
        .notController()

fun patternPredicateSelector(block: () -> Block, vararg abilities: PartAbility): PatternPredicateSelector =
    PatternPredicateSelector.containsBlock(block)
        .containsAbilities(*abilities)
        .notController()

fun MultiblockMachineDefinition.patchPatternPredicates(
    name: String,
    selector: PatternPredicateSelector,
    append: TraceabilityPredicate,
    requireAtLeastOne: Boolean = true
) {
    patchPatternPredicates(name, selector, { append }, requireAtLeastOne)
}

fun MultiblockMachineDefinition.patchPatternPredicates(
    name: String,
    selector: PatternPredicateSelector,
    append: () -> TraceabilityPredicate,
    requireAtLeastOne: Boolean = true
) {
    transformPatternPredicates(name, selector, requireAtLeastOne) { predicate -> predicate.or(append()) }
}

fun MultiblockMachineDefinition.replacePatternPredicates(
    name: String,
    selector: PatternPredicateSelector,
    replacement: TraceabilityPredicate,
    requireAtLeastOne: Boolean = true
) {
    replacePatternPredicates(name, selector, { replacement }, requireAtLeastOne)
}

fun MultiblockMachineDefinition.replacePatternPredicates(
    name: String,
    selector: PatternPredicateSelector,
    replacement: () -> TraceabilityPredicate,
    requireAtLeastOne: Boolean = true
) {
    transformPatternPredicates(name, selector, requireAtLeastOne) { replacement() }
}

fun MultiblockMachineDefinition.transformPatternPredicates(
    name: String,
    selector: PatternPredicateSelector,
    requireAtLeastOne: Boolean = true,
    transform: (TraceabilityPredicate) -> TraceabilityPredicate
) {
    val oldFactory = patternFactory
    patternFactory = SupplierMemoizer.memoize {
        oldFactory.get().patchPredicates(id.toString(), name, selector, requireAtLeastOne, transform)
    }
}

private fun BlockPattern.patchPredicates(
    machineId: String,
    patchName: String,
    selector: PatternPredicateSelector,
    requireAtLeastOne: Boolean,
    transform: (TraceabilityPredicate) -> TraceabilityPredicate
): BlockPattern {
    val matches = IdentityHashMap<TraceabilityPredicate, TraceabilityPredicate>()
    val blockMatches = (this as BlockPatternAccessor).`gtladd$getBlockMatches`()

    for (aisle in blockMatches) {
        for (row in aisle) {
            for (predicate in row) {
                if (!matches.containsKey(predicate) && selector.matches(predicate)) {
                    matches[predicate] = transform(predicate)
                }
            }
        }
    }

    if (matches.isEmpty() && requireAtLeastOne) {
        error("Pattern patch '$patchName' matched no predicates for $machineId")
    }

    for (aisleIndex in blockMatches.indices) {
        val aisle = blockMatches[aisleIndex]
        for (rowIndex in aisle.indices) {
            val row = aisle[rowIndex]
            for (columnIndex in row.indices) {
                row[columnIndex] = matches[row[columnIndex]] ?: row[columnIndex]
            }
        }
    }

    return this
}

private fun TraceabilityPredicate.candidateBlocks(): Set<Block> =
    (common.asSequence() + limited.asSequence())
        .flatMap { simplePredicate ->
            simplePredicate.candidates?.get()?.asSequence() ?: emptySequence()
        }
        .map { it.blockState.block }
        .toSet()