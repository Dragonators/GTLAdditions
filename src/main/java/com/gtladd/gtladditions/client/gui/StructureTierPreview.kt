package com.gtladd.gtladditions.client.gui

import appeng.api.stacks.AEKey
import appeng.api.stacks.GenericStack
import com.gregtechceu.gtceu.api.block.IMachineBlock
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo
import com.gregtechceu.gtceu.client.renderer.MultiblockInWorldPreviewRenderer
import com.gtladd.gtladditions.api.machine.feature.IStructureTierMachine
import com.gtladd.gtladditions.api.pattern.StructureTierLayout
import com.gtladd.gtladditions.mixin.gtceu.client.MultiblockInWorldPreviewRendererAccessor
import com.gtladd.gtladditions.mixin.gtlcore.client.SparsePreviewShapeAccessor
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.lowdragmc.lowdraglib.utils.BlockInfo
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import it.unimi.dsi.fastutil.longs.LongSets
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.client.preview.PreviewHeight
import org.gtlcore.gtlcore.client.preview.SparsePreviewShape
import org.gtlcore.gtlcore.client.preview.WorldPreview
import java.util.WeakHashMap

@OnlyIn(Dist.CLIENT)
object StructureTierPreview {

    private val kept = HashMap<Pair<StructureTierLayout, IntRange>, LongSet>()
    private val sparse = WeakHashMap<SparsePreviewShape, HashMap<IntRange, SparsePreviewShape>>()
    private val full = WeakHashMap<MultiblockShapeInfo, HashMap<IntRange, MultiblockShapeInfo>>()
    private val hidden = WeakHashMap<IMultiController, Array<LongSet?>>()
    private val heights = WeakHashMap<IMultiController, Array<PreviewHeight?>>()

    private var shownAt: BlockPos? = null
    private var shownTicks = 0

    private var deferred: Pair<IStructureTierMachine, IntRange>? = null
    private var deferredAt = 0L

    // ========================================
    // In-world preview
    // ========================================

    fun shown(machine: IStructureTierMachine): IntRange {
        val tiers = machine.structureTierTrait
        return if (tiers.structureTier >= tiers.layout.minTier) {
            tiers.structureTier + 1..tiers.unlockedTier
        } else {
            0..tiers.unlockedTier
        }
    }

    fun clear() {
        shownAt = null
        shownTicks = 0
        deferred = null
        deferredAt = 0L
    }

    fun opened(pos: BlockPos?, ticks: Int) {
        shownAt = pos?.immutable()
        shownTicks = ticks
    }

    /** GT's entry hands large structures to GTLCore's preview. */
    fun refresh(machine: IStructureTierMachine) {
        val controller = machine as MultiblockControllerMachine
        if (controller.pos != shownAt) return
        if (!WorldPreview.active() && MultiblockInWorldPreviewRendererAccessor.getLastPos() != controller.pos) return

        MultiblockInWorldPreviewRenderer.cleanPreview()
        MultiblockInWorldPreviewRenderer.showPreview(controller.pos, controller, shownTicks)
    }

    fun filter(
        shape: SparsePreviewShape,
        definition: MultiblockMachineDefinition,
        machine: IStructureTierMachine,
        range: IntRange
    ): SparsePreviewShape {
        nothingToBuild(range)
        return sparse.getOrPut(shape) { HashMap() }.getOrPut(range) {
            val offsets = kept(machine.structureTierTrait.layout, range)
            val positions = LongArrayList(offsets.size)
            val blocks = ObjectArrayList<BlockInfo>(offsets.size)

            for (i in shape.positions.indices) {
                val p = shape.positions[i]
                if (!offsets.contains(offset(BlockPos.getX(p), BlockPos.getY(p), BlockPos.getZ(p), shape.controller))) continue
                positions.add(p)
                blocks.add(shape.blocks[i])
            }

            val copy = SparsePreviewShape(MultiblockShapeInfo(arrayOf<Array<Array<BlockInfo>>>()), definition)
            (copy as SparsePreviewShapeAccessor).apply {
                setPositions(positions.toLongArray())
                setBlocks(blocks.toTypedArray())
                setController(shape.controller)
                setHeight(shape.height)
            }

            copy
        }
    }

    /** GTLCore declined [machine]'s structure as small: GT's own path reads the definition's shapes next, in the same tick. */
    fun deferCut(machine: IStructureTierMachine?, range: IntRange?) {
        deferred = if (machine == null || range == null) null else machine to range
        deferredAt = Minecraft.getInstance().level?.gameTime ?: 0L
    }

