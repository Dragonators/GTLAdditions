package com.gtladd.gtladditions.utils

import com.gregtechceu.gtceu.client.renderer.GTRenderTypes
import com.gtladd.gtladditions.client.GTLAddRenderTypes
import com.gtladd.gtladditions.common.data.CircularMotionParams
import com.gtladd.gtladditions.common.data.RotationParams
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import dev.ftb.mods.ftbchunks.client.FTBChunksRenderTypes
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FastColor
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.model.data.ModelData
import org.gtlcore.gtlcore.client.ClientUtil
import org.gtlcore.gtlcore.client.GlobalRenderClock
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.*

@Suppress("DuplicatedCode", "unused", "SameParameterValue")
@OnlyIn(Dist.CLIENT)
object RenderUtils {

    /**
     * Generates random rotation parameters with uniformly distributed axis and speed
     *
     * @param random   Random source for generating rotation parameters
     * @param minSpeed Minimum rotation speed in degrees per tick
     * @param maxSpeed Maximum rotation speed in degrees per tick
     * @return Rotation parameters containing axis, speed, and offset
     */
    fun createRandomRotation(random: RandomSource, minSpeed: Float, maxSpeed: Float): RotationParams {
        val theta = random.nextFloat() * 2.0f * Math.PI.toFloat()
        val phi = acos((2.0f * random.nextFloat() - 1.0f).toDouble()).toFloat()

        val rotationAxis = Vector3f(
            (sin(phi.toDouble()) * cos(theta.toDouble())).toFloat(),
            (sin(phi.toDouble()) * sin(theta.toDouble())).toFloat(),
            cos(phi.toDouble()).toFloat()
        )

        // Use square root distribution to concentrate speeds in the middle range, avoiding extreme fast rotation
        val speedFactor = sqrt(random.nextFloat().toDouble()).toFloat()
        val rotationSpeed = minSpeed + speedFactor * (maxSpeed - minSpeed)
        val rotationOffset = random.nextFloat() * 360f
        return RotationParams(rotationAxis, rotationSpeed, rotationOffset)
    }

    /**
     * Renders a star layer with color tinting and rotation
     *
     * @param poseStack     Pose stack for transformations
     * @param buffer        Multi-buffer source for rendering
     * @param modelLocation Resource location of the model to render
     * @param size          Scale size of the star layer
     * @param rotationAxis  Rotation axis vector
     * @param angle         Rotation angle in degrees
     * @param argb32        ARGB32 color value for tinting
     * @param type          Render type to use
     */
    fun renderStarLayer(
        poseStack: PoseStack, buffer: MultiBufferSource,
        modelLocation: ResourceLocation?, size: Float,
        rotationAxis: Vector3f, angle: Float, argb32: Int, type: RenderType
    ) {
        poseStack.pushPose()
        poseStack.scale(size, size, size)
        poseStack.mulPose(
            Quaternionf().fromAxisAngleDeg(
                rotationAxis.x, rotationAxis.y, rotationAxis.z, angle
            )
        )

        ClientUtil.modelRenderer().renderModel(
            poseStack.last(),
            buffer.getBuffer(type),
            null,
            ClientUtil.getBakedModel(modelLocation),
            FastColor.ARGB32.red(argb32) / 255f,
            FastColor.ARGB32.green(argb32) / 255f,
            FastColor.ARGB32.blue(argb32) / 255f,
            LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            ModelData.EMPTY,
            type
        )
        poseStack.popPose()
    }

    /**
     * Renders a halo layer with glow effect using eyes render type
     *
     * @param poseStack     Pose stack for transformations
     * @param buffer        Multi-buffer source for rendering
     * @param size          Scale size of the halo layer
     * @param rotationAxis  Rotation axis vector
     * @param angle         Rotation angle in degrees
     * @param haloTexture   Texture resource location for the halo glow effect
     * @param modelLocation Resource location of the model to render
     */
    fun renderHaloLayer(
        poseStack: PoseStack, buffer: MultiBufferSource, size: Float,
        rotationAxis: Vector3f, angle: Float,
        haloTexture: ResourceLocation, modelLocation: ResourceLocation
    ) {
        renderHaloLayer(poseStack, buffer, size, rotationAxis, angle, haloTexture, modelLocation, 1.0f, false)
    }

