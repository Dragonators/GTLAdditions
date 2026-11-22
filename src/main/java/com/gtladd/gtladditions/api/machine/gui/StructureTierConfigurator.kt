package com.gtladd.gtladditions.api.machine.gui

import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gtladd.gtladditions.api.machine.feature.IStructureTierMachine
import com.gtladd.gtladditions.client.gui.StructureTierRenderer
import com.gtladd.gtladditions.common.machine.trait.StructureTierTrait
import com.gtladd.gtladditions.utils.ComponentExtensions.literal
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.lowdragmc.lowdraglib.LDLib
import com.lowdragmc.lowdraglib.gui.ingredient.IIngredientSlot
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.jei.JEIPlugin
import dev.emi.emi.api.stack.EmiStack
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

class StructureTierConfigurator(private val machine: IStructureTierMachine) : IFancyConfigurator {
    override fun getTitle(): Component = "gtladditions.structure_tier.title".toComponent

    override fun getIcon(): IGuiTexture = ICON

    override fun getTooltips(): List<Component> = listOf(title, "gtladditions.structure_tier.tooltip".toComponent)

    override fun createConfigurator(): Widget = TierPage(machine)

    private class TierPage(private val machine: IStructureTierMachine) : WidgetGroup(0, 0, WIDTH, HEIGHT) {
        private val tiers = machine.structureTierTrait
        private var unlockedTier = tiers.unlockedTier
        private var structureTier = tiers.structureTier
        private var hasPendingTier = tiers.hasPendingTier
        private var selectedTier = minOf(unlockedTier + 1, tiers.layout.maxTier)
            set(value) {
                field = value
                requirement = tiers.requirement(value)
            }
        private var sentTier = selectedTier
        private var requirement = tiers.requirement(selectedTier)
        private val costCount get() = minOf(COSTS, requirement?.costs?.size ?: 0)
        private val paid = LongArray(COSTS)

        init {
            addWidget(text(4, 2) { it.add("gtladditions.structure_tier.status".toComponent(unlockedTier, tiers.layout.maxTier)) })
            addWidget(TierButton(4, 13, 14, Action.PREVIOUS))
            addWidget(text(22, 16) { it.add(selection()) })
            addWidget(TierButton(170, 13, 14, Action.NEXT))
            addWidget(
                text(4, 32) {
                    it.add(if (requirement == null) "gtladditions.structure_tier.free".toComponent else "gtladditions.structure_tier.required".toComponent)
                }
            )
            for (i in 0 until COSTS) addWidget(CostWidget(4 + i / COST_ROWS * 44, 44 + i % COST_ROWS * 18, i))
            addWidget(text(INPUT_X, 32) { it.add("gtladditions.structure_tier.inserted".toComponent) })

            for (slot in 0 until StructureTierTrait.SLOTS) {
                addWidget(
                    SlotWidget(tiers.inventory, slot, INPUT_X + slot % 4 * 18, 44 + slot / 4 * 18, true, true)
                        .setBackgroundTexture(GuiTextures.SLOT)
                )
            }

            addWidget(text(4, 122, ::conditions))
            addWidget(TierButton(4, 172, 126, Action.UNLOCK))
            addWidget(TierButton(134, 172, 126, Action.ACTIVATE))
        }

        private fun text(x: Int, y: Int, lines: (MutableList<Component>) -> Unit): Widget =
            ComponentPanelWidget(x, y) { if (!tiers.machine.isRemote) lines(it) }.setMaxWidthLimit(WIDTH - 8)

        private fun selection(): Component {
            val state = when {
                selectedTier <= structureTier -> "active"
                selectedTier <= unlockedTier -> "unlocked"
                selectedTier == unlockedTier + 1 -> "next"
                else -> "locked"
            }
            return "gtladditions.structure_tier.selected".toComponent(selectedTier, "gtladditions.structure_tier.state.$state".toComponent)
        }

