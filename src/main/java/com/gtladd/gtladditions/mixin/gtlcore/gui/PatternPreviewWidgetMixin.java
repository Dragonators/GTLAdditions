package com.gtladd.gtladditions.mixin.gtlcore.gui;

import org.gtlcore.gtlcore.api.gui.PatternPreviewWidget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PatternPreviewWidget.class)
public class PatternPreviewWidgetMixin {

    @ModifyConstant(method = "setPage", remap = false, constant = @Constant(intValue = 36, ordinal = 0))
    private int modifyContainer(int constant) {
        return 48;
    }
}
