package com.gtladd.gtladditions.mixin.stargatejourney.stargate;

import net.minecraft.server.MinecraftServer;
import net.povstalec.sgjourney.common.sgjourney.StargateConnection;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StargateConnection.class)
public abstract class StargateConnectionMixin {

    @Shadow(remap = false)
    @Final
    @Mutable
    protected static boolean ENERGY_BYPASS_ENABLED;

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false)
    private static void forceEnergyBypass(CallbackInfo ci) {
        ENERGY_BYPASS_ENABLED = true;
    }

    /**
     * @author Dragons
     * @reason Disable energy
     */
    @Overwrite(remap = false)
    private boolean depleteEnergy(MinecraftServer server, long energyDraw) {
        return true;
    }
}
