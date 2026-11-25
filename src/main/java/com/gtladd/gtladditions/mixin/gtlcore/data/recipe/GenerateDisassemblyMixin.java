package com.gtladd.gtladditions.mixin.gtlcore.data.recipe;

import org.gtlcore.gtlcore.data.recipe.GenerateDisassembly;

import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.data.recipes.FinishedRecipe;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(GenerateDisassembly.class)
public abstract class GenerateDisassemblyMixin {

    @Inject(method = "generateDisassembly", at = @At("HEAD"), remap = false, cancellable = true)
    private static void beforeGenerateDisassembly(GTRecipeBuilder r, Consumer<FinishedRecipe> p, CallbackInfo ci) {
        if (r.id.getPath().contains("ultimate_tea")) {
            ci.cancel();
        }
    }
}
