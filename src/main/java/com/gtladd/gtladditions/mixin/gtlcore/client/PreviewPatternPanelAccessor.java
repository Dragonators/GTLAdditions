package com.gtladd.gtladditions.mixin.gtlcore.client;

import org.gtlcore.gtlcore.client.preview.PreviewPatternPanel;

import appeng.api.stacks.GenericStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = PreviewPatternPanel.class, remap = false)
public interface PreviewPatternPanelAccessor {

    @Accessor("outputs")
    List<GenericStack> getOutputs();

    @Mutable
    @Accessor("outputs")
    void setOutputs(List<GenericStack> outputs);
}
