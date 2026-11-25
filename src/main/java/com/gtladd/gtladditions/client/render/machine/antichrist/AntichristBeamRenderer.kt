package com.gtladd.gtladditions.client.render.machine.antichrist

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.IndexedEffectVertexBuffer
import com.gtladd.gtladditions.client.render.withPose
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.math.Axis
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@OnlyIn(Dist.CLIENT)
object AntichristBeamRenderer {
    private const val MAX_SEGMENTS = 10
    private const val SEGMENT_QUADS = 16
    private const val ENDPOINT_FLOATS = (MAX_SEGMENTS + 1) * 3
    private const val PI = 3.1415927f
    private const val ALPHA_PACK_SCALE = 10000.0f
    private const val BACK_PLATE_DISTANCE = -121.5f
    private const val BACK_PLATE_RADIUS = 13.0f
    private const val INTENSE_BEAM_TANGENT_FADE_DISTANCE = 3.75f

    private val SPACE_LAYER = GTLAdditions.id("textures/effect/astrograph/space_layer.png")

    private val beamBuffer: IndexedEffectVertexBuffer by lazy { IndexedEffectVertexBuffer() }

    private val beamIndices = IntArray(MAX_SEGMENTS * SEGMENT_QUADS * 6).also { indices ->
        var offset = 0

        for (segment in 0 until MAX_SEGMENTS) {
            for (quad in 0 until SEGMENT_QUADS) {
                val a = segment * (SEGMENT_QUADS + 1) + quad
                val b = a + SEGMENT_QUADS + 1
                indices[offset++] = b + 1
                indices[offset++] = a + 1
                indices[offset++] = a
                indices[offset++] = a
                indices[offset++] = b
                indices[offset++] = b + 1
            }
        }
    }

    private val beamLayerBuffer: IndexedEffectVertexBuffer by lazy { IndexedEffectVertexBuffer() }
    private val beamViews = FloatArray(6 * 16)
    private val beamCameraX = FloatArray(6)
    private val beamCameraY = FloatArray(6)

    private val beamLayerIndices = IntArray(6 * SEGMENT_QUADS * 6) { index ->
        val layer = index / (SEGMENT_QUADS * 6)
        beamIndices[index % (SEGMENT_QUADS * 6)] + layer * 2 * (SEGMENT_QUADS + 1)
    }

    private val softBeam = SegmentBuffer()
    private val intenseBeam = SegmentBuffer()
    private val boundsBeam = SegmentBuffer()
    private val cameraPosition = Vector3f()

    data class VerticalTaperBeamProfile(
        val tick: Double,
        val basePos: Vec3,
        val height: Float,
        val bottomRadius: Float,
        val topRadius: Float,
        val colorR: Float,
        val colorG: Float,
        val colorB: Float,
        val beamAlpha: Float
    )

    fun drawVerticalTaper(profile: VerticalTaperBeamProfile, poseStack: PoseStack, blockPos: BlockPos, camera: Vec3): Boolean {
        val shader = AntichristShaders.beamShader ?: return false
        updateCameraPositionInVerticalBeamSpace(profile, blockPos, camera)
        writeVerticalSoftBeamSegments(profile, softBeam)
        writeVerticalIntenseBeamSegments(profile, intenseBeam)

        poseStack.withPose {
            translate(profile.basePos.x, profile.basePos.y, profile.basePos.z)
            mulPose(Axis.XP.rotationDegrees(-90.0f))
            RenderUtils.withSavedRenderState {
                renderCurrentSegments(shader, profile.tick, profile.colorR, profile.colorG, profile.colorB, 1.0f, 1.0f, 1.0f)
            }
        }

        return true
    }

