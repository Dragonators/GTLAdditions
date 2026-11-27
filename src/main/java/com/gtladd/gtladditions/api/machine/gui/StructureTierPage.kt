package com.gtladd.gtladditions.api.machine.gui

import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.feature.IStructureTierMachine
import com.gtladd.gtladditions.client.gui.ExoticChemicalPainter
import com.gtladd.gtladditions.client.gui.StructureTierRenderer
import com.gtladd.gtladditions.common.machine.trait.StructureTierTrait
import com.gtladd.gtladditions.utils.ComponentExtensions.literal
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.lowdragmc.lowdraglib.LDLib
import com.lowdragmc.lowdraglib.gui.ingredient.IIngredientSlot
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.jei.JEIPlugin
import dev.emi.emi.api.stack.EmiStack
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.roundToInt

class StructureTierPage(private val machine: IStructureTierMachine) : IFancyUIProvider {

    override fun createMainPage(widget: FancyMachineUIWidget): Widget = TierScreenWidget(machine)

    override fun getTabIcon(): IGuiTexture = ICON

    override fun getTitle(): Component = "gtladditions.structure_tier.title".toComponent

    override fun getTabTooltips(): List<Component> = listOf(
        title,
        "gtladditions.structure_tier.tooltip.0".toComponent.withStyle(ChatFormatting.GRAY),
        "gtladditions.structure_tier.tooltip.1".toComponent.withStyle(ChatFormatting.GRAY)
    )

