package com.gtladd.gtladditions.api.client.scene

import com.gtladd.gtladditions.client.render.scene.SceneScheduler
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

/** Client-only entry point to the scene scheduler; call it on the render thread. Described in dev/scene-api-example/API.md. */
@OnlyIn(Dist.CLIENT)
object SceneRendering {
    @JvmField
    val API_VERSION: Int = 1

    /** Registers a provider once for the client's lifetime. */
    @JvmStatic
    fun register(id: ResourceLocation, provider: SceneProvider): ProviderHandle = SceneScheduler.register(id, provider)

    @JvmStatic
    fun register(id: ResourceLocation): ProviderHandle = SceneScheduler.register(id, null)

    /** The main-view frame while it accepts submissions, else null. */
    @JvmStatic
    fun frame(): SceneFrame? = SceneScheduler.openFrame()

    @JvmStatic
    fun capabilities(): SceneCapabilities = SceneScheduler.capabilities

    /** The part's outcome in the last frame that ended. */
    @JvmStatic
    fun lastOutcome(key: PartKey): PartOutcome? = SceneScheduler.lastOutcome(key)
}

class ProviderHandle internal constructor(val id: ResourceLocation, internal val provider: SceneProvider?) {
    override fun toString(): String = id.toString()
}

data class PartKey(val provider: ProviderHandle, val instance: Long, val part: Int) {
    companion object {
        @JvmStatic
        fun block(provider: ProviderHandle, pos: BlockPos, part: Int) = PartKey(provider, pos.asLong(), part)
    }
}