    fun verticalTaperBounds(profile: VerticalTaperBeamProfile, blockPos: BlockPos): AABB {
        var radius = 0f
        var near = Float.MAX_VALUE
        var far = -Float.MAX_VALUE

        for (intense in booleanArrayOf(false, true)) {
            if (intense) writeVerticalIntenseBeamSegments(profile, boundsBeam) else writeVerticalSoftBeamSegments(profile, boundsBeam)

            for (endpoint in 0..boundsBeam.segmentCount) {
                radius = max(radius, boundsBeam.radius(endpoint))
                near = minOf(near, boundsBeam.offset(endpoint))
                far = max(far, boundsBeam.offset(endpoint))
            }
        }

        val x = blockPos.x + profile.basePos.x
        val y = blockPos.y + profile.basePos.y
        val z = blockPos.z + profile.basePos.z
        return AABB(x - radius, y + near, z - radius, x + radius, y + far, z + radius)
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

    private fun updateCameraPositionInVerticalBeamSpace(profile: VerticalTaperBeamProfile, blockPos: BlockPos, camera: Vec3) {
        val relativeX = camera.x - blockPos.x - profile.basePos.x
        val relativeY = camera.y - blockPos.y - profile.basePos.y
        val relativeZ = camera.z - blockPos.z - profile.basePos.z

        cameraPosition.set(
            relativeX.toFloat(),
            -relativeZ.toFloat(),
            relativeY.toFloat()
        )
    }

    fun draw(profile: AntichristRenderProfile, poseStack: PoseStack, blockPos: BlockPos, camera: Vec3): Boolean {
        val shader = AntichristShaders.beamShader ?: return false
        updateCameraPositionInBeamSpace(profile, blockPos, camera)
        writeSoftBeamSegments(profile.starRadius, profile.beamAlpha, softBeam)
        writeIntenseBeamSegments(profile.starRadius, profile.beamAlpha, intenseBeam)

        poseStack.withPose {
            translate(profile.starPos.x, profile.starPos.y, profile.starPos.z)
            mulPose(Axis.YP.rotationDegrees(profile.beamYawDegrees))
            RenderUtils.withSavedRenderState {
                renderCurrentSegments(shader, profile.tick, profile.colorR, profile.colorG, profile.colorB, 1.0f, 1.0f, 1.0f)
            }
        }

        return true
    }

    fun bounds(profile: AntichristRenderProfile, blockPos: BlockPos): AABB {
        var radius = 0f
        var near = Float.MAX_VALUE
        var far = -Float.MAX_VALUE

        for (intense in booleanArrayOf(false, true)) {
            if (intense) {
                writeIntenseBeamSegments(profile.starRadius, profile.beamAlpha, boundsBeam)
            } else {
                writeSoftBeamSegments(profile.starRadius, profile.beamAlpha, boundsBeam)
            }

            for (endpoint in 0..boundsBeam.segmentCount) {
                radius = max(radius, boundsBeam.radius(endpoint))
                near = minOf(near, boundsBeam.offset(endpoint))
                far = max(far, boundsBeam.offset(endpoint))
            }
        }

        val turn = Axis.YP.rotationDegrees(profile.beamYawDegrees)
        val corner = Vector3f()
        var bounds: AABB? = null

        for (i in 0..7) {
            turn.transform(
                corner.set(
                    if (i and 1 == 0) -radius else radius,
                    if (i and 2 == 0) -radius else radius,
                    if (i and 4 == 0) near else far
                )
            )
            val x = blockPos.x + profile.starPos.x + corner.x
            val y = blockPos.y + profile.starPos.y + corner.y
            val z = blockPos.z + profile.starPos.z + corner.z
            bounds = bounds?.minmax(AABB(x, y, z, x, y, z)) ?: AABB(x, y, z, x, y, z)
        }

        return requireNotNull(bounds)
    }

    fun renderVerticalLayers(
        blockEntity: BlockEntity,
        pose: PoseStack,
        tick: Double,
        origins: List<Vector3f>,
        height: Float,
        radius: Float,
        alpha: Float,
        shader: ShaderInstance,
        camera: Vec3
    ) {
        require(origins.size <= 6)
        if (origins.isEmpty() || height <= 0.001f) return

        val rotation = Quaternionf().rotationTo(0f, 0f, 1f, 0f, 1f, 0f)
        val builder = Tesselator.getInstance().builder
        builder.begin(VertexFormat.Mode.TRIANGLES, AntichristShaders.BEAM_VERTEX_FORMAT)

        for ((layer, origin) in origins.withIndex()) {
            val from = Vec3(origin.x.toDouble(), origin.y.toDouble(), origin.z.toDouble())
            updateCameraPositionInLocalBeamSpace(blockEntity, from, rotation, camera)
            writeLinearBeamSegments(radius, radius, height, alpha, softBeam)
            writeBeamVertices(builder, softBeam, tick, layer)
            beamCameraX[layer] = cameraPosition.x
            beamCameraY[layer] = cameraPosition.y

            pose.withPose {
                translate(from.x, from.y, from.z)
                mulPose(rotation)
                last().pose().get(beamViews, layer * 16)
            }
        }

        shader.getUniform("BeamViews")?.set(beamViews)
        shader.getUniform("BeamCameraX")?.set(beamCameraX)
        shader.getUniform("BeamCameraY")?.set(beamCameraY)
        RenderSystem.setShaderTexture(0, SPACE_LAYER)

        RenderUtils.withBoundBuffer(beamLayerBuffer) {
            beamLayerBuffer.uploadIndexed(builder.end(), beamLayerIndices)
            beamLayerBuffer.drawCount = origins.size * SEGMENT_QUADS * 6
            beamLayerBuffer.drawWithShader(pose.last().pose(), RenderSystem.getProjectionMatrix(), shader)
        }
    }

    private fun PoseStack.renderCurrentSegments(
        shader: ShaderInstance,
        tick: Double,
        colorR: Float,
        colorG: Float,
        colorB: Float,
        intenseColorR: Float,
        intenseColorG: Float,
        intenseColorB: Float
    ) {
        // The tube shows its inside too; blending is the program's own, depth comes from the composition baseline.
        RenderSystem.disableCull()
        RenderSystem.setShaderTexture(0, SPACE_LAYER)

        shader.getUniform("CameraPosition")?.set(cameraPosition.x, cameraPosition.y, cameraPosition.z)

        shader.getUniform("BeamColor")?.set(colorR, colorG, colorB)
        shader.getUniform("Intensity")?.set(2.0f)
        uploadBeamBuffer(softBeam, tick)
        RenderUtils.withBoundBuffer(beamBuffer) {
            beamBuffer.drawWithShader(last().pose(), RenderSystem.getProjectionMatrix(), shader)
        }

        shader.getUniform("BeamColor")?.set(intenseColorR, intenseColorG, intenseColorB)
        shader.getUniform("Intensity")?.set(4.0f)
        uploadBeamBuffer(intenseBeam, tick)
        RenderUtils.withBoundBuffer(beamBuffer) {
            beamBuffer.drawWithShader(last().pose(), RenderSystem.getProjectionMatrix(), shader)
        }
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

    private fun writeLinearBeamSegments(
        startRadius: Float,
        endRadius: Float,
        beamLength: Float,
        alpha: Float,
        segments: SegmentBuffer
    ) {
        segments.clear()
        segments.add(startRadius, 0.0f, alpha)
        segments.add(endRadius, beamLength, alpha)
        segments.repeatLastEndpoint()
    }

    private fun updateCameraPositionInBeamSpace(
        profile: AntichristRenderProfile,
        blockPos: BlockPos,
        camera: Vec3
    ) {
        val cameraRelativeX = camera.x - blockPos.x.toDouble() - profile.starPos.x
        val cameraRelativeY = camera.y - blockPos.y.toDouble() - profile.starPos.y
        val cameraRelativeZ = camera.z - blockPos.z.toDouble() - profile.starPos.z
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

    private fun updateCameraPositionInLocalBeamSpace(
        blockEntity: BlockEntity,
        origin: Vec3,
        rotation: Quaternionf,
        camera: Vec3
    ) {
        val blockPos = blockEntity.blockPos
        val relative = Vector3f(
            (camera.x - blockPos.x.toDouble() - origin.x).toFloat(),
            (camera.y - blockPos.y.toDouble() - origin.y).toFloat(),
            (camera.z - blockPos.z.toDouble() - origin.z).toFloat()
        )

        Quaternionf(rotation).conjugate().transform(relative)
        cameraPosition.set(relative)
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

    private fun uploadBeamBuffer(segments: SegmentBuffer, tick: Double) {
        val builder = Tesselator.getInstance().builder
        builder.begin(VertexFormat.Mode.TRIANGLES, AntichristShaders.BEAM_VERTEX_FORMAT)
        writeBeamVertices(builder, segments, tick)

        RenderUtils.withBoundBuffer(beamBuffer) {
            beamBuffer.uploadIndexed(builder.end(), beamIndices)
            beamBuffer.drawCount = segments.segmentCount * SEGMENT_QUADS * 6
        }
    }

    private fun writeBeamVertices(builder: BufferBuilder, segments: SegmentBuffer, tick: Double, layer: Int = 0) {
        val cameraAngle = atan2(cameraPosition.y, cameraPosition.x)

        for (endpoint in 0..segments.segmentCount) {
            for (angle in 0..SEGMENT_QUADS) {
                addBeamVertex(builder, segments, endpoint, angle, 2, cameraAngle, tick, layer)
            }
        }
    }

    private fun addBeamVertex(
        builder: BufferBuilder,
        segments: SegmentBuffer,
        endpointId: Int,
        quadId: Int,
        localId: Int,
        cameraAngle: Float,
        tick: Double,
        layer: Int
    ) {
        val radius = segments.radius(endpointId)
        val offset = segments.offset(endpointId)
        val angle = getVertexAngle(quadId, localId, cameraAngle)
        val x = cos(angle) * radius
        val y = sin(angle) * radius
        // Both texture coordinates repeat after three timer units.
        val timer = ((tick / 240.0) % 3.0).toFloat()
        val heightOffset = (offset / 256.0f) + timer
        val alpha = packAlpha(segments.transparency(endpointId))

        builder.vertex(x.toDouble(), y.toDouble(), offset.toDouble())
            .uv(heightOffset, angle / (2.0f * PI) + heightOffset / 3.0f + timer)
            .overlayCoords(alpha or (layer shl 16))
            .endVertex()
    }

    private fun packAlpha(alpha: Float): Int =
        (alpha * ALPHA_PACK_SCALE)
            .roundToInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()) and 0xFFFF

    private fun getVertexAngle(quadId: Int, localId: Int, cameraAngle: Float): Float {
        val idOffset = if (localId in 2..4) 0 else 1
        return (PI * (quadId + idOffset).toFloat()) / SEGMENT_QUADS.toFloat() + (cameraAngle - PI / 2.0f)
    }

    private class SegmentBuffer {
        val values = FloatArray(ENDPOINT_FLOATS)
        private var endpointCount = 0
        val segmentCount: Int
            get() = (endpointCount - 1).coerceAtLeast(0)

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

        fun radius(endpointId: Int): Float = values[endpointId * 3]

        fun offset(endpointId: Int): Float = values[endpointId * 3 + 1]

        fun transparency(endpointId: Int): Float = values[endpointId * 3 + 2]
    }
}
