package com.gtladd.gtladditions.mixin.gtlcore.client;

import org.gtlcore.gtlcore.integration.ae2.MEFluidUnits;
import org.gtlcore.gtlcore.utils.NumberUtils;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import com.gtladd.gtladditions.integration.gtlcore.monitor.ExoticChemicalMonitor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/** Server rates of the complex's merged batches; the server writes none for non-consumed or chanced rows. */
@Mixin(targets = "org.gtlcore.gtlcore.client.monitor.MachineMonitorView$Row", remap = false)
public abstract class MachineMonitorViewRowMixin {

    @Unique
    private static final DecimalFormat gtladditions$RATE_FORMAT = new DecimalFormat(
            "#,##0.##",
            DecimalFormatSymbols.getInstance(Locale.US));

    @Shadow
    @Final
    List<FluidStack> fluids;

    @Unique
    private boolean gtladditions$hasExoticRate;
    @Unique
    private double gtladditions$exoticRate;
    @Unique
    private double gtladditions$exoticMaxRate;

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void gtladditions$readExoticRate(CompoundTag tag, CallbackInfo ci) {
        gtladditions$hasExoticRate = tag.contains(ExoticChemicalMonitor.EXOTIC_RATE_PER_SEC, Tag.TAG_DOUBLE);
        gtladditions$exoticRate = tag.getDouble(ExoticChemicalMonitor.EXOTIC_RATE_PER_SEC);
        gtladditions$exoticMaxRate = tag.getDouble(ExoticChemicalMonitor.EXOTIC_MAX_RATE_PER_SEC);
    }

    @Inject(method = "rate(Lnet/minecraft/nbt/CompoundTag;Z)Ljava/lang/String;",
            at = @At("HEAD"),
            cancellable = true,
            require = 1)
    private void gtladditions$exoticRate(CompoundTag state, boolean compact, CallbackInfoReturnable<String> cir) {
        if (!gtladditions$hasExoticRate) return;

        String value = gtladditions$format(gtladditions$exoticRate, compact);

        // An absent upper bound reads 0, and both bounds stay 0 while no unit holding this row runs.
        if (gtladditions$exoticMaxRate > gtladditions$exoticRate) {
            cir.setReturnValue(value + "–" + gtladditions$format(gtladditions$exoticMaxRate, compact));
        } else {
            cir.setReturnValue(value);
        }
    }

    @Unique
    private String gtladditions$format(double rate, boolean compact) {
        if (compact) {
            return (fluids.isEmpty() ? NumberUtils.formatDouble(rate) : MEFluidUnits.formatDisplayAmount(rate)) + "/s";
        }

        return gtladditions$RATE_FORMAT.format(rate) + (fluids.isEmpty() ? "/s" : " mB/s");
    }
}
