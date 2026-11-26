package com.gtladd.gtladditions.mixin.ae2.client;

import appeng.api.stacks.GenericStack;
import appeng.integration.modules.jei.transfer.EncodePatternTransferHandler;
import com.gtladd.gtladditions.integration.xei.structureupgrade.StructureUpgradeJeiCategory;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;

@Mixin(value = EncodePatternTransferHandler.class, remap = false)
public class EncodePatternTransferHandlerMixin {

    @ModifyArg(method = "transferRecipe(Lappeng/menu/me/items/PatternEncodingTermMenu;Ljava/lang/Object;Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/world/entity/player/Player;ZZ)Lmezz/jei/api/recipe/transfer/IRecipeTransferError;",
               at = @At(value = "INVOKE",
                        target = "Lappeng/integration/modules/jeirei/EncodingHelper;encodeProcessingRecipe(Lappeng/menu/me/items/PatternEncodingTermMenu;Ljava/util/List;Ljava/util/List;)V"),
               index = 2)
    private List<GenericStack> gtladditions$structureUpgradeOutput(List<GenericStack> outputs, @Local(argsOnly = true) Object recipeBase) {
        if (!(recipeBase instanceof StructureUpgradeJeiCategory.Wrapper wrapper)) return outputs;
        return List.of(GenericStack.fromItemStack(wrapper.getEntry().patternOutput()));
    }
}
