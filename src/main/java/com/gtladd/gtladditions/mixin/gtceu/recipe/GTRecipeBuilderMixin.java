package com.gtladd.gtladditions.mixin.gtceu.recipe;

import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.google.gson.JsonObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.GENESIS_ENGINE;

@Mixin(value = GTRecipeBuilder.class, priority = 2000)
public abstract class GTRecipeBuilderMixin {

    @Shadow(remap = false)
    public int duration;

    @Shadow(remap = false)
    public GTRecipeType recipeType;

    @Inject(method = "toJson", at = @At("TAIL"), remap = false)
    public void toJson(JsonObject json, CallbackInfo ci) {
        if (recipeType == GENESIS_ENGINE) {
            json.remove("duration");
            json.addProperty("duration", duration);
        }
    }
}
