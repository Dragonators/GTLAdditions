package com.gtladd.gtladditions.client.render.machine.heart

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.RenderUtils
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.Resource
import net.minecraft.server.packs.resources.ResourceProvider
import net.minecraftforge.client.event.RegisterShadersEvent
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL32C
import java.io.ByteArrayInputStream
import java.util.Optional

/** A subdraw selects its original diffuse group, with the native BLOCK vertex layout. */
internal object HeartShellTerrain {
    private const val PALETTE = "HeartShades"
    private val main = Regex("""\bvoid\s+main\s*\(\s*(?:void\s*)?\)\s*\{""")
    private val color = Regex("""\buniform\s+vec4\s+ColorModulator\s*;""")
    private val version = Regex("""(?m)^(\s*#version[^\r\n]*)""")
    private val shaders = HashMap<RenderType, ShaderInstance>()

    fun hasShader(type: RenderType): Boolean = type in shaders

    fun register(event: RegisterShadersEvent) {
        shaders.clear()
        if (!GL.getCapabilities().GL_ARB_shader_draw_parameters) return

        for ((type, name) in listOf(
            RenderType.solid() to "solid",
            RenderType.cutoutMipped() to "cutout_mipped",
            RenderType.cutout() to "cutout"
        )) {
            val id = GTLAdditions.id("heart_shell_$name")
            val provider = try {
                patchedProvider(event.resourceProvider, name, id)
            } catch (error: Exception) {
                GTLAdditions.LOGGER.error("Unable to load shader {}", id, error)
                continue
            } ?: continue

            RenderUtils.registerShader(event, id, { ShaderInstance(provider, id, DefaultVertexFormat.BLOCK) }) { shader ->
                if (shader?.getUniform(PALETTE) != null) shaders[type] = shader
            }
        }
    }

    private fun patchedProvider(resources: ResourceProvider, name: String, id: ResourceLocation): ResourceProvider? {
        fun resource(name: String, ext: String): Resource {
            val path = ResourceLocation(name)
            return resources.getResourceOrThrow(ResourceLocation(path.namespace, "shaders/core/${path.path}.$ext"))
        }

        fun Resource.text(): String = openAsReader().use { it.readText() }

        val source = resource("rendertype_$name", "json")
        val json = JsonParser.parseString(source.text()).asJsonObject
        val vertex = resource(json["vertex"].asString, "vsh").text()
        val fragment = resource(json["fragment"].asString, "fsh").text()

        // An unfamiliar core shader keeps the native grouped draw. Only ordinary terrain modulators can be batched here.
        val mainStart = main.findAll(vertex).singleOrNull()
        val fragmentColor = color.findAll(fragment).singleOrNull()
        if (mainStart == null || color.containsMatchIn(vertex) || fragmentColor == null) {
            GTLAdditions.LOGGER.warn("Skipping batched Heart shell shader {} for unfamiliar rendertype_{}", id, name)
            return null
        }

        json.addProperty("vertex", id.toString())
        json.addProperty("fragment", id.toString())
        json.getAsJsonArray("uniforms").add(
            JsonObject().apply {
                addProperty("name", PALETTE)
                addProperty("type", "float")
                addProperty("count", HeartShellRenderer.GROUPS)
                add("values", JsonArray().apply { add(1.0f) })
            }
        )

        val stages = mapOf(
            "json" to json.toString(),
            "vsh" to vertex.replaceRange(
                mainStart.range,
                "uniform float $PALETTE[${HeartShellRenderer.GROUPS}];\nflat out float heartFaceShade;\n" +
                    "${mainStart.value}\nheartFaceShade = $PALETTE[gl_DrawIDARB];\n"
            ).replaceFirst(version, "$1\n#extension GL_ARB_shader_draw_parameters : require"),
            "fsh" to fragment.replaceRange(
                fragmentColor.range,
                "uniform vec4 ColorModulator;\nflat in float heartFaceShade;\n" +
                    "#define ColorModulator (ColorModulator * vec4(vec3(heartFaceShade), 1.0))\n"
            )
        )

        return ResourceProvider { location ->
            val stage = if (location.namespace == id.namespace && location.path.substringBeforeLast('.') == "shaders/core/${id.path}") {
                stages[location.path.substringAfterLast('.')]
            } else {
                null
            }

            if (stage == null) {
                resources.getResource(location)
            } else {
                Optional.of(Resource(source.source()) { ByteArrayInputStream(stage.toByteArray(Charsets.UTF_8)) })
            }
        }
    }

    fun upload(builder: BufferBuilder, verticesPerGroup: IntArray): VertexBuffer {
        val buffer = DirectionBuffer(verticesPerGroup)
        try {
            RenderUtils.withBoundBuffer(buffer) {
                buffer.upload(builder.end())
            }

            return buffer
        } catch (error: Throwable) {
            buffer.close()
            throw error
        }
    }

    private class DirectionBuffer(verticesPerGroup: IntArray) : VertexBuffer(Usage.STATIC) {
        // Every range starts at index zero in Minecraft's shared quad indices. Empty slots retain their gl_DrawID.
        private val counts = BufferUtils.createIntBuffer(verticesPerGroup.size)
        private val bases = BufferUtils.createIntBuffer(verticesPerGroup.size)
        private val offsets = BufferUtils.createPointerBuffer(verticesPerGroup.size)

        init {
            var base = 0

            for ((group, vertices) in verticesPerGroup.withIndex()) {
                counts.put(group, vertices / 4 * 6)
                bases.put(group, base)
                base += vertices
            }
        }

        override fun draw() {
            val indexType = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS).type().asGLType
            GL32C.glMultiDrawElementsBaseVertex(GL11C.GL_TRIANGLES, counts, indexType, offsets, bases)
        }
    }

    class Layer(private val buffers: Map<RenderType, VertexBuffer>) : HeartShellRenderer.Terrain {
        override fun draw(pose: PoseStack.Pose, shades: FloatArray) {
            for ((type, buffer) in buffers) {
                RenderUtils.withRenderType(type) {
                    val shader = shaders.getValue(type)
                    shader.getUniform(PALETTE)!!.set(shades)
                    RenderUtils.withBoundBuffer(buffer) {
                        buffer.drawWithShader(pose.pose(), RenderSystem.getProjectionMatrix(), shader)
                    }
                }
            }
        }

        override fun close() = RenderUtils.releaseBuffers(buffers.values)
    }
}