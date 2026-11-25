package com.gtladd.gtladditions.mixin.gtlcore.client;

import org.gtlcore.gtlcore.client.preview.PreviewShapeCache;
import org.gtlcore.gtlcore.client.preview.SparsePreviewShape;
import org.gtlcore.gtlcore.client.preview.WorldPreview;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;

import net.minecraft.core.BlockPos;

import com.gtladd.gtladditions.api.machine.feature.IStructureTierMachine;
import com.gtladd.gtladditions.client.gui.StructureTierPreview;
import kotlin.ranges.IntRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The range takes the cache key's page, always 0 here. */
@Mixin(value = WorldPreview.class, remap = false)
public abstract class WorldPreviewMixin {

    @Unique
    private static IStructureTierMachine gtladditions$machine;

    @Unique
    private static IntRange gtladditions$range;

    @Inject(method = "show", at = @At("HEAD"))
    private static void gtladditions$rememberMachine(BlockPos pos, MultiblockControllerMachine controller, int ticks, CallbackInfoReturnable<Boolean> cir) {
        gtladditions$machine = controller instanceof IStructureTierMachine machine ? machine : null;
        StructureTierPreview.INSTANCE.opened(gtladditions$machine == null ? null : pos, ticks);
        if (gtladditions$machine != null) gtladditions$range = StructureTierPreview.INSTANCE.shown(gtladditions$machine);
    }

    @Redirect(method = "show",
              at = @At(value = "INVOKE",
                       target = "Lorg/gtlcore/gtlcore/client/preview/PreviewShapeCache;sparse(Lcom/gregtechceu/gtceu/api/pattern/MultiblockShapeInfo;Lcom/gregtechceu/gtceu/api/machine/MultiblockMachineDefinition;)Lorg/gtlcore/gtlcore/client/preview/SparsePreviewShape;"))
    private static SparsePreviewShape gtladditions$cutToTiers(MultiblockShapeInfo shape, MultiblockMachineDefinition definition) {
        SparsePreviewShape full = PreviewShapeCache.sparse(shape, definition);
        if (gtladditions$machine == null) return full;
        return StructureTierPreview.INSTANCE.filter(full, definition, gtladditions$machine, gtladditions$range);
    }

    @Inject(method = "show", at = @At("RETURN"))
    private static void gtladditions$forgetMachine(BlockPos pos, MultiblockControllerMachine controller, int ticks, CallbackInfoReturnable<Boolean> cir) {
        // Declined as small, the preview goes on to GTLCore's overwrite of GT's showPreview.
        StructureTierPreview.INSTANCE.deferCut(cir.getReturnValueZ() ? null : gtladditions$machine, gtladditions$range);
        gtladditions$machine = null;
        gtladditions$range = null;
    }

    @ModifyArg(method = "show",
               at = @At(value = "INVOKE",
                        target = "Lorg/gtlcore/gtlcore/client/preview/PreviewScenes$Key;<init>(ILjava/lang/Object;Lnet/minecraft/resources/ResourceLocation;IIZ)V"),
               index = 3)
    private static int gtladditions$rangeInKey(int page) {
        if (gtladditions$machine == null) return page;
        return gtladditions$range.getFirst() * 16 + gtladditions$range.getLast();
    }
}
