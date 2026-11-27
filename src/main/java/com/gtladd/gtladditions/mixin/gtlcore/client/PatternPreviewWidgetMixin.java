package com.gtladd.gtladditions.mixin.gtlcore.client;

import org.gtlcore.gtlcore.api.gui.PatternPreviewWidget;
import org.gtlcore.gtlcore.client.preview.PreviewControls;
import org.gtlcore.gtlcore.client.preview.PreviewHeight;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.gtladd.gtladditions.api.machine.feature.IStructureTierMachine;
import com.gtladd.gtladditions.api.pattern.StructureTierLayout;
import com.gtladd.gtladditions.client.gui.StructureTierPreview;
import kotlin.ranges.IntRange;
import org.spongepowered.asm.mixin.Final;
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
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** The shown tiers (below 16) enter the scene's cache key through the layer. */
@Mixin(value = PatternPreviewWidget.class, remap = false)
public abstract class PatternPreviewWidgetMixin {

    @Shadow
    private int index;

    @Shadow
    private boolean showModules;

    @Shadow
    @Final
    private MultiblockMachineDefinition controllerDefinition;

    @Shadow
    @Final
    private Map<BlockPos, AEItemKey> materialReplacements;

    @Shadow
    private PreviewControls previewControls;

    @Unique
    private Object gtladditions$pattern;

    /** Null for machines without tiers. */
    @Unique
    private IntRange gtladditions$shown;

    @Unique
    private boolean gtladditions$upgrade;

    @Unique
    private ButtonWidget gtladditions$upgradeButton;

    @Shadow
    public abstract void setPage(int index, ClickData x);

    @Shadow
    private IGuiTexture controlTexture(Supplier<String> label) {
        throw new AssertionError();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void gtladditions$addTierButtons(MultiblockMachineDefinition controllerDefinition, CallbackInfo ci) {
        if (!(gtladditions$pattern instanceof PatternPreviewPatternAccessor pattern) ||
                !(pattern.getControllerBase() instanceof IStructureTierMachine machine))
            return;
        var layout = machine.getStructureTierTrait().getLayout();
        gtladditions$shown = new IntRange(0, layout.getMaxTier());
        var widget = (WidgetGroup) (Object) this;
        int y = pattern.getHasModule() ? 90 : 70;

        widget.addWidget(new ButtonWidget(138, y, 18, 18, controlTexture(() -> "T:" + gtladditions$shown.getLast()), click -> {
            int tier = gtladditions$shown.getLast();
            if (click.isShiftClick) tier = layout.getMaxTier();
            else tier = tier >= layout.getMaxTier() ? layout.getMinTier() : tier + 1;
            gtladditions$show(layout, tier);
        }).setHoverBorderTexture(1, -1).setHoverTooltips(Component.translatable("gtladditions.structure_tier.preview")));

        gtladditions$upgradeButton = new ButtonWidget(138, y + 20, 18, 18, controlTexture(() -> StructureTierPreview.INSTANCE.upgradeLabel(layout, gtladditions$shown)), click -> {
            if (gtladditions$shown.getLast() == layout.getMinTier()) return;
            gtladditions$upgrade = !gtladditions$upgrade;
            gtladditions$show(layout, gtladditions$shown.getLast());
        });
        gtladditions$upgradeButton.setHoverBorderTexture(1, -1).setHoverTooltips(StructureTierPreview.INSTANCE.upgradeTooltips(layout, gtladditions$shown));
        widget.addWidget(gtladditions$upgradeButton);
    }

    @Unique
    private void gtladditions$show(StructureTierLayout layout, int tier) {
        gtladditions$shown = StructureTierPreview.INSTANCE.shown(layout, tier, gtladditions$upgrade);
        gtladditions$upgradeButton.setHoverTooltips(StructureTierPreview.INSTANCE.upgradeTooltips(layout, gtladditions$shown));
        var panel = (PreviewPatternPanelAccessor) (Object) ((PreviewControlsAccessor) (Object) previewControls).getPatternPanel();
        panel.setOutputs(StructureTierPreview.INSTANCE.outputs(controllerDefinition, layout, gtladditions$shown));
        setPage(index, null);
    }

    @Inject(method = "setupScene", at = @At("HEAD"))
    private void gtladditions$rememberPattern(@Coerce Object pattern, CallbackInfo ci) {
        gtladditions$pattern = pattern;
    }

    @ModifyArg(method = "setupScene",
               at = @At(value = "INVOKE",
                        target = "Lorg/gtlcore/gtlcore/client/preview/PreviewScenes;configure(Lcom/lowdragmc/lowdraglib/gui/widget/SceneWidget;Lcom/gregtechceu/gtceu/api/machine/MultiblockMachineDefinition;IIZ)V"),
               index = 3)
    private int gtladditions$tiersInKey(int layer) {
        if (gtladditions$shown == null) return layer;
        return (layer * 16 + gtladditions$shown.getFirst()) * 16 + gtladditions$shown.getLast();
    }

    @ModifyArg(method = { "setupScene", "selectPatternMaterial" },
               at = @At(value = "INVOKE",
                        target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;",
                        ordinal = 0))
    private Predicate<BlockPos> gtladditions$hideTiers(Predicate<BlockPos> shown) {
        if (gtladditions$shown == null) return shown;
        return StructureTierPreview.INSTANCE.filterShown((PatternPreviewPatternAccessor) gtladditions$pattern, gtladditions$shown, shown);
    }

    @Inject(method = "patternIngredients(Z)Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private void gtladditions$hideTierMaterials(boolean filterDefaults, CallbackInfoReturnable<List<List<GenericStack>>> cir) {
        if (gtladditions$shown == null) return;
        var current = cir.getReturnValue();
        var filtered = StructureTierPreview.INSTANCE.materials((PatternPreviewPatternAccessor) gtladditions$pattern, gtladditions$shown, current, materialReplacements);
        if (filtered != current) cir.setReturnValue(filtered);
    }

    @Inject(method = "dimensions", at = @At("HEAD"), cancellable = true)
    private void gtladditions$tierDimensions(CallbackInfoReturnable<PreviewHeight.Dimensions> cir) {
        if (gtladditions$shown == null) return;
        var dimensions = StructureTierPreview.INSTANCE.dimensions((PatternPreviewPatternAccessor) gtladditions$pattern, gtladditions$shown, showModules);
        if (dimensions != null) cir.setReturnValue(dimensions);
    }

    @Inject(method = "controllerHeight", at = @At("HEAD"), cancellable = true)
    private void gtladditions$tierControllerHeight(CallbackInfoReturnable<Integer> cir) {
        if (gtladditions$shown == null) return;
        Integer height = StructureTierPreview.INSTANCE.controllerHeight((PatternPreviewPatternAccessor) gtladditions$pattern, gtladditions$shown, showModules);
        if (height != null) cir.setReturnValue(height);
    }
}
