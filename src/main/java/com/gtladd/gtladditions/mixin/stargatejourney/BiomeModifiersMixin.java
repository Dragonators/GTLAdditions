package com.gtladd.gtladditions.mixin.stargatejourney;

import net.minecraftforge.eventbus.api.IEventBus;
import net.povstalec.sgjourney.common.world.biomemod.BiomeModifiers;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BiomeModifiers.class)
public abstract class BiomeModifiersMixin {

    @Inject(method = "register", at = @At("HEAD"), cancellable = true, remap = false)
    private static void preventItemRegistration(IEventBus eventBus, CallbackInfo ci) {
        ci.cancel();
    }
}
