package com.gtladd.gtladditions.common.machine

import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.gui.widget.CloudComputationMonitorOverviewPage
import com.gtladd.gtladditions.utils.CloudNetworkManager
import com.gtladd.gtladditions.utils.CloudTeamUtil
import com.hepdd.gtmthings.api.capability.IBindable
import com.hepdd.gtmthings.utils.TeamUtil
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.util.UUID

class CloudOpticalComputationMonitorMachine(holder: IMachineBlockEntity) :
    MetaMachine(holder),
    IFancyUIMachine,
    IMachineLife,
    IDataStickInteractable,
    IBindable {

    companion object {

        @JvmField
        val MANAGED_FIELD_HOLDER = ManagedFieldHolder(
            CloudOpticalComputationMonitorMachine::class.java,
            MetaMachine.MANAGED_FIELD_HOLDER
        )
    }

    @Persisted
    @DescSynced
    var teamId: UUID? = null
        private set

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    override fun isRemote(): Boolean = super<MetaMachine>.isRemote

    override fun getUUID(): UUID? = teamId

    override fun setUUID(uuid: UUID?) {
        teamId = uuid
    }

    override fun onDataStickRightClick(player: Player, stack: ItemStack): InteractionResult =
        CloudTeamUtil.bindFromDataStick(this, player)

    override fun onDataStickLeftClick(player: Player, stack: ItemStack): Boolean =
        CloudTeamUtil.unbindFromDataStick(this, player)

    override fun onMachinePlaced(player: LivingEntity?, stack: ItemStack) {
        CloudTeamUtil.bindOnPlacement(this, player)
    }

    override fun createUIWidget(): Widget {
        val group = WidgetGroup(0, 0, 190, 125)
        group.addWidget(
            DraggableScrollableWidgetGroup(4, 4, 182, 117)
                .setBackground(GuiTextures.DISPLAY)
                .addWidget(ComponentPanelWidget(4, 5, ::addDisplayText).setMaxWidthLimit(150))
        )
        group.setBackground(GuiTextures.BACKGROUND_INVERSE)
        return group
    }

    private fun addDisplayText(textList: MutableList<Component>) {
        if (isRemote) return
        val boundPlayerId = teamId
        if (boundPlayerId == null) {
            textList.add(
                Component.translatable("gui.gtladditions.cloud.not_bound").withStyle(ChatFormatting.RED)
            )
            return
        }
        textList.add(self().blockState.block.name)
        if (TeamUtil.hasOwner(level, boundPlayerId)) {
            textList.add(
                Component.translatable(
                    "gui.gtladditions.cloud.bind_success",
                    TeamUtil.GetName(level, boundPlayerId)
                )
            )
        }
        textList.add(
            Component.translatable(
                "gui.gtladditions.cloud_computation_monitor.provider_count",
                CloudNetworkManager.getProviderCount(boundPlayerId)
            )
        )
        textList.add(
            Component.translatable(
                "gui.gtladditions.cloud_computation_monitor.max_cwu",
                FormattingUtil.formatNumbers(CloudNetworkManager.getMaxCWU(boundPlayerId))
            )
        )
        textList.add(
            Component.translatable(
                "gui.gtladditions.cloud_computation_monitor.requestable_cwu",
                FormattingUtil.formatNumbers(CloudNetworkManager.getRemainingCWU(boundPlayerId))
            )
        )
    }

    override fun attachSideTabs(tabs: TabsWidget) {
        super.attachSideTabs(tabs)
        if (teamId != null) tabs.attachSubTab(CloudComputationMonitorOverviewPage(teamId))
    }
}