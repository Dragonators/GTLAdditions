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

    fun erase(write: (BlockPos, BlockState, CompoundTag?) -> Unit) {
        val air = Blocks.AIR.defaultBlockState()
        blocks.keys.forEach { write(BlockPos.of(it), air, null) }
    }

    fun restore(write: (BlockPos, BlockState, CompoundTag?) -> Unit) {
        blocks.forEach { (position, state) ->
            write(BlockPos.of(position), state, blockEntities[position]?.copy())
        }
    }

    fun save(): CompoundTag = CompoundTag().apply {
        val palette = ListTag()
        blocks.long2ObjectEntrySet().groupBy({ it.value }, { it.longKey }).forEach { (state, positions) ->
            palette.add(
                CompoundTag().apply {
                    put("state", NbtUtils.writeBlockState(state))
                    putLongArray("positions", positions)
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
            positions: Set<BlockPos>,
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

        fun load(tag: CompoundTag): RemovedStructure {
            val blocks = Long2ObjectLinkedOpenHashMap<BlockState>()
            val palette = tag.getList("palette", Tag.TAG_COMPOUND.toInt())

            for (index in palette.indices) {
                val entry = palette.getCompound(index)
                val state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), entry.getCompound("state"))
                entry.getLongArray("positions").forEach { blocks[it] = state }
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
    }
}