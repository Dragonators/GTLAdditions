package com.gtladd.gtladditions.mixin.gtceu.recipe;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.gregtechceu.gtceu.data.recipe.generated.MaterialRecipeHandler;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

import static org.gtlcore.gtlcore.common.data.GTLMaterials.Eternity;

@Mixin(MaterialRecipeHandler.class)
public abstract class MaterialRecipeHandlerMixin {

    @Inject(
            method = "processEBFRecipe",
            at = @At(
                     value = "INVOKE",
                     target = "Lcom/gregtechceu/gtceu/data/recipe/builder/GTRecipeBuilder;save(Ljava/util/function/Consumer;)V",
                     ordinal = 2),
            remap = false)
    private static void onEBFRecipeBeforeSave(
                                              Material material, BlastProperty property, ItemStack output, Consumer<FinishedRecipe> provider, CallbackInfo ci, @Local GTRecipeBuilder blastBuilder) {
        if (material == Eternity) {
            blastBuilder.circuitMeta(1);
        }
    }
}
