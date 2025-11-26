package com.gtladd.gtladditions.utils.antichrist
import com.gtladd.gtladditions.client.render.machine.ForgeOfAntichristRenderer.Companion.BASE_DIRECTION
import com.gtladd.gtladditions.client.render.machine.ForgeOfAntichristRenderer.Companion.STAR_OFFSET_X
import com.gtladd.gtladditions.common.data.MachineInfo
import com.gtladd.gtladditions.common.machine.muiltblock.structure.RingStructure
import com.gtladd.gtladditions.utils.CommonUtils
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object ClientRingBlockHelper {
    private const val FLAG = Block.UPDATE_MOVE_BY_PISTON or
            Block.UPDATE_SUPPRESS_DROPS or
            Block.UPDATE_KNOWN_SHAPE or
            Block.UPDATE_CLIENTS or
            Block.UPDATE_IMMEDIATE

    private var currentDimension: ResourceKey<Level>? = null
    private val DIMENSION_MACHINE_BLOCKS = Object2ReferenceOpenHashMap<ResourceKey<Level>, Long2ReferenceMap<LongSet>>()
    private val DIMENSION_CHUNK_BLOCKS = Object2ReferenceOpenHashMap<ResourceKey<Level>, Long2ReferenceMap<LongSet>>()

    fun getProtectedBlocksInChunk(level: Level, x: Int, z: Int): LongSet? {
        return DIMENSION_CHUNK_BLOCKS[level.dimension()]?.get(ChunkPos.asLong(x, z))
    }

    fun syncDimensionMachines(level: Level, machines: Array<MachineInfo>) {
        clearDimensionData()

        currentDimension = level.dimension()

        for (machineInfo in machines) {
            hideRingsAtPosition(level, machineInfo.posLong, machineInfo.facing)
        }
    }

    fun hideRingsAtPosition(level: Level, machinePos: Long, frontFacing: Direction) {
        val machineBlocks = DIMENSION_MACHINE_BLOCKS.computeIfAbsent(level.dimension()) {
            Long2ReferenceOpenHashMap()
        }

        machineBlocks.computeIfAbsent(machinePos) {
            val realPosSet = LongOpenHashSet()

            for (ring in RingStructure.RINGS) {
                collectAndRemoveStructure(level, BlockPos.of(machinePos), frontFacing, ring, realPosSet)
            }

            realPosSet
        }
    }

    fun restoreRingsAtPosition(level: Level, machinePos: Long, frontFacing: Direction) {
        val dimensionKey = level.dimension()

        val machines = DIMENSION_MACHINE_BLOCKS[dimensionKey] ?: return
        val realPosSet = machines.remove(machinePos) ?: return
        if (machines.isEmpty()) DIMENSION_MACHINE_BLOCKS.remove(dimensionKey)

        for (ring in RingStructure.RINGS) {
            restoreStructureByPosition(level, BlockPos.of(machinePos), frontFacing, ring, realPosSet)
        }
    }

    fun clearAllData() {
        currentDimension = null
        clearDimensionData()
    }

    // ========================================
    // Utils
    // ========================================

    private fun collectAndRemoveStructure(
        level: Level,
        machinePos: BlockPos,
        frontFacing: Direction,
        structure: Array<Array<String>>,
        realPosSet: LongSet
    ) {
        val chunkBlocks = DIMENSION_CHUNK_BLOCKS.computeIfAbsent(level.dimension()) {
            Long2ReferenceOpenHashMap()
        }

        for (x in structure.indices) {
            val plane = structure[x]
            for (y in plane.indices) {
                val row = plane[y]
                for (z in row.indices) {
                    val letter = row[z]
                    if (letter == ' ') continue

                    val realPos = getWorldRealPosByPosition(machinePos, frontFacing, x, y, z, structure)
                    val posLong = realPos.asLong()
                    val chunkPos = ChunkPos.asLong(realPos)

                    realPosSet.add(posLong)
                    chunkBlocks
                        .computeIfAbsent(chunkPos) { LongOpenHashSet() }
                        .add(posLong)

                    if (level.isLoaded(realPos)) {
                        level.setBlock(realPos, Blocks.AIR.defaultBlockState(), FLAG)
                    }
                }
            }
        }
    }

    private fun restoreStructureByPosition(
        level: Level,
        machinePos: BlockPos,
        frontFacing: Direction,
        structure: Array<Array<String>>,
        realPosSet: LongSet
    ) {
        val dimensionKey = level.dimension()
        val chunkBlocks = DIMENSION_CHUNK_BLOCKS[dimensionKey]

        for (x in structure.indices) {
            val plane = structure[x]
            for (y in plane.indices) {
                val row = plane[y]
                for (z in row.indices) {
                    val letter = row[z]
                    if (letter == ' ') continue

                    val realPos = getWorldRealPosByPosition(machinePos, frontFacing, x, y, z, structure)
                    val posLong = realPos.asLong()
                    val chunkPos = ChunkPos.asLong(realPos)

                    chunkBlocks?.get(chunkPos)?.remove(posLong)

                    if (!realPosSet.contains(posLong)) continue
                    if (!level.isLoaded(realPos)) continue

                    val block = RingStructureVertexBuffer.BLOCK_MAPPER[letter] ?: continue
                    val blockState = block.defaultBlockState()
                    level.setBlock(realPos, blockState, FLAG)
                }
            }
        }

        chunkBlocks?.let { chunkMap ->
            chunkMap.values.removeIf { it.isEmpty() }
            if (chunkMap.isEmpty()) {
                DIMENSION_CHUNK_BLOCKS.remove(dimensionKey)
            }
        }
    }

    private fun getWorldRealPosByPosition(
        machinePos: BlockPos,
        frontFacing: Direction,
        x: Int,
        y: Int,
        z: Int,
        structure: Array<Array<String>>
    ): BlockPos {
        val offsetStructureCenter = CommonUtils.getRotatedRenderPosition(
            Direction.WEST,
            frontFacing,
            (x - structure.size / 2).toDouble(),
            (structure[0].size / 2 - y).toDouble(),
            (z - structure[0][0].length / 2).toDouble()
        )

        val offsetStarCenter = CommonUtils.getRotatedRenderPosition(
            BASE_DIRECTION,
            frontFacing,
            if (structure.size == 94) STAR_OFFSET_X - 1 else STAR_OFFSET_X,
            0.0,
            0.0
        )

        return machinePos.offset(BlockPos.containing(offsetStarCenter)).offset(BlockPos.containing(offsetStructureCenter)).immutable()
    }

    private fun clearDimensionData() {
        DIMENSION_MACHINE_BLOCKS.clear()
        DIMENSION_CHUNK_BLOCKS.clear()
    }
}