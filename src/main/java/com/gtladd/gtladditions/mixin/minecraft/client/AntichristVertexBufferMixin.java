package com.gtladd.gtladditions.mixin.minecraft.client;

import net.minecraft.client.renderer.ShaderInstance;

import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristOculusCompat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexBuffer.class)
public abstract class AntichristVertexBufferMixin {

    @Inject(
            method = "_drawWithShader",
            at = @At(
                     value = "INVOKE",
                     target = "Lnet/minecraft/client/renderer/ShaderInstance;apply()V",
                     shift = At.Shift.BEFORE))
    private void gtladditions$unlockOculusDepthColorBeforeAntichristDraw(
                                                                         Matrix4f modelViewMatrix, Matrix4f projectionMatrix, ShaderInstance shader, CallbackInfo ci) {
        if (AntichristOculusCompat.isAntichristShader(shader.getName())) {
            AntichristOculusCompat.beginAntichristShaderPass();
        }
    }

    @Inject(
            method = "_drawWithShader",
            at = @At(
                     value = "INVOKE",
                     target = "Lnet/minecraft/client/renderer/ShaderInstance;clear()V",
                     shift = At.Shift.AFTER))
    private void gtladditions$bindMainTargetAfterAntichristDraw(
                                                                Matrix4f modelViewMatrix, Matrix4f projectionMatrix, ShaderInstance shader, CallbackInfo ci) {
        if (AntichristOculusCompat.isAntichristShader(shader.getName())) {
            AntichristOculusCompat.endAntichristShaderPass();
        }
    }
}