package com.gtladd.gtladditions.client.render.machine.antichrist

import com.gtladd.gtladditions.mixin.oculus.DepthColorStorageAccessor
import net.irisshaders.iris.Iris
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline
import net.minecraft.client.Minecraft
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object AntichristOculusCompat {
    private const val STAR_SHADER = "gtladditions:gtladditions_antichrist_star"
    private const val BEAM_SHADER = "gtladditions:gtladditions_antichrist_beam"
    private var directMainTargetDepth = 0

    @JvmStatic
    fun isAntichristShader(shaderName: String): Boolean = shaderName == STAR_SHADER || shaderName == BEAM_SHADER

    @JvmStatic
    fun beginAntichristShaderPass() {
        if (directMainTargetDepth > 0) {
            Minecraft.getInstance().mainRenderTarget.bindWrite(false)
            unlockDepthColorForAntichristShader()
            return
        }

        val pipeline = Iris.getPipelineManager().pipelineNullable
        if (pipeline is AntichristIrisPipelineBridge) {
            pipeline.beginAntichristFallbackTarget()
        } else {
            Minecraft.getInstance().mainRenderTarget.bindWrite(false)
        }
        unlockDepthColorForAntichristShader()
    }

    @JvmStatic
    fun endAntichristShaderPass() {
        if (directMainTargetDepth > 0) {
            Minecraft.getInstance().mainRenderTarget.bindWrite(false)
            return
        }

        val pipeline = Iris.getPipelineManager().pipelineNullable
        if (pipeline is AntichristIrisPipelineBridge) {
            pipeline.endAntichristFallbackTarget()
        } else {
            Minecraft.getInstance().mainRenderTarget.bindWrite(false)
        }
    }

    fun beginDirectMainTargetPass() {
        directMainTargetDepth++
        Minecraft.getInstance().mainRenderTarget.bindWrite(false)
        unlockDepthColorForAntichristShader()
    }

    fun endDirectMainTargetPass() {
        if (directMainTargetDepth > 0) {
            directMainTargetDepth--
        }
        Minecraft.getInstance().mainRenderTarget.bindWrite(false)
    }

    fun shouldRenderAfterShaderpackFinal(): Boolean = shouldUseShaderpackCompatPath()

    private fun unlockDepthColorForAntichristShader() {
        if (DepthColorStorageAccessor.`gtladditions$isDepthColorLocked`()) {
            DepthColorStorageAccessor.`gtladditions$unlockDepthColor`()
        }
    }

    private fun shouldUseShaderpackCompatPath(): Boolean {
        val pipeline = Iris.getPipelineManager().pipelineNullable
        return pipeline is AntichristIrisPipelineBridge &&
            pipeline is ShaderRenderingPipeline &&
            pipeline.shouldOverrideShaders()
    }
}