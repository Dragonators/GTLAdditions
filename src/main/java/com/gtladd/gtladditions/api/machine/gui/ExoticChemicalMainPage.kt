package com.gtladd.gtladditions.api.machine.gui

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.client.gui.ExoticChemicalPainter
import com.gtladd.gtladditions.client.gui.ExoticChemicalPainter.State
import com.gtladd.gtladditions.common.machine.multiblock.controller.ExoticChemicalComplex
import com.gtladd.gtladditions.utils.CommonUtils
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import net.minecraft.ChatFormatting
import net.minecraft.Util
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.fluids.FluidStack
import java.math.BigInteger
import kotlin.math.roundToInt

class ExoticChemicalMainPage(private val host: ExoticChemicalComplex) : WidgetGroup(0, 0, 314, 218) {

    private var selected = 0
    private var page = 0

    // Server: the values last sent; client: the values last received.
    private var formed = false
    private var available = false
    private var allSuspendAfterFinish = false
    private var structureTier = 0
    private var infinite = false
    private var free = 0L
    private var eut: BigInteger = BigInteger.ZERO
    private val counts = IntArray(4)
    private val unitViews = Array(13) { UnitView() }
    private var releaseAfterFinish = false
    private var paused = false
    private var suspendAfterFinish = false
    private var limitMax = 0L
    private var message: Component? = null
    private var answered = 0

    // Server only.
    private var refresh = true
    private var sentAt = 0L
    private var shownRecipe: GTRecipe? = null

    // Client only.
    private var received = false
    private var independents = 0
    private var outputCount = 0
    private var pages = 1
    private var previewOutputs = emptyList<Output>()
    private var detailOutputs = emptyList<Output>()
    private var outputsView = false
    private var input = "0"
    private var syncing = false

    // Debounce automatic limit edits and keep the typed value while the server reply is pending.
    private var pendingSince = -1L
    private var awaiting = 0
    private var sent = 0
    private val hots = ArrayList<Hot>()
    private val field = LimitField(193, 134, 82, 16) { onInput(it) }

    private val selectedView: UnitView
        get() = unitViews[selected]

    private val limitApplies: Boolean
        get() = formed && available && selected > 0 && selectedView.independent && !releaseAfterFinish && structureTier >= 2

    private val unlimited: Boolean
        get() = infinite && !selectedView.limitAll

    private val showField: Boolean
        get() = formed && !outputsView && selected > 0 && selectedView.independent && !unlimited

    init {
        field.isVisible = false
        field.isActive = false
        addWidget(field.setClientSideWidget())
    }

    // ========================================
    // INPUT & CLIENT ACTIONS
    // ========================================

    @OnlyIn(Dist.CLIENT)
    private fun step(): Long = when {
        Screen.hasShiftDown() && Screen.hasControlDown() -> 512L
        Screen.hasControlDown() -> 64L
        Screen.hasShiftDown() -> 8L
        else -> 1L
    }

    private fun onInput(text: String) {
        input = text
        if (!syncing && limitApplies) pendingSince = System.currentTimeMillis()
    }

    private fun setInput(value: Long) {
        syncing = true
        input = value.toString()
        field.setCurrentString(input)
        syncing = false
    }

    @OnlyIn(Dist.CLIENT)
    private fun nudge(sign: Int) {
        val current = (input.toLongOrNull() ?: 0L).coerceAtLeast(0L)
        val step = step()
        val updated = if (sign > 0) {
            current + minOf(step, Long.MAX_VALUE - current)
        } else {
            current - minOf(step, current)
        }

        setInput(minOf(updated, limitMax))
        if (limitApplies) pendingSince = System.currentTimeMillis()
    }

    private fun submit(limitAll: Boolean = selectedView.limitAll) {
        pendingSince = -1L
        if (!limitApplies) return

        val value = (input.toLongOrNull() ?: 0L).coerceIn(0L, limitMax)
        if (value.toString() != input) setInput(value)

        edit("limit", value, limitAll)
        awaiting = 40
    }

    private fun select(id: Int, nextPage: Int = 0) {
        if (!formed) return
        if (pendingSince >= 0L && id != selected) submit()
        if (id != selected) outputsView = false

        writeClientAction(1) { buf ->
            buf.writeVarInt(id)
            buf.writeVarInt(nextPage)
        }
    }

    private fun edit(action: String, number: Long, flag: Boolean) {
        if (!received || !formed) return

        writeClientAction(2) { buf ->
            buf.writeVarInt(selected)
            buf.writeUtf(action, 32)
            buf.writeLong(number)
            buf.writeBoolean(flag)
            buf.writeVarInt(++sent)
        }
    }

    // ========================================
    // WIDGET LIFECYCLE & EVENTS
    // ========================================

    @OnlyIn(Dist.CLIENT)
    override fun updateScreen() {
        super.updateScreen()
        field.isVisible = showField
        field.isActive = showField && limitApplies
        if (!field.isActive && field.isFocus) field.setFocus(false)

        if (awaiting > 0) awaiting--
        if (pendingSince >= 0L && System.currentTimeMillis() - pendingSince >= 300L) submit()
    }

