package com.gtladd.gtladditions.api.machine.gui

import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.registry.GTRegistries
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.client.gui.WirelessMonitorPainter
import com.gtladd.gtladditions.mixin.gtmtings.WirelessEnergyMonitorAccessor
import com.gtladd.gtladditions.utils.CommonUtils
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.hepdd.gtmthings.api.misc.GlobalVariableStorage
import com.hepdd.gtmthings.api.misc.WirelessEnergyManager
import com.hepdd.gtmthings.utils.TeamUtil
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import io.netty.buffer.Unpooled
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import net.minecraft.ChatFormatting
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.client.gtmt.WirelessEnergyLocatorClient
import org.gtlcore.gtlcore.integration.gtmt.WirelessEnergyLocator
import org.gtlcore.gtlcore.integration.gtmt.monitor.EnergyHistory
import org.gtlcore.gtlcore.integration.gtmt.monitor.WirelessEnergyHistoryData
import org.gtlcore.gtlcore.integration.gtmt.monitor.WirelessEnergyMachineStats
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt

class WirelessEnergyMonitorPage(private val monitor: MetaMachine, private val user: UUID?) : WidgetGroup(0, 0, WIDTH, HEIGHT) {

    // View state: chosen on the client, mirrored to the server through client actions.
    private var all = false
    private var filter = 0
    private var range = 1

    // Server: the value last sent; client: the value last received.
    private var stored: BigInteger = BigInteger.ZERO

    // Server only.
    private var typeFilter: IntOpenHashSet? = null
    private var first = 0
    private var ownerSent = false
    private var storedAt = -5L
    private var sample = Long.MIN_VALUE
    private var trendDirty = true
    private var rowsDirty = true
    private var rowsSent: ByteArray? = null
    private val typeIds = Object2IntOpenHashMap<String>().apply { defaultReturnValue(-1) }
    private val typeKeys = ArrayList<String>()
    private val dimensionIds = Object2IntOpenHashMap<String>().apply { defaultReturnValue(-1) }
    private val dimensionKeys = ArrayList<String>()
    private var typesSent = 0
    private var dimensionsSent = 0

    // Client only.
    private var received = false
    private var owner: Component? = null
    private var rate: BigDecimal? = null
    private var average: BigDecimal? = null
    private val types = ArrayList<TypeView>()
    private val dimensions = ArrayList<String>()
    private var generation: BigDecimal = BigDecimal.ZERO
    private var consumption: BigDecimal = BigDecimal.ZERO
    private var generators = 0
    private var consumers = 0
    private var matched = 0
    private var windowStart = 0
    private var windowRows = emptyList<RowView>()
    private var requested = 0
    private var query = ""
    private var queryTypes: IntArrayList? = null
    private var scroll = 0f
    private var scrollTarget = 0f
    private var lastFrame = 0L
    private var dragging = false
    private val hots = ArrayList<Hot>()
    private val search = SearchField(LIST_X + 13, SEARCH_Y, 136, 12) { onQuery(it) }

    // Client only: trend chart. 16-bit levels: means over chartLow + chartSpread, swings (tooltip only) over chartSwingMax.
    private var chartRange = -1
    private var chartFirst = 0L
    private var chartLow: BigInteger = BigInteger.ZERO
    private var chartSpread: BigInteger = BigInteger.ZERO
    private var chartMean = IntArray(0)
    private var chartSwingMax: BigInteger = BigInteger.ZERO
    private var chartSwing = IntArray(0)
    private var trendTick = Long.MIN_VALUE
    private var trendMillis = 0L
    private var trendInterval = 1.0
    private var viewTick = Double.NEGATIVE_INFINITY
    private var viewSpeed = 0.0
    private var shownLow = 0.0
    private var shownHigh = 0.0
    private var rescale = true
    private var sampleX = FloatArray(0)
    private var sampleMean = FloatArray(0)

    init {
        addWidget(search.setClientSideWidget())
    }

    // ========================================
    // CLIENT ACTIONS
    // ========================================

    private fun setScope(value: Boolean) {
        if (value == all) return
        all = value
        resetList()
        writeClientAction(SCOPE) { it.writeBoolean(value) }
    }

    private fun setFilter(value: Int) {
        if (value == filter) return
        filter = value
        resetList()
        writeClientAction(FILTER) { it.writeVarInt(value) }
    }

    private fun setRange(value: Int) {
        if (value == range) return
        range = value
        writeClientAction(RANGE) { it.writeVarInt(value) }
    }

    private fun onQuery(text: String) {
        query = text
        updateQueryTypes()
    }

    private fun updateQueryTypes() {
        val q = query.trim().lowercase()
        val next = if (q.isEmpty()) {
            null
        } else {
            IntArrayList().apply { types.forEachIndexed { i, t -> if (q in t.search) add(i) } }
        }
        if (next == queryTypes) return

        queryTypes = next
        resetList()
        writeClientAction(TYPES) { buf -> buf.writeNullable(next) { b, list -> b.writeIntIdList(list) } }
    }

    private fun resetList() {
        scroll = 0f
        scrollTarget = 0f
        requested = 0
    }

    override fun handleClientAction(id: Int, buf: FriendlyByteBuf) {
        when (id) {
            SCOPE -> all = buf.readBoolean()
            FILTER -> filter = buf.readVarInt().coerceIn(0, 2)
            TYPES -> typeFilter = buf.readNullable { IntOpenHashSet(it.readIntIdList()) }
            FIRST -> first = buf.readVarInt().coerceAtLeast(0)
            RANGE -> range = buf.readVarInt().coerceIn(0, EnergyHistory.WINDOWS.size - 1)
            else -> return super.handleClientAction(id, buf)
        }

        if (id == RANGE) {
            trendDirty = true
        } else {
            if (id != FIRST) first = 0
            rowsDirty = true
        }
    }

    // ========================================
    // NETWORK SYNCHRONIZATION
    // ========================================

    /**
     * Synchronizes the bound network and the machine statistics to the client.
     *
     * - The stored energy goes out when it changes, at most every 5 ticks.
     * - The trend is rebuilt and sent once per GTLCore publish or on a range change.
     * - The rows are rebuilt at the same moments or on a view change, and sent only when their bytes differ.
     * - Rows go out as a window around the client's scroll position; the type and dimension tables grow incrementally.
     */
    override fun detectAndSendChanges() {
        super.detectAndSendChanges()

        val user = user ?: return
        val level = monitor.level as? ServerLevel ?: return
        val server = level.server
        val now = server.overworld().gameTime
        val account = TeamUtil.getTeamUUID(user)

        if (!ownerSent) {
            ownerSent = true
            writeUpdateInfo(OWNER) { it.writeComponent(TeamUtil.GetName(level, user)) }
        }

        val energy = WirelessEnergyManager.getUserEU(user)
        if (energy != stored && now - storedAt >= 5L) {
            stored = energy
            storedAt = now
            writeUpdateInfo(STORED) { it.writeByteArray(energy.toByteArray()) }
        }

        // GTLCore samples and publishes at the END of every 20th overworld tick, after menus broadcast.
        val latest = now - 1 - Math.floorMod(now - 1, SECOND)
        if (latest != sample) {
            sample = latest
            trendDirty = true
            rowsDirty = true
        }

        if (trendDirty) sendTrend(WirelessEnergyHistoryData.get(server).track(account))
        if (rowsDirty) sendRows(server, account)
    }

