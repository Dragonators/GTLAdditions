package com.gtladd.gtladditions.common.machine.multiblock.controller

import appeng.api.config.Actionable
import appeng.api.stacks.AEItemKey
import appeng.api.storage.cells.CellState
import appeng.me.cells.BasicCellInventory
import com.gregtechceu.gtceu.api.GTValues.UHV
import com.gregtechceu.gtceu.api.GTValues.VA
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper.getBlock
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.block
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IMachineModifyDrops
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.common.data.GTMaterials
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gtladd.gtladditions.api.machine.gui.MultiblockDisplayText
import com.gtladd.gtladditions.common.items.GTLAddItems
import com.gtladd.gtladditions.common.machine.hatch.MEBlockConversationHatch
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer
import com.lowdragmc.lowdraglib.syncdata.ISubscription
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.Hash
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks.*
import org.gtlcore.gtlcore.api.machine.trait.IRecipeStatus
import org.gtlcore.gtlcore.api.machine.trait.MEStock.IMETransfer
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.api.recipe.RecipeResult.fail
import org.gtlcore.gtlcore.common.data.GTLItems.CONVERSION_SIMULATE_CARD
import org.gtlcore.gtlcore.common.data.GTLItems.FAST_CONVERSION_SIMULATE_CARD
import org.gtlcore.gtlcore.utils.Registries.getBlock
import kotlin.math.min
import kotlin.math.pow

