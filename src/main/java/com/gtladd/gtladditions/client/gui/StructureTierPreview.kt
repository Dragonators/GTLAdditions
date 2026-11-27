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
import com.gtladd.gtladditions.mixin.gtlcore.client.PatternPreviewPatternAccessor
import com.gtladd.gtladditions.mixin.gtlcore.client.SparsePreviewShapeAccessor
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.lowdragmc.lowdraglib.utils.BlockInfo
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import it.unimi.dsi.fastutil.longs.LongSets
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.client.preview.PreviewHeight
import org.gtlcore.gtlcore.client.preview.SparsePreviewShape
import org.gtlcore.gtlcore.client.preview.WorldPreview
import org.gtlcore.gtlcore.integration.ae2.pattern.MultiblockPatternEncoding
import java.util.WeakHashMap
import java.util.function.Predicate

@OnlyIn(Dist.CLIENT)
object StructureTierPreview {

    private val kept = HashMap<Pair<StructureTierLayout, IntRange>, LongSet>()
    private val sparse = WeakHashMap<SparsePreviewShape, HashMap<IntRange, SparsePreviewShape>>()
    private val full = WeakHashMap<MultiblockShapeInfo, HashMap<IntRange, MultiblockShapeInfo>>()
    private val tiers = WeakHashMap<IMultiController, TierCache>()

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
        val size = range.sumOf { layout.cells(it).size }
        if (size == 0) return@getOrPut LongSets.EMPTY_SET
        LongOpenHashSet(size).also { set ->
            for (tier in range) {
                layout.forEachPosition(tier, 0, 0, 0, Direction.NORTH, Direction.NORTH, false) { x, y, z ->
                    set.add(BlockPos.asLong(x, y, z))
                }
            }
        }
    }

    private fun offset(x: Int, y: Int, z: Int, controller: BlockPos): Long =
        BlockPos.asLong(x - controller.x, y - controller.y, z - controller.z)

    // ========================================
    // Recipe viewer page
    // ========================================

    fun shown(layout: StructureTierLayout, tier: Int, upgrade: Boolean): IntRange =
        if (upgrade && tier > layout.minTier) tier..tier else 0..tier

    fun filterShown(
        pattern: PatternPreviewPatternAccessor,
        shown: IntRange,
        predicate: Predicate<BlockPos>
    ): Predicate<BlockPos> {
        val cache = cache(pattern, shown) ?: return predicate
        if (cache.allInTiers && shown.first == shown.last) {
            val kept = cache.tierPositions[shown.first]
            return Predicate { predicate.test(it) && kept.contains(it.asLong()) }
        }
        val last = cache.tierPositions.lastIndex
        if (shown.first == 0 && shown.last == last - 1) {
            val hidden = cache.tierPositions[last]
            return Predicate { predicate.test(it) && !hidden.contains(it.asLong()) }
        }
        return Predicate { predicate.test(it) && !cache.isHidden(it.asLong(), shown) }
    }

    /** GTLCore already counts a replaced position as its replacement, so a hidden one gives that back. */
    fun materials(
        pattern: PatternPreviewPatternAccessor,
        shown: IntRange,
        list: List<List<GenericStack>>,
        replacements: Map<BlockPos, AEKey>
    ): List<List<GenericStack>> {
        val cache = cache(pattern, shown) ?: return list
        val minus = cache.hiddenMaterials(shown, pattern.materials, replacements)

        return list.mapNotNull { options ->
            val deduct = minus.getLong(options[0].what)
            when {
                deduct == 0L -> options
                options[0].amount <= deduct -> null
                else -> {
                    val left = options[0].amount - deduct
                    options.map { GenericStack(it.what, left) }
                }
            }
        }
    }

    fun dimensions(pattern: PatternPreviewPatternAccessor, shown: IntRange, modules: Boolean): PreviewHeight.Dimensions? =
        cache(pattern, shown)?.bounds(shown)?.dimensions(modules)

    /** The controller belongs to the base that upgrade views hide, so its height is read from the whole structure up to the tier. */
    fun controllerHeight(pattern: PatternPreviewPatternAccessor, shown: IntRange, modules: Boolean): Int? {
        val range = if (shown.first == 0) shown else 0..shown.last
        return cache(pattern, range)?.bounds(range)?.aboveBottom(pattern.controllerBase.self().pos.y, modules)
    }

    private fun cache(pattern: PatternPreviewPatternAccessor, shown: IntRange): TierCache? {
        val controller = pattern.controllerBase
        val layout = (controller as IStructureTierMachine).structureTierTrait.layout
        if (whole(layout, shown)) return null
        return tiers.getOrPut(controller) { TierCache.build(pattern, layout) }
    }

    fun outputs(definition: MultiblockMachineDefinition, layout: StructureTierLayout, shown: IntRange): List<GenericStack> {
        if (whole(layout, shown)) return MultiblockPatternEncoding.outputs(definition)

        val machine = Component.translatable(definition.descriptionId)
        val name = if (shown.first == 0) {
            "gtladditions.structure_tier.xei.structure".toComponent(machine, shown.last)
        } else {
            "gtladditions.structure_tier.xei.structure.upgrade".toComponent(machine, shown.first - 1, shown.last)
        }
        val book = ItemStack(Items.WRITTEN_BOOK).apply {
            hoverName = name.withStyle { it.withItalic(false).withColor(ChatFormatting.LIGHT_PURPLE) }
            hideTooltipPart(ItemStack.TooltipPart.ADDITIONAL)
        }
        return listOfNotNull(GenericStack.fromItemStack(book))
    }

    fun upgradeLabel(layout: StructureTierLayout, shown: IntRange): String = when {
        shown.last == layout.minTier -> "${ChatFormatting.GRAY}Σ"
        shown.first > 0 -> "Δ"
        else -> "Σ"
    }

    fun upgradeTooltips(layout: StructureTierLayout, shown: IntRange): List<Component> = when {
        shown.last == layout.minTier -> listOf("gtladditions.structure_tier.preview.initial".toComponent(shown.last))
        else -> listOf(
            if (shown.first > 0) {
                "gtladditions.structure_tier.preview.upgrade".toComponent(shown.first - 1, shown.last)
            } else {
                "gtladditions.structure_tier.preview.whole".toComponent(shown.last)
            },
            "gtladditions.structure_tier.preview.upgrade.hint".toComponent.withStyle(ChatFormatting.GRAY)
        )
    }

    private fun whole(layout: StructureTierLayout, shown: IntRange): Boolean = shown.first == 0 && shown.last == layout.maxTier

    private class Box(
        var minX: Int,
        var minY: Int,
        var minZ: Int,
        var maxX: Int,
        var maxY: Int,
        var maxZ: Int
    ) {
        constructor(x: Int, y: Int, z: Int) : this(x, y, z, x, y, z)

        fun include(x: Int, y: Int, z: Int) {
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (z < minZ) minZ = z
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
            if (z > maxZ) maxZ = z
        }

        fun includeInto(height: PreviewHeight, moduleOnly: Boolean) {
            height.include(minX, minY, minZ, moduleOnly)
            height.include(maxX, maxY, maxZ, moduleOnly)
        }
    }

    private class TierCache(
        val tierPositions: Array<LongSet>,
        private val tierMaterials: Array<Object2LongOpenHashMap<AEKey>>,
        private val tierHostBounds: Array<Box?>,
        private val tierModuleBounds: Array<Box?>,
        private val extraHost: Box?,
        private val extraModule: Box?,
        val allInTiers: Boolean
    ) {
        private val heights = HashMap<IntRange, PreviewHeight>()
        private val baseHiddenMaterials = HashMap<IntRange, Object2LongOpenHashMap<AEKey>>()

        fun isHidden(posLong: Long, shown: IntRange): Boolean {
            for (tier in 0 until shown.first) {
                if (tierPositions[tier].contains(posLong)) return true
            }
            for (tier in shown.last + 1 until tierPositions.size) {
                if (tierPositions[tier].contains(posLong)) return true
            }
            return false
        }

        fun hiddenMaterials(
            shown: IntRange,
            materials: Map<BlockPos, AEKey>,
            replacements: Map<BlockPos, AEKey>
        ): Object2LongOpenHashMap<AEKey> {
            val base = baseHiddenMaterials.getOrPut(shown) {
                if (shown.first == 0 && shown.last == tierMaterials.lastIndex - 1) {
                    tierMaterials[tierMaterials.lastIndex]
                } else {
                    Object2LongOpenHashMap<AEKey>().also { sum ->
                        for (tier in 0 until shown.first) addAll(sum, tierMaterials[tier])
                        for (tier in shown.last + 1 until tierMaterials.size) addAll(sum, tierMaterials[tier])
                    }
                }
            }
            if (replacements.isEmpty()) return base

            var adjusted: Object2LongOpenHashMap<AEKey>? = null
            for ((pos, replacement) in replacements) {
                if (!isHidden(pos.asLong(), shown)) continue
                val original = materials[pos] ?: continue
                if (original == replacement) continue
                val map = adjusted ?: base.clone().also { adjusted = it }
                map.addTo(original, -1L)
                map.addTo(replacement, 1L)
            }
            return adjusted ?: base
        }

        fun bounds(shown: IntRange): PreviewHeight = heights.getOrPut(shown) {
            PreviewHeight().also { height ->
                for (tier in shown) {
                    tierHostBounds[tier]?.includeInto(height, false)
                    tierModuleBounds[tier]?.includeInto(height, true)
                }
                extraHost?.includeInto(height, false)
                extraModule?.includeInto(height, true)
            }
        }

        companion object {
            fun build(pattern: PatternPreviewPatternAccessor, layout: StructureTierLayout): TierCache {
                val controller = pattern.controllerBase.self()
                val origin = controller.pos
                val facing = controller.frontFacing
                val upwards = controller.upwardsFacing
                val flipped = controller.isFlipped
                val tierCount = layout.maxTier + 1

                val tierPositions = Array<LongSet>(tierCount) { tier ->
                    val size = layout.cells(tier).size
                    if (size == 0) LongSets.EMPTY_SET else LongOpenHashSet(size)
                }
                val tierMaterials = Array(tierCount) { Object2LongOpenHashMap<AEKey>() }
                val tierHostBounds = arrayOfNulls<Box>(tierCount)
                val tierModuleBounds = arrayOfNulls<Box>(tierCount)
                val materials = pattern.materials
                val blockMap = pattern.blockMap
                val moduleOnly = pattern.moduleOnlyBlocks
                val hasModules = moduleOnly.isNotEmpty()
                val cursor = BlockPos.MutableBlockPos()
                var countedBlocks = 0
                var countedMaterials = 0

                for (tier in 0 until tierCount) {
                    val positions = tierPositions[tier] as? LongOpenHashSet ?: continue
                    val matCounts = tierMaterials[tier]
                    var hostBox: Box? = null
                    var modBox: Box? = null

                    layout.forEachPosition(tier, origin.x, origin.y, origin.z, facing, upwards, flipped) { x, y, z ->
                        positions.add(BlockPos.asLong(x, y, z))
                        cursor.set(x, y, z)

                        val key = materials[cursor]
                        if (key != null) {
                            matCounts.addTo(key, 1L)
                            countedMaterials++
                        }

                        val info = blockMap[cursor]
                        if (info != null) {
                            countedBlocks++
                            if (!info.blockState.isAir) {
                                if (hasModules && cursor in moduleOnly) {
                                    modBox?.include(x, y, z) ?: run { modBox = Box(x, y, z) }
                                } else {
                                    hostBox?.include(x, y, z) ?: run { hostBox = Box(x, y, z) }
                                }
                            }
                        }
                    }

                    tierHostBounds[tier] = hostBox
                    tierModuleBounds[tier] = modBox
                }

                var extraHost: Box? = null
                var extraModule: Box? = null
                if (countedBlocks < blockMap.size) {
                    for ((pos, info) in blockMap) {
                        if (info == null || info.blockState.isAir) continue
                        val packed = pos.asLong()
                        if (containsAny(tierPositions, packed)) continue
                        if (hasModules && pos in moduleOnly) {
                            extraModule?.include(pos.x, pos.y, pos.z) ?: run { extraModule = Box(pos.x, pos.y, pos.z) }
                        } else {
                            extraHost?.include(pos.x, pos.y, pos.z) ?: run { extraHost = Box(pos.x, pos.y, pos.z) }
                        }
                    }
                }

                return TierCache(
                    tierPositions = tierPositions,
                    tierMaterials = tierMaterials,
                    tierHostBounds = tierHostBounds,
                    tierModuleBounds = tierModuleBounds,
                    extraHost = extraHost,
                    extraModule = extraModule,
                    allInTiers = countedBlocks == blockMap.size && countedMaterials == materials.size
                )
            }

            private fun containsAny(sets: Array<LongSet>, posLong: Long): Boolean {
                for (set in sets) {
                    if (set.contains(posLong)) return true
                }
                return false
            }

            private fun addAll(target: Object2LongOpenHashMap<AEKey>, source: Object2LongOpenHashMap<AEKey>) {
                val iterator = source.object2LongEntrySet().fastIterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    target.addTo(entry.key, entry.longValue)
                }
            }
        }
    }
}