        private fun conditions(list: MutableList<Component>) {
            for (condition in requirement?.conditions ?: return) {
                val met = condition.test(machine)
                list.add(
                    (if (met) "✔ " else "✘ ").literal
                        .withStyle(if (met) ChatFormatting.GREEN else ChatFormatting.RED)
                        .append(condition.describe(machine))
                )
            }
        }

        private fun writeTiers(buf: FriendlyByteBuf, changedCosts: Int) {
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
        }

        private fun readTiers(buf: FriendlyByteBuf) {
            selectedTier = buf.readVarInt()
            unlockedTier = buf.readVarInt()
            structureTier = buf.readVarInt()
            hasPendingTier = buf.readBoolean()
            val changedCosts = buf.readVarInt()
            for (i in paid.indices) {
                if (changedCosts and (1 shl i) != 0) paid[i] = buf.readVarLong()
            }
        }

        override fun writeInitialData(buffer: FriendlyByteBuf) {
            writeTiers(buffer, (1 shl costCount) - 1)
            super.writeInitialData(buffer)
        }

        @OnlyIn(Dist.CLIENT)
        override fun readInitialData(buffer: FriendlyByteBuf) {
            readTiers(buffer)
            super.readInitialData(buffer)
        }

        override fun detectAndSendChanges() {
            var changedCosts = 0
            for (i in 0 until costCount) {
                if (sentTier != selectedTier || paid[i] != tiers.paid(selectedTier, i)) changedCosts = changedCosts or (1 shl i)
            }
            if (changedCosts != 0 || sentTier != selectedTier || unlockedTier != tiers.unlockedTier || structureTier != tiers.structureTier ||
                hasPendingTier != tiers.hasPendingTier
            ) {
                writeUpdateInfo(UPDATE_TIERS) { writeTiers(it, changedCosts) }
            }
            super.detectAndSendChanges()
        }

        @OnlyIn(Dist.CLIENT)
        override fun readUpdateInfo(id: Int, buffer: FriendlyByteBuf) {
            if (id != UPDATE_TIERS) return super.readUpdateInfo(id, buffer)
            readTiers(buffer)
        }

        private inner class CostWidget(x: Int, y: Int, private val index: Int) :
            Widget(x, y, 42, 18),
            IIngredientSlot {
            private val cost get() = requirement?.costs?.getOrNull(index)
            private val stack
                get() = cost?.ingredient?.items?.takeIf { it.isNotEmpty() }?.let {
                    it[(System.currentTimeMillis() / 1000L % it.size).toInt()]
                }

            @OnlyIn(Dist.CLIENT)
            override fun drawInBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
                val cost = cost ?: return
                val stack = stack ?: return
                val pos = position
                GuiTextures.SLOT.draw(graphics, mouseX, mouseY, pos.x.toFloat(), pos.y.toFloat(), 18, 18)
                DrawerHelper.drawItemStack(graphics, stack, pos.x + 1, pos.y + 1, -1, null)
                val amountPaid = paid[index]
                val font = Minecraft.getInstance().font
                when {
                    amountPaid >= cost.amount -> graphics.drawString(font, "✔", pos.x + 22, pos.y + 5, 0x55FF55, true)
                    else -> graphics.drawString(
                        font,
                        "x${cost.amount - amountPaid}",
                        pos.x + 20,
                        pos.y + 5,
                        if (amountPaid == 0L) 0xFF5555 else 0xFFFF55,
                        true
                    )
                }
            }

            @OnlyIn(Dist.CLIENT)
            override fun drawInForeground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
                if (!isMouseOverElement(mouseX.toDouble(), mouseY.toDouble())) return
                val stack = stack ?: return
                gui.modularUIGui.setHoverTooltip(DrawerHelper.getItemToolTip(stack), stack, null, null)
            }

