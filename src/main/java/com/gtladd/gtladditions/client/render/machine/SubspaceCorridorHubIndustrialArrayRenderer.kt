package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.client.renderer.GTRenderTypes
import com.gregtechceu.gtceu.client.renderer.machine.WorkableCasingMachineRenderer
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.data.CircularMotionParams
import com.gtladd.gtladditions.common.data.RotationParams
import com.gtladd.gtladditions.common.machine.muiltblock.controller.SubspaceCorridorHubIndustrialArray
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.vertex.PoseStack
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FastColor
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.model.data.ModelData
import org.gtlcore.gtlcore.GTLCore
import org.gtlcore.gtlcore.client.ClientUtil
import org.gtlcore.gtlcore.client.renderer.RenderBufferHelper
import org.gtlcore.gtlcore.utils.RenderUtil
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import kotlin.math.cos
import kotlin.math.sin

@Suppress("SameParameterValue")
class SubspaceCorridorHubIndustrialArrayRenderer : WorkableCasingMachineRenderer(
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
            val machine = blockEntity.metaMachine as? SubspaceCorridorHubIndustrialArray ?: return
            if (machine.recipeLogic.isWorking) {
                val tick = RenderUtil.getSmoothTick(machine, partialTicks)
                val seed = blockEntity.blockPos.asLong()

                val (x, y, z) = when (machine.frontFacing) {
                    Direction.NORTH -> Triple(0.5, -94.5, 105.5)
                    Direction.SOUTH -> Triple(0.5, -94.5, -104.5)
                    Direction.WEST -> Triple(105.5, -94.5, 0.5)
                    Direction.EAST -> Triple(-104.5, -94.5, 0.5)
                    else -> Triple(0.5, -94.5, 0.5)
                }

                val cache = getOrCreateCache(seed, machine.frontFacing, Vec3(x, y, z))

                renderBeamCylinders(poseStack, buffer, machine.frontFacing, tick)

                RenderUtils.drawBeaconToSky(
                    poseStack, buffer, x, y - 36, z,
                    FastColor.ARGB32.color(255, 255, 255, 255),
                    tick, blockEntity, 2.9f
                )

                renderStar(tick, poseStack, buffer, cache, x, y, z)

                renderOrbit(poseStack, buffer, cache, tick)
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    override fun onAdditionalModel(registry: Consumer<ResourceLocation>) {
        super.onAdditionalModel(registry)
        registry.accept(STAR_LAYER)
        registry.accept(CLIMBER_MODEL)
        ORBIT_OBJECTS.forEach(registry)
    }

    @OnlyIn(Dist.CLIENT)
    override fun hasTESR(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun isGlobalRenderer(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun getViewDistance(): Int = 384

    @OnlyIn(Dist.CLIENT)
    private class RenderCache(seed: Long, centerPos: Vec3) {
        val planetRotations: MutableList<RotationParams> = ObjectArrayList(13)
        val orbitParams: MutableList<CircularMotionParams> = ObjectArrayList(13)
        val planetSizes: MutableList<Float> = ObjectArrayList(13)
        val starRotation: RotationParams

        init {
            for (i in ORBIT_OBJECTS.indices) {
                planetRotations.add(
                    RenderUtils.createRandomRotation(
                        RandomSource.create(seed + i * 2000L), 0.1f, 0.5f
                    )
                )
                orbitParams.add(getCircularMotionParams(seed, i, centerPos, 27.0f, 55.0f, 0f, 30f))
                planetSizes.add(getPlanetSize(seed, i, 1.3f, 2.7f))
            }

            starRotation = RenderUtils.createRandomRotation(RandomSource.create(seed), 0.5f, 2.0f)
        }
    }

    @OnlyIn(Dist.CLIENT)
    companion object {
        private val STAR_LAYER: ResourceLocation = GTLAdditions.id("obj/star_layer_1")
        private val CLIMBER_MODEL: ResourceLocation = GTLCore.id("obj/climber")
        private val ORBIT_OBJECTS: List<ResourceLocation> = listOf(
            GTLAdditions.id("obj/planets/the_nether"),
            GTLAdditions.id("obj/planets/overworld"),
            GTLAdditions.id("obj/planets/the_end"),
            GTLAdditions.id("obj/planets/ceres"),
            GTLAdditions.id("obj/planets/enceladus"),
            GTLAdditions.id("obj/planets/ganymede"),
            GTLAdditions.id("obj/planets/io"),
            GTLAdditions.id("obj/planets/mars"),
            GTLAdditions.id("obj/planets/mercury"),
            GTLAdditions.id("obj/planets/moon"),
            GTLAdditions.id("obj/planets/pluto"),
            GTLAdditions.id("obj/planets/titan"),
            GTLAdditions.id("obj/planets/venus")
        )
        private val BEAM_CYLINDER_OFFSETS = arrayOf(
            intArrayOf(-24, -109, -30),
            intArrayOf(-120, -109, -83),
            intArrayOf(-171, -109, -53),
            intArrayOf(-171, -109, 53),
            intArrayOf(-121, -109, 83),
            intArrayOf(-24, -109, 31)
        )

        private val CACHE_MAP: ConcurrentHashMap<Long, RenderCache> = ConcurrentHashMap()

        private fun renderBeamCylinders(
            poseStack: PoseStack,
            buffer: MultiBufferSource,
            facing: Direction,
            tick: Float
        ) {
            for (offset in BEAM_CYLINDER_OFFSETS) {
                poseStack.pushPose()

                val (x, z) = when (facing) {
                    Direction.NORTH -> (0.5 - offset[2]) to (0.5 - offset[0])
                    Direction.SOUTH -> (0.5 + offset[2]) to (0.5 + offset[0])
                    Direction.WEST -> (0.5 - offset[0]) to (0.5 - offset[2])
                    else -> (0.5 + offset[0]) to (0.5 + offset[2])
                }
                val y = 0.5 + offset[1]

                RenderBufferHelper.renderCylinder(
                    poseStack,
                    buffer.getBuffer(GTRenderTypes.getLightRing()),
                    x.toFloat(), y.toFloat(), z.toFloat(),
                    0.3f,
                    400f,
                    20,
                    255f, 255f, 255f, 255f
                )

                val positionSeed = BlockPos(offset[0], offset[1], offset[2]).asLong()
                val random = RandomSource.create(positionSeed)

                val tickOffset = random.nextFloat() * 320.0f
                val offsetTick = tick + tickOffset

                val climberY = y + 180 + (140 * sin(offsetTick / 160.0))
                poseStack.translate(x, climberY, z)

                renderClimber(poseStack, buffer)
                poseStack.popPose()
            }
        }

        private fun renderClimber(poseStack: PoseStack, buffer: MultiBufferSource) {
            poseStack.pushPose()
            poseStack.scale(4f, 4f, 4f)
            ClientUtil.modelRenderer().renderModel(
                poseStack.last(),
                buffer.getBuffer(RenderType.solid()),
                null,
                ClientUtil.getBakedModel(CLIMBER_MODEL),
                1.0f, 1.0f, 1.0f,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY,
                RenderType.solid()
            )
            poseStack.popPose()
        }

        private fun renderStar(
            tick: Float,
            poseStack: PoseStack,
            buffer: MultiBufferSource,
            cache: RenderCache,
            x: Double,
            y: Double,
            z: Double
        ) {
            poseStack.pushPose()
            poseStack.translate(x, y, z)

            val rotation = cache.starRotation

            RenderUtils.renderStarLayer(
                poseStack, buffer, STAR_LAYER, 0.23f,
                rotation.axis, rotation.getAngle(tick),
                FastColor.ARGB32.color(255, 255, 255, 255),
                RenderType.solid()
            )

            poseStack.popPose()
        }

        private fun renderOrbit(
            poseStack: PoseStack,
            buffer: MultiBufferSource,
            cache: RenderCache,
            tick: Float
        ) {
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

        private fun getPlanetSize(seed: Long, index: Int, minSize: Float, maxSize: Float): Float {
            val sizeRandom = Random(seed + index * 500L)
            return minSize + sizeRandom.nextFloat() * (maxSize - minSize)
        }

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
            val thetaTilt = objRandom.nextFloat() * 2.0f * Math.PI.toFloat()
            val tiltDirection = Vector3f(
                cos(thetaTilt.toDouble()).toFloat(),
                0.0f,
                sin(thetaTilt.toDouble()).toFloat()
            )

            return CircularMotionParams(
                centerPos,
                radius,
                speed,
                angleOffset,
                tiltAngle,
                tiltDirection
            )
        }

        private fun createCacheKey(seed: Long, facing: Direction): Long {
            return (seed and 0x1FFFFFFFFFFFFFFFL) or (facing.ordinal.toLong() shl 61)
        }

        private fun getOrCreateCache(seed: Long, facing: Direction, centerPos: Vec3): RenderCache {
            val key = createCacheKey(seed, facing)
            return CACHE_MAP.computeIfAbsent(key) { RenderCache(seed, centerPos) }
        }
    }
}