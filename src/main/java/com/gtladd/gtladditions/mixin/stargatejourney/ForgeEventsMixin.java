package com.gtladd.gtladditions.mixin.stargatejourney;

import net.minecraftforge.event.village.VillagerTradesEvent;
import net.povstalec.sgjourney.common.events.ForgeEvents;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ForgeEvents.class)
public abstract class ForgeEventsMixin {

    @Inject(method = "addCustomTrades", at = @At("HEAD"), cancellable = true, remap = false)
    private static void preventVillagerTrades(VillagerTradesEvent event, CallbackInfo ci) {
        ci.cancel();
    }
}
