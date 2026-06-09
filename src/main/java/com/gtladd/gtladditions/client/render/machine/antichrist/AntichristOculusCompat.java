package com.gtladd.gtladditions.client.render.machine.antichrist;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.gtladd.gtladditions.mixin.oculus.DepthColorStorageAccessor;

import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public final class AntichristOculusCompat {

    private static final String STAR_SHADER = "gtladditions:gtladditions_antichrist_star";
    private static final String BEAM_SHADER = "gtladditions:gtladditions_antichrist_beam";
    private static int directMainTargetDepth;

    private AntichristOculusCompat() {}

    public static boolean isAntichristShader(String shaderName) {
        return STAR_SHADER.equals(shaderName) || BEAM_SHADER.equals(shaderName);
    }

    public static void unlockDepthColorForAntichristShader() {
        if (DepthColorStorageAccessor.gtladditions$isDepthColorLocked()) {
            DepthColorStorageAccessor.gtladditions$unlockDepthColor();
        }
    }

    public static void beginAntichristShaderPass() {
        if (directMainTargetDepth > 0) {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
            unlockDepthColorForAntichristShader();
            return;
        }

        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline instanceof AntichristIrisPipelineBridge bridge) {
            bridge.gtladditions$beginAntichristFallbackTarget();
        } else {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        }
        unlockDepthColorForAntichristShader();
    }

    public static void endAntichristShaderPass() {
        if (directMainTargetDepth > 0) {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
            return;
        }

        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline instanceof AntichristIrisPipelineBridge bridge) {
            bridge.gtladditions$endAntichristFallbackTarget();
        } else {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        }
    }

    public static void beginDirectMainTargetPass() {
        directMainTargetDepth++;
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        unlockDepthColorForAntichristShader();
    }

    public static void endDirectMainTargetPass() {
        if (directMainTargetDepth > 0) {
            directMainTargetDepth--;
        }
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
    }

    public static boolean shouldRenderAfterShaderpackFinal() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (!(pipeline instanceof AntichristIrisPipelineBridge) || !(pipeline instanceof ShaderRenderingPipeline shaderPipeline) || !shaderPipeline.shouldOverrideShaders()) {
            return false;
        }

        String packName = Iris.getCurrentPackName();
        if (packName == null) {
            return false;
        }

        String normalizedPackName = packName.toLowerCase(Locale.ROOT);
        return normalizedPackName.contains("bsl");
    }

    public static boolean shouldUseTerrainRingRenderer() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (!(pipeline instanceof AntichristIrisPipelineBridge) || !(pipeline instanceof ShaderRenderingPipeline shaderPipeline) || !shaderPipeline.shouldOverrideShaders()) {
            return false;
        }

        String packName = Iris.getCurrentPackName();
        return packName != null && packName.toLowerCase(Locale.ROOT).contains("photon");
    }

    public static boolean shouldWriteStarShellDepthForShaderpack() {
        if (directMainTargetDepth > 0) {
            return false;
        }

        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        return pipeline instanceof AntichristIrisPipelineBridge && pipeline instanceof ShaderRenderingPipeline shaderPipeline && shaderPipeline.shouldOverrideShaders();
    }
}