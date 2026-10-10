package com.gtladd.gtladditions.api.machine.gui

import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget
import com.gregtechceu.gtceu.data.lang.LangHandler
import com.gtladd.gtladditions.common.machine.GTLAddMachines
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.wideBuffer.WideBufferTools
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import net.minecraft.network.chat.Component

class OutputTransferConfigurator(
    private val getLimit: () -> Int,
    private val setLimit: (Int) -> Unit
) : IFancyConfigurator {
    override fun getTitle(): Component = "gtladditions.machine.output_transfer.title".toComponent

    override fun getIcon(): IGuiTexture = ItemStackTexture(GTLAddMachines.ME_SUPER_EXTENDED_EXPORT_BUFFER.asStack())

    override fun getTooltips(): List<Component> = listOf(getTitle()) + LangHandler.getMultiLang("gtladditions.machine.output_transfer.tooltip")

    override fun createConfigurator(): Widget = WidgetGroup(0, 0, 118, 44).apply {
        addWidget(LabelWidget(6, 5, "gtladditions.machine.output_transfer.attempts"))
        addWidget(
            IntInputWidget(6, 18, 106, 20, getLimit) { value ->
                value?.let(setLimit)
            }.setMin(WideBufferTools.MIN_OUTPUT_TRANSFER_LIMIT)
                .setMax(WideBufferTools.MAX_OUTPUT_TRANSFER_LIMIT)
        )
    }
}