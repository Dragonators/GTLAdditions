package com.gtladd.gtladditions.client.render.machine.qft

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.client.scene.PartBlend
import com.gtladd.gtladditions.api.client.scene.PartBounds
import com.gtladd.gtladditions.api.client.scene.PartDrawContext
import com.gtladd.gtladditions.api.client.scene.PartFeature
import com.gtladd.gtladditions.api.client.scene.PartKey
import com.gtladd.gtladditions.api.client.scene.PartOutcome
import com.gtladd.gtladditions.api.client.scene.PartRenderer
import com.gtladd.gtladditions.api.client.scene.SceneFrame
import com.gtladd.gtladditions.api.client.scene.ScenePart
import com.gtladd.gtladditions.api.client.scene.SceneRendering
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexSorting
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.config.ConfigHolder

@OnlyIn(Dist.CLIENT)
internal object QFTForceFieldRenderer : PartRenderer<QFTForceField> {
    private val texture = GTLAdditions.id("textures/effect/qft/force_field.png")
    private val provider = SceneRendering.register(GTLAdditions.id("qft"))
    private val builder = BufferBuilder(4096)
    private val vertexBuffer by lazy { VertexBuffer(VertexBuffer.Usage.DYNAMIC) }
    var shader: ShaderInstance? = null

    fun submit(frame: SceneFrame, pos: BlockPos, facing: Direction, machineTicks: Double) {
        val center = QFTForceFieldGeometry.center(pos, facing)
        // A shared AnimationFrame is valid only when the machines share the frame's animation clock.
        val smooth = ConfigHolder.INSTANCE.graphics.enableSmoothAnimations
        val ticks = if (smooth) frame.renderTicks else machineTicks
        val part = ScenePart.builder(PartKey.block(provider, pos, 0), this, QFTForceField(center, ticks))
            .blend(PartBlend.PREMULTIPLIED)
            .bounds(PartBounds.box(QFTForceFieldGeometry.bounds(center)))
            .features(PartFeature.INTERNAL_SORT)
        if (smooth) part.batchKey(this)
        frame.submit(part.build())
    }

    override fun draw(context: PartDrawContext, parts: List<ScenePart<QFTForceField>>) {
        val shader = shader ?: return parts.forEach { context.report(it, PartOutcome.RESOURCE_UNAVAILABLE) }
        val frame = context.frame
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
        for (part in parts) QFTForceFieldGeometry.write(builder, part.payload.center.subtract(frame.cameraX, frame.cameraY, frame.cameraZ))
        builder.setQuadSorting(VertexSorting.DISTANCE_TO_ORIGIN)
        RenderUtils.withSavedRenderState {
            RenderUtils.withBoundBuffer(vertexBuffer) {
                vertexBuffer.upload(builder.end())
                RenderSystem.disableCull()
                RenderSystem.setShaderTexture(0, texture)
                shader.getUniform("AnimationFrame")?.set(QFTForceFieldGeometry.animationFrame((parts[0].payload.ticks * 50.0).toLong()))
                vertexBuffer.drawWithShader(context.poseStack.last().pose(), RenderSystem.getProjectionMatrix(), shader)
            }
        }
    }
}

internal class QFTForceField(val center: Vec3, val ticks: Double)