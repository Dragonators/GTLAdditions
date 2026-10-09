package com.gtladd.gtladditions.integration.xei

import dev.emi.emi.api.EmiDragDropHandler
import dev.emi.emi.api.stack.EmiIngredient
import dev.emi.emi.api.stack.EmiStack
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.item.Item
import net.minecraft.world.level.material.Fluid

object TerminalSearchBoxEmiHandler : EmiDragDropHandler<Screen> {
    override fun dropStack(screen: Screen, stack: EmiIngredient, x: Int, y: Int): Boolean {
        val field = SearchBoxDrop.getTextFieldAreas(screen).firstOrNull { SearchBoxDrop.isInside(it, x, y) }
            ?: return false

        val dragged = firstSupportedStack(stack) ?: return false

        return SearchBoxDrop.fillTextField(screen, field, dragged.name.string)
    }

    override fun render(
        screen: Screen,
        dragged: EmiIngredient,
        draw: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float
    ) {
        if (firstSupportedStack(dragged) == null) return

        for (field in SearchBoxDrop.getTextFieldAreas(screen)) {
            val area = SearchBoxDrop.getArea(field)
            draw.fill(area.x, area.y, area.x + area.width, area.y + area.height, 0x8822BB33.toInt())
        }
    }

    private fun firstSupportedStack(ingredient: EmiIngredient): EmiStack? =
        ingredient.emiStacks.firstOrNull { !it.isEmpty && (it.key is Item || it.key is Fluid) }
}