    /**
     * Renders a halo layer with glow effect and brightness control
     *
     * @param poseStack     Pose stack for transformations
     * @param buffer        Multi-buffer source for rendering
     * @param size          Scale size of the halo layer
     * @param rotationAxis  Rotation axis vector
     * @param angle         Rotation angle in degrees
     * @param haloTexture   Texture resource location for the halo glow effect
     * @param modelLocation Resource location of the model to render
     * @param alpha         Alpha/brightness multiplier (0.0 - 1.0), controls glow intensity
     * @param useCustomRenderType If true, uses custom non-additive blending to prevent over-brightness
     */
    fun renderHaloLayer(
        poseStack: PoseStack, buffer: MultiBufferSource, size: Float,
        rotationAxis: Vector3f, angle: Float,
        haloTexture: ResourceLocation, modelLocation: ResourceLocation,
        alpha: Float = 1.0f,
        useCustomRenderType: Boolean = false
    ) {
        poseStack.pushPose()
        poseStack.scale(size, size, size)
        poseStack.mulPose(
            Quaternionf().fromAxisAngleDeg(
                rotationAxis.x, rotationAxis.y, rotationAxis.z, angle
            )
        )

        val renderType = if (useCustomRenderType) {
            GTLAddRenderTypes.createFullBrightGlowLayer(haloTexture)
        } else {
            RenderType.eyes(haloTexture)
        }

        val consumer = buffer.getBuffer(renderType)

        val finalAlpha = alpha.coerceIn(0.0f, 1.0f)

        ClientUtil.modelRenderer().renderModel(
            poseStack.last(),
            consumer,
            null,
            ClientUtil.getBakedModel(modelLocation),
            finalAlpha, finalAlpha, finalAlpha,
            LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            ModelData.EMPTY,
            renderType
        )
        poseStack.popPose()
    }

    /**
     * Draws a beacon from the machine position to the star center
     * with fade effect near the star, always facing the camera
     *
     * @param poseStack   Pose stack for transformations
     * @param buffer      Multi-buffer source for rendering
     * @param starX       X coordinate of the star center (block-relative)
     * @param starY       Y coordinate of the star center (block-relative)
     * @param starZ       Z coordinate of the star center (block-relative)
     * @param argb32      ARGB32 color value for the beacon
     * @param tick        Current tick count with partial ticks for animation
     * @param blockEntity Block entity to get the machine position from
     * @param outerRadius Outer radius of the star, used to calculate beam width multiplier
     */
    fun drawBeaconToStar(
        poseStack: PoseStack, buffer: MultiBufferSource,
        starX: Double, starY: Double, starZ: Double,
        argb32: Int, tick: Float, blockEntity: BlockEntity, outerRadius: Float
    ) {
        val vertexConsumer = buffer.getBuffer(FTBChunksRenderTypes.WAYPOINTS_DEPTH)

        // Extract RGB from ARGB
        val r = (argb32 shr 16) and 0xFF
        val g = (argb32 shr 8) and 0xFF
        val b = argb32 and 0xFF

        // Get camera position and convert to block-relative coordinates
        val cameraWorldPos = Minecraft.getInstance().gameRenderer.mainCamera.position
        val blockWorldPos = Vec3.atLowerCornerOf(blockEntity.blockPos)
        val playerPos = cameraWorldPos.subtract(blockWorldPos) // Convert to block-relative coordinates

        // Beacon parameters
        val beaconWidth = 2.9f
        val fadeRatio = 0.1f
        val expandRatio = 0.4f
        val endWidthMultiplier = outerRadius / beaconWidth * 144

        // Calculate alpha with pulsing effect
        val baseAlpha = 150
        val pulse = (sin(tick * 0.05) * 0.2 + 0.8).toFloat()
        var alpha = (baseAlpha * pulse).toInt()
        alpha = max(0, min(255, alpha))

        // Define start and end points (both in block-relative coordinates)
        val from = Vec3(0.5, 0.5, 0.5) // Block center
        val to = Vec3(starX, starY, starZ) // Star position

        // Render the beacon
        drawBeaconBetweenPoints(
            poseStack,
            from,
            to,
            playerPos,
            vertexConsumer,
            r, g, b, alpha,
            beaconWidth,
            fadeRatio,
            expandRatio,
            endWidthMultiplier
        )
    }