            @OnlyIn(Dist.CLIENT)
            override fun getXEIIngredientOverMouse(mouseX: Double, mouseY: Double): Any? {
                if (!isMouseOverElement(mouseX, mouseY)) return null
                val stack = stack ?: return null
                return when {
                    LDLib.isJeiLoaded() -> JEIPlugin.getItemIngredient(stack, position.x, position.y, 18, 18)
                    LDLib.isEmiLoaded() -> EmiStack.of(stack)
                    else -> stack
                }
            }
        }

        private inner class TierButton(x: Int, y: Int, width: Int, private val action: Action) : Widget(x, y, width, 14) {
            private var pending = false

            private fun enabled(unlocked: Int, hasPending: Boolean): Boolean = when (action) {
                Action.PREVIOUS -> selectedTier > tiers.layout.minTier
                Action.NEXT -> selectedTier < tiers.layout.maxTier
                Action.UNLOCK -> selectedTier == unlocked + 1
                Action.ACTIVATE -> hasPending
            }

            @OnlyIn(Dist.CLIENT)
            override fun drawInBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
                StructureTierRenderer.drawButton(
                    graphics, action.label, position.x, position.y, size.width, size.height,
                    enabled(unlockedTier, hasPendingTier), pending, isMouseOverElement(mouseX.toDouble(), mouseY.toDouble())
                )
            }

            @OnlyIn(Dist.CLIENT)
            override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
                if (button != 0 || !isMouseOverElement(mouseX, mouseY) || pending || !enabled(unlockedTier, hasPendingTier)) return false
                pending = true
                writeClientAction(CLICK) {}
                playButtonClickSound()
                return true
            }

            override fun handleClientAction(id: Int, buffer: FriendlyByteBuf) {
                if (id != CLICK) return super.handleClientAction(id, buffer)
                val player = gui.entityPlayer as? ServerPlayer ?: return
                val world = player.serverLevel()
                val controller = tiers.machine
                val pos = controller.pos
                var activation: StructureTierTrait.Activation? = null
                if (player.isAlive && !player.isSpectator && world === controller.level &&
                    (world.getBlockEntity(pos) as? IMachineBlockEntity)?.metaMachine === controller &&
                    player.containerMenu.stillValid(player) && player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0 &&
                    enabled(tiers.unlockedTier, tiers.hasPendingTier)
                ) {
                    when (action) {
                        Action.PREVIOUS -> selectedTier--
                        Action.NEXT -> selectedTier++
                        Action.UNLOCK -> if (world.mayInteract(player, pos)) {
                            tiers.unlock().forEach(player::sendSystemMessage)
                            if (tiers.unlockedTier == selectedTier) selectedTier = minOf(selectedTier + 1, tiers.layout.maxTier)
                        }
                        Action.ACTIVATE -> if (world.mayInteract(player, pos) && world.gameTime >= tiers.nextGuiActivationTick) {
                            tiers.nextGuiActivationTick = world.gameTime + 20
                            activation = tiers.activate(player.blockPosition())
                            activation.messages.forEach(player::sendSystemMessage)
                        }
                    }
                    this@TierPage.detectAndSendChanges()
                }

                writeUpdateInfo(RESULT) { buf ->
                    buf.writeBoolean(activation != null)
                    if (activation != null) {
                        buf.writeVarInt(activation.cells.size)
                        for (cell in activation.cells) buf.writeLong(cell.asLong())
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

            @OnlyIn(Dist.CLIENT)
            override fun readUpdateInfo(id: Int, buffer: FriendlyByteBuf) {
                if (id != RESULT) return super.readUpdateInfo(id, buffer)
                pending = false
                if (buffer.readBoolean()) StructureTierRenderer.highlight(buffer)
            }
        }
    }

    private enum class Action(val label: String) {
        PREVIOUS("<"),
        NEXT(">"),
        UNLOCK("gtladditions.structure_tier.button.unlock"),
        ACTIVATE("gtladditions.structure_tier.button.activate")
    }

    companion object {
        private const val WIDTH = 264
        private const val HEIGHT = 190
        private const val INPUT_X = 188
        private const val COSTS = 16
        private const val COST_ROWS = 4
        private const val UPDATE_TIERS = 3
        private const val CLICK = 1
        private const val RESULT = 1
        private val ICON = ResourceTexture("gtladditions:textures/gui/structure_tier/icon.png")
    }
}