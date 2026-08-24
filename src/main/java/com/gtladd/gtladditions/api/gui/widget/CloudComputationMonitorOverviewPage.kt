package com.gtladd.gtladditions.api.gui.widget

import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.common.data.CloudMachineSnapshot
import com.gtladd.gtladditions.common.data.ComputationLiveSnapshot
import com.gtladd.gtladditions.common.data.ComputationTopologySnapshot
import com.gtladd.gtladditions.events.ClientCloudHighlighter
import com.gtladd.gtladditions.utils.CloudNetworkManager
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture
import com.lowdragmc.lowdraglib.gui.texture.TextTexture
import com.lowdragmc.lowdraglib.gui.util.ClickData
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.commands.arguments.EntityAnchorArgument
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.api.gui.ExtendLabelWidget
import java.util.UUID

class CloudComputationMonitorOverviewPage(
    private val teamId: UUID?
) : IFancyUIProvider {

    companion object {

        private val ICON: IGuiTexture = ResourceTexture(
            ResourceLocation("gtceu", "textures/item/computer_monitor_cover.png")
        )
    }

    override fun createMainPage(widget: FancyMachineUIWidget): Widget = CloudOverviewWidget(teamId)

    override fun getTabIcon(): IGuiTexture = ICON

    override fun getTitle(): Component = Component.translatable("gui.gtladditions.cloud_monitor.overview")

    override fun getTabTooltips(): List<Component> = listOf(title)
}

