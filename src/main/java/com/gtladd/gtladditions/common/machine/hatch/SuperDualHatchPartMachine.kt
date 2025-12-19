package com.gtladd.gtladditions.common.machine.hatch

import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.FancyTankConfigurator
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank
import com.hepdd.gtmthings.common.block.machine.multiblock.part.HugeBusPartMachine
import com.hepdd.gtmthings.common.block.machine.trait.CatalystFluidStackHandler
import com.hepdd.gtmthings.utils.FormatUtil
import com.lowdragmc.lowdraglib.gui.util.ClickData
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import com.lowdragmc.lowdraglib.side.fluid.FluidTransferHelper
import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer
import com.lowdragmc.lowdraglib.side.fluid.forge.FluidHelperImpl
import com.lowdragmc.lowdraglib.side.item.ItemTransferHelper
import com.lowdragmc.lowdraglib.syncdata.ISubscription
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.level.Level
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.fluids.capability.IFluidHandler
import org.gtlcore.gtlcore.utils.NumberUtils
import java.util.function.Predicate

open class SuperDualHatchPartMachine(holder: IMachineBlockEntity, tier: Int, vararg args: Any?) :
    HugeBusPartMachine(holder, tier, IO.IN, 9, *args) {
    @field:Persisted
    protected val tank: NotifiableFluidTank

    @field:Persisted
    protected val shareTank: CatalystFluidStackHandler
    protected var tankSubs: ISubscription? = null
    private var hasFluidTransfer = false
    private var hasItemTransfer = false

    protected open fun createTank(): NotifiableFluidTank {
        return SuperNotifiableFluidTank(this@SuperDualHatchPartMachine, 24, Long.Companion.MAX_VALUE shr 12, IO.IN)
    }

    fun getTankInventorySize() : Int {
        return this.tank.storages.size
    }

    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        super.attachConfigurators(configuratorPanel)
        configuratorPanel.attachConfigurators(
            (FancyTankConfigurator(
                this.shareTank.storages,
                Component.translatable("gui.gtceu.share_tank.title")
            ))
                .setTooltips(
                    listOf<Component?>(Component.translatable("gui.gtceu.share_tank.desc.0"),
                        Component.translatable("gui.gtceu.share_inventory.desc.1"))
                )
        )
    }

    override fun onLoad() {
        super.onLoad()
        this.tankSubs = this.tank.addChangedListener { this.updateInventorySubscription() }
    }

    override fun onUnload() {
        super.onUnload()
        if (this.tankSubs != null) {
            this.tankSubs!!.unsubscribe()
            this.tankSubs = null
        }
    }

    override fun refundAll(clickData: ClickData) {
        super.refundAll(clickData)
        if (this.hasFluidTransfer) {
            this.tank.exportToNearby(this.frontFacing)
        }
    }

    override fun updateInventorySubscription() {
        val canOutput = this.io == IO.OUT && (!this.tank.isEmpty || !this.getInventory().isEmpty)
        val level = this.level
        if (level != null) {
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
        } else {
            this.hasItemTransfer = false
            this.hasFluidTransfer = false
        }
        if (!this.isWorkingEnabled || !canOutput && this.io != IO.IN || !this.hasItemTransfer && !this.hasFluidTransfer) {
            if (this.autoIOSubs != null) {
                this.autoIOSubs!!.unsubscribe()
                this.autoIOSubs = null
            }
        } else {
            this.autoIOSubs = this.subscribeServerTick(this.autoIOSubs) { this.autoIO() }
        }
    }

    override fun autoIO() {
        if (this.offsetTimer % 5L == 0L && this.isWorkingEnabled) {
            if (this.io == IO.OUT) {
                if (this.hasItemTransfer) {
                    this.getInventory().exportToNearby(this.frontFacing)
                }
                if (this.hasFluidTransfer) {
                    this.tank.exportToNearby(this.frontFacing)
                }
            } else if (this.io == IO.IN) {
                if (this.hasItemTransfer) {
                    this.getInventory().importFromNearby(this.frontFacing)
                }
                if (this.hasFluidTransfer) {
                    this.tank.importFromNearby(this.frontFacing)
                }
            }
        }
    }

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
        var itemCount = 0
        var tankCount = 0
        for (i in 0..< inventorySize - 1) {
            val `is` = super.getInventory().getStackInSlot(i)
            if (!`is`.isEmpty) {
                textList.add(
                    `is`.displayName.copy().setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))
                        .append(
                            Component.literal(FormatUtil.formatNumber(`is`.count.toLong()))
                                .setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA))
                        )
                )
                ++itemCount
            }
        }
        for (i in 0..getTankInventorySize() - 1) {
            val fs = this.tank.getFluidInTank(i)
            if (!fs.isEmpty) {
                textList.add(
                    fs.displayName.copy().setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD))
                        .append(
                            Component.literal(
                                if (fs.amount < 1000L) fs.amount
                                    .toString() + "mB" else NumberUtils.formatLong(fs.amount / 1000L) + "B"
                            )
                                .setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA))
                        )
                )
                ++tankCount
            }
        }
        if (textList.isEmpty()) {
            textList.add(Component.translatable("gtmthings.machine.huge_item_bus.tooltip.3"))
        }
        textList.add(
            0,
            Component.translatable("gtmthings.machine.huge_item_bus.tooltip.2", itemCount, inventorySize)
                .setStyle(
                    Style.EMPTY.withColor(ChatFormatting.GREEN)
                )
        )
        textList.add(
            1, Component.translatable("gtmthings.machine.huge_dual_hatch.tooltip.2", tankCount, getTankInventorySize()).setStyle(
                Style.EMPTY.withColor(ChatFormatting.GREEN)
            )
        )
    }

    override fun getFieldHolder(): ManagedFieldHolder {
        return MANAGED_FIELD_HOLDER
    }

    init {
        this.tank = this.createTank()
        this.shareTank = CatalystFluidStackHandler(this, 9, 16000L, IO.IN, IO.NONE)
        this.workingEnabled = false
    }

    companion object {
        protected val MANAGED_FIELD_HOLDER: ManagedFieldHolder =
            ManagedFieldHolder(SuperDualHatchPartMachine::class.java, HugeBusPartMachine.MANAGED_FIELD_HOLDER)

        private class SuperNotifiableFluidTank(
            machine: MetaMachine,
            slots: Int,
            capacity: Long,
            io: IO
        ) : NotifiableFluidTank(machine, slots, capacity, io) {

            override fun exportToNearby(vararg facings: Direction) {
                if (!isEmpty) {
                    machine.level?.let { level ->
                        for (facing in facings) {
                            exportToTarget(
                                this,
                                machine.getFluidCapFilter(facing),
                                level,
                                machine.pos.relative(facing),
                                facing.opposite
                            )
                        }
                    }
                }
            }

            override fun canCapOutput(): Boolean = true

            companion object {
                fun exportToTarget(
                    source: IFluidTransfer,
                    filter: Predicate<FluidStack>,
                    level: Level,
                    pos: BlockPos,
                    direction: Direction?
                ) {
                    val state = level.getBlockState(pos)
                    if (!state.hasBlockEntity()) return

                    val blockEntity = level.getBlockEntity(pos) ?: return
                    val cap = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, direction).resolve()
                    if (!cap.isPresent) return

                    val target = cap.get()
                    for (srcIndex in 0 until source.tanks) {
                        val currentFluid = source.getFluidInTank(srcIndex)
                        if (currentFluid.isEmpty || !filter.test(currentFluid)) {
                            continue
                        }

                        val toDrain = currentFluid.copy()
                        var remainAmount = currentFluid.amount
                        do {
                            val filled = target.fill(
                                FluidHelperImpl.toFluidStack(source.drain(toDrain, true)),
                                IFluidHandler.FluidAction.SIMULATE
                            )
                            if (filled > 0) {
                                toDrain.amount = filled.toLong()
                                target.fill(
                                    FluidHelperImpl.toFluidStack(source.drain(toDrain, false)),
                                    IFluidHandler.FluidAction.EXECUTE
                                )
                                remainAmount -= filled
                            } else {
                                break
                            }
                            toDrain.amount = remainAmount
                        } while (!toDrain.isEmpty)
                    }
                }
            }
        }
    }
}
