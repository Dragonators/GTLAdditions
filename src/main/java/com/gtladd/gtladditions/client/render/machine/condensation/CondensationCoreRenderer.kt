package com.gtladd.gtladditions.client.render.machine.condensation

import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredMachineRenderer
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredOculusCompat
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderContext
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderEntry
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.common.machine.multiblock.controller.AntientropyCondensationCenter
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Quaternionf
import org.lwjgl.opengl.GL11C

@OnlyIn(Dist.CLIENT)
object CondensationCoreRenderer {
    private var core: VertexBuffer? = null
    private var shell: VertexBuffer? = null

    fun releaseBuffers() {
        core?.close()
        shell?.close()
        core = null
        shell = null
    }

    fun enqueue(blockEntity: BlockEntity, machine: AntientropyCondensationCenter, rotationTicks: Double, flowPhase: Float) {
        DeferredMachineRenderer.enqueue(Entry(blockEntity, machine, rotationTicks, flowPhase))
    }

    private fun model(location: ResourceLocation): VertexBuffer {
        val builder = BufferBuilder(32768)
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION)
        CondensationCoreMesh.writeModel(builder, Minecraft.getInstance().modelManager.getModel(location))
        return RenderUtils.uploadStaticBuffer(builder)
    }

    private fun draw(buffer: VertexBuffer, pose: PoseStack, shader: ShaderInstance) {
        RenderUtils.withBoundBuffer(buffer) {
            buffer.drawWithShader(pose.last().pose(), RenderSystem.getProjectionMatrix(), shader)
        }
    }

    private fun render(pose: PoseStack, rotationTicks: Double, flowPhase: Float) {
        val shader = CondensationShaders.core ?: return
        val coreBuffer = core ?: model(CondensationCoreMesh.CORE_MODEL).also { core = it }
        val shellBuffer = shell ?: model(CondensationCoreMesh.SHELL_MODEL).also { shell = it }
        RenderUtils.withSavedRenderState {
            RenderSystem.disableBlend()
            RenderSystem.enableDepthTest()
            RenderSystem.depthFunc(GL11C.GL_LEQUAL)
            RenderSystem.depthMask(true)
            DeferredOculusCompat.withDeferredShaderPass {
                shader.getUniform("Phase")?.set(flowPhase)
                shader.getUniform("ColorCore")?.set(0.86f, 0.98f, 1.0f)
                shader.getUniform("ColorSpike")?.set(0.16f, 0.65f, 0.86f)
                RenderSystem.disableCull()
                pose.withPose {
                    // The combined Y and X rotation repeats after 2880 ticks.
                    mulPose(Axis.YP.rotationDegrees((rotationTicks % 2880.0).toFloat()))
                    mulPose(Axis.XP.rotationDegrees(((rotationTicks % 2880.0) / 8.0).toFloat()))
                    scale(CondensationCoreGeometry.CORE_SCALE, CondensationCoreGeometry.CORE_SCALE, CondensationCoreGeometry.CORE_SCALE)
                    draw(coreBuffer, this, shader)
                }

                // GTNH's inverted smooth sphere is an opaque inner wall, not a translucent overlay.
                // Negative scale and back-face culling expose the far wall around the smaller core.
                RenderSystem.enableCull()
                shader.getUniform("ColorCore")?.set(0.015f, 0.04f, 0.06f)
                shader.getUniform("ColorSpike")?.set(0.015f, 0.04f, 0.06f)
                pose.withPose {
                    val radius = -CondensationCoreGeometry.SHELL_RADIUS
                    scale(radius, radius, radius)
                    draw(shellBuffer, this, shader)
                }
            }
        }
    }

    private class Entry(
        override val blockEntity: BlockEntity,
        private val machine: AntientropyCondensationCenter,
        private val rotationTicks: Double,
        private val flowPhase: Float
    ) : DeferredRenderEntry {
        private val center = CondensationCoreGeometry.center(machine.pos, machine.frontFacing)
        private val up = RelativeDirection.UP.getRelativeFacing(machine.frontFacing, machine.upwardsFacing, machine.isFlipped)
        private val bounds = CondensationCoreGeometry.bounds(center)

        override fun isVisible(context: DeferredRenderContext): Boolean =
            !blockEntity.isRemoved && machine.isFormed && (context.frustum?.isVisible(bounds) != false)

        override fun render(context: DeferredRenderContext) {
            context.poseStack.withPose {
                val offset = center.subtract(context.cameraPosition)
                translate(offset.x, offset.y, offset.z)
                mulPose(Quaternionf().rotationTo(0.0f, 1.0f, 0.0f, up.stepX.toFloat(), up.stepY.toFloat(), up.stepZ.toFloat()))
                render(this, rotationTicks, flowPhase)
            }
        }
    }
}