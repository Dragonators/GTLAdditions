package com.gtladd.gtladditions.api.machine.gui

import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator
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
import net.minecraft.world.entity.player.Player
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

class StructureTierConfigurator(private val machine: IStructureTierMachine) : IFancyConfigurator {
    private val tiers: StructureTierTrait get() = machine.structureTierTrait

    override fun getTitle(): Component = "gtladditions.structure_tier.title".toComponent

    override fun getIcon(): IGuiTexture = ICON

    override fun getTooltips(): List<Component> = listOf(title, "gtladditions.structure_tier.tooltip".toComponent)

    override fun createConfigurator(): Widget {
        val group = WidgetGroup(0, 0, WIDTH, HEIGHT)
        group.addWidget(text(4, 2) { list -> list.add(status()) })
        group.addWidget(TierButton(4, 13, 14, "<", { tiers.selectedTier > tiers.layout.minTier }, select(-1)))
        group.addWidget(text(22, 16) { list -> list.add(selection()) })
        group.addWidget(TierButton(170, 13, 14, ">", { tiers.selectedTier < tiers.layout.maxTier }, select(1)))
        group.addWidget(text(4, 32) { list -> list.add(header()) })
        for (i in 0 until COSTS) group.addWidget(CostWidget(4 + i / COST_ROWS * 44, 44 + i % COST_ROWS * 18, i))
        group.addWidget(text(INPUT_X, 32) { list -> list.add("gtladditions.structure_tier.inserted".toComponent) })

        for (slot in 0 until StructureTierTrait.SLOTS) {
            group.addWidget(
                SlotWidget(tiers.inventory, slot, INPUT_X + slot % 4 * 18, 44 + slot / 4 * 18, true, true)
                    .setBackgroundTexture(GuiTextures.SLOT)
            )
        }

        group.addWidget(text(4, 122) { list -> conditions(list) })
        group.addWidget(
            TierButton(
                4,
                172,
                126,
                "gtladditions.structure_tier.button.unlock",
                { tiers.selectedTier == tiers.unlockedTier + 1 },
                { player, buf ->
                    tiers.unlock(buf.readVarInt()).forEach(player::sendSystemMessage)
                    null
                },
                { buf -> buf.writeVarInt(tiers.selectedTier) }
            )
        )
        group.addWidget(
            TierButton(
                134,
                172,
                126,
                "gtladditions.structure_tier.button.activate",
                { tiers.hasPendingTier },
                { player, _ -> activate(player) }
            )
        )
        return group
    }

