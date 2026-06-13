package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.client.renderer.machine.WorkableCasingMachineRenderer
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.render.machine.lighthunter.LightHunterSpaceStationBeamRenderer
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.common.data.CircularMotionParams
import com.gtladd.gtladditions.common.data.RotationParams
import com.gtladd.gtladditions.common.machine.multiblock.controller.LightHunterSpaceStation
import com.gtladd.gtladditions.utils.CommonUtils.getRotatedRenderPosition
import com.gtladd.gtladditions.utils.Constants.ORBIT_OBJECTS
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.vertex.PoseStack
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FastColor
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.client.ClientUtil
import org.gtlcore.gtlcore.utils.RenderUtil
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class LightHunterSpaceStationRenderer :
    WorkableCasingMachineRenderer(
        GTCEu.id("block/casings/hpca/high_power_casing"),
        GTCEu.id("block/multiblock/data_bank")
    ) {

    @OnlyIn(Dist.CLIENT)
    override fun render(
        blockEntity: BlockEntity,
        partialTicks: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        combinedLight: Int,
        combinedOverlay: Int
    ) {
        if (blockEntity is IMachineBlockEntity) {
            val machine = blockEntity.metaMachine as? LightHunterSpaceStation ?: return
            if (machine.recipeLogic.isWorking) {
                val tick = RenderUtil.getSmoothTick(machine, partialTicks)
                val seed = blockEntity.blockPos.asLong()
                val facing = machine.frontFacing
                val beamEnd = getRotatedRenderPosition(BASE_DIRECTION, facing, BEAM_OFFSET_X, 0.0, 0.0)
                val starPos = getRotatedRenderPosition(BASE_DIRECTION, facing, STAR_OFFSET_X, 0.0, 0.0)

                LightHunterSpaceStationBeamRenderer.enqueue(blockEntity, tick, Vec3(0.5, 0.5, 0.5), beamEnd)
                if (machine.unlockParadoxical()) {
                    renderBlackHole(poseStack, buffer, facing, starPos, tick, seed)
                } else {
                    renderStar(poseStack, buffer, facing, starPos, tick, seed)
                }
                renderOrbit(poseStack, buffer, facing, starPos, tick, seed)
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    override fun onAdditionalModel(registry: Consumer<ResourceLocation>) {
        super.onAdditionalModel(registry)
        registry.accept(STAR_LAYER)
        registry.accept(SPACE_MODEL)
        ORBIT_OBJECTS.forEach(registry)
    }

    @OnlyIn(Dist.CLIENT)
    override fun hasTESR(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun isGlobalRenderer(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun getViewDistance(): Int = 384

    @OnlyIn(Dist.CLIENT)
    private data class RenderCache(val seed: Long, val centerPos: Vec3) {
        val planetRotations = ObjectArrayList<RotationParams>(13)
        val orbitParams = ObjectArrayList<CircularMotionParams>(13)
        val planetSizes = ObjectArrayList<Float>(13)
        val starRotation: RotationParams

        init {
            for (i in ORBIT_OBJECTS.indices) {
                planetRotations.add(RenderUtils.createRandomRotation(RandomSource.create(seed + i * 2000L), 0.1f, 0.5f))
                orbitParams.add(getCircularMotionParams(seed, i, centerPos, MIN_RADIUS, MAX_RADIUS, MIN_TILT_ANGLE, MAX_TILT_ANGLE))
                planetSizes.add(getPlanetSize(seed, i, 1.3f, 2.7f))
            }
            starRotation = RenderUtils.createRandomRotation(RandomSource.create(seed), 0.5f, 2.0f)
        }
    }

    @OnlyIn(Dist.CLIENT)
    companion object {
        private const val BEAM_OFFSET_X = -174.0
        private const val STAR_OFFSET_X = -206.0
        private const val MIN_RADIUS = 22.0f
        private const val MAX_RADIUS = 53.0f
        private const val MIN_TILT_ANGLE = 70f
        private const val MAX_TILT_ANGLE = 90f
        private val BASE_DIRECTION = Direction.EAST

        private val STAR_LAYER = GTLAdditions.id("multiblock/light_hunter_space_station/star_layer_1")
        private val SPACE_MODEL = GTLAdditions.id("multiblock/light_hunter_space_station/space")
        private val HALO_TEX = GTLAdditions.id("textures/block/obj/halo_tex1.png")
        private val CACHE_MAP = ConcurrentHashMap<Long, RenderCache>()

        private fun renderStar(
            poseStack: PoseStack,
            buffer: MultiBufferSource,
            facing: Direction,
            starPos: Vec3,
            tick: Float,
            seed: Long
        ) {
            val cache = getOrCreateCache(seed, facing, starPos)

            poseStack.withPose {
                translate(starPos.x, starPos.y, starPos.z)

                val rotation = cache.starRotation

                RenderUtils.renderStarLayer(
                    this,
                    buffer,
                    STAR_LAYER,
                    0.20f,
                    rotation.axis,
                    rotation.getAngle(tick),
                    FastColor.ARGB32.color(255, 255, 255, 255),
                    RenderType.solid()
                )
            }
        }

        private fun renderBlackHole(
            poseStack: PoseStack,
            buffer: MultiBufferSource,
            facing: Direction,
            starPos: Vec3,
            tick: Float,
            seed: Long
        ) {
            val cache = getOrCreateCache(seed, facing, starPos)

            poseStack.withPose {
                translate(starPos.x, starPos.y, starPos.z)

                val rotation = cache.starRotation

                RenderUtils.renderHaloLayer(
                    this,
                    buffer,
                    0.20f * 1.02f,
                    rotation.axis,
                    rotation.getAngle(tick),
                    HALO_TEX,
                    SPACE_MODEL
                )

                RenderUtils.renderStarLayer(
                    this,
                    buffer,
                    SPACE_MODEL,
                    0.20f,
                    rotation.axis,
                    rotation.getAngle(tick),
                    FastColor.ARGB32.color(255, 255, 255, 255),
                    RenderType.solid()
                )
            }
        }

        private fun renderOrbit(
            poseStack: PoseStack,
            buffer: MultiBufferSource,
            facing: Direction,
            starPos: Vec3,
            tick: Float,
            seed: Long
        ) {
            val cache = getOrCreateCache(seed, facing, starPos)

            for (i in ORBIT_OBJECTS.indices) {
                val motionParams = cache.orbitParams[i]
                RenderUtils.renderOrbitRing(poseStack, buffer, motionParams, 128)
            }

            val planetConsumer = buffer.getBuffer(RenderType.solid())

            for (i in ORBIT_OBJECTS.indices) {
                val motionParams = cache.orbitParams[i]
                val rotationParams = cache.planetRotations[i]
                val planetSize = cache.planetSizes[i]

                val rotation = Quaternionf().fromAxisAngleDeg(
                    rotationParams.axis.x,
                    rotationParams.axis.y,
                    rotationParams.axis.z,
                    rotationParams.getAngle(tick)
                )

                RenderUtils.renderCircularMotionModelDirect(
                    poseStack,
                    planetConsumer,
                    ClientUtil.getBakedModel(ORBIT_OBJECTS[i]),
                    motionParams,
                    tick,
                    planetSize,
                    FastColor.ARGB32.color(255, 255, 255, 255),
                    RenderType.solid(),
                    true,
                    rotation,
                    true
                )
            }
        }

        @Suppress("SameParameterValue")
        private fun getPlanetSize(seed: Long, index: Int, minSize: Float, maxSize: Float): Float {
            val sizeRandom = Random(seed + index * 500L)
            return minSize + sizeRandom.nextFloat() * (maxSize - minSize)
        }

        @Suppress("SameParameterValue")
        private fun getCircularMotionParams(
            seed: Long,
            i: Int,
            centerPos: Vec3,
            minRadius: Float,
            maxRadius: Float,
            minTiltAngle: Float,
            maxTiltAngle: Float
        ): CircularMotionParams {
            val objRandom = Random(seed + i * 1000L)

            val radius = minRadius + objRandom.nextFloat() * (maxRadius - minRadius)
            val speed = 0.5f + objRandom.nextFloat()
            val angleOffset = objRandom.nextFloat() * 360f
            val tiltAngle = minTiltAngle + objRandom.nextFloat() * (maxTiltAngle - minTiltAngle)
            val thetaTilt = objRandom.nextFloat() * 2.0f * PI.toFloat()
            val tiltDirection = Vector3f(cos(thetaTilt), 0.0f, sin(thetaTilt))

            return CircularMotionParams(centerPos, radius, speed, angleOffset, tiltAngle, tiltDirection)
        }

        private fun createCacheKey(seed: Long, facing: Direction): Long =
            (seed and 0x1FFFFFFFFFFFFFFFL) or (facing.ordinal.toLong() shl 61)

        private fun getOrCreateCache(seed: Long, facing: Direction, centerPos: Vec3): RenderCache {
            val key = createCacheKey(seed, facing)
            return CACHE_MAP.computeIfAbsent(key) { RenderCache(seed, centerPos) }
        }
    }
}