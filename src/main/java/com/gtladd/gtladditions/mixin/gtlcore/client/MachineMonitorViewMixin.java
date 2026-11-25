package com.gtladd.gtladditions.mixin.gtlcore.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import com.gtladd.gtladditions.integration.gtlcore.monitor.ExoticChemicalMonitor;
import com.gtladd.gtladditions.integration.gtlcore.monitor.WirelessMonitorPower;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "org.gtlcore.gtlcore.client.monitor.MachineMonitorView", remap = false)
public abstract class MachineMonitorViewMixin {

    @ModifyArg(method = "draw(Lorg/gtlcore/gtlcore/client/monitor/MachineMonitorView$Canvas;Lorg/gtlcore/gtlcore/client/monitor/MachineMonitorClient$Display;II)V",
               at = @At(value = "INVOKE",
                        target = "Lorg/gtlcore/gtlcore/client/monitor/MachineMonitorView$Canvas;right(Lnet/minecraft/network/chat/Component;FFII)V"),
               slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=type"),
                              to = @At(value = "CONSTANT", args = "stringValue=progress")),
               index = 0,
               require = 1,
               allow = 1)
    private static Component gtladditions$recipeMode(Component type, @Local(name = "state") CompoundTag state) {
        if (!state.contains(ExoticChemicalMonitor.EXOTIC_RECIPE_MODE)) return type;

        return ExoticChemicalMonitor.modeName(state.getByte(ExoticChemicalMonitor.EXOTIC_RECIPE_MODE));
    }

    @ModifyArg(method = "draw(Lorg/gtlcore/gtlcore/client/monitor/MachineMonitorView$Canvas;Lorg/gtlcore/gtlcore/client/monitor/MachineMonitorClient$Display;II)V",
               at = @At(value = "INVOKE",
                        target = "Lorg/gtlcore/gtlcore/client/monitor/MachineMonitorView$Canvas;right(Lnet/minecraft/network/chat/Component;FFII)V"),
               slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=progress")),
               index = 0,
               require = 1,
               allow = 1)
    private static Component gtladditions$unitFraction(Component percent, @Local(name = "state") CompoundTag state) {
        if (!state.getBoolean(ExoticChemicalMonitor.EXOTIC_MULTI_UNIT)) return percent;

        return Component.literal(
                state.getInt(ExoticChemicalMonitor.EXOTIC_RUNNING_UNITS) +
                        "/" +
                        state.getInt(ExoticChemicalMonitor.EXOTIC_UNLOCKED_UNITS));
    }

    @ModifyVariable(method = "draw(Lorg/gtlcore/gtlcore/client/monitor/MachineMonitorView$Canvas;Lorg/gtlcore/gtlcore/client/monitor/MachineMonitorClient$Display;II)V",
                    at = @At("STORE"),
                    name = "time",
                    require = 1,
                    allow = 1)
    private static Component gtladditions$runningUnits(Component time, @Local(name = "state") CompoundTag state) {
        if (!state.getBoolean(ExoticChemicalMonitor.EXOTIC_MULTI_UNIT)) return time;

        return Component.translatable(
                ExoticChemicalMonitor.RUNNING_TEXT,
                state.getInt(ExoticChemicalMonitor.EXOTIC_RUNNING_UNITS),
                state.getInt(ExoticChemicalMonitor.EXOTIC_UNLOCKED_UNITS));
    }

    @ModifyVariable(method = "draw(Lorg/gtlcore/gtlcore/client/monitor/MachineMonitorView$Canvas;Lorg/gtlcore/gtlcore/client/monitor/MachineMonitorClient$Display;II)V",
                    at = @At("STORE"),
                    name = "ratio",
                    require = 1,
                    allow = 1)
    private static double gtladditions$runningShare(double ratio, @Local(name = "state") CompoundTag state) {
        if (!state.getBoolean(ExoticChemicalMonitor.EXOTIC_MULTI_UNIT)) return ratio;

        // The Synthesis Nexus (unit 0) is always unlocked, and only unlocked units count as running.
        return (double) state.getInt(ExoticChemicalMonitor.EXOTIC_RUNNING_UNITS) /
                state.getInt(ExoticChemicalMonitor.EXOTIC_UNLOCKED_UNITS);
    }

    @Inject(method = "energy", at = @At("HEAD"), cancellable = true, require = 1)
    private static void gtladditions$wirelessPower(CompoundTag state, boolean compact, CallbackInfoReturnable<Component> cir) {
        Component power = WirelessMonitorPower.energy(state, compact);
        if (power != null) cir.setReturnValue(power);
    }

    @ModifyVariable(method = "draw(Lorg/gtlcore/gtlcore/client/monitor/MachineMonitorView$Canvas;Lorg/gtlcore/gtlcore/client/monitor/MachineMonitorClient$Display;II)V",
                    at = @At("LOAD"),
                    name = "hasRecipe",
                    slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=energy"),
                                   to = @At(value = "CONSTANT", args = "stringValue=inputs")),
                    require = 1,
                    allow = 1)
    private static boolean gtladditions$showPower(boolean hasRecipe, @Local(name = "state") CompoundTag state) {
        return hasRecipe || WirelessMonitorPower.hasNonZeroWirelessTickInputs(state);
    }
}