    /**
     * Draws a beacon beam from a base position upward to the sky (Y + 360)
     * with fade effect at the top, always facing the camera
     *
     * @param poseStack   Pose stack for transformations
     * @param buffer      Multi-buffer source for rendering
     * @param baseX       X coordinate of the beam starting point (relative to current poseStack origin)
     * @param baseY       Y coordinate of the beam starting point (relative to current poseStack origin)
     * @param baseZ       Z coordinate of the beam starting point (relative to current poseStack origin)
     * @param argb32      ARGB32 color value for the beacon
     * @param tick        Current tick count with partial ticks for animation
     * @param blockEntity Block entity reference for coordinate system calculations
     * @param beaconWidth Beam width multiplier base value
     */
    fun drawBeaconToSky(
        poseStack: PoseStack, buffer: MultiBufferSource,
        baseX: Double, baseY: Double, baseZ: Double,
        argb32: Int, tick: Float, blockEntity: BlockEntity, beaconWidth: Float
    ) {
        val from = Vec3(baseX, baseY + 460.0, baseZ)
        val to = Vec3(baseX, baseY, baseZ)
        drawBeaconToSky(poseStack, buffer, from, to, argb32, tick, blockEntity, beaconWidth)
    }

    /**
     * Draws a beacon beam between two arbitrary points
     * with fade effect, always facing the camera
     *
     * @param poseStack   Pose stack for transformations
     * @param buffer      Multi-buffer source for rendering
     * @param from        Starting point of the beam (relative to current poseStack origin)
     * @param to          Ending point of the beam (relative to current poseStack origin)
     * @param argb32      ARGB32 color value for the beacon
     * @param tick        Current tick count with partial ticks for animation
     * @param blockEntity Block entity reference for coordinate system calculations
     * @param beaconWidth Beam width multiplier base value
     */
    fun drawBeaconToSky(
        poseStack: PoseStack, buffer: MultiBufferSource,
        from: Vec3, to: Vec3,
        argb32: Int, tick: Float, blockEntity: BlockEntity, beaconWidth: Float
    ) {
        drawBeacon(
            poseStack, buffer, from, to, argb32, tick, blockEntity,
            beaconWidth, 0.1f, 0.3f, beaconWidth * 3
        )
    }

