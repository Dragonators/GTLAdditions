package com.gtladd.gtladditions.common.machine.multiblock.part

import appeng.api.crafting.IPatternDetails
import appeng.api.networking.IGridNode
import appeng.api.networking.ticking.IGridTickable
import appeng.api.networking.ticking.TickRateModulation
import appeng.api.stacks.AEFluidKey
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.AEKey
import appeng.api.stacks.KeyCounter
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient
import com.gregtechceu.gtceu.common.data.GTItems
import com.gregtechceu.gtceu.integration.ae2.gui.widget.AETextInputButtonWidget
import com.gregtechceu.gtceu.utils.ResearchManager
import com.gtladd.gtladditions.api.machine.IWidePatternBuffer
import com.gtladd.gtladditions.api.machine.IWidePatternSlot
import com.gtladd.gtladditions.api.machine.gui.FOAPatternConfigurator
import com.gtladd.gtladditions.integration.ae2.*
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.wideBuffer.Int128Accumulator
import com.gtladd.gtladditions.utils.wideBuffer.WideBufferTools
import com.gtladd.gtladditions.utils.wideBuffer.compareTo
import com.gtladd.gtladditions.utils.wideBuffer.getBestFluidMatch
import com.gtladd.gtladditions.utils.wideBuffer.getBestFluidMatchSimulate
import com.gtladd.gtladditions.utils.wideBuffer.getBestItemMatch
import com.gtladd.gtladditions.utils.wideBuffer.getBestItemMatchSimulate
import com.gtladd.gtladditions.utils.wideBuffer.longValue
import com.gtladd.gtladditions.utils.wideBuffer.saturatedAdd
import com.lowdragmc.lowdraglib.gui.util.ClickData
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.objects.*
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import org.gtlcore.gtlcore.api.gui.MEPatternCatalystUIManager
import org.gtlcore.gtlcore.common.data.GTLItems
import org.gtlcore.gtlcore.common.machine.multiblock.part.PaginationUIManager
import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEPatternBufferPartMachine
import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEPatternBufferRecipeHandlerTrait
import org.gtlcore.gtlcore.integration.ae2.AEUtils
import org.gtlcore.gtlcore.utils.datastructure.Int128
import kotlin.math.max

