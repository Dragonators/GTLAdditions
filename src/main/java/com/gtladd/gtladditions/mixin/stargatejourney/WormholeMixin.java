package com.gtladd.gtladditions.mixin.stargatejourney;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.povstalec.sgjourney.common.sgjourney.Wormhole;
import net.povstalec.sgjourney.common.sgjourney.stargate.Stargate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.gtladd.gtladditions.utils.CommonUtils.VALID_TAG;

@Mixin(Wormhole.class)
public abstract class WormholeMixin {

    @Inject(method = "transportPlayer", at = @At("HEAD"), remap = false)
    private void beforeTransport(ServerLevel destinationLevel, Stargate destinationStargate, ServerPlayer player, Vec3 destinationPosition, Vec3 destinationMomentum, Vec3 destinationLookAngle, CallbackInfoReturnable<Entity> cir) {
        player.addTag(VALID_TAG);
    }
}
