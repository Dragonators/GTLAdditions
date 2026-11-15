package com.gtladd.gtladditions.mixin.ldlib;

import com.lowdragmc.lowdraglib.gui.widget.TankWidget;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TankWidget.class, remap = false)
public interface TankWidgetAccessor {

    @Nullable
    @Accessor(value = "currentJEIRenderedIngredient", remap = false)
    FluidStack getCurrentJEIRenderedIngredient();
}