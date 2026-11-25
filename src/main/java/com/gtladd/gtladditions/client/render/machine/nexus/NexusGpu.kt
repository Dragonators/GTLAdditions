package com.gtladd.gtladditions.client.render.machine.nexus

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.scene.ResourceSlot
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.server.packs.resources.ResourceProvider
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL
import org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LOD
import org.lwjgl.opengl.GL13.*
import org.lwjgl.opengl.GL14.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL31.*
import org.lwjgl.opengl.GL32.GL_PROGRAM_POINT_SIZE
import org.lwjgl.opengl.GL33.glVertexAttribDivisor
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream

/** Undoes the exporter's `byteShuffle` layout, which stores byte k of every element contiguously. */
internal fun unshuffleBytes(packed: ByteArray, stride: Int): ByteArray {
    val raw = ByteArray(packed.size)
    var source = 0

    for (lane in 0 until stride) {
        for (target in lane until raw.size step stride) raw[target] = packed[source++]
    }

    return raw
}

internal object NexusGpu {
    private const val ROOT = "subspace_nexus/"
    private const val COMMON = "shaders/scene_common.glsl"
    private const val HAND = "shaders/scene_hand.glsl"

    private const val BELT_SOURCE = "dir.beltK1"
    private const val BELT_ROCKS = "dir.beltRocks"

    private const val SHIP_INSTANCES = "ship-instances"

    private val FRAME_UNIFORMS = setOf(
        "modelMatrix",
        "viewMatrix",
        "modelViewMatrix",
        "projectionMatrix",
        "normalMatrix",
        "uCam",
        "cameraPosition",
        "uTime",
        "uGlow",
        "uAmb",
        "uPix",
        "uScale",
        "uProjZ",
        "NexusHandMaskEnabled",
        "NexusHandMask"
    )

    // No reload invalidates this slot: the served set and the scenes built from it stay consistent until [replace].
    private val slot = ResourceSlot<Loaded>("Subspace Nexus resources")

    val current: Loaded?
        get() = slot.value

    fun bytes(file: String): ByteArray = Minecraft.getInstance().resourceManager
        .getResourceOrThrow(GTLAdditions.id(ROOT + file)).open().use { it.readBytes() }

    fun nodes(): List<Node> = checkNotNull(current).nodes

    fun shipBatch(data: JsonObject): Node = checkNotNull(current).shipBatch(data)

    fun effects(): JsonObject = checkNotNull(current).effects

    fun manifest(): JsonObject = checkNotNull(current).manifest

    fun belt(): JsonObject = checkNotNull(current).belt

    fun beltIndices(): IntArray = checkNotNull(current).beltIndices

    @Throws(IOException::class)
    fun read(resources: ResourceProvider): Prepared {
        fun file(name: String) = resources.getResourceOrThrow(GTLAdditions.id(ROOT + name)).open().use { it.readBytes() }
        fun json(name: String) = JsonParser.parseString(file(name).toString(Charsets.UTF_8)).asJsonObject
        val scene = json("scene.json")
        val sources = HashMap<String, String>()
        for (name in listOf(COMMON, HAND)) sources[name] = file(name).toString(Charsets.UTF_8)

        for ((_, shader) in scene.getAsJsonObject("shaders").entrySet()) {
            for (stage in listOf("vertex", "fragment")) {
                val name = shader.asJsonObject.get(stage).asString
                sources.getOrPut(name) { file(name).toString(Charsets.UTF_8) }
            }
        }

        val meshes = HashMap<String, ByteArray>()
        val arrays = HashMap<String, NexusTextureArray.Layers>()
        var beltIndices: IntArray? = null

        for (element in scene.getAsJsonArray("nodes")) {
            val node = element.asJsonObject
            val label = node.get("label").asString
            if (label != BELT_SOURCE) {
                val descriptor = node.getAsJsonObject("geometry")
                val raw = meshes.getOrPut(descriptor.toString()) {
                    val packed = GZIPInputStream(file(descriptor.get("file").asString).inputStream()).use { it.readBytes() }
                    descriptor.get("byteShuffle")?.asInt?.let { unshuffleBytes(packed, it) } ?: packed
                }

                if (label == BELT_ROCKS) {
                    val buffer = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
                    buffer.position(descriptor.get("indexOffset").asInt)
                    beltIndices = IntArray(descriptor.get("indexCount").asInt).also { buffer.asIntBuffer().get(it) }
                }
            }

            for ((_, entry) in node.getAsJsonObject("uniforms").entrySet()) {
                if (entry.isJsonObject && entry.asJsonObject.has("layers")) {
                    arrays.getOrPut(entry.toString()) { NexusTextureArray.read(entry.asJsonObject, ::file) }
                }
            }
        }

        return Prepared(
            scene,
            json("effects.json"),
            json("asteroid-belt.json"),
            sources,
            meshes,
            arrays,
            checkNotNull(beltIndices) { "Nexus scene has no $BELT_ROCKS" }
        )
    }

