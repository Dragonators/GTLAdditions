package com.gtladd.gtladditions.client.render.machine.antichrist

import com.gtladd.gtladditions.GTLAdditions
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

    fun render(profile: AntichristRenderProfile, poseStack: PoseStack, blockEntity: BlockEntity) {
        if (!profile.shouldRenderBeam) return
        val shader = AntichristShaders.beamShader ?: return

        val camera = getCameraPositionInBeamSpace(profile, blockEntity)
        val softSegments = bufferSoftBeam(profile.starRadius)
        val intenseSegments = bufferIntenseBeam(profile.starRadius)

        poseStack.withPose {
            translate(profile.starPos.x, profile.starPos.y, profile.starPos.z)
            mulPose(Axis.YP.rotationDegrees(profile.beamYawDegrees))

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
            shader.getUniform("CameraPosition")?.set(camera.x, camera.y, camera.z)
            shader.getUniform("Time")?.set(profile.tick)

            beamBuffer.bind()

            shader.getUniform("Color")?.set(profile.colorR, profile.colorG, profile.colorB)
            shader.getUniform("Intensity")?.set(2.0f)
            shader.getUniform("SegmentArray")?.set(softSegments)
            beamBuffer.drawWithShader(last().pose(), RenderSystem.getProjectionMatrix(), shader)

            shader.getUniform("Color")?.set(1.0f, 1.0f, 1.0f)
            shader.getUniform("Intensity")?.set(4.0f)
            shader.getUniform("SegmentArray")?.set(intenseSegments)
            beamBuffer.drawWithShader(last().pose(), RenderSystem.getProjectionMatrix(), shader)

            VertexBuffer.unbind()
            RenderSystem.enableCull()
            RenderSystem.depthMask(true)
            RenderSystem.disableBlend()
        }
    }

    private fun bufferSoftBeam(starRadius: Float): FloatArray {
        val endpoints = ArrayList<Vector3f>(MAX_SEGMENTS + 1)
        val angle = getStartAngle(starRadius)
        val radius = starRadius * 1.1f
        val startX = -radius * cos(angle)
        val startY = radius * sin(angle)

        endpoints.add(Vector3f(startY, startX, 0.0f))

        for (i in 2 downTo 0) {
            endpoints.add(Vector3f(getLensRadius(i), getLensDistance(i), 1.0f))
        }

        endpoints.add(Vector3f(BACK_PLATE_RADIUS, BACK_PLATE_DISTANCE, -0.05f))
        return fillEndpointArray(endpoints)
    }

    private fun bufferIntenseBeam(starRadius: Float): FloatArray {
        val endpoints = ArrayList<Vector3f>(MAX_SEGMENTS + 1)
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
        addIntenseBeamStart(endpoints, backY, backX, nextY, nextX, transparency)
        for (i in 2 downTo 0) {
            endpoints.add(Vector3f(getLensRadius(i) / 2.0f, getLensDistance(i), transparency))
            transparency += 0.3f
        }

        val currentX = getLensDistance(0)
        val currentY = getLensRadius(0) / 2.0f
        val lastX = BACK_PLATE_DISTANCE
        val lastY = minOf(getLensRadius(firstLens), BACK_PLATE_RADIUS)
        val midX = lastX + 8.0f
        val midY = interpolate(currentX, lastX, currentY, lastY, midX)

        endpoints.add(Vector3f(midY, midX, transparency))
        endpoints.add(Vector3f(lastY, lastX, 0.0f))
        return fillEndpointArray(endpoints)
    }

    private fun fillEndpointArray(endpoints: List<Vector3f>): FloatArray {
        val values = FloatArray(ENDPOINT_FLOATS)
        val fallback = endpoints.last()

        for (i in 0..MAX_SEGMENTS) {
            val endpoint = endpoints.getOrElse(i) { fallback }
            val base = i * 3
            values[base] = endpoint.x
            values[base + 1] = endpoint.y
            values[base + 2] = endpoint.z
        }

        return values
    }

    private fun getCameraPositionInBeamSpace(profile: AntichristRenderProfile, blockEntity: BlockEntity): Vector3f {
        val cameraWorldPos = Minecraft.getInstance().gameRenderer.mainCamera.position
        val blockWorldPos = Vec3.atLowerCornerOf(blockEntity.blockPos)
        val cameraRelative = cameraWorldPos.subtract(blockWorldPos).subtract(profile.starPos)
        val yawRadians = Math.toRadians((-profile.beamYawDegrees).toDouble())
        val cosYaw = cos(yawRadians).toFloat()
        val sinYaw = sin(yawRadians).toFloat()
        val x = cameraRelative.x.toFloat()
        val z = cameraRelative.z.toFloat()

        return Vector3f(
            cosYaw * x + sinYaw * z,
            cameraRelative.y.toFloat(),
            -sinYaw * x + cosYaw * z
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
        endpoints: MutableList<Vector3f>,
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

        endpoints.add(Vector3f(fadeRadius, fadeOffset, 0.0f))
        endpoints.add(Vector3f(tangentRadius, tangentOffset, tangentTransparency))
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
}