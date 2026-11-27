package com.gtladd.gtladditions.common.machine.trait

import com.gregtechceu.gtceu.api.block.ActiveBlock
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait
import com.gregtechceu.gtceu.api.pattern.BlockPattern
import com.gregtechceu.gtceu.api.pattern.MultiblockState
import com.gregtechceu.gtceu.api.pattern.MultiblockWorldSavedData
import com.gtladd.gtladditions.api.machine.feature.IStructureTierMachine
import com.gtladd.gtladditions.api.machine.feature.TierRequirement
import com.gtladd.gtladditions.api.pattern.RemainingStructurePattern
import com.gtladd.gtladditions.api.pattern.StructureTierLayout
import com.gtladd.gtladditions.client.gui.StructureTierPreview
import com.gtladd.gtladditions.mixin.gtceu.api.machine.WorkableMultiblockMachineAccessor
import com.gtladd.gtladditions.mixin.gtceu.api.pattern.MultiblockStateAccessor
import com.gtladd.gtladditions.utils.ComponentExtensions.literal
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.annotation.UpdateListener
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.ints.IntSet
import it.unimi.dsi.fastutil.ints.IntSets
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import it.unimi.dsi.fastutil.longs.LongSets
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.TickTask
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.AABB
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.common.item.UltimateTerminalBehavior
import java.util.PriorityQueue
import kotlin.concurrent.withLock

