package com.gtladd.gtladditions.common.machine

import com.gregtechceu.gtceu.api.GTCEuAPI
import com.gregtechceu.gtceu.api.pattern.MultiblockState
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate
import com.gregtechceu.gtceu.api.pattern.error.PatternStringError
import com.gregtechceu.gtceu.api.pattern.predicates.PredicateBlocks
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate
import com.gregtechceu.gtceu.common.block.CoilBlock
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.lowdragmc.lowdraglib.utils.BlockInfo
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.block.state.properties.SlabType
import java.util.function.Predicate

object GTLAddPredicates {
    fun slabBlock(slabType: SlabType, vararg block: Block): TraceabilityPredicate = TraceabilityPredicate(object : PredicateBlocks(*block) {
        override fun test(blockWorldState: MultiblockState): Boolean = super.test(blockWorldState) &&
            blockWorldState.blockState.getValue(SlabBlock.TYPE) == slabType
    })

    fun heatingCoils(temperature: Int, equals: Boolean = false): TraceabilityPredicate {
        val matchesTemperature: (Int) -> Boolean = if (equals) {
            { coilTemperature -> coilTemperature == temperature }
        } else {
            { coilTemperature -> coilTemperature >= temperature }
        }

        return TraceabilityPredicate(object : SimplePredicate(
            Predicate {
                for (entry in GTCEuAPI.HEATING_COILS.entries) {
                    if (it.blockState.`is`(entry.value.get())) {
                        val stats = entry.key
                        val currentCoil = it.matchContext.getOrPut("CoilType", stats)
                        if (currentCoil != stats) {
                            it.setError(PatternStringError("gtceu.multiblock.pattern.error.coils"))
                            return@Predicate false
                        }
                        return@Predicate true
                    }
                }
                false
            },
            {
                val (matched, notMatched) = GTCEuAPI.HEATING_COILS.entries.partition { matchesTemperature(it.key.coilTemperature) }
                (matched.sortedBy { it.key.coilTemperature } + notMatched.sortedBy { it.key.coilTemperature }).map { BlockInfo.fromBlockState(it.value.get().defaultBlockState()) }.toTypedArray()
            }
        ) {
            override fun test(blockWorldState: MultiblockState): Boolean {
                if (super.test(blockWorldState)) return matchesTemperature((blockWorldState.blockState.block as CoilBlock).coilType.coilTemperature)
                return false
            }
        }).addTooltips("gtceu.multiblock.pattern.error.coils".toComponent)
    }
}