    fun replace(prepared: () -> Prepared, retire: () -> Unit) {
        var next: Loaded? = null
        var failure: Exception? = null
        try {
            next = build(prepared())
        } catch (error: Exception) {
            failure = error
        }

        retire()
        if (next != null) slot.publish(next) else slot.fail(checkNotNull(failure))
    }

    /** Binds textures on the active unit only and uses no program. */
    private fun build(prepared: Prepared): Loaded {
        val vao = glGetInteger(GL_VERTEX_ARRAY_BINDING)
        val buffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
        val texture = glGetInteger(GL_TEXTURE_BINDING_2D)
        val array = glGetInteger(GL_TEXTURE_BINDING_2D_ARRAY)
        try {
            return RenderUtils.withTightlyPackedPixels { Loaded(prepared) }
        } finally {
            glBindVertexArray(vao)
            glBindBuffer(GL_ARRAY_BUFFER, buffer)
            RenderSystem.bindTexture(texture)
            glBindTexture(GL_TEXTURE_2D_ARRAY, array)
        }
    }

    fun preserve(draw: () -> Unit) {
        val textureUnits = checkNotNull(current).textureUnits
        val previousProgram = glGetInteger(GL_CURRENT_PROGRAM)
        val vao = glGetInteger(GL_VERTEX_ARRAY_BINDING)
        val buffer = glGetInteger(GL_ARRAY_BUFFER_BINDING)
        val active = glGetInteger(GL_ACTIVE_TEXTURE)
        val offset = glIsEnabled(GL_POLYGON_OFFSET_FILL)
        val pointSize = glIsEnabled(GL_PROGRAM_POINT_SIZE)
        val offsetFactor = glGetFloat(GL_POLYGON_OFFSET_FACTOR)
        val offsetUnits = glGetFloat(GL_POLYGON_OFFSET_UNITS)

        val bound = IntArray(textureUnits) { i ->
            RenderSystem.activeTexture(GL_TEXTURE0 + i)
            glGetInteger(GL_TEXTURE_BINDING_2D)
        }

        val arrays = IntArray(textureUnits) { i ->
            RenderSystem.activeTexture(GL_TEXTURE0 + i)
            glGetInteger(GL_TEXTURE_BINDING_2D_ARRAY)
        }

        try {
            RenderUtils.withSavedRenderState(draw)
        } finally {
            glUseProgram(previousProgram)
            glBindVertexArray(vao)
            glBindBuffer(GL_ARRAY_BUFFER, buffer)

            for (i in bound.indices) {
                RenderSystem.activeTexture(GL_TEXTURE0 + i)
                RenderSystem.bindTexture(bound[i])
                glBindTexture(GL_TEXTURE_2D_ARRAY, arrays[i])
            }

            RenderSystem.activeTexture(active)
            glPolygonOffset(offsetFactor, offsetUnits)
            if (offset) glEnable(GL_POLYGON_OFFSET_FILL) else glDisable(GL_POLYGON_OFFSET_FILL)
            if (pointSize) glEnable(GL_PROGRAM_POINT_SIZE) else glDisable(GL_PROGRAM_POINT_SIZE)
        }
    }

    /** [meshes] by geometry descriptor, vertex then index bytes; [arrays] by uniform entry; [sources] by shader file. */
    class Prepared(
        val scene: JsonObject,
        val effects: JsonObject,
        val belt: JsonObject,
        val sources: Map<String, String>,
        val meshes: Map<String, ByteArray>,
        val arrays: Map<String, NexusTextureArray.Layers>,
        val beltIndices: IntArray
    )

