package com.gtladd.gtladditions.utils

import com.gregtechceu.gtceu.client.renderer.GTRenderTypes
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.common.data.CircularMotionParams
import com.gtladd.gtladditions.common.data.RotationParams
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.FastColor
import net.minecraft.util.RandomSource
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.model.data.ModelData
import org.gtlcore.gtlcore.client.ClientUtil
import org.gtlcore.gtlcore.client.GlobalRenderClock
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

        poseStack.withPose {
            translate(position.x, position.y, position.z)

            if (faceMotion) {
                val facingAngle = motionParams.getFacingAngle(tick)
                mulPose(Quaternionf().fromAxisAngleDeg(0f, 1f, 0f, facingAngle + 90f))
            }

            if (additionalRotation != null) {
                mulPose(additionalRotation)
            }

            scale(size, size, size)

            if (centered) {
                translate(-0.5, -0.5, -0.5)
            }

            ClientUtil.modelRenderer().renderModel(
                last(),
                vertexConsumer, // Use pre-obtained consumer
                null,
                bakedModel, // Use pre-obtained model
                FastColor.ARGB32.red(argb32) / 255f,
                FastColor.ARGB32.green(argb32) / 255f,
                FastColor.ARGB32.blue(argb32) / 255f,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY,
                type
            )
        }
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
        poseStack.withPose {
            val center = motionParams.centerPos
            translate(center.x, center.y, center.z)

            val tiltAngleRad = Math.toRadians(motionParams.tiltAngle.toDouble()).toFloat()
            val tiltDir = motionParams.tiltDirection
            mulPose(Quaternionf().fromAxisAngleRad(tiltDir.x, tiltDir.y, tiltDir.z, tiltAngleRad))

            val vertexConsumer = buffer.getBuffer(GTRenderTypes.getLightRing())
            val matrix = last().pose()

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
        }
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
            val durationTicks = durationMillis / 50.0f // 1 tick = 50ms
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