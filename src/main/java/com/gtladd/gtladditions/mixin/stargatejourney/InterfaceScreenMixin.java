package com.gtladd.gtladditions.mixin.stargatejourney;

import net.minecraft.client.gui.components.EditBox;
import net.povstalec.sgjourney.client.screens.InterfaceScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InterfaceScreen.class)
public abstract class InterfaceScreenMixin {

    @Shadow(remap = false)
    protected EditBox commandEdit;

    @Inject(method = "init()V", at = @At("TAIL"))
    private void disableEnergyTargetField(CallbackInfo ci) {
        if (commandEdit != null) {
            commandEdit.setVisible(false);
            commandEdit.active = false;
        }
    }
}