class MESuperPatternBufferPartMachine @JvmOverloads constructor(
    holder: IMachineBlockEntity?,
    patternsPerRow: Int = 9,
    rowsPerPage: Int = 6,
    maxPages: Int = 3
) : MEPatternBufferPartMachine(holder, patternsPerRow * rowsPerPage * maxPages, IO.BOTH),
    IWidePatternBuffer {

    private val paginationUIManager: PaginationUIManager
    private val pending = Object2ObjectOpenHashMap<AEKey, Int128>()

    @field:Persisted
    private var foaModeEnabled: Boolean = false

    @field:Persisted
    private var foaPatternOutputMultiplier: Int = DEFAULT_MULTIPLIER

    init {
        // The node is not live yet; AE2 replaces Core's ticker service with this buffer's implementation.
        mainNode.addService(IGridTickable::class.java, SuperPatternBufferTicker())

        val uiWidth = max(patternsPerRow * 18 + 16, 106)
        val uiHeight = rowsPerPage * 18 + 28
        paginationUIManager = PaginationUIManager(
            patternsPerRow,
            rowsPerPage,
            maxPages,
            uiWidth,
            uiHeight,
            { index: Int -> this.onPatternChange(index) },
            { i: Int? -> cacheRecipe[i!!] },
            patternInventory
        )
    }

    override fun createInternalSlot(slotIndex: Int): InternalSlot = SuperPatternBufferInternalSlot(slotIndex)

    override fun createRecipeHandler(io: IO): MEPatternBufferRecipeHandlerTrait = SuperPatternBufferRecipeHandler(this, io)

    // ========================================
    // PATTERN
    // ========================================

    // AT exposes Core's decoding hook; inherited load and refresh paths dispatch here too.
    override fun getRealPattern(slot: Int, stack: ItemStack): IPatternDetails? {
        val pattern = super.getRealPattern(slot, stack)
        return if (foaModeEnabled) {
            rewriteForgeOfTheAntichristPattern(pattern, level, foaPatternOutputMultiplier)
        } else {
            pattern
        }
    }

    override fun onPatternChange(index: Int) {
        if (isRemote) return

        val slot = internalInventory[index]
        val newPattern = getRealPattern(index, patternInventory.getStackInSlot(index))
        val oldPattern = slot2PatternMap.get(index)

        if (newPattern != null) {
            slot2PatternMap.put(index, newPattern)
            hasPatternArray[index] = true
        } else {
            slot2PatternMap.remove(index)
            hasPatternArray[index] = false
        }

        if (oldPattern != null && oldPattern != newPattern) {
            slot.cacheManager.clearAllCaches()
            slot.clearVirtualSupply()
            removeSlotFromGTRecipeCache(index)
            WideBufferTools.refundSlot(pending, wideSlot(index).wideInputs())
            WideBufferTools.flushToNetwork(pending, mainNode.grid, actionSource)
        }

        reCalculatePatternSlotMap()
        needPatternSync = true
    }

    override fun pushPattern(patternDetails: IPatternDetails, inputHolder: Array<KeyCounter>): Boolean {
        if (!mainNode.isActive) return false

        val index = getSlotIndexForPattern(patternDetails) ?: return false
        if (index !in internalInventory.indices || !wideSlot(index).canPushWide(inputHolder)) return false

        internalInventory[index].pushPattern(inputHolder)
        return true
    }

    override fun `gtlcore$graphCapacity`(
        pattern: IPatternDetails,
        inputPerRun: Map<AEKey, Long>,
        requested: Long
    ): Long {
        if (!mainNode.isActive || requested < 0L) return 0L

        val slot = getSlotIndexForPattern(pattern) ?: return 0L
        if (slot >= internalSlotCount) return 0L

        // Int128.MAX is practically unreachable in gameplay, so storage capacity does not limit valid dispatch.
        return requested
    }

    // ========================================
    // BUFFER ACCESS AND REFUNDS
    // ========================================

    override fun wideSlot(index: Int): IWidePatternSlot = internalInventory[index] as SuperPatternBufferInternalSlot

    override fun wideSlotCount(): Int = internalInventory.size

    override fun widePending() = pending

    override fun refundAll(clickData: ClickData) {
        if (clickData.isRemote) return

        for (index in internalInventory.indices) {
            WideBufferTools.refundSlot(pending, wideSlot(index).wideInputs())
            internalInventory[index].clearVirtualSupply()
        }

        WideBufferTools.flushToNetwork(pending, mainNode.grid, actionSource)
    }

    // ========================================
    // PERSISTENCE
    // ========================================

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    override fun saveCustomPersistedData(tag: CompoundTag, forDrop: Boolean) {
        super.saveCustomPersistedData(tag, forDrop)
        tag.put("buffer", WideBufferTools.writeAmounts(pending, AEKey::toTagGeneric))
    }

    override fun loadCustomPersistedData(tag: CompoundTag) {
        super.loadCustomPersistedData(tag)

        pending.clear()
        val entries = tag.getList("buffer", Tag.TAG_COMPOUND.toInt())
        val loaded = WideBufferTools.readAmounts(entries, AEKey::fromTagGeneric, buffer::clear)
        pending.putAll(loaded)
    }

    // ========================================
    // FOA CONFIGURATION
    // ========================================

    fun isFOAModeEnabled(): Boolean = foaModeEnabled

    fun getFOAPatternOutputMultiplier(): Int = foaPatternOutputMultiplier

    fun setFOAModeEnabled(enabled: Boolean) {
        if (foaModeEnabled == enabled) return
        foaModeEnabled = enabled
        refreshAllByProduct()
    }

    fun setFOAPatternOutputMultiplier(multiplier: Int) {
        if (foaPatternOutputMultiplier == multiplier) return
        foaPatternOutputMultiplier = multiplier
        if (foaModeEnabled) refreshAllByProduct()
    }

    // ========================================
    // DATASTICK INTERACTION
    // ========================================

    override fun onUse(
        state: BlockState?,
        world: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult?
    ): InteractionResult {
        val stack = player.getItemInHand(hand)
        if (stack.isEmpty) return InteractionResult.PASS

        if (stack.`is`(GTItems.TOOL_DATA_STICK.asItem())) {
            if (!world.isClientSide) {
                // Check if it's research data - if so, pass to avoid conflicts
                val researchData = ResearchManager.readResearchId(stack)
                if (researchData != null) {
                    return InteractionResult.PASS
                }

                if (stack.getOrCreateTag().contains("pos", Tag.TAG_INT_ARRAY.toInt())) {
                    stack.getOrCreateTag().remove("pos")
                }

                // Store this pattern buffer's position in the data stick
                stack.getOrCreateTag().putIntArray("superPos", intArrayOf(pos.x, pos.y, pos.z))
                player.sendSystemMessage("gtceu.machine.me.import_copy_settings".toComponent)
            }
            return InteractionResult.sidedSuccess(world.isClientSide)
        }

        return InteractionResult.PASS
    }

    // ========================================
    // GUI SYSTEM
    // ========================================

    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        super.attachConfigurators(configuratorPanel)
        configuratorPanel.attachConfigurators(FOAPatternConfigurator(this))
    }

    override fun createUIWidget(): Widget {
        val group = WidgetGroup(0, 0, paginationUIManager.uiWidth, paginationUIManager.uiHeight)

        // ME Network status indicator
        group.addWidget(
            LabelWidget(
                8,
                2
            ) { if (this.isOnline) "gtceu.gui.me_network.online" else "gtceu.gui.me_network.offline" }
        )

        // Custom name input widget
        group.addWidget(
            AETextInputButtonWidget(paginationUIManager.uiWidth - 78, 2, 70, 10)
                .setText(customName)
                .setOnConfirm { customName: String? -> this.setCustomName(customName) }
                .setButtonTooltips("gui.gtceu.rename.desc".toComponent)
        )

        val catalystUIManager = MEPatternCatalystUIManager(
            group.sizeWidth + 4,
            catalystItems,
            catalystFluids,
            cacheRecipeCount
        ) { slot: Int -> this.removeSlotFromGTRecipeCache(slot) }
        group.waitToAdded(catalystUIManager)

        // Create pagination UI using the manager
        group.addWidget(
            paginationUIManager.createPaginationUI { index: Int ->
                catalystUIManager.toggleFor(
                    index
                )
            }
        )

        return group
    }

    // ========================================
    // RECIPE OUTPUT HANDLERS
    // ========================================

    private class SuperPatternBufferRecipeHandler(
        machine: MESuperPatternBufferPartMachine,
        io: IO
    ) : MEPatternBufferRecipeHandlerTrait(machine, io) {
        override fun createMEItemHandler(io: IO): MEItemInputHandlerBase =
            object : MEItemInputHandlerBase(machine as MESuperPatternBufferPartMachine, io) {
                override fun getMachine(): MESuperPatternBufferPartMachine = super.getMachine() as MESuperPatternBufferPartMachine

                override fun meHandleRecipeOutputInner(
                    left: List<Ingredient>,
                    simulate: Boolean
                ): List<Ingredient> = WideBufferTools.itemOutputs(getMachine().widePending(), left, simulate)
            }

        override fun createMEFluidHandler(io: IO): MEFluidHandlerBase =
            object : MEFluidHandlerBase(machine as MESuperPatternBufferPartMachine, io) {
                override fun getMachine(): MESuperPatternBufferPartMachine = super.getMachine() as MESuperPatternBufferPartMachine

                override fun meHandleRecipeOutputInner(
                    left: List<FluidIngredient>,
                    simulate: Boolean
                ): List<FluidIngredient> = WideBufferTools.fluidOutputs(getMachine().widePending(), left, simulate)
            }
    }

    // ========================================
    // NETWORK OUTPUT TICKER
    // ========================================

    private inner class SuperPatternBufferTicker : Ticker() {
        override fun tickingRequest(node: IGridNode, ticksSinceLastCall: Int): TickRateModulation {
            if (!mainNode.isActive) {
                isSleeping = true
                return TickRateModulation.SLEEP
            }

            if (pending.isEmpty()) {
                return if (ticksSinceLastCall >= getTickingRequest(node).maxTickRate()) {
                    isSleeping = true
                    TickRateModulation.SLEEP
                } else {
                    TickRateModulation.SLOWER
                }
            }

            return if (WideBufferTools.flushToNetwork(pending, mainNode.grid, actionSource)) {
                TickRateModulation.URGENT
            } else {
                TickRateModulation.SLOWER
            }
        }
    }

    // ========================================
    // PATTERN INPUT SLOT
    // ========================================

    private inner class SuperPatternBufferInternalSlot(slotIndex: Int) :
        PatternBufferInternalSlot(slotIndex),
        IWidePatternSlot {
        private val inputs = Object2ObjectOpenHashMap<AEKey, Int128>()

        override fun wideInputs() = inputs

        override fun catalystItems(): Object2LongMap<AEItemKey> = itemCatalystInventory

        override fun catalystFluids(): Object2LongMap<AEFluidKey> = fluidCatalystInventory

        // ========================================
        // SLOT ACTIVITY
        // ========================================

        private fun hasInputs(kind: Class<out AEKey>): Boolean = inputs.keys.any { kind.isInstance(it) }

        override fun isActive(): Boolean = hasPatternInSlot(slotIndex) && !inputs.isEmpty()

        override fun isItemActive(simulate: Boolean): Boolean = hasPatternInSlot(slotIndex) &&
            (
                hasInputs(AEItemKey::class.java) ||
                    simulate &&
                    (
                        !sharedCatalystInventory.isEmpty ||
                            !getCircuitForRecipe(slotIndex).isEmpty ||
                            hasItemCatalystInventory() ||
                            hasVirtualItemSupply()
                        )
                )

        override fun isFluidActive(simulate: Boolean): Boolean = hasPatternInSlot(slotIndex) &&
            (hasInputs(AEFluidKey::class.java) || simulate && (!sharedCatalystTank.isEmpty || hasFluidCatalystInventory() || hasVirtualFluidSupply()))

        // ========================================
        // PATTERN INPUT
        // ========================================

        override fun canPushWide(inputs: Array<KeyCounter>): Boolean = inputs.all { holder ->
            holder.all { entry -> entry.longValue >= 0L && (entry.key is AEItemKey || entry.key is AEFluidKey) }
        }

        private fun storesQuantity(key: AEKey): Boolean = key is AEFluidKey ||
            key is AEItemKey &&
            !GTLItems.VIRTUAL_INGREDIENT.isIn(key.readOnlyStack)

        override fun add(what: AEKey, amount: Long) {
            if (amount <= 0L) return

            if (storesQuantity(what)) {
                inputs.getOrPut(what) { Int128.ZERO() }.saturatedAdd(amount)
            } else {
                // Let Core handle virtual items, fluids and circuit payloads.
                super.add(what, amount)
            }
        }

        override fun pushPattern(inputHolder: Array<KeyCounter>) {
            val totals = Object2ObjectOpenHashMap<AEKey, Int128>()
            AEUtils.pushInputsToMEPatternBufferInventory(inputHolder) { key, amount ->
                if (amount > 0L && storesQuantity(key)) {
                    Int128Accumulator.getOrCopy(totals, inputs, key).saturatedAdd(amount)
                } else {
                    add(key, amount)
                }
            }

            commitAmounts(totals)
            onContentsChanged.run()
        }

        private fun commitAmounts(amounts: MutableMap<AEKey, Int128>) = Int128Accumulator.commit(amounts, inputs)

        // ========================================
        // RECIPE INPUT CONSUMPTION
        // ========================================

        override fun handleItemInternal(left: Object2LongMap<Ingredient>, leftCircuit: Int, simulate: Boolean): Boolean {
            if (left.isEmpty() && leftCircuit < 0) return true
            if (simulate && leftCircuit > 0 && leftCircuit != cacheManager.circuitCache) return false

            val iterator = Object2LongMaps.fastIterator(left)
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.longValue <= 0L) {
                    iterator.remove()
                    continue
                }

                val key = if (simulate) {
                    cacheManager.getBestItemMatchSimulate(entry.key, inputs, itemCatalystInventory, entry.longValue)
                } else {
                    cacheManager.getBestItemMatch(entry.key, inputs, entry.longValue)
                }
                if (key == null) return false
            }

            if (!simulate) {
                val consumed = Object2LongMaps.fastIterator(left)
                while (consumed.hasNext()) {
                    val entry = consumed.next()
                    val key = cacheManager.getBestItemMatch(entry.key, inputs, entry.longValue) ?: continue
                    val remaining = inputs[key]!!

                    remaining.add(-entry.longValue)
                    if (remaining <= 0) inputs.remove(key)
                    consumed.remove()
                }
            }

            return true
        }

        override fun handleFluidInternal(left: Object2LongMap<FluidIngredient>, simulate: Boolean): Boolean {
            val iterator = Object2LongMaps.fastIterator(left)
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.longValue <= 0L) {
                    iterator.remove()
                    continue
                }

                val key = if (simulate) {
                    cacheManager.getBestFluidMatchSimulate(entry.key, inputs, fluidCatalystInventory, entry.longValue)
                } else {
                    cacheManager.getBestFluidMatch(entry.key, inputs, entry.longValue)
                }
                if (key == null) return false
            }

            if (!simulate) {
                val consumed = Object2LongMaps.fastIterator(left)
                while (consumed.hasNext()) {
                    val entry = consumed.next()
                    val key = cacheManager.getBestFluidMatch(entry.key, inputs, entry.longValue) ?: continue
                    val remaining = inputs[key]!!

                    remaining.add(-entry.longValue)
                    if (remaining <= 0) inputs.remove(key)
                    consumed.remove()
                }
            }

            return true
        }

        // ========================================
        // INVENTORY VIEWS
        // ========================================

        // Long-only Core APIs receive bounded result views, never the authoritative inventory.
        override fun getItemInventory(): Object2LongOpenHashMap<AEItemKey> = WideBufferTools.itemLongAmounts(inputs)

        override fun getFluidInventory(): Object2LongOpenHashMap<AEFluidKey> = WideBufferTools.fluidLongAmounts(inputs)

        override fun getItemStackInputMap(): Object2LongMap<ItemStack> = Object2LongOpenHashMap<ItemStack>().also { result ->
            inputs.forEach { (key, amount) ->
                if (key is AEItemKey && amount > 0) result.addTo(key.toStack(1), amount.longValue())
            }
        }

        override fun getFluidStackInputMap(): Object2LongMap<FluidStack> = Object2LongOpenHashMap<FluidStack>().also { result ->
            inputs.forEach { (key, amount) ->
                if (key is AEFluidKey && amount > 0) result.addTo(FluidStack.create(key.fluid, 1L), amount.longValue())
            }
        }

        override fun getLimitItemStackInput(): ObjectList<ItemStack> = ObjectArrayList<ItemStack>().also { result ->
            inputs.forEach { (key, amount) ->
                if (key is AEItemKey && amount > 0) result.add(key.toStack(minOf(amount.longValue(), Int.MAX_VALUE.toLong()).toInt()))
            }

            appendLimitItemCatalystInputs(result)
            appendVirtualItemInputs(result)
        }

        override fun getLimitFluidStackInput(): ObjectList<FluidStack> = ObjectArrayList<FluidStack>().also { result ->
            inputs.forEach { (key, amount) ->
                if (key is AEFluidKey && amount > 0) result.add(FluidStack.create(key.fluid, amount.longValue()))
            }

            appendLimitFluidCatalystInputs(result)
            appendVirtualFluidInputs(result)
        }

        // ========================================
        // SLOT PERSISTENCE
        // ========================================

        override fun serializeNBT(): CompoundTag = super.serializeNBT().apply {
            remove("inventory")
            remove("fluidInventory")
            put(SLOT_INPUT_KEY, WideBufferTools.writeAmounts(inputs, AEKey::toTagGeneric))
        }

        override fun deserializeNBT(tag: CompoundTag) {
            val loaded = if (tag.contains(SLOT_INPUT_KEY)) {
                WideBufferTools.readAmounts(tag.getList(SLOT_INPUT_KEY, Tag.TAG_COMPOUND.toInt()), AEKey::fromTagGeneric)
            } else {
                Object2ObjectOpenHashMap<AEKey, Int128>().apply {
                    putAll(WideBufferTools.readAmounts(tag.getList("inventory", Tag.TAG_COMPOUND.toInt()), AEItemKey::fromTag))
                    putAll(WideBufferTools.readAmounts(tag.getList("fluidInventory", Tag.TAG_COMPOUND.toInt()), AEFluidKey::fromTag))
                }
            }

            // Core still restores catalyst and virtual-supply state; keep its long quantity maps empty.
            super.deserializeNBT(
                tag.copy().apply {
                    remove("inventory")
                    remove("fluidInventory")
                }
            )

            inputs.clear()
            inputs.putAll(loaded)
        }
    }

    companion object {
        private const val DEFAULT_MULTIPLIER = 15
        const val MIN_MULTIPLIER = 1
        const val MAX_MULTIPLIER = 30
        const val SLOT_INPUT_KEY = "wideInput"

        private val MANAGED_FIELD_HOLDER = ManagedFieldHolder(
            MESuperPatternBufferPartMachine::class.java,
            MEPatternBufferPartMachine.MANAGED_FIELD_HOLDER
        )
    }
}