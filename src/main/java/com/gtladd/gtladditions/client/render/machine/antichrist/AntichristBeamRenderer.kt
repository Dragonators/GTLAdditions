package com.gtladd.gtladditions.client.render.machine.antichrist

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredOculusCompat
import com.gtladd.gtladditions.client.render.withPose
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Vector3f
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

@OnlyIn(Dist.CLIENT)
object AntichristBeamRenderer {
    private const val MAX_SEGMENTS = 10
    private const val SEGMENT_QUADS = 16
    private const val ENDPOINT_FLOATS = (MAX_SEGMENTS + 1) * 3
    private const val BACK_PLATE_DISTANCE = -121.5f
    private const val BACK_PLATE_RADIUS = 13.0f
    private const val INTENSE_BEAM_TANGENT_FADE_DISTANCE = 3.75f

    private val SPACE_LAYER = GTLAdditions.id("textures/block/multiblock/forge_of_antichrist/space_layer.png")

    private val beamBuffer: VertexBuffer by lazy {
        buildBeamBuffer()
    }

    private val softBeam = SegmentBuffer()
    private val intenseBeam = SegmentBuffer()
    private val cameraPosition = Vector3f()

    data class VerticalTaperBeamProfile(
        val tick: Float,
        val basePos: Vec3,
        val height: Float,
        val bottomRadius: Float,
        val topRadius: Float,
        val colorR: Float,
        val colorG: Float,
        val colorB: Float,
        val beamAlpha: Float
    ) {
        val shouldRenderBeam: Boolean
            get() = beamAlpha > 0.001f && height > 0.001f && bottomRadius > 0.001f && topRadius >= 0.0f
    }

    fun render(profile: AntichristRenderProfile, poseStack: PoseStack, blockEntity: BlockEntity) {
        if (!profile.shouldRenderBeam) return
        val shader = AntichristShaders.beamShader ?: return

        updateCameraPositionInBeamSpace(profile, blockEntity)
        writeSoftBeamSegments(profile.starRadius, profile.beamAlpha, softBeam)
        writeIntenseBeamSegments(profile.starRadius, profile.beamAlpha, intenseBeam)

        poseStack.withPose {
            translate(profile.starPos.x, profile.starPos.y, profile.starPos.z)
            mulPose(Axis.YP.rotationDegrees(profile.beamYawDegrees))
            renderCurrentSegments(
                shader,
                profile.tick,
                profile.colorR,
                profile.colorG,
                profile.colorB,
                1.0f,
                1.0f,
                1.0f
            )
        }
    }

    fun renderVerticalTaper(profile: VerticalTaperBeamProfile, poseStack: PoseStack, blockEntity: BlockEntity) {
        if (!profile.shouldRenderBeam) return
        val shader = AntichristShaders.beamShader ?: return

        updateCameraPositionInVerticalBeamSpace(profile, blockEntity)
        writeVerticalSoftBeamSegments(profile, softBeam)
        writeVerticalIntenseBeamSegments(profile, intenseBeam)

        poseStack.withPose {
            translate(profile.basePos.x, profile.basePos.y, profile.basePos.z)
            mulPose(Axis.XP.rotationDegrees(-90.0f))
            renderCurrentSegments(
                shader,
                profile.tick,
                profile.colorR,
                profile.colorG,
                profile.colorB,
                1.0f,
                1.0f,
                1.0f
            )
        }
    }

    private fun PoseStack.renderCurrentSegments(
        shader: ShaderInstance,
        tick: Float,
        colorR: Float,
        colorG: Float,
        colorB: Float,
        intenseColorR: Float,
        intenseColorG: Float,
        intenseColorB: Float
    ) {
        RenderSystem.enableBlend()
        RenderSystem.blendFunc(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        )
        RenderSystem.enableDepthTest()
        RenderSystem.depthMask(false)
        RenderSystem.disableCull()
        RenderSystem.setShaderTexture(0, SPACE_LAYER)

        shader.getUniform("SegmentQuads")?.set(SEGMENT_QUADS.toFloat())
        shader.getUniform("CameraPosition")?.set(cameraPosition.x, cameraPosition.y, cameraPosition.z)
        shader.getUniform("Time")?.set(tick)

        beamBuffer.bind()

        shader.getUniform("Color")?.set(colorR, colorG, colorB)
        shader.getUniform("Intensity")?.set(2.0f)
        shader.getUniform("SegmentArray")?.set(softBeam.values)
        DeferredOculusCompat.withDeferredShaderPass {
            beamBuffer.drawWithShader(last().pose(), RenderSystem.getProjectionMatrix(), shader)
        }

        shader.getUniform("Color")?.set(intenseColorR, intenseColorG, intenseColorB)
        shader.getUniform("Intensity")?.set(4.0f)
        shader.getUniform("SegmentArray")?.set(intenseBeam.values)
        DeferredOculusCompat.withDeferredShaderPass {
            beamBuffer.drawWithShader(last().pose(), RenderSystem.getProjectionMatrix(), shader)
        }

        VertexBuffer.unbind()
        RenderSystem.enableCull()
        RenderSystem.depthMask(true)
        RenderSystem.disableBlend()
    }

