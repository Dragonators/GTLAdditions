package com.gtladd.gtladditions.api.machine.gui;

import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.network.chat.Component;

import com.gtladd.gtladditions.api.machine.IGTLAddMultiRecipe;
import com.hepdd.gtmthings.GTMThings;

public class LimitedDurationConfigurator implements IFancyConfigurator {

    private final IGTLAddMultiRecipe machine;

    public LimitedDurationConfigurator(IGTLAddMultiRecipe machine) {
        this.machine = machine;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gtceu.machine.limitduration_configurator");
    }

    @Override
    public IGuiTexture getIcon() {
        return new ResourceTexture(GTMThings.id("textures/item/opv_4a_wireless_energy_receive_cover.png"));
    }

    @Override
    public Widget createConfigurator() {
        return new WidgetGroup(0, 0, 100, 20)
                .addWidget(new IntInputWidget(machine::getLimitedDuration, machine::setLimitedDuration).setMin(10).setMax(200));
    }
}
