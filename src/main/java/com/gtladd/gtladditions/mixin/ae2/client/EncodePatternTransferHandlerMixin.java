package com.gtladd.gtladditions.mixin.ae2.client;

import org.gtlcore.gtlcore.api.gui.PatternPreviewWidget;

import com.gregtechceu.gtceu.integration.jei.multipage.MultiblockInfoWrapper;

import com.lowdragmc.lowdraglib.gui.widget.Widget;

import appeng.api.stacks.GenericStack;
import appeng.integration.modules.jei.transfer.EncodePatternTransferHandler;
import com.gtladd.gtladditions.integration.xei.structureupgrade.StructureUpgradeJeiCategory;
import com.gtladd.gtladditions.mixin.gtlcore.client.PreviewControlsAccessor;
import com.gtladd.gtladditions.mixin.gtlcore.client.PreviewPatternPanelAccessor;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;

/** Priority 1100 runs after GTLCore's multiblock output on this method. */
@Mixin(value = EncodePatternTransferHandler.class, remap = false, priority = 1100)
public class EncodePatternTransferHandlerMixin {

    @ModifyArg(method = "transferRecipe(Lappeng/menu/me/items/PatternEncodingTermMenu;Ljava/lang/Object;Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/world/entity/player/Player;ZZ)Lmezz/jei/api/recipe/transfer/IRecipeTransferError;",
               at = @At(value = "INVOKE",
                        target = "Lappeng/integration/modules/jeirei/EncodingHelper;encodeProcessingRecipe(Lappeng/menu/me/items/PatternEncodingTermMenu;Ljava/util/List;Ljava/util/List;)V"),
               index = 2)
    private List<GenericStack> gtladditions$patternOutput(List<GenericStack> outputs, @Local(argsOnly = true) Object recipeBase) {
        if (recipeBase instanceof StructureUpgradeJeiCategory.Wrapper wrapper) {
            return List.of(GenericStack.fromItemStack(wrapper.getEntry().patternOutput()));
        }
        if (recipeBase instanceof MultiblockInfoWrapper wrapper) {
            // GTLCore swaps in its own widget; reading it as Widget emits no cast to GTCEu's type.
            Widget widget = wrapper.getWidget();
            if (widget instanceof PatternPreviewWidget preview) {
                var panel = ((PreviewControlsAccessor) (Object) preview.getPreviewControls()).getPatternPanel();
                return ((PreviewPatternPanelAccessor) (Object) panel).getOutputs();
            }
        }
        return outputs;
    }
}
