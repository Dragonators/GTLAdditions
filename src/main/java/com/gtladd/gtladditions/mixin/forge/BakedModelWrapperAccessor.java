package com.gtladd.gtladditions.mixin.forge;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraftforge.client.model.BakedModelWrapper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = BakedModelWrapper.class, remap = false)
public interface BakedModelWrapperAccessor {

    @Accessor("originalModel")
    BakedModel getOriginalModel();
}