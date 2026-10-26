package com.gtladd.gtladditions.mixin.oculus;

import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.shaderpack.programs.ProgramFallbackResolver;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.targets.RenderTargets;
import net.minecraft.client.renderer.ShaderInstance;

import com.google.common.collect.ImmutableSet;
import com.gtladd.gtladditions.client.render.machine.antichrist.RingBatchShaderProgram;
import com.gtladd.gtladditions.client.render.machine.antichrist.RingStructureVertexBuffer;
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredFramebufferTargets;
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredIrisPipelineBridge;
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredMachineRenderer;
import com.gtladd.gtladditions.client.render.machine.dimensionfocus.DimensionFocusOrbitRenderer;
import com.gtladd.gtladditions.client.render.machine.heart.HeartHandDepthMask;
import com.gtladd.gtladditions.client.render.machine.matrix.MatrixFrameTerrainRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.Optional;

@Mixin(value = IrisRenderingPipeline.class, remap = false)
@Implements(@Interface(iface = DeferredIrisPipelineBridge.class, prefix = "gtladditions$"))
public abstract class IrisRenderingPipelineMixin {

    @Shadow
    @Final
    private ProgramFallbackResolver resolver;
    @Shadow
    @Final
    private RenderTargets renderTargets;
    @Shadow
    @Final
    private ImmutableSet<Integer> flippedAfterPrepare;
    @Shadow
    @Final
    private ImmutableSet<Integer> flippedAfterTranslucent;
    @Shadow
    public boolean isBeforeTranslucent;
    @Shadow
    private boolean isMainBound;

    @Shadow
    private ShaderInstance createShader(String name, Optional<ProgramSource> source, ShaderKey key) throws IOException {
        throw new AssertionError();
    }

    @Unique
    private final RingBatchShaderProgram gtladditions$ringShader = new RingBatchShaderProgram();
    @Unique
    private DeferredFramebufferTargets gtladditions$deferredTargets;

    @Unique
    public ShaderInstance gtladditions$getFoaBatchShader() {
        return gtladditions$ringShader.getOrCreate(this, resolver, this::createShader);
    }

    @Unique
    public void gtladditions$beginDeferredTarget() {
        gtladditions$deferredTargets.begin(isBeforeTranslucent, isMainBound);
        isMainBound = false;
    }

    @Unique
    public void gtladditions$endDeferredTarget() {
        isMainBound = gtladditions$deferredTargets.end(isMainBound);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void gtladditionsInternal$initializeMachineRendering(ProgramSet programSet, CallbackInfo ci) {
        gtladditions$deferredTargets = new DeferredFramebufferTargets(renderTargets, flippedAfterPrepare, flippedAfterTranslucent);
        RingStructureVertexBuffer.useShaderCompatibleTerrainUploadMode();
        MatrixFrameTerrainRenderer.useShaderCompatibleTerrainUploadMode();
        DimensionFocusOrbitRenderer.useShaderCompatibleTerrainUploadMode();
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    private void gtladditionsInternal$destroyMachineRendering(CallbackInfo ci) {
        RingStructureVertexBuffer.useVanillaCompatibleTerrainUploadMode();
        MatrixFrameTerrainRenderer.useVanillaCompatibleTerrainUploadMode();
        DimensionFocusOrbitRenderer.useVanillaCompatibleTerrainUploadMode();
        gtladditions$deferredTargets.close();
        HeartHandDepthMask.destroy();
    }

    @Inject(method = "setPhase", at = @At("HEAD"))
    private void gtladditionsInternal$beginHeartHandPhase(WorldRenderingPhase phase, CallbackInfo ci) {
        if (phase == WorldRenderingPhase.HAND_SOLID) {
            HeartHandDepthMask.beginSolidHand();
        } else if (phase == WorldRenderingPhase.HAND_TRANSLUCENT) {
            HeartHandDepthMask.beginTranslucentHand();
        }
    }

    @Inject(method = "finalizeLevelRendering", at = @At("HEAD"))
    private void gtladditionsInternal$finishHeartHandMask(CallbackInfo ci) {
        HeartHandDepthMask.finishFrame(renderTargets);
    }

    @Inject(method = "finalizeLevelRendering", at = @At("TAIL"))
    private void gtladditionsInternal$renderMachinesAfterShaderpackFinal(CallbackInfo ci) {
        DeferredMachineRenderer.renderAfterShaderpackFinal();
    }
}