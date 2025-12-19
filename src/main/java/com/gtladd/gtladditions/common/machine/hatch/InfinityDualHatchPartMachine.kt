package com.gtladd.gtladditions.common.machine.hatch

import appeng.api.config.Actionable
import appeng.api.networking.security.IActionSource
import appeng.api.stacks.AEKey
import appeng.api.storage.MEStorage
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CircuitFancyConfigurator
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.FancyTankConfigurator
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDistinctPart
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine
import com.gtladd.gtladditions.common.machine.trait.FastNotifiableInputFluidTank
import com.gtladd.gtladditions.common.machine.trait.FastNotifiableInputItemStack
import com.gtladd.gtladditions.utils.CommonUtils.createRainbowComponent
import com.gtladd.gtladditions.utils.TransferHelper
import com.hepdd.gtmthings.api.machine.fancyconfigurator.ButtonConfigurator
import com.hepdd.gtmthings.api.machine.fancyconfigurator.InventoryFancyConfigurator
import com.hepdd.gtmthings.common.block.machine.trait.CatalystFluidStackHandler
import com.hepdd.gtmthings.common.block.machine.trait.CatalystItemStackHandler
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup
import com.lowdragmc.lowdraglib.gui.texture.TextTexture
import com.lowdragmc.lowdraglib.gui.util.ClickData
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import com.lowdragmc.lowdraglib.side.fluid.FluidTransferHelper
import com.lowdragmc.lowdraglib.side.item.ItemTransferHelper
import com.lowdragmc.lowdraglib.syncdata.ISubscription
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.objects.Object2LongMap
import it.unimi.dsi.fastutil.objects.Object2LongMaps
import it.unimi.dsi.fastutil.objects.ObjectIterator
import it.unimi.dsi.fastutil.objects.ObjectSets
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.server.TickTask
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import org.gtlcore.gtlcore.api.machine.trait.NotifiableCircuitItemStackHandler