    /**
     * Draws a beacon beam between two arbitrary points with full control over appearance
     * with fade effect and optional expansion, always facing the camera
     *
     * @param poseStack          Pose stack for transformations
     * @param buffer             Multi-buffer source for rendering
     * @param from               Starting point of the beam (relative to current poseStack origin)
     * @param to                 Ending point of the beam (relative to current poseStack origin)
     * @param argb32             ARGB32 color value for the beacon
     * @param tick               Current tick count with partial ticks for animation
     * @param blockEntity        Block entity reference for coordinate system calculations
     * @param beaconWidth        Beam width base value
     * @param fadeRatio          Ratio of beam length that fades out (0.0-1.0, 0 = no fade)
     * @param expandRatio        Ratio of beam length where expansion starts (0.0-1.0, 0 = no expand)
     * @param endWidthMultiplier Width multiplier at the end point (1.0 = straight beam, no expansion)
     */
    fun drawBeacon(
        poseStack: PoseStack, buffer: MultiBufferSource,
        from: Vec3, to: Vec3,
        argb32: Int, tick: Float, blockEntity: BlockEntity,
        beaconWidth: Float, fadeRatio: Float, expandRatio: Float, endWidthMultiplier: Float
    ) {
        val vertexConsumer = buffer.getBuffer(FTBChunksRenderTypes.WAYPOINTS_DEPTH)

        // Extract RGB from ARGB
        val r = (argb32 shr 16) and 0xFF
        val g = (argb32 shr 8) and 0xFF
        val b = argb32 and 0xFF

        val cameraWorldPos = Minecraft.getInstance().gameRenderer.mainCamera.position
        val blockWorldPos = Vec3.atLowerCornerOf(blockEntity.blockPos)
        val playerPos = cameraWorldPos.subtract(blockWorldPos)

        val baseAlpha = 150
        val pulse = (sin(tick * 0.05) * 0.2 + 0.8).toFloat()
        var alpha = (baseAlpha * pulse).toInt()
        alpha = max(0, min(255, alpha))

        drawBeaconBetweenPoints(
            poseStack,
            from,
            to,
            playerPos,
            vertexConsumer,
            r, g, b, alpha,
            beaconWidth,
            fadeRatio,
            expandRatio,
            endWidthMultiplier
        )
    }