    @OnlyIn(Dist.CLIENT)
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            val mx = mouseX - position.x
            val my = mouseY - position.y
            for (hot in hots) {
                if (mx < hot.x || my < hot.y || mx >= hot.x + hot.w || my >= hot.y + hot.h) continue
                val action = hot.action ?: break
                action()
                playButtonClickSound()
                return true
            }
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun handleClientAction(id: Int, buf: FriendlyByteBuf) {
        if (id != 1 && id != 2) return super.handleClientAction(id, buf)

        val player = gui.entityPlayer as? ServerPlayer ?: return
        val world = player.serverLevel()
        val sameMachine = world === host.level &&
            (world.getBlockEntity(host.pos) as? IMachineBlockEntity)?.metaMachine === host
        val inReach = player.distanceToSqr(host.pos.x + 0.5, host.pos.y + 0.5, host.pos.z + 0.5) <= 64.0
        if (!sameMachine || !inReach || !player.containerMenu.stillValid(player) || !host.isFormed) return

        if (id == 1) {
            selected = buf.readVarInt().coerceIn(0, 12)
            page = buf.readVarInt().coerceAtLeast(0)
        } else {
            val target = buf.readVarInt()
            val action = buf.readUtf(32)
            val n = buf.readLong()
            val flag = buf.readBoolean()
            answered = buf.readVarInt()

            if (!player.isSpectator && world.mayInteract(player, host.pos) && target == selected) {
                host.getRecipeLogic().configure(target, action, n, flag)
            }
        }

        refresh = true
    }

    // ========================================
    // NETWORK SYNCHRONIZATION
    // ========================================

    /**
     * Synchronizes machine and unit state to the client.
     *
     * - Progress, energy, and free capacity move every tick while running and are throttled to at most every 5 ticks.
     * - Any other change goes out immediately; an unchanged machine sends nothing.
     */
    override fun detectAndSendChanges() {
        super.detectAndSendChanges()

        val now = host.level?.gameTime ?: return
        if (!host.isFormed) {
            if (!refresh && !formed) return
            formed = false
            refresh = false
            shownRecipe = null
            writeUpdateInfo(1) { it.writeBoolean(false) }
            return
        }

        val logic = host.getRecipeLogic()
        val selectedUnit = logic.units[selected]
        val recipe = selectedUnit.currentRecipe

        val fullSync = refresh || !formed
        var urgent = fullSync || recipe !== shownRecipe
        fun <T> track(old: T, next: T): T {
            if (old != next) urgent = true
            return next
        }

        formed = true
        available = track(available, host.isRecipeLogicAvailable)
        allSuspendAfterFinish = track(allSuspendAfterFinish, logic.allSuspendAfterFinish)
        structureTier = track(structureTier, host.structureTier)
        infinite = track(infinite, structureTier == 3)
        releaseAfterFinish = track(releaseAfterFinish, selectedUnit.releaseAfterFinish)
        paused = track(paused, selectedUnit.isSuspend)
        suspendAfterFinish = track(suspendAfterFinish, selectedUnit.suspendAfterFinish)
        limitMax = track(limitMax, logic.maxParallelLimit(selected))
        message = track(message, ExoticChemicalDisplay.message(selectedUnit))

        val nextCounts = IntArray(counts.size)
        var moved = false
        for (unitLogic in logic.units) {
            val state = ExoticChemicalDisplay.state(logic, unitLogic)
            if (logic.isUnitActive(unitLogic)) nextCounts[state.ordinal]++

            val limit = logic.parallelLimitOf(unitLogic.id)
            if (unitViews[unitLogic.id].update(unitLogic.independent, state, unitLogic.duration, unitLogic.limitAll, limit)) {
                urgent = true
            }
            if (unitViews[unitLogic.id].progress != unitLogic.progress) {
                moved = true
            }
        }
        if (!nextCounts.contentEquals(counts)) urgent = true

        val nextEut = ExoticChemicalDisplay.netEut(logic)
        val nextFree = if (available) logic.availableParallel(0) else 0L
        if (nextEut != eut || nextFree != free) moved = true
        if (!urgent && (!moved || now - sentAt < 5L)) return

        nextCounts.copyInto(counts)
        for (unitLogic in logic.units) {
            unitViews[unitLogic.id].progress = unitLogic.progress
        }
        eut = nextEut
        free = nextFree

        val products = if (fullSync || recipe !== shownRecipe) {
            recipe?.let(ExoticChemicalDisplay::outputs).orEmpty()
        } else {
            null
        }
        if (products != null) {
            page = page.coerceIn(0, (products.size - 1).coerceAtLeast(0) / ExoticChemicalDisplay.PER_PAGE)
        }

        refresh = false
        sentAt = now
        shownRecipe = recipe
        writeUpdateInfo(1) { write(it, products) }
    }