    private class TierScreenWidget(private val machine: IStructureTierMachine) :
        Widget(0, 0, WIDTH, pageHeight(machine)),
        IIngredientSlot {

        private val tiers = machine.structureTierTrait
        private val wellH = size.height - WELL_Y - (BASE_HEIGHT - BASE_WELL_Y - BASE_WELL_H)
        private val actionY = WELL_Y + wellH + 3

        private var unlockedTier = tiers.unlockedTier
        private var structureTier = tiers.structureTier
        private var hasPendingTier = tiers.hasPendingTier

        private var selectedTier = minOf(unlockedTier + 1, tiers.layout.maxTier)
            set(value) {
                if (field != value) {
                    field = value
                    requirement = tiers.requirement(value)
                }
            }

        private var sentTier = selectedTier
        private var requirement = tiers.requirement(selectedTier)

        private val costCount: Int
            get() = minOf(COSTS, requirement?.costs?.size ?: 0)

        private val paid = LongArray(COSTS)
        private val conditionViews = ArrayList<ConditionView>()
        private val shownStacks = Array(COSTS) { ItemStack.EMPTY }
        private var layout = computeGridLayout(0)

        private var hasRendered = false
        private var pendingAction: Action? = null
        private var nextBtnX = 160

        // ========================================
        // SYNCHRONIZATION
        // ========================================

        private fun syncConditions(force: Boolean): Boolean {
            val conditions = requirement?.conditions.orEmpty()
            var changed = force || conditionViews.size != conditions.size

            if (conditionViews.size != conditions.size) {
                conditionViews.clear()
                for (i in conditions.indices) {
                    val c = conditions[i]
                    conditionViews.add(ConditionView(c.test(machine), c.describe(machine)))
                }

                layout = computeGridLayout(conditionViews.size)
                return true
            }

            for (i in conditions.indices) {
                val c = conditions[i]
                val met = c.test(machine)
                val text = c.describe(machine)
                val view = conditionViews[i]

                if (view.met != met || view.text != text) {
                    view.met = met
                    view.text = text
                    changed = true
                }
            }

            return changed
        }

        private fun writeTiers(buf: FriendlyByteBuf, changedCosts: Int, sendConditions: Boolean) {
            sentTier = selectedTier
            unlockedTier = tiers.unlockedTier
            structureTier = tiers.structureTier
            hasPendingTier = tiers.hasPendingTier

            buf.writeVarInt(selectedTier)
            buf.writeVarInt(unlockedTier)
            buf.writeVarInt(structureTier)
            buf.writeBoolean(hasPendingTier)

            buf.writeVarInt(changedCosts)
            for (i in 0 until costCount) {
                if (changedCosts and (1 shl i) == 0) continue
                paid[i] = tiers.paid(selectedTier, i)
                buf.writeVarLong(paid[i])
            }

            buf.writeBoolean(sendConditions)
            if (sendConditions) {
                buf.writeVarInt(conditionViews.size)
                for (i in conditionViews.indices) {
                    val view = conditionViews[i]
                    buf.writeBoolean(view.met)
                    buf.writeComponent(view.text)
                }
            }
        }

        private fun readTiers(buf: FriendlyByteBuf) {
            selectedTier = buf.readVarInt()
            unlockedTier = buf.readVarInt()
            structureTier = buf.readVarInt()
            hasPendingTier = buf.readBoolean()

            val changedCosts = buf.readVarInt()
            for (i in paid.indices) {
                if (changedCosts and (1 shl i) != 0) {
                    paid[i] = buf.readVarLong()
                }
            }

            if (buf.readBoolean()) {
                conditionViews.clear()
                repeat(buf.readVarInt()) {
                    conditionViews.add(ConditionView(buf.readBoolean(), buf.readComponent()))
                }
                layout = computeGridLayout(conditionViews.size)
            }
        }

        override fun writeInitialData(buffer: FriendlyByteBuf) {
            syncConditions(true)
            writeTiers(buffer, (1 shl costCount) - 1, true)
            super.writeInitialData(buffer)
        }

        @OnlyIn(Dist.CLIENT)
        override fun readInitialData(buffer: FriendlyByteBuf) {
            readTiers(buffer)
            super.readInitialData(buffer)
        }

        override fun detectAndSendChanges() {
            val tierChanged = sentTier != selectedTier
            var changedCosts = 0
            for (i in 0 until costCount) {
                if (tierChanged || paid[i] != tiers.paid(selectedTier, i)) {
                    changedCosts = changedCosts or (1 shl i)
                }
            }

            val conditionsChanged = syncConditions(tierChanged)
            if (changedCosts != 0 ||
                conditionsChanged ||
                tierChanged ||
                unlockedTier != tiers.unlockedTier ||
                structureTier != tiers.structureTier ||
                hasPendingTier != tiers.hasPendingTier
            ) {
                writeUpdateInfo(UPDATE_TIERS) { writeTiers(it, changedCosts, conditionsChanged) }
            }

            super.detectAndSendChanges()
        }

        @OnlyIn(Dist.CLIENT)
        override fun readUpdateInfo(id: Int, buffer: FriendlyByteBuf) {
            when (id) {
                UPDATE_TIERS -> readTiers(buffer)

                UPDATE_RESULT -> {
                    pendingAction = null
                    if (buffer.readBoolean()) StructureTierRenderer.highlight(buffer)
                }

                else -> super.readUpdateInfo(id, buffer)
            }
        }

        // ========================================
        // INPUT & ACTIONS
        // ========================================

        private fun enabled(action: Action, unlocked: Int, hasPending: Boolean): Boolean = when (action) {
            Action.PREVIOUS -> selectedTier > tiers.layout.minTier
            Action.NEXT -> selectedTier < tiers.layout.maxTier
            Action.UNLOCK -> selectedTier == unlocked + 1
            Action.ACTIVATE -> hasPending
        }

        @OnlyIn(Dist.CLIENT)
        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            // TabsWidget leaves its opening click unconsumed after resizing/recentering the GUI.
            if (!hasRendered) return true

            if (button == 0 && pendingAction == null) {
                val mx = mouseX - position.x
                val my = mouseY - position.y
                val clicked = when {
                    over(mx, my, PAD, TOP_Y, STEP_BTN_W, TOP_H) -> Action.PREVIOUS
                    over(mx, my, nextBtnX, TOP_Y, STEP_BTN_W, TOP_H) -> Action.NEXT
                    over(mx, my, PAD, actionY, ACTION_W, ACTION_H) -> Action.UNLOCK
                    over(mx, my, ACTION_RIGHT_X, actionY, ACTION_W, ACTION_H) -> Action.ACTIVATE
                    else -> null
                }

                if (clicked != null && enabled(clicked, unlockedTier, hasPendingTier)) {
                    pendingAction = clicked
                    writeClientAction(CLICK_ACTION) { it.writeVarInt(clicked.ordinal) }
                    playButtonClickSound()
                    return true
                }
            }

            return super.mouseClicked(mouseX, mouseY, button)
        }