private class CloudOverviewWidget(
    private val teamId: UUID?
) : WidgetGroup(0, 0, 280, 222) {

    companion object {

        private const val UPDATE_FULL_TOPOLOGY = 0
        private const val UPDATE_LIVE_VALUES = 1

        private fun writeTopologySnapshot(
            buffer: FriendlyByteBuf,
            snapshot: ComputationTopologySnapshot
        ) {
            buffer.writeLong(snapshot.topologyVersion)
            buffer.writeInt(snapshot.providers.size)
            snapshot.providers.forEach { writeMachineSnapshot(buffer, it) }
            buffer.writeInt(snapshot.receivers.size)
            snapshot.receivers.forEach { writeMachineSnapshot(buffer, it) }
            buffer.writeInt(snapshot.otherProviderCount)
            buffer.writeInt(snapshot.otherReceiverCount)
        }

        private fun readTopologySnapshot(buffer: FriendlyByteBuf): ComputationTopologySnapshot {
            val version = buffer.readLong()
            val providers = List(buffer.readInt()) { readMachineSnapshot(buffer) }
            val receivers = List(buffer.readInt()) { readMachineSnapshot(buffer) }
            return ComputationTopologySnapshot(
                version,
                providers,
                receivers,
                buffer.readInt(),
                buffer.readInt()
            )
        }

        private fun writeMachineSnapshot(buffer: FriendlyByteBuf, snapshot: CloudMachineSnapshot) {
            buffer.writeUtf(snapshot.dimensionId)
            buffer.writeBlockPos(snapshot.pos)
            buffer.writeBoolean(snapshot.frontPos != null)
            snapshot.frontPos?.let(buffer::writeBlockPos)
            buffer.writeItem(snapshot.item)
            buffer.writeLong(snapshot.currentCwu)
            buffer.writeLong(snapshot.maxCwu)
            buffer.writeInt(snapshot.requestedCwu)
        }

        private fun readMachineSnapshot(buffer: FriendlyByteBuf): CloudMachineSnapshot {
            val dimensionId = buffer.readUtf()
            val pos = buffer.readBlockPos()
            val frontPos = if (buffer.readBoolean()) buffer.readBlockPos() else null
            val item = buffer.readItem()
            return CloudMachineSnapshot(
                dimensionId,
                pos,
                frontPos,
                item,
                buffer.readLong(),
                buffer.readLong(),
                buffer.readInt()
            )
        }

        private fun writeLiveValues(buffer: FriendlyByteBuf, snapshot: ComputationLiveSnapshot) {
            buffer.writeInt(snapshot.providerCount)
            repeat(snapshot.providerCount) { index ->
                buffer.writeLong(snapshot.getProviderCurrentCwu(index))
                buffer.writeLong(snapshot.getProviderMaxCwu(index))
            }
            buffer.writeInt(snapshot.receiverCount)
            repeat(snapshot.receiverCount) { index ->
                buffer.writeInt(snapshot.getReceiverRequestedCwu(index))
            }
        }
    }

    private val providerScroll = DraggableScrollableWidgetGroup(4, 18, 272, 95)
        .setBackground(GuiTextures.DISPLAY)
    private val receiverScroll = DraggableScrollableWidgetGroup(4, 131, 272, 87)
        .setBackground(GuiTextures.DISPLAY)
    private val providerRows = mutableListOf<CloudMonitorRowWidget>()
    private val receiverRows = mutableListOf<CloudMonitorRowWidget>()

    private var topologyVersion = Long.MIN_VALUE
    private var tick = 0

    init {
        addWidget(
            ExtendLabelWidget(6, 4, Component.translatable("gui.gtladditions.cloud_monitor.providers"))
        )
        providerScroll.setYScrollBarWidth(4)
            .setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(1.0f))
        addWidget(providerScroll)

        addWidget(
            ExtendLabelWidget(6, 117, Component.translatable("gui.gtladditions.cloud_monitor.requesters"))
        )
        receiverScroll.setYScrollBarWidth(4)
            .setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(1.0f))
        addWidget(receiverScroll)
    }

    override fun writeInitialData(buffer: FriendlyByteBuf) {
        super.writeInitialData(buffer)
        val snapshot = CloudNetworkManager.getComputationTopologySnapshot(teamId)
        topologyVersion = snapshot.topologyVersion
        writeTopologySnapshot(buffer, snapshot)
    }

    override fun readInitialData(buffer: FriendlyByteBuf) {
        super.readInitialData(buffer)
        rebuildRows(readTopologySnapshot(buffer))
    }

    override fun detectAndSendChanges() {
        super.detectAndSendChanges()
        if (tick++ % 20 != 0) return

        val currentTopologyVersion = CloudNetworkManager.getTopologyVersion()
        if (currentTopologyVersion != topologyVersion) {
            val snapshot = CloudNetworkManager.getComputationTopologySnapshot(teamId)
            topologyVersion = snapshot.topologyVersion
            writeUpdateInfo(UPDATE_FULL_TOPOLOGY) { writeTopologySnapshot(it, snapshot) }
        } else {
            val snapshot = CloudNetworkManager.getComputationLiveSnapshot(teamId)
            writeUpdateInfo(UPDATE_LIVE_VALUES) { writeLiveValues(it, snapshot) }
        }
    }

    override fun readUpdateInfo(id: Int, buffer: FriendlyByteBuf) {
        when (id) {
            UPDATE_FULL_TOPOLOGY -> rebuildRows(readTopologySnapshot(buffer))
            UPDATE_LIVE_VALUES -> readLiveValues(buffer)
            else -> super.readUpdateInfo(id, buffer)
        }
    }

    private fun rebuildRows(snapshot: ComputationTopologySnapshot) {
        topologyVersion = snapshot.topologyVersion
        providerScroll.clearAllWidgets()
        receiverScroll.clearAllWidgets()
        providerRows.clear()
        receiverRows.clear()

        var y = 0
        for (provider in snapshot.providers) {
            val row = CloudMonitorRowWidget(y, true, CloudMonitorRowWidget.Kind.MACHINE, provider, 0)
            addClientRow(providerScroll, row)
            providerRows += row
            y += 20
        }
        if (providerRows.isEmpty()) {
            addClientRow(
                providerScroll,
                CloudMonitorRowWidget(y, true, CloudMonitorRowWidget.Kind.NO_ENTRIES, null, 0)
            )
            y += 28
        }
        if (snapshot.otherProviderCount > 0) {
            addClientRow(
                providerScroll,
                CloudMonitorRowWidget(
                    y,
                    true,
                    CloudMonitorRowWidget.Kind.OTHER_TEAM,
                    null,
                    snapshot.otherProviderCount
                )
            )
        }

        y = 0
        for (receiver in snapshot.receivers) {
            val row = CloudMonitorRowWidget(y, false, CloudMonitorRowWidget.Kind.MACHINE, receiver, 0)
            addClientRow(receiverScroll, row)
            receiverRows += row
            y += 20
        }
        if (receiverRows.isEmpty()) {
            addClientRow(
                receiverScroll,
                CloudMonitorRowWidget(y, false, CloudMonitorRowWidget.Kind.NO_ENTRIES, null, 0)
            )
            y += 28
        }
        if (snapshot.otherReceiverCount > 0) {
            addClientRow(
                receiverScroll,
                CloudMonitorRowWidget(
                    y,
                    false,
                    CloudMonitorRowWidget.Kind.OTHER_TEAM,
                    null,
                    snapshot.otherReceiverCount
                )
            )
        }
    }

    private fun addClientRow(scroll: DraggableScrollableWidgetGroup, row: CloudMonitorRowWidget) {
        row.setClientSideWidget()
        scroll.addWidget(row)
    }

    private fun readLiveValues(buffer: FriendlyByteBuf) {
        val providerCount = buffer.readInt()
        val current = LongArray(providerCount)
        val max = LongArray(providerCount)
        repeat(providerCount) { index ->
            current[index] = buffer.readLong()
            max[index] = buffer.readLong()
        }
        val receiverCount = buffer.readInt()
        val requested = IntArray(receiverCount) { buffer.readInt() }

        if (providerCount == providerRows.size) {
            repeat(providerCount) { providerRows[it].setProviderValues(current[it], max[it]) }
        }
        if (receiverCount == receiverRows.size) {
            repeat(receiverCount) { receiverRows[it].setRequestedCwu(requested[it]) }
        }
    }
}

