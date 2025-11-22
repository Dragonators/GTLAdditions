package com.gtladd.gtladditions.mixin.gtceu.xei;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ItemEmiStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@SuppressWarnings("UnstableApiUsage")
@Mixin(ItemEmiStack.class)
@Pseudo
public abstract class ItemEmiStackMixin extends EmiStack {

    @ModifyArg(
               method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIFI)V",
               at = @At(value = "INVOKE",
                        target = "Ldev/emi/emi/EmiPort;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;",
                        remap = false),
               index = 0,
               remap = false)
    private String formatAmount(String count) {
        if (this.amount > 1L) {
            return gTLAdditions$formatNumber(this.amount);
        } else {
            return count;
        }
    }

    @Unique
    private static String gTLAdditions$formatNumber(long amount) {
        if (amount >= 1_000_000_000) {
            return (amount / 1_000_000_000) + "G";
        } else if (amount >= 1_000_000) {
            return (amount / 1_000_000) + "M";
        } else if (amount >= 1_000) {
            return (amount / 1_000) + "K";
        } else {
            return String.valueOf(amount);
        }
    }
}
