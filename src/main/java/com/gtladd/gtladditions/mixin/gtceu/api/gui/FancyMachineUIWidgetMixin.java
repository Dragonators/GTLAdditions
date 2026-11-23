package com.gtladd.gtladditions.mixin.gtceu.api.gui;

import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;

import com.gtladd.gtladditions.api.machine.feature.IStructureTierMachine;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = FancyMachineUIWidget.class, remap = false)
public abstract class FancyMachineUIWidgetMixin {

    @ModifyExpressionValue(method = "<init>",
                           at = @At(value = "INVOKE",
                                    target = "Lcom/gregtechceu/gtceu/api/gui/fancy/IFancyUIProvider;hasPlayerInventory()Z"),
                           require = 1)
    private boolean gtladditions$createSubTabPlayerInventory(boolean original, @Local(argsOnly = true) IFancyUIProvider mainPage) {
        return original || mainPage instanceof IStructureTierMachine;
    }
}