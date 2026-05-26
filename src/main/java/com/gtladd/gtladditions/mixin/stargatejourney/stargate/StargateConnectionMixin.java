package com.gtladd.gtladditions.mixin.stargatejourney.stargate;

import net.minecraft.server.MinecraftServer;
import net.povstalec.sgjourney.common.sgjourney.StargateConnection;
import net.povstalec.sgjourney.common.sgjourney.StargateInfo;
import net.povstalec.sgjourney.common.sgjourney.stargate.Stargate;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/povstalec/sgjourney/common/sgjourney/stargate/Stargate;shouldAutoclose(Lnet/minecraft/server/MinecraftServer;Lnet/povstalec/sgjourney/common/sgjourney/StargateConnection;)Z", ordinal = 0), remap = false)
    private boolean disableDialingSideAutoclose(Stargate stargate, MinecraftServer server, StargateConnection connection) {
        return false;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/povstalec/sgjourney/common/sgjourney/stargate/Stargate;shouldAutoclose(Lnet/minecraft/server/MinecraftServer;Lnet/povstalec/sgjourney/common/sgjourney/StargateConnection;)Z", ordinal = 1), remap = false)
    private boolean disableDialedSideAutoclose(Stargate stargate, MinecraftServer server, StargateConnection connection) {
        return false;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/povstalec/sgjourney/common/sgjourney/stargate/Stargate;doWormhole(Lnet/minecraft/server/MinecraftServer;Lnet/povstalec/sgjourney/common/sgjourney/StargateConnection;ZLnet/povstalec/sgjourney/common/sgjourney/StargateInfo$WormholeTravel;)V", ordinal = 1), remap = false)
    private void forceDialedSideWormholeTravel(Stargate stargate, MinecraftServer server, StargateConnection connection, boolean isDialedSide, StargateInfo.WormholeTravel travel) {
        stargate.doWormhole(server, connection, isDialedSide, StargateInfo.WormholeTravel.ENABLED);
    }
}