    class Ledger : AutoCloseable {
        private val programs = ArrayList<Int>()
        private val buffers = ArrayList<Int>()
        private val vertexArrays = ArrayList<Int>()
        private val textures = ArrayList<Int>()

        fun program(id: Int) = id.also(programs::add)

        fun buffer(id: Int) = id.also(buffers::add)

        fun vertexArray(id: Int) = id.also(vertexArrays::add)

        fun texture(id: Int) = id.also(textures::add)

        override fun close() {
            vertexArrays.forEach { glDeleteVertexArrays(it) }
            buffers.forEach { glDeleteBuffers(it) }
            textures.forEach { glDeleteTextures(it) }
            programs.forEach { glDeleteProgram(it) }
        }
    }

    private data class GeometryKey(val mesh: Mesh, val program: Program, val bindingGroup: String)

    class Loaded internal constructor(prepared: Prepared) : AutoCloseable {
        internal val ledger = Ledger()
        val manifest = prepared.scene
        val effects = prepared.effects
        val belt = prepared.belt
        val beltIndices = prepared.beltIndices
        private val programs = HashMap<String, Program>()
        private val meshes = HashMap<String, Mesh>()
        private val geometry = HashMap<GeometryKey, Geometry>()
        private val arrays = HashMap<String, NexusTextureArray>()

        internal val configuredTextures = HashSet<Int>()
        private val whiteTexture: Int
        private val whiteArray: Int
        val nodes: List<Node>
        val textureUnits: Int

        init {
            try {
                for ((id, shader) in manifest.getAsJsonObject("shaders").entrySet()) {
                    programs[id] = Program(shader.asJsonObject, prepared.sources, this)
                }

                nodes = manifest.getAsJsonArray("nodes").map { element ->
                    val data = element.asJsonObject
                    val label = data.get("label").asString
                    if (label == BELT_SOURCE) return@map Node(data)
                    val program = programs.getValue(data.get("shader").asString)
                    arrays(data, program, prepared)
                    if (label.startsWith("ship.")) {
                        geometry(data.getAsJsonObject("geometry"), program, SHIP_INSTANCES, prepared)
                        Node(data)
                    } else {
                        // Static rings can share their VAO; mutable bindings stay isolated.
                        val bindingGroup = if (label.startsWith("dir.sr.styles.3.rings.")) "" else label
                        Node(data, program, geometry(data.getAsJsonObject("geometry"), program, bindingGroup, prepared))
                    }
                }

                textureUnits = nodes.maxOf { it.textureUnitCount }
                whiteTexture = white(GL_TEXTURE_2D)
                whiteArray = white(GL_TEXTURE_2D_ARRAY)
            } catch (error: Exception) {
                ledger.close()
                throw error
            }
        }

        private fun geometry(descriptor: JsonObject, program: Program, bindingGroup: String, prepared: Prepared): Geometry {
            val mesh = meshes.getOrPut(descriptor.toString()) { Mesh(descriptor, prepared.meshes.getValue(descriptor.toString()), ledger) }
            return geometry.getOrPut(GeometryKey(mesh, program, bindingGroup)) { Geometry(descriptor, program, mesh) }
        }

        private fun arrays(data: JsonObject, program: Program, prepared: Prepared) {
            for ((name, entry) in data.getAsJsonObject("uniforms").entrySet()) {
                if (program.samplerType(name) != GL_SAMPLER_2D_ARRAY || !entry.isJsonObject || !entry.asJsonObject.has("layers")) continue
                arrays.getOrPut(entry.toString()) {
                    NexusTextureArray(entry.asJsonObject, prepared.arrays.getValue(entry.toString()), ledger)
                }
            }
        }

        internal fun array(spec: JsonObject): NexusTextureArray = arrays.getValue(spec.toString())

        fun clearSceneBindings() {
            val vao = glGetInteger(GL_VERTEX_ARRAY_BINDING)
            try {
                for (binding in geometry.values) binding.clearSceneBinding()
            } finally {
                glBindVertexArray(vao)
            }
        }

