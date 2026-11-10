package com.gtladd.gtladditions.api.client.scene

/** A provider's optional callbacks, on the render thread. */
interface SceneProvider {
    /** Called just before submissions close. */
    fun collect(frame: SceneFrame) {}

    fun onResourcesReloaded(generation: Int) {}

    /** A shaderpack pipeline was created or destroyed. */
    fun onPipelineChanged(generation: Int) {}

    /** A client level was unloaded. */
    fun onLevelChanged() {}
}

interface SceneCapabilities {
    /** False for external providers in v1. */
    val layeredVolumes: Boolean

    /** The scheduler supports a shared hand stencil under a shaderpack; not the state of a frame. */
    val handStencil: Boolean

    /** Major * 10 + minor. */
    val glVersion: Int
    val computeShaders: Boolean
}

enum class SubmitResult {
    ACCEPTED,

    /** The shadow pass. */
    WRONG_VIEW,

    /** The submission window has closed. */
    LATE,

    /** The frame already holds a part with this key. */
    DUPLICATE,

    /** The part declares [PartFeature.WRITES_DEPTH]. */
    UNSUPPORTED,
    INVALID_BOUNDS
}

enum class PartOutcome {
    DRAWN,

    /** Internal renderers only. */
    DRAWN_LAYERED,

    /** Drawn in an approximate order where it overlaps a part it does not commute with. */
    DRAWN_UNORDERED,

    CULLED,

    /** Its resources are being published. */
    PENDING,
    RESOURCE_UNAVAILABLE,

    FAILED,

    /** Accepted by a frame that ended without drawing or culling it. */
    DROPPED
}