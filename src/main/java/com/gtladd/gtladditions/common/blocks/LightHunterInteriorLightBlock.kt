package com.gtladd.gtladditions.common.blocks

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gtladd.gtladditions.common.machine.multiblock.controller.LightHunterSpaceStation
import com.gtladd.gtladditions.utils.LightHunterInteriorLayout
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

@Suppress("OVERRIDE_DEPRECATION")
class LightHunterInteriorLightBlock(properties: Properties) : Block(properties) {
    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.EAST).setValue(SLOT, 0))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, SLOT)
    }

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.INVISIBLE

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape = Shapes.empty()

    override fun getShadeBrightness(state: BlockState, level: BlockGetter, pos: BlockPos): Float = 1f

    override fun propagatesSkylightDown(state: BlockState, level: BlockGetter, pos: BlockPos): Boolean = true

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, movedByPiston: Boolean) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 20)
    }

    override fun tick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
        val owner = ownerPosition(state, pos)
        // Do not load a controller chunk or extinguish a valid ship merely because it is unloaded.
        val ownerChunk = level.chunkSource.getChunkNow(owner.x shr 4, owner.z shr 4)
        if (ownerChunk != null) {
            val machine = (ownerChunk.getBlockEntity(owner) as? IMachineBlockEntity)?.metaMachine as? LightHunterSpaceStation
            if (machine == null || !machine.isFormed || machine.frontFacing != state.getValue(FACING)) {
                if (level.getBlockState(pos) == state) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_CLIENTS or UPDATE_KNOWN_SHAPE)
                }
                return
            }
        }

        level.scheduleTick(pos, this, 80 + state.getValue(SLOT))
    }

    companion object {
        val FACING: DirectionProperty = BlockStateProperties.HORIZONTAL_FACING
        val SLOT: IntegerProperty = IntegerProperty.create("slot", 0, LightHunterInteriorLayout.LIGHT_COUNT - 1)

        fun lightLevel(state: BlockState): Int = LightHunterInteriorLayout.lightLevel(state.getValue(SLOT))

        fun ownerPosition(state: BlockState, pos: BlockPos): BlockPos =
            pos.subtract(LightHunterInteriorLayout.lightOffset(state.getValue(SLOT), state.getValue(FACING)))
    }
}
