package com.gtladd.gtladditions.utils

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
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.*

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
    @JvmStatic
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
    @JvmStatic
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
    @JvmStatic
    fun renderHaloLayer(
        poseStack: PoseStack, buffer: MultiBufferSource, size: Float,
        rotationAxis: Vector3f, angle: Float,
        haloTexture: ResourceLocation, modelLocation: ResourceLocation?
    ) {
        poseStack.pushPose()
        poseStack.scale(size, size, size)
        poseStack.mulPose(
            Quaternionf().fromAxisAngleDeg(
                rotationAxis.x, rotationAxis.y, rotationAxis.z, angle
            )
        )

        val consumer = buffer.getBuffer(RenderType.eyes(haloTexture))

        ClientUtil.modelRenderer().renderModel(
            poseStack.last(),
            consumer,
            null,
            ClientUtil.getBakedModel(modelLocation),
            1.0f, 1.0f, 1.0f,
            LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            ModelData.EMPTY,
            RenderType.eyes(haloTexture)
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
    @JvmStatic
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
        val beamDirection = to.subtract(from)
        val beamLength = beamDirection.length()
        if (beamLength < 0.01) return

        val normalizedBeamDir = beamDirection.normalize()

        val fromToPlayer = playerPos.subtract(from)

        var projectionLength = fromToPlayer.dot(normalizedBeamDir)

        projectionLength = max(0.0, min(beamLength, projectionLength))

        val perpFootPoint = from.add(normalizedBeamDir.scale(projectionLength))

        val perpToPlayer = playerPos.subtract(perpFootPoint)
        val perpLength = perpToPlayer.length()

        val billboardNormal: Vec3?
        if (perpLength < 0.001) {
            val arbitrary = if (abs(normalizedBeamDir.y) > 0.9) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)
            billboardNormal = normalizedBeamDir.cross(arbitrary).normalize()
        } else {
            billboardNormal = perpToPlayer.normalize()
        }

        val widthDirection = normalizedBeamDir.cross(billboardNormal).normalize()

        val fadeStartDist = beamLength * (1.0 - fadeRatio)
        val expandStartDist = beamLength * (1.0 - expandRatio)

        val pose = poseStack.last()
        val matrix = pose.pose()

        val baseWidthHalf = width * 0.5f
        val endWidthHalf = width * endWidthMultiplier * 0.5f

        val segments = 32
        for (i in 0..<segments) {
            val segStart = i * beamLength / segments
            val segEnd = (i + 1) * beamLength / segments

            val segStartPos = from.add(normalizedBeamDir.scale(segStart))
            val segEndPos = from.add(normalizedBeamDir.scale(segEnd))

            val segStartWidth = calculateSegmentWidth(
                segStart, expandStartDist, beamLength,
                baseWidthHalf, endWidthHalf, expandRatio
            )
            val segEndWidth = calculateSegmentWidth(
                segEnd, expandStartDist, beamLength,
                baseWidthHalf, endWidthHalf, expandRatio
            )

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

            val p1 = segStartPos.subtract(widthDirection.scale(segStartWidth.toDouble()))
            val p2 = segStartPos.add(widthDirection.scale(segStartWidth.toDouble()))
            val p3 = segEndPos.add(widthDirection.scale(segEndWidth.toDouble()))
            val p4 = segEndPos.subtract(widthDirection.scale(segEndWidth.toDouble()))

            renderQuadSimple(
                buffer, matrix, p1, p2, p3, p4,
                r, g, b, segStartAlpha, segEndAlpha,
                0.0f, v1, 1.0f, v2
            )
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

    @Suppress("DuplicatedCode", "SameParameterValue")
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

    @JvmRecord
    data class RotationParams(val axis: Vector3f, val speed: Float, val offset: Float) {
        fun getAngle(tick: Float): Float {
            return (offset + tick * speed) % 360f
        }
    }
}