    @Suppress("SameParameterValue")
    private fun writeSoftBeamSegments(starRadius: Float, beamAlpha: Float, segments: SegmentBuffer) {
        segments.clear()

        val angle = getStartAngle(starRadius)
        val radius = starRadius * 1.1f
        val startX = -radius * cos(angle)
        val startY = radius * sin(angle)

        segments.add(startY, startX, 0.0f)

        for (i in 2 downTo 0) {
            segments.add(getLensRadius(i), getLensDistance(i), beamAlpha)
        }

        segments.add(BACK_PLATE_RADIUS, BACK_PLATE_DISTANCE, -0.05f * beamAlpha)
        segments.repeatLastEndpoint()
    }

    @Suppress("SameParameterValue")
    private fun writeIntenseBeamSegments(starRadius: Float, beamAlpha: Float, segments: SegmentBuffer) {
        segments.clear()

        val angle = getStartAngle(starRadius)
        val radius = starRadius * 1.05f
        val startX = -radius * cos(angle)
        val startY = radius * sin(angle)
        val firstLens = 2

        val nextX = getLensDistance(firstLens)
        val nextY = getLensRadius(firstLens) * 0.75f
        val backX = max(-radius, (nextX + radius) / 2.0f)
        val backY = interpolate(startX, nextX, startY, nextY, backX)

        var transparency = 0.2f
        addIntenseBeamStart(segments, backY, backX, nextY, nextX, transparency * beamAlpha)
        for (i in 2 downTo 0) {
            segments.add(getLensRadius(i) / 2.0f, getLensDistance(i), transparency * beamAlpha)
            transparency += 0.3f
        }

        val currentX = getLensDistance(0)
        val currentY = getLensRadius(0) / 2.0f
        val lastX = BACK_PLATE_DISTANCE
        val lastY = minOf(getLensRadius(firstLens), BACK_PLATE_RADIUS)
        val midX = lastX + 8.0f
        val midY = interpolate(currentX, lastX, currentY, lastY, midX)

        segments.add(midY, midX, transparency * beamAlpha)
        segments.add(lastY, lastX, 0.0f)
        segments.repeatLastEndpoint()
    }

    private fun writeVerticalSoftBeamSegments(
        profile: VerticalTaperBeamProfile,
        segments: SegmentBuffer
    ) {
        segments.clear()

        val fadeEndOffset = profile.height * 0.1f
        val fadeEndRadius = interpolate(0.0f, profile.height, profile.bottomRadius, profile.topRadius, fadeEndOffset)

        segments.add(profile.bottomRadius, 0.0f, 0.0f)
        segments.add(fadeEndRadius, fadeEndOffset, profile.beamAlpha)
        segments.add(profile.topRadius, profile.height, profile.beamAlpha * 0.85f)
        segments.repeatLastEndpoint()
    }

    private fun writeVerticalIntenseBeamSegments(
        profile: VerticalTaperBeamProfile,
        segments: SegmentBuffer
    ) {
        segments.clear()

        val fadeEndOffset = profile.height * 0.1f
        val fadeEndRadius = interpolate(
            0.0f,
            profile.height,
            profile.bottomRadius * 0.45f,
            profile.topRadius * 0.5f,
            fadeEndOffset
        )

        segments.add(profile.bottomRadius * 0.45f, 0.0f, 0.0f)
        segments.add(fadeEndRadius, fadeEndOffset, profile.beamAlpha * 0.55f)
        segments.add(profile.topRadius * 0.5f, profile.height, profile.beamAlpha * 0.85f)
        segments.repeatLastEndpoint()
    }

    private fun updateCameraPositionInBeamSpace(
        profile: AntichristRenderProfile,
        blockEntity: BlockEntity
    ) {
        val cameraWorldPos = Minecraft.getInstance().gameRenderer.mainCamera.position
        val blockPos = blockEntity.blockPos
        val cameraRelativeX = cameraWorldPos.x - blockPos.x.toDouble() - profile.starPos.x
        val cameraRelativeY = cameraWorldPos.y - blockPos.y.toDouble() - profile.starPos.y
        val cameraRelativeZ = cameraWorldPos.z - blockPos.z.toDouble() - profile.starPos.z
        val yawRadians = Math.toRadians((-profile.beamYawDegrees).toDouble())
        val cosYaw = cos(yawRadians).toFloat()
        val sinYaw = sin(yawRadians).toFloat()
        val x = cameraRelativeX.toFloat()
        val z = cameraRelativeZ.toFloat()

        cameraPosition.set(
            cosYaw * x + sinYaw * z,
            cameraRelativeY.toFloat(),
            -sinYaw * x + cosYaw * z
        )
    }

