package com.gtladd.gtladditions.common.machine.trait

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait
import com.gregtechceu.gtceu.api.pattern.MultiblockState
import com.gtladd.gtladditions.api.machine.feature.IStructureTierMachine
import com.gtladd.gtladditions.api.machine.feature.TierRequirement
import com.gtladd.gtladditions.api.pattern.StructureTierLayout
import com.gtladd.gtladditions.client.gui.StructureTierPreview
import com.gtladd.gtladditions.mixin.gtceu.api.pattern.MultiblockStateAccessor
import com.gtladd.gtladditions.utils.ComponentExtensions.literal
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.annotation.UpdateListener
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.TickTask
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ChunkPos
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
    private val onPhysicalStructure: (IntRange) -> Unit = {}
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
    @field:DescSynced
    var payment = LongArray(0)
        private set

    /** 0 follows the next tier to unlock. */
    @field:Persisted
    @field:DescSynced
    @field:UpdateListener(methodName = "onShownTiersChanged")
    private var chosenTier = 0

    @field:Persisted
    val inventory = ItemStackTransfer(SLOTS)

    val selectedTier: Int
        get() = if (chosenTier in layout.minTier..layout.maxTier) {
            chosenTier
        } else {
            minOf(
                unlockedTier + 1,
                layout.maxTier
            )
        }

    val buildTier: Int
        get() = minOf(selectedTier, unlockedTier)

    val hasDropData: Boolean
        get() = unlockedTier > layout.minTier || payment.any { it > 0 }

    val hasPendingTier: Boolean
        get() = (machine as MultiblockControllerMachine).isFormed && structureTier < unlockedTier

    // ========================================
    // TIER SELECTION & UNLOCK
    // ========================================

    @Suppress("unused")
    @OnlyIn(Dist.CLIENT)
    fun onShownTiersChanged(newValue: Int, oldValue: Int) {
        StructureTierPreview.refresh(machine as IStructureTierMachine)
    }

    fun select(tier: Int) {
        chosenTier = tier.coerceIn(layout.minTier, layout.maxTier)
        onChanged()
    }

    fun requirement(tier: Int): TierRequirement? = (machine as IStructureTierMachine).tierRequirement(tier)

    fun paid(tier: Int, index: Int): Long = when {
        tier <= unlockedTier -> requirement(tier)?.costs?.getOrNull(index)?.amount ?: 0L
        tier == unlockedTier + 1 -> payment.getOrElse(index) { 0L }
        else -> 0L
    }

    fun unlock(tier: Int): List<Component> {
        if (tier != unlockedTier + 1) return emptyList()
        val requirement = requirement(tier) ?: return emptyList()

        val paid = LongArray(requirement.costs.size) { paid(tier, it) }
        requirement.costs.forEachIndexed { i, cost ->
            for (slot in 0 until inventory.slots) {
                val missing = cost.amount - paid[i]
                if (missing <= 0) break

                val stack = inventory.getStackInSlot(slot)
                if (stack.isEmpty || !cost.ingredient.test(stack)) continue

                paid[i] += inventory.extractItem(
                    slot,
                    minOf(missing, stack.count.toLong()).toInt(),
                    false
                ).count.toLong()
            }
        }

        payment = paid
        val unpaid = requirement.costs.indices.count { paid[it] < requirement.costs[it].amount }
        val unmet = requirement.conditions.filterNot { it.test(machine as IStructureTierMachine) }
        if (unpaid == 0 && unmet.isEmpty()) {
            unlockedTier = tier
            payment = LongArray(0)
            chosenTier = 0
        }

        onChanged()
        if (unlockedTier == tier) {
            return listOf("gtladditions.structure_tier.unlocked".toComponent(tier).withStyle(ChatFormatting.GREEN))
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
        val controller = machine as MultiblockControllerMachine
        val first = if (controller.isFormed) structureTier + 1 else 0
        if (first > buildTier) {
            player.sendSystemMessage("gtladditions.structure_tier.nothing_to_build".toComponent)
            return
        }

        // The terminal clears its working state before placing blocks. Keep the live match and its mapped positions intact.
        val world = player.level()
        val buildState = MultiblockState(world, controller.pos)
        layout.template { it in first..buildTier }.autoBuild(player, buildState, setting)
        if (!controller.isFormed) return

        val origin = player.blockPosition()
        val finish = Runnable { activate(origin).messages.forEach(player::sendSystemMessage) }

        // Forge publishes the placement snapshots after the terminal returns. Register the new cells after those notifications.
        if (structureModify == null && world is ServerLevel && world.captureBlockSnapshots) {
            world.server.tell(TickTask(world.server.tickCount, finish))
        } else {
            finish.run()
        }
    }

    /** [origin] orders the reported problems by distance. */
    fun activate(origin: BlockPos): Activation {
        val controller = machine as MultiblockControllerMachine
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

            val scan = scan(structureTier + 1, unlockedTier, origin)
            val messages = ArrayList<Component>()
            if (scan.complete > structureTier) {
                structureModify?.extend(structureTier + 1..scan.complete)
                structureTier = scan.complete
                if (structureModify == null) onPhysicalStructure(1..structureTier)
                onChanged()
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

    // ========================================
    // LIFECYCLE & SCANNING
    // ========================================

    fun onStructureFormed() {
        // JEI also forms controllers in a TrackedDummyWorld.
        if (machine.level !is ServerLevel) return

        val controller = machine as MultiblockControllerMachine
        controller.patternLock.withLock {
            // Formed again after an unload or a recheck: the active tiers stand.
            if (structureModify?.hasRemovedStructure == true) return

            // Rechecking physical stages can lower the active tier. Only activation extends it again.
            val lastTier = if (structureModify == null && structureTier > 0) structureTier else unlockedTier
            val scan = scan(layout.minTier + 1, lastTier, controller.pos)
            structureTier = scan.complete
            onChanged()

            if (structureModify != null) {
                structureModify.form(1..structureTier)
            } else {
                onPhysicalStructure(1..structureTier)
            }
        }
    }

    fun onStructureRestored() {
        structureTier = 0
    }

    /** [complete] is the last complete tier; the other fields describe [tier], the first incomplete one. */
    private class Scan(
        val complete: Int,
        val tier: Int = 0,
        val missing: Int = 0,
        val wrong: Int = 0,
        val problems: List<Pair<BlockPos, Int>> = emptyList(),
        val unloaded: List<AABB> = emptyList()
    )

    /** Never loads chunks. */
    private fun scan(from: Int, to: Int, origin: BlockPos): Scan {
        val controller = machine as MultiblockControllerMachine
        val state = MultiblockState(machine.level as ServerLevel, controller.pos)
        if (structureModify == null) {
            controller.multiblockState.matchContext.get<Any>("CoilType")?.let { state.matchContext.set("CoilType", it) }
        }

        for (tier in from..to) {
            var missing = 0
            var wrong = 0
            val nearest = PriorityQueue<Pair<BlockPos, Int>>(compareByDescending { it.first.distSqr(origin) })
            val unloaded = Long2ObjectOpenHashMap<AABB>()

            for (cell in layout.cells(tier)) {
                val predicate = layout.predicate(cell)
                val pos = layout.position(cell, controller)
                if (!(state as MultiblockStateAccessor).invokeUpdate(pos, predicate)) {
                    val key = ChunkPos.asLong(pos.x shr 4, pos.z shr 4)
                    unloaded[key] = unloaded[key]?.minmax(AABB(pos)) ?: AABB(pos)
                    continue
                }

                if (predicate.test(state)) continue
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
                return Scan(tier - 1, tier, missing, wrong, nearest.sortedBy { it.first.distSqr(origin) }, chunks)
            }
        }

        return Scan(maxOf(to, from - 1))
    }

    // ========================================
    // PERSISTENCE
    // ========================================

    // The input slots drop as items.
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
        const val SLOTS = 16
        const val DROP_KEY = "gtladditionsStructureTier"

        private const val HIGHLIGHTED = 64
        private const val LISTED = 3
        private val MANAGED_FIELD_HOLDER = ManagedFieldHolder(StructureTierTrait::class.java)
    }
}