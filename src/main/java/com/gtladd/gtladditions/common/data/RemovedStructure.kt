package com.gtladd.gtladditions.common.data

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.nbt.Tag
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

class RemovedStructure private constructor(
    private val blocks: Long2ObjectMap<BlockState>,
    private val blockEntities: Map<Long, CompoundTag>
) {

    val isEmpty: Boolean
        get() = blocks.isEmpty()

    fun blockState(pos: BlockPos): BlockState? = blocks.get(pos.asLong())

    fun contains(pos: BlockPos): Boolean = blocks.containsKey(pos.asLong())

    fun erase(write: (BlockPos, BlockState, CompoundTag?) -> Unit) {
        val air = Blocks.AIR.defaultBlockState()
        blocks.keys.forEach { write(BlockPos.of(it), air, null) }
    }

    fun restore(write: (BlockPos, BlockState, CompoundTag?) -> Unit) {
        blocks.forEach { (position, state) ->
            write(BlockPos.of(position), state, blockEntities[position]?.copy())
        }
    }

    fun restore(positions: Collection<BlockPos>, write: (BlockPos, BlockState, CompoundTag?) -> Unit): RemovedStructure {
        if (positions.none(::contains)) return this

        val remaining = Long2ObjectLinkedOpenHashMap(blocks)
        val entities = HashMap(blockEntities)

        for (pos in positions) {
            val key = pos.asLong()
            val state = remaining.remove(key) ?: continue
            write(pos, state, entities.remove(key)?.copy())
        }

        return if (remaining.isEmpty()) EMPTY else RemovedStructure(remaining, entities)
    }

    /** [other] must hold positions disjoint from these. */
    operator fun plus(other: RemovedStructure): RemovedStructure {
        if (isEmpty) return other

        val merged = Long2ObjectLinkedOpenHashMap<BlockState>(blocks.size + other.blocks.size)
        merged.putAll(blocks)
        merged.putAll(other.blocks)

        return RemovedStructure(merged, blockEntities + other.blockEntities)
    }

    fun save(): CompoundTag = CompoundTag().apply {
        val palette = ListTag()
        blocks.long2ObjectEntrySet().groupBy({ it.value }, { it.longKey }).forEach { (state, positions) ->
            palette.add(
                CompoundTag().apply {
                    put("state", NbtUtils.writeBlockState(state))
                    putByteArray("packed", pack(positions))
                }
            )
        }

        put("palette", palette)

        val entities = ListTag()
        blockEntities.forEach { (position, data) ->
            entities.add(
                CompoundTag().apply {
                    putLong("position", position)
                    put("data", data.copy())
                }
            )
        }

        put("blockEntities", entities)
    }

    companion object {
        val EMPTY = RemovedStructure(Long2ObjectMaps.emptyMap(), emptyMap())

        fun capture(
            positions: Collection<BlockPos>,
            stateAt: (BlockPos) -> BlockState,
            blockEntityAt: (BlockPos) -> CompoundTag?
        ): RemovedStructure {
            val blocks = Long2ObjectLinkedOpenHashMap<BlockState>(positions.size)
            val entities = HashMap<Long, CompoundTag>()
            positions.forEach { pos ->
                blocks[pos.asLong()] = stateAt(pos)
                blockEntityAt(pos)?.let { entities[pos.asLong()] = it.copy() }
            }

            return RemovedStructure(blocks, entities)
        }

        @Suppress("DEPRECATION")
        fun load(tag: CompoundTag): RemovedStructure {
            val blocks = Long2ObjectLinkedOpenHashMap<BlockState>()
            val palette = tag.getList("palette", Tag.TAG_COMPOUND.toInt())

            for (index in palette.indices) {
                val entry = palette.getCompound(index)
                // Loaded before the machine has a level, and Forge registries offer no HolderGetter.
                val state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), entry.getCompound("state"))
                unpack(entry.getByteArray("packed")) { blocks[it] = state }
            }

            if (blocks.isEmpty()) return EMPTY

            val entities = HashMap<Long, CompoundTag>()
            val storedEntities = tag.getList("blockEntities", Tag.TAG_COMPOUND.toInt())

            for (index in storedEntities.indices) {
                val entry = storedEntities.getCompound(index)
                entities[entry.getLong("position")] = entry.getCompound("data").copy()
            }

            return RemovedStructure(blocks, entities)
        }

        /** Unsigned LEB128 deltas of the sorted positions. */
        private fun pack(positions: List<Long>): ByteArray {
            val sorted = positions.toLongArray().apply { sort() }
            val out = java.io.ByteArrayOutputStream(sorted.size * 2)
            var previous = 0L

            for (position in sorted) {
                var delta = position - previous
                previous = position

                while (delta and 0x7fL.inv() != 0L) {
                    out.write(((delta and 0x7f) or 0x80).toInt())
                    delta = delta ushr 7
                }

                out.write(delta.toInt())
            }

            return out.toByteArray()
        }

        private inline fun unpack(packed: ByteArray, consumer: (Long) -> Unit) {
            var index = 0
            var position = 0L

            while (index < packed.size) {
                var delta = 0L
                var shift = 0

                do {
                    val byte = packed[index++].toInt()
                    delta = delta or ((byte and 0x7f).toLong() shl shift)
                    shift += 7
                } while (byte and 0x80 != 0)

                position += delta
                consumer(position)
            }
        }
    }
}