package com.gtladd.gtladditions.mixin.oculus;

import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.targets.RenderTargets;

import com.google.common.collect.ImmutableSet;
import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristIrisPipelineBridge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = IrisRenderingPipeline.class, remap = false)
public abstract class IrisRenderingPipelineMixin implements AntichristIrisPipelineBridge {

    @Unique
    private static final int GTLADDITIONS_ANTICHRIST_FALLBACK_TARGET = 0;

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

    @Unique
    private GlFramebuffer gtladditions$antichristFallbackBeforeTranslucent;

    @Unique
    private GlFramebuffer gtladditions$antichristFallbackAfterTranslucent;

    @Unique
    private int gtladditions$antichristFallbackWidth = -1;

    @Unique
    private int gtladditions$antichristFallbackHeight = -1;

    @Override
    public void gtladditions$bindAntichristFallbackTarget() {
        gtladditions$ensureAntichristFallbackTargets();

        GlFramebuffer fallback = isBeforeTranslucent ? gtladditions$antichristFallbackBeforeTranslucent : gtladditions$antichristFallbackAfterTranslucent;
        fallback.bind();
    }

    @Unique
    private void gtladditions$ensureAntichristFallbackTargets() {
        int width = renderTargets.getCurrentWidth();
        int height = renderTargets.getCurrentHeight();
        if (gtladditions$antichristFallbackBeforeTranslucent != null && gtladditions$antichristFallbackAfterTranslucent != null && width == gtladditions$antichristFallbackWidth && height == gtladditions$antichristFallbackHeight) {
            return;
        }

        gtladditions$destroyAntichristFallbackTargets();
        gtladditions$antichristFallbackBeforeTranslucent = gtladditions$createAntichristFallbackTarget(flippedAfterPrepare);
        gtladditions$antichristFallbackAfterTranslucent = gtladditions$createAntichristFallbackTarget(flippedAfterTranslucent);
        gtladditions$antichristFallbackWidth = width;
        gtladditions$antichristFallbackHeight = height;
    }

    @Unique
    private GlFramebuffer gtladditions$createAntichristFallbackTarget(ImmutableSet<Integer> flippedTargets) {
        int[] drawBuffers = new int[] { GTLADDITIONS_ANTICHRIST_FALLBACK_TARGET };
        return flippedTargets.contains(GTLADDITIONS_ANTICHRIST_FALLBACK_TARGET) ? renderTargets.createFramebufferWritingToAlt(drawBuffers) : renderTargets.createFramebufferWritingToMain(drawBuffers);
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    private void gtladditions$destroyAntichristFallbackTargets(CallbackInfo ci) {
        gtladditions$destroyAntichristFallbackTargets();
    }

    @Unique
    private void gtladditions$destroyAntichristFallbackTargets() {
        if (gtladditions$antichristFallbackBeforeTranslucent != null) {
            renderTargets.destroyFramebuffer(gtladditions$antichristFallbackBeforeTranslucent);
            gtladditions$antichristFallbackBeforeTranslucent = null;
        }
        if (gtladditions$antichristFallbackAfterTranslucent != null) {
            renderTargets.destroyFramebuffer(gtladditions$antichristFallbackAfterTranslucent);
            gtladditions$antichristFallbackAfterTranslucent = null;
        }
        gtladditions$antichristFallbackWidth = -1;
        gtladditions$antichristFallbackHeight = -1;
    }
}