    /** Refreshed on draw so it switches in the same frame as the cost slots and the buttons. */
    private fun text(x: Int, y: Int, lines: (MutableList<Component>) -> Unit): Widget = object : ComponentPanelWidget(x, y, lines) {
        override fun detectAndSendChanges() {}

        @OnlyIn(Dist.CLIENT)
        override fun drawInBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
            updateScreen()
            super.drawInBackground(graphics, mouseX, mouseY, partialTicks)
        }
    }.setMaxWidthLimit(WIDTH - 8).setClientSideWidget()

    private fun select(step: Int): (Player, FriendlyByteBuf) -> ((FriendlyByteBuf) -> Unit)? = { _, _ ->
        tiers.select(tiers.selectedTier + step)
        null
    }

    private fun status(): Component =
        "gtladditions.structure_tier.status".toComponent(tiers.unlockedTier, tiers.layout.maxTier)

    private fun selection(): Component {
        val tier = tiers.selectedTier
        val state = when {
            tier <= tiers.structureTier -> "active"
            tier <= tiers.unlockedTier -> "unlocked"
            tier == tiers.unlockedTier + 1 -> "next"
            else -> "locked"
        }

        return "gtladditions.structure_tier.selected".toComponent(tier, "gtladditions.structure_tier.state.$state".toComponent)
    }

    private fun header(): Component = if (tiers.requirement(tiers.selectedTier) == null) {
        "gtladditions.structure_tier.free".toComponent
    } else {
        "gtladditions.structure_tier.required".toComponent
    }

    private fun conditions(list: MutableList<Component>) {
        for (condition in tiers.requirement(tiers.selectedTier)?.conditions ?: return) {
            val met = condition.test(machine)
            list.add(
                (if (met) "✔ " else "✘ ").literal
                    .withStyle(if (met) ChatFormatting.GREEN else ChatFormatting.RED)
                    .append(condition.describe(machine))
            )
        }
    }

    private fun activate(player: Player): (FriendlyByteBuf) -> Unit {
        val activation = tiers.activate(player.blockPosition())
        activation.messages.forEach(player::sendSystemMessage)
        return { buf ->
            buf.writeVarInt(activation.cells.size)
            activation.cells.forEach { buf.writeLong(it.asLong()) }
            buf.writeVarInt(activation.chunks.size)
            activation.chunks.forEach { box ->
                doubleArrayOf(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ).forEach(buf::writeDouble)
            }
        }
    }

    private inner class CostWidget(x: Int, y: Int, private val index: Int) :
        Widget(x, y, 42, 18),
        IIngredientSlot {
        private val cost get() = tiers.requirement(tiers.selectedTier)?.costs?.getOrNull(index)
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
            val paid = tiers.paid(tiers.selectedTier, index)
            val font = Minecraft.getInstance().font
            when {
                paid >= cost.amount -> graphics.drawString(font, "✔", pos.x + 22, pos.y + 5, 0x55FF55, true)
                else -> graphics.drawString(
                    font,
                    "x${cost.amount - paid}",
                    pos.x + 20,
                    pos.y + 5,
                    if (paid == 0L) 0xFF5555 else 0xFFFF55,
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

    private inner class TierButton(
        x: Int,
        y: Int,
        width: Int,
        private val label: String,
        private val enabled: () -> Boolean,
        private val action: (Player, FriendlyByteBuf) -> ((FriendlyByteBuf) -> Unit)?,
        private val writeRequest: (FriendlyByteBuf) -> Unit = {}
    ) : Widget(x, y, width, 14) {
        private var pending = false

        @OnlyIn(Dist.CLIENT)
        override fun drawInBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
            StructureTierRenderer.drawButton(
                graphics,
                label,
                position.x,
                position.y,
                size.width,
                size.height,
                enabled(),
                pending,
                isMouseOverElement(mouseX.toDouble(), mouseY.toDouble())
            )
        }

        @OnlyIn(Dist.CLIENT)
        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            if (!isMouseOverElement(mouseX, mouseY) || !enabled() || pending) return false
            pending = true
            writeClientAction(CLICK) { buf -> writeRequest(buf) }
            playButtonClickSound()
            return true
        }

        override fun handleClientAction(id: Int, buffer: FriendlyByteBuf) {
            if (id != CLICK) return super.handleClientAction(id, buffer)
            var result: ((FriendlyByteBuf) -> Unit)? = null
            if (enabled()) {
                result = action(gui.entityPlayer, buffer)
                // Sync the fields before the result, so the page has switched by the time the button is released.
                tiers.machine.holder.syncNow(false)
            }

            writeUpdateInfo(RESULT) { buf ->
                buf.writeBoolean(result != null)
                result?.invoke(buf)
            }
        }

        @OnlyIn(Dist.CLIENT)
        override fun readUpdateInfo(id: Int, buffer: FriendlyByteBuf) {
            if (id != RESULT) return super.readUpdateInfo(id, buffer)
            pending = false
            if (buffer.readBoolean()) StructureTierRenderer.highlight(buffer)
        }
    }

    companion object {
        private const val WIDTH = 264
        private const val HEIGHT = 190
        private const val INPUT_X = 188
        private const val COSTS = 16
        private const val COST_ROWS = 4
        private const val CLICK = 1
        private const val RESULT = 1
        private val ICON = ResourceTexture("gtladditions:textures/gui/structure_tier/icon.png")
    }
}