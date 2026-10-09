package com.gtladd.gtladditions.utils

import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.SectionPos
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.FallingBlock
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.structure.BoundingBox
import kotlin.math.sqrt

/** Shared fixed-radius removal; callers select their existing resistance and update semantics. */
object SynchronousSphereExplosion {
    const val PROTECTED_RESISTANCE = 3_600_000f

    private const val UPDATE_KNOWN_SHAPE = 16
    private const val UPDATE_CLIENTS = 2
    private const val INFLOW_GUARD_MARGIN = 16

    private val DIRECTIONS = Direction.entries.toTypedArray()

    enum class Profile {
        CHARGE,
        PLANETARY_TOWER;

        @Suppress("DEPRECATION")
        fun preserves(state: BlockState, world: BlockGetter, pos: BlockPos): Boolean {
            val resistance = if (this == PLANETARY_TOWER) {
                state.block.getExplosionResistance(state, world, pos, null)
            } else {
                state.block.getExplosionResistance()
            }

            return resistance >= PROTECTED_RESISTANCE
        }
    }

    fun explode(level: ServerLevel, centre: BlockPos, radius: Int, profile: Profile = Profile.CHARGE): Long {
        require(radius > 0)

        return Execution(level, centre.immutable(), radius, profile).run()
    }

    fun contains(centre: BlockPos, radius: Int, x: Int, y: Int, z: Int): Boolean {
        val dx = x.toLong() - centre.x
        val dy = y.toLong() - centre.y
        val dz = z.toLong() - centre.z

        return radius >= 0 && dx * dx + dy * dy + dz * dz <= radius.toLong() * radius
    }

    /** Streams section coordinates; no per-block offsets or packed coordinate cache are allocated. */
    fun forEachSection(
        centre: BlockPos,
        radius: Int,
        minBuildHeight: Int,
        maxBuildHeight: Int,
        visit: (Int, Int, Int, Boolean) -> Unit
    ) {
        require(radius > 0)
        if (minBuildHeight >= maxBuildHeight) return

        val minX = (centre.x.toLong() - radius).coerceAtLeast(-Level.MAX_LEVEL_SIZE.toLong()).toInt()
        val maxX = (centre.x.toLong() + radius).coerceAtMost(Level.MAX_LEVEL_SIZE.toLong() - 1).toInt()
        val minZ = (centre.z.toLong() - radius).coerceAtLeast(-Level.MAX_LEVEL_SIZE.toLong()).toInt()
        val maxZ = (centre.z.toLong() + radius).coerceAtMost(Level.MAX_LEVEL_SIZE.toLong() - 1).toInt()

        val r2 = radius.toLong() * radius
        val inner = (radius.toLong() - 2).coerceAtLeast(0)

        for (sx in (minX shr 4)..(maxX shr 4)) {
            val bx = sx shl 4
            val dx = nearestDistance(bx, bx + 15, centre.x)

            for (sz in (minZ shr 4)..(maxZ shr 4)) {
                val bz = sz shl 4
                val dz = nearestDistance(bz, bz + 15, centre.z)
                val columnDistance = dx * dx + dz * dz
                if (columnDistance > r2) continue

                val dy = integerSqrt(r2 - columnDistance)
                val minY = maxOf(centre.y.toLong() - dy, minBuildHeight.toLong())
                val maxY = minOf(centre.y.toLong() + dy, maxBuildHeight.toLong() - 1)
                if (minY > maxY) continue

                for (sy in (minY.toInt() shr 4)..(maxY.toInt() shr 4)) {
                    val by = sy shl 4
                    val fy = farthestDistance(by, by + 15, centre.y)
                    val fx = farthestDistance(bx, bx + 15, centre.x)
                    val fz = farthestDistance(bz, bz + 15, centre.z)
                    visit(sx, sy, sz, fx * fx + fy * fy + fz * fz <= inner * inner)
                }
            }
        }
    }

    private fun nearestDistance(lo: Int, hi: Int, value: Int): Long = when {
        value < lo -> lo.toLong() - value
        value > hi -> value.toLong() - hi
        else -> 0L
    }

    private fun farthestDistance(lo: Int, hi: Int, value: Int): Long =
        maxOf(kotlin.math.abs(lo.toLong() - value), kotlin.math.abs(hi.toLong() - value))

    private fun integerSqrt(value: Long): Long {
        var result = sqrt(value.toDouble()).toLong()
        while (result * result > value) result--
        while ((result + 1) * (result + 1) <= value) result++

        return result
    }

