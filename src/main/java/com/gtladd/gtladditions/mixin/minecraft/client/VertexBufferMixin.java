package com.gtladd.gtladditions.mixin.minecraft.client;

import net.minecraft.client.renderer.ShaderInstance;

import com.gtladd.gtladditions.client.RenderUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VertexBuffer.class)
public abstract class VertexBufferMixin {

    @WrapOperation(method = "_drawWithShader",
                   at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ShaderInstance;clear()V"))
    private void gtladditions$clearAfterTerrainBatch(ShaderInstance shader, Operation<Void> original) {
        if (!RenderUtils.shouldDeferShaderClear(shader)) original.call(shader);
    }
}