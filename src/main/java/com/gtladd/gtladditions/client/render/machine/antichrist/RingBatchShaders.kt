package com.gtladd.gtladditions.client.render.machine.antichrist

import com.google.gson.JsonParser
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredIrisPipelineBridge
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.irisshaders.iris.Iris
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PathPackResources
import net.minecraft.server.packs.resources.Resource
import net.minecraft.server.packs.resources.ResourceProvider
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.lwjgl.opengl.GL31C
import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.util.Optional

@OnlyIn(Dist.CLIENT)
object RingBatchShaders {
    private val vanillaId = GTLAdditions.id("${RingBatchShaderSources.PREFIX}vanilla")
    var vanilla: ShaderInstance? = null

    @JvmStatic
    fun capacity(): Int {
        // Whole machines occupy three ring records.
        return (GL31C.glGetInteger(GL31C.GL_MAX_UNIFORM_BLOCK_SIZE) / RingBatchShaderSources.BYTES_PER_RING / 3) * 3
    }

    fun current(): ShaderInstance? {
        val pipeline = Iris.getPipelineManager().pipelineNullable
        if (pipeline is ShaderRenderingPipeline && pipeline.shouldOverrideShaders()) {
            return (pipeline as? DeferredIrisPipelineBridge)?.foaBatchShader
        }

        return vanilla
    }

    fun createVanilla(resources: ResourceProvider): ShaderInstance {
        fun text(id: ResourceLocation): String = resources.getResourceOrThrow(id).open().bufferedReader().use { it.readText() }
        val json = JsonParser.parseString(text(ResourceLocation("minecraft", "shaders/core/rendertype_translucent.json"))).asJsonObject
        fun stage(name: String, suffix: String): String {
            val id = ResourceLocation(json.get(name).asString)
            return text(ResourceLocation(id.namespace, "shaders/core/${id.path}.$suffix"))
        }

        val sources = RingBatchShaderSources.patchVanilla(stage("vertex", "vsh"), stage("fragment", "fsh"), capacity())
        json.addProperty("vertex", vanillaId.toString())
        json.addProperty("fragment", vanillaId.toString())
        val pack = PathPackResources("gtladditions-foa-batch", Path.of("."), false)
        val provider = ResourceProvider { id ->
            if (id.namespace == vanillaId.namespace && id.path.startsWith("shaders/core/${vanillaId.path}.")) {
                val source = when {
                    id.path.endsWith(".json") -> json.toString()
                    id.path.endsWith(".vsh") -> sources.vertex
                    id.path.endsWith(".fsh") -> sources.fragment
                    else -> error("Unexpected FOA shader resource: $id")
                }
                Optional.of(Resource(pack) { ByteArrayInputStream(source.toByteArray(Charsets.UTF_8)) })
            } else {
                resources.getResource(id)
            }
        }

        return ShaderInstance(provider, vanillaId, DefaultVertexFormat.BLOCK)
    }
}