    private fun write(buf: FriendlyByteBuf, products: List<ExoticChemicalOutput>?) {
        buf.writeBoolean(true)
        buf.writeBoolean(available)
        buf.writeBoolean(allSuspendAfterFinish)
        buf.writeVarInt(structureTier)
        buf.writeBoolean(infinite)
        buf.writeVarLong(free)
        buf.writeByteArray(eut.toByteArray())
        for (n in counts) buf.writeVarInt(n)
        for (v in unitViews) v.write(buf)
        buf.writeVarInt(selected)
        buf.writeBoolean(releaseAfterFinish)
        buf.writeBoolean(paused)
        buf.writeBoolean(suspendAfterFinish)
        buf.writeVarLong(limitMax)
        buf.writeNullable(message, FriendlyByteBuf::writeComponent)
        buf.writeVarInt(answered)

        /*
         * Send outputs only when the selected unit, its run, or the page changed.
         * The current page always goes along so opening the products view needs no round trip;
         * on page 0 it also supplies the three stacks for the products link.
         */
        buf.writeNullable(products) { b, list ->
            b.writeVarInt(list.size)
            b.writeVarInt(page)
            b.writeCollection(list.drop(page * ExoticChemicalDisplay.PER_PAGE).take(ExoticChemicalDisplay.PER_PAGE), ::writeOutput)
            if (page > 0) b.writeCollection(list.take(3), ::writeOutput)
        }
    }

    private fun writeOutput(buf: FriendlyByteBuf, o: ExoticChemicalOutput) {
        buf.writeBoolean(o.item.isEmpty)
        if (o.item.isEmpty) o.fluid.writeToPacket(buf) else buf.writeItem(o.item)
        buf.writeVarLong(o.amount)
    }

    @OnlyIn(Dist.CLIENT)
    private fun readOutput(buf: FriendlyByteBuf): Output {
        val fluid = buf.readBoolean()
        val item = if (fluid) ItemStack.EMPTY else buf.readItem()
        val stack = if (fluid) FluidStack.readFromPacket(buf) else FluidStack.EMPTY
        val amount = buf.readVarLong()

        val font = ExoticChemicalPainter.font
        val prefix = FormattingUtil.formatNumbers(amount) + if (fluid) " mB " else "× "
        val name = (if (fluid) stack.displayName else item.hoverName).string

        // Like the Jade products, a long name is cut at normal size with one ellipsis; the hover tooltip keeps it whole.
        val room = OUTPUT_WIDTH - font.width(prefix)
        val shown = if (font.width(name) <= room) {
            name
        } else {
            font.plainSubstrByWidth(name, room - font.width(ELLIPSIS)) + ELLIPSIS
        }

        return Output(item, stack, amount, Component.literal(prefix + shown))
    }

    @OnlyIn(Dist.CLIENT)
    override fun readUpdateInfo(id: Int, buffer: FriendlyByteBuf) {
        if (id != 1) return super.readUpdateInfo(id, buffer)

        val wasFormed = received && formed
        val lastSelected = selected

        formed = buffer.readBoolean()
        if (!formed) {
            received = true
            outputsView = false
            pendingSince = -1L
            awaiting = 0
            field.isVisible = false
            field.isActive = false
            field.setFocus(false)
            return
        }

        available = buffer.readBoolean()
        allSuspendAfterFinish = buffer.readBoolean()
        structureTier = buffer.readVarInt()
        infinite = buffer.readBoolean()
        free = buffer.readVarLong()
        eut = BigInteger(buffer.readByteArray())
        for (i in counts.indices) counts[i] = buffer.readVarInt()
        for (v in unitViews) v.read(buffer)
        independents = (1..12).count { unitViews[it].independent }
        selected = buffer.readVarInt()
        releaseAfterFinish = buffer.readBoolean()
        paused = buffer.readBoolean()
        suspendAfterFinish = buffer.readBoolean()
        limitMax = buffer.readVarLong()
        message = buffer.readNullable(FriendlyByteBuf::readComponent)
        answered = buffer.readVarInt()

        if (buffer.readBoolean()) {
            outputCount = buffer.readVarInt()
            page = buffer.readVarInt()
            pages = 1 + (outputCount - 1).coerceAtLeast(0) / ExoticChemicalDisplay.PER_PAGE
            detailOutputs = buffer.readList(::readOutput)
            previewOutputs = if (page > 0) buffer.readList(::readOutput) else detailOutputs.take(3)
        }

        received = true
        if (answered == sent) awaiting = 0

        val moved = !wasFormed || selected != lastSelected
        if (moved || !limitApplies || unlimited) pendingSince = -1L
        if (moved || (pendingSince < 0L && awaiting == 0 && input.toLongOrNull() != selectedView.limit)) {
            setInput(selectedView.limit)
        }
    }

    // ========================================
    // RENDERING - OVERVIEW
    // ========================================

