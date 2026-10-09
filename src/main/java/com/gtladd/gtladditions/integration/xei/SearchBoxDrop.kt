package com.gtladd.gtladditions.integration.xei

import appeng.client.gui.AEBaseScreen
import com.gtladd.gtladditions.mixin.ae2.client.AEBaseScreenAccessor
import com.gtladd.gtladditions.mixin.ae2.client.WidgetContainerAccessor
import com.gtladd.gtladditions.mixin.minecraft.client.EditBoxAccessor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.Rect2i

object SearchBoxDrop {
    fun getTextFieldAreas(screen: Screen): List<EditBox> {
        if (screen !is AEBaseScreen<*>) return emptyList()

        val widgets = (screen as? AEBaseScreenAccessor)?.getWidgets() ?: return emptyList()
        val field = (widgets as? WidgetContainerAccessor)?.getWidgets()?.get("search") as? EditBox
            ?: return emptyList()

        if (!field.visible || !field.active || field.width <= 0 || field.height <= 0) return emptyList()
        if ((field as? EditBoxAccessor)?.isEditable != true) return emptyList()
        if (screen.children().none { it === field }) return emptyList()

        return listOf(field)
    }

    fun getArea(field: EditBox) = Rect2i(field.x, field.y, field.width, field.height)

    fun isInside(field: EditBox, x: Int, y: Int): Boolean =
        x >= field.x && y >= field.y && x < field.x + field.width && y < field.y + field.height

    fun fillTextField(screen: Screen, field: EditBox, name: String): Boolean {
        if (name.isBlank() || getTextFieldAreas(screen).none { it === field }) return false

        field.value = name
        field.cursorPosition = 0
        field.setHighlightPos(0)
        screen.focused = field

        return true
    }
}