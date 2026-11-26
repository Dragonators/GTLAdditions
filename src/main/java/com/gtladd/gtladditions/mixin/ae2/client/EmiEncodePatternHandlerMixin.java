package com.gtladd.gtladditions.mixin.ae2.client;

import appeng.api.stacks.GenericStack;
import appeng.integration.modules.emi.EmiEncodePatternHandler;
import com.gtladd.gtladditions.integration.xei.structureupgrade.StructureUpgradeEmiCategory;
import com.llamalad7.mixinextras.sugar.Local;
import dev.emi.emi.api.recipe.EmiRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;

@Mixin(value = EmiEncodePatternHandler.class, remap = false)
public class EmiEncodePatternHandlerMixin {

    @ModifyArg(method = "transferRecipe(Lappeng/menu/me/items/PatternEncodingTermMenu;Lnet/minecraft/world/item/crafting/Recipe;Ldev/emi/emi/api/recipe/EmiRecipe;Z)Lappeng/integration/modules/emi/AbstractRecipeHandler$Result;",
               at = @At(value = "INVOKE",
                        target = "Lappeng/integration/modules/jeirei/EncodingHelper;encodeProcessingRecipe(Lappeng/menu/me/items/PatternEncodingTermMenu;Ljava/util/List;Ljava/util/List;)V"),
               index = 2)
    private List<GenericStack> gtladditions$structureUpgradeOutput(List<GenericStack> outputs, @Local(argsOnly = true) EmiRecipe emiRecipe) {
        if (!(emiRecipe instanceof StructureUpgradeEmiCategory.Recipe recipe)) return outputs;
        return List.of(GenericStack.fromItemStack(recipe.getEntry().patternOutput()));
    }
}
