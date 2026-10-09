package com.gtladd.gtladditions.mixin.ae2.client;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.WidgetContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AEBaseScreen.class)
public interface AEBaseScreenAccessor {

    @Accessor(value = "widgets", remap = false)
    WidgetContainer getWidgets();
}