    @OnlyIn(Dist.CLIENT)
    override fun drawInBackground(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val ox = position.x
        val oy = position.y
        val isFormed = if (received) formed else host.isFormed

        hots.clear()
        if (!isFormed) {
            ExoticChemicalPainter.unformedPage(g, ox, oy)
            g.pose().pushPose()
            g.pose().translate(ox.toFloat(), oy.toFloat(), 0f)
            drawUnformed(g)
            g.pose().popPose()
            super.drawInBackground(g, mouseX, mouseY, partialTicks)
            return
        }

        ExoticChemicalPainter.page(g, ox, oy)
        if (received) {
            g.pose().pushPose()
            g.pose().translate(ox.toFloat(), oy.toFloat(), 0f)

            val mx = mouseX - ox
            val my = mouseY - oy
            drawHeader(g)
            drawBlueprint(g)

            val all = allSuspendAfterFinish
            ExoticChemicalPainter.button(
                g,
                10,
                186,
                148,
                16,
                label("suspend_after_finish_all"),
                "clock",
                active = all,
                hovered = over(mx, my, 10, 186, 148, 16)
            )
            hots += Hot(10, 186, 148, 16, { edit("suspend_after_finish_all", 0L, !all) })

            when {
                outputsView -> drawOutputs(g, mx, my)
                selected == 0 -> drawCentral(g, mx, my)
                selectedView.independent -> drawIndependent(g, mx, my)
                else -> drawShared(g)
            }

            g.pose().popPose()
        }

        super.drawInBackground(g, mouseX, mouseY, partialTicks)
        if (showField && received) drawFieldValue(g)
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawUnformed(g: GuiGraphics) {
        val p = ExoticChemicalPainter
        val invalid = Component.translatable("gtceu.multiblock.invalid_structure")
        val tw = p.font.width(invalid)
        val tx = (314 - tw) / 2
        val ty = p.ty(0, 218)
        p.text(g, invalid, tx, ty, 0xFFFF5555.toInt())
        hots += Hot(
            tx - 4,
            ty - 2,
            tw + 8,
            12,
            null,
            tooltip = {
                listOf(Component.translatable("gtceu.multiblock.invalid_structure.tooltip").withStyle(ChatFormatting.GRAY))
            }
        )
    }

    @OnlyIn(Dist.CLIENT)
    override fun drawInForeground(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawInForeground(g, mouseX, mouseY, partialTicks)

        val mx = mouseX - position.x
        val my = mouseY - position.y
        val hot = hots.firstOrNull { it.tooltip != null && over(mx, my, it.x, it.y, it.w, it.h) } ?: return
        val lines = hot.tooltip!!()
        if (lines.isNotEmpty()) {
            gui.modularUIGui.setHoverTooltip(lines, hot.stack, null, null)
        }
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawHeader(g: GuiGraphics) {
        val p = ExoticChemicalPainter
        val structureTierText = label("tier.$structureTier")
        val tw = p.font.width(structureTierText) + 10
        p.tag(g, 10, 9, tw, 12, p.ACCENT_SOFT, p.ACCENT_BORDER)
        p.textCenter(g, structureTierText, 10 + tw / 2, 11, p.ACCENT_TEXT)

        val eu = Component.literal(CommonUtils.formatDouble(eut.abs().toDouble()) + " EU/t")
        val ex = 304 - p.font.width(eu)
        p.text(g, eu, ex, 11, p.TEXT)
        p.icon(g, "bolt", ex - 10, 10, State.WAIT.main)

        var cx = 10 + tw + 8
        for (st in State.entries) {
            val n = if (st == State.SHARED) 12 - independents else counts[st.ordinal]
            if (n <= 0) continue

            val name = label("state." + st.key)
            val width = 9 + p.font.width(name) + p.font.width(n.toString())
            if (cx + width > ex - 14) break

            if (st == State.SHARED) {
                p.rect(g, cx, 12, 5, 5, p.WELL_BG)
                p.frame(g, cx, 12, 5, 5, st.main)
            } else {
                p.rect(g, cx, 12, 5, 5, st.main)
                p.rect(g, cx, 12, 5, 1, 0x61FFFFFF)
            }

            val lw = p.text(g, name, cx + 7, 11, p.TEXT_DIM)
            val dw = p.text(g, n.toString(), cx + 8 + lw, 11, p.TEXT)
            cx += 8 + lw + dw + 7
        }
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawBlueprint(g: GuiGraphics) {
        val p = ExoticChemicalPainter
        p.blueprint(g, 10, 32, 148)

        val cx = 84
        val cy = 106
        val k = 144f * 292 / 304 / 87
        fun podX(dx: Float) = (cx + dx * k - 8).roundToInt()
        fun podY(dy: Float) = (cy + dy * k - 8).roundToInt()

        PODS.firstOrNull { it.id == selected }?.let { pod ->
            p.dashed(g, cx, cy, podX(pod.dx) + 8, podY(pod.dy) + 8, p.alpha(p.ACCENT, 0.65f))
        }

        for (pod in PODS) {
            val v = unitViews[pod.id]
            val x = podX(pod.dx)
            val y = podY(pod.dy)
            val st = look(v)
            val sel = pod.id == selected

            if (st == State.SHARED) {
                p.octagon(
                    g,
                    x,
                    y,
                    16,
                    4,
                    p.alpha(st.fillB, 0.88f),
                    p.alpha(st.fillA, 0.88f),
                    if (sel) p.ACCENT else p.alpha(st.main, 0.6f),
                    true
                )
                p.textCenter(
                    g,
                    Component.literal(num(pod.id)),
                    x + 8,
                    y + 4,
                    if (sel) p.ACCENT_TEXT else p.alpha(st.text, 0.72f)
                )
            } else {
                p.octagon(g, x, y, 16, 4, st.fillA, st.fillB, if (sel) p.ACCENT else st.main, false)
                p.textCenter(g, Component.literal(num(pod.id)), x + 8, y + 3, if (sel) -1 else st.text)
                p.rect(g, x + 3, y + 12, 10, 2, 0x8C000000.toInt())
                if (v.duration > 0 && v.progress > 0) {
                    val fillW = maxOf(2, (10f * v.progress.coerceAtMost(v.duration) / v.duration).roundToInt())
                    p.rect(g, x + 3, y + 12, fillW, 2, st.main)
                }
            }

            if (sel) p.reticle(g, x - 2, y - 2, 20, 20, p.ACCENT)
            hots += Hot(x, y, 16, 16, { select(pod.id) }, tooltip = { podTooltip(pod.id) })
        }

        val central = unitViews[0]
        val cst = look(central)
        val sel = selected == 0

        p.octagon(
            g,
            71,
            93,
            26,
            4,
            p.lightness(p.CENTRAL_BASE, 1.05f),
            p.lightness(p.CENTRAL_BASE, 0.65f),
            if (sel) p.ACCENT else cst.main,
            false
        )
        p.textCenter(g, label("central"), cx, 98, -1)
        p.rect(g, 75, 109, 18, 2, 0x8C000000.toInt())
        if (central.duration > 0) {
            val fillW = maxOf(2, (18f * central.progress.coerceAtMost(central.duration) / central.duration).roundToInt())
            p.cylinder(g, 75, 109, fillW, 2, cst.bar)
        }
        p.rect(g, 82, 116, 5, 2, p.ACCENT)
        if (sel) p.reticle(g, 69, 91, 30, 30, p.ACCENT)
        p.rect(g, 81, 177, 6, 1, p.alpha(p.ACCENT, 0.6f))
        hots += Hot(71, 93, 26, 26, { select(0) }, tooltip = { podTooltip(0) })
    }

    @OnlyIn(Dist.CLIENT)
    private fun podTooltip(id: Int): List<Component> {
        val v = unitViews[id]
        val st = look(v)
        val lines = mutableListOf<Component>(
            Component.literal(num(id) + " ").append(unitName(id)),
            label("state." + st.key).withStyle { it.withColor(st.main and 0xFFFFFF) }
        )

        if (id > 0 && v.independent) {
            val limitText = if (infinite && !v.limitAll) "∞" else FormattingUtil.formatNumbers(v.limit)
            lines += label("limit_value", limitText)
        }

        return lines
    }

    // ========================================
    // RENDERING - UNIT DECKS
    // ========================================

    @OnlyIn(Dist.CLIENT)
    private fun drawDeckHeader(g: GuiGraphics, st: State) {
        val p = ExoticChemicalPainter
        val shared = st == State.SHARED

        p.tag(
            g,
            171,
            38,
            18,
            12,
            if (shared) p.SUB_WELL else p.ACCENT_SOFT,
            if (shared) p.ROW_BORDER else p.ACCENT_BORDER
        )
        p.textCenter(
            g,
            Component.literal(num(selected)),
            180,
            p.ty(38, 12),
            if (shared) p.TEXT_DIM else p.ACCENT_TEXT
        )

        val status = label("state." + st.key)
        val sbW = p.font.width(status) + 13
        val sbx = 297 - sbW
        fitText(g, unitName(selected), 194, 40, sbx - 198, p.TEXT)

        p.tag(g, sbx, 38, sbW, 12, st.badge, p.alpha(st.main, 0.45f))
        if (shared) {
            p.rect(g, sbx + 4, 42, 4, 4, p.WELL_BG)
            p.frame(g, sbx + 4, 42, 4, 4, st.main)
        } else {
            p.rect(g, sbx + 4, 42, 4, 4, st.main)
        }
        p.text(g, status, sbx + 10, p.ty(38, 12), st.text)

        message?.let { m ->
            hots += Hot(sbx, 38, sbW, 12, null, tooltip = { listOf(m) })
        }
        p.groove(g, 166, 55, 136)
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawShared(g: GuiGraphics) {
        val p = ExoticChemicalPainter
        drawDeckHeader(g, State.SHARED)
        fitText(g, label("independent_run"), 172, 65, 97, p.TEXT)

        val disabled = !available || structureTier < 2 || releaseAfterFinish
        p.switch(g, 273, 63, false, disabled)
        if (!disabled) {
            hots += Hot(273, 63, p.SWITCH_W, 11, { edit("independent", 0L, false) })
        }
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawCentral(g: GuiGraphics, mx: Int, my: Int) {
        val p = ExoticChemicalPainter
        val st = look(selectedView)

        drawDeckHeader(g, st)
        drawProgress(g, 63, st)
        drawProductsLink(g, 80, mx, my)

        p.well(g, 171, 101, 126, 42, 0xFF121820.toInt())

        val available = Component.literal(if (infinite) "∞" else FormattingUtil.formatNumbers(free))
        val availableLabel = label("available")
        val maxValWidth = 114 - minOf(p.font.width(availableLabel), 44) - 4
        val valWidth = minOf(p.font.width(available), maxValWidth)
        fitText(g, availableLabel, 177, 108, 114 - valWidth - 4, p.TEXT_DIM)
        fitTextRight(g, available, 291, 108, valWidth, p.ACCENT_TEXT)

        p.rect(g, 177, 121, 114, 1, 0x12FFFFFF)

        val hosted = Component.literal("${12 - independents} / 12")
        p.textRight(g, hosted, 291, 127, 0xFFEEF3F8.toInt())
        fitText(g, label("hosted"), 177, 127, 110 - p.font.width(hosted), p.TEXT_DIM)

        drawActions(g, mx, my)
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawIndependent(g: GuiGraphics, mx: Int, my: Int) {
        val p = ExoticChemicalPainter
        val st = look(selectedView)

        drawDeckHeader(g, st)
        fitText(g, label("independent_run"), 172, 64, 97, p.TEXT)
        p.switch(g, 273, 62, !releaseAfterFinish, false, pending = releaseAfterFinish)
        hots += if (releaseAfterFinish) {
            Hot(273, 62, p.SWITCH_W, 11, null, tooltip = { listOf(label("release_after_finish")) })
        } else {
            Hot(273, 62, p.SWITCH_W, 11, { edit("share", 0L, false) })
        }

        drawProgress(g, 80, st)
        drawProductsLink(g, 97, mx, my)

        p.groove(g, 166, 116, 136)
        val limitWidth = p.text(g, label("limit"), 172, 123, p.TEXT_DIM)
        if (!infinite) {
            fitTextRight(
                g,
                label("remaining", FormattingUtil.formatNumbers(free)),
                296,
                123,
                296 - (172 + limitWidth + 4),
                p.TEXT_MUTED
            )
        }

        val locked = unlimited || !limitApplies
        val value = input.toLongOrNull() ?: 0L
        val step = step()
        val minusOff = locked || value <= 0L
        val plusOff = locked || value >= limitMax

        p.button(g, 171, 134, 22, 16, null, disabled = minusOff, hovered = over(mx, my, 171, 134, 22, 16))
        p.textFit(g, "-$step", 171, 134, 22, 16, if (minusOff) p.TEXT_MUTED else p.TEXT)
        p.well(g, 193, 134, 82, 16, if (locked) p.BTN_DISABLED else 0xFF0E1319.toInt())
        p.button(g, 275, 134, 22, 16, null, disabled = plusOff, hovered = over(mx, my, 275, 134, 22, 16))
        p.textFit(g, "+$step", 275, 134, 22, 16, if (plusOff) p.TEXT_MUTED else p.TEXT)

        if (!minusOff) hots += Hot(171, 134, 22, 16, { nudge(-1) })
        if (!plusOff) hots += Hot(275, 134, 22, 16, { nudge(1) })
        if (unlimited) p.textCenter(g, Component.literal("∞"), 234, p.ty(134, 16), p.TEXT_MUTED)

        val limitAll = selectedView.limitAll
        fitText(g, label("limit_all"), 172, 157, 97, p.TEXT)
        p.switch(g, 273, 155, limitAll, !limitApplies)
        if (limitApplies) {
            hots += Hot(273, 155, p.SWITCH_W, 11, { submit(!limitAll) })
        }

        drawActions(g, mx, my)
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawOutputs(g: GuiGraphics, mx: Int, my: Int) {
        val p = ExoticChemicalPainter

        p.button(g, 171, 38, 36, 14, label("back"), "back", hovered = over(mx, my, 171, 38, 36, 14))
        hots += Hot(
            171,
            38,
            36,
            14,
            {
                outputsView = false
                if (page != 0) select(selected, 0)
            }
        )
        p.text(g, Component.literal(num(selected) + " ").append(label("products")), 213, 41, p.TEXT)
        p.groove(g, 166, 56, 136)

        p.well(g, 171, 62, 126, if (pages > 1) 112 else 133, 0xFF11161E.toInt())
        for ((i, o) in detailOutputs.withIndex()) {
            val y = 68 + 13 * i
            if (o.item.isEmpty) {
                p.fluid(g, o.fluid, 176, y)
            } else {
                p.item(g, o.item, 176, y)
            }

            // Names are already cut to the row; only an amount wider than the row itself still gets squeezed.
            fitText(g, o.line, 187, y, OUTPUT_WIDTH, 0xFFEDF2F7.toInt())
            hots += Hot(
                175,
                y - 2,
                120,
                12,
                null,
                o.item,
                tooltip = {
                    if (o.item.isEmpty) {
                        listOf(o.fluid.displayName, Component.literal(FormattingUtil.formatNumbers(o.amount) + " mB"))
                    } else {
                        DrawerHelper.getItemToolTip(o.item)
                    }
                }
            )
        }

        if (pages > 1) {
            val back = page <= 0
            val next = page + 1 >= pages
            p.button(
                g,
                171,
                180,
                22,
                15,
                Component.literal("<"),
                disabled = back,
                hovered = over(mx, my, 171, 180, 22, 15)
            )
            p.textCenter(g, Component.literal("${page + 1} / $pages"), 234, p.ty(180, 15), p.TEXT_DIM)
            p.button(
                g,
                275,
                180,
                22,
                15,
                Component.literal(">"),
                disabled = next,
                hovered = over(mx, my, 275, 180, 22, 15)
            )

            if (!back) hots += Hot(171, 180, 22, 15, { select(selected, page - 1) })
            if (!next) hots += Hot(275, 180, 22, 15, { select(selected, page + 1) })
        }
    }

    // ========================================
    // RENDERING - SHARED COMPONENTS & HELPERS
    // ========================================

    @OnlyIn(Dist.CLIENT)
    private fun drawProgress(g: GuiGraphics, y: Int, st: State) {
        val v = selectedView
        val barLabel = if (v.duration > 0) label("bar", v.progress, v.duration) else null
        ExoticChemicalPainter.progress(g, 171, y, 126, 13, v.progress, v.duration, st, barLabel)
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawProductsLink(g: GuiGraphics, y: Int, mx: Int, my: Int) {
        if (outputCount <= 0) return

        val p = ExoticChemicalPainter
        p.flushLink(g, 171, y, 126, 15, over(mx, my, 171, y, 126, 15))

        var x = 176
        for (o in previewOutputs) {
            if (o.item.isEmpty) p.fluid(g, o.fluid, x, y + 4) else p.item(g, o.item, x, y + 4)
            x += 10
        }

        p.text(g, label("products"), x + 2, p.ty(y, 15), p.TEXT_DIM)
        hots += Hot(
            171,
            y,
            126,
            15,
            {
                outputsView = true
                if (page != 0) select(selected, 0)
            }
        )
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawActions(g: GuiGraphics, mx: Int, my: Int) {
        val p = ExoticChemicalPainter
        val stopped = paused
        val after = suspendAfterFinish

        p.button(
            g,
            171,
            180,
            44,
            16,
            label(if (stopped) "resume" else "pause"),
            if (stopped) "play" else "pause",
            active = stopped,
            hovered = over(mx, my, 171, 180, 44, 16)
        )
        hots += Hot(171, 180, 44, 16, { edit("pause", 0L, !stopped) })

        val afterDisabled = stopped || releaseAfterFinish
        p.button(
            g,
            219,
            180,
            78,
            16,
            label("suspend_after_finish"),
            "clock",
            active = after,
            disabled = afterDisabled,
            hovered = over(mx, my, 219, 180, 78, 16)
        )
        if (!afterDisabled) {
            hots += Hot(219, 180, 78, 16, { edit("suspend_after_finish", 0L, !after) })
        }
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawFieldValue(g: GuiGraphics) {
        val p = ExoticChemicalPainter
        val x = position.x + 193
        val y = position.y + 134
        val tw = p.font.width(input)
        val k = if (tw > 76) 76f / tw else 1f
        val drawnW = tw * k
        val tx = x + (82f - drawnW) / 2f

        if (k < 1f) {
            g.pose().pushPose()
            g.pose().translate(tx, y + 8f - 4f * k, 0f)
            g.pose().scale(k, k, 1f)
            g.drawString(p.font, input, 0, 0, p.LCD_TEXT, false)
            g.pose().popPose()
        } else {
            p.text(g, input, tx.roundToInt(), p.ty(y, 16), p.LCD_TEXT)
        }
        if (field.isFocus) {
            p.rect(g, x + 1, y + 14, 80, 1, p.ACCENT)
            if (field.cursorAtEnd() && Util.getMillis() / 300L % 2L == 0L) {
                val cx = minOf((tx + drawnW + 1f).roundToInt(), x + 80)
                p.rect(g, cx, y + 4, 1, 8, p.ACCENT)
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private fun fitText(g: GuiGraphics, text: Component, x: Int, y: Int, width: Int, color: Int) {
        val tw = ExoticChemicalPainter.font.width(text)
        if (tw <= width) {
            ExoticChemicalPainter.text(g, text, x, y, color)
            return
        }

        val k = width.toFloat() / tw
        g.pose().pushPose()
        g.pose().translate(x.toFloat(), y + 4f * (1f - k), 0f)
        g.pose().scale(k, k, 1f)
        g.drawString(ExoticChemicalPainter.font, text, 0, 0, color, false)
        g.pose().popPose()
    }

    @OnlyIn(Dist.CLIENT)
    private fun fitTextRight(g: GuiGraphics, text: Component, right: Int, y: Int, width: Int, color: Int) {
        val tw = ExoticChemicalPainter.font.width(text)
        if (tw <= width) {
            ExoticChemicalPainter.text(g, text, right - tw, y, color)
            return
        }

        val k = width.toFloat() / tw
        g.pose().pushPose()
        g.pose().translate(right - width.toFloat(), y + 4f * (1f - k), 0f)
        g.pose().scale(k, k, 1f)
        g.drawString(ExoticChemicalPainter.font, text, 0, 0, color, false)
        g.pose().popPose()
    }

    private fun over(mx: Int, my: Int, x: Int, y: Int, w: Int, h: Int): Boolean =
        mx >= x && my >= y && mx < x + w && my < y + h

    @OnlyIn(Dist.CLIENT)
    private fun look(v: UnitView): State = State.entries[v.state.ordinal]

    private fun label(key: String, vararg args: Any?): MutableComponent = ExoticChemicalDisplay.label(key, *args)

    private fun unitName(id: Int): MutableComponent = if (id == 0) label("central") else label("unit.$id")

    private fun num(id: Int): String = id.toString().padStart(2, '0')

    // ========================================
    // VIEW MODELS & SUB-WIDGETS
    // ========================================

    private class Hot(
        val x: Int,
        val y: Int,
        val w: Int,
        val h: Int,
        val action: (() -> Unit)?,
        val stack: ItemStack = ItemStack.EMPTY,
        val tooltip: (() -> List<Component>)? = null
    )

    private class UnitView {
        var independent = false
        var state = ExoticChemicalUnitState.IDLE
        var progress = 0
        var duration = 0
        var limitAll = false
        var limit = 0L

        fun update(
            independent: Boolean,
            state: ExoticChemicalUnitState,
            duration: Int,
            limitAll: Boolean,
            limit: Long
        ): Boolean {
            val changed = independent != this.independent ||
                state != this.state ||
                duration != this.duration ||
                limitAll != this.limitAll ||
                limit != this.limit
            this.independent = independent
            this.state = state
            this.duration = duration
            this.limitAll = limitAll
            this.limit = limit
            return changed
        }

        fun write(buf: FriendlyByteBuf) {
            buf.writeBoolean(independent)
            buf.writeByte(state.ordinal)
            buf.writeVarInt(progress)
            buf.writeVarInt(duration)
            buf.writeBoolean(limitAll)
            buf.writeVarLong(limit)
        }

        fun read(buf: FriendlyByteBuf) {
            independent = buf.readBoolean()
            state = ExoticChemicalUnitState.entries[buf.readByte().toInt()]
            progress = buf.readVarInt()
            duration = buf.readVarInt()
            limitAll = buf.readBoolean()
            limit = buf.readVarLong()
        }
    }

    private class Output(
        val item: ItemStack,
        val fluid: FluidStack,
        val amount: Long,
        val line: Component
    )

    private class Pod(
        val id: Int,
        val dx: Float,
        val dy: Float
    )

    // GT NumberInputWidget behaviour; its own text is near-transparent and the page draws the value centred, shadowless.
    private class LimitField(
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        responder: (String) -> Unit
    ) : TextFieldWidget(x, y, w, h, null, { responder(it) }) {

        init {
            setBordered(false)
            setMaxStringLength(19)
            setNumbersOnly(0L, Long.MAX_VALUE)
            setWheelDur(0f)
            hover = null
            // Near-transparent but non-zero alpha: Font forces an alpha of 0 to opaque.
            setTextColor(0x04FFFFFF)
        }

        @OnlyIn(Dist.CLIENT)
        fun cursorAtEnd(): Boolean = textField.cursorPosition >= textField.value.length

        @OnlyIn(Dist.CLIENT)
        private fun shift(): Int = maxOf(0, (size.width - ExoticChemicalPainter.font.width(currentString)) / 2 - 2)

        @OnlyIn(Dist.CLIENT)
        override fun drawInBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
            graphics.pose().pushPose()
            graphics.pose().translate(shift().toFloat(), 0f, 0f)
            super.drawInBackground(graphics, mouseX, mouseY, partialTicks)
            graphics.pose().popPose()
        }

        @OnlyIn(Dist.CLIENT)
        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            if (!isMouseOverElement(mouseX, mouseY)) return super.mouseClicked(mouseX, mouseY, button)
            val shifted = (mouseX - shift()).coerceIn(position.x.toDouble(), position.x + size.width - 1.0)
            return super.mouseClicked(shifted, mouseY, button)
        }
    }

    private companion object {
        const val OUTPUT_WIDTH = 107
        const val ELLIPSIS = "…"

        // Pod centres in blocks from the core, read from the structure's top view (south = controller side).
        val PODS = listOf(
            Pod(1, -8.2f, 23f),
            Pod(2, 8.2f, 23f),
            Pod(3, -17.1f, 17.1f),
            Pod(4, 17.1f, 17.1f),
            Pod(5, -23f, 8.2f),
            Pod(6, 23f, 8.2f),
            Pod(7, -23f, -8.2f),
            Pod(8, 23f, -8.2f),
            Pod(9, -17.1f, -17.1f),
            Pod(10, 17.1f, -17.1f),
            Pod(11, -8.2f, -23f),
            Pod(12, 8.2f, -23f)
        )
    }
}