class InfinityDualHatchPartMachine(holder: IMachineBlockEntity) :
    TieredIOPartMachine(holder, 14, IO.IN), IMachineLife, IDistinctPart, MEStorage {

    @field:Persisted
    private val shareTank: CatalystFluidStackHandler = CatalystFluidStackHandler(this, 9, 16000L, IO.IN, IO.NONE)

    @field:Persisted
    private val shareInventory: CatalystItemStackHandler = CatalystItemStackHandler(this, 9, IO.IN, IO.NONE)

    @field:Persisted
    private val circuitInventory: NotifiableCircuitItemStackHandler = NotifiableCircuitItemStackHandler(this)

    @field:Persisted
    private val tank: FastNotifiableInputFluidTank = FastNotifiableInputFluidTank(this)

    @field:Persisted
    private val inventory: FastNotifiableInputItemStack = FastNotifiableInputItemStack(this)

    private var autoIOSubs: TickableSubscription? = null
    private var inventorySubs: ISubscription? = null
    private var tankSubs: ISubscription? = null
    private var hasFluidTransfer = false
    private var hasItemTransfer = false

    val itemIterator: ObjectIterator<Object2LongMap.Entry<ItemStack>>
        get() = Object2LongMaps.unmodifiable(inventory.getItemStorage()).object2LongEntrySet().iterator()

    val fluidIterator: ObjectIterator<FluidStack>
        get() = ObjectSets.unmodifiable(tank.getFluidStorage()).iterator()

    override fun onLoad() {
        super.onLoad()
        (this.level as? ServerLevel)?.let { serverLevel ->
            serverLevel.server.tell(TickTask(0, this::updateInventorySubscription))
            inventorySubs = inventory.addChangedListener(this::updateInventorySubscription)
            tankSubs = tank.addChangedListener(this::updateInventorySubscription)
        }
    }

    override fun onUnload() {
        super.onUnload()
        this.inventorySubs?.unsubscribe().also { this.inventorySubs = null }
        this.tankSubs?.unsubscribe().also { this.tankSubs = null }
    }

    override fun setWorkingEnabled(workingEnabled: Boolean) {
        super.setWorkingEnabled(workingEnabled)
        this.updateInventorySubscription()
    }

    override fun onNeighborChanged(block: Block, fromPos: BlockPos, isMoving: Boolean) {
        super.onNeighborChanged(block, fromPos, isMoving)
        this.updateInventorySubscription()
    }

    override fun onMachineRemoved() {
        clearInventory(this.shareInventory)
    }

    private fun updateInventorySubscription() {
        this.level?.let { level ->
            this.hasItemTransfer = ItemTransferHelper.getItemTransfer(
                level,
                this.pos.relative(this.frontFacing),
                this.frontFacing.opposite
            ) != null
            this.hasFluidTransfer = FluidTransferHelper.getFluidTransfer(
                level,
                this.pos.relative(this.frontFacing),
                this.frontFacing.opposite
            ) != null
        } ?: run {
            this.hasItemTransfer = false
            this.hasFluidTransfer = false
        }

        if (!this.isWorkingEnabled || !this.hasItemTransfer && !this.hasFluidTransfer) {
            this.autoIOSubs?.unsubscribe().also { this.autoIOSubs = null }
        } else {
            this.autoIOSubs = this.subscribeServerTick(this.autoIOSubs) { this.autoIO() }
        }
    }

    private fun autoIO() {
        if (this.offsetTimer % 5L == 0L && this.isWorkingEnabled) {
            if (this.hasItemTransfer) {
                this.inventory.importFromNearby(this.frontFacing)
            }
            if (this.hasFluidTransfer) {
                this.tank.importFromNearby(this.frontFacing)
            }
        }
    }

    private fun refundAll() {
        var changed = false
        if (this.hasItemTransfer) {
            this.inventory.exportToNearby(this.frontFacing)
            changed = true
        }
        if (this.hasFluidTransfer) {
            this.tank.exportToNearby(this.frontFacing)
            changed = true
        }
        if (changed) setWorkingEnabled(false)
    }

    // ========================================
    // ME integration
    // ========================================

    override fun insert(what: AEKey?, amount: Long, mode: Actionable?, source: IActionSource?): Long {
        MEStorage.checkPreconditions(what, amount, mode, source)

        return TransferHelper.insertFromME(
            this.inventory,
            this.tank,
            what!!,
            amount,
            mode!!
        )
    }

    override fun extract(what: AEKey?, amount: Long, mode: Actionable?, source: IActionSource?): Long = 0

    override fun getDescription(): Component? {
        return Component.translatable("block.gtladditions.infinity_input_dual_hatch")
    }

    // ========================================
    // GUI
    // ========================================

    override fun createUIWidget(): Widget {
        val height = 117
        val width = 178
        val group = WidgetGroup(0, 0, width + 8, height + 4)
        val componentPanel = (ComponentPanelWidget(8, 5) { textList: MutableList<Component?>? ->
            this.addDisplayText(
                textList!!
            )
        })
            .setMaxWidthLimit(width - 16)
        val screen = (DraggableScrollableWidgetGroup(4, 4, width, height))
            .setBackground(GuiTextures.DISPLAY).addWidget(componentPanel)
        group.addWidget(screen)
        return group
    }

    private fun addDisplayText(textList: MutableList<Component?>) {
        inventory.addDisplayText(textList)
        tank.addDisplayText(textList)

        if (textList.isEmpty()) {
            textList.add(Component.translatable("gtmthings.machine.huge_item_bus.tooltip.3"))
        }

        textList.add(
            0,
            Component.translatable(
                "gtmthings.machine.huge_item_bus.tooltip.2", inventory.realSize, createRainbowComponent(
                    Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.parallel").string
                )
            )
                .setStyle(
                    Style.EMPTY.withColor(ChatFormatting.GREEN)
                )
        )
        textList.add(
            1, Component.translatable(
                "gtmthings.machine.huge_dual_hatch.tooltip.2", tank.realSize, createRainbowComponent(
                    Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.parallel").string
                )
            ).setStyle(
                Style.EMPTY.withColor(ChatFormatting.GREEN)
            )
        )
    }

    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        super<TieredIOPartMachine>.attachConfigurators(configuratorPanel)

        configuratorPanel.attachConfigurators(CircuitFancyConfigurator(this.circuitInventory.storage))

        configuratorPanel.attachConfigurators(
            (ButtonConfigurator(
                GuiTextureGroup(
                    GuiTextures.BUTTON,
                    TextTexture("\ud83d\udd19")
                )
            ) { clickData: ClickData ->
                this.refundAll()
            }.setTooltips(listOf<Component>(Component.translatable("gtmthings.machine.huge_item_bus.tooltip.1"))))
        )

        configuratorPanel.attachConfigurators(
            InventoryFancyConfigurator(
                this.shareInventory.storage,
                Component.translatable("gui.gtmthings.share_inventory.title")
            ).setTooltips(
                listOf<Component>(
                    Component.translatable("gui.gtmthings.share_inventory.desc.0"),
                    Component.translatable("gui.gtmthings.share_inventory.desc.1"),
                    Component.translatable("gui.gtmthings.share_inventory.desc.2")
                )
            )
        )

        configuratorPanel.attachConfigurators(
            (FancyTankConfigurator(
                this.shareTank.storages,
                Component.translatable("gui.gtceu.share_tank.title")
            ))
                .setTooltips(
                    listOf<Component>(
                        Component.translatable("gui.gtceu.share_tank.desc.0"),
                        Component.translatable("gui.gtceu.share_inventory.desc.1")
                    )
                )
        )
    }

    override fun isDistinct(): Boolean = inventory.isDistinct && circuitInventory.isDistinct && shareInventory.isDistinct

    override fun setDistinct(isDistinct: Boolean) {
        inventory.isDistinct = isDistinct
        circuitInventory.isDistinct = isDistinct
        shareInventory.isDistinct = isDistinct
    }

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    init {
        this.workingEnabled = false
    }

    companion object {
        private val MANAGED_FIELD_HOLDER: ManagedFieldHolder =
            ManagedFieldHolder(InfinityDualHatchPartMachine::class.java, TieredIOPartMachine.MANAGED_FIELD_HOLDER)
    }
}