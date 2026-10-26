package com.gtladd.gtladditions.mixin.stargatejourney.stargate;

import net.povstalec.sgjourney.common.sgjourney.StargateConnection;
import net.povstalec.sgjourney.common.sgjourney.StargateInfo;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StargateConnection.class)
public abstract class StargateConnectionMixin {

    @Shadow(remap = false)
    @Final
    @Mutable
    public static boolean ENERGY_BYPASS_ENABLED;

    @Shadow(remap = false)
    @Final
    @Mutable
    public static boolean REQUIRE_ENERGY;

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false)
    private static void gtladditions$disableConnectionEnergy(CallbackInfo ci) {
        ENERGY_BYPASS_ENABLED = true;
        REQUIRE_ENERGY = false;
    }

    @Inject(method = "shouldAutoclose", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtladditions$disableAutoclose(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @ModifyArg(method = "tick",
               at = @At(value = "INVOKE",
                        target = "Lnet/povstalec/sgjourney/common/sgjourney/stargate/Stargate;doWormhole(Lnet/povstalec/sgjourney/common/sgjourney/StargateConnection;ZLnet/povstalec/sgjourney/common/sgjourney/StargateInfo$WormholeTravel;)V",
                        ordinal = 1),
               index = 2,
               remap = false,
               require = 1,
               allow = 1)
    private StargateInfo.WormholeTravel gtladditions$enableDialedSideTravel(StargateInfo.WormholeTravel original) {
        return StargateInfo.WormholeTravel.ENABLED;
    }
}