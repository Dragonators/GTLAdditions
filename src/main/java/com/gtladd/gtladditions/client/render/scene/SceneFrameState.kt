package com.gtladd.gtladditions.client.render.scene

import com.gtladd.gtladditions.api.client.scene.CompositionPoint
import com.gtladd.gtladditions.api.client.scene.PartFeature
import com.gtladd.gtladditions.api.client.scene.PartKey
import com.gtladd.gtladditions.api.client.scene.PartOutcome
import com.gtladd.gtladditions.api.client.scene.SceneFrame
import com.gtladd.gtladditions.api.client.scene.ScenePart
import com.gtladd.gtladditions.api.client.scene.SubmitResult
import net.irisshaders.iris.shadows.ShadowRenderingState
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f

/** Values are copies taken at AFTER_SKY; later stages never change them. */
internal class SceneFrameState(
    override val id: Long,
    val level: ClientLevel,
    camera: Vec3,
    override val modelView: Matrix4f,
    override val projection: Matrix4f,
    override val viewportWidth: Int,
    override val viewportHeight: Int,
    override val partialTick: Float,
    override val renderTicks: Double,
    override val shaderpackActive: Boolean,
    val fabulous: Boolean,
    override val resourceGeneration: Int,
    override val pipelineGeneration: Int
) : SceneFrame {
    override val dimension: ResourceKey<Level> = level.dimension()
    override val cameraX = camera.x
    override val cameraY = camera.y
    override val cameraZ = camera.z

    /** Parts are culled with the matrices they are drawn with: vanilla's event frustum leaves out the hurt and view bob. */
    val drawingFrustum = Frustum(modelView, projection).apply { prepare(cameraX, cameraY, cameraZ) }
    override val composition = if (shaderpackActive) CompositionPoint.AFTER_SHADERPACK_FINAL else CompositionPoint.BEFORE_WEATHER

    val parts = LinkedHashMap<PartKey, ScenePart<*>>()
    var accepting = true

    var visible: List<ScenePart<*>> = emptyList()

    var needsDepth = false
    var needsHandMask = false

    var handStencil = false

    val outcomes = HashMap<PartKey, PartOutcome>()

    override fun submit(part: ScenePart<*>): SubmitResult = when {
        // The Oculus shadow pass also runs block entity renderers.
        ShadowRenderingState.areShadowsCurrentlyBeingRendered() -> SubmitResult.WRONG_VIEW
        !accepting -> SubmitResult.LATE
        !part.bounds.valid -> SubmitResult.INVALID_BOUNDS
        PartFeature.WRITES_DEPTH in part.features -> SubmitResult.UNSUPPORTED
        parts.putIfAbsent(part.key, part) != null -> SubmitResult.DUPLICATE
        else -> SubmitResult.ACCEPTED
    }
}
