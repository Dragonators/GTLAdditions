package com.gtladd.gtladditions.client.render.machine

import com.github.benmanes.caffeine.cache.Caffeine
import com.gtladd.gtladditions.GTLAdditions
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.RandomSource
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.event.level.LevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.joml.Quaternionf
import java.util.function.Consumer
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.sin

@OnlyIn(Dist.CLIENT)
internal object ArcanicAstrographOrbits {
    const val MIN_COUNT = 3
    const val MAX_COUNT = 10
    private const val CACHE_CAPACITY = 512L
    private const val BASE_PARALLEL_AMOUNT = 2048
    private const val PARALLEL_AMOUNT_STEP = 128.0
    private const val FIRST_PARALLEL_EXPONENT = 3
    private const val MAX_PARALLEL_EXPONENT = 24
    private const val SPEED_SCALE = 0.1
    private const val PHASE_STEP_DEGREES = 137.50776405003785

    private val models = listOf(
        "ne", "tf", "eg", "mo", "de", "ma", "ph", "as", "ca", "ce",
        "eu", "ga", "rb", "io", "me", "ve", "en", "mi", "ob", "ti",
        "ra", "pr", "tr", "ha", "kb", "mm", "pl", "bc", "be", "bf",
        "cb", "te", "vb", "an", "ho", "mh", "mb", "np", "se", "dd"
    ).map { GTLAdditions.id("effect/astrograph/planets/$it") }

    private val layouts = Caffeine.newBuilder()
        .maximumSize(CACHE_CAPACITY)
        .build<Long, List<Orbit>> { seed -> generate(seed) }

    data class Orbit(
        val model: ResourceLocation,
        val radius: Double,
        val scale: Float,
        val orbitSpeed: Float,
        val phaseDegrees: Double,
        val rotationSpeed: Float,
        val xAngle: Float,
        val zAngle: Float
    ) {
        private val tilt = rotation(zAngle.toDouble(), 0f, 0f, 1f)
            .mul(rotation(xAngle.toDouble(), 1f, 0f, 0f))

        fun applyTransform(pose: PoseStack, tick: Float, scratch: Quaternionf) {
            val orbitAngle = (tick.toDouble() * orbitSpeed * SPEED_SCALE + phaseDegrees) % 360.0
            val spinAngle = tick.toDouble() * rotationSpeed * SPEED_SCALE % 360.0
            pose.mulPose(tilt)
            pose.mulPose(yRotation(orbitAngle, scratch))
            pose.translate(-radius, 0.0, 0.0)
            pose.mulPose(yRotation(spinAngle, scratch))
            pose.scale(scale, scale, scale)
            pose.translate(-0.5, -0.5, -0.5)
        }
    }

    fun visibleCount(parallelAmount: Int): Int {
        if (parallelAmount <= BASE_PARALLEL_AMOUNT) return MIN_COUNT
        if (parallelAmount == Int.MAX_VALUE) return MAX_COUNT
        val exponent = floor(log2((parallelAmount - BASE_PARALLEL_AMOUNT) / PARALLEL_AMOUNT_STEP))
            .toInt().coerceAtMost(MAX_PARALLEL_EXPONENT)
        val progress = (exponent - FIRST_PARALLEL_EXPONENT).coerceAtLeast(0).toDouble() /
            (MAX_PARALLEL_EXPONENT - FIRST_PARALLEL_EXPONENT)
        return MIN_COUNT + floor(progress * (MAX_COUNT - MIN_COUNT)).toInt()
    }

    fun get(seed: Long): List<Orbit> = layouts.get(seed)

    fun clearCache() = layouts.invalidateAll()

    fun registerModels(registry: Consumer<ResourceLocation>) = models.forEach(registry)

    private fun generate(seed: Long): List<Orbit> {
        val random = RandomSource.create(seed)
        val selectedModels = models.toMutableList()

        for (index in selectedModels.lastIndex downTo 1) {
            val other = random.nextInt(index + 1)
            val previous = selectedModels[index]
            selectedModels[index] = selectedModels[other]
            selectedModels[other] = previous
        }

        val phaseOffset = RandomSource.create(seed xor 0x6A09E667F3BCC909L).nextDouble() * 360.0
        return List(MAX_COUNT) { index ->
            val xAngle = (random.nextFloat() * 2f - 1f) * 30f
            val zAngle = (random.nextFloat() * 2f - 1f) * 30f
            val distance = index + 2.0 + (random.nextDouble() * 2.0 - 1.0) * 0.2
            val scale = 0.2f + random.nextFloat() * (0.9f - 0.2f)
            val rotationSpeed = 0.5f + random.nextFloat()
            val orbitSpeed = 0.5f + random.nextFloat()
            Orbit(
                selectedModels[index],
                0.2 + distance + 0.2 * ArcanicAstrographStarRenderer.RADIUS,
                scale,
                orbitSpeed,
                (phaseOffset + index * PHASE_STEP_DEGREES) % 360.0,
                rotationSpeed,
                xAngle,
                zAngle
            )
        }
    }

    private fun rotation(degrees: Double, x: Float, y: Float, z: Float): Quaternionf {
        val halfAngle = Math.toRadians(degrees) * 0.5
        val sine = sin(halfAngle).toFloat()
        return Quaternionf(x * sine, y * sine, z * sine, cos(halfAngle).toFloat())
    }

    private fun yRotation(degrees: Double, result: Quaternionf): Quaternionf {
        val halfAngle = Math.toRadians(degrees) * 0.5
        return result.set(0f, sin(halfAngle).toFloat(), 0f, cos(halfAngle).toFloat())
    }
}

@Suppress("unused")
@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, value = [Dist.CLIENT])
object ArcanicAstrographClientEvents {
    @SubscribeEvent
    @JvmStatic
    fun onLevelUnload(event: LevelEvent.Unload) {
        if (event.level.isClientSide) clear()
    }

    @SubscribeEvent
    @JvmStatic
    fun onLogout(event: ClientPlayerNetworkEvent.LoggingOut) = clear()

    private fun clear() {
        if (RenderSystem.isOnRenderThread()) {
            ArcanicAstrographOrbits.clearCache()
            ArcanicAstrographStarRenderer.releaseBuffers()
        } else {
            RenderSystem.recordRenderCall { clear() }
        }
    }
}