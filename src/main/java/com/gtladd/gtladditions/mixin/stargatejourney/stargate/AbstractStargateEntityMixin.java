package com.gtladd.gtladditions.mixin.stargatejourney.stargate;

import net.minecraft.core.Direction;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import net.povstalec.sgjourney.common.config.CommonStargateConfig;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractStargateEntity.class)
public abstract class AbstractStargateEntityMixin {

    @Shadow(remap = false)
    @Final
    @Mutable
    private static boolean REQUIRE_ENERGY;

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false)
    private static void gtladditions$disableWormholeEnergy(CallbackInfo ci) {
        REQUIRE_ENERGY = false;
    }

    @Inject(method = "getCapability", at = @At("HEAD"), cancellable = true, remap = false)
    private <T> void gtladditions$hideEnergyCapability(Capability<T> capability, Direction side, CallbackInfoReturnable<LazyOptional<T>> cir) {
        if (capability == ForgeCapabilities.ENERGY) cir.setReturnValue(LazyOptional.empty());
    }

    @Inject(method = { "getEnergyCapacity", "getMaxEnergyReceive", "getMaxEnergyExtract", "getMaxEnergyDeplete" },
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void gtladditions$disableEnergyStorage(CallbackInfoReturnable<Long> cir) {
        cir.setReturnValue(0L);
    }

    @Inject(method = "canReceiveZeroPointEnergy", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtladditions$disableZeroPointEnergy(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Redirect(method = "disconnectStargate", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/ForgeConfigSpec$BooleanValue;get()Ljava/lang/Object;"), remap = false)
    private Object allowDisconnectFromBothEnds(ForgeConfigSpec.BooleanValue value) {
        if (value == CommonStargateConfig.end_connection_from_both_ends) return true;
        return value.get();
    }
}