    private class Execution(
        val level: ServerLevel,
        val centre: BlockPos,
        val radius: Int,
        val profile: Profile
    ) {
        private val air = Blocks.AIR.defaultBlockState()
        private val pos = BlockPos.MutableBlockPos()
        private val neighbour = BlockPos.MutableBlockPos()
        private val cursor = BlockPos.MutableBlockPos()
        private val below = BlockPos.MutableBlockPos()

        private val changes = ShortOpenHashSet()

        private val r2 = radius.toLong() * radius
        private val surface = (radius.toLong() - 1) * (radius - 1)

        private var removed = 0L

        fun run(): Long {
            if (profile == Profile.CHARGE) {
                clearFluidTicks()
                level.playSound(null, centre, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 48.0f, 0.5f)
            }

            try {
                forEachSection(centre, radius, level.minBuildHeight, level.maxBuildHeight) { sx, sy, sz, inside ->
                    clearSection(sx, sy, sz, inside)
                }

                if (profile == Profile.CHARGE) {
                    // Settle only charges' boundaries; the tower keeps its original block-update behavior.
                    forEachSection(centre, radius, level.minBuildHeight, level.maxBuildHeight) { sx, sy, sz, inside ->
                        if (!inside) settleBoundary(sx, sy, sz)
                    }
                }
            } finally {
                if (profile == Profile.CHARGE) clearFluidTicks()
            }

            return removed
        }

        private fun clearSection(sx: Int, sy: Int, sz: Int, inside: Boolean) {
            val chunk = level.getChunk(sx, sz)
            val index = level.getSectionIndexFromSectionY(sy)
            val section = chunk.sections[index]
            if (section.hasOnlyAir()) return

            changes.clear()
            val bx = sx shl 4
            val by = sy shl 4
            val bz = sz shl 4

            for (lx in 0..15) {
                val x = bx + lx
                if (x < -Level.MAX_LEVEL_SIZE || x >= Level.MAX_LEVEL_SIZE) continue

                for (ly in 0..15) {
                    val y = by + ly
                    if (y < level.minBuildHeight || y >= level.maxBuildHeight) continue

                    for (lz in 0..15) {
                        val z = bz + lz
                        if (z < -Level.MAX_LEVEL_SIZE || z >= Level.MAX_LEVEL_SIZE) continue
                        if (!inside && !contains(centre, radius, x, y, z)) continue

                        val state = section.getBlockState(lx, ly, lz)
                        if (state.isAir) continue

                        pos.set(x, y, z)
                        if (profile.preserves(state, level, pos)) continue

                        val flags = if (profile == Profile.PLANETARY_TOWER) UPDATE_CLIENTS else UPDATE_KNOWN_SHAPE
                        if (level.setBlock(pos, air, flags)) {
                            if (profile == Profile.CHARGE) changes.add(SectionPos.sectionRelativePos(pos))
                            removed++
                        }
                    }
                }
            }

            if (changes.isEmpty()) return

            val players = level.chunkSource.chunkMap.getPlayers(ChunkPos(sx, sz), false)
            if (players.isNotEmpty()) {
                val packet = ClientboundSectionBlocksUpdatePacket(SectionPos.of(sx, sy, sz), changes, section)
                players.forEach { it.connection.send(packet) }
            }
        }

        private fun settleBoundary(sx: Int, sy: Int, sz: Int) {
            val bx = sx shl 4
            val by = sy shl 4
            val bz = sz shl 4

            for (lx in 0..15) {
                for (ly in 0..15) {
                    val y = by + ly
                    if (y < level.minBuildHeight || y >= level.maxBuildHeight) continue

                    for (lz in 0..15) {
                        val dx = bx + lx - centre.x
                        val dy = y - centre.y
                        val dz = bz + lz - centre.z
                        val d2 = dx.toLong() * dx + dy.toLong() * dy + dz.toLong() * dz
                        if (d2 <= surface || d2 > r2) continue

                        pos.set(bx + lx, y, bz + lz)
                        if (!level.isInWorldBounds(pos) || !level.getBlockState(pos).isAir) continue

                        for (direction in DIRECTIONS) {
                            neighbour.set(pos).move(direction)
                            if (!level.isInWorldBounds(neighbour)) continue
                            if (contains(centre, radius, neighbour.x, neighbour.y, neighbour.z)) continue

                            val state = level.getBlockState(neighbour)
                            if (state.isAir || state.block is LiquidBlock) continue

                            if (!state.canSurvive(level, neighbour)) {
                                level.setBlock(neighbour, air, UPDATE_KNOWN_SHAPE or UPDATE_CLIENTS)
                            } else if (state.block is FallingBlock) {
                                below.set(neighbour).move(Direction.DOWN)
                                if (FallingBlock.isFree(level.getBlockState(below))) collapseFallingColumn()
                            }
                        }
                    }
                }
            }
        }

        private fun collapseFallingColumn() {
            cursor.set(neighbour)
            while (cursor.y < level.maxBuildHeight) {
                val state = level.getBlockState(cursor)
                if (state.block !is FallingBlock) break

                below.set(cursor).move(Direction.DOWN)
                if (!FallingBlock.isFree(level.getBlockState(below))) break

                level.setBlock(cursor, air, UPDATE_KNOWN_SHAPE or UPDATE_CLIENTS)
                cursor.move(Direction.UP)
            }

            if (cursor.y < level.maxBuildHeight) {
                val state = level.getBlockState(cursor)
                if (!state.isAir && state.block !is LiquidBlock && !state.canSurvive(level, cursor)) {
                    level.setBlock(cursor, air, UPDATE_KNOWN_SHAPE or UPDATE_CLIENTS)
                }
            }
        }

        private fun clearFluidTicks() {
            level.fluidTicks.clearArea(
                BoundingBox(
                    centre.x - radius - INFLOW_GUARD_MARGIN,
                    level.minBuildHeight,
                    centre.z - radius - INFLOW_GUARD_MARGIN,
                    centre.x + radius + INFLOW_GUARD_MARGIN,
                    level.maxBuildHeight - 1,
                    centre.z + radius + INFLOW_GUARD_MARGIN
                )
            )
        }
    }
}