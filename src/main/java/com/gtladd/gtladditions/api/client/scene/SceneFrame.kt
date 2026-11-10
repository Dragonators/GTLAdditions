package com.gtladd.gtladditions.api.client.scene

import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import org.joml.Matrix4fc

/** One main-view frame, sampled once when it begins; valid for that frame only. */
interface SceneFrame {
    val id: Long
    val dimension: ResourceKey<Level>
    val cameraX: Double
    val cameraY: Double
    val cameraZ: Double

    /** Camera-relative world to view. */
    val modelView: Matrix4fc

    /** Unjittered. */
    val projection: Matrix4fc
    val viewportWidth: Int
    val viewportHeight: Int
    val partialTick: Float

    /** Pause-aware render clock, in ticks. */
    val renderTicks: Double

    val shaderpackActive: Boolean

    /** Grow on a resource reload and on a shaderpack pipeline change. */
    val resourceGeneration: Int
    val pipelineGeneration: Int

    val composition: CompositionPoint

    fun submit(part: ScenePart<*>): SubmitResult
}

enum class CompositionPoint { BEFORE_WEATHER, AFTER_SHADERPACK_FINAL }