    /**
     * Renders a beacon beam between two arbitrary 3D points, facing the camera.
     * The beam consists of a solid section and a fading section for smooth visual effect.
     *
     * @param poseStack          The pose stack for transformations (must already be in camera-relative space)
     * @param from               Starting point of the beacon beam (world coordinates)
     * @param to                 Ending point of the beacon beam (world coordinates)
     * @param playerPos          Camera position (world coordinates, used for billboard calculation)
     * @param buffer             Vertex consumer for rendering
     * @param r                  Red color component (0-255)
     * @param g                  Green color component (0-255)
     * @param b                  Blue color component (0-255)
     * @param alpha              Alpha value at the solid end (0-255)
     * @param width              Width of the beacon beam
     * @param fadeRatio          Ratio of the beam length that fades out (0.0-1.0, e.g., 0.3 = 30% fade)
     * @param expandRatio        Ratio of the beam length where expansion starts (0.0-1.0, e.g., 0.4 = last 40% expands)
     * @param endWidthMultiplier Multiplier for the beam width at the end point
     */
    fun drawBeaconBetweenPoints(
        poseStack: PoseStack,
        from: Vec3,
        to: Vec3,
        playerPos: Vec3,
        buffer: VertexConsumer,
        r: Int, g: Int, b: Int, alpha: Int,
        width: Float,
        fadeRatio: Float,
        expandRatio: Float,
        endWidthMultiplier: Float
    ) {
        val beamDirX = to.x - from.x
        val beamDirY = to.y - from.y
        val beamDirZ = to.z - from.z
        val beamLength = sqrt(beamDirX * beamDirX + beamDirY * beamDirY + beamDirZ * beamDirZ)
        if (beamLength < 0.01) return

        val invLength = 1.0 / beamLength
        val normDirX = beamDirX * invLength
        val normDirY = beamDirY * invLength
        val normDirZ = beamDirZ * invLength

        val toPlayerX = playerPos.x - from.x
        val toPlayerY = playerPos.y - from.y
        val toPlayerZ = playerPos.z - from.z

        var projectionLength = toPlayerX * normDirX + toPlayerY * normDirY + toPlayerZ * normDirZ
        projectionLength = max(0.0, min(beamLength, projectionLength))

        val perpFootX = from.x + normDirX * projectionLength
        val perpFootY = from.y + normDirY * projectionLength
        val perpFootZ = from.z + normDirZ * projectionLength

        val perpX = playerPos.x - perpFootX
        val perpY = playerPos.y - perpFootY
        val perpZ = playerPos.z - perpFootZ
        val perpLength = sqrt(perpX * perpX + perpY * perpY + perpZ * perpZ)

        val billboardNormX: Double
        val billboardNormY: Double
        val billboardNormZ: Double

        if (perpLength < 0.001) {
            val arbX = if (abs(normDirY) > 0.9) 1.0 else 0.0
            val arbY = if (abs(normDirY) > 0.9) 0.0 else 1.0
            val arbZ = 0.0

            val crossX = normDirY * arbZ - normDirZ * arbY
            val crossY = normDirZ * arbX - normDirX * arbZ
            val crossZ = normDirX * arbY - normDirY * arbX
            val crossLen = sqrt(crossX * crossX + crossY * crossY + crossZ * crossZ)

            billboardNormX = crossX / crossLen
            billboardNormY = crossY / crossLen
            billboardNormZ = crossZ / crossLen
        } else {
            val invPerpLen = 1.0 / perpLength
            billboardNormX = perpX * invPerpLen
            billboardNormY = perpY * invPerpLen
            billboardNormZ = perpZ * invPerpLen
        }

        val widthDirX = normDirY * billboardNormZ - normDirZ * billboardNormY
        val widthDirY = normDirZ * billboardNormX - normDirX * billboardNormZ
        val widthDirZ = normDirX * billboardNormY - normDirY * billboardNormX
        val widthDirLen = sqrt(widthDirX * widthDirX + widthDirY * widthDirY + widthDirZ * widthDirZ)

        val widthDirNormX = widthDirX / widthDirLen
        val widthDirNormY = widthDirY / widthDirLen
        val widthDirNormZ = widthDirZ / widthDirLen

        val fadeStartDist = beamLength * (1.0 - fadeRatio)
        val expandStartDist = beamLength * (1.0 - expandRatio)
        val baseWidthHalf = width * 0.5f
        val endWidthHalf = width * endWidthMultiplier * 0.5f

        val pose = poseStack.last()
        val matrix = pose.pose()

        val segments = when {
            beamLength < 10.0 -> 8
            beamLength < 50.0 -> 16
            beamLength < 200.0 -> 24
            else -> 32
        }

        val invSegments = 1.0 / segments

        for (i in 0..<segments) {
            val segStart = i * beamLength * invSegments
            val segEnd = (i + 1) * beamLength * invSegments

            val segStartX = from.x + normDirX * segStart
            val segStartY = from.y + normDirY * segStart
            val segStartZ = from.z + normDirZ * segStart

            val segEndX = from.x + normDirX * segEnd
            val segEndY = from.y + normDirY * segEnd
            val segEndZ = from.z + normDirZ * segEnd

            val segStartWidth = calculateSegmentWidth(
                segStart, expandStartDist, beamLength,
                baseWidthHalf, endWidthHalf, expandRatio
            ).toDouble()
            val segEndWidth = calculateSegmentWidth(
                segEnd, expandStartDist, beamLength,
                baseWidthHalf, endWidthHalf, expandRatio
            ).toDouble()

            val segStartAlpha = calculateSegmentAlpha(
                segStart, fadeStartDist, beamLength,
                alpha, fadeRatio
            )
            val segEndAlpha = calculateSegmentAlpha(
                segEnd, fadeStartDist, beamLength,
                alpha, fadeRatio
            )

            val v1 = (segStart / beamLength).toFloat()
            val v2 = (segEnd / beamLength).toFloat()

            val p1x = segStartX - widthDirNormX * segStartWidth
            val p1y = segStartY - widthDirNormY * segStartWidth
            val p1z = segStartZ - widthDirNormZ * segStartWidth

            val p2x = segStartX + widthDirNormX * segStartWidth
            val p2y = segStartY + widthDirNormY * segStartWidth
            val p2z = segStartZ + widthDirNormZ * segStartWidth

            val p3x = segEndX + widthDirNormX * segEndWidth
            val p3y = segEndY + widthDirNormY * segEndWidth
            val p3z = segEndZ + widthDirNormZ * segEndWidth

            val p4x = segEndX - widthDirNormX * segEndWidth
            val p4y = segEndY - widthDirNormY * segEndWidth
            val p4z = segEndZ - widthDirNormZ * segEndWidth

            buffer.vertex(matrix, p1x.toFloat(), p1y.toFloat(), p1z.toFloat())
                .color(r, g, b, segStartAlpha)
                .uv(0.0f, v1)
                .endVertex()

            buffer.vertex(matrix, p2x.toFloat(), p2y.toFloat(), p2z.toFloat())
                .color(r, g, b, segStartAlpha)
                .uv(1.0f, v1)
                .endVertex()

            buffer.vertex(matrix, p3x.toFloat(), p3y.toFloat(), p3z.toFloat())
                .color(r, g, b, segEndAlpha)
                .uv(1.0f, v2)
                .endVertex()

            buffer.vertex(matrix, p4x.toFloat(), p4y.toFloat(), p4z.toFloat())
                .color(r, g, b, segEndAlpha)
                .uv(0.0f, v2)
                .endVertex()
        }
    }

