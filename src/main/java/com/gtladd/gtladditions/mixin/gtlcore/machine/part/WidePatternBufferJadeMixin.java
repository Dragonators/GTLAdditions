package com.gtladd.gtladditions.mixin.gtlcore.machine.part;

import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEPatternBufferPartMachineBase;
import org.gtlcore.gtlcore.integration.jade.provider.MEPatternBufferProvider;

import net.minecraft.nbt.CompoundTag;

import com.gtladd.gtladditions.integration.ae2.WideBufferDisplay;
import com.gtladd.gtladditions.integration.ae2.WidePatternBufferAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import snownee.jade.api.ITooltip;

@Mixin(value = MEPatternBufferProvider.class, remap = false)
public abstract class WidePatternBufferJadeMixin {

    @Inject(method = "putTag", at = @At("HEAD"), cancellable = true)
    private static void gtladd$preciseBufferTag(CompoundTag tag, MEPatternBufferPartMachineBase machine, CallbackInfo ci) {
        var pending = machine instanceof WidePatternBufferAccess access ? access.widePending() : null;
        if (pending != null) {
            WideBufferDisplay.INSTANCE.write(tag, machine, pending);
            ci.cancel();
        }
    }

    @Inject(method = "readBufferContents", at = @At("HEAD"), cancellable = true)
    private static void gtladd$preciseBufferTooltip(ITooltip tooltip, CompoundTag tag, CallbackInfo ci) {
        if (tag.getBoolean(WideBufferDisplay.MARKER)) {
            WideBufferDisplay.INSTANCE.read(tooltip, tag);
            ci.cancel();
        }
    }
}