package com.gtladd.gtladditions.mixin.minecraft.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;

@Mixin(MultiBufferSource.BufferSource.class)
public interface BufferSourceAccessor {

    @Accessor("lastState")
    Optional<RenderType> getActiveRenderType();

    @Invoker("getBuilderRaw")
    BufferBuilder getRawBuffer(RenderType type);
}