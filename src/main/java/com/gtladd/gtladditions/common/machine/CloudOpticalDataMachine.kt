package com.gtladd.gtladditions.common.machine

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gregtechceu.gtceu.api.machine.TieredEnergyMachine
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.client.util.TooltipHelper
import com.gregtechceu.gtceu.common.data.machines.GTResearchMachines
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gregtechceu.gtceu.utils.ResearchManager
import com.gtladd.gtladditions.utils.CloudNetworkManager
import com.gtladd.gtladditions.utils.CloudTeamUtil
import com.hepdd.gtmthings.api.capability.IBindable
import com.hepdd.gtmthings.utils.TeamUtil
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.utils.TextUtil
import java.util.UUID

class CloudOpticalDataMachine(holder: IMachineBlockEntity) :
    TieredEnergyMachine(holder, GTValues.UIV),
    IMachineLife,
    IFancyUIMachine,
    IDataStickInteractable,
    IBindable {

    companion object {

        @JvmField
        val MANAGED_FIELD_HOLDER = ManagedFieldHolder(
            CloudOpticalDataMachine::class.java,
            TieredEnergyMachine.MANAGED_FIELD_HOLDER
        )

        private val ENERGY_PER_DATA = GTValues.V[GTValues.UV] * 3 / 4
    }

    @Persisted
    @DescSynced
    var teamId: UUID? = null
        private set

    @Persisted
    protected val importItems = NotifiableItemStackHandler(this, 90, IO.BOTH)

    @Persisted
    protected val createItem = NotifiableItemStackHandler(this, 1, IO.NONE, IO.BOTH) {
        object : ItemStackTransfer(1) {
            override fun getSlotLimit(slot: Int): Int = 1
        }
    }

    private val recipes: MutableSet<GTRecipe> = ObjectOpenHashSet()
    private var recipesDirty = true

    @Persisted
    private var isCreate = false

    private var hasPower = true
    private var energySubs: TickableSubscription? = null

    init {
        importItems
            .setFilter { stack ->
                ResearchManager.isStackDataItem(stack, true) && ResearchManager.hasResearchTag(stack)
            }
            .addChangedListener(::markRecipesDirty)
        createItem
            .setFilter { stack -> stack.`is`(GTResearchMachines.CREATIVE_DATA_ACCESS_HATCH.item) }
            .addChangedListener {
                isCreate = createItem.getStackInSlot(0).`is`(GTResearchMachines.CREATIVE_DATA_ACCESS_HATCH.item)
            }
    }

    override fun createEnergyContainer(vararg args: Any): NotifiableEnergyContainer =
        NotifiableEnergyContainer.receiverContainer(
            this,
            64L * GTValues.V[GTValues.UIV],
            GTValues.V[GTValues.UIV],
            16
        )

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    override fun isRemote(): Boolean = super<TieredEnergyMachine>.isRemote

    override fun getUUID(): UUID? = teamId

    override fun setUUID(uuid: UUID?) {
        teamId = uuid
    }

    override fun onLoad() {
        super.onLoad()
        if (!isRemote) {
            CloudNetworkManager.registerDataMachine(this)
            markRecipesDirty()
            energySubs = subscribeServerTick(energySubs, ::updateEnergy)
        }
    }

    override fun onUnload() {
        super.onUnload()
        if (!isRemote) CloudNetworkManager.unregisterDataMachine(this)
        energySubs?.unsubscribe()
        energySubs = null
    }

    override fun onMachinePlaced(player: LivingEntity?, stack: ItemStack) {
        CloudTeamUtil.bindOnPlacement(this, player)
        if (!isRemote) {
            CloudNetworkManager.registerDataMachine(this)
            markRecipesDirty()
        }
    }

    override fun onDataStickRightClick(player: Player, stack: ItemStack): InteractionResult =
        CloudTeamUtil.bindFromDataStick(this, player)

    override fun onDataStickLeftClick(player: Player, stack: ItemStack): Boolean =
        CloudTeamUtil.unbindFromDataStick(this, player)

    override fun onMachineRemoved() {
        if (!isRemote) CloudNetworkManager.unregisterDataMachine(this)
        clearInventory(importItems)
        createItem.setStackInSlot(0, ItemStack.EMPTY)
    }

    fun getDataCount(): Long {
        var count = 1L
        for (slot in 0 until importItems.slots) {
            val stack = importItems.getStackInSlot(slot)
            if (!stack.isEmpty && ResearchManager.isStackDataItem(stack, true)) count++
        }
        return count
    }

    fun getEnergyDemand(): Long = if (isCreate) ENERGY_PER_DATA else getDataCount() * ENERGY_PER_DATA

    private fun updateEnergy() {
        val demand = getEnergyDemand()
        if (demand <= 0L) {
            hasPower = true
            return
        }
        hasPower = energyContainer.removeEnergy(demand) >= demand
    }

    fun rebuildData() {
        if (level == null) return
        recipes.clear()
        for (slot in 0 until importItems.slots) {
            val stack = importItems.getStackInSlot(slot)
            if (stack.isEmpty) continue
            val researchId = ResearchManager.readResearchId(stack) ?: continue
            if (!ResearchManager.isStackDataItem(stack, true)) continue
            val entries = researchId.first.getDataStickEntry(researchId.second)
            if (entries != null) recipes.addAll(entries)
        }
    }

    private fun markRecipesDirty() {
        recipesDirty = true
    }

    private fun refreshRecipesIfNeeded() {
        if (!recipesDirty || level == null) return
        rebuildData()
        recipesDirty = false
    }

    private fun getRecipes(): Set<GTRecipe> {
        refreshRecipesIfNeeded()
        return recipes
    }

    fun providesRecipe(recipe: GTRecipe): Boolean = hasPower && (isCreate || recipe in getRecipes())

    fun tryStoreResearchData(dataStack: ItemStack): Boolean {
        if (
            dataStack.isEmpty ||
            !ResearchManager.isStackDataItem(dataStack, true) ||
            !ResearchManager.hasResearchTag(dataStack)
        ) {
            return false
        }

        for (slot in 0 until importItems.slots) {
            if (!importItems.getStackInSlot(slot).isEmpty) continue
            if (!importItems.insertItem(slot, dataStack, true).isEmpty) continue
            return importItems.insertItem(slot, dataStack.copy(), false).isEmpty
        }
        return false
    }

    override fun createUIWidget(): Widget {
        val group = WidgetGroup(0, 0, 176, 145)
        group.addWidget(ComponentPanelWidget(5, 5, ::addDisplayText).setMaxWidthLimit(136))
        val slotScroll = DraggableScrollableWidgetGroup(5, 84, 168, 54)
        for (row in 0 until 10) {
            for (column in 0 until 9) {
                slotScroll.addWidget(
                    object : SlotWidget(importItems, row * 9 + column, 2 + column * 18, row * 18) {
                        override fun isEnabled(): Boolean = true
                    }.setBackgroundTexture(GuiTextures.SLOT)
                )
            }
        }
        group.addWidget(slotScroll)
        group.addWidget(
            SlotWidget(createItem, 0, group.sizeWidth - 30, 20)
                .setBackgroundTexture(GuiTextures.SLOT)
                .appendHoverTooltips(Component.translatable("gui.gtladditions.cloud_data_machine.create"))
        )
        group.setBackground(GuiTextures.BACKGROUND_INVERSE)
        return group
    }

    private fun addDisplayText(textList: MutableList<Component>) {
        if (isRemote) return
        textList.add(self().blockState.block.name)
        val boundTeamId = teamId
        if (boundTeamId == null) {
            textList.add(Component.translatable("gui.gtladditions.cloud.not_bound"))
        } else {
            if (TeamUtil.hasOwner(level, boundTeamId)) {
                textList.add(
                    Component.translatable(
                        "gui.gtladditions.cloud.bind_success",
                        TeamUtil.GetName(level, boundTeamId)
                    )
                )
            }
            textList.add(
                Component.translatable(
                    "gui.gtladditions.cloud_data_machine.cloud_count",
                    CloudNetworkManager.getLoadedDataMachineCount(boundTeamId)
                )
            )
        }
        if (isCreate) {
            textList.add(
                Component.translatable(
                    "gui.gtladditions.cloud_data_machine.recipes_count.1",
                    Component.literal(
                        TextUtil.full_color(
                            Component.translatable("gui.gtladditions.cloud_data_machine.recipes_count.2").string
                        )
                    ).withStyle { style -> style.withColor(TooltipHelper.RAINBOW.current) }
                )
            )
        } else {
            textList.add(
                Component.translatable(
                    "gui.gtladditions.cloud_data_machine.recipes_count.0",
                    getRecipes().size
                )
            )
        }
        textList.add(
            Component.translatable(
                "gui.gtladditions.cloud_data_machine.energy_demand",
                FormattingUtil.formatNumbers(getEnergyDemand())
            )
        )
        textList.add(
            Component.translatable(
                if (hasPower) {
                    "gui.gtladditions.cloud_data_machine.power_normal"
                } else {
                    "gui.gtladditions.cloud_data_machine.power_insufficient"
                }
            ).withStyle { style -> style.withColor(if (hasPower) 0x55FF55 else 0xFF5555) }
        )
    }
}