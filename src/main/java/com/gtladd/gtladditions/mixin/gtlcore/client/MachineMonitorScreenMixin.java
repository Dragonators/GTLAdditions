package com.gtladd.gtladditions.mixin.gtlcore.client;

import net.minecraft.nbt.CompoundTag;

import com.gtladd.gtladditions.integration.gtlcore.monitor.WirelessMonitorPower;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(targets = "org.gtlcore.gtlcore.client.monitor.MachineMonitorScreen", remap = false)
public abstract class MachineMonitorScreenMixin {

    @WrapOperation(method = "tooltip",
                   at = @At(value = "INVOKE",
                            target = "Lnet/minecraft/nbt/CompoundTag;getBoolean(Ljava/lang/String;)Z",
                            remap = true),
                   slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=HasRecipe", ordinal = 0),
                                  to = @At(value = "INVOKE",
                                           target = "Lorg/gtlcore/gtlcore/client/monitor/MachineMonitorView;energy(Lnet/minecraft/nbt/CompoundTag;Z)Lnet/minecraft/network/chat/Component;")),
                   require = 1,
                   allow = 1)
    private boolean gtladditions$showPowerTooltip(CompoundTag state, String key, Operation<Boolean> original) {
        return original.call(state, key) || WirelessMonitorPower.hasNonZeroWirelessTickInputs(state);
    }
}