        fun shipBatch(data: JsonObject): Node {
            val program = programs.getValue(data.get("shader").asString)
            val descriptor = data.getAsJsonObject("geometry")
            return Node(data, program, geometry.getValue(GeometryKey(meshes.getValue(descriptor.toString()), program, SHIP_INSTANCES)))
        }

        private fun white(target: Int): Int {
            val id = ledger.texture(glGenTextures())
            if (target == GL_TEXTURE_2D) RenderSystem.bindTexture(id) else glBindTexture(target, id)
            MemoryStack.stackPush().use { stack ->
                val pixel = stack.malloc(4).putInt(-1).flip()
                if (target == GL_TEXTURE_2D_ARRAY) {
                    glTexImage3D(target, 0, GL_RGBA8, 1, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixel)
                } else {
                    glTexImage2D(target, 0, GL_RGBA8, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixel)
                }
            }

            glTexParameteri(target, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTexParameteri(target, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            return id
        }

        internal fun bindWhite(type: Int) {
            if (type == GL_SAMPLER_2D_ARRAY) glBindTexture(GL_TEXTURE_2D_ARRAY, whiteArray) else RenderSystem.bindTexture(whiteTexture)
        }

        override fun close() = ledger.close()
    }

    class Program internal constructor(source: JsonObject, sources: Map<String, String>, internal val owner: Loaded) {
        val id: Int

        private class Uniform(val location: Int, val type: Int) {
            var previous: FloatArray? = null
            var sampler = Int.MIN_VALUE
        }

        private val uniforms = HashMap<String, Uniform>()
        private val scalarValues = FloatArray(1)
        private val pairValues = FloatArray(2)

        init {
            val defines = source.getAsJsonObject("defines")?.entrySet().orEmpty().joinToString("") { (name, value) -> "#define $name ${value.asString}\n" }
            fun compile(file: String, stage: Int): Int {
                val material = defines + sources.getValue(file)
                val common = sources.getValue(COMMON)
                val code = if (stage == GL_VERTEX_SHADER) {
                    NexusShaderSources.vertex(material, common)
                } else {
                    NexusShaderSources.fragment(material, common, sources.getValue(HAND))
                }

                val shader = glCreateShader(stage)
                glShaderSource(shader, code)
                glCompileShader(shader)
                if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
                    val message = glGetShaderInfoLog(shader)
                    glDeleteShader(shader)
                    error("Nexus shader $file: $message")
                }

                return shader
            }

            val vertex = compile(source.get("vertex").asString, GL_VERTEX_SHADER)
            id = try {
                val fragment = compile(source.get("fragment").asString, GL_FRAGMENT_SHADER)
                try {
                    // The ledger deletes it with the rest of a failed build.
                    val program = owner.ledger.program(glCreateProgram())
                    glAttachShader(program, vertex)
                    glAttachShader(program, fragment)
                    glLinkProgram(program)
                    if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
                        error("Nexus program ${source.get("fragment").asString}: ${glGetProgramInfoLog(program)}")
                    }

                    program
                } finally {
                    glDeleteShader(fragment)
                }
            } finally {
                glDeleteShader(vertex)
            }

            MemoryStack.stackPush().use { stack ->
                val size = stack.mallocInt(1)
                val type = stack.mallocInt(1)

                for (i in 0 until glGetProgrami(id, GL_ACTIVE_UNIFORMS)) {
                    val name = glGetActiveUniform(id, i, size, type).removeSuffix("[0]")
                    uniforms[name] = Uniform(glGetUniformLocation(id, name), type[0])
                }
            }
        }

        fun set(name: String, values: FloatArray) {
            val uniform = uniforms[name] ?: return
            val previous = uniform.previous
            if (previous != null && previous.contentEquals(values)) return

            if (previous == null || previous.size != values.size) {
                uniform.previous = values.copyOf()
            } else {
                values.copyInto(previous)
            }

            val location = uniform.location
            when (uniform.type) {
                GL_FLOAT -> glUniform1fv(location, values)
                GL_FLOAT_VEC2 -> glUniform2fv(location, values)
                GL_FLOAT_VEC3 -> glUniform3fv(location, values)
                GL_FLOAT_VEC4 -> glUniform4fv(location, values)
                GL_FLOAT_MAT3 -> glUniformMatrix3fv(location, false, values)
                GL_FLOAT_MAT4 -> glUniformMatrix4fv(location, false, values)
                GL_INT, GL_BOOL -> glUniform1i(location, values[0].toInt())
            }
        }

