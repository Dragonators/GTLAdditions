package com.gtladd.gtladditions.client.render.machine.matrix

import com.gtladd.gtladditions.client.render.machine.deferred.DeferredMachineRenderer
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredOculusCompat
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderContext
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderEntry
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredSceneRenderer
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.common.machine.multiblock.controller.ApocalypticWarpQuantumMatrix
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.core.Direction
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.AABB
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.cos
import kotlin.math.sin

/** Shared frame geometry and the eye-linked iris-star seal, ordered after eye/mist composition. */
@OnlyIn(Dist.CLIENT)
object MatrixEffectRenderer {
    private var frameLights: List<VertexBuffer>? = null

    fun enqueue(blockEntity: BlockEntity, machine: ApocalypticWarpQuantumMatrix, ticks: Double, eyeSeconds: Double) {
        MatrixFrameTerrainRenderer.enqueue(blockEntity, machine, ticks)
        val eye = MatrixEyeRenderer.sample(blockEntity, eyeSeconds, machine.matrixEffects.isRunning())
        DeferredMachineRenderer.enqueue(Entry(blockEntity, machine, ticks, eye))
    }

    fun releaseBuffers() {
        MatrixEnergyCoreRenderer.releaseBuffers()
        MatrixEyeRenderer.releaseBuffers()
        MatrixIrisStarRenderer.releaseBuffers()
        MatrixSealRenderer.releaseBuffers()
        MatrixFrameTerrainRenderer.releaseBuffers()
        RenderUtils.releaseBuffers(frameLights)
        frameLights = null
    }

    private fun frameLightBuffers(): List<VertexBuffer> =
        frameLights ?: (0 until MatrixEffectGeometry.ENERGY_CORE_LAYER_INDEX).map { index ->
        val builder = BufferBuilder(65536)
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)
        for (quad in MatrixFrameDetails.layer(index).lights) {
            for (i in 0..3) {
                builder.vertex(
                            quad.vertices[i * 3].toDouble(),
                            quad.vertices[i * 3 + 1].toDouble(),
                            quad.vertices[i * 3 + 2].toDouble()
                        )
                    .color(quad.color[0], quad.color[1], quad.color[2], quad.alpha).endVertex()
            }
        }
        RenderUtils.uploadStaticBuffer(builder)
    }.also { frameLights = it }

    private fun renderFrameLights(entry: Entry, poseStack: PoseStack, yaw: Float) {
        val shader = MatrixShaders.seal ?: return
        RenderSystem.enableBlend()
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE)
        RenderSystem.enableDepthTest()
        RenderSystem.depthMask(false)
        RenderSystem.disableCull()
        frameLightBuffers().forEachIndexed { index, buffer ->
            val intensity = if (entry.machine.matrixEffects.isRunning()) 0.9f + 0.1f * sin(entry.ticks * 0.045 + index).toFloat() else 0.35f
            shader.getUniform("Intensity")?.set(intensity)
            poseStack.withPose {
                MatrixFrameTransforms.apply(this, index, entry.ticks, yaw, entry.machine.isFlipped)
                RenderUtils.withBoundBuffer(buffer) {
                    DeferredOculusCompat.withDeferredShaderPass {
                        buffer.drawWithShader(last().pose(), RenderSystem.getProjectionMatrix(), shader)
                    }
                }
            }
        }
    }

    private class Entry(
        override val blockEntity: BlockEntity,
        val machine: ApocalypticWarpQuantumMatrix,
        val ticks: Double,
        val eye: MatrixEyeAnimation.Frame
    ) : DeferredRenderEntry {
        override fun isVisible(context: DeferredRenderContext): Boolean =
            !blockEntity.isRemoved && machine.isFormed && blockEntity.level === Minecraft.getInstance().level

        private val yaw: Float
            get() = when (machine.frontFacing) {
                Direction.WEST -> -90f
                Direction.NORTH -> 180f
                Direction.EAST -> 90f
                else -> 0f
            }

        private fun renderEye(context: DeferredRenderContext, opaque: Boolean) {
            val angle = Math.toRadians(yaw.toDouble())
            val pos = blockEntity.blockPos
            val x = pos.x + 0.5 - 15.0 * sin(angle)
            val z = pos.z + 0.5 - 15.0 * cos(angle)
            if (context.frustum?.isVisible(AABB(x - 11.5, pos.y + 0.9, z - 11.5, x + 11.5, pos.y + 7.0, z + 11.5)) == false) return
            context.renderRelativeToCamera(blockEntity) {
                MatrixEyeRenderer.applyTransform(this, yaw, machine.isFlipped)
                MatrixFrameTransforms.withWinding(machine.isFlipped) {
                    DeferredOculusCompat.withDeferredShaderPass {
                        if (opaque) {
                            MatrixEyeRenderer.renderSurfaces(this, eye)
                        } else {
                            MatrixEyeRenderer.renderMist(this, eye)
                        }
                    }
                }
            }
        }

        override fun renderOpaque(context: DeferredRenderContext) = renderEye(context, true)

        override fun render(context: DeferredRenderContext) {
            RenderUtils.withSavedRenderState {
                renderEye(context, false)
                context.renderRelativeToCamera(blockEntity) {
                    MatrixFrameTransforms.withWinding(machine.isFlipped) {
                        renderFrameLights(this@Entry, this, yaw)
                        withPose {
                            MatrixEyeRenderer.applyTransform(this, yaw, machine.isFlipped)
                            DeferredOculusCompat.withDeferredShaderPass {
                                MatrixIrisStarRenderer.render(this, eye)
                            }
                        }
                    }
                    MatrixIrisStarRenderer.renderBeams(blockEntity, this, eye, yaw, machine.isFlipped)
                }
            }
            val core = MatrixEnergyCoreRenderer.Entry(blockEntity, ticks, yaw, machine.isFlipped)
            if (!DeferredSceneRenderer.tryDeferEffect(core)) core.renderDirect(context)
        }
    }
}