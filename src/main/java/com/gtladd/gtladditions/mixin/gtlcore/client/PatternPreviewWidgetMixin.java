package com.gtladd.gtladditions.mixin.gtlcore.client;

import org.gtlcore.gtlcore.api.gui.PatternPreviewWidget;
import org.gtlcore.gtlcore.client.preview.PreviewHeight;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import appeng.api.stacks.GenericStack;
import com.gtladd.gtladditions.api.machine.feature.IStructureTierMachine;
import com.gtladd.gtladditions.client.gui.StructureTierPreview;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** The tier enters the scene's cache key through the layer. */
@Mixin(value = PatternPreviewWidget.class, remap = false)
public abstract class PatternPreviewWidgetMixin {

    @Shadow
    private int index;

    @Shadow
    private boolean showModules;

    @Unique
    private Object gtladditions$pattern;

    /** 0 for machines without tiers. */
    @Unique
    private int gtladditions$tier;

    @Shadow
    public abstract void setPage(int index, ClickData x);

    @Shadow
    private IGuiTexture controlTexture(Supplier<String> label) {
        throw new AssertionError();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void gtladditions$addTierButton(MultiblockMachineDefinition controllerDefinition, CallbackInfo ci) {
        if (!(gtladditions$pattern instanceof PatternPreviewPatternAccessor pattern) ||
                !(pattern.getControllerBase() instanceof IStructureTierMachine machine))
            return;
        var layout = machine.getStructureTierTrait().getLayout();
        gtladditions$tier = layout.getMaxTier();
        int y = pattern.getHasModule() ? 90 : 70;
        ((WidgetGroup) (Object) this).addWidget(new ButtonWidget(138, y, 18, 18, controlTexture(() -> "T:" + gtladditions$tier), click -> {
            if (click.isShiftClick) gtladditions$tier = layout.getMaxTier();
            else gtladditions$tier = gtladditions$tier >= layout.getMaxTier() ? layout.getMinTier() : gtladditions$tier + 1;
            setPage(index, null);
        }).setHoverBorderTexture(1, -1).setHoverTooltips(Component.translatable("gtladditions.structure_tier.preview")));
    }

    @Inject(method = "setupScene", at = @At("HEAD"))
    private void gtladditions$rememberPattern(@Coerce Object pattern, CallbackInfo ci) {
        gtladditions$pattern = pattern;
    }

    @ModifyArg(method = "setupScene",
               at = @At(value = "INVOKE",
                        target = "Lorg/gtlcore/gtlcore/client/preview/PreviewScenes;configure(Lcom/lowdragmc/lowdraglib/gui/widget/SceneWidget;Lcom/gregtechceu/gtceu/api/machine/MultiblockMachineDefinition;IIZ)V"),
               index = 3)
    private int gtladditions$tierInKey(int layer) {
        return gtladditions$tier == 0 ? layer : layer * 16 + gtladditions$tier;
    }

    @ModifyArg(method = "setupScene",
               at = @At(value = "INVOKE",
                        target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;",
                        ordinal = 0))
    private Predicate<BlockPos> gtladditions$hideTiers(Predicate<BlockPos> shown) {
        LongSet hidden = gtladditions$hidden();
        return hidden.isEmpty() ? shown : shown.and(pos -> !hidden.contains(pos.asLong()));
    }

    @Inject(method = "patternIngredients(Z)Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private void gtladditions$hideTierMaterials(boolean filterDefaults, CallbackInfoReturnable<List<List<GenericStack>>> cir) {
        LongSet hidden = gtladditions$hidden();
        if (hidden.isEmpty()) return;
        var materials = ((PatternPreviewPatternAccessor) gtladditions$pattern).getMaterials();
        cir.setReturnValue(StructureTierPreview.INSTANCE.materials(cir.getReturnValue(), materials, hidden));
    }

    @Inject(method = "dimensions", at = @At("HEAD"), cancellable = true)
    private void gtladditions$tierDimensions(CallbackInfoReturnable<PreviewHeight.Dimensions> cir) {
        PreviewHeight bounds = gtladditions$bounds();
        if (bounds != null) cir.setReturnValue(bounds.dimensions(showModules));
    }

    @Inject(method = "controllerHeight", at = @At("HEAD"), cancellable = true)
    private void gtladditions$tierControllerHeight(CallbackInfoReturnable<Integer> cir) {
        PreviewHeight bounds = gtladditions$bounds();
        if (bounds == null) return;
        int controllerY = ((PatternPreviewPatternAccessor) gtladditions$pattern).getControllerBase().self().getPos().getY();
        cir.setReturnValue(bounds.aboveBottom(controllerY, showModules));
    }

    @Unique
    private LongSet gtladditions$hidden() {
        if (gtladditions$tier == 0 || !(gtladditions$pattern instanceof PatternPreviewPatternAccessor pattern)) return LongSets.EMPTY_SET;
        return StructureTierPreview.INSTANCE.hidden(pattern.getControllerBase(), gtladditions$tier);
    }

    @Unique
    private PreviewHeight gtladditions$bounds() {
        LongSet hidden = gtladditions$hidden();
        if (hidden.isEmpty()) return null;
        var pattern = (PatternPreviewPatternAccessor) gtladditions$pattern;
        return StructureTierPreview.INSTANCE.bounds(pattern.getControllerBase(), gtladditions$tier, pattern.getBlockMap(), pattern.getModuleOnlyBlocks(), hidden);
    }
}
