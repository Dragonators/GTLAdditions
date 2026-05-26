package com.gtladd.gtladditions.client.render.machine

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.client.renderer.machine.IControllerRenderer
import com.gregtechceu.gtceu.common.data.GTBlocks.HIGH_POWER_CASING
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.common.machine.multiblock.controller.ArcanicAstrograph
import com.gtladd.gtladditions.utils.Constants.ORBIT_OBJECTS
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.model.data.ModelData
import org.gtlcore.gtlcore.GTLCore
import org.gtlcore.gtlcore.client.ClientUtil
import org.gtlcore.gtlcore.utils.RenderUtil
import org.joml.Quaternionf
import java.util.function.Consumer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.sin
import kotlin.math.sqrt

class ArcanicAstrographRender :
    PartWorkableCasingMachineRenderer(GTLCore.id("block/create_casing"), GTCEu.id("block/multiblock/cosmos_simulation"), HIGH_POWER_CASING, GTCEu.id("block/casings/hpca/high_power_casing")),
    IControllerRenderer {

    companion object {
        private const val CENTER = 0.5
        private const val FRONT_OFFSET = 16.5
        private const val BACK_OFFSET = -15.5
        private const val SPACE_SHELL_SCALE = 0.175f
        private const val STAR_SCALE = 0.02f
        private const val BASE_PARALLEL_AMOUNT = 2048
        private const val PARALLEL_AMOUNT_STEP = 128.0
        private const val MIN_ORBIT_COUNT = 3
        private const val MAX_ORBIT_COUNT = 13
        private const val FIRST_PARALLEL_EXPONENT = 3
        private const val MAX_PARALLEL_EXPONENT = 24
        private const val MAX_ORBIT_RADIUS = 10.304
        private const val ORBIT_RADIUS_JITTER = 0.12
        private const val ORBIT_SPEED_MULTIPLIER = 2.5f
        private const val MIN_PLANET_SCALE = 0.4f
        private const val PLANET_SCALE_STEP = 0.06f
        private const val GOLDEN_ANGLE = 2.399963229728653

        private val SPACE_MODEL = GTLCore.id("obj/space")
        private val STAR_MODEL: ResourceLocation = GTLCore.id("obj/star")
        private val FIXED_ORBIT_RADII = doubleArrayOf(2.8, 3.75, 4.7)
        private val RANDOM_ORBIT_CACHE = mutableMapOf<Long, List<RandomOrbitParams>>()

        private data class RandomOrbitParams(
            val radius: Double,
            val orbitSpeed: Float,
            val angleOffset: Double,
            val rotationSpeed: Float,
            val rotationAxisX: Float,
            val rotationAxisY: Float,
            val rotationAxisZ: Float,
            val planeUx: Double,
            val planeUy: Double,
            val planeUz: Double,
            val planeVx: Double,
            val planeVy: Double,
            val planeVz: Double
        )

        private fun getRenderOrbitCount(parallelAmount: Int): Int {
            if (parallelAmount <= BASE_PARALLEL_AMOUNT) return MIN_ORBIT_COUNT
            if (parallelAmount >= Int.MAX_VALUE) return MAX_ORBIT_COUNT

            val exponent = floor(log2((parallelAmount - BASE_PARALLEL_AMOUNT) / PARALLEL_AMOUNT_STEP))
                .toInt()
                .coerceAtMost(MAX_PARALLEL_EXPONENT)
            val exponentProgress = (exponent - FIRST_PARALLEL_EXPONENT)
                .coerceAtLeast(0)
                .toDouble() / (MAX_PARALLEL_EXPONENT - FIRST_PARALLEL_EXPONENT).toDouble()

            return MIN_ORBIT_COUNT + floor(exponentProgress * (MAX_ORBIT_COUNT - MIN_ORBIT_COUNT)).toInt()
        }

        private fun getRandomOrbitParams(seed: Long): List<RandomOrbitParams> =
            RANDOM_ORBIT_CACHE.getOrPut(seed) {
                List(ORBIT_OBJECTS.size) { index ->
                    val random = RandomSource.create(seed + index * 1000L)
                    val radius = getOrbitRadius(index, random)
                    val (planeU, planeV) = createRandomOrbitPlane(random, index)

                    RandomOrbitParams(
                        radius = radius,
                        orbitSpeed = (0.5f + random.nextFloat()) * ORBIT_SPEED_MULTIPLIER,
                        angleOffset = random.nextDouble() * 2.0 * PI,
                        rotationSpeed = 0.5f + random.nextFloat() * 1.5f,
                        rotationAxisX = random.nextFloat(),
                        rotationAxisY = random.nextFloat(),
                        rotationAxisZ = random.nextFloat(),
                        planeUx = planeU.x,
                        planeUy = planeU.y,
                        planeUz = planeU.z,
                        planeVx = planeV.x,
                        planeVy = planeV.y,
                        planeVz = planeV.z
                    )
                }
            }

        private fun createRandomOrbitPlane(random: RandomSource, index: Int): Pair<Vec3, Vec3> {
            val normal = createOrbitNormal(random, index)
            val reference = if (kotlin.math.abs(normal.y) < 0.85) {
                Vec3(0.0, 1.0, 0.0)
            } else {
                Vec3(1.0, 0.0, 0.0)
            }
            val u = normal.cross(reference).normalize()
            val v = normal.cross(u).normalize()
            return u to v
        }

        private fun getOrbitRadius(index: Int, random: RandomSource): Double {
            if (index < FIXED_ORBIT_RADII.size) return FIXED_ORBIT_RADII[index]

            val radiusStep = (MAX_ORBIT_RADIUS - FIXED_ORBIT_RADII.last()) /
                (ORBIT_OBJECTS.lastIndex - FIXED_ORBIT_RADII.lastIndex)
            val baseRadius = FIXED_ORBIT_RADII.last() + radiusStep * (index - FIXED_ORBIT_RADII.lastIndex)
            val jitter = (random.nextDouble() * 2.0 - 1.0) * ORBIT_RADIUS_JITTER
            return (baseRadius + jitter).coerceAtMost(MAX_ORBIT_RADIUS)
        }

        private fun createOrbitNormal(random: RandomSource, index: Int): Vec3 {
            val z = (index + 0.5) / ORBIT_OBJECTS.size
            val theta = index * GOLDEN_ANGLE + random.nextDouble() * 2.0 * PI
            val horizontal = sqrt(1.0 - z * z)
            return Vec3(horizontal * cos(theta), horizontal * sin(theta), z)
        }
    }

    @OnlyIn(Dist.CLIENT)
    override fun render(
        blockEntity: BlockEntity,
        partialTicks: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        combinedLight: Int,
        combinedOverlay: Int
    ) {
        if (blockEntity !is IMachineBlockEntity) return

        val machine = blockEntity.metaMachine as? ArcanicAstrograph ?: return
        if (!machine.isActive) return

        val tick = RenderUtil.getSmoothTick(machine, partialTicks)
        val seed = blockEntity.blockPos.asLong()
        val center = getRenderPosition(machine.frontFacing)

        poseStack.withPose {
            translate(center.x, center.y, center.z)
            renderStar(tick, buffer)
            renderOrbitObjects(tick, buffer, getRenderOrbitCount(machine.parallelAmount), seed)
            renderOuterSpaceShell(buffer)
        }
    }

    @OnlyIn(Dist.CLIENT)
    private fun getRenderPosition(frontFacing: Direction): Vec3 = when (frontFacing) {
        Direction.NORTH -> Vec3(CENTER, CENTER, FRONT_OFFSET)
        Direction.SOUTH -> Vec3(CENTER, CENTER, BACK_OFFSET)
        Direction.WEST -> Vec3(FRONT_OFFSET, CENTER, CENTER)
        Direction.EAST -> Vec3(BACK_OFFSET, CENTER, CENTER)
        else -> Vec3(CENTER, CENTER, CENTER)
    }

    @OnlyIn(Dist.CLIENT)
    private fun PoseStack.renderStar(tick: Float, buffer: MultiBufferSource) {
        withPose {
            scale(STAR_SCALE, STAR_SCALE, STAR_SCALE)
            mulPose(Quaternionf().fromAxisAngleDeg(0f, 1f, 1f, (tick / 2) % 360f))
            val renderType = RenderType.translucent()
            ClientUtil.modelRenderer().renderModel(
                last(),
                buffer.getBuffer(renderType),
                null,
                ClientUtil.getBakedModel(STAR_MODEL),
                1.0f,
                1.0f,
                1.0f,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY,
                renderType
            )
        }
    }

    @OnlyIn(Dist.CLIENT)
    private fun PoseStack.renderOrbitObjects(
        tick: Float,
        buffer: MultiBufferSource,
        orbitCount: Int,
        seed: Long
    ) {
        val randomOrbitParams = getRandomOrbitParams(seed)

        ORBIT_OBJECTS
            .take(orbitCount.coerceIn(MIN_ORBIT_COUNT, ORBIT_OBJECTS.size))
            .forEachIndexed { index, model ->
                val planetScale = MIN_PLANET_SCALE + PLANET_SCALE_STEP * index

                withPose {
                    translateRandomOrbit(tick, randomOrbitParams[index])
                    scale(planetScale, planetScale, planetScale)
                    applyPlanetRotation(tick, randomOrbitParams[index])
                    val renderType = RenderType.solid()
                    ClientUtil.modelRenderer().renderModel(
                        last(),
                        buffer.getBuffer(renderType),
                        null,
                        ClientUtil.getBakedModel(model),
                        1.0f,
                        1.0f,
                        1.0f,
                        LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY,
                        ModelData.EMPTY,
                        renderType
                    )
                }
            }
    }

    @OnlyIn(Dist.CLIENT)
    private fun PoseStack.translateRandomOrbit(tick: Float, params: RandomOrbitParams) {
        val angle = tick * params.orbitSpeed / 80 + params.angleOffset
        val sinAngle = sin(angle)
        val cosAngle = cos(angle)
        translate(
            params.radius * (params.planeUx * sinAngle + params.planeVx * cosAngle),
            params.radius * (params.planeUy * sinAngle + params.planeVy * cosAngle),
            params.radius * (params.planeUz * sinAngle + params.planeVz * cosAngle)
        )
    }

    @OnlyIn(Dist.CLIENT)
    private fun PoseStack.applyPlanetRotation(tick: Float, params: RandomOrbitParams) {
        mulPose(
            Quaternionf().fromAxisAngleDeg(
                params.rotationAxisX,
                params.rotationAxisY,
                params.rotationAxisZ,
                (tick * params.rotationSpeed) % 360f
            )
        )
    }

    @OnlyIn(Dist.CLIENT)
    private fun PoseStack.renderOuterSpaceShell(buffer: MultiBufferSource) {
        withPose {
            scale(SPACE_SHELL_SCALE, SPACE_SHELL_SCALE, SPACE_SHELL_SCALE)
            val renderType = RenderType.solid()
            ClientUtil.modelRenderer().renderModel(
                last(),
                buffer.getBuffer(renderType),
                null,
                ClientUtil.getBakedModel(SPACE_MODEL),
                1.0f,
                1.0f,
                1.0f,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY,
                renderType
            )
        }
    }

    override fun onAdditionalModel(registry: Consumer<ResourceLocation>) {
        super.onAdditionalModel(registry)
        registry.accept(SPACE_MODEL)
        registry.accept(STAR_MODEL)
        ORBIT_OBJECTS.forEach(registry)
    }

    @OnlyIn(Dist.CLIENT)
    override fun hasTESR(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun isGlobalRenderer(blockEntity: BlockEntity): Boolean = true

    @OnlyIn(Dist.CLIENT)
    override fun getViewDistance(): Int = 128
}