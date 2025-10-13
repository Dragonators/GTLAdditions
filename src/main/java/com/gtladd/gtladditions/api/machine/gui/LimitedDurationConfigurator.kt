package com.gtladd.gtladditions.api.machine.gui

import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget
import com.gtladd.gtladditions.api.machine.IGTLAddMultiRecipeMachine
import com.hepdd.gtmthings.GTMThings
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import net.minecraft.network.chat.Component

class LimitedDurationConfigurator(private val machine: IGTLAddMultiRecipeMachine) : IFancyConfigurator {
    override fun getTitle(): Component {
        return Component.translatable("gtceu.machine.limitduration_configurator")
    }

    override fun getIcon(): IGuiTexture {
        return ResourceTexture(GTMThings.id("textures/item/opv_4a_wireless_energy_receive_cover.png"))
    }

    override fun createConfigurator(): Widget? {
        return WidgetGroup(0, 0, 100, 20)
            .addWidget(IntInputWidget({ machine.limitedDuration }, { duration: Int? ->
                machine.setLimitedDuration(duration!!)
            }).setMin(10).setMax(200))
    }
}
