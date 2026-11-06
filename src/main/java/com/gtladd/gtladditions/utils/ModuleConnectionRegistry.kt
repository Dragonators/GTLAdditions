package com.gtladd.gtladditions.utils

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level

/** Server thread only; connection never reads another chunk. */
class ModuleConnectionRegistry<H : Any, M : Any> {
    class Slot<H>(val host: H, val index: Int)

    private class Dimension<H, M> {
        val slots = Long2ObjectOpenHashMap<MutableList<Slot<H>>>()
        val modules = Long2ObjectOpenHashMap<M>()
    }

    private val dimensions = HashMap<ResourceKey<Level>, Dimension<H, M>>()

    fun register(dimension: ResourceKey<Level>, host: H, positions: Array<BlockPos>) {
        val slots = dimensions.getOrPut(dimension) { Dimension() }.slots
        positions.forEachIndexed { index, pos ->
            val owners = slots.computeIfAbsent(pos.asLong()) { ArrayList() }
            if (owners.none { it.host === host }) owners.add(Slot(host, index))
        }
    }

    fun unregister(dimension: ResourceKey<Level>, host: H, positions: Array<BlockPos>) {
        val state = dimensions[dimension] ?: return
        for (pos in positions) {
            val owners = state.slots[pos.asLong()] ?: continue
            owners.removeIf { it.host === host }
            if (owners.isEmpty()) state.slots.remove(pos.asLong())
        }

        removeEmpty(dimension, state)
    }

    fun slotsAt(dimension: ResourceKey<Level>, pos: BlockPos): List<Slot<H>> =
        dimensions[dimension]?.slots?.get(pos.asLong()) ?: emptyList()

    fun addModule(dimension: ResourceKey<Level>, pos: BlockPos, module: M) {
        dimensions.getOrPut(dimension) { Dimension() }.modules.put(pos.asLong(), module)
    }

    fun removeModule(dimension: ResourceKey<Level>, pos: BlockPos, module: M) {
        val state = dimensions[dimension] ?: return
        if (state.modules[pos.asLong()] === module) state.modules.remove(pos.asLong())
        removeEmpty(dimension, state)
    }

    fun moduleAt(dimension: ResourceKey<Level>, pos: BlockPos): M? = dimensions[dimension]?.modules?.get(pos.asLong())

    fun clearLevel(dimension: ResourceKey<Level>) {
        dimensions.remove(dimension)
    }

    private fun removeEmpty(dimension: ResourceKey<Level>, state: Dimension<H, M>) {
        if (state.slots.isEmpty() && state.modules.isEmpty()) dimensions.remove(dimension)
    }
}