    private fun updateCameraPositionInVerticalBeamSpace(
        profile: VerticalTaperBeamProfile,
        blockEntity: BlockEntity
    ) {
        val cameraWorldPos = Minecraft.getInstance().gameRenderer.mainCamera.position
        val blockPos = blockEntity.blockPos
        val relativeX = cameraWorldPos.x - blockPos.x.toDouble() - profile.basePos.x
        val relativeY = cameraWorldPos.y - blockPos.y.toDouble() - profile.basePos.y
        val relativeZ = cameraWorldPos.z - blockPos.z.toDouble() - profile.basePos.z

        cameraPosition.set(
            relativeX.toFloat(),
            -relativeZ.toFloat(),
            relativeY.toFloat()
        )
    }

    private fun getStartAngle(starRadius: Float): Float {
        val x = -getLensDistance(2)
        val y = getLensRadius(2)
        val alpha = atan2(y, x)
        val beta = asin(starRadius / sqrt(x * x + y * y))
        return alpha + (Math.PI.toFloat() / 2.0f - beta)
    }

    private fun getLensDistance(lensId: Int): Float = when (lensId) {
        0 -> -61.5f
        1 -> -54.5f
        2 -> -44.5f
        else -> error("Unexpected lens id $lensId")
    }

    private fun getLensRadius(lensId: Int): Float = when (lensId) {
        0 -> 1.1f
        1 -> 3.5f
        2 -> 5.0f
        else -> error("Unexpected lens id $lensId")
    }

    private fun addIntenseBeamStart(
        segments: SegmentBuffer,
        outerRadius: Float,
        outerOffset: Float,
        lensRadius: Float,
        lensOffset: Float,
        lensTransparency: Float
    ) {
        val tangentProgress = closestPointProgressToCenter(lensRadius, lensOffset, outerRadius, outerOffset)
        val tangentRadius = interpolate(0.0f, 1.0f, lensRadius, outerRadius, tangentProgress)
        val tangentOffset = interpolate(0.0f, 1.0f, lensOffset, outerOffset, tangentProgress)
        val tangentTransparency = lensTransparency * (1.0f - tangentProgress)
        val fadeProgress = moveToward(
            tangentRadius,
            tangentOffset,
            outerRadius,
            outerOffset,
            INTENSE_BEAM_TANGENT_FADE_DISTANCE
        )
        val fadeRadius = interpolate(0.0f, 1.0f, tangentRadius, outerRadius, fadeProgress)
        val fadeOffset = interpolate(0.0f, 1.0f, tangentOffset, outerOffset, fadeProgress)

        segments.add(fadeRadius, fadeOffset, 0.0f)
        segments.add(tangentRadius, tangentOffset, tangentTransparency)
    }

    private fun closestPointProgressToCenter(
        startRadius: Float,
        startOffset: Float,
        endRadius: Float,
        endOffset: Float
    ): Float {
        val radiusDelta = endRadius - startRadius
        val offsetDelta = endOffset - startOffset
        val lengthSqr = radiusDelta * radiusDelta + offsetDelta * offsetDelta
        if (lengthSqr <= 0.0001f) return 1.0f

        return (-(startRadius * radiusDelta + startOffset * offsetDelta) / lengthSqr).coerceIn(0.0f, 1.0f)
    }

    @Suppress("SameParameterValue")
    private fun moveToward(
        startRadius: Float,
        startOffset: Float,
        outerRadius: Float,
        outerOffset: Float,
        distance: Float
    ): Float {
        val radiusDelta = outerRadius - startRadius
        val offsetDelta = outerOffset - startOffset
        val length = sqrt(radiusDelta * radiusDelta + offsetDelta * offsetDelta)
        if (length <= 0.0001f) return 0.0f

        return minOf(distance, length) / length
    }

    private fun interpolate(x0: Float, x1: Float, y0: Float, y1: Float, x: Float): Float =
        y0 + ((x - x0) * (y1 - y0)) / (x1 - x0)

    private fun buildBeamBuffer(): VertexBuffer {
        val buffer = VertexBuffer(VertexBuffer.Usage.STATIC)
        val builder = Tesselator.getInstance().builder
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION)

        repeat(MAX_SEGMENTS * SEGMENT_QUADS * 6) {
            addVertex(builder)
        }

        buffer.bind()
        buffer.upload(builder.end())
        VertexBuffer.unbind()
        return buffer
    }

    private fun addVertex(builder: BufferBuilder) {
        builder.vertex(0.0, 0.0, 0.0).endVertex()
    }

    private class SegmentBuffer {
        val values = FloatArray(ENDPOINT_FLOATS)
        private var endpointCount = 0

        fun clear() {
            endpointCount = 0
        }

        fun add(radius: Float, offset: Float, transparency: Float) {
            val base = endpointCount * 3
            values[base] = radius
            values[base + 1] = offset
            values[base + 2] = transparency
            endpointCount++
        }

        fun repeatLastEndpoint() {
            val fallbackBase = (endpointCount - 1) * 3
            for (i in endpointCount..MAX_SEGMENTS) {
                val base = i * 3
                values[base] = values[fallbackBase]
                values[base + 1] = values[fallbackBase + 1]
                values[base + 2] = values[fallbackBase + 2]
            }
        }
    }
}