package com.gtladd.gtladditions.common.machine.multiblock.part

import appeng.api.networking.IGridNode
import appeng.api.networking.ticking.IGridTickable
import appeng.api.networking.ticking.TickRateModulation
import appeng.api.stacks.AEFluidKey
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.AEKey
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient
import com.gtladd.gtladditions.api.gui.widget.WideBufferOutputWidget
import com.gtladd.gtladditions.api.machine.gui.OutputTransferConfigurator
import com.gtladd.gtladditions.mixin.gtlcore.machine.part.MEOutputFilterHandlerAccessor
import com.gtladd.gtladditions.utils.wideBuffer.WideBufferTools
import com.gtladd.gtladditions.utils.wideBuffer.longValue
import com.gtladd.gtladditions.utils.wideBuffer.saturatedAdd
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient
import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEExtendedOutputPartMachine
import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.NotifiableMERecipeHandlerTrait
import org.gtlcore.gtlcore.utils.datastructure.Int128

class MESuperExtendedOutputPartMachine(holder: IMachineBlockEntity) : MEExtendedOutputPartMachine(holder) {
    private val pending = Object2ObjectOpenHashMap<AEKey, Int128>()

    @field:Persisted
    private var outputTransferLimit = WideBufferTools.DEFAULT_OUTPUT_TRANSFER_LIMIT

    /** The only authoritative output inventory; long-returning compatibility views are detached. */
    fun widePending(): Object2ObjectOpenHashMap<AEKey, Int128> = pending

    override fun getBuffer(): Object2LongOpenHashMap<AEKey> = Object2LongOpenHashMap<AEKey>().also { view ->
        pending.forEach { (key, amount) -> view.put(key, amount.longValue()) }
    }

    fun getOutputTransferLimit(): Int = WideBufferTools.clampOutputTransferLimit(outputTransferLimit)

    fun setOutputTransferLimit(value: Int) {
        outputTransferLimit = WideBufferTools.clampOutputTransferLimit(value)
    }

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    override fun saveCustomPersistedData(tag: CompoundTag, forDrop: Boolean) {
        super.saveCustomPersistedData(tag, forDrop)
        tag.put("buffer", WideBufferTools.writeAmounts(pending, AEKey::toTagGeneric))
    }

    override fun loadCustomPersistedData(tag: CompoundTag) {
        super.loadCustomPersistedData(tag)
        buffer.clear()
        pending.clear()
        pending.putAll(WideBufferTools.readAmounts(tag.getList("buffer", Tag.TAG_COMPOUND.toInt()), AEKey::fromTagGeneric))
    }

    override fun writeConfigToTag(): CompoundTag = super.writeConfigToTag().apply {
        putInt(WideBufferTools.OUTPUT_TRANSFER_LIMIT_TAG, getOutputTransferLimit())
    }

    override fun readConfigFromTag(tag: CompoundTag) {
        super.readConfigFromTag(tag)
        if (tag.contains(WideBufferTools.OUTPUT_TRANSFER_LIMIT_TAG)) {
            setOutputTransferLimit(WideBufferTools.readOutputTransferLimit(tag))
        }
    }

    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        super.attachConfigurators(configuratorPanel)
        configuratorPanel.attachConfigurators(OutputTransferConfigurator(::getOutputTransferLimit, ::setOutputTransferLimit))
    }

    override fun createUIWidget(): Widget = WidgetGroup(0, 0, 168, 146).apply {
        addWidget(LabelWidget(0, 0) { if (isOnline) "gtceu.gui.me_network.online" else "gtceu.gui.me_network.offline" })
        addWidget(WideBufferOutputWidget(5, 20, 7, pending))
    }

    override fun createItemOutputHandler(): NotifiableMERecipeHandlerTrait<Ingredient, ItemStack> =
        object : MEItemOutputHandler(this) {
            override fun getMachine(): MESuperExtendedOutputPartMachine = this@MESuperExtendedOutputPartMachine

            override fun outputHasFilter(): Boolean = filterHandler.isHasItemFilter

            override fun meHandleRecipeOutputInner(left: MutableList<Ingredient>, simulate: Boolean): List<Ingredient> {
                if (simulate) return filterHandler.testIngredient(left)

                val iterator = left.iterator()
                while (iterator.hasNext()) {
                    val ingredient = iterator.next()
                    if (ingredient is IntProviderIngredient) {
                        ingredient.setItemStacks(null)
                        ingredient.setSampledCount(null)
                    }
                    val stack = ingredient.items.firstOrNull()
                    if (stack == null || stack.isEmpty) {
                        iterator.remove()
                    } else if ((filterHandler as MEOutputFilterHandlerAccessor).invokeItemTest(stack)) {
                        val amount = if (ingredient is LongIngredient) ingredient.actualAmount else stack.count.toLong()
                        if (amount > 0L) {
                            pending.getOrPut(AEItemKey.of(stack)!!) { Int128.ZERO() }.saturatedAdd(amount)
                        }
                        iterator.remove()
                    }
                }
                return left
            }
        }

    override fun createFluidOutputHandler(): NotifiableMERecipeHandlerTrait<FluidIngredient, FluidStack> =
        object : MEFluidOutputHandler(this) {
            override fun getMachine(): MESuperExtendedOutputPartMachine = this@MESuperExtendedOutputPartMachine

            override fun outputHasFilter(): Boolean = filterHandler.isHasFluidFilter

            override fun meHandleRecipeOutputInner(left: MutableList<FluidIngredient>, simulate: Boolean): List<FluidIngredient> {
                if (simulate) return filterHandler.testFluidIngredient(left)

                val iterator = left.iterator()
                while (iterator.hasNext()) {
                    val ingredient = iterator.next()
                    val stack = if (ingredient.isEmpty) null else ingredient.getStacks().firstOrNull()
                    if (stack == null || stack.isEmpty) {
                        iterator.remove()
                    } else if ((filterHandler as MEOutputFilterHandlerAccessor).invokeFluidTest(stack)) {
                        if (stack.amount > 0L) {
                            pending.getOrPut(AEFluidKey.of(stack.fluid)) { Int128.ZERO() }.saturatedAdd(stack.amount)
                        }
                        iterator.remove()
                    }
                }
                return left
            }
        }

    override fun registerDefaultServices() {
        mainNode.addService(IGridTickable::class.java, SuperOutputTicker())
    }

    private inner class SuperOutputTicker : Ticker() {
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
            return if (WideBufferTools.flushToNetwork(pending, mainNode.grid, actionSource, getOutputTransferLimit())) {
                TickRateModulation.URGENT
            } else {
                TickRateModulation.SLOWER
            }
        }
    }

    companion object {
        private val MANAGED_FIELD_HOLDER = ManagedFieldHolder(MESuperExtendedOutputPartMachine::class.java, MEExtendedOutputPartMachine.MANAGED_FIELD_HOLDER)
    }
}