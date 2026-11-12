package com.gtladd.gtladditions.client.render.machine.condensation

import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.client.scene.PartBounds
import com.gtladd.gtladditions.api.client.scene.PartDrawContext
import com.gtladd.gtladditions.api.client.scene.PartKey
import com.gtladd.gtladditions.api.client.scene.PartOutcome
import com.gtladd.gtladditions.api.client.scene.PartPass
import com.gtladd.gtladditions.api.client.scene.PartRenderer
import com.gtladd.gtladditions.api.client.scene.ScenePart
import com.gtladd.gtladditions.api.client.scene.SceneRendering
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.common.machine.multiblock.controller.AntientropyCondensationCenter
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Quaternionf

@OnlyIn(Dist.CLIENT)
object CondensationCoreRenderer : PartRenderer<CondensationCoreRenderer.Core> {
    private val provider = SceneRendering.register(GTLAdditions.id("condensation_core"))
    private var core: VertexBuffer? = null
    private var shell: VertexBuffer? = null

    fun releaseBuffers() {
        core?.close()
        shell?.close()
        core = null
        shell = null
    }

    fun submit(blockEntity: BlockEntity, machine: AntientropyCondensationCenter, rotationTicks: Double, flowPhase: Float) {
        val frame = SceneRendering.frame() ?: return
        val center = CondensationCoreGeometry.center(machine.pos, machine.frontFacing)
        val up = RelativeDirection.UP.getRelativeFacing(machine.frontFacing, machine.upwardsFacing, machine.isFlipped)
        frame.submit(
            ScenePart.builder(PartKey.block(provider, blockEntity.blockPos, 0), this, Core(center, up, rotationTicks, flowPhase))
                .pass(PartPass.OPAQUE)
                .bounds(PartBounds.box(CondensationCoreGeometry.bounds(center)))
                .build()
        )
    }

    override fun draw(context: PartDrawContext, parts: List<ScenePart<Core>>) {
        val shader = CondensationShaders.core ?: return parts.forEach { context.report(it, PartOutcome.RESOURCE_UNAVAILABLE) }
        val frame = context.frame

        for (part in parts) {
            val core = part.payload
            context.poseStack.withPose {
                translate(core.center.x - frame.cameraX, core.center.y - frame.cameraY, core.center.z - frame.cameraZ)
                mulPose(
                    Quaternionf().rotationTo(
                        0.0f,
                        1.0f,
                        0.0f,
                        core.up.stepX.toFloat(),
                        core.up.stepY.toFloat(),
                        core.up.stepZ.toFloat()
                    )
                )
                render(this, shader, core.rotationTicks, core.flowPhase)
            }
        }
    }

    private fun model(location: ResourceLocation): VertexBuffer = RenderUtils.withStagingBuilder { builder ->
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION)
        CondensationCoreMesh.writeModel(builder, Minecraft.getInstance().modelManager.getModel(location))
        RenderUtils.uploadStaticBuffer(builder)
    }

    private fun draw(buffer: VertexBuffer, pose: PoseStack, shader: ShaderInstance) {
        RenderUtils.withBoundBuffer(buffer) {
            buffer.drawWithShader(pose.last().pose(), RenderSystem.getProjectionMatrix(), shader)
        }
    }

    /** From the OPAQUE step baseline: depth test and writes, no blending. */
    private fun render(pose: PoseStack, shader: ShaderInstance, rotationTicks: Double, flowPhase: Float) {
        val coreBuffer = core ?: model(CondensationCoreMesh.CORE_MODEL).also { core = it }
        val shellBuffer = shell ?: model(CondensationCoreMesh.SHELL_MODEL).also { shell = it }
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

        // The inverted sphere is an opaque inner wall: negative scale and back-face culling expose the far wall around
        // the smaller core.
        RenderSystem.enableCull()
        shader.getUniform("ColorCore")?.set(0.015f, 0.04f, 0.06f)
        shader.getUniform("ColorSpike")?.set(0.015f, 0.04f, 0.06f)
        pose.withPose {
            val radius = -CondensationCoreGeometry.SHELL_RADIUS
            scale(radius, radius, radius)
            draw(shellBuffer, this, shader)
        }
    }

    class Core(val center: Vec3, val up: Direction, val rotationTicks: Double, val flowPhase: Float)
}