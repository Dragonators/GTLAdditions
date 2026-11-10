package com.gtladd.gtladditions.api.client.scene

import net.minecraft.world.phys.AABB
import java.util.EnumSet

/** OPAQUE parts write depth into the main target; TRANSLUCENT parts are drawn in the scheduler's order. */
enum class PartPass { OPAQUE, TRANSLUCENT }

/** A program's own blend state must equal the declaration. */
enum class PartBlend {
    OPAQUE,

    /** ONE, ONE_MINUS_SRC_ALPHA. */
    PREMULTIPLIED,

    /** SRC_ALPHA, ONE_MINUS_SRC_ALPHA. */
    STRAIGHT,

    /** ONE, ONE. */
    ADDITIVE,

    /** ONE, ONE_MINUS_SRC_COLOR. */
    SCREEN
}

enum class PartFeature {
    /** No effect in v1. */
    INTERNAL_SORT,

    /** Samples [SceneDepth]. */
    SCENE_DEPTH,

    /** Discards on the first-person hand with [PartDrawContext.handMaskTexture]. */
    OWN_HAND_MASK,

    /** Unsupported in v1. */
    WRITES_DEPTH
}

/** Conservative world-space bounds made of convex pieces. */
class PartBounds private constructor(internal val pieces: List<Piece>) {
    internal sealed interface Piece

    internal class Box(val minX: Double, val minY: Double, val minZ: Double, val maxX: Double, val maxY: Double, val maxZ: Double) : Piece

    internal class Sphere(val x: Double, val y: Double, val z: Double, val radius: Double) : Piece

    internal val valid = pieces.isNotEmpty() &&
        pieces.all { piece ->
            when (piece) {
                is Box -> doubleArrayOf(piece.minX, piece.minY, piece.minZ, piece.maxX, piece.maxY, piece.maxZ).all(Double::isFinite)
                is Sphere -> doubleArrayOf(piece.x, piece.y, piece.z, piece.radius).all(Double::isFinite) && piece.radius >= 0
            }
        }

    companion object {
        @JvmStatic
        fun box(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double) = PartBounds(
            listOf(Box(minOf(minX, maxX), minOf(minY, maxY), minOf(minZ, maxZ), maxOf(minX, maxX), maxOf(minY, maxY), maxOf(minZ, maxZ)))
        )

        @JvmStatic
        fun box(box: AABB) = box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ)

        @JvmStatic
        fun sphere(x: Double, y: Double, z: Double, radius: Double) = PartBounds(listOf(Sphere(x, y, z, radius)))

        @JvmStatic
        fun union(vararg bounds: PartBounds) = PartBounds(bounds.flatMap { it.pieces })
    }
}

/** Immutable; [payload] stays unchanged until the frame ends. */
class ScenePart<P : Any> private constructor(
    val key: PartKey,
    val renderer: PartRenderer<P>,
    val payload: P,
    val pass: PartPass,
    val blend: PartBlend,
    val bounds: PartBounds,
    val features: Set<PartFeature>,
    val batchKey: Any?
) {
    class Builder<P : Any> internal constructor(private val key: PartKey, private val renderer: PartRenderer<P>, private val payload: P) {
        private var pass = PartPass.TRANSLUCENT
        private var blend: PartBlend? = null
        private var bounds: PartBounds? = null
        private var features: Set<PartFeature> = emptySet()
        private var batchKey: Any? = null

        fun pass(pass: PartPass) = apply { this.pass = pass }

        fun blend(blend: PartBlend) = apply { this.blend = blend }

        fun bounds(bounds: PartBounds) = apply { this.bounds = bounds }

        fun features(vararg features: PartFeature) = apply {
            this.features = if (features.isEmpty()) emptySet() else EnumSet.copyOf(features.asList())
        }

        /** Explicit equal keys group adjacent translucent parts of one renderer; OPAQUE parts are drawn one per call. */
        fun batchKey(key: Any) = apply { batchKey = key }

        fun build(): ScenePart<P> = ScenePart(
            key,
            renderer,
            payload,
            pass,
            if (pass == PartPass.OPAQUE) PartBlend.OPAQUE else checkNotNull(blend) { "A translucent part must declare its blend" },
            checkNotNull(bounds) { "A part must declare conservative bounds" },
            features,
            batchKey
        )
    }

    companion object {
        @JvmStatic
        fun <P : Any> builder(key: PartKey, renderer: PartRenderer<P>, payload: P) = Builder(key, renderer, payload)
    }
}