    private fun calculateSegmentWidth(
        position: Double, expandStart: Double, beamLength: Double,
        baseWidth: Float, endWidth: Float, expandRatio: Float
    ): Float {
        if (position <= expandStart) {
            return baseWidth
        }

        val expandLength = beamLength * expandRatio
        var progress = (position - expandStart) / expandLength
        progress = min(1.0, progress)

        val t = (progress * progress * (3.0 - 2.0 * progress)).toFloat()
        return baseWidth + (endWidth - baseWidth) * t
    }

    private fun calculateSegmentAlpha(
        position: Double, fadeStart: Double, beamLength: Double,
        maxAlpha: Int, fadeRatio: Float
    ): Int {
        if (position <= fadeStart) {
            return maxAlpha
        }

        val fadeLength = beamLength * fadeRatio
        var progress = (position - fadeStart) / fadeLength
        progress = min(1.0, progress)

        return (maxAlpha * (1.0 - progress)).toInt()
    }

    private fun renderQuadSimple(
        buffer: VertexConsumer, matrix: Matrix4f,
        p1: Vec3, p2: Vec3, p3: Vec3, p4: Vec3,
        r: Int, g: Int, b: Int, alphaStart: Int, alphaEnd: Int,
        u1: Float, v1: Float, u2: Float, v2: Float
    ) {
        buffer.vertex(matrix, p1.x.toFloat(), p1.y.toFloat(), p1.z.toFloat())
            .color(r, g, b, alphaStart)
            .uv(u1, v1)
            .endVertex()

        buffer.vertex(matrix, p2.x.toFloat(), p2.y.toFloat(), p2.z.toFloat())
            .color(r, g, b, alphaStart)
            .uv(u2, v1)
            .endVertex()

        buffer.vertex(matrix, p3.x.toFloat(), p3.y.toFloat(), p3.z.toFloat())
            .color(r, g, b, alphaEnd)
            .uv(u2, v2)
            .endVertex()

        buffer.vertex(matrix, p4.x.toFloat(), p4.y.toFloat(), p4.z.toFloat())
            .color(r, g, b, alphaEnd)
            .uv(u1, v2)
            .endVertex()
    }

