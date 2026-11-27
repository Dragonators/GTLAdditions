package com.gtladd.gtladditions.mixin.gtlcore.client;

import org.gtlcore.gtlcore.client.preview.PreviewControls;
import org.gtlcore.gtlcore.client.preview.PreviewPatternPanel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = PreviewControls.class, remap = false)
public interface PreviewControlsAccessor {

    @Accessor("patternPanel")
    PreviewPatternPanel getPatternPanel();
}
