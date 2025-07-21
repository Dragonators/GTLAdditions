package com.gtladd.gtladditions.mixin.gtceu.recipe;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.data.recipe.generated.MaterialRecipeHandler;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;

import com.gtladd.gtladditions.GTLAdditions;
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(MaterialRecipeHandler.class)
public class MaterialRecipeHandlerMixin {

    @Inject(method = "processEBFRecipe", at = @At(value = "INVOKE", target = "Lcom/gregtechceu/gtceu/api/data/chemical/material/properties/BlastProperty;getEUtOverride()I"), remap = false)
    private static void processEBFRecipe(Material material, BlastProperty property, ItemStack output, Consumer<FinishedRecipe> provider, CallbackInfo ci) {
        if (TagPrefix.ingotHot.doGenerateItem(material)) {
            GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION.recipeBuilder(GTLAdditions.id("cool_hot_" + material.getName() + "_ingot"))
                    .inputItems(TagPrefix.ingotHot, material).outputItems(TagPrefix.ingot, material)
                    .duration((int) material.getMass() * 2).EUt(120).save(provider);
        }
    }
}