    /**
     * Renders a model performing circular motion around a center point in a tilted plane
     *
     * @param poseStack     Pose stack for transformations
     * @param buffer        Multi-buffer source for rendering
     * @param modelLocation Resource location of the model to render
     * @param motionParams  Parameters defining the circular motion
     * @param tick          Current tick with partial ticks for animation
     * @param size          Scale size of the model
     * @param argb32        ARGB32 color value for tinting
     * @param type          Render type to use
     * @param faceMotion    If true, rotate the model to face the direction of motion
     * @param additionalRotation Optional additional rotation to apply after positioning
     * @param centered      If true, center the model at the orbit position (offsets by -0.5 in all axes after scaling)
     */
    fun renderCircularMotionModel(
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        modelLocation: ResourceLocation?,
        motionParams: CircularMotionParams,
        tick: Float,
        size: Float = 1.0f,
        argb32: Int = 0xFFFFFFFF.toInt(),
        type: RenderType,
        faceMotion: Boolean = true,
        additionalRotation: Quaternionf? = null,
        centered: Boolean = false
    ) {
        val vertexConsumer = buffer.getBuffer(type)
        val bakedModel = ClientUtil.getBakedModel(modelLocation)
        renderCircularMotionModelDirect(
            poseStack, vertexConsumer, bakedModel, motionParams, tick,
            size, argb32, type, faceMotion, additionalRotation, centered
        )
    }

    /**
     * Renders a model performing circular motion using a pre-obtained VertexConsumer and BakedModel
     * Use this for batch rendering multiple models with the same RenderType
     *
     * @param poseStack     Pose stack for transformations
     * @param vertexConsumer Pre-obtained vertex consumer (for batch rendering)
     * @param bakedModel    Pre-obtained baked model (for batch rendering)
     * @param motionParams  Parameters defining the circular motion
     * @param tick          Current tick with partial ticks for animation
     * @param size          Scale size of the model
     * @param argb32        ARGB32 color value for tinting
     * @param type          Render type to use
     * @param faceMotion    If true, rotate the model to face the direction of motion
     * @param additionalRotation Optional additional rotation to apply after positioning
     * @param centered      If true, center the model at the orbit position (offsets by -0.5 in all axes after scaling)
     */
    fun renderCircularMotionModelDirect(
        poseStack: PoseStack,
        vertexConsumer: VertexConsumer,
        bakedModel: net.minecraft.client.resources.model.BakedModel,
        motionParams: CircularMotionParams,
        tick: Float,
        size: Float = 1.0f,
        argb32: Int = 0xFFFFFFFF.toInt(),
        type: RenderType,
        faceMotion: Boolean = true,
        additionalRotation: Quaternionf? = null,
        centered: Boolean = false
    ) {
        val position = motionParams.getPosition(tick)

        poseStack.pushPose()
        poseStack.translate(position.x, position.y, position.z)

        if (faceMotion) {
            val facingAngle = motionParams.getFacingAngle(tick)
            poseStack.mulPose(Quaternionf().fromAxisAngleDeg(0f, 1f, 0f, facingAngle + 90f))
        }

        if (additionalRotation != null) {
            poseStack.mulPose(additionalRotation)
        }

        poseStack.scale(size, size, size)

        if (centered) {
            poseStack.translate(-0.5, -0.5, -0.5)
        }

        ClientUtil.modelRenderer().renderModel(
            poseStack.last(),
            vertexConsumer,  // Use pre-obtained consumer
            null,
            bakedModel,  // Use pre-obtained model
            FastColor.ARGB32.red(argb32) / 255f,
            FastColor.ARGB32.green(argb32) / 255f,
            FastColor.ARGB32.blue(argb32) / 255f,
            LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            ModelData.EMPTY,
            type
        )

        poseStack.popPose()
    }

