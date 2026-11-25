package com.gtladd.gtladditions.mixin.forge;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraftforge.client.model.CompositeModel;

import com.google.common.collect.ImmutableMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = CompositeModel.Baked.class, remap = false)
public interface CompositeModelAccessor {

    @Accessor("children")
    ImmutableMap<String, BakedModel> getChildren();
}