class StructureTierTrait(
    machine: MultiblockControllerMachine,
    val layout: StructureTierLayout,
    private val structureModify: StructureModifyTrait? = null,
    private val sharedContextKeys: Set<String> = setOf("CoilType"),
    private val onTierSynced: (IntRange) -> Unit = {}
) : MachineTrait(machine) {

    @field:Persisted
    @field:DescSynced
    @field:UpdateListener(methodName = "onShownTiersChanged")
    var structureTier = 0
        private set

    @field:Persisted
    @field:DescSynced
    @field:UpdateListener(methodName = "onShownTiersChanged")
    var unlockedTier = layout.minTier
        private set

    @field:Persisted
    var payment = LongArray(0)
        private set

    internal var nextGuiActivationTick = 0L

    val hasDropData: Boolean
        get() = unlockedTier > layout.minTier || payment.any { it > 0 }

    val hasPendingTier: Boolean
        get() = controller.isFormed && structureTier < unlockedTier

    private val controller: MultiblockControllerMachine
        get() = machine as MultiblockControllerMachine

    private var physicalActiveBlocks: LongSet = LongSets.EMPTY_SET

    private val virtualMask: IntSet by lazy {
        structureModify?.let { RemainingStructurePattern.virtualCells(layout.pattern, it.renderType) }
            ?: IntSets.EMPTY_SET
    }

    private val virtualCells: Array<IntArray> by lazy {
        Array(layout.maxTier + 1) { tier ->
            if (virtualMask.isEmpty()) {
                IntArray(0)
            } else {
                layout.cells(tier).filter(virtualMask::contains).toIntArray()
            }
        }
    }

    private val physicalCells: Array<IntArray> by lazy {
        Array(layout.maxTier + 1) { tier ->
            val cells = layout.cells(tier)
            when (virtualCells[tier].size) {
                0 -> cells
                cells.size -> IntArray(0)
                else -> cells.filterNot(virtualMask::contains).toIntArray()
            }
        }
    }

    private val hasPhysicalUpperCells: Boolean by lazy {
        hasPhysicalCells(layout.minTier + 1..layout.maxTier)
    }

    private val basePattern: BlockPattern by lazy {
        when {
            structureModify == null -> layout.base
            !hasPhysicalUpperCells -> RemainingStructurePattern.create(layout.pattern, structureModify.renderType)
            else -> layout.templateCells { tier, cell -> tier <= layout.minTier && !virtualMask.contains(cell) }
        }
    }

    // ========================================
    // TIER SELECTION & UNLOCK
    // ========================================

    @Suppress("unused")
    @OnlyIn(Dist.CLIENT)
    fun onShownTiersChanged(newValue: Int, oldValue: Int) {
        StructureTierPreview.refresh(machine as IStructureTierMachine)
    }

    fun requirement(tier: Int): TierRequirement? = (machine as IStructureTierMachine).tierRequirement(tier)

    fun paid(tier: Int, index: Int): Long = when {
        tier <= unlockedTier -> requirement(tier)?.costs?.getOrNull(index)?.amount ?: 0L
        tier == unlockedTier + 1 -> payment.getOrElse(index) { 0L }
        else -> 0L
    }

    fun unlock(player: Player): List<Component> {
        val tier = unlockedTier + 1
        val requirement = requirement(tier) ?: return emptyList()

        if (player.isCreative) return completeUnlock(tier)

        val items = player.inventory.items
        val paid = LongArray(requirement.costs.size) { paid(tier, it) }
        var consumed = false
        requirement.costs.forEachIndexed { i, cost ->
            for (element in items) {
                val missing = cost.amount - paid[i]
                if (missing <= 0L) break

                if (element.isEmpty || !cost.ingredient.test(element)) continue

                val taken = minOf(missing, element.count.toLong()).toInt()
                element.shrink(taken)
                paid[i] += taken
                consumed = true
            }
        }

        if (consumed) {
            player.inventory.setChanged()
        }

        val unpaid = requirement.costs.indices.count { paid[it] < requirement.costs[it].amount }
        val unmet = requirement.conditions.filterNot { it.test(machine as IStructureTierMachine) }
        if (unpaid == 0 && unmet.isEmpty()) return completeUnlock(tier)

        if (consumed) {
            payment = paid
            onChanged()
        }

        return buildList {
            add("gtladditions.structure_tier.not_unlocked".toComponent(tier).withStyle(ChatFormatting.RED))
            if (unpaid > 0) add("gtladditions.structure_tier.unpaid".toComponent(unpaid).withStyle(ChatFormatting.GRAY))

            for (condition in unmet) {
                add(
                    " - ".literal
                        .withStyle(ChatFormatting.GRAY)
                        .append(condition.describe(machine as IStructureTierMachine))
                )
            }
        }
    }

    private fun completeUnlock(tier: Int): List<Component> {
        unlockedTier = tier
        payment = LongArray(0)
        onChanged()
        return listOf("gtladditions.structure_tier.unlocked".toComponent(tier).withStyle(ChatFormatting.GREEN))
    }

    // ========================================
    // BUILDING & ACTIVATION
    // ========================================

    /** [cells] are nearest first; [chunks] are the unloaded parts the scan could not read. */
    class Activation(
        val messages: List<Component>,
        val cells: List<BlockPos>,
        val chunks: List<AABB>
    )

    fun autoBuild(player: Player, setting: UltimateTerminalBehavior.AutoBuildSetting) {
        val formed = controller.isFormed
        val previousTier = structureTier
        val replace = formed && setting.isReplaceMode && setting.blocks.isNotEmpty() &&
            controller.multiblockState.error !== MultiblockState.UNLOAD_ERROR &&
            hasReplaceableCells(0..previousTier, setting.blocks)
        val first = if (formed && !replace) previousTier + 1 else 0
        val last = unlockedTier
        if (first > last) {
            player.sendSystemMessage("gtladditions.structure_tier.nothing_to_build".toComponent)
            return
        }

        if (formed) {
            setting.isFlipped = controller.isFlipped
            if (!replace) setting.isReplaceMode = false
        }

        // The terminal clears its working state before placing blocks. Keep the live match and its mapped positions intact.
        val world = player.level()
        val buildState = MultiblockState(world, controller.pos)
        layout.template { it in first..last }.autoBuild(player, buildState, setting)

        if (!controller.isFormed) {
            if (formed) {
                (world as? ServerLevel)?.let {
                    MultiblockWorldSavedData.getOrCreate(it).removeMapping(controller.multiblockState)
                }
                controller.onPartUnload()
            }
            return
        }

        val origin = player.blockPosition()
        val finish = Runnable { activate(origin).messages.forEach(player::sendSystemMessage) }

        // Forge publishes the placement snapshots after the terminal returns. Register the new cells after those notifications.
        if (world is ServerLevel && world.captureBlockSnapshots && hasPhysicalCells(first..last)) {
            world.server.tell(TickTask(world.server.tickCount, finish))
        } else {
            finish.run()
        }
    }

    /** [origin] orders the reported problems by distance. */
    fun activate(origin: BlockPos): Activation {
        controller.patternLock.withLock {
            if (controller.isInValid || !controller.isFormed || controller.multiblockState.hasError()) {
                return Activation(
                    listOf("gtladditions.structure_tier.not_formed".toComponent),
                    emptyList(),
                    emptyList()
                )
            }

            if (structureTier >= unlockedTier) {
                val message = if (unlockedTier < layout.maxTier) {
                    "gtladditions.structure_tier.unlock_first".toComponent(unlockedTier + 1)
                } else {
                    "gtladditions.structure_tier.all_active".toComponent
                }
                return Activation(listOf(message), emptyList(), emptyList())
            }

            val oldTier = structureTier
            recoverSharedContext(controller.multiblockState, 1..oldTier)
            val scan = scan(oldTier + 1, unlockedTier, origin, skipRemoved = false)
            val messages = ArrayList<Component>()

            if (scan.complete > oldTier) {
                structureTier = scan.complete
                scan.sharedContext.forEach(controller.multiblockState.matchContext::set)
                onChanged()

                if (hasPhysicalUpperCells) {
                    val active = LongOpenHashSet(physicalActiveBlocks).apply { addAll(scan.activeBlocks) }
                    syncPhysical(machine.level as ServerLevel, active)
                }

                structureModify?.capture(virtualPositions(oldTier + 1..structureTier))
                onTierSynced(1..structureTier)
                if (hasPhysicalUpperCells) updateActiveBlocks()

                messages += "gtladditions.structure_tier.activated".toComponent(structureTier)
                    .withStyle(ChatFormatting.GREEN)
            }

            if (scan.problems.isNotEmpty()) {
                messages += "gtladditions.structure_tier.incomplete"
                    .toComponent(scan.tier, scan.missing, scan.wrong, scan.problems.size)
                    .withStyle(ChatFormatting.RED)

                for ((pos, cell) in scan.problems.take(LISTED)) {
                    val position = "[${pos.x}, ${pos.y}, ${pos.z}]"
                    val expected = layout.predicate(cell).common
                        .firstNotNullOfOrNull { it.candidates?.get()?.firstOrNull() }?.blockState?.block?.name
                    val current = controller.level!!.getBlockState(pos)

                    messages += if (current.isAir) {
                        "gtladditions.structure_tier.needs".toComponent(position, expected)
                    } else {
                        "gtladditions.structure_tier.needs_instead".toComponent(position, expected, current.block.name)
                    }
                }
            }

            if (scan.unloaded.isNotEmpty()) {
                messages += "gtladditions.structure_tier.unloaded"
                    .toComponent(scan.tier, scan.unloaded.size)
                    .withStyle(ChatFormatting.YELLOW)
            }

            return Activation(messages, scan.problems.map { it.first }, scan.unloaded)
        }
    }

    private fun hasReplaceableCells(tiers: IntRange, blocks: Set<Block>): Boolean {
        for (tier in tiers) {
            for (cell in layout.distinctPredicateCells(tier)) {
                val predicate = layout.predicate(cell)
                var total = 0
                var matched = false

                for (part in predicate.common) {
                    val candidates = part.candidates?.get() ?: continue
                    total += candidates.size
                    if (!matched && candidates.any { it.blockState.block in blocks }) matched = true
                }

                for (part in predicate.limited) {
                    val candidates = part.candidates?.get() ?: continue
                    total += candidates.size
                    if (!matched && candidates.any { it.blockState.block in blocks }) matched = true
                }

                if (matched && total > 1) return true
            }
        }

        return false
    }

    // ========================================
    // LIFECYCLE
    // ========================================

    fun checkPattern(): Boolean {
        val state = controller.multiblockState
        if (!basePattern.checkPatternAt(state, false)) return false

        if (structureModify != null && !structureModify.hasRemovedStructure) {
            for (tier in 1..layout.minTier) {
                if (!layout.matches(virtualCells[tier], state, controller)) return false
            }
        }

        val level = machine.level as? ServerLevel ?: return true
        if (!hasPhysicalUpperCells || structureTier <= layout.minTier) return true

        val checked = LongOpenHashSet()
        for (tier in layout.minTier + 1..structureTier) {
            for (cell in physicalCells[tier]) {
                val pos = layout.position(cell, controller, state.isNeededFlip)
                if (checked.add(ChunkPos.asLong(pos)) && !level.chunkSource.hasChunk(pos.x shr 4, pos.z shr 4)) {
                    state.setError(MultiblockState.UNLOAD_ERROR)
                    return false
                }
            }
        }

        return true
    }

    fun onStructureFormed() {
        // JEI also forms controllers in a TrackedDummyWorld.
        val level = machine.level as? ServerLevel ?: return

        controller.patternLock.withLock {
            recoverSharedContext(controller.multiblockState, 1..layout.minTier)

            val oldTier = structureTier
            val lastTier = if (oldTier > 0) oldTier else unlockedTier
            val scan = scan(layout.minTier + 1, lastTier, controller.pos, skipRemoved = true)

            structureTier = scan.complete
            scan.sharedContext.forEach(controller.multiblockState.matchContext::set)
            onChanged()

            val syncActive = hasPhysicalUpperCells || physicalActiveBlocks.isNotEmpty()
            if (syncActive) syncPhysical(level, scan.activeBlocks)

            if (structureModify != null) {
                if (structureTier < oldTier) {
                    structureModify.restore(virtualPositions(structureTier + 1..oldTier))
                }
                structureModify.capture(virtualPositions(1..structureTier, skipRemoved = true))
            }

            onTierSynced(1..structureTier)
            if (syncActive) updateActiveBlocks()
        }
    }

    fun onStructureInvalid() {
        if (controller.multiblockState.error === MultiblockState.UNLOAD_ERROR) return

        (machine.level as? ServerLevel)?.let { extinguish(it, LongSets.EMPTY_SET) }
        onMachineRemoved()
        onChanged()
    }

    fun onMachineRemoved() {
        physicalActiveBlocks = LongSets.EMPTY_SET
        structureModify?.restore()
        structureTier = 0
    }

    fun onStructureRestored() {
        structureTier = 0
    }

    // ========================================
    // SYNC & SCANNING
    // ========================================

    private fun hasPhysicalCells(tiers: IntRange): Boolean = tiers.any { physicalCells[it].isNotEmpty() }

    private fun syncPhysical(level: ServerLevel, active: LongSet) {
        val state = controller.multiblockState
        val saved = MultiblockWorldSavedData.getOrCreate(level)

        saved.removeMapping(state)
        for (tier in layout.minTier + 1..structureTier) {
            for (cell in physicalCells[tier]) {
                state.addPosCache(layout.position(cell, controller))
            }
        }
        saved.addMapping(state)

        extinguish(level, active)
        physicalActiveBlocks = active

        val merged = LongOpenHashSet(state.matchContext.getOrDefault<LongSet>("vaBlocks", LongSets.EMPTY_SET))
        merged.addAll(active)
        state.matchContext.set("vaBlocks", merged)
        (controller as? WorkableMultiblockMachineAccessor)?.setActiveBlocks(merged)
    }

    private fun extinguish(level: ServerLevel, keep: LongSet) {
        val previous = physicalActiveBlocks.iterator()
        while (previous.hasNext()) {
            val key = previous.nextLong()
            if (keep.contains(key)) continue

            val pos = BlockPos.of(key)
            if (!level.isLoaded(pos)) continue

            val state = level.getBlockState(pos)
            val block = state.block
            if (block is ActiveBlock && block.isActive(state)) {
                level.setBlock(
                    pos,
                    block.changeActive(state, false),
                    Block.UPDATE_CLIENTS or Block.UPDATE_KNOWN_SHAPE
                )
            }
        }
    }

    private fun updateActiveBlocks() {
        (controller as? WorkableMultiblockMachine)?.let { it.updateActiveBlocks(it.recipeLogic.isWorking) }
    }

    private fun isCaptured(cells: IntArray): Boolean =
        cells.isNotEmpty() && structureModify?.contains(layout.position(cells[0], controller)) == true

    private fun virtualPositions(tiers: IntRange, skipRemoved: Boolean = false): List<BlockPos> = buildList {
        for (tier in tiers) {
            val cells = virtualCells[tier]
            if (skipRemoved && isCaptured(cells)) continue

            for (cell in cells) {
                add(layout.position(cell, controller))
            }
        }
    }

    private fun recoverSharedContext(target: MultiblockState, tiers: IntRange) {
        if (structureModify?.hasRemovedStructure != true) return
        if (sharedContextKeys.all { target.matchContext.get<Any>(it) != null }) return

        val scratch = MultiblockState(target.world, controller.pos)
        val accessor = scratch as MultiblockStateAccessor

        for (tier in tiers) {
            for (cell in layout.distinctPredicateCells(tier)) {
                if (!virtualMask.contains(cell)) continue

                val pos = layout.position(cell, controller)
                val block = structureModify.blockState(pos) ?: continue

                accessor.setPos(pos)
                accessor.setBlockState(block)
                val predicate = layout.predicate(cell)
                scratch.predicate = predicate
                if (!predicate.test(scratch)) continue

                for (key in sharedContextKeys) {
                    if (target.matchContext.get<Any>(key) == null) {
                        scratch.matchContext.get<Any>(key)?.let { target.matchContext.set(key, it) }
                    }
                }

                if (sharedContextKeys.all { target.matchContext.get<Any>(it) != null }) return
            }
        }
    }

    /** [complete] is the last complete tier; the other fields describe [tier], the first incomplete one. */
    private class Scan(
        val complete: Int,
        val sharedContext: Map<String, Any>,
        val activeBlocks: LongSet,
        val tier: Int = 0,
        val missing: Int = 0,
        val wrong: Int = 0,
        val problems: List<Pair<BlockPos, Int>> = emptyList(),
        val unloaded: List<AABB> = emptyList()
    )

    /** Never loads chunks. */
    private fun scan(from: Int, to: Int, origin: BlockPos, skipRemoved: Boolean): Scan {
        val state = MultiblockState(machine.level as ServerLevel, controller.pos)
        for (key in sharedContextKeys) {
            controller.multiblockState.matchContext.get<Any>(key)?.let { state.matchContext.set(key, it) }
        }

        val shared = HashMap<String, Any>()
        val active = if (hasPhysicalUpperCells) LongOpenHashSet() else LongSets.EMPTY_SET

        for (tier in from..to) {
            val cells = if (skipRemoved && tier <= structureTier && isCaptured(virtualCells[tier])) {
                recoverSharedContext(state, tier..tier)
                physicalCells[tier]
            } else {
                layout.cells(tier)
            }

            var tierActive: LongOpenHashSet? = null
            var missing = 0
            var wrong = 0
            val nearest = PriorityQueue<Pair<BlockPos, Int>>(compareByDescending { it.first.distSqr(origin) })
            val unloaded = Long2ObjectOpenHashMap<AABB>()

            for (cell in cells) {
                val predicate = layout.predicate(cell)
                val pos = layout.position(cell, controller)
                if (!(state as MultiblockStateAccessor).invokeUpdate(pos, predicate)) {
                    val key = ChunkPos.asLong(pos.x shr 4, pos.z shr 4)
                    unloaded[key] = unloaded[key]?.minmax(AABB(pos)) ?: AABB(pos)
                    continue
                }

                if (predicate.test(state)) {
                    if (!virtualMask.contains(cell) && state.blockState.block is ActiveBlock) {
                        if (tierActive == null) tierActive = LongOpenHashSet()
                        tierActive.add(pos.asLong())
                    }
                    continue
                }

                if (state.blockState.isAir) missing++ else wrong++
                nearest.add(pos to cell)
                if (nearest.size > HIGHLIGHTED) nearest.poll()
            }

            if (missing + wrong > 0 || unloaded.isNotEmpty()) {
                val chunks = unloaded.long2ObjectEntrySet().map { entry ->
                    val x = ChunkPos.getX(entry.longKey) * 16.0
                    val z = ChunkPos.getZ(entry.longKey) * 16.0
                    AABB(x, entry.value.minY, z, x + 16, entry.value.maxY, z + 16)
                }

                return Scan(
                    tier - 1,
                    shared,
                    active,
                    tier,
                    missing,
                    wrong,
                    nearest.sortedBy { it.first.distSqr(origin) },
                    chunks
                )
            }

            // A failed tier must not publish even its correctly matched cells or their shared attributes.
            tierActive?.let { active.addAll(it) }
            for (key in sharedContextKeys) {
                state.matchContext.get<Any>(key)?.let { shared[key] = it }
            }
        }

        return Scan(maxOf(to, from - 1), shared, active)
    }

    // ========================================
    // PERSISTENCE
    // ========================================

    override fun saveCustomPersistedData(tag: CompoundTag, forDrop: Boolean) {
        if (!forDrop) return

        tag.put(
            DROP_KEY,
            CompoundTag().apply {
                putInt("unlocked", unlockedTier)
                putLongArray("payment", payment)
            }
        )
    }

    override fun loadCustomPersistedData(tag: CompoundTag) {
        if (!tag.contains(DROP_KEY)) return

        val drop = tag.getCompound(DROP_KEY)
        unlockedTier = drop.getInt("unlocked")
        payment = drop.getLongArray("payment")
    }

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        const val DROP_KEY = "gtladditionsStructureTier"

        private const val HIGHLIGHTED = 64
        private const val LISTED = 3
        private val MANAGED_FIELD_HOLDER = ManagedFieldHolder(StructureTierTrait::class.java)
    }
}