    /**
     * Sends the window as buckets on the absolute tick grid, one publish step wider on the left for the client's scroll.
     *
     * - A bucket's edges are multiples of its width in game time, so a closed bucket never changes between publishes.
     * - Only buckets whose last 20-tick sample is in are sent: a partly filled one would swing with the phase of a pulsing load.
     * - Short windows read the 20-tick samples by exact key; long windows aggregate GTLCore's saved records.
     */
    private fun sendTrend(history: EnergyHistory) {
        trendDirty = false
        val window = EnergyHistory.WINDOWS[range]
        val width = BUCKET_TICKS[range]
        val from = sample - window - SECOND
        val first = Math.floorDiv(from, width)
        val end = Math.floorDiv(sample + SECOND, width) * width
        val buckets = Buckets(first, width, (end / width - first).toInt())

        if (window <= EnergyHistory.WINDOWS[1]) {
            // GTLCore keeps every 20-tick sample for WINDOWS[1] + 20 ticks, so each query hits a stored key.
            for (tick in from until end step SECOND) {
                val energy = history.energyAt(tick) ?: continue
                buckets.add(buckets.index(tick), energy)
            }
        } else {
            var last: EnergyHistory.Point? = null
            for (point in history.savedPoints()) {
                // Also stops at the sample GTLCore takes for the next publish, which a menu can see between ticks.
                if (point.tick() >= end) break
                if (point.tick() >= first * width) {
                    last?.let { buckets.bridge(it, point) }
                    buckets.add(buckets.index(point.tick()), point.energy())
                }
                last = point
            }
        }

        writeUpdateInfo(TREND) { buf ->
            buf.writeNullable(history.average(sample, SECOND), ::writeDecimal)
            buf.writeNullable(history.average(sample, window), ::writeDecimal)
            buf.writeVarInt(range)
            buf.writeVarLong(sample)
            buckets.write(buf, width > SECOND)
        }
    }

    private fun sendRows(server: MinecraftServer, account: UUID) {
        rowsDirty = false
        val matches = ArrayList<Entry>()
        var generated = BigDecimal.ZERO
        var consumed = BigDecimal.ZERO
        var generatorCount = 0
        var consumerCount = 0

        fun visit(row: WirelessEnergyMachineStats.Row, own: Boolean) {
            val type = index((row.name().contents as TranslatableContents).key, typeIds, typeKeys)
            val positive = row.energy().signum() > 0
            if (positive) generated += row.power() else consumed += row.power()

            if (typeFilter?.contains(type) == false) return
            if (positive) generatorCount++ else consumerCount++
            if (filter == 1 && !positive || filter == 2 && positive) return
            matches += Entry(row, type, own)
        }

        if (all) {
            for (other in GlobalVariableStorage.GlobalEnergy.keys) {
                val own = other == account
                for (row in WirelessEnergyMachineStats.rows(server, other, 0)) visit(row, own)
            }
            matches.sortWith(ORDER)
        } else {
            for (row in WirelessEnergyMachineStats.rows(server, account, 0)) visit(row, true)
        }

        val start = first.coerceIn(0, max(0, matches.size - WINDOW_ROWS))
        val end = min(matches.size, start + WINDOW_ROWS)
        for (i in start until end) index(matches[i].row.dimension(), dimensionIds, dimensionKeys)

        rowsSent = sendIfChanged(ROWS, rowsSent) { buf ->
            buf.writeVarInt(typeKeys.size - typesSent)
            for (i in typesSent until typeKeys.size) buf.writeUtf(typeKeys[i])
            buf.writeVarInt(dimensionKeys.size - dimensionsSent)
            for (i in dimensionsSent until dimensionKeys.size) buf.writeUtf(dimensionKeys[i])

            writeDecimal(buf, generated)
            writeDecimal(buf, consumed)
            buf.writeVarInt(generatorCount)
            buf.writeVarInt(consumerCount)
            buf.writeVarInt(matches.size)
            buf.writeVarInt(start)
            buf.writeVarInt(end - start)
            for (i in start until end) {
                val entry = matches[i]
                buf.writeVarInt(entry.type)
                buf.writeVarInt(dimensionIds.getInt(entry.row.dimension()))
                buf.writeLong(entry.row.pos().asLong())
                writeDecimal(buf, entry.row.power())
                buf.writeBoolean(entry.own)
            }
        }

        // New table entries always change the bytes, so whatever was written has been sent.
        typesSent = typeKeys.size
        dimensionsSent = dimensionKeys.size
    }

    private fun sendIfChanged(id: Int, last: ByteArray?, write: (FriendlyByteBuf) -> Unit): ByteArray? {
        val buf = FriendlyByteBuf(Unpooled.buffer())
        write(buf)
        val bytes = ByteArray(buf.readableBytes())
        buf.readBytes(bytes)
        buf.release()

        if (bytes.contentEquals(last)) return last
        writeUpdateInfo(id) { it.writeBytes(bytes) }
        return bytes
    }

    @OnlyIn(Dist.CLIENT)
    override fun readUpdateInfo(id: Int, buffer: FriendlyByteBuf) {
        when (id) {
            OWNER -> owner = buffer.readComponent()
            STORED -> stored = BigInteger(buffer.readByteArray())
            TREND -> readTrend(buffer)
            ROWS -> readRows(buffer)
            else -> return super.readUpdateInfo(id, buffer)
        }
        received = true
    }

    @OnlyIn(Dist.CLIENT)
    private fun readTrend(buf: FriendlyByteBuf) {
        rate = buf.readNullable(::readDecimal)
        average = buf.readNullable(::readDecimal)
        val next = buf.readVarInt()
        follow(buf.readVarLong())

        val count = buf.readVarInt()
        val kept = next == chartRange && chartMean.isNotEmpty()
        val low = chartLow
        val spread = chartSpread
        chartRange = next
        if (count == 0) {
            chartMean = IntArray(0)
            return
        }

        chartFirst = buf.readVarLong()
        chartLow = BigInteger(buf.readByteArray())
        chartSpread = BigInteger(buf.readByteArray())
        chartMean = IntArray(count) { buf.readUnsignedShort() }
        chartSwingMax = if (BUCKET_TICKS[next] > SECOND) BigInteger(buf.readByteArray()) else BigInteger.ZERO
        chartSwing = if (chartSwingMax.signum() > 0) IntArray(count) { buf.readUnsignedShort() } else IntArray(0)
        if (sampleX.size < count * MAX_SUBDIVISIONS + 2) {
            sampleX = FloatArray(count * MAX_SUBDIVISIONS + 2)
            sampleMean = FloatArray(sampleX.size)
        }

        // Re-expresses the shown scale in this packet's levels, so it keeps easing across packets.
        if (kept && chartSpread.signum() > 0) {
            val to = BigDecimal(chartSpread)
            val shift = BigDecimal((low - chartLow) * BIG_LEVELS).divide(to, MathContext.DECIMAL64).toDouble()
            val scale = BigDecimal(spread).divide(to, MathContext.DECIMAL64).toDouble()
            shownLow = shift + shownLow * scale
            shownHigh = shift + shownHigh * scale
        } else {
            rescale = true
        }
    }