        override fun handleClientAction(id: Int, buffer: FriendlyByteBuf) {
            if (id != CLICK_ACTION) return super.handleClientAction(id, buffer)

            val action = Action.entries.getOrNull(buffer.readVarInt()) ?: return
            val player = gui.entityPlayer as? ServerPlayer ?: return
            val world = player.serverLevel()
            val controller = tiers.machine
            val pos = controller.pos

            var activation: StructureTierTrait.Activation? = null
            if (player.isAlive &&
                !player.isSpectator &&
                world === controller.level &&
                (world.getBlockEntity(pos) as? IMachineBlockEntity)?.metaMachine === controller &&
                player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0 &&
                enabled(action, tiers.unlockedTier, tiers.hasPendingTier)
            ) {
                when (action) {
                    Action.PREVIOUS -> selectedTier--

                    Action.NEXT -> selectedTier++

                    Action.UNLOCK -> if (world.mayInteract(player, pos)) {
                        tiers.unlock(player).forEach(player::sendSystemMessage)
                        if (tiers.unlockedTier == selectedTier) {
                            selectedTier = minOf(selectedTier + 1, tiers.layout.maxTier)
                        }
                    }

                    Action.ACTIVATE -> if (world.mayInteract(player, pos) && world.gameTime >= tiers.nextGuiActivationTick) {
                        // Throttle full-structure block scans to once per second.
                        tiers.nextGuiActivationTick = world.gameTime + SCAN_COOLDOWN_TICKS
                        activation = tiers.activate(player.blockPosition())
                        activation.messages.forEach(player::sendSystemMessage)
                    }
                }

                detectAndSendChanges()
            }

            writeUpdateInfo(UPDATE_RESULT) { buf ->
                buf.writeBoolean(activation != null)
                if (activation != null) {
                    buf.writeVarInt(activation.cells.size)
                    for (cell in activation.cells) {
                        buf.writeLong(cell.asLong())
                    }

                    buf.writeVarInt(activation.chunks.size)
                    for (box in activation.chunks) {
                        buf.writeDouble(box.minX)
                        buf.writeDouble(box.minY)
                        buf.writeDouble(box.minZ)
                        buf.writeDouble(box.maxX)
                        buf.writeDouble(box.maxY)
                        buf.writeDouble(box.maxZ)
                    }
                }
            }
        }

        // ========================================
        // RENDERING & XEI
        // ========================================

        @OnlyIn(Dist.CLIENT)
        private fun updateShownStacks() {
            val costs = requirement?.costs
            if (costs.isNullOrEmpty()) {
                shownStacks.fill(ItemStack.EMPTY)
                return
            }

            val second = System.currentTimeMillis() / 1000L
            for (i in 0 until COSTS) {
                val items = costs.getOrNull(i)?.ingredient?.items
                shownStacks[i] = if (items.isNullOrEmpty()) {
                    ItemStack.EMPTY
                } else {
                    items[(second % items.size).toInt()]
                }
            }
        }

        @OnlyIn(Dist.CLIENT)
        override fun drawInBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
            super.drawInBackground(graphics, mouseX, mouseY, partialTicks)
            updateShownStacks()

            val p = ExoticChemicalPainter
            val ox = position.x
            val oy = position.y
            val mx = (mouseX - ox).toDouble()
            val my = (mouseY - oy).toDouble()

            gPush(graphics, ox, oy) {
                p.plate(
                    graphics,
                    0,
                    0,
                    WIDTH,
                    size.height,
                    p.PANEL_A,
                    p.PANEL_B,
                    p.PANEL_BORDER,
                    p.PANEL_HI,
                    p.PANEL_LO,
                    1
                )
                drawTopBar(graphics, mx, my)
                drawMiddleWell(graphics, mx, my)
                drawBottomActions(graphics, mx, my)
            }