private class CloudMonitorRowWidget(
    y: Int,
    private val provider: Boolean,
    private val kind: Kind,
    machine: CloudMachineSnapshot?,
    private val otherCount: Int
) : WidgetGroup(4, y + 4, 260, 18) {

    enum class Kind {
        MACHINE,
        NO_ENTRIES,
        OTHER_TEAM
    }

    private val dimensionId = machine?.dimensionId.orEmpty()
    private val pos = machine?.pos
    private val frontPos = machine?.frontPos
    private val item = machine?.item ?: ItemStack.EMPTY

    private var current = machine?.currentCwu ?: 0L
    private var max = machine?.maxCwu ?: 0L
    private var cwu = machine?.requestedCwu ?: 0

    init {
        val icon = ImageWidget(0, 0, 18, 18) { ItemStackTexture(item) }
        if (kind == Kind.MACHINE) icon.setHoverTooltips(item.hoverName)
        val label = ComponentPanelWidget(24, 4, ::buildText).setMaxWidthLimit(172)
        label.setClientSideWidget()
        val button = createHighlightButton()
        if (kind != Kind.MACHINE) {
            button.setActive(false)
            button.setVisible(false)
            setSizeHeight(26)
        } else {
            refreshButtonTooltip(button)
        }
        addWidget(icon)
        addWidget(label)
        addWidget(button)
    }

    private fun createHighlightButton(): ButtonWidget = object : ButtonWidget(
        200,
        2,
        56,
        14,
        TextTexture(
            Component.translatable("gui.gtladditions.cloud_monitor.highlight").string,
            16777045
        ),
        {
            if (pos != null && dimensionId.isNotEmpty()) {
                ClientCloudHighlighter.highlight(pos, dimensionId)
            }
        }
    ) {

        @OnlyIn(Dist.CLIENT)
        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            if (!isMouseOverElement(mouseX, mouseY)) return false
            onPressCallback?.accept(ClickData())
            val minecraft = Minecraft.getInstance()
            val level = minecraft.level
            if (level != null && pos != null) {
                if (minecraft.screen != null) minecraft.setScreen(null)
                val player = gui.entityPlayer
                if (player != null) {
                    if (dimensionId == level.dimension().location().toString()) {
                        player.lookAt(
                            EntityAnchorArgument.Anchor.EYES,
                            Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
                        )
                    } else if (dimensionId.isNotEmpty()) {
                        sendCrossDimensionMessage(player)
                    }
                }
            }
            playButtonClickSound()
            return true
        }
    }

    private fun buildText(list: MutableList<Component>) {
        when (kind) {
            Kind.MACHINE -> {
                if (provider) {
                    list.add(
                        Component.translatable(
                            "gui.gtladditions.cloud_monitor.provider_info",
                            FormattingUtil.formatNumbers(current),
                            FormattingUtil.formatNumbers(max)
                        )
                    )
                } else {
                    list.add(
                        Component.translatable(
                            "gui.gtladditions.cloud_monitor.requester_info",
                            FormattingUtil.formatNumbers(cwu)
                        )
                    )
                }
            }
            Kind.NO_ENTRIES -> list.add(
                Component.translatable(
                    if (provider) {
                        "gui.gtladditions.cloud_monitor.no_providers"
                    } else {
                        "gui.gtladditions.cloud_monitor.no_requesters"
                    }
                ).withStyle(ChatFormatting.GRAY)
            )
            Kind.OTHER_TEAM -> list.add(
                Component.translatable(
                    if (provider) {
                        "gui.gtladditions.cloud_monitor.other_team_providers"
                    } else {
                        "gui.gtladditions.cloud_monitor.other_team_receivers"
                    },
                    otherCount
                ).withStyle(ChatFormatting.YELLOW)
            )
        }
    }

    fun setProviderValues(current: Long, max: Long) {
        this.current = current
        this.max = max
    }

    fun setRequestedCwu(cwu: Int) {
        this.cwu = cwu
    }

    @OnlyIn(Dist.CLIENT)
    private fun sendCrossDimensionMessage(player: Player) {
        val targetPos = frontPos ?: pos ?: return
        val sourcePos = pos ?: return
        val command = "/execute in $dimensionId run tp @s ${targetPos.x + 0.5} ${targetPos.y} ${targetPos.z + 0.5}"
        val coordinates = Component.literal("[${sourcePos.x}, ${sourcePos.y}, ${sourcePos.z}]")
            .withStyle { style ->
                style.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                    .withUnderlined(true)
                    .withColor(ChatFormatting.GREEN)
            }
        player.displayClientMessage(
            Component.translatable(
                "gui.gtladditions.cloud_monitor.cross_dim",
                Component.literal("[$dimensionId]")
                    .withStyle { style -> style.withColor(ChatFormatting.GREEN) },
                coordinates
            ),
            false
        )
    }

    private fun refreshButtonTooltip(button: ButtonWidget) {
        val sourcePos = pos ?: return
        button.setHoverTooltips(
            Component.translatable("gui.gtladditions.cloud_monitor.tooltip_dim", dimensionId),
            Component.translatable(
                "gui.gtladditions.cloud_monitor.tooltip_pos",
                sourcePos.x,
                sourcePos.y,
                sourcePos.z
            )
        )
    }
}