        fun scalar(name: String, value: Float) {
            scalarValues[0] = value
            set(name, scalarValues)
        }

        fun pair(name: String, x: Float, y: Float) {
            pairValues[0] = x
            pairValues[1] = y
            set(name, pairValues)
        }

        fun sampler(name: String, unit: Int) {
            val uniform = uniforms[name] ?: return
            if (uniform.sampler == unit) return
            glUniform1i(uniform.location, unit)
            uniform.sampler = unit
        }

        fun active(name: String): Boolean = uniforms.containsKey(name)

        fun samplerType(name: String): Int? = uniforms[name]?.type?.takeIf { it == GL_SAMPLER_2D || it == GL_SAMPLER_2D_ARRAY }
    }

    class Mesh internal constructor(val descriptor: JsonObject, raw: ByteArray, ledger: Ledger) {
        val vertices = ledger.buffer(glGenBuffers())
        val elements = ledger.buffer(glGenBuffers())
        val count = descriptor.get("indexCount").asInt

        init {
            val data = MemoryUtil.memAlloc(raw.size).put(raw).flip()
            try {
                val split = descriptor.get("indexOffset").asInt
                glBindBuffer(GL_ARRAY_BUFFER, vertices)
                data.limit(split)
                glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW)

                // Upload through ARRAY_BUFFER so creation never changes a caller's VAO element binding.
                glBindBuffer(GL_ARRAY_BUFFER, elements)
                data.limit(raw.size).position(split)
                glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW)
            } finally {
                MemoryUtil.memFree(data)
            }
        }
    }

    class Geometry internal constructor(val descriptor: JsonObject, private val program: Program, val mesh: Mesh) {
        private val ledger = program.owner.ledger
        val vao = ledger.vertexArray(glGenVertexArrays())

        private class Attribute(val buffer: Int, val active: Boolean)
        private val dynamic = HashMap<String, Attribute>()
        private var uploadedNode: Node? = null
        private var uploadedVersion = -1L
        private var streamedIndices = false
        val count = mesh.count

        init {
            glBindVertexArray(vao)
            glBindBuffer(GL_ARRAY_BUFFER, mesh.vertices)

            for ((name, entry) in descriptor.getAsJsonObject("attributes").entrySet()) {
                val location = glGetAttribLocation(program.id, name)
                if (location < 0) continue
                val attribute = entry.asJsonObject
                glEnableVertexAttribArray(location)
                glVertexAttribPointer(location, attribute.get("size").asInt, GL_FLOAT, false, 0, attribute.get("offset").asLong)
            }

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, mesh.elements)
        }

        fun bind(node: Node, attributes: Collection<FloatData>, source: Geometry = this) {
            glBindVertexArray(vao)
            if (uploadedNode === node && uploadedVersion == node.version) return

            for (data in attributes) {
                if (data.size == 0) continue
                val attribute = dynamic.getOrPut(data.name) {
                    val locations = data.layout.map { glGetAttribLocation(program.id, it.first) }
                    val active = locations.any { it >= 0 }
                    val buffer = when {
                        !active -> 0
                        source === this -> ledger.buffer(glGenBuffers())
                        else -> source.dynamic.getValue(data.name).buffer
                    }

                    if (active) {
                        glBindBuffer(GL_ARRAY_BUFFER, buffer)
                        var offset = 0L

                        for ((index, field) in data.layout.withIndex()) {
                            val location = locations[index]
                            if (location >= 0) {
                                glEnableVertexAttribArray(location)
                                glVertexAttribPointer(location, field.second, GL_FLOAT, false, data.components * Float.SIZE_BYTES, offset)
                                glVertexAttribDivisor(location, if (data.instanced) 1 else 0)
                            }

                            offset += field.second * Float.SIZE_BYTES
                        }
                    }

                    Attribute(buffer, active)
                }

                if (!attribute.active || source !== this) continue
                glBindBuffer(GL_ARRAY_BUFFER, attribute.buffer)
                glBufferData(GL_ARRAY_BUFFER, data.buffer(), GL_STREAM_DRAW)
            }

            uploadedNode = node
            uploadedVersion = node.version
        }

        fun bindIndices(indices: NexusIndexPages?) {
            if (indices != null) {
                indices.bind()
                streamedIndices = true
            } else if (streamedIndices) {
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, mesh.elements)
                streamedIndices = false
            }
        }

        fun clearSceneBinding() {
            uploadedNode = null
            if (streamedIndices) {
                // A shared VAO keeps the scene's EBO alive until its element binding is replaced.
                glBindVertexArray(vao)
                bindIndices(null)
            }
        }
    }

    /** Direct buffers are reclaimed with their weakly cached scene. */
    class FloatData(
        val name: String,
        val components: Int,
        val instanced: Boolean,
        val layout: List<Pair<String, Int>> = listOf(name to components)
    ) {
        private var storage = org.lwjgl.BufferUtils.createFloatBuffer(64)
        var size = 0
            private set

        fun clear() {
            size = 0
            storage.clear()
        }

        private fun reserve(count: Int) {
            if (size + count <= storage.capacity()) return
            val next = org.lwjgl.BufferUtils.createFloatBuffer(maxOf(size + count, storage.capacity() * 2))
            storage.position(0).limit(size)
            next.put(storage)
            storage = next
        }

        fun put(x: Double) {
            reserve(1)
            storage.put(size++, x.toFloat())
        }

        fun put(x: Double, y: Double) {
            put(x)
            put(y)
        }

        fun put(x: Double, y: Double, z: Double) {
            put(x)
            put(y)
            put(z)
        }

        fun put(x: Double, y: Double, z: Double, w: Double) {
            put(x)
            put(y)
            put(z)
            put(w)
        }

        fun put(matrix: Matrix4f) {
            reserve(16)
            matrix.get(size, storage)
            size += 16
        }

        fun put(v: org.joml.Vector3d) {
            put(v.x, v.y, v.z)
        }

        fun set(values: FloatArray) {
            size = 0
            reserve(values.size)
            storage.clear()
            storage.put(values)
            size = values.size
        }

        fun buffer(): java.nio.FloatBuffer = storage.position(0).limit(size)
    }

    class Node(val data: JsonObject, val program: Program? = null, val geometry: Geometry? = null) {
        val label: String = data.get("label").asString
        private val baseMatrix = Matrix4f().set(data.getAsJsonArray("matrix").map { it.asFloat }.toFloatArray())
        val matrix = Matrix4f(baseMatrix)

        private class Value(val name: String, val floats: FloatArray, var active: Boolean = false)

        private data class Texture(
            val name: String,
            val type: Int,
            val id: net.minecraft.resources.ResourceLocation?,
            val wrapS: Int,
            val wrapT: Int,
            val array: NexusTextureArray?
        )

        private val values = HashMap<String, Value>()
        private val activeValues = ArrayList<Value>()
        private val attributes = LinkedHashMap<String, FloatData>()
        private val defaults = ArrayList<Value>()
        private val textures = ArrayList<Texture>()
        val textureUnitCount: Int get() = textures.size + 1

        private val modelView = Matrix4f()
        private val normal = Matrix3f()
        private val matrixValues = FloatArray(16)
        private val normalValues = FloatArray(9)

        // A second pass can read the originating node's VBO through its own program-specific VAO.
        var instanceSource: Node? = null
        var visible = true
        var instances = 0
        var drawCount = geometry?.count ?: 0
        var indices: NexusIndexPages? = null
        var version = 0L
            private set

        val opaque = data.get("depthWrite").asBoolean
        val order = data.get("order").asDouble
        private val doubleSided = data.get("side").asInt == 2
        private val transparent = data.get("transparent").asBoolean
        private val polygonOffset = data.get("polygonOffset").asBoolean
        private val mode = if (data.get("points").asBoolean) GL_POINTS else GL_TRIANGLES
        var instanced = data.getAsJsonObject("instanced").size() > 0
        private val blendSrc = factor(data.get("blendSrc").asInt)
        private val blendDst = factor(data.get("blendDst").asInt)

        init {
            for ((name, entry) in data.getAsJsonObject("uniforms").entrySet()) {
                if (program == null || !program.active(name) || name in FRAME_UNIFORMS) continue
                val type = program.samplerType(name)
                if (type != null) {
                    val file = if (entry.isJsonObject) entry.asJsonObject.get("texture") else null
                    val id = if (file != null && !file.isJsonNull) GTLAdditions.id(ROOT + file.asString) else null
                    textures.add(
                        Texture(
                            name,
                            type,
                            id,
                            if (entry.isJsonObject && entry.asJsonObject.get("wrapS")?.asInt == 1000) GL_REPEAT else GL_CLAMP_TO_EDGE,
                            if (entry.isJsonObject && entry.asJsonObject.get("wrapT")?.asInt == 1000) GL_REPEAT else GL_CLAMP_TO_EDGE,
                            if (type == GL_SAMPLER_2D_ARRAY && entry.isJsonObject && entry.asJsonObject.has("layers")) {
                                program.owner.array(entry.asJsonObject)
                            } else {
                                null
                            }
                        )
                    )
                } else if (!entry.isJsonNull) {
                    val floats = if (entry.isJsonArray) {
                        entry.asJsonArray.map { it.asFloat }.toFloatArray()
                    } else if (entry.isJsonPrimitive) {
                        floatArrayOf(
                            if (entry.asJsonPrimitive.isBoolean) {
                                if (entry.asBoolean) 1f else 0f
                            } else {
                                entry.asFloat
                            }
                        )
                    } else {
                        continue
                    }
                    defaults.add(Value(name, floats))
                }
            }
        }

        private fun valueStorage(name: String, size: Int): FloatArray? {
            if (program != null && !program.active(name)) return null
            val value = values.getOrPut(name) { Value(name, FloatArray(size)) }
            check(value.floats.size == size)
            if (!value.active) {
                value.active = true
                activeValues.add(value)
            }

            return value.floats
        }

        fun value(name: String, x: Double) = value(name, x.toFloat())

        fun value(name: String, x: Float) {
            valueStorage(name, 1)?.let { it[0] = x }
        }

        fun value(name: String, x: Double, y: Double) {
            valueStorage(name, 2)?.let {
                it[0] = x.toFloat()
                it[1] = y.toFloat()
            }
        }

        fun value(name: String, x: Double, y: Double, z: Double) {
            valueStorage(name, 3)?.let {
                it[0] = x.toFloat()
                it[1] = y.toFloat()
                it[2] = z.toFloat()
            }
        }

        fun value(name: String, x: Double, y: Double, z: Double, w: Double) {
            valueStorage(name, 4)?.let {
                it[0] = x.toFloat()
                it[1] = y.toFloat()
                it[2] = z.toFloat()
                it[3] = w.toFloat()
            }
        }

        fun value(name: String, value: FloatArray) {
            valueStorage(name, value.size)?.let { value.copyInto(it) }
        }

        fun attributeData(name: String, size: Int, instanced: Boolean = false): FloatData =
            attributes.getOrPut(name) { FloatData(name, size, instanced) }

        fun attributeStream(name: String, layout: List<Pair<String, Int>>, instanced: Boolean): FloatData =
            attributes.getOrPut(name) { FloatData(name, layout.sumOf { it.second }, instanced, layout) }

        fun scalarValue(name: String): Float = values[name]?.takeIf { it.active }?.floats?.get(0)
            ?: data.getAsJsonObject("uniforms").get(name)?.asFloat ?: 0f

        fun attribute(name: String, size: Int, values: FloatArray, instanced: Boolean = false) {
            attributeData(name, size, instanced).set(values)
        }

        fun reset() {
            for (value in activeValues) value.active = false
            activeValues.clear()
            for (attribute in attributes.values) attribute.clear()
            visible = false
            instances = 0
            drawCount = geometry?.count ?: 0
            matrix.set(baseMatrix)
            version++
        }

        fun draw(view: Matrix4f, projection: Matrix4f, camera: FloatArray, time: Double, glow: Float, ambient: Float, handMask: Int = 0) {
            if (!visible || drawCount <= 0 || (instanced && instances <= 0)) return
            val program = program ?: return
            val geometry = geometry ?: return
            glUseProgram(program.id)
            for (value in defaults) if (values[value.name]?.active != true) program.set(value.name, value.floats)

            var unit = 0
            for (texture in textures) {
                RenderSystem.activeTexture(GL_TEXTURE0 + unit)
                if (texture.array != null) {
                    glBindTexture(GL_TEXTURE_2D_ARRAY, texture.array.id)
                } else if (texture.id != null) {
                    val manager = Minecraft.getInstance().textureManager
                    manager.bindForSetup(texture.id)
                    if (program.owner.configuredTextures.add(manager.getTexture(texture.id).id)) {
                        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
                        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
                        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, texture.wrapS)
                        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, texture.wrapT)
                        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 1000)
                        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_LOD, 1000f)
                        glGenerateMipmap(GL_TEXTURE_2D)
                    }
                } else {
                    program.owner.bindWhite(texture.type)
                }

                program.sampler(texture.name, unit++)
            }

            program.scalar("NexusHandMaskEnabled", if (handMask != 0) 1f else 0f)
            program.sampler("NexusHandMask", unit)
            if (handMask != 0) {
                RenderSystem.activeTexture(GL_TEXTURE0 + unit)
                RenderSystem.bindTexture(handMask)
            }

            if (program.active("modelMatrix")) program.set("modelMatrix", matrix.get(matrixValues))
            if (program.active("viewMatrix")) program.set("viewMatrix", view.get(matrixValues))
            if (program.active("projectionMatrix")) program.set("projectionMatrix", projection.get(matrixValues))
            if (program.active("modelViewMatrix") || program.active("normalMatrix")) {
                modelView.set(view).mul(matrix)
                if (program.active("modelViewMatrix")) program.set("modelViewMatrix", modelView.get(matrixValues))
                if (program.active("normalMatrix")) program.set("normalMatrix", modelView.normal(normal).get(normalValues))
            }

            program.set("uCam", camera)
            program.set("cameraPosition", camera)
            program.scalar("uTime", time.toFloat())
            program.scalar("uGlow", glow)
            program.scalar("uAmb", ambient)
            val pixel = 2f / (projection.m11() * Minecraft.getInstance().window.height)
            program.scalar("uPix", pixel)
            program.scalar("uScale", 1f / pixel)
            program.pair("uProjZ", projection.m22(), projection.m32())
            for (value in activeValues) program.set(value.name, value.floats)

            RenderSystem.enableDepthTest()
            RenderSystem.depthMask(opaque)
            if (doubleSided) RenderSystem.disableCull() else RenderSystem.enableCull()

            if (transparent) {
                RenderSystem.enableBlend()
                glBlendEquation(GL_FUNC_ADD)
                RenderSystem.blendFunc(blendSrc, blendDst)
            } else {
                RenderSystem.disableBlend()
            }

            if (polygonOffset) {
                glEnable(GL_POLYGON_OFFSET_FILL)
                glPolygonOffset(-1f, -2f)
            } else {
                glDisable(GL_POLYGON_OFFSET_FILL)
            }

            val source = instanceSource
            if (source == null) {
                geometry.bind(this, attributes.values)
            } else {
                val sourceGeometry = checkNotNull(source.geometry)
                sourceGeometry.bind(source, source.attributes.values)
                geometry.bind(source, source.attributes.values, sourceGeometry)
            }
            // A VAO can be shared by nodes with different compact draw lists.
            geometry.bindIndices(indices)

            if (mode == GL_POINTS) glEnable(GL_PROGRAM_POINT_SIZE)
            if (instanced) {
                glDrawElementsInstanced(mode, drawCount, GL_UNSIGNED_INT, 0L, instances)
            } else {
                glDrawElements(mode, drawCount, GL_UNSIGNED_INT, 0L)
            }
        }
    }

    private fun factor(value: Int): Int = when (value) {
        200 -> GL_ZERO
        201 -> GL_ONE
        202 -> GL_SRC_COLOR
        203 -> GL_ONE_MINUS_SRC_COLOR
        204 -> GL_SRC_ALPHA
        205 -> GL_ONE_MINUS_SRC_ALPHA
        else -> GL_ONE
    }
}
