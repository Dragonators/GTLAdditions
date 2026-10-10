package com.gtladd.gtladditions.api.gui.widget

import appeng.api.stacks.AEFluidKey
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.AEKey
import appeng.api.stacks.GenericStack
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.integration.ae2.gui.widget.slot.AEConfigSlotWidget
import com.gtladd.gtladditions.utils.wideBuffer.longValue
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.world.item.TooltipFlag
import org.gtlcore.gtlcore.client.gui.widget.MEOutListGridWidget
import org.gtlcore.gtlcore.utils.datastructure.Int128
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.util.Optional

/** Reuses Core's scrolling rows and ingredient links, with exact, absolute quantity updates. */
class WideBufferOutputWidget(
    x: Int,
    y: Int,
    rows: Int,
    private val amounts: Map<AEKey, Int128>
) : MEOutListGridWidget(x, y, rows, Object2LongOpenHashMap()) {
    private val sentAmounts = linkedMapOf<AEKey, BigInteger>()
    private val displayedAmounts = linkedMapOf<AEKey, BigInteger>()

    private fun updateRowKeys() {
        list.clear()
        amounts.forEach { (key, amount) ->
            if (amount.isPositive) list.put(key, amount.longValue())
        }
    }

    override fun detectAndSendChanges() {
        updateRowKeys()
        super.detectAndSendChanges()
    }

    override fun writeInitialData(buffer: FriendlyByteBuf) {
        updateRowKeys()
        sentAmounts.clear()
        super.writeInitialData(buffer)
    }

    override fun writeListChange(buffer: FriendlyByteBuf) {
        val changes = linkedMapOf<AEKey, BigInteger>()
        val iterator = sentAmounts.keys.iterator()
        while (iterator.hasNext()) {
            val key = iterator.next()
            if (amounts[key]?.isPositive != true) {
                changes[key] = BigInteger.ZERO
                iterator.remove()
            }
        }
        amounts.forEach { (key, amount) ->
            if (amount.isPositive) {
                val exact = amount.toBigInteger()
                if (sentAmounts[key] != exact) {
                    changes[key] = exact
                    sentAmounts[key] = exact
                }
            }
        }
        buffer.writeVarInt(changes.size)
        changes.forEach { (key, amount) ->
            buffer.writeNbt(key.toTagGeneric())
            buffer.writeUtf(amount.toString(), 39)
        }
    }

    override fun readListChange(buffer: FriendlyByteBuf) {
        val count = buffer.readVarInt()
        if (count == 0) return
        repeat(count) {
            val tag = buffer.readNbt()
            val amount = BigInteger(buffer.readUtf(39))
            val key = tag?.let(AEKey::fromTagGeneric)
            if (key != null) {
                if (amount.signum() > 0) displayedAmounts[key] = amount else displayedAmounts.remove(key)
            }
        }
        displayList.clear()
        displayedAmounts.forEach { (key, amount) ->
            // The inherited ingredient-link API only needs a bounded icon stack.
            displayList.add(GenericStack(key, amount.min(BigInteger.valueOf(Long.MAX_VALUE)).toLong()))
        }
    }

    fun getExactAmount(index: Int): BigInteger? = getAt(index)?.what()?.let(displayedAmounts::get)

    override fun createDisplayWidget(x: Int, y: Int, index: Int): Widget = WideDisplaySlot(x, y, this, index)

    private class WideDisplaySlot(
        x: Int,
        y: Int,
        private val grid: WideBufferOutputWidget,
        private val index: Int
    ) : DisplaySlot(x, y, grid, index) {
        override fun drawInBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
            val key = grid.getAt(index)?.what()
            val x = positionX
            val y = positionY
            val texture = if (key is AEFluidKey) GuiTextures.FLUID_SLOT else GuiTextures.SLOT
            texture.draw(graphics, mouseX, mouseY, x.toFloat(), y.toFloat(), 18, 18)
            GuiTextures.NUMBER_BACKGROUND.draw(graphics, mouseX, mouseY, (x + 18).toFloat(), y.toFloat(), 140, 18)

            when (key) {
                is AEItemKey -> DrawerHelper.drawItemStack(graphics, key.toStack(1), x + 1, y + 1, -1, null)
                is AEFluidKey -> DrawerHelper.drawFluidForGui(graphics, FluidStack.create(key.fluid, 1L, key.tag), 1L, x + 1, y + 1, 16, 16)
            }
            val amount = grid.getExactAmount(index)
            if (key != null && amount != null) {
                val full = formatAmount(key, amount)
                val text = if (Minecraft.getInstance().font.width(full) <= 132) {
                    full
                } else {
                    val shortened = BigDecimal(amount).round(MathContext(6)).stripTrailingZeros().toEngineeringString()
                    if (key is AEFluidKey) "≈$shortened mB" else "≈$shortened"
                }
                DrawerHelper.drawText(graphics, text, (x + 21).toFloat(), (y + 6).toFloat(), 1.0f, -1)
            }
            if (isMouseOverElement(mouseX.toDouble(), mouseY.toDouble())) {
                AEConfigSlotWidget.drawSelectionOverlay(graphics, x + 1, y + 1, 16, 16)
            }
        }

        override fun drawInForeground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
            if (!isMouseOverElement(mouseX.toDouble(), mouseY.toDouble())) return
            val key = grid.getAt(index)?.what() ?: return
            val amount = grid.getExactAmount(index) ?: return
            val minecraft = Minecraft.getInstance()
            val tooltip = when (key) {
                is AEItemKey -> key.toStack(1).getTooltipLines(minecraft.player, TooltipFlag.NORMAL).toMutableList()
                is AEFluidKey -> mutableListOf(FluidStack.create(key.fluid, 1L, key.tag).displayName)
                else -> mutableListOf()
            }
            tooltip.add(Component.literal(formatAmount(key, amount)))
            graphics.renderTooltip(minecraft.font, tooltip, Optional.empty(), mouseX, mouseY)
        }
    }

    companion object {
        fun formatAmount(key: AEKey, amount: BigInteger): String = if (key is AEFluidKey) "$amount mB" else "× $amount"
    }
}