class ConversationMachine(holder: IMachineBlockEntity) :
    CoilWorkableElectricMultiblockMachine(holder),
    IMachineModifyDrops {

    private var bcHatch: MEBlockConversationHatch? = null
    private var hatchSubscription: ISubscription? = null

    @Persisted
    private val machineStorage: NotifiableItemStackHandler = NotifiableItemStackHandler(this, 1, IO.NONE, IO.BOTH) {
        object : ItemStackTransfer(1) {
            override fun getSlotLimit(slot: Int): Int = 1
        }
    }.setFilter(::filter).also { it.addChangedListener(::refreshParallel) }

    @Persisted
    var cardId = 0

    @Persisted
    var parallel = 0L

    private fun filter(stack: ItemStack) = stack.`is`(CARD_1) || stack.`is`(CARD_2) || stack.`is`(CARD_3) || stack.`is`(CARD_4)

    private fun refreshParallel() {
        val base = (coilType.coilTemperature / 100).toDouble()
        when (machineStorage.getStackInSlot(0).item) {
            CARD_4 -> {
                cardId = 4
                parallel = base.pow(6.35).toLong()
            }
            CARD_3 -> {
                cardId = 3
                parallel = base.pow(4.2).toLong()
            }
            CARD_2 -> {
                cardId = 2
                parallel = base.pow(3.5).toLong()
            }
            CARD_1 -> {
                cardId = 1
                parallel = base.pow(2).toLong()
            }
            else -> {
                cardId = 0
                parallel = 0
            }
        }
    }

    private fun tickConsume(): Boolean {
        if (this.maxVoltage > EUT && EUT <= energyContainer.energyStored) {
            energyContainer.changeEnergy((-EUT))
            return true
        }
        return false
    }

    private fun beforeWork(): Boolean {
        if (cardId == 0 || parallel <= 0) {
            RecipeResult.ofWorking(this, fail("gtceu.machine.block_conversation.fail.2".toComponent))
            return false
        }

        val hatch = bcHatch
        val cell = hatch?.getCellInventory()

        if (cell == null) {
            RecipeResult.ofWorking(this, fail("gtceu.machine.block_conversation.fail.0".toComponent))
            return false
        }
        if (cell.status == CellState.FULL || cell.status == CellState.TYPES_FULL) {
            RecipeResult.ofWorking(this, fail("gtceu.machine.block_conversation.fail.1".toComponent))
            return false
        }

        val amount = min(parallel, (cell as? BasicCellInventory)?.remainingItemCount ?: Long.MAX_VALUE)
        var hasInput = false

        for (slot in hatch.aeItemHandler.inventory) {
            val stack = slot.getStackInSlot(0)
            if (stack.isEmpty) continue
            val output = blockMap[stack.item] ?: continue

            hasInput = true
            val available = (slot as IMETransfer).extractGenericStack(amount, true, false)?.amount ?: 0
            if (hatch.canInsertCell(AEItemKey.of(output), available)) {
                RecipeResult.ofWorking(this, null)
                return true
            }
            // These cards finish after the first matching input slot.
            if (cardId < 3) break
        }

        RecipeResult.ofWorking(
            this,
            if (hasInput) fail("gtceu.machine.block_conversation.fail.3".toComponent) else RecipeResult.FAIL_FIND
        )
        return false
    }

    private fun onConversionInputsChanged() {
        if (!isRemote && isFormed() && recipeLogic.isIdle) {
            recipeLogic.updateTickSubscription()
        }
    }

    private fun unsubscribeHatch() {
        hatchSubscription?.unsubscribe()
        hatchSubscription = null
    }

    override fun getRecipeLogic() = super.getRecipeLogic() as ConversationRecipeLogic

    override fun createRecipeLogic(vararg args: Any) = ConversationRecipeLogic(this)

    @Suppress("unused")
    fun supportsBatchProcessing(): Boolean = false

    @Suppress("unused")
    fun canConfigureBatchProcessing(): Boolean = false

    override fun onStructureFormed() {
        super.onStructureFormed()
        unsubscribeHatch()
        bcHatch = null
        refreshParallel()
        parts.forEach {
            if (it is MEBlockConversationHatch) {
                bcHatch = it
                hatchSubscription = it.addConversionChangedListener(::onConversionInputsChanged)
                return
            }
        }
    }

    override fun onStructureInvalid() {
        unsubscribeHatch()
        super.onStructureInvalid()
        bcHatch = null
    }

    override fun onDrops(drops: MutableList<ItemStack>) = this.clearInventory(this.machineStorage.storage)

    override fun afterWorking() {
        super.afterWorking()
        bcHatch?.let {
            val c = it.getCellInventory()
            if (c == null) {
                RecipeResult.ofWorking(this, fail("gtceu.machine.block_conversation.fail.0".toComponent))
                return
            }
            if (c.status == CellState.FULL || c.status == CellState.TYPES_FULL) {
                RecipeResult.ofWorking(this, fail("gtceu.machine.block_conversation.fail.1".toComponent))
                return
            }
            if (cardId == 0) {
                RecipeResult.ofWorking(this, fail("gtceu.machine.block_conversation.fail.2".toComponent))
                return
            }
            RecipeResult.ofWorking(this, null)
            for (s in it.aeItemHandler.inventory) {
                val i = s.getStackInSlot(0)
                if (!i.isEmpty) {
                    blockMap[i.item]?.let { b ->
                        val p = min(this.parallel, ((c as? BasicCellInventory)?.remainingItemCount ?: Long.MAX_VALUE))
                        val g = (s as IMETransfer).extractGenericStack(p, false, true)
                        it.insertCell(AEItemKey.of(b), g!!.amount, Actionable.MODULATE)
                        if (cardId < 3) return
                    }
                }
            }
        }
    }

    override fun addDisplayText(textList: MutableList<Component>) {
        MultiblockDisplayText.builder(textList, isFormed())
            .setWorkingStatus(recipeLogic.isWorkingEnabled, recipeLogic.isActive)
            .addEnergyUsageLine(energyContainer)
            .addEnergyTierLine(tier)
            .addParallelsLine(parallel)
            .addWorkingStatusLine()
            .addProgressLine(recipeLogic.progressPercent)
    }

    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        configuratorPanel.attachConfigurators(
            IFancyConfiguratorButton.Toggle(
                GuiTextures.BUTTON_POWER.getSubTexture(0.0, 0.0, 1.0, 0.5),
                GuiTextures.BUTTON_POWER.getSubTexture(0.0, 0.5, 1.0, 0.5),
                { this.isWorkingEnabled },
                { _, pressed -> this.isWorkingEnabled = pressed }
            )
                .setTooltipsSupplier { listOf(if (it) "behaviour.soft_hammer.enabled".toComponent else "behaviour.soft_hammer.disabled".toComponent) }
        )
    }

    override fun createUIWidget(): Widget = (super.createUIWidget() as WidgetGroup).let {
        return it.addWidget(
            SlotWidget(
                machineStorage.storage,
                0,
                it.sizeWidth - 30,
                it.sizeHeight - 30,
                true,
                true
            ).setBackground(GuiTextures.SLOT)
        )
    }

    override fun getFieldHolder() = MANAGED_FIELD_HOLDER

    companion object {
        val CARD_1: Item by lazy { CONVERSION_SIMULATE_CARD.asItem() }
        val CARD_2: Item by lazy { FAST_CONVERSION_SIMULATE_CARD.asItem() }
        val CARD_3: Item by lazy { GTLAddItems.ULTIMATE_CONVERSATION_CARD.asItem() }
        val CARD_4: Item by lazy { GTLAddItems.ASTRAL_ARRAY.asItem() }
        val RAW_RECIPE: GTRecipe by lazy { GTRecipeBuilder.ofRaw().inputEU(EUT).buildRawRecipe() }
        val EUT: Long = VA[UHV].toLong()
        val MANAGED_FIELD_HOLDER = ManagedFieldHolder(ConversationMachine::class.java, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER)

        val HASH_STRATEGY = object : Hash.Strategy<Item> {
            override fun hashCode(o: Item?) = o?.hashCode() ?: 0
            override fun equals(a: Item?, b: Item?) = a?.hashCode() == b?.hashCode()
        }

        val blockMap: Map<Item, Item> by lazy {
            val map = Object2ObjectOpenCustomHashMap<Item, Item>(HASH_STRATEGY)
            map[BONE_BLOCK.asItem()] = getBlock("kubejs:essence_block").asItem()
            map[OAK_LOG.asItem()] = CRIMSON_STEM.asItem()
            map[BIRCH_LOG.asItem()] = WARPED_STEM.asItem()
            map[getBlock(block, GTMaterials.Calcium).asItem()] = BONE_BLOCK.asItem()
            map[MOSS_BLOCK.asItem()] = SCULK.asItem()
            map[GRASS_BLOCK.asItem()] = MOSS_BLOCK.asItem()
            map[getBlock("kubejs:infused_obsidian").asItem()] = getBlock("kubejs:draconium_block_charged").asItem()
            return@lazy map
        }

        class ConversationRecipeLogic(val cMachine: ConversationMachine) :
            RecipeLogic(cMachine),
            IRecipeStatus {

            override fun findAndHandleRecipe() {
                lastRecipe = null
                recipeDirty = false
                if (!cMachine.beforeWork()) {
                    status = Status.IDLE
                    progress = 0
                    duration = 0
                    return
                }
                setupRecipe(RAW_RECIPE)
            }

            override fun setupRecipe(recipe: GTRecipe) {
                if (recipe.checkConditions(this).isSuccess) {
                    this.lastRecipe = recipe
                    this.status = Status.WORKING
                    this.progress = 0
                    this.duration = when (cMachine.cardId) {
                        4 -> 20
                        3 -> 30
                        2 -> 40
                        1 -> 60
                        else -> 60
                    }
                }
            }

            override fun handleRecipeWorking() {
                if (cMachine.tickConsume()) {
                    this.status = Status.WORKING
                    cMachine.onWorking()
                    ++this.progress
                } else {
                    this.setWaiting(null)
                }
                if (this.status == Status.WAITING) this.doDamping()
            }

            override fun onRecipeFinish() {
                cMachine.afterWorking()
                findAndHandleRecipe()
            }
        }
    }
}