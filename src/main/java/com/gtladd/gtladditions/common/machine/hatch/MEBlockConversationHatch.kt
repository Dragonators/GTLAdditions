package com.gtladd.gtladditions.common.machine.hatch

import appeng.api.config.Actionable
import appeng.api.networking.IGridNodeListener
import appeng.api.networking.IManagedGridNode
import appeng.api.networking.security.IActionSource
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.AEKeyType
import appeng.api.stacks.GenericStack
import appeng.api.storage.MEStorage
import appeng.api.storage.cells.StorageCell
import appeng.items.storage.BasicStorageCell
import appeng.me.cells.BasicCellHandler
import com.gregtechceu.gtceu.api.capability.IControllable
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler
import com.gregtechceu.gtceu.integration.ae2.gui.widget.AEItemConfigWidget
import com.gregtechceu.gtceu.integration.ae2.machine.feature.IGridConnectedMachine
import com.gregtechceu.gtceu.integration.ae2.machine.trait.GridNodeHolder
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemList
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer
import com.lowdragmc.lowdraglib.syncdata.ISubscription
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.annotation.RequireRerender
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.core.Direction
import net.minecraft.server.TickTask
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.api.machine.trait.MEStock.IMETransfer
import org.gtlcore.gtlcore.integration.ae2.FastInfinityCell
import org.gtlcore.gtlcore.integration.ae2.InfinityCell
import org.gtlcore.gtlcore.integration.ae2.storage.FastInfinityCellHandler
import org.gtlcore.gtlcore.integration.ae2.storage.InfinityCellHandler
import java.util.*

class MEBlockConversationHatch(holder: IMachineBlockEntity) :
    MultiblockPartMachine(holder),
    IGridConnectedMachine,
    IControllable,
    IInteractedMachine,
    IMachineLife,
    IDropSaveMachine {
    @DescSynced
    private var isOnline = false

    @Persisted
    @DescSynced
    @RequireRerender
    private var workingEnabled = true

    @Persisted
    private val nodeHolder = GridNodeHolder(this)

    @Persisted
    private val machineStorage = NotifiableItemStackHandler(this, 1, IO.NONE, IO.BOTH) {
        object : ItemStackTransfer(1) {
            override fun getSlotLimit(slot: Int): Int = 1
        }
    }.setFilter(::filter)

    @Persisted
    val aeItemHandler = ExportOnlyAEItemList(this, 8)
    private var autoIOSubs: TickableSubscription? = null
    private var inventorySubs: ISubscription? = null
    private val actionSource = IActionSource.ofMachine(nodeHolder.getMainNode()::getNode)

    private fun filter(stack: ItemStack) = (stack.item is InfinityCell && (stack.item as InfinityCell).keyType == AEKeyType.items()) ||
        (stack.item is BasicStorageCell && (stack.item as BasicStorageCell).keyType == AEKeyType.items() || (stack.item is FastInfinityCell))

    fun getCellInventory(): StorageCell? {
        val i = machineStorage.getStackInSlot(0)
        return when (i.item) {
            is FastInfinityCell -> FastInfinityCellHandler.INSTANCE.getCellInventory(i, null)
            is InfinityCell -> InfinityCellHandler.INSTANCE.getCellInventory(i, null)
            is BasicStorageCell -> BasicCellHandler.INSTANCE.getCellInventory(i, null)
            else -> null
        }
    }

    fun insertCell(what: AEItemKey, amount: Long, mode: Actionable) = this.getCellInventory()!!.insert(what, amount, mode, actionSource)

    override fun createUIWidget(): Widget {
        val group = WidgetGroup(0, 0, 152, 80)
        val container = WidgetGroup(4, 4, 144, 72).also {
            it.addWidget(LabelWidget(0, 42) { if (this.isOnline) "gtceu.gui.me_network.online" else "gtceu.gui.me_network.offline" })
                .addWidget(AEItemConfigWidget(0, 4, this.aeItemHandler))
                .addWidget(SlotWidget(machineStorage.storage, 0, 63, 50, true, true).setBackground(GuiTextures.SLOT))
        }
        return group.addWidget(container)
    }

    override fun onRotated(oldFacing: Direction, newFacing: Direction) {
        super.onRotated(oldFacing, newFacing)
        this.getMainNode().setExposedOnSides(EnumSet.of(newFacing))
    }

    override fun onMainNodeStateChanged(reason: IGridNodeListener.State) {
        super.onMainNodeStateChanged(reason)
        this.updateInventorySubscription()
    }

    override fun onLoad() {
        super.onLoad()
        if (level is ServerLevel) level?.server!!.tell(TickTask(0, ::updateInventorySubscription))
        inventorySubs = aeItemHandler.addChangedListener(::updateInventorySubscription)
    }

    override fun onUnload() {
        super.onUnload()
        if (inventorySubs != null) {
            inventorySubs!!.unsubscribe()
            inventorySubs = null
        }
    }

    private fun updateInventorySubscription() {
        if (shouldSubscribe()) {
            autoIOSubs = subscribeServerTick(autoIOSubs, ::autoIO)
        } else if (autoIOSubs != null) {
            autoIOSubs!!.unsubscribe()
            autoIOSubs = null
        }
    }

    private fun autoIO() {
        if (isWorkingEnabled() && shouldSyncME() && updateMEStatus()) {
            syncME()
            updateInventorySubscription()
        }
    }

    private fun syncME() {
        val networkInv = getMainNode().grid!!.storageService.inventory
        refundCell(networkInv)
        for (s in aeItemHandler.getInventory()) {
            val g = s.exceedStack()
            if (g != null) {
                val l = g.amount()
                val n = networkInv.insert(g.what(), g.amount(), Actionable.MODULATE, actionSource)
                if (n > 0) {
                    (s as IMETransfer).extractGenericStack(n, false, true)
                    continue
                } else {
                    (s as IMETransfer).extractGenericStack(l, false, true)
                }
            }
            s.requestStack()?.let {
                val n = networkInv.extract(it.what(), it.amount(), Actionable.MODULATE, actionSource)
                if (n != 0L) s.addStack(GenericStack(it.what(), n))
            }
        }
    }

    private fun refundCell(inventory: MEStorage) = getCellInventory()?.let {
        val k = it.availableStacks.iterator()
        while (k.hasNext()) {
            val e = k.next()
            val n = inventory.insert(e.key, e.longValue, Actionable.MODULATE, actionSource)
            if (n > 0) it.extract(e.key, n, Actionable.MODULATE, actionSource)
        }
    }

    private fun shouldSubscribe() = this.isWorkingEnabled() && this.isOnline()

    override fun getMainNode(): IManagedGridNode = nodeHolder.getMainNode()

    override fun isOnline() = this.isOnline

    override fun setOnline(online: Boolean) {
        this.isOnline = online
    }

    override fun isWorkingEnabled() = this.workingEnabled

    override fun setWorkingEnabled(workingEnabled: Boolean) {
        this.workingEnabled = workingEnabled
        this.updateInventorySubscription()
    }

    override fun getFieldHolder() = MANAGED_FIELD_HOLDER

    override fun onMachineRemoved() = clearInventory(machineStorage)

    companion object {
        val MANAGED_FIELD_HOLDER = ManagedFieldHolder(MEBlockConversationHatch::class.java, MultiblockPartMachine.MANAGED_FIELD_HOLDER)
    }
}