package com.gtladd.gtladditions.mixin.gtlcore.machine.part;

import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEPatternBufferPartMachineBase;
import org.gtlcore.gtlcore.integration.jade.provider.MEPatternBufferProvider;

import net.minecraft.nbt.CompoundTag;

import com.gtladd.gtladditions.api.machine.IWidePatternBuffer;
import com.gtladd.gtladditions.utils.wideBuffer.WideBufferDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import snownee.jade.api.ITooltip;

@Mixin(value = MEPatternBufferProvider.class, remap = false)
public abstract class WidePatternBufferJadeMixin {

    @Inject(method = "putTag", at = @At("HEAD"), cancellable = true)
    private static void gtladd$preciseBufferTag(CompoundTag compoundTag, MEPatternBufferPartMachineBase buffer, CallbackInfo ci) {
        var pending = buffer instanceof IWidePatternBuffer access ? access.widePending() : null;
        if (pending != null) {
            WideBufferDisplay.INSTANCE.write(compoundTag, buffer, pending);
            ci.cancel();
        }
    }

    @Inject(method = "readBufferContents", at = @At("HEAD"), cancellable = true)
    private static void gtladd$preciseBufferTooltip(ITooltip iTooltip, CompoundTag serverData, CallbackInfo ci) {
        if (serverData.getBoolean(WideBufferDisplay.MARKER)) {
            WideBufferDisplay.INSTANCE.read(iTooltip, serverData);
            ci.cancel();
        }
    }
}