            hasRendered = true
        }

        @OnlyIn(Dist.CLIENT)
        private inline fun gPush(g: GuiGraphics, ox: Int, oy: Int, block: () -> Unit) {
            g.pose().pushPose()
            g.pose().translate(ox.toFloat(), oy.toFloat(), 0f)
            block()
            g.pose().popPose()
        }

        @OnlyIn(Dist.CLIENT)
        private fun drawTopBar(g: GuiGraphics, mx: Double, my: Double) {
            val p = ExoticChemicalPainter
            val maxTier = tiers.layout.maxTier
            val badgeText = "gtladditions.structure_tier.status".toComponent(
                structureTier,
                maxTier,
                unlockedTier,
                maxTier
            )
            val badgeW = (p.font.width(badgeText) + 8).coerceIn(76, 140)
            val badgeX = WIDTH - PAD - badgeW
            nextBtnX = badgeX - 4 - STEP_BTN_W

            // Chamfered status capsule badge matching adjacent controls
            p.plate(g, badgeX, TOP_Y, badgeW, TOP_H, BADGE_BG, BADGE_BG, BADGE_BORDER, BADGE_BORDER, BADGE_BORDER, 1)
            fitTextCenter(g, badgeText, badgeX + 3, TOP_Y, badgeW - 6, TOP_H, BADGE_TEXT)

            // Left / Right stepper buttons
            val prevEnabled = enabled(Action.PREVIOUS, unlockedTier, hasPendingTier)
            val nextEnabled = enabled(Action.NEXT, unlockedTier, hasPendingTier)
            drawActionButton(
                g,
                PAD,
                TOP_Y,
                STEP_BTN_W,
                TOP_H,
                "<".literal,
                accent = false,
                enabled = prevEnabled,
                pressed = pendingAction == Action.PREVIOUS,
                hovered = pendingAction == null && over(mx, my, PAD, TOP_Y, STEP_BTN_W, TOP_H)
            )
            drawActionButton(
                g,
                nextBtnX,
                TOP_Y,
                STEP_BTN_W,
                TOP_H,
                ">".literal,
                accent = false,
                enabled = nextEnabled,
                pressed = pendingAction == Action.NEXT,
                hovered = pendingAction == null && over(mx, my, nextBtnX, TOP_Y, STEP_BTN_W, TOP_H)
            )

            // Center recessed readout well
            val readoutX = PAD + STEP_BTN_W + 2
            val readoutW = nextBtnX - 2 - readoutX
            p.well(g, readoutX, TOP_Y, readoutW, TOP_H, READOUT_BG, 1)

            val tierLabel = "gtladditions.structure_tier.selected".toComponent(selectedTier, maxTier)
            val (stateKey, stateColor) = when {
                selectedTier <= structureTier -> "active" to STATE_ACTIVE
                selectedTier <= unlockedTier -> "unlocked" to STATE_UNLOCKED
                selectedTier == unlockedTier + 1 -> "next" to STATE_NEXT
                else -> "locked" to STATE_LOCKED
            }
            val stateLabel = "gtladditions.structure_tier.state.$stateKey".toComponent

            val innerX = readoutX + 4
            val innerW = (readoutW - 8).coerceAtLeast(16)
            val leftW = p.font.width(tierLabel)
            val rightW = p.font.width(stateLabel)
            val needed = leftW + rightW + 4

            if (needed <= innerW) {
                val ty = p.ty(TOP_Y, TOP_H)
                p.text(g, tierLabel, innerX, ty, p.TEXT)
                p.text(g, stateLabel, innerX + innerW - rightW, ty, stateColor)
            } else {
                val k = innerW.toFloat() / needed
                val baseY = TOP_Y + (TOP_H - 7f * k) / 2f

                g.pose().pushPose()
                g.pose().translate(innerX.toFloat(), baseY, 0f)
                g.pose().scale(k, k, 1f)
                g.drawString(p.font, tierLabel, 0, 0, p.TEXT, false)
                g.pose().popPose()

                g.pose().pushPose()
                g.pose().translate(innerX + innerW - rightW * k, baseY, 0f)
                g.pose().scale(k, k, 1f)
                g.drawString(p.font, stateLabel, 0, 0, stateColor, false)
                g.pose().popPose()
            }
        }

        @OnlyIn(Dist.CLIENT)
        private fun drawMiddleWell(g: GuiGraphics, mx: Double, my: Double) {
            val p = ExoticChemicalPainter
            p.rect(g, WELL_X, WELL_Y, WELL_W, wellH, WELL_BG)
            p.frame(g, WELL_X, WELL_Y, WELL_W, wellH, WELL_BORDER)
            p.rect(g, WELL_X + 1, WELL_Y + 1, WELL_W - 2, 1, 0x38000000)

            val req = requirement
            if (req == null) {
                p.textCenter(
                    g,
                    "gtladditions.structure_tier.free".toComponent,
                    WIDTH / 2,
                    p.ty(WELL_Y, wellH),
                    p.TEXT_DIM
                )
                return
            }

            for (i in 0 until COSTS) {
                val cx = GRID_X + i % GRID_COLS * (CARD_W + COL_GAP)
                val cy = layout.gridY + i / GRID_COLS * (layout.cardH + layout.rowGap)
                if (i < costCount) {
                    drawCostCard(
                        g,
                        cx,
                        cy,
                        layout.cardH,
                        i,
                        req.costs[i].amount,
                        over(mx, my, cx, cy, CARD_W, layout.cardH)
                    )
                } else {
                    p.rect(g, cx, cy, CARD_W, layout.cardH, EMPTY_CARD_BG)
                    p.frame(g, cx, cy, CARD_W, layout.cardH, EMPTY_CARD_BORDER)
                }
            }

            if (conditionViews.isNotEmpty()) {
                val gridBottom = layout.gridY + GRID_ROWS * layout.cardH + (GRID_ROWS - 1) * layout.rowGap
                val divY = gridBottom + 1
                p.rect(g, GRID_X, divY, WELL_W - 8, 1, DIVIDER_COLOR)

                val startY = if (conditionViews.size == 1) {
                    p.ty(divY + 1, WELL_Y + wellH - 2 - divY)
                } else {
                    divY + 2
                }

                for (i in conditionViews.indices) {
                    val view = conditionViews[i]
                    val cy = startY + i * COND_LINE_H
                    val mark = if (view.met) "✔ " else "✘ "
                    val markColor = if (view.met) STATE_ACTIVE else COST_UNPAID
                    val textColor = if (view.met) COND_MET_TEXT else COND_UNMET_TEXT

                    val markW = p.text(g, mark, GRID_X + 2, cy, markColor)
                    fitTextLeft(g, view.text, GRID_X + 2 + markW, cy, WELL_W - 12 - markW, textColor)
                }
            }
        }

        @OnlyIn(Dist.CLIENT)
        private fun drawCostCard(
            g: GuiGraphics,
            cx: Int,
            cy: Int,
            cardH: Int,
            index: Int,
            requiredAmount: Long,
            hovered: Boolean
        ) {
            val p = ExoticChemicalPainter
            val amountPaid = paid[index]
            val done = amountPaid >= requiredAmount
            val baseBg = if (done) CARD_DONE_BG else CARD_BG
            val bg = if (hovered) p.lightness(baseBg, 1.12f) else baseBg
            val border = when {
                done && hovered -> CARD_DONE_BORDER_HI
                done -> CARD_DONE_BORDER
                hovered -> CARD_BORDER_HI
                else -> CARD_BORDER
            }

            p.rect(g, cx, cy, CARD_W, cardH, bg)
            p.frame(g, cx, cy, CARD_W, cardH, border)
            p.rect(g, cx + 1, cy + 1, CARD_W - 2, 1, 0x12FFFFFF)

            // 1:1 16x16 integer-aligned item icon
            val upperH = cardH - 5
            val stack = shownStacks[index]
            if (!stack.isEmpty) {
                g.renderItem(stack, cx + 2, cy + 1 + (upperH - 16).coerceAtLeast(0) / 2)
            }

            val statusText = if (done) "✔" else "x${requiredAmount - amountPaid}"
            val statusColor = when {
                done -> STATE_ACTIVE
                amountPaid > 0L -> COST_PARTIAL_TEXT
                else -> COST_UNPAID
            }
            fitStringRight(g, statusText, cx + CARD_W - 4, p.ty(cy + 1, upperH), CARD_W - 23, statusColor)

            // 2px inner bottom progress bar with symmetric 1px margin on left, right, and bottom
            val barX = cx + 2
            val barY = cy + cardH - 4
            val barW = CARD_W - 4
            p.rect(g, barX, barY, barW, 2, BAR_TRACK_BG)

            if (amountPaid > 0L && requiredAmount > 0L) {
                val fillW = if (done) {
                    barW
                } else {
                    (barW * (amountPaid.toDouble() / requiredAmount)).roundToInt().coerceIn(1, barW - 1)
                }
                p.rect(g, barX, barY, fillW, 2, if (done) STATE_ACTIVE else STATE_UNLOCKED)
            }
        }

        @OnlyIn(Dist.CLIENT)
        private fun drawBottomActions(g: GuiGraphics, mx: Double, my: Double) {
            val unlockEnabled = enabled(Action.UNLOCK, unlockedTier, hasPendingTier)
            val unlockLabel = when {
                selectedTier == unlockedTier + 1 && gui.entityPlayer?.isCreative == true ->
                    "gtladditions.structure_tier.button.unlock_creative".toComponent
                selectedTier == unlockedTier + 1 -> "gtladditions.structure_tier.button.unlock".toComponent
                selectedTier <= unlockedTier -> "gtladditions.structure_tier.button.unlocked".toComponent
                else -> "gtladditions.structure_tier.button.locked".toComponent
            }
            drawActionButton(
                g,
                PAD,
                actionY,
                ACTION_W,
                ACTION_H,
                unlockLabel,
                accent = unlockEnabled,
                enabled = unlockEnabled,
                pressed = pendingAction == Action.UNLOCK,
                hovered = pendingAction == null && over(mx, my, PAD, actionY, ACTION_W, ACTION_H)
            )

            val activateEnabled = enabled(Action.ACTIVATE, unlockedTier, hasPendingTier)
            val activateLabel = if (hasPendingTier) {
                "gtladditions.structure_tier.button.activate_tier".toComponent(unlockedTier)
            } else {
                "gtladditions.structure_tier.button.activate".toComponent
            }
            drawActionButton(
                g,
                ACTION_RIGHT_X,
                actionY,
                ACTION_W,
                ACTION_H,
                activateLabel,
                accent = activateEnabled,
                enabled = activateEnabled,
                pressed = pendingAction == Action.ACTIVATE,
                hovered = pendingAction == null && over(mx, my, ACTION_RIGHT_X, actionY, ACTION_W, ACTION_H)
            )
        }

        @OnlyIn(Dist.CLIENT)
        private fun drawActionButton(
            g: GuiGraphics,
            x: Int,
            y: Int,
            w: Int,
            h: Int,
            label: Component,
            accent: Boolean,
            enabled: Boolean,
            pressed: Boolean,
            hovered: Boolean
        ) {
            val p = ExoticChemicalPainter
            when {
                !enabled && !pressed -> p.well(g, x, y, w, h, p.BTN_DISABLED, 1)

                pressed && accent -> p.plate(
                    g,
                    x,
                    y,
                    w,
                    h,
                    p.BTN_ACT_B,
                    p.BTN_ACT_LO,
                    p.BTN_BORDER,
                    p.BTN_ACT_LO,
                    p.BTN_ACT_HI
                )

                pressed -> p.plate(g, x, y, w, h, p.BTN_B, p.BTN_LO, p.BTN_BORDER, p.BTN_LO, p.BTN_HI)

                accent && hovered -> p.plate(
                    g,
                    x,
                    y,
                    w,
                    h,
                    p.lightness(p.BTN_ACT_A, 1.14f),
                    p.lightness(p.BTN_ACT_B, 1.14f),
                    p.BTN_BORDER,
                    p.BTN_ACT_HI,
                    p.BTN_ACT_LO
                )

                accent -> p.plate(g, x, y, w, h, p.BTN_ACT_A, p.BTN_ACT_B, p.BTN_BORDER, p.BTN_ACT_HI, p.BTN_ACT_LO)

                hovered -> p.plate(
                    g,
                    x,
                    y,
                    w,
                    h,
                    p.lightness(p.BTN_A, 1.12f),
                    p.lightness(p.BTN_B, 1.12f),
                    p.BTN_BORDER,
                    p.BTN_HI,
                    p.BTN_LO
                )

                else -> p.plate(g, x, y, w, h, p.BTN_A, p.BTN_B, p.BTN_BORDER, p.BTN_HI, p.BTN_LO)
            }

            val color = when {
                !enabled && !pressed -> p.TEXT_MUTED
                accent -> p.ACCENT_TEXT
                else -> p.TEXT
            }
            fitTextCenter(g, label, x + 4, y + if (pressed) 1 else 0, w - 8, h, color)
        }

        @OnlyIn(Dist.CLIENT)
        private fun fitTextCenter(g: GuiGraphics, text: Component, x: Int, y: Int, w: Int, h: Int, color: Int) {
            val p = ExoticChemicalPainter
            val tw = p.font.width(text)
            if (tw <= w) {
                p.text(g, text, x + (w - tw + 1) / 2, p.ty(y, h), color)
                return
            }

            val k = w.toFloat() / tw
            g.pose().pushPose()
            g.pose().translate(x.toFloat(), y + (h - 7f * k) / 2f, 0f)
            g.pose().scale(k, k, 1f)
            g.drawString(p.font, text, 0, 0, color, false)
            g.pose().popPose()
        }

        @OnlyIn(Dist.CLIENT)
        private fun fitTextLeft(g: GuiGraphics, text: Component, x: Int, y: Int, maxW: Int, color: Int) {
            val p = ExoticChemicalPainter
            val tw = p.font.width(text)
            if (tw <= maxW || maxW <= 0) {
                p.text(g, text, x, y, color)
                return
            }

            val k = maxW.toFloat() / tw
            g.pose().pushPose()
            g.pose().translate(x.toFloat(), y + 3.5f * (1f - k), 0f)
            g.pose().scale(k, k, 1f)
            g.drawString(p.font, text, 0, 0, color, false)
            g.pose().popPose()
        }

        @OnlyIn(Dist.CLIENT)
        private fun fitStringRight(g: GuiGraphics, text: String, right: Int, y: Int, maxW: Int, color: Int) {
            val p = ExoticChemicalPainter
            val tw = p.font.width(text)
            if (tw <= maxW) {
                p.text(g, text, right - tw, y, color)
                return
            }

            val k = maxW.toFloat() / tw
            g.pose().pushPose()
            g.pose().translate(right - maxW.toFloat(), y + 3.5f * (1f - k), 0f)
            g.pose().scale(k, k, 1f)
            g.drawString(p.font, text, 0, 0, color, false)
            g.pose().popPose()
        }

        @OnlyIn(Dist.CLIENT)
        override fun drawInForeground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
            super.drawInForeground(graphics, mouseX, mouseY, partialTicks)

            val req = requirement ?: return
            val index = cardAt(mouseX - position.x.toDouble(), mouseY - position.y.toDouble())
            if (index !in 0 until costCount) return
            val stack = shownStacks[index]
            if (stack.isEmpty) return

            val required = req.costs[index].amount
            val amountPaid = paid[index].coerceAtMost(required)
            val remaining = (required - amountPaid).coerceAtLeast(0L)
            val paidStr = FormattingUtil.formatNumbers(amountPaid)
            val totalStr = FormattingUtil.formatNumbers(required)

            val lines = ArrayList(DrawerHelper.getItemToolTip(stack))
            lines += if (remaining == 0L) {
                "gtladditions.structure_tier.progress.done".toComponent(paidStr, totalStr)
                    .withStyle(ChatFormatting.GREEN)
            } else {
                "gtladditions.structure_tier.progress.remaining".toComponent(
                    paidStr,
                    totalStr,
                    FormattingUtil.formatNumbers(remaining)
                ).withStyle(ChatFormatting.GRAY)
            }

            gui.modularUIGui.setHoverTooltip(lines, stack, null, null)
        }

        @OnlyIn(Dist.CLIENT)
        override fun getXEIIngredientOverMouse(mouseX: Double, mouseY: Double): Any? {
            if (!hasRendered || requirement == null) return null

            val index = cardAt(mouseX - position.x, mouseY - position.y)
            if (index !in 0 until costCount) return null
            val stack = shownStacks[index]
            if (stack.isEmpty) return null

            val cardX = position.x + GRID_X + index % GRID_COLS * (CARD_W + COL_GAP)
            val cardY = position.y + layout.gridY + index / GRID_COLS * (layout.cardH + layout.rowGap)

            return when {
                LDLib.isJeiLoaded() -> JEIPlugin.getItemIngredient(stack, cardX, cardY, CARD_W, layout.cardH)
                LDLib.isEmiLoaded() -> EmiStack.of(stack)
                else -> stack
            }
        }

        // ========================================
        // GEOMETRY HELPERS
        // ========================================

        private fun computeGridLayout(condCount: Int): GridLayout {
            if (condCount == 0) {
                val gridH = GRID_ROWS * CARD_H_FULL + (GRID_ROWS - 1) * ROW_GAP_FULL
                return GridLayout(WELL_Y + (wellH - gridH) / 2, CARD_H_FULL, ROW_GAP_FULL)
            }

            return GridLayout(WELL_Y + 2, CARD_H_COND, ROW_GAP_COND)
        }

        private fun cardAt(mx: Double, my: Double): Int {
            if (requirement == null) return -1

            val colStride = CARD_W + COL_GAP
            val rowStride = layout.cardH + layout.rowGap
            val rx = mx - GRID_X
            val ry = my - layout.gridY
            if (rx < 0 || ry < 0) return -1

            val col = (rx / colStride).toInt()
            val row = (ry / rowStride).toInt()
            if (col !in 0 until GRID_COLS || row !in 0 until GRID_ROWS) return -1
            if (rx - col * colStride >= CARD_W || ry - row * rowStride >= layout.cardH) return -1

            return row * GRID_COLS + col
        }

        private fun over(mx: Double, my: Double, x: Int, y: Int, w: Int, h: Int): Boolean =
            mx >= x && my >= y && mx < x + w && my < y + h
    }

    private class ConditionView(var met: Boolean, var text: Component)

    private data class GridLayout(val gridY: Int, val cardH: Int, val rowGap: Int)

    private enum class Action {
        PREVIOUS,
        NEXT,
        UNLOCK,
        ACTIVATE
    }

    companion object {
        private const val WIDTH = 284
        private const val BASE_HEIGHT = 144
        private const val PAD = 4

        private const val TOP_Y = 4
        private const val TOP_H = 15
        private const val STEP_BTN_W = 16

        private const val WELL_X = 4
        private const val BASE_WELL_Y = 21
        private const val WELL_Y = BASE_WELL_Y
        private const val WELL_W = WIDTH - PAD * 2
        private const val BASE_WELL_H = 102

        private const val COSTS = 16
        private const val GRID_COLS = 4
        private const val GRID_ROWS = 4
        private const val GRID_X = 8
        private const val CARD_W = 64
        private const val COL_GAP = 4
        private const val CARD_H_FULL = 22
        private const val ROW_GAP_FULL = 2
        private const val CARD_H_COND = 21
        private const val ROW_GAP_COND = 1
        private const val COND_LINE_H = 9

        private const val ACTION_H = 15
        private const val ACTION_W = (WELL_W - 4) / 2
        private const val ACTION_RIGHT_X = PAD + ACTION_W + 4

        private const val UPDATE_TIERS = 3
        private const val UPDATE_RESULT = 4
        private const val CLICK_ACTION = 3
        private const val SCAN_COOLDOWN_TICKS = 20

        private const val READOUT_BG = 0xFF111417.toInt()
        private const val BADGE_BG = 0xFF143832.toInt()
        private const val BADGE_BORDER = 0xFF2A8D7B.toInt()
        private const val BADGE_TEXT = 0xFF9CF6E4.toInt()

        private const val STATE_ACTIVE = 0xFF4CE07F.toInt()
        private const val STATE_UNLOCKED = 0xFF34D5B8.toInt()
        private const val STATE_NEXT = 0xFFF6B83B.toInt()
        private const val STATE_LOCKED = 0xFF858E96.toInt()

        private const val WELL_BG = 0xFF15171A.toInt()
        private const val WELL_BORDER = 0xFF2B3035.toInt()
        private const val DIVIDER_COLOR = 0xFF262B31.toInt()

        private const val CARD_BG = 0xFF202428.toInt()
        private const val CARD_BORDER = 0xFF343940.toInt()
        private const val CARD_BORDER_HI = 0xFF4B535D.toInt()
        private const val CARD_DONE_BG = 0xFF152721.toInt()
        private const val CARD_DONE_BORDER = 0xFF2A7A5B.toInt()
        private const val CARD_DONE_BORDER_HI = 0xFF39A37A.toInt()
        private const val EMPTY_CARD_BG = 0xFF121417.toInt()
        private const val EMPTY_CARD_BORDER = 0xFF1D2126.toInt()

        private const val BAR_TRACK_BG = 0xFF101214.toInt()
        private const val COST_PARTIAL_TEXT = 0xFFFFE299.toInt()
        private const val COST_UNPAID = 0xFFFF8080.toInt()
        private const val COND_MET_TEXT = 0xFFA1F5BE.toInt()
        private const val COND_UNMET_TEXT = 0xFFFFB0B0.toInt()

        private val ICON = ResourceTexture("gtladditions:textures/gui/structure_tier/icon.png")

        private fun pageHeight(machine: IStructureTierMachine): Int {
            val layout = machine.structureTierTrait.layout
            var maxConds = 0

            for (t in layout.minTier..layout.maxTier) {
                val count = machine.tierRequirement(t)?.conditions?.size ?: 0
                if (count > maxConds) maxConds = count
            }

            return BASE_HEIGHT + (maxConds - 1).coerceAtLeast(0) * COND_LINE_H
        }
    }
}
