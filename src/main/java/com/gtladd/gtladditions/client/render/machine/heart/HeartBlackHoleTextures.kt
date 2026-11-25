package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.client.render.scene.ResourceSlot
import com.mojang.blaze3d.platform.GlStateManager
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceProvider
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller

internal object HeartBlackHoleTextures {
    private val slot = ResourceSlot<Loaded>("Heart of the Universe textures")

    val pending: Boolean
        get() = slot.state == ResourceSlot.State.EMPTY || slot.state == ResourceSlot.State.PENDING

    fun invalidate() = slot.invalidate()

    private fun read(resources: ResourceProvider) =
        Prepared(HeartBlackHoleLookupTextures.readResources(resources), HeartBlackHoleAppearanceTextures.readResources(resources))

    private fun load(prepared: Prepared) {
        val created = ArrayList<Int>(4)
        val next = try {
            HeartBlackHoleRenderState.withSavedState {
                Loaded(
                    HeartBlackHoleLookupTextures.uploadTable(prepared.tables.deflection).also(created::add),
                    HeartBlackHoleLookupTextures.uploadTable(prepared.tables.inverseRadius).also(created::add),
                    HeartBlackHoleAppearanceTextures.uploadTexture(prepared.textures.noise).also(created::add),
                    HeartBlackHoleAppearanceTextures.uploadTexture(prepared.textures.galaxy).also(created::add)
                )
            }
        } catch (error: Exception) {
            created.forEach(GlStateManager::_deleteTexture)
            throw error
        }

        slot.publish(next)
    }

    fun setSamplers(shader: ShaderInstance): Boolean {
        val textures = slot.value ?: return false
        shader.setSampler("RayDeflectionSampler", textures.deflection)
        shader.setSampler("RayInverseRadiusSampler", textures.inverseRadius)
        shader.setSampler("DiscNoiseSampler", textures.noise)
        shader.setSampler("GalaxySampler", textures.galaxy)

        return true
    }

    fun setGalaxySampler(shader: ShaderInstance): Boolean {
        val textures = slot.value ?: return false
        shader.setSampler("GalaxySampler", textures.galaxy)
        return true
    }

    private class Prepared(
        val tables: HeartBlackHoleLookupTextures.PreparedTables,
        val textures: HeartBlackHoleAppearanceTextures.PreparedTextures
    )

    private class Loaded(val deflection: Int, val inverseRadius: Int, val noise: Int, val galaxy: Int) : AutoCloseable {
        override fun close() {
            for (texture in intArrayOf(deflection, inverseRadius, noise, galaxy)) GlStateManager._deleteTexture(texture)
        }
    }

    object Reload : SimplePreparableReloadListener<() -> Unit>() {
        override fun prepare(resources: ResourceManager, profiler: ProfilerFiller): () -> Unit {
            val prepared = try {
                read(resources)
            } catch (error: Exception) {
                return { throw error }
            }
            return { load(prepared) }
        }

        override fun apply(upload: () -> Unit, resources: ResourceManager, profiler: ProfilerFiller) {
            try {
                upload()
            } catch (error: Exception) {
                slot.fail(error)
            }
        }
    }
}
