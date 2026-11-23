package com.gtladd.gtladditions.api.machine.gui

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.client.gui.ExoticChemicalPainter
import com.gtladd.gtladditions.client.gui.ExoticChemicalPainter.State
import com.gtladd.gtladditions.common.machine.multiblock.controller.ExoticChemicalComplex
import com.gtladd.gtladditions.common.machine.multiblock.controller.ExoticChemicalComplex.Companion.TSD_COST_COEFFICIENT
import com.gtladd.gtladditions.common.machine.multiblock.controller.TimeSpaceDistorter
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
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes.DISTORT_RECIPES
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
    private var capacity = 0L
    private var distorterMultiplier = 1.0
    private var eut: BigInteger = BigInteger.ZERO
    private val counts = IntArray(4)
    private val unitViews = Array(13) { UnitView() }
    private var paused = false
    private var suspendAfterFinish = false
    private var message: Component? = null
    private var answered = 0

    // Server only.
    private var refresh = true
    private var sentAt = 0L
    private var shownRecipe: GTRecipe? = null

    // Client only.
    private var received = false
    private var outputCount = 0
    private var pages = 1
    private var previewOutputs = emptyList<Output>()
    private var detailOutputs = emptyList<Output>()
    private var outputsView = false
    private var input = "1"
    private var syncing = false

    // Debounce automatic limit edits and keep the typed value while the server reply is pending.
    private var pendingSince = -1L
    private var awaiting = 0
    private var sent = 0
    private val hots = ArrayList<Hot>()
    private val field = LimitField(193, LIMIT_Y, 82, 16) { onInput(it) }

    private val selectedView: UnitView
        get() = unitViews[selected]

    private val limitApplies: Boolean
        get() = formed && available && selected > 0 && structureTier >= 2

    private val infinite: Boolean
        get() = structureTier >= 3

    private val limitMax: Long
        get() = if (infinite) Long.MAX_VALUE else maxOf(1L, capacity)

    private val unlimited: Boolean
        get() = infinite && !selectedView.limitAll

    private val showField: Boolean
        get() = formed && !outputsView && selected > 0 && structureTier >= 2 && !unlimited

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
        val current = (input.toLongOrNull() ?: 1L).coerceAtLeast(1L)
        val step = step()
        val updated = if (sign > 0) {
            current + minOf(step, Long.MAX_VALUE - current)
        } else {
            current - minOf(step, current - 1L)
        }

        setInput(minOf(updated, limitMax))
        if (limitApplies) pendingSince = System.currentTimeMillis()
    }

    private fun submit() {
        pendingSince = -1L
        if (!limitApplies) return

        val value = (input.toLongOrNull() ?: 1L).coerceIn(1L, limitMax)
        if (value.toString() != input) setInput(value)

        edit("limit", value, false)
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
     * - Progress and energy move every tick while running and are throttled to at most every 5 ticks.
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
        capacity = track(capacity, logic.capacity)

        val multiplier = if (infinite && available && logic.units.any { it.recipeType === DISTORT_RECIPES }) {
            host.resolveTSD()?.takeIf(TimeSpaceDistorter::isReady)?.outputMultiplier ?: 1.0
        } else {
            1.0
        }
        distorterMultiplier = track(distorterMultiplier, multiplier)

        paused = track(paused, selectedUnit.isSuspend)
        suspendAfterFinish = track(suspendAfterFinish, selectedUnit.suspendAfterFinish)
        message = track(message, if (logic.isUnitUnlocked(selected)) ExoticChemicalDisplay.message(selectedUnit) else null)

        val nextCounts = IntArray(counts.size)
        var moved = false
        for (unitLogic in logic.units) {
            val state = ExoticChemicalDisplay.state(logic, unitLogic)
            if (state != ExoticChemicalUnitState.LOCKED) nextCounts[state.ordinal]++

            if (unitViews[unitLogic.id].update(
                    unitLogic.recipeType === DISTORT_RECIPES,
                    state,
                    unitLogic.duration,
                    unitLogic.limitAll,
                    unitLogic.parallelLimit
                )
            ) {
                urgent = true
            }
            if (unitViews[unitLogic.id].progress != unitLogic.progress) {
                moved = true
            }
        }

        if (!nextCounts.contentEquals(counts)) urgent = true

        val nextEut = ExoticChemicalDisplay.netEut(logic)
        if (nextEut != eut) moved = true
        if (!urgent && (!moved || now - sentAt < 5L)) return

        nextCounts.copyInto(counts)
        for (unitLogic in logic.units) {
            unitViews[unitLogic.id].progress = unitLogic.progress
        }
        eut = nextEut

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
        buf.writeVarLong(capacity)
        buf.writeDouble(distorterMultiplier)
        buf.writeByteArray(eut.toByteArray())
        for (n in counts) buf.writeVarInt(n)
        for (v in unitViews) v.write(buf)
        buf.writeVarInt(selected)
        buf.writeBoolean(paused)
        buf.writeBoolean(suspendAfterFinish)
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
        capacity = buffer.readVarLong()
        distorterMultiplier = buffer.readDouble()
        eut = BigInteger(buffer.readByteArray())
        for (i in counts.indices) counts[i] = buffer.readVarInt()
        for (v in unitViews) v.read(buffer)
        selected = buffer.readVarInt()
        paused = buffer.readBoolean()
        suspendAfterFinish = buffer.readBoolean()
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
        if (selectedView.state == ExoticChemicalUnitState.LOCKED) outputsView = false
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

            if (outputsView) drawOutputs(g, mx, my) else drawUnitDetails(g, mx, my)

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

        val statusX = 10 + tw + 8
        val statusWidth = counts.indices.sumOf { i ->
            15 + p.font.width(label("state." + State.entries[i].key)) + p.font.width(counts[i].toString())
        }
        val scale = minOf(1f, (ex - 14 - statusX) / statusWidth.toFloat())
        g.pose().pushPose()
        g.pose().translate(statusX.toFloat(), 15f - 4f * scale, 0f)
        g.pose().scale(scale, scale, 1f)

        var cx = 0
        for (i in counts.indices) {
            val st = State.entries[i]
            p.rect(g, cx, 1, 5, 5, st.main)
            p.rect(g, cx, 1, 5, 1, 0x61FFFFFF)
            val lw = p.text(g, label("state." + st.key), cx + 7, 0, p.TEXT_DIM)
            val dw = p.text(g, counts[i].toString(), cx + 8 + lw, 0, p.TEXT)
            cx += 8 + lw + dw + 7
        }
        g.pose().popPose()
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

            if (st == State.LOCKED) {
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
                p.rect(g, x + 6, y + 14, 4, 1, if (v.distortRecipes) p.DISTORT_ACCENT else p.ACCENT)
            }

            if (sel) p.reticle(g, x - 2, y - 2, 20, 20, p.ACCENT)
            hots += Hot(x, y, 16, 16, { select(pod.id) }, tooltip = { podTooltip(pod.id) })
        }

        val nexus = unitViews[0]
        val nst = look(nexus)
        val sel = selected == 0

        p.octagon(
            g,
            71,
            93,
            26,
            4,
            p.lightness(p.NEXUS_BASE, 1.05f),
            p.lightness(p.NEXUS_BASE, 0.65f),
            if (sel) p.ACCENT else nst.main,
            false
        )
        p.textCenter(g, Component.literal(num(0)), cx, 98, -1)
        p.rect(g, 75, 109, 18, 2, 0x8C000000.toInt())
        if (nexus.duration > 0) {
            val fillW = maxOf(2, (18f * nexus.progress.coerceAtMost(nexus.duration) / nexus.duration).roundToInt())
            p.cylinder(g, 75, 109, fillW, 2, nst.bar)
        }
        p.rect(g, 82, 116, 5, 2, if (nexus.distortRecipes) p.DISTORT_ACCENT else p.ACCENT)
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

        if (st == State.LOCKED) {
            lines += label("locked_hint")
            return lines
        }

        val p = ExoticChemicalPainter
        val modeColor = if (v.distortRecipes) p.DISTORT_ACCENT else p.ACCENT
        lines += ExoticChemicalDisplay.recipeMode(v.distortRecipes).withStyle { it.withColor(modeColor and 0xFFFFFF) }
        if (id > 0) {
            val limitText = if (infinite && !v.limitAll) "∞" else FormattingUtil.formatNumbers(v.limit)
            lines += label("limit_value", limitText)
        }
        distorterText(v)?.let { lines += it.withStyle(ChatFormatting.GOLD) }

        return lines
    }

    // ========================================
    // RENDERING - UNIT DECKS
    // ========================================

    @OnlyIn(Dist.CLIENT)
    private fun drawDeckHeader(g: GuiGraphics, st: State) {
        val p = ExoticChemicalPainter
        val locked = st == State.LOCKED

        p.tag(
            g,
            171,
            38,
            18,
            12,
            if (locked) p.SUB_WELL else p.ACCENT_SOFT,
            if (locked) p.ROW_BORDER else p.ACCENT_BORDER
        )
        p.textCenter(
            g,
            Component.literal(num(selected)),
            180,
            p.ty(38, 12),
            if (locked) p.TEXT_DIM else p.ACCENT_TEXT
        )

        val status = label("state." + st.key)
        val sbW = p.font.width(status) + 13
        val sbx = 297 - sbW
        fitText(g, unitName(selected), 194, 40, sbx - 198, p.TEXT)

        p.tag(g, sbx, 38, sbW, 12, st.badge, p.alpha(st.main, 0.45f))
        if (locked) {
            p.rect(g, sbx + 4, 42, 4, 4, p.WELL_BG)
            p.frame(g, sbx + 4, 42, 4, 4, st.main)
        } else {
            p.rect(g, sbx + 4, 42, 4, 4, st.main)
        }
        p.text(g, status, sbx + 10, p.ty(38, 12), st.text)

        message?.let { m ->
            hots += Hot(sbx, 38, sbW, 12, null, tooltip = { listOf(m) })
        }
        p.groove(g, 171, 53, 126)
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawUnitDetails(g: GuiGraphics, mx: Int, my: Int) {
        val p = ExoticChemicalPainter
        val st = look(selectedView)
        drawDeckHeader(g, st)
        drawRecipeModes(g, mx, my)

        if (st == State.LOCKED) {
            p.well(g, 171, 93, 126, 40, p.WELL_BG)
            p.textFit(g, label("locked_title").string, 171, 97, 126, 14, p.TEXT_DIM)
            p.textFit(g, label("locked_hint").string, 171, 114, 126, 14, p.TEXT_MUTED)
            return
        }

        drawProgress(g, 75, st)
        drawProductsLink(g, 90, mx, my)
        p.groove(g, 171, 107, 126)

        val boost = distorterText(selectedView)
        if (selected == 0) {
            drawNexusParallel(g, boost)
        } else {
            drawParallelControls(g, mx, my)
            if (boost != null) {
                p.tag(g, 171, 157, 126, 15, p.DISTORT_SOFT, p.DISTORT_ACCENT)
                p.textFit(g, boost.string, 174, 157, 120, 15, p.DISTORT_TEXT)
            }
        }

        drawActions(g, mx, my)
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawRecipeModes(g: GuiGraphics, mx: Int, my: Int) {
        val p = ExoticChemicalPainter
        val enabled = infinite && available

        for (index in 0..1) {
            val x = 171 + index * 63
            val distort = index == 1
            val current = distort == selectedView.distortRecipes
            val active = enabled && current
            val hovered = enabled && over(mx, my, x, 57, 63, 14)

            val bg = when {
                active -> if (distort) p.DISTORT_SOFT else p.ACCENT_SOFT
                hovered -> p.ROW_HI
                else -> p.BTN_DISABLED
            }
            val border = if (active) {
                if (distort) p.DISTORT_ACCENT else p.ACCENT_BORDER
            } else {
                p.ROW_BORDER
            }
            p.tag(g, x, 57, 63, 14, bg, border)

            val color = when {
                active -> if (distort) p.DISTORT_TEXT else p.ACCENT_TEXT
                current || enabled -> p.TEXT_DIM
                else -> p.TEXT_MUTED
            }
            p.textFit(g, ExoticChemicalDisplay.recipeMode(distort).string, x, 57, 63, 14, color)

            if (enabled && !current) {
                hots += Hot(x, 57, 63, 14, { edit("recipe_type", 0L, distort) })
            } else if (!infinite) {
                hots += Hot(x, 57, 63, 14, null, tooltip = { listOf(label("recipe.unlock")) })
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawNexusParallel(g: GuiGraphics, boost: Component?) {
        val p = ExoticChemicalPainter
        p.well(g, 171, 111, 126, if (boost == null) 18 else 34, 0xFF121820.toInt())

        val value = Component.literal(if (infinite) "∞" else FormattingUtil.formatNumbers(capacity))
        val parallel = label("parallel")
        val maxValWidth = 114 - minOf(p.font.width(parallel), 44) - 4
        val valWidth = minOf(p.font.width(value), maxValWidth)
        fitText(g, parallel, 177, 116, 114 - valWidth - 4, p.TEXT_DIM)
        fitTextRight(g, value, 291, 116, valWidth, p.ACCENT_TEXT)

        if (boost != null) {
            p.rect(g, 177, 128, 114, 1, 0x12FFFFFF)
            fitText(g, boost, 177, 132, 114, p.DISTORT_TEXT)
        }
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawParallelControls(g: GuiGraphics, mx: Int, my: Int) {
        val p = ExoticChemicalPainter
        val limitWidth = p.text(g, label("limit"), 172, 111, p.TEXT_DIM)
        val limitStatus = when {
            !infinite -> label("limit_capacity", FormattingUtil.formatNumbers(capacity))
            unlimited -> label("limit_unlimited")
            else -> label("limit_manual")
        }
        fitTextRight(g, limitStatus, 296, 111, 296 - (172 + limitWidth + 4), p.TEXT_MUTED)

        val locked = unlimited || !limitApplies
        val value = input.toLongOrNull() ?: 1L
        val step = step()
        val minusOff = locked || value <= 1L
        val plusOff = locked || value >= limitMax

        p.button(g, 171, LIMIT_Y, 22, 16, null, disabled = minusOff, hovered = over(mx, my, 171, LIMIT_Y, 22, 16))
        p.textFit(g, "-$step", 171, LIMIT_Y, 22, 16, if (minusOff) p.TEXT_MUTED else p.TEXT)
        p.well(g, 193, LIMIT_Y, 82, 16, if (locked) p.BTN_DISABLED else 0xFF0E1319.toInt())
        p.button(g, 275, LIMIT_Y, 22, 16, null, disabled = plusOff, hovered = over(mx, my, 275, LIMIT_Y, 22, 16))
        p.textFit(g, "+$step", 275, LIMIT_Y, 22, 16, if (plusOff) p.TEXT_MUTED else p.TEXT)

        if (!minusOff) hots += Hot(171, LIMIT_Y, 22, 16, { nudge(-1) })
        if (!plusOff) hots += Hot(275, LIMIT_Y, 22, 16, { nudge(1) })
        if (unlimited) p.textCenter(g, Component.literal("∞"), 234, p.ty(LIMIT_Y, 16), p.TEXT_MUTED)

        val limitAll = selectedView.limitAll
        fitText(g, label("limit_all"), 172, 142, 97, p.TEXT)
        p.switch(g, 273, 140, limitAll, !limitApplies)
        if (limitApplies) {
            hots += Hot(273, 140, p.SWITCH_W, 11, {
                if (pendingSince >= 0L) submit()
                edit("limit_all", 0L, !limitAll)
            })
        }
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
        val barLabel = if (v.duration > 0) ExoticChemicalDisplay.progressLabel(v.progress, v.duration) else null
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

        p.button(
            g,
            219,
            180,
            78,
            16,
            label("suspend_after_finish"),
            "clock",
            active = after,
            disabled = stopped,
            hovered = over(mx, my, 219, 180, 78, 16)
        )
        if (!stopped) {
            hots += Hot(219, 180, 78, 16, { edit("suspend_after_finish", 0L, !after) })
        }
    }

    @OnlyIn(Dist.CLIENT)
    private fun drawFieldValue(g: GuiGraphics) {
        val p = ExoticChemicalPainter
        val x = position.x + 193
        val y = position.y + LIMIT_Y
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

    private fun distorterText(view: UnitView): MutableComponent? = if (view.distortRecipes && distorterMultiplier > 1.0) {
        label("distorter", distorterMultiplier, TSD_COST_COEFFICIENT)
    } else {
        null
    }

    private fun unitName(id: Int): MutableComponent = label("unit.$id")

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
        var distortRecipes = false
        var state = ExoticChemicalUnitState.IDLE
        var progress = 0
        var duration = 0
        var limitAll = false
        var limit = 1L

        fun update(
            distortRecipes: Boolean,
            state: ExoticChemicalUnitState,
            duration: Int,
            limitAll: Boolean,
            limit: Long
        ): Boolean {
            val changed = distortRecipes != this.distortRecipes ||
                state != this.state ||
                duration != this.duration ||
                limitAll != this.limitAll ||
                limit != this.limit
            this.distortRecipes = distortRecipes
            this.state = state
            this.duration = duration
            this.limitAll = limitAll
            this.limit = limit
            return changed
        }

        fun write(buf: FriendlyByteBuf) {
            buf.writeBoolean(distortRecipes)
            buf.writeByte(state.ordinal)
            buf.writeVarInt(progress)
            buf.writeVarInt(duration)
            buf.writeBoolean(limitAll)
            buf.writeVarLong(limit)
        }

        fun read(buf: FriendlyByteBuf) {
            distortRecipes = buf.readBoolean()
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
            setNumbersOnly(1L, Long.MAX_VALUE)
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
        const val LIMIT_Y = 121
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