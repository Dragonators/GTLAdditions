package com.gtladd.gtladditions.mixin.gtceu.api.machine.trait;

import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RecipeLogic.class)
public interface RecipeLogicAccessor {

    @Accessor(value = "status", remap = false)
    void setStatusField(RecipeLogic.Status status);
}