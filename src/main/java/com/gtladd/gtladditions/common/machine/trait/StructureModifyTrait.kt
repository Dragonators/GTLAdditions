package com.gtladd.gtladditions.common.machine.trait

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait
import com.gregtechceu.gtceu.api.pattern.BlockPattern
import com.gregtechceu.gtceu.api.pattern.MultiblockState
import com.gtladd.gtladditions.api.pattern.RemainingStructurePattern
import com.gtladd.gtladditions.api.pattern.StructureTierLayout
import com.gtladd.gtladditions.common.data.RemovedStructure
import com.gtladd.gtladditions.common.data.StructureRenderType
import com.gtladd.gtladditions.common.machine.multiblock.structure.StructureBlockLayout
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import kotlin.concurrent.withLock

class StructureModifyTrait(
    machine: MultiblockControllerMachine,
    private val type: StructureRenderType,
    private val tiers: StructureTierLayout? = null,
    private val onRestored: () -> Unit = {}
) : MachineTrait(machine) {
    private lateinit var remainingPattern: BlockPattern

    @Volatile
    private var removedStructure = RemovedStructure.EMPTY
    private var restoring = false

    val hasRemovedStructure: Boolean
        get() = !removedStructure.isEmpty

    override fun onMachineLoad() {
        if (machine.level !is ServerLevel || removedStructure.isEmpty) return
        prepareRemainingPattern()
    }

    fun checkPattern(): Boolean {
        val controller = machine as MultiblockControllerMachine
        val state = controller.multiblockState
        if (tiers == null) {
            // The remaining template is already prepared by formation or machine loading.
            val validation = if (removedStructure.isEmpty) controller.pattern else remainingPattern
            return validation.checkPatternAt(state, false)
        }

        prepareRemainingPattern()
        return remainingPattern.checkPatternAt(state, false) &&
            (!removedStructure.isEmpty || (1..tiers.minTier).all { tiers.matches(it, state, controller) })
    }

    fun form() = form {
        val controller = machine as MultiblockControllerMachine
        StructureBlockLayout.positions(type, controller.pos, controller.frontFacing, controller.upwardsFacing, controller.isFlipped)
    }

    fun form(tierRange: IntRange) = form { tiers!!.positions(tierRange, machine as MultiblockControllerMachine) }

    private fun form(positions: () -> Collection<BlockPos>) {
        val level = machine.level as? ServerLevel ?: return
        val controller = machine as MultiblockControllerMachine
        controller.patternLock.withLock {
            if (!removedStructure.isEmpty || restoring) return

            prepareRemainingPattern()
            remove(level, controller, positions())
        }
    }

    /** [tierRange] must have been checked in the same tick. */
    fun extend(tierRange: IntRange) {
        val level = machine.level as? ServerLevel ?: return
        val controller = machine as MultiblockControllerMachine
        controller.patternLock.withLock { remove(level, controller, tiers!!.positions(tierRange, controller)) }
    }

    private fun remove(level: ServerLevel, controller: MultiblockControllerMachine, positions: Collection<BlockPos>) {
        val captured = RemovedStructure.capture(
            positions,
            level::getBlockState
        ) { level.getBlockEntity(it)?.saveWithFullMetadata() }
        removedStructure += captured
        // Remove these positions before erasure, so Core does not enqueue one recheck per removed block.
        positions.forEach { controller.multiblockState.cache.remove(it.asLong()) }
        onChanged()
        captured.erase { pos, state, data -> replace(level, pos, state, data) }
    }

    fun invalidate() {
        val controller = machine as MultiblockControllerMachine
        // A partially unloaded structure is rechecked after its chunks return, without loading them during unload.
        if (controller.multiblockState.error === MultiblockState.UNLOAD_ERROR) return
        restore()
    }

    fun restore() {
        val level = machine.level as? ServerLevel ?: return
        val controller = machine as MultiblockControllerMachine
        controller.patternLock.withLock {
            if (removedStructure.isEmpty || restoring) return

            restoring = true
            try {
                removedStructure.restore { pos, state, data -> replace(level, pos, state, data) }
                removedStructure = RemovedStructure.EMPTY
                onChanged()
                onRestored()
            } finally {
                restoring = false
            }
        }
    }

    private fun prepareRemainingPattern() {
        if (!::remainingPattern.isInitialized) {
            remainingPattern = RemainingStructurePattern.create((machine as MultiblockControllerMachine).pattern, type)
        }
    }

    private fun replace(level: ServerLevel, pos: BlockPos, state: BlockState, data: CompoundTag?) {
        // Resolve the chunk before removing its block entity, including when restoration crosses unloaded chunks.
        level.getChunk(pos)
        val current = level.getBlockState(pos)
        if (current.hasBlockEntity()) {
            // Recreate even an identical block entity so intervening data cannot survive restoration.
            level.removeBlockEntity(pos)
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS)
            if (!state.isAir) level.setBlock(pos, state, FLAGS)
        } else if (current !== state) {
            // Forge removes a no-op write's snapshot by a linear list search during item use.
            // Writing AIR to each already-empty cell makes a large restoration quadratic.
            level.setBlock(pos, state, FLAGS)
        }

        data?.let { level.getBlockEntity(pos)?.load(it) }
    }

    override fun saveCustomPersistedData(tag: CompoundTag, forDrop: Boolean) {
        if (!forDrop && !removedStructure.isEmpty) {
            tag.put(NBT_KEY, removedStructure.save())
        } else {
            tag.remove(NBT_KEY)
        }
    }

    override fun loadCustomPersistedData(tag: CompoundTag) {
        removedStructure = RemovedStructure.load(tag.getCompound(NBT_KEY))
    }

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        private const val NBT_KEY = "gtladditionsRemovedStructure"
        private const val FLAGS =
            Block.UPDATE_CLIENTS or Block.UPDATE_KNOWN_SHAPE or Block.UPDATE_SUPPRESS_DROPS or Block.UPDATE_MOVE_BY_PISTON
        private val MANAGED_FIELD_HOLDER = ManagedFieldHolder(StructureModifyTrait::class.java)
    }
}