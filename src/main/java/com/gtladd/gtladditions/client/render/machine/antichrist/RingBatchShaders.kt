package com.gtladd.gtladditions.client.render.machine.antichrist

import com.google.gson.JsonParser
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.render.scene.IrisPipelineBridge
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
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL31C
import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.util.Optional

@OnlyIn(Dist.CLIENT)
object RingBatchShaders {
    private const val MAX_PALETTE_BYTES = 65536
    private val vanillaId = GTLAdditions.id("${RingBatchShaderSources.PREFIX}vanilla")
    var vanilla: ShaderInstance? = null

    @JvmStatic
    fun capacity(): Int {
        // Whole machines occupy three ring records; cap at 64 KiB so drivers reporting multi-MB UBO limits do not oversize the block.
        val blockBytes = minOf(GL31C.glGetInteger(GL31C.GL_MAX_UNIFORM_BLOCK_SIZE), MAX_PALETTE_BYTES)
        return (blockBytes / RingBatchShaderSources.BYTES_PER_RING / 3) * 3
    }

    fun current(): ShaderInstance? {
        val pipeline = Iris.getPipelineManager().pipelineNullable
        if (pipeline is ShaderRenderingPipeline && pipeline.shouldOverrideShaders()) {
            return (pipeline as? IrisPipelineBridge)?.foaBatchShader
        }

        return vanilla
    }

    fun isUsable(shader: ShaderInstance): Boolean =
        GL20C.glGetProgrami(shader.id, GL20C.GL_LINK_STATUS) != 0 &&
            GL20C.glGetAttribLocation(shader.id, RingBatchShaderSources.ATTRIBUTE) >= 0 &&
            GL31C.glGetUniformBlockIndex(shader.id, RingBatchShaderSources.BLOCK) != GL31C.GL_INVALID_INDEX

    fun createVanilla(resources: ResourceProvider): ShaderInstance? {
        fun text(id: ResourceLocation): String = resources.getResourceOrThrow(id).open().bufferedReader().use { it.readText() }
        val json = JsonParser.parseString(text(ResourceLocation("minecraft", "shaders/core/rendertype_translucent.json"))).asJsonObject
        fun stage(name: String, suffix: String): String {
            val id = ResourceLocation(json.get(name).asString)
            return text(ResourceLocation(id.namespace, "shaders/core/${id.path}.$suffix"))
        }

        val sources = RingBatchShaderSources.patchVanilla(stage("vertex", "vsh"), stage("fragment", "fsh"), capacity()) ?: return null
        json.addProperty("vertex", vanillaId.toString())
        json.addProperty("fragment", vanillaId.toString())
        val stages = mapOf("json" to json.toString(), "vsh" to sources.vertex, "fsh" to sources.fragment)
        val pack = PathPackResources("gtladditions-foa-batch", Path.of("."), false)
        val provider = ResourceProvider { id ->
            val stage = if (id.namespace == vanillaId.namespace && id.path.substringBeforeLast('.') == "shaders/core/${vanillaId.path}") {
                stages[id.path.substringAfterLast('.')]
            } else {
                null
            }

            if (stage == null) {
                resources.getResource(id)
            } else {
                Optional.of(Resource(pack) { ByteArrayInputStream(stage.toByteArray(Charsets.UTF_8)) })
            }
        }

        return ShaderInstance(provider, vanillaId, DefaultVertexFormat.BLOCK)
    }
}
