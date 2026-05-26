package com.gtladd.gtladditions.api.machine.gui

import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget
import com.gtladd.gtladditions.api.machine.IGTLAddMultiRecipeMachine
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.hepdd.gtmthings.GTMThings
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import net.minecraft.network.chat.Component

class LimitedDurationConfigurator(private val machine: IGTLAddMultiRecipeMachine) : IFancyConfigurator {
    override fun getTitle(): Component = "gtceu.machine.limitduration_configurator".toComponent

    override fun getIcon(): IGuiTexture = ResourceTexture(GTMThings.id("textures/item/opv_4a_wireless_energy_receive_cover.png"))

    override fun createConfigurator(): Widget? = WidgetGroup(0, 0, 100, 20)
        .addWidget(
            IntInputWidget({ machine.getLimitedDuration() }, { duration: Int? ->
                machine.setLimitedDuration(duration!!)
            }).setMin(10).setMax(200)
        )
}