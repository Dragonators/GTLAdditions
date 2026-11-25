package com.gtladd.gtladditions.mixin.gtceu.xei;

import net.minecraft.network.chat.Component;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.config.FluidUnit;
import it.unimi.dsi.fastutil.doubles.Double2ObjectFunction;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FluidUnit.class)
@Pseudo
public abstract class FluidUnitMixin {

    @Shadow(remap = false)
    @Final
    @Mutable
    private Double2ObjectFunction<Component> translator;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void fixLongOverflow(CallbackInfo ci) {
        // Enum constructors include synthetic parameters; the logical name is assigned by RETURN.
        String unitName = ((FluidUnit) (Object) this).getName();
        if ("millibuckets".equals(unitName)) {
            this.translator = (a) -> {
                int divisor = FluidUnit.literDivisor();
                long displayAmount = Math.round(a / divisor);
                return EmiPort.translatable("emi.fluid.amount.millibuckets",
                        EmiRenderHelper.TEXT_FORMAT.format(displayAmount));
            };
        } else if ("liters".equals(unitName)) {
            this.translator = (a) -> {
                int divisor = FluidUnit.literDivisor();
                long displayAmount = Math.round(a / divisor);
                return EmiPort.translatable("emi.fluid.amount.liters",
                        EmiRenderHelper.TEXT_FORMAT.format(displayAmount));
            };
        }
    }
}