    fun takeCut(definition: MultiblockMachineDefinition): List<MultiblockShapeInfo>? {
        val (machine, range) = deferred ?: return null
        val mc = Minecraft.getInstance()
        if ((machine as MultiblockControllerMachine).definition != definition || !mc.isSameThread) return null

        deferred = null
        if (mc.level?.gameTime != deferredAt) return null

        val shapes = definition.matchingShapes
        return listOf(filter(shapes[0], machine, range)) + shapes.drop(1)
    }

    fun filter(shape: MultiblockShapeInfo, machine: IStructureTierMachine, range: IntRange): MultiblockShapeInfo {
        nothingToBuild(range)
        return full.getOrPut(shape) { HashMap() }.getOrPut(range) {
            val definition = (machine as MultiblockControllerMachine).definition
            val source = shape.blocks
            val controller = source.indices.firstNotNullOf { x ->
                source[x].indices.firstNotNullOfOrNull { y ->
                    source[x][y].indices
                        .firstOrNull { z -> (source[x][y][z]?.blockState?.block as? IMachineBlock)?.definition == definition }
                        ?.let { z -> BlockPos(x, y, z) }
                }
            }

            val offsets = kept(machine.structureTierTrait.layout, range)
            MultiblockShapeInfo(
                Array(source.size) { x ->
                    Array(source[x].size) { y ->
                        Array(source[x][y].size) { z ->
                            val shown = offsets.contains(offset(x, y, z, controller)) ||
                                x == controller.x && y == controller.y && z == controller.z
                            if (shown) source[x][y][z] else null
                        }
                    }
                }
            )
        }
    }

    private fun nothingToBuild(range: IntRange) {
        if (range.isEmpty()) {
            Minecraft.getInstance().gui.setOverlayMessage("gtladditions.structure_tier.nothing_to_build".toComponent, false)
        }
    }

    private fun kept(layout: StructureTierLayout, range: IntRange): LongSet = kept.getOrPut(layout to range) {
        LongOpenHashSet().also { set ->
            for (tier in range) {
                for (cell in layout.cells(tier)) {
                    set.add(layout.offset(cell, Direction.NORTH, Direction.NORTH, false).asLong())
                }
            }
        }
    }

    private fun offset(x: Int, y: Int, z: Int, controller: BlockPos): Long =
        BlockPos.asLong(x - controller.x, y - controller.y, z - controller.z)

    // ========================================
    // Recipe viewer page
    // ========================================

    fun hidden(controller: IMultiController, tier: Int): LongSet {
        val layout = (controller as IStructureTierMachine).structureTierTrait.layout
        if (tier >= layout.maxTier) return LongSets.EMPTY_SET

        return hidden.getOrPut(controller) { arrayOfNulls(layout.maxTier + 1) }.let { sets ->
            sets[tier] ?: LongOpenHashSet().also { set ->
                for (pos in layout.positions(tier + 1..layout.maxTier, controller as MultiblockControllerMachine)) {
                    set.add(pos.asLong())
                }
                sets[tier] = set
            }
        }
    }

    fun materials(list: List<List<GenericStack>>, materials: Map<BlockPos, AEKey>, hidden: LongSet): List<List<GenericStack>> {
        if (hidden.isEmpty()) return list

        val minus = Object2LongOpenHashMap<AEKey>()
        for ((pos, key) in materials) {
            if (pos.asLong() in hidden) minus.addTo(key, 1L)
        }

        return list.mapNotNull { options ->
            val left = options[0].amount - minus.getLong(options[0].what)
            if (left <= 0) null else options.map { GenericStack(it.what, left) }
        }
    }

    fun bounds(
        controller: IMultiController,
        tier: Int,
        blocks: Map<BlockPos, BlockInfo?>,
        moduleOnly: Set<BlockPos>,
        hidden: LongSet
    ): PreviewHeight {
        val layout = (controller as IStructureTierMachine).structureTierTrait.layout

        return heights.getOrPut(controller) { arrayOfNulls(layout.maxTier + 1) }.let { cached ->
            cached[tier] ?: PreviewHeight().also { height ->
                for ((pos, info) in blocks) {
                    if (info == null || info.blockState.isAir || pos.asLong() in hidden) continue
                    height.include(pos.x, pos.y, pos.z, pos in moduleOnly)
                }
                cached[tier] = height
            }
        }
    }
}