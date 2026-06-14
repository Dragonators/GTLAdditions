package com.gtladd.gtladditions.api.machine.gui

import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget
import com.gregtechceu.gtceu.api.gui.widget.ToggleButtonWidget
import com.gregtechceu.gtceu.data.lang.LangHandler
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine
import com.gtladd.gtladditions.common.machine.multiblock.part.MESuperPatternBufferPartMachine
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import net.minecraft.network.chat.Component

class FOAPatternConfigurator(private val machine: MESuperPatternBufferPartMachine) : IFancyConfigurator {
    override fun getTitle(): Component = "gtladditions.machine.me_super_pattern_buffer.foa_config.title".toComponent

    override fun getIcon(): IGuiTexture = ItemStackTexture(MultiBlockMachine.FORGE_OF_THE_ANTICHRIST.asStack())

    override fun getTooltips(): List<Component> =
        mutableListOf(getTitle()).apply {
            addAll(LangHandler.getMultiLang("gtladditions.machine.me_super_pattern_buffer.foa_config.tooltip"))
        }

    override fun createConfigurator(): Widget {
        val group = WidgetGroup(0, 0, 118, 56)
        group.addWidget(
            ToggleButtonWidget(
                6,
                5,
                20,
                20,
                GuiTextures.BUTTON_POWER,
                { machine.isFOAModeEnabled() },
                { enabled -> machine.setFOAModeEnabled(enabled) }
            ).setTooltipText("gtladditions.machine.me_super_pattern_buffer.foa_mode")
        )
        group.addWidget(LabelWidget(32, 10, "gtladditions.machine.me_super_pattern_buffer.foa_mode"))
        group.addWidget(LabelWidget(6, 36, "gtladditions.machine.me_super_pattern_buffer.foa_multiplier"))
        group.addWidget(
            IntInputWidget(
                58,
                31,
                54,
                20,
                { machine.getFOAPatternOutputMultiplier() },
                { multiplier: Int? -> machine.setFOAPatternOutputMultiplier(multiplier ?: machine.getFOAPatternOutputMultiplier()) }
            ).setMin(
                MESuperPatternBufferPartMachine.MIN_MULTIPLIER
            ).setMax(
                MESuperPatternBufferPartMachine.MAX_MULTIPLIER
            )
        )
        return group
    }
}