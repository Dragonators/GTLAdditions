package com.gtladd.gtladditions.mixin.gtceu.client;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;

import com.gtladd.gtladditions.client.gui.StructureTierPreview;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Priority 900 runs ahead of GTLCore's shape cache on this method. */
@Mixin(value = MultiblockMachineDefinition.class, remap = false, priority = 900)
public abstract class MultiblockMachineDefinitionMixin {

    @Inject(method = "getMatchingShapes", at = @At("HEAD"), cancellable = true)
    private void gtladditions$tierCut(CallbackInfoReturnable<List<MultiblockShapeInfo>> cir) {
        List<MultiblockShapeInfo> cut = StructureTierPreview.INSTANCE.takeCut((MultiblockMachineDefinition) (Object) this);
        if (cut != null) cir.setReturnValue(cut);
    }
}