    /**
     * Times the view's right edge, which trails the newest sample by up to one publish step.
     *
     * - Each publish sets the speed that reaches the new sample when the next one is due, so the edge neither stalls nor drifts at any TPS.
     * - The first packet, a reconnect or a jump in server time places the view instead of sliding it there.
     */
    @OnlyIn(Dist.CLIENT)
    private fun follow(tick: Long) {
        if (tick == trendTick) return
        val now = Util.getMillis()
        if (tick < trendTick || tick - viewTick > 2 * SECOND) {
            viewTick = (tick - SECOND).toDouble()
        } else {
            trendInterval += ((now - trendMillis) / 1000.0 * SECOND / (tick - trendTick) - trendInterval) / 2
        }
        trendTick = tick
        trendMillis = now
        viewSpeed = (tick - viewTick) / trendInterval
    }

    @OnlyIn(Dist.CLIENT)
    private fun readRows(buf: FriendlyByteBuf) {
        val newTypes = buf.readVarInt()
        repeat(newTypes) {
            val key = buf.readUtf()
            val name = Component.translatable(key)
            val search = ChatFormatting.stripFormatting(name.string) + " " + key
            types += TypeView(name, machineIcons[key] ?: ItemStack.EMPTY, search.lowercase())
        }
        repeat(buf.readVarInt()) { dimensions += buf.readUtf() }

        generation = readDecimal(buf)
        consumption = readDecimal(buf)
        generators = buf.readVarInt()
        consumers = buf.readVarInt()
        matched = buf.readVarInt()
        windowStart = buf.readVarInt()
        windowRows = List(buf.readVarInt()) { readRow(buf) }

        if (newTypes > 0 && queryTypes != null) updateQueryTypes()
    }

    @OnlyIn(Dist.CLIENT)
    private fun readRow(buf: FriendlyByteBuf): RowView {
        val type = types[buf.readVarInt()]
        val dimension = dimensions[buf.readVarInt()]
        val pos = BlockPos.of(buf.readLong())
        val power = readDecimal(buf)
        val own = buf.readBoolean()

        val magnitude = power.abs()
        val positive = power.signum() >= 0
        val total = if (positive) generation else consumption.negate()
        val share = if (total.signum() > 0) magnitude.divide(total, MathContext.DECIMAL32).toFloat() else 0f

        return RowView(
            type,
            dimension,
            pos,
            power,
            own,
            (if (positive) "+" else "-") + CommonUtils.formatCompactBigDecimal(magnitude),
            "${pos.x}, ${pos.y}, ${pos.z} · " + ResourceLocation(dimension).path,
            CommonUtils.getEnergyTierComponent(magnitude.toBigInteger()),
            share.coerceIn(0f, 1f)
        )
    }

    // ========================================
    // INPUT
    // ========================================

    @OnlyIn(Dist.CLIENT)
    override fun mouseWheelMove(mouseX: Double, mouseY: Double, wheelDelta: Double): Boolean {
        if (overList(mouseX - position.x, mouseY - position.y) && maxScroll() > 0f) {
            scrollTarget = (scrollTarget - wheelDelta.toFloat() * ROW_H * 2).coerceIn(0f, maxScroll())
            return true
        }

        return super.mouseWheelMove(mouseX, mouseY, wheelDelta)
    }