    /**
     * Renders an orbit ring directly using a pre-obtained VertexConsumer
     * Use this for batch rendering multiple rings with the same RenderType
     *
     * @param poseStack Pose stack for transformations
     * @param buffer Multi-buffer source for rendering
     * @param motionParams Circular motion parameters defining the orbit
     * @param segments Number of segments (default 128 for full quality, reduce for LOD)
     */
    fun renderOrbitRing(
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        motionParams: CircularMotionParams,
        segments: Int = 128
    ) {
        poseStack.pushPose()

        val center = motionParams.centerPos
        poseStack.translate(center.x, center.y, center.z)

        val tiltAngleRad = Math.toRadians(motionParams.tiltAngle.toDouble()).toFloat()
        val tiltDir = motionParams.tiltDirection
        poseStack.mulPose(Quaternionf().fromAxisAngleRad(tiltDir.x, tiltDir.y, tiltDir.z, tiltAngleRad))

        val vertexConsumer = buffer.getBuffer(GTRenderTypes.getLightRing())
        val matrix = poseStack.last().pose()

        val radius = motionParams.radius
        val lineWidth = 1.5f

        for (i in 0..<segments) {
            val angle1 = (2 * Math.PI * i / segments).toFloat()
            val angle2 = (2 * Math.PI * (i + 1) / segments).toFloat()

            val cos1 = MathCache.fastCos(angle1)
            val sin1 = MathCache.fastSin(angle1)
            val cos2 = MathCache.fastCos(angle2)
            val sin2 = MathCache.fastSin(angle2)

            val x1 = radius * cos1
            val z1 = radius * sin1
            val x2 = radius * cos2
            val z2 = radius * sin2

            val perpX1 = -sin1 * lineWidth
            val perpZ1 = cos1 * lineWidth
            val perpX2 = -sin2 * lineWidth
            val perpZ2 = cos2 * lineWidth

            vertexConsumer.vertex(matrix, x1 - perpX1, 0f, z1 - perpZ1)
                .color(255, 255, 255, 200)
                .uv(0f, 0f)
                .endVertex()

            vertexConsumer.vertex(matrix, x1 + perpX1, 0f, z1 + perpZ1)
                .color(255, 255, 255, 200)
                .uv(1f, 0f)
                .endVertex()

            vertexConsumer.vertex(matrix, x2 + perpX2, 0f, z2 + perpZ2)
                .color(255, 255, 255, 200)
                .uv(1f, 1f)
                .endVertex()

            vertexConsumer.vertex(matrix, x2 - perpX2, 0f, z2 - perpZ2)
                .color(255, 255, 255, 200)
                .uv(0f, 1f)
                .endVertex()
        }

        poseStack.popPose()
    }

    class SmoothAnimationTimer {
        private var startTick: Float = GlobalRenderClock.getSmoothTick()

        fun reset() {
            startTick = GlobalRenderClock.getSmoothTick()
        }

        /**
         * @param durationMillis Animation duration (milliseconds)
         * @return Current progress, range 0.0 to 1.0
         */
        fun getProgress(durationMillis: Long): Float {
            val durationTicks = durationMillis / 50.0f  // 1 tick = 50ms
            val elapsedTicks = GlobalRenderClock.getSmoothTick() - startTick
            return (elapsedTicks / durationTicks).coerceIn(0f, 1f)
        }
    }

    private object MathCache {
        private const val TABLE_SIZE = 4096
        private const val TABLE_SIZE_MASK = TABLE_SIZE - 1
        private const val ANGLE_TO_INDEX = TABLE_SIZE / (2.0 * Math.PI)

        private val sinTable = FloatArray(TABLE_SIZE)
        private val cosTable = FloatArray(TABLE_SIZE)

        init {
            for (i in 0 until TABLE_SIZE) {
                val angle = i * 2.0 * Math.PI / TABLE_SIZE
                sinTable[i] = sin(angle).toFloat()
                cosTable[i] = cos(angle).toFloat()
            }
        }

        fun fastSin(radians: Float): Float {
            val index = (radians * ANGLE_TO_INDEX).toInt() and TABLE_SIZE_MASK
            return sinTable[index]
        }

        fun fastCos(radians: Float): Float {
            val index = (radians * ANGLE_TO_INDEX).toInt() and TABLE_SIZE_MASK
            return cosTable[index]
        }
    }
}