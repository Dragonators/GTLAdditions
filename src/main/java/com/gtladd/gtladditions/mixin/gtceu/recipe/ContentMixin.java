package com.gtladd.gtladditions.mixin.gtceu.recipe;

import com.gregtechceu.gtceu.api.recipe.content.Content;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(Content.class)
public abstract class ContentMixin {

    @WrapOperation(method = "lambda$codec$6",
                   at = @At(value = "INVOKE",
                            target = "Lcom/mojang/serialization/Codec;optionalFieldOf(Ljava/lang/String;Ljava/lang/Object;)Lcom/mojang/serialization/MapCodec;"),
                   slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=tierChanceBoost"),
                                  to = @At(value = "CONSTANT", args = "stringValue=slotName")),
                   remap = false,
                   require = 1,
                   allow = 1)
    private static MapCodec<?> gtladditions$allowNegativeTierBoost(Codec<?> codec, String name,
                                                                   Object defaultValue, Operation<MapCodec<?>> original) {
        return original.call(Codec.INT, name, defaultValue);
    }
}