    @OnlyIn(Dist.CLIENT)
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            val mx = (mouseX - position.x).toInt()
            val my = (mouseY - position.y).toInt()
            if (maxScroll() > 0f && over(mx, my, BAR_X - 1, LIST_Y, 5, LIST_H)) {
                dragging = true
                dragTo(my)
                return true
            }
            for (hot in hots) {
                if (!over(mx, my, hot.x, hot.y, hot.w, hot.h)) continue
                val action = hot.action ?: break
                action()
                playButtonClickSound()
                return true
            }
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    @OnlyIn(Dist.CLIENT)
    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        if (!dragging) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
        dragTo((mouseY - position.y).toInt())
        return true
    }

    @OnlyIn(Dist.CLIENT)
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!dragging) return super.mouseReleased(mouseX, mouseY, button)
        dragging = false
        return true
    }

    @OnlyIn(Dist.CLIENT)
    private fun dragTo(my: Int) {
        val max = maxScroll()
        if (max <= 0f) {
            dragging = false
            scrollTarget = 0f
            scroll = 0f
            return
        }

        val thumb = thumbHeight()
        val t = ((my - LIST_Y - thumb / 2f) / (LIST_H - thumb)).coerceIn(0f, 1f)
        scrollTarget = t * max
        scroll = scrollTarget
    }

    @OnlyIn(Dist.CLIENT)
    private fun locate(row: RowView) {
        val dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation(row.dimension))
        WirelessEnergyLocatorClient.highlight(WirelessEnergyLocator.Target(dimension, row.pos))

        val player = Minecraft.getInstance().player ?: return
        player.displayClientMessage(Component.literal(row.dimension + " [" + row.pos.toShortString() + "]"), true)
        player.closeContainer()
    }

    // ========================================
    // RENDERING
    // ========================================

    @OnlyIn(Dist.CLIENT)
    override fun drawInBackground(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val p = WirelessMonitorPainter
        val ox = position.x
        val oy = position.y
        val mx = mouseX - ox
        val my = mouseY - oy

        hots.clear()
        val dt = frameDelta()
        animateScroll(dt)
        animateTrend(dt)

        g.pose().pushPose()
        g.pose().translate(ox.toFloat(), oy.toFloat(), 0f)
        p.card(g, 0, 0, WIDTH, HEIGHT, 6, p.PAGE, p.PAGE_STROKE)
        p.card(g, 10, 26, 156, 184, 4, p.CARD, p.CARD_STROKE)
        p.card(g, 172, 26, 160, 184, 4, p.CARD, p.CARD_STROKE)
        if (received) {
            drawHeader(g, mx, my)
            drawNetwork(g)
            drawTrend(g, mx, my, ox, oy)
            drawFilters(g, mx, my, ox, oy)
            drawSearch(g)
            drawRows(g, mx, my, ox, oy)
        }
        g.pose().popPose()

        super.drawInBackground(g, mouseX, mouseY, partialTicks)
    }

    @OnlyIn(Dist.CLIENT)
    override fun drawInForeground(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawInForeground(g, mouseX, mouseY, partialTicks)

        val mx = mouseX - position.x
        val my = mouseY - position.y
        val lines = hots.firstOrNull { it.tooltip != null && over(mx, my, it.x, it.y, it.w, it.h) }?.tooltip?.invoke()
            ?: chartTooltip(mx, my)
            ?: return
        gui.modularUIGui.setHoverTooltip(lines, ItemStack.EMPTY, null, null)
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawHeader(g: GuiGraphics, mx: Int, my: Int) {
        val p = WirelessMonitorPainter
        owner?.let { name ->
            p.round(g, 10, 10, 6, 6, 3, p.ACCENT)
            p.fit(g, name, 20, 9, 200, p.TEXT)
        }

        val scopes = listOf(label("scope.team"), label("scope.all"))
        p.segments(g, SCOPE_X, 7, 80, 13, scopes, if (all) 1 else 0, hoveredSegment(mx, my, SCOPE_X, 7, 80, 13, 2))
        segmentHots(SCOPE_X, 7, 80, 13, 2) { setScope(it == 1) }
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawNetwork(g: GuiGraphics) {
        val p = WirelessMonitorPainter
        val r = rate

        p.text(g, label("stored"), 16, 32, p.LABEL)
        p.fitRight(g, Component.literal(CommonUtils.formatCompactBigInteger(stored) + " EU"), 160, 32, 110, p.TEXT)
        hots += Hot(15, 30, 146, 11, null, tooltip = { listOf(Component.literal(FormattingUtil.formatNumbers(stored) + " EU")) })

        p.text(g, label("net"), 16, 44, p.LABEL)
        if (r == null) {
            p.fitRight(g, label("collecting"), 160, 44, 110, p.MUTED)
        } else {
            p.fitRight(g, Component.literal(signed(r) + " EU/t"), 160, 44, 110, rateColor(r))
            p.fitRight(g, CommonUtils.getEnergyTierComponent(r.abs().toBigInteger()), 160, 56, 140, p.LABEL)
            hots += Hot(15, 42, 146, 23, null, tooltip = {
                listOf(
                    Component.literal(exact(r) + " EU/t").withStyle { it.withColor(rateColor(r) and 0xFFFFFF) },
                    CommonUtils.getEnergyTierComponent(r.abs().toBigInteger()),
                    label("net_window").withStyle(ChatFormatting.DARK_GRAY)
                )
            })
        }

        p.text(g, label("outlook"), 16, 68, p.LABEL)
        val (outlook, color) = outlook(r)
        p.fitRight(g, outlook, 160, 68, 110, color)
        p.rect(g, 15f, 81f, 146f, p.hairline(), p.CARD_STROKE)
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawTrend(g: GuiGraphics, mx: Int, my: Int, ox: Int, oy: Int) {
        val p = WirelessMonitorPainter
        val windows = EnergyHistory.WINDOWS.indices.map { label("window.$it") }
        p.segments(g, 15, 86, 146, 12, windows, range, hoveredSegment(mx, my, 15, 86, 146, 12, windows.size))
        segmentHots(15, 86, 146, 12, windows.size) { setRange(it) }

        p.round(g, CHART_X, CHART_Y, CHART_W, CHART_H, 3, p.CHART)
        for (i in 1..3) p.rect(g, CHART_X + 2f, (CHART_Y + CHART_H * i / 4).toFloat(), CHART_W - 4f, p.hairline(), 0x18FFFFFF)
        p.text(g, label("average"), 16, 194, p.LABEL)

        // Until the switched range arrives, the previous range's curve and average stay hidden.
        if (chartRange != range) return
        if (chartMean.isNotEmpty()) drawCurve(g, mx, my, ox, oy)

        val avg = average
        if (avg == null) {
            p.fitRight(g, label("collecting"), 160, 194, 110, p.MUTED)
        } else {
            p.fitRight(g, Component.literal(signed(avg) + " EU/t"), 160, 194, 110, rateColor(avg))
        }
    }

    /**
     * Draws the smoothed mean line over a fill fading to the floor; every range looks the same, swings only show in the tooltip.
     *
     * - Geometry is in sub-pixel GUI units, so edges are drawn at the screen's resolution instead of in GUI-pixel steps.
     * - The line is an opaque core with a one-pixel feather on each side, offset along the normal of the sampled curve.
     */
    @OnlyIn(Dist.CLIENT)
    private fun drawCurve(g: GuiGraphics, mx: Int, my: Int, ox: Int, oy: Int) {
        val p = WirelessMonitorPainter
        val count = sampleCurve()
        val px = p.pixel()
        val floor = CHART_Y + CHART_H - 1f
        g.enableScissor(ox + CHART_X + 1, oy + CHART_Y + 1, ox + CHART_X + CHART_W - 1, oy + CHART_Y + CHART_H - 1)
        val b = g.bufferSource().getBuffer(RenderType.gui())
        val m = g.pose().last().pose()

        for (j in 1 until count) {
            p.vertex(b, m, sampleX[j - 1], sampleMean[j - 1], fade(sampleMean[j - 1]))
            p.vertex(b, m, sampleX[j - 1], floor, fade(floor))
            p.vertex(b, m, sampleX[j], floor, fade(floor))
            p.vertex(b, m, sampleX[j], sampleMean[j], fade(sampleMean[j]))
        }

        // Normals average the neighbouring samples, so consecutive quads share their edges and the ribbon stays seamless.
        val core = LINE_CORE / 2
        val edge = core + px
        val clear = p.ACCENT and 0xFFFFFF
        var lastX = 0f
        var lastY = 0f
        var lastNx = 0f
        var lastNy = 0f
        for (j in 0 until count) {
            val dx = sampleX[min(j + 1, count - 1)] - sampleX[max(j - 1, 0)]
            val dy = sampleMean[min(j + 1, count - 1)] - sampleMean[max(j - 1, 0)]
            // Samples are strictly increasing in x, so the length only vanishes when there is a single sample, which draws no segment.
            val length = sqrt(dx * dx + dy * dy)
            val nx = if (length > 0f) dy / length else 0f
            val ny = if (length > 0f) -dx / length else -1f
            val x = sampleX[j]
            val y = sampleMean[j]
            if (j > 0) {
                p.vertex(b, m, lastX + lastNx * edge, lastY + lastNy * edge, clear)
                p.vertex(b, m, lastX + lastNx * core, lastY + lastNy * core, p.ACCENT)
                p.vertex(b, m, x + nx * core, y + ny * core, p.ACCENT)
                p.vertex(b, m, x + nx * edge, y + ny * edge, clear)

                p.vertex(b, m, lastX + lastNx * core, lastY + lastNy * core, p.ACCENT)
                p.vertex(b, m, lastX - lastNx * core, lastY - lastNy * core, p.ACCENT)
                p.vertex(b, m, x - nx * core, y - ny * core, p.ACCENT)
                p.vertex(b, m, x + nx * core, y + ny * core, p.ACCENT)

                p.vertex(b, m, lastX - lastNx * core, lastY - lastNy * core, p.ACCENT)
                p.vertex(b, m, lastX - lastNx * edge, lastY - lastNy * edge, clear)
                p.vertex(b, m, x - nx * edge, y - ny * edge, clear)
                p.vertex(b, m, x - nx * core, y - ny * core, p.ACCENT)
            }
            lastX = x
            lastY = y
            lastNx = nx
            lastNy = ny
        }

        val hovered = hoveredBucket(mx, my)
        if (hovered != null) {
            val x = vertexX(hovered.toDouble())
            val y = levelY(chartMean[hovered].toDouble())
            p.rect(b, m, x - px / 2, CHART_Y + 1f, px, CHART_H - 2f, 0x40FFFFFF)
            p.disc(b, m, x, y, 2.5f, p.CHART)
            p.disc(b, m, x, y, 2f, p.TEXT)
            p.disc(b, m, x, y, 1.1f, p.ACCENT)
        }
        g.flush()
        g.disableScissor()
    }

    /**
     * Samples the visible part of the curve into the sample arrays and returns how many were taken.
     *
     * - Between bucket vertices the curve is a monotone cubic: smooth, yet never beyond the values on either side.
     * - Steep intervals take more samples; the first bucket is held flat to its start, the last one for a bucket past its end.
     * - That hold reaches the right edge while the next bucket fills, at most about a pixel in every range.
     */
    @OnlyIn(Dist.CLIENT)
    private fun sampleCurve(): Int {
        val window = EnergyHistory.WINDOWS[chartRange]
        val width = BUCKET_TICKS[chartRange]
        val origin = (chartFirst * width + (width - SECOND) / 2).toDouble()
        val last = chartMean.size - 1
        val from = floor((viewTick - window - origin) / width).coerceIn(0.0, last.toDouble()).toInt()
        val to = ceil((viewTick - origin) / width).coerceIn(0.0, last.toDouble()).toInt()
        val step = INNER_W.toDouble() * width / window

        var k = 0
        if (from == 0 && width > SECOND) k = sample(k, -(width - SECOND) / 2.0 / width)
        k = sample(k, from.toDouble())
        for (i in from until to) {
            val rise = abs(levelY(chartMean[i + 1].toDouble()) - levelY(chartMean[i].toDouble()))
            val parts = ceil(max(step * 2, rise / 3.0)).toInt().coerceIn(1, MAX_SUBDIVISIONS)
            for (j in 1..parts) k = sample(k, i + j.toDouble() / parts)
        }
        if (to == last) k = sample(k, last + 1 + (width + SECOND) / 2.0 / width)
        return k
    }

    private fun sample(k: Int, u: Double): Int {
        sampleX[k] = vertexX(u)
        sampleMean[k] = levelY(hermite(chartMean, u))
        return k + 1
    }

    // Accent that fades by FADE_ALPHA from the chart's top down to AREA_ALPHA at its floor.
    private fun fade(y: Float): Int =
        WirelessMonitorPainter.alpha(WirelessMonitorPainter.ACCENT, AREA_ALPHA + FADE_ALPHA * (CHART_Y + CHART_H - 1 - y) / (CHART_H - 2))

    @OnlyIn(Dist.CLIENT)
    private fun drawFilters(g: GuiGraphics, mx: Int, my: Int, ox: Int, oy: Int) {
        val p = WirelessMonitorPainter
        val counts = intArrayOf(generators + consumers, generators, consumers)
        val labels = counts.indices.map { label("filter.$it").append(" " + FormattingUtil.formatNumbers(counts[it])) }
        p.segments(g, LIST_X, 31, 150, 13, labels, filter, hoveredSegment(mx, my, LIST_X, 31, 150, 13, labels.size))
        segmentHots(LIST_X, 31, 150, 13, labels.size) { setFilter(it) }

        val total = generation - consumption
        if (total.signum() > 0) {
            // Both colours clip one capsule on either side of the split, so they share its outline and meet at a hard seam;
            // the outer ends keep a pixel of room for the feather.
            val gw = (150 * generation.divide(total, MathContext.DECIMAL32).toFloat()).roundToInt()
            val split = ox + LIST_X + gw
            if (gw > 0) {
                g.enableScissor(ox + LIST_X - 1, oy + 46, if (gw < 150) split else ox + LIST_X + 151, oy + 51)
                p.capsule(g, LIST_X.toFloat(), 47f, 150f, 3f, p.GEN)
                g.disableScissor()
            }
            if (gw < 150) {
                g.enableScissor(if (gw > 0) split else ox + LIST_X - 1, oy + 46, ox + LIST_X + 151, oy + 51)
                p.capsule(g, LIST_X.toFloat(), 47f, 150f, 3f, p.CON)
                g.disableScissor()
            }
        } else {
            p.capsule(g, LIST_X.toFloat(), 47f, 150f, 3f, p.SUNKEN)
        }
        hots += Hot(LIST_X, 45, 150, 7, null, tooltip = {
            listOf(
                label("generation", "+" + FormattingUtil.formatNumbers(generation)).withStyle { it.withColor(p.GEN and 0xFFFFFF) },
                label("consumption", "-" + FormattingUtil.formatNumbers(consumption.negate())).withStyle { it.withColor(p.CON and 0xFFFFFF) }
            )
        })
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawSearch(g: GuiGraphics) {
        val p = WirelessMonitorPainter
        val focused = search.isFocus
        p.card(g, LIST_X, SEARCH_Y, 150, 13, 3, p.SUNKEN, p.SUNKEN_STROKE)
        // Fluent text box: a hairline bottom stroke over the box's own bottom edge, doubled in accent while focused.
        val underline = if (focused) 2 * p.hairline() else p.hairline()
        p.rect(g, LIST_X + 3f, SEARCH_Y + 13f - underline, 144f, underline, if (focused) p.ACCENT else 0xFF5A5A5A.toInt())
        p.searchIcon(g, LIST_X + 4, SEARCH_Y + 3, if (focused) p.ACCENT else p.LABEL)

        val room = 132
        val shown = if (p.font.width(query) <= room) query else p.font.plainSubstrByWidth(query.reversed(), room).reversed()
        val tw = p.text(g, shown, LIST_X + 14, p.ty(SEARCH_Y, 12), p.TEXT)
        if (focused && search.cursorAtEnd() && Util.getMillis() / 300L % 2L == 0L) {
            p.rect(g, LIST_X + 15f + tw, SEARCH_Y + 2f, p.hairline(), 8f, p.ACCENT)
        }
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawRows(g: GuiGraphics, mx: Int, my: Int, ox: Int, oy: Int) {
        val p = WirelessMonitorPainter
        val top = (scroll / ROW_H).toInt()
        val bottom = min(matched, top + VISIBLE)
        val right = LIST_X + ROW_W - 1
        requestWindow(top, bottom)

        g.enableScissor(ox + LIST_X, oy + LIST_Y, ox + LIST_X + ROW_W, oy + LIST_Y + LIST_H)
        val hoverIndex = if (overList(mx.toDouble(), my.toDouble())) ((my - LIST_Y + scroll) / ROW_H).toInt() else -1
        for (i in top until bottom) {
            val y = LIST_Y + (i * ROW_H - scroll).roundToInt()
            val row = windowRows.getOrNull(i - windowStart)
            if (row == null) {
                p.round(g, NAME_X, y + 3, 64, 4, 2, 0x14FFFFFF)
                p.round(g, NAME_X, y + 12, 40, 4, 2, 0x0CFFFFFF)
                continue
            }

            val color = if (row.power.signum() >= 0) p.GEN else p.CON
            if (i == hoverIndex) p.round(g, LIST_X, y, ROW_W, ROW_H, 3, p.ROW_HOVER)
            // Own machines in the server view: a short soft pill centred on the icon, kept a step clear of it.
            if (all && row.own) p.capsule(g, LIST_X + 0.5f, y + 4.5f, 1.25f, 9f, p.alpha(p.ACCENT, 0.7f))

            // Linear share with a 2 px floor, so machines that round to nothing still show they are counted.
            val track = (right - NAME_X + 1).toFloat()
            p.capsule(g, NAME_X.toFloat(), y + 18f, track, 2f, 0x14FFFFFF)
            p.capsule(g, NAME_X.toFloat(), y + 18f, max(2f, track * row.share), 2f, color)

            g.renderItem(row.type.icon, LIST_X + 3, y + 1)

            val powerW = p.font.width(row.powerText)
            p.text(g, row.powerText, right - powerW, y + 1, color)
            p.text(g, p.cut(row.type.name.string, right - powerW - 4 - NAME_X), NAME_X, y + 1, p.TEXT)

            val tierW = min(p.font.width(row.tier), 72)
            p.fitRight(g, row.tier, right, y + 10, tierW, p.LABEL)
            p.text(g, p.cut(row.location, right - tierW - 4 - NAME_X), NAME_X, y + 10, p.MUTED)

            val hy = max(y, LIST_Y)
            val hh = min(y + ROW_H, LIST_Y + LIST_H) - hy
            hots += Hot(LIST_X, hy, ROW_W, hh, { locate(row) }, tooltip = { rowTooltip(row) })
        }
        g.disableScissor()

        val maxScroll = maxScroll()
        if (maxScroll > 0f) {
            val thumb = thumbHeight()
            val ty = LIST_Y + (LIST_H - thumb) * scroll / maxScroll
            p.capsule(g, BAR_X.toFloat(), ty, 2f, thumb, if (dragging) p.ACCENT else p.MUTED)
        }
    }

    @OnlyIn(Dist.CLIENT)
    private fun rowTooltip(row: RowView): List<Component> {
        val positive = row.power.signum() >= 0
        val color = if (positive) WirelessMonitorPainter.GEN else WirelessMonitorPainter.CON
        val share = when {
            row.share >= 1f -> "100%"
            row.share < 0.001f -> "<0.1%"
            else -> String.format(Locale.ROOT, "%.1f%%", min(row.share * 100f, 99.9f))
        }
        val lines = mutableListOf<Component>(
            row.type.name,
            label("dimension", row.dimension).withStyle(ChatFormatting.GRAY),
            label("position", row.pos.toShortString()).withStyle(ChatFormatting.GRAY),
            Component.literal(exact(row.power) + " EU/t").withStyle { it.withColor(color and 0xFFFFFF) },
            row.tier,
            label(if (positive) "share.generation" else "share.consumption", share).withStyle(ChatFormatting.GRAY)
        )
        if (all) lines += label(if (row.own) "own_team" else "other_team").withStyle(ChatFormatting.GRAY)
        lines += label("locate").withStyle(ChatFormatting.DARK_GRAY)
        return lines
    }

    @OnlyIn(Dist.CLIENT)
    private fun chartTooltip(mx: Int, my: Int): List<Component>? {
        val i = hoveredBucket(mx, my) ?: return null
        val width = BUCKET_TICKS[chartRange]
        val start = (chartFirst + i) * width
        val mean = CommonUtils.formatCompactBigInteger(chartLevel(chartMean[i]))
        if (width == SECOND) {
            return listOf(label("ago", duration(viewTick - start)).withStyle(ChatFormatting.GRAY), Component.literal("$mean EU"))
        }

        val time = label("ago_span", duration(max(0.0, viewTick - start - width)), duration(viewTick - start)).withStyle(ChatFormatting.GRAY)
        val level = if (chartSwing.isEmpty()) 0 else chartSwing[i]
        if (level == 0) return listOf(time, Component.literal("$mean EU"))
        val swing = chartSwingMax * BigInteger.valueOf(level.toLong()) / BIG_LEVELS
        return listOf(time, label("mean", mean), label("swing", CommonUtils.formatCompactBigInteger(swing)).withStyle(ChatFormatting.GRAY))
    }

    // ========================================
    // HELPERS
    // ========================================

    @OnlyIn(Dist.CLIENT)
    private fun outlook(r: BigDecimal?): Pair<Component, Int> {
        val p = WirelessMonitorPainter
        return when {
            r == null -> label("collecting") to p.MUTED
            r.signum() > 0 -> label("eta.growing") to p.GEN
            r.signum() == 0 -> label("eta.balanced") to p.LABEL
            stored.signum() == 0 -> label("eta.empty") to p.CON
            else -> {
                val seconds = BigDecimal(stored).divide(r.abs().multiply(BIG_SECOND), 0, RoundingMode.CEILING).toBigInteger()
                label("eta.drain", WirelessEnergyMonitorAccessor.invokeGetTimeToFillDrainText(seconds)) to p.CAUTION
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private fun frameDelta(): Float {
        val now = Util.getMillis()
        val dt = (now - lastFrame).coerceIn(0L, 100L) / 1000f
        lastFrame = now
        return dt
    }

    /**
     * Advances the chart's right edge and fits the shown scale to what is on screen.
     *
     * - The fit covers the means of the buckets overlapping the view plus the line where it crosses either edge; swings are never drawn, so they do not widen it.
     * - A new extreme widens the scale at once; once an old one has scrolled out, the scale eases back in.
     */
    @OnlyIn(Dist.CLIENT)
    private fun animateTrend(dt: Float) {
        viewTick = min(trendTick.toDouble(), viewTick + viewSpeed * dt)
        if (chartRange != range || chartMean.isEmpty()) return

        val width = BUCKET_TICKS[chartRange]
        val left = viewTick - EnergyHistory.WINDOWS[chartRange]
        val leftLevel = hermite(chartMean, vertexIndex(left))
        val rightLevel = hermite(chartMean, vertexIndex(viewTick))
        var low = min(leftLevel, rightLevel)
        var high = max(leftLevel, rightLevel)
        for (i in chartMean.indices) {
            val start = (chartFirst + i) * width
            if (start >= viewTick || start + width <= left) continue
            low = min(low, chartMean[i].toDouble())
            high = max(high, chartMean[i].toDouble())
        }

        if (rescale) {
            rescale = false
            shownLow = low
            shownHigh = high
            return
        }
        val ease = 1f - exp(-dt * 3f)
        shownLow = if (low < shownLow) low else shownLow + (low - shownLow) * ease
        shownHigh = if (high > shownHigh) high else shownHigh + (high - shownHigh) * ease
    }

    @OnlyIn(Dist.CLIENT)
    private fun animateScroll(dt: Float) {
        scrollTarget = scrollTarget.coerceIn(0f, maxScroll())
        if (!dragging) scroll += (scrollTarget - scroll) * (1f - exp(-dt * 16f))
        if (abs(scrollTarget - scroll) < 0.25f) scroll = scrollTarget
    }

    @OnlyIn(Dist.CLIENT)
    private fun requestWindow(top: Int, bottom: Int) {
        if (matched == 0 || top >= windowStart && bottom <= windowStart + windowRows.size) return

        val desired = max(0, top - (WINDOW_ROWS - VISIBLE) / 2)
        if (desired == requested) return
        requested = desired
        writeClientAction(FIRST) { it.writeVarInt(desired) }
    }

    @OnlyIn(Dist.CLIENT)
    private fun rateColor(value: BigDecimal): Int = when (value.signum()) {
        1 -> WirelessMonitorPainter.GEN
        -1 -> WirelessMonitorPainter.CON
        else -> WirelessMonitorPainter.LABEL
    }

    // Splits like WirelessMonitorPainter.segments, so hit boxes match the drawn segments.
    private fun segmentHots(x: Int, y: Int, w: Int, h: Int, n: Int, action: (Int) -> Unit) {
        for (i in 0 until n) {
            val sx = x + w * i / n
            hots += Hot(sx, y, x + w * (i + 1) / n - sx, h, { action(i) })
        }
    }

    private fun hoveredSegment(mx: Int, my: Int, x: Int, y: Int, w: Int, h: Int, n: Int): Int =
        if (over(mx, my, x, y, w, h)) (mx - x) * n / w else -1

    private fun maxScroll(): Float = max(0, matched * ROW_H - LIST_H).toFloat()

    private fun thumbHeight(): Float = max(12f, LIST_H.toFloat() * LIST_H / (matched * ROW_H))

    private fun overList(mx: Double, my: Double): Boolean =
        mx >= LIST_X && my >= LIST_Y && mx < LIST_X + ROW_W && my < LIST_Y + LIST_H

    // The bucket whose vertex is nearest the cursor, among the vertices inside the view.
    private fun hoveredBucket(mx: Int, my: Int): Int? {
        if (chartRange != range || chartMean.isEmpty() || !over(mx, my, CHART_X + 1, CHART_Y + 1, INNER_W, CHART_H - 2)) return null
        val window = EnergyHistory.WINDOWS[chartRange]
        val width = BUCKET_TICKS[chartRange]
        val origin = (chartFirst * width + (width - SECOND) / 2).toDouble()
        val first = max(0.0, ceil((viewTick - window - origin) / width))
        val last = min(chartMean.size - 1.0, floor((viewTick - origin) / width))
        if (first > last) return null
        val tick = viewTick - (CHART_X + CHART_W - 1.5 - mx) * window / INNER_W
        return ((tick - origin) / width).roundToInt().coerceIn(first.toInt(), last.toInt())
    }

    // Fractional bucket index of a tick, counted between vertices.
    private fun vertexIndex(tick: Double): Double {
        val width = BUCKET_TICKS[chartRange]
        return (tick - chartFirst * width - (width - SECOND) / 2) / width
    }

    private fun vertexX(u: Double): Float {
        val width = BUCKET_TICKS[chartRange]
        val tick = chartFirst * width + (width - SECOND) / 2 + u * width
        return (CHART_X + CHART_W - 1 - (viewTick - tick) * INNER_W / EnergyHistory.WINDOWS[chartRange]).toFloat()
    }

    // The drawn curve at a fractional vertex index, flat beyond the first and the last vertex.
    private fun hermite(levels: IntArray, u: Double): Double {
        val last = levels.size - 1
        if (u <= 0) return levels[0].toDouble()
        if (u >= last) return levels[last].toDouble()
        val i = u.toInt()
        val s = u - i
        val s2 = s * s
        val s3 = s2 * s
        return (2 * s3 - 3 * s2 + 1) * levels[i] + (s3 - 2 * s2 + s) * tangent(levels, i) +
            (3 * s2 - 2 * s3) * levels[i + 1] + (s3 - s2) * tangent(levels, i + 1)
    }

    // Fritsch–Butland slopes: zero at turning points and at both ends, which keeps every interval monotone.
    private fun tangent(levels: IntArray, i: Int): Double {
        if (i == 0 || i == levels.size - 1) return 0.0
        val a = (levels[i] - levels[i - 1]).toDouble()
        val b = (levels[i + 1] - levels[i]).toDouble()
        return if (a * b <= 0) 0.0 else 2 * a * b / (a + b)
    }

    // 15% headroom above and below the shown scale; a flat view is centred.
    private fun levelY(level: Double): Float {
        val pad = if (shownHigh > shownLow) (shownHigh - shownLow) * 0.15 else 1.0
        val top = CHART_Y + 1.0
        val bottom = CHART_Y + CHART_H - 2.0
        return (bottom - (level - shownLow + pad) / (shownHigh - shownLow + 2 * pad) * (bottom - top)).toFloat()
    }

    private fun chartLevel(level: Int): BigInteger = chartLow + chartSpread * BigInteger.valueOf(level.toLong()) / BIG_LEVELS

    private fun duration(ticks: Double): Component {
        val seconds = (ticks / SECOND).roundToLong()
        return when {
            seconds < 60 -> label("duration.s", seconds)
            seconds < 3600 -> label("duration.ms", seconds / 60, seconds % 60)
            else -> label("duration.hm", seconds / 3600, seconds / 60 % 60)
        }
    }

    private fun over(mx: Int, my: Int, x: Int, y: Int, w: Int, h: Int): Boolean =
        mx >= x && my >= y && mx < x + w && my < y + h

    private fun label(key: String, vararg args: Any?): MutableComponent = ("gtladditions.wireless_monitor.$key").toComponent(*args)

    private fun signed(value: BigDecimal): String = sign(value) + CommonUtils.formatCompactBigDecimal(value.abs())

    private fun exact(value: BigDecimal): String = sign(value) + FormattingUtil.formatNumbers(value.abs())

    private fun sign(value: BigDecimal): String = when (value.signum()) {
        1 -> "+"
        -1 -> "-"
        else -> ""
    }

    // ========================================
    // VIEW MODELS & SUB-WIDGETS
    // ========================================

    private class Entry(val row: WirelessEnergyMachineStats.Row, val type: Int, val own: Boolean)

    // Server only: sum, low and high of each bucket; a bucket's vertex is the mean tick of its 20-tick samples.
    private class Buckets(private val first: Long, private val width: Long, size: Int) {
        private val sum = Array<BigInteger>(size) { BigInteger.ZERO }
        private val low = Array<BigInteger>(size) { BigInteger.ZERO }
        private val high = Array<BigInteger>(size) { BigInteger.ZERO }
        private val count = IntArray(size)

        fun index(tick: Long): Int = (Math.floorDiv(tick, width) - first).toInt()

        fun add(i: Int, energy: BigInteger) {
            low[i] = if (count[i] == 0) energy else low[i].min(energy)
            high[i] = if (count[i] == 0) energy else high[i].max(energy)
            sum[i] += energy
            count[i]++
        }

        // Buckets with no record between two sparse minute records take GTLCore's interpolation at their vertex, as energyAt does.
        fun bridge(from: EnergyHistory.Point, to: EnergyHistory.Point) {
            for (i in max(index(from.tick()) + 1, 0) until index(to.tick())) {
                val tick = (first + i) * width + (width - SECOND) / 2
                val rise = (to.energy() - from.energy()) * BigInteger.valueOf(tick - from.tick())
                add(i, from.energy() + rise / BigInteger.valueOf(to.tick() - from.tick()))
            }
        }

        /**
         * Writes the means quantized over their own range, then for aggregated ranges each bucket's swing (high − low) for the tooltip.
         *
         * - Empty buckets only lead (before the account's first sample) or trail (history behind the publish), so they are trimmed.
         * - Swings go as levels of the largest one and round up, so a bucket with any swing keeps a non-zero level.
         */
        fun write(buf: FriendlyByteBuf, swings: Boolean) {
            val head = count.indexOfFirst { it > 0 }
            if (head < 0) {
                buf.writeVarInt(0)
                return
            }
            val tail = count.indexOfLast { it > 0 }
            val mean = Array(tail - head + 1) { sum[head + it] / BigInteger.valueOf(count[head + it].toLong()) }
            var floor = mean[0]
            var ceiling = mean[0]
            for (value in mean) {
                floor = floor.min(value)
                ceiling = ceiling.max(value)
            }
            val spread = ceiling - floor

            buf.writeVarInt(mean.size)
            buf.writeVarLong(first + head)
            buf.writeByteArray(floor.toByteArray())
            buf.writeByteArray(spread.toByteArray())
            for (value in mean) buf.writeShort(if (spread.signum() == 0) 0 else ((value - floor) * BIG_LEVELS / spread).toInt())
            if (!swings) return

            var widest = BigInteger.ZERO
            for (i in head..tail) widest = widest.max(high[i] - low[i])
            buf.writeByteArray(widest.toByteArray())
            if (widest.signum() == 0) return
            for (i in head..tail) buf.writeShort(((high[i] - low[i]) * BIG_LEVELS + widest - BigInteger.ONE).divide(widest).toInt())
        }
    }

    private class TypeView(val name: Component, val icon: ItemStack, val search: String)

    private class RowView(
        val type: TypeView,
        val dimension: String,
        val pos: BlockPos,
        val power: BigDecimal,
        val own: Boolean,
        val powerText: String,
        val location: String,
        val tier: Component,
        val share: Float
    )

    private class Hot(
        val x: Int,
        val y: Int,
        val w: Int,
        val h: Int,
        val action: (() -> Unit)?,
        val tooltip: (() -> List<Component>)? = null
    )

    // Its own text is near-transparent; the page draws the query shadowless like the rest of the page.
    private class SearchField(
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        responder: (String) -> Unit
    ) : TextFieldWidget(x, y, w, h, null, { responder(it) }) {

        init {
            setBordered(false)
            setMaxStringLength(32)
            hover = null
            // Font forces an alpha of 0 to opaque.
            setTextColor(0x04FFFFFF)
        }

        @OnlyIn(Dist.CLIENT)
        fun cursorAtEnd(): Boolean = textField.cursorPosition >= textField.value.length
    }

    companion object {
        const val WIDTH = 340
        const val HEIGHT = 218

        private const val OWNER = 10
        private const val STORED = 11
        private const val TREND = 12
        private const val ROWS = 13

        private const val SCOPE = 10
        private const val FILTER = 11
        private const val TYPES = 12
        private const val FIRST = 13
        private const val RANGE = 14

        private const val SECOND = 20L
        private val BIG_SECOND = BigDecimal.valueOf(SECOND)

        // Bucket width per EnergyHistory.WINDOWS entry: 60 × 1 s, 150 × 2 s, 120 × 30 s and 144 × 10 min.
        private val BUCKET_TICKS = longArrayOf(20L, 40L, 600L, 12000L)

        // Bucket values are quantized to 16 bits on the wire.
        private val BIG_LEVELS = BigInteger.valueOf(0xFFFF)

        private const val SCOPE_X = 250
        private const val CHART_X = 15
        private const val CHART_Y = 102
        private const val CHART_W = 146
        private const val CHART_H = 86
        private const val INNER_W = CHART_W - 2

        // Curve look in GUI units and accent alpha; the line is LINE_CORE plus one physical pixel of feather wide.
        private const val MAX_SUBDIVISIONS = 8
        private const val LINE_CORE = 0.6f
        private const val AREA_ALPHA = 0.03f
        private const val FADE_ALPHA = 0.2f
        private const val SEARCH_Y = 53
        private const val LIST_X = 177
        private const val LIST_Y = 70
        private const val LIST_H = 137
        private const val ROW_W = 146
        private const val ROW_H = 21
        private const val NAME_X = 198
        private const val BAR_X = 325
        private const val VISIBLE = LIST_H / ROW_H + 2
        private const val WINDOW_ROWS = 24

        private val ORDER = compareByDescending<Entry> { it.row.energy().abs() }
            .thenBy { it.row.dimension() }
            .thenBy { it.row.pos().asLong() }

        private val machineIcons: Map<String, ItemStack> by lazy {
            GTRegistries.MACHINES.values().associate { it.descriptionId to it.asStack() }
        }

        private fun index(key: String, ids: Object2IntOpenHashMap<String>, keys: MutableList<String>): Int {
            val id = ids.getInt(key)
            if (id >= 0) return id
            ids.put(key, keys.size)
            keys += key
            return keys.size - 1
        }

        private fun writeDecimal(buf: FriendlyByteBuf, value: BigDecimal) {
            buf.writeByteArray(value.unscaledValue().toByteArray())
            buf.writeVarInt(value.scale())
        }

        private fun readDecimal(buf: FriendlyByteBuf): BigDecimal = BigDecimal(BigInteger(buf.readByteArray()), buf.readVarInt())
    }
}
