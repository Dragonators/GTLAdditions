package com.gtladd.gtladditions.client.render.machine.antichrist

import com.gtladd.gtladditions.GTLAdditions
import net.irisshaders.iris.pipeline.programs.ShaderKey
import net.irisshaders.iris.shaderpack.programs.ProgramFallbackResolver
import net.irisshaders.iris.shaderpack.programs.ProgramSource
import net.minecraft.client.renderer.ShaderInstance
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import java.io.IOException
import java.util.Optional

@OnlyIn(Dist.CLIENT)
class RingBatchShaderProgram {
    private var shader: ShaderInstance? = null
    private var failed = false

    fun getOrCreate(pipeline: Any, resolver: ProgramFallbackResolver, factory: Factory): ShaderInstance? {
        if (shader != null || failed) return shader
        try {
            val key = ShaderKey.TERRAIN_TRANSLUCENT
            val source = resolver.resolve(key.program)
            if (source.isEmpty) {
                failed = true
                return null
            }

            val name = RingBatchShaderSources.PREFIX + Integer.toHexString(System.identityHashCode(pipeline)) + "_water"
            // Native creation retains samplers, uniforms, blend settings and both gbuffer targets.
            // It also registers the variant in loadedShaders for pipeline-owned destruction.
            shader = factory.create(name, source, key)
        } catch (error: IOException) {
            onFailure(error)
        } catch (error: RuntimeException) {
            onFailure(error)
        }

        return shader
    }

    private fun onFailure(error: Exception) {
        failed = true
        GTLAdditions.LOGGER.warn("FOA batched glass program could not be created; using cached material groups", error)
    }

    fun interface Factory {
        @Throws(IOException::class)
        fun create(name: String, source: Optional<ProgramSource>, key: ShaderKey): ShaderInstance
    }
}
