package com.gtladd.gtladditions.client.render.machine.antichrist;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.gtladd.gtladditions.mixin.oculus.DepthColorStorageAccessor;

@OnlyIn(Dist.CLIENT)
public final class AntichristOculusCompat {

    private static final String STAR_SHADER = "gtladditions:gtladditions_antichrist_star";
    private static final String BEAM_SHADER = "gtladditions:gtladditions_antichrist_beam";

    private AntichristOculusCompat() {}

    public static boolean isAntichristShader(String shaderName) {
        return STAR_SHADER.equals(shaderName) || BEAM_SHADER.equals(shaderName);
    }

    public static void unlockDepthColorForAntichristShader() {
        if (DepthColorStorageAccessor.gtladditions$isDepthColorLocked()) {
            DepthColorStorageAccessor.gtladditions$unlockDepthColor();
        }
    }

    public static void bindFallbackTargetForAntichristShader() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline instanceof AntichristIrisPipelineBridge bridge) {
            bridge.gtladditions$bindAntichristFallbackTarget();
            pipeline.getRenderTargetStateListener().setIsMainBound(false);
        } else {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        }
        unlockDepthColorForAntichristShader();
    }

    public static void bindMainTargetAfterAntichristShader() {
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
    }

    public static boolean shouldWriteStarShellDepthForShaderpack() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        return pipeline instanceof AntichristIrisPipelineBridge && pipeline instanceof ShaderRenderingPipeline shaderPipeline && shaderPipeline.shouldOverrideShaders();
    }
}