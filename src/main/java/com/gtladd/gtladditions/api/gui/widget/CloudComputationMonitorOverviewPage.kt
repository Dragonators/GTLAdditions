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
import com.lowdragmc.lowdraglib.LDLib
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
) : WidgetGroup(0, 0, 280, calculateFittedHeight()) {

    companion object {

        private const val UPDATE_FULL_TOPOLOGY = 0
        private const val UPDATE_LIVE_VALUES = 1

        private fun calculateFittedHeight(): Int {
            if (!LDLib.isRemote()) return 150
            return maxOf(150, Minecraft.getInstance().window.guiScaledHeight - 126)
        }

        private fun writeTopologySnapshot(
            buffer: FriendlyByteBuf,
            snapshot: ComputationTopologySnapshot
        ) {
            buffer.writeLong(snapshot.topologyVersion)
            buffer.writeInt(snapshot.providers.size)
            snapshot.providers.forEach { writeMachineSnapshot(buffer, it) }
            buffer.writeInt(snapshot.receivers.size)
            snapshot.receivers.forEach { writeMachineSnapshot(buffer, it) }
            buffer.writeInt(snapshot.unboundProviders.size)
            snapshot.unboundProviders.forEach { writeMachineSnapshot(buffer, it) }
            buffer.writeInt(snapshot.unboundReceivers.size)
            snapshot.unboundReceivers.forEach { writeMachineSnapshot(buffer, it) }
        }

        private fun readTopologySnapshot(buffer: FriendlyByteBuf): ComputationTopologySnapshot {
            val version = buffer.readLong()
            val providers = List(buffer.readInt()) { readMachineSnapshot(buffer) }
            val receivers = List(buffer.readInt()) { readMachineSnapshot(buffer) }
            val unboundProviders = List(buffer.readInt()) { readMachineSnapshot(buffer) }
            val unboundReceivers = List(buffer.readInt()) { readMachineSnapshot(buffer) }
            return ComputationTopologySnapshot(
                version,
                providers,
                receivers,
                unboundProviders,
                unboundReceivers
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

    private val scrollHeight = (size.height - 36) / 2
    private val providerScroll = DraggableScrollableWidgetGroup(4, 18, 272, scrollHeight)
        .setBackground(GuiTextures.DISPLAY)
    private val receiverScroll = DraggableScrollableWidgetGroup(
        4,
        36 + scrollHeight,
        272,
        size.height - 36 - scrollHeight
    )
        .setBackground(GuiTextures.DISPLAY)
    private val providerRows = mutableListOf<CloudMonitorRowWidget>()
    private val receiverRows = mutableListOf<CloudMonitorRowWidget>()

    private var topologyVersion = Long.MIN_VALUE
    private var tick = 0
    private var providerSortDescending = true
    private var receiverSortDescending = true

    init {
        addWidget(
            ExtendLabelWidget(6, 4, Component.translatable("gui.gtladditions.cloud_monitor.providers"))
        )
        providerScroll.setYScrollBarWidth(4)
            .setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(1.0f))
        addWidget(providerScroll)
        addWidget(
            createSortButton(
                3,
                { providerSortDescending },
                {
                    providerSortDescending = !providerSortDescending
                    applySort(providerScroll, providerRows, true, providerSortDescending)
                }
            )
        )

        addWidget(
            ExtendLabelWidget(
                6,
                22 + scrollHeight,
                Component.translatable("gui.gtladditions.cloud_monitor.requesters")
            )
        )
        receiverScroll.setYScrollBarWidth(4)
            .setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(1.0f))
        addWidget(receiverScroll)
        addWidget(
            createSortButton(
                21 + scrollHeight,
                { receiverSortDescending },
                {
                    receiverSortDescending = !receiverSortDescending
                    applySort(receiverScroll, receiverRows, false, receiverSortDescending)
                }
            )
        )
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
            val row = CloudMonitorRowWidget(y, true, CloudMonitorRowWidget.Kind.MACHINE, provider, emptyList())
            addClientRow(providerScroll, row)
            providerRows += row
            y += 20
        }
        if (providerRows.isEmpty()) {
            addClientRow(
                providerScroll,
                CloudMonitorRowWidget(y, true, CloudMonitorRowWidget.Kind.NO_ENTRIES, null, emptyList())
            )
            y += 28
        }
        if (snapshot.unboundProviders.isNotEmpty()) {
            addClientRow(
                providerScroll,
                CloudMonitorRowWidget(
                    y,
                    true,
                    CloudMonitorRowWidget.Kind.UNBOUND,
                    null,
                    snapshot.unboundProviders
                )
            )
        }

        y = 0
        for (receiver in snapshot.receivers) {
            val row = CloudMonitorRowWidget(y, false, CloudMonitorRowWidget.Kind.MACHINE, receiver, emptyList())
            addClientRow(receiverScroll, row)
            receiverRows += row
            y += 20
        }
        if (receiverRows.isEmpty()) {
            addClientRow(
                receiverScroll,
                CloudMonitorRowWidget(y, false, CloudMonitorRowWidget.Kind.NO_ENTRIES, null, emptyList())
            )
            y += 28
        }
        if (snapshot.unboundReceivers.isNotEmpty()) {
            addClientRow(
                receiverScroll,
                CloudMonitorRowWidget(
                    y,
                    false,
                    CloudMonitorRowWidget.Kind.UNBOUND,
                    null,
                    snapshot.unboundReceivers
                )
            )
        }

        applySort(providerScroll, providerRows, true, providerSortDescending)
        applySort(receiverScroll, receiverRows, false, receiverSortDescending)
    }

    private fun createSortButton(
        y: Int,
        descending: () -> Boolean,
        onPress: () -> Unit
    ): ButtonWidget = ButtonWidget(
        234,
        y,
        42,
        13,
        TextTexture {
            Component.translatable("gui.gtladditions.cloud_monitor.sort_quantity_short").string +
                if (descending()) "▼" else "▲"
        }.setColor(16777045),
        { onPress() }
    ).apply {
        setClientSideWidget()
        setHoverTooltips(Component.translatable("gui.gtladditions.cloud_monitor.sort"))
    }

    private fun applySort(
        scroll: DraggableScrollableWidgetGroup,
        rows: List<CloudMonitorRowWidget>,
        byMax: Boolean,
        descending: Boolean
    ) {
        scroll.setScrollYOffset(0)
        val orderedRows = if (descending) {
            rows.sortedByDescending { it.getSortValue(byMax) }
        } else {
            rows.sortedBy { it.getSortValue(byMax) }
        }
        orderedRows.forEachIndexed { index, row -> row.selfPositionY = index * 20 + 4 }
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
    unboundMachines: List<CloudMachineSnapshot>
) : WidgetGroup(4, y + 4, 260, 18) {

    enum class Kind {
        MACHINE,
        NO_ENTRIES,
        UNBOUND
    }

    private val locations = when (kind) {
        Kind.MACHINE -> listOfNotNull(machine)
        Kind.UNBOUND -> unboundMachines.toList()
        Kind.NO_ENTRIES -> emptyList()
    }
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
        if (kind == Kind.MACHINE) {
            refreshButtonTooltip(button)
        } else {
            sizeHeight = 26
            label.setSelfPosition(0, 4)
            if (kind == Kind.UNBOUND) {
                label.setMaxWidthLimit(196)
                button.setHoverTooltips(Component.translatable("gui.gtladditions.cloud_monitor.highlight_all"))
            } else {
                label.setMaxWidthLimit(252)
                button.isActive = false
                button.isVisible = false
            }
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
            for (location in locations) {
                if (location.dimensionId.isNotEmpty()) {
                    ClientCloudHighlighter.highlight(location.pos, location.dimensionId)
                }
            }
        }
    ) {

        @OnlyIn(Dist.CLIENT)
        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            if (!isMouseOverElement(mouseX, mouseY)) return false
            onPressCallback?.accept(ClickData())
            val minecraft = Minecraft.getInstance()
            val level = minecraft.level
            val player = gui.entityPlayer
            if (level != null && player != null) {
                if (minecraft.screen != null) minecraft.setScreen(null)
                if (kind == Kind.UNBOUND) {
                    sendLocationMessages(player, locations)
                } else {
                    val location = locations.firstOrNull()
                    if (location != null && location.dimensionId == level.dimension().location().toString()) {
                        player.lookAt(
                            EntityAnchorArgument.Anchor.EYES,
                            Vec3(location.pos.x + 0.5, location.pos.y + 0.5, location.pos.z + 0.5)
                        )
                    } else if (location != null && location.dimensionId.isNotEmpty()) {
                        sendLocationMessages(player, listOf(location))
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
            Kind.UNBOUND -> list.add(
                Component.translatable(
                    if (provider) {
                        "gui.gtladditions.cloud_monitor.unbound_providers"
                    } else {
                        "gui.gtladditions.cloud_monitor.unbound_receivers"
                    },
                    locations.size
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

    fun getSortValue(byMax: Boolean): Long = if (byMax) max else cwu.toLong()

    @OnlyIn(Dist.CLIENT)
    private fun sendLocationMessages(player: Player, locations: List<CloudMachineSnapshot>) {
        val canTeleport = player.hasPermissions(2)
        for (location in locations) {
            val targetPos = location.frontPos ?: location.pos
            val command =
                "/execute in ${location.dimensionId} run tp @s ${targetPos.x + 0.5} ${targetPos.y} ${targetPos.z + 0.5}"
            val coordinates = Component.literal("[${location.pos.x}, ${location.pos.y}, ${location.pos.z}]")
            if (canTeleport) {
                coordinates.withStyle { style ->
                    style.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withUnderlined(true)
                        .withColor(ChatFormatting.GREEN)
                }
            } else {
                coordinates.withStyle(ChatFormatting.GREEN)
            }
            player.displayClientMessage(
                Component.translatable(
                    "gui.gtladditions.cloud_monitor.cross_dim",
                    Component.literal("[${location.dimensionId}]")
                        .withStyle { style -> style.withColor(ChatFormatting.GREEN) },
                    coordinates
                ),
                false
            )
        }
    }

    private fun refreshButtonTooltip(button: ButtonWidget) {
        val location = locations.firstOrNull() ?: return
        button.setHoverTooltips(
            Component.translatable("gui.gtladditions.cloud_monitor.tooltip_dim", location.dimensionId),
            Component.translatable(
                "gui.gtladditions.cloud_monitor.tooltip_pos",
                location.pos.x,
                location.pos.y,
                location.pos.z
            )
        )
    }
}