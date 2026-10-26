package com.gtladd.gtladditions.utils

import com.gtladd.gtladditions.common.blocks.GTLAddBlocks
import com.gtladd.gtladditions.common.blocks.LightHunterInteriorLightBlock
import com.gtladd.gtladditions.common.machine.multiblock.controller.LightHunterSpaceStation
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk

object LightHunterInteriorLighting {
    interface WorldAccess {
        /** Must never wait on a chunk-loading future. */
        fun stateIfLoaded(pos: BlockPos): BlockState?

        fun setIfLoaded(pos: BlockPos, expected: BlockState, replacement: BlockState)
    }

    /** A FULL future must be complete; visibility/currentlyLoading alone does not prove readiness. */
    class ServerWorldAccess(private val level: ServerLevel) : WorldAccess {
        private fun fullChunk(pos: BlockPos): LevelChunk? {
            if (!level.server.isSameThread) return null
            val source = level.chunkSource
            // This getter only inspects completed futures. Earlier generation stages return
            // ProtoChunk/ImposterProtoChunk, never the fully loaded LevelChunk required here.
            val complete = source.getChunkForLighting(pos.x shr 4, pos.z shr 4) as? LevelChunk ?: return null
            return complete.takeIf { source.getChunkNow(pos.x shr 4, pos.z shr 4) === it }
        }

        override fun stateIfLoaded(pos: BlockPos): BlockState? = fullChunk(pos)?.getBlockState(pos)

        override fun setIfLoaded(pos: BlockPos, expected: BlockState, replacement: BlockState) {
            val chunk = fullChunk(pos) ?: return
            if (chunk.getBlockState(pos) == expected) {
                // No neighbour or shape propagation into adjacent loading chunks.
                level.setBlock(pos, replacement, Block.UPDATE_CLIENTS or Block.UPDATE_KNOWN_SHAPE)
            }
        }
    }

    fun update(machine: LightHunterSpaceStation, enabled: Boolean) {
        val level = machine.level as? ServerLevel ?: return
        if (!level.server.isSameThread) return
        reconcile(ServerWorldAccess(level), machine.pos, machine.frontFacing, GTLAddBlocks.LIGHT_HUNTER_INTERIOR_LIGHT.get(), enabled)
    }

    fun reconcile(world: WorldAccess, host: BlockPos, facing: Direction, light: LightHunterInteriorLightBlock, enabled: Boolean) {
        for (slot in 0 until LightHunterInteriorLayout.LIGHT_COUNT) {
            val pos = host.offset(LightHunterInteriorLayout.lightOffset(slot, facing))
            val current = world.stateIfLoaded(pos) ?: continue
            if (enabled && current.isAir) {
                world.setIfLoaded(
                    pos,
                    current,
                    light.defaultBlockState()
                        .setValue(LightHunterInteriorLightBlock.FACING, facing)
                        .setValue(LightHunterInteriorLightBlock.SLOT, slot)
                )
            } else if (!enabled && current.`is`(light) && LightHunterInteriorLightBlock.ownerPosition(current, pos) == host) {
                world.setIfLoaded(pos, current, Blocks.AIR.defaultBlockState())
            }
        }
    }
}