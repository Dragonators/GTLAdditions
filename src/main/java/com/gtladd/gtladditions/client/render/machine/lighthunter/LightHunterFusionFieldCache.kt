package com.gtladd.gtladditions.client.render.machine.lighthunter

import com.gtladd.gtladditions.GTLAdditions
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.preprocessor.GlslPreprocessor
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceProvider
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL12C
import org.lwjgl.opengl.GL13C
import org.lwjgl.opengl.GL15C
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL30C
import org.lwjgl.opengl.GL42C
import org.lwjgl.opengl.GL43C
import kotlin.math.abs
import kotlin.math.floor

class LightHunterFusionFieldCache : AutoCloseable {
    private var resources: ResourceProvider? = null
    private var program = 0
    private val fields = ArrayList<Field>(MAX_FIELDS)
    private var firstSlotUniform = -1
    private var noiseOffsetUniform = -1
    private var cellCountUniform = -1
    private var sizeUniform = -1
    private var frame = 0L
    private var useSequence = 0L
    private var unavailable = false

    fun beginFrame() {
        frame++
    }

    fun setResources(provider: ResourceProvider) {
        close()
        resources = provider
        unavailable = false
    }

    fun prepare(axialCell: Double): Field? {
        if (unavailable || !GL.getCapabilities().OpenGL43) return null
        val provider = resources ?: return null
        val originCell = floor(axialCell / ORIGIN_STRIDE).toLong() * ORIGIN_STRIDE - AXIAL_PADDING
        try {
            if (program == 0) initialize(provider)
            val field = fields.firstOrNull { it.lastUse != 0L && it.originCell == originCell } ?: nextField()
            if (field.lastUse == 0L || field.originCell != originCell) {
                update(field, originCell)
                field.originCell = originCell
            }

            field.lastFrame = frame
            field.lastUse = ++useSequence
            return field
        } catch (error: Exception) {
            close()
            unavailable = true
            GTLAdditions.LOGGER.warn("Light Hunter fusion cache unavailable; using procedural sampling until resources reload.", error)
            return null
        }
    }

    private fun nextField(): Field {
        val oldest = fields.minByOrNull { it.lastUse }
        if (oldest != null && (oldest.lastFrame != frame || fields.size == MAX_FIELDS)) return oldest
        return Field(allocateTexture()).also { fields.add(it) }
    }

    private fun initialize(provider: ResourceProvider) {
        val source = provider.openAsReader(GTLAdditions.id("shaders/compute/light_hunter_fusion.csh")).use { it.readText() }
        val imported = hashSetOf<ResourceLocation>()
        val preprocessor = object : GlslPreprocessor() {
            override fun applyImport(useFullPath: Boolean, path: String): String? {
                val id = ResourceLocation(path)
                val include = ResourceLocation(id.namespace, "shaders/include/${id.path}")
                return if (imported.add(include)) provider.openAsReader(include).use { it.readText() } else null
            }
        }

        val compute = GL20C.glCreateShader(GL43C.GL_COMPUTE_SHADER)
        try {
            GL20C.glShaderSource(compute, preprocessor.process(source).joinToString(""))
            GL20C.glCompileShader(compute)
            check(GL20C.glGetShaderi(compute, GL20C.GL_COMPILE_STATUS) != 0) { GL20C.glGetShaderInfoLog(compute) }
            program = GL20C.glCreateProgram()
            GL20C.glAttachShader(program, compute)
            GL20C.glLinkProgram(program)
            check(GL20C.glGetProgrami(program, GL20C.GL_LINK_STATUS) != 0) { GL20C.glGetProgramInfoLog(program) }
            firstSlotUniform = GL20C.glGetUniformLocation(program, "FirstSlot")
            noiseOffsetUniform = GL20C.glGetUniformLocation(program, "FusionNoiseOffset")
            cellCountUniform = GL20C.glGetUniformLocation(program, "CellCount")
            sizeUniform = GL20C.glGetUniformLocation(program, "FieldSize")
        } finally {
            GL20C.glDeleteShader(compute)
        }
    }

    private fun allocateTexture(): Int {
        val activeTexture = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE)
        RenderSystem.activeTexture(GL13C.GL_TEXTURE2)
        val previous = GL11C.glGetInteger(GL12C.GL_TEXTURE_BINDING_3D)
        val texture = GL11C.glGenTextures()
        try {
            GL11C.glBindTexture(GL12C.GL_TEXTURE_3D, texture)
            // Immutable storage does not interpret a caller's pixel-unpack buffer as input.
            GL42C.glTexStorage3D(GL12C.GL_TEXTURE_3D, 1, GL30C.GL_RG16F, ANGULAR, RADIAL, AXIAL)
            check(GL11C.glGetTexLevelParameteri(GL12C.GL_TEXTURE_3D, 0, GL11C.GL_TEXTURE_WIDTH) == ANGULAR) {
                "Could not allocate the Light Hunter fusion field"
            }

            GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_LINEAR)
            GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_LINEAR)
            GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL11C.GL_TEXTURE_WRAP_S, GL11C.GL_REPEAT)
            GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL11C.GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE)
            GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL12C.GL_TEXTURE_WRAP_R, GL11C.GL_REPEAT)
            return texture
        } catch (error: Exception) {
            GL11C.glDeleteTextures(texture)
            throw error
        } finally {
            GL11C.glBindTexture(GL12C.GL_TEXTURE_3D, previous)
            RenderSystem.activeTexture(activeTexture)
        }
    }

    private fun update(field: Field, originCell: Long) {
        val initialized = field.lastUse != 0L
        val previousOrigin = field.originCell
        // The axial axis is a ring buffer.
        val delta = if (!initialized) AXIAL.toLong() else originCell - previousOrigin
        val partial = initialized && abs(delta) < AXIAL
        val count = if (partial) abs(delta).toInt() else AXIAL
        val first = if (partial && delta > 0) previousOrigin + AXIAL else originCell
        val activeTexture = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE)
        val previousProgram = GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM)
        val image = GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_NAME, 0)
        val level = GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_LEVEL, 0)
        val layered = GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_LAYERED, 0) != 0
        val layer = GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_LAYER, 0)
        val access = GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_ACCESS, 0)
        val format = GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_FORMAT, 0)
        try {
            // Finish earlier reads before reusing a field for another animation phase.
            GL42C.glMemoryBarrier(GL42C.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT)
            GlStateManager._glUseProgram(program)
            GL20C.glUniform1i(firstSlotUniform, Math.floorMod(first, AXIAL))
            GL20C.glUniform1fv(noiseOffsetUniform, noiseOffsets(first * AXIAL_STEP))
            GL20C.glUniform1i(cellCountUniform, count)
            GL20C.glUniform3i(sizeUniform, ANGULAR, RADIAL, AXIAL)
            GL42C.glBindImageTexture(0, field.texture, 0, true, 0, GL15C.GL_WRITE_ONLY, GL30C.GL_RG16F)
            GL43C.glDispatchCompute(ANGULAR / 8, RADIAL / 4, (count + 3) / 4)
            GL42C.glMemoryBarrier(GL42C.GL_TEXTURE_FETCH_BARRIER_BIT or GL42C.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT)
        } finally {
            GL42C.glBindImageTexture(0, image, level, layered, layer, access, format)
            GlStateManager._glUseProgram(previousProgram)
            RenderSystem.activeTexture(activeTexture)
        }
    }

    override fun close() {
        fields.forEach { GL11C.glDeleteTextures(it.texture) }
        fields.clear()
        if (program != 0) GL20C.glDeleteProgram(program)
        program = 0
        frame = 0
        useSequence = 0
    }

    class Field internal constructor(val texture: Int) {
        internal var originCell = 0L
        internal var lastFrame = -1L
        internal var lastUse = 0L
        val originSlot: Float get() = Math.floorMod(originCell, AXIAL).toFloat()
    }

    companion object {
        private const val ANGULAR = 2048
        private const val RADIAL = 48
        private const val AXIAL = 128
        const val AXIAL_STEP = 0.078125
        private const val ORIGIN_STRIDE = 8
        private const val AXIAL_PADDING = 58
        private const val MAX_FIELDS = 4
        private val offsets = FloatArray(10)

        /** Axial translation of each noise octave, shared by the analytic and cached field. */
        internal fun noiseOffsets(z: Double): FloatArray {
            fun octave(start: Int, scale: Double) {
                offsets[start] = (z * scale % 1024.0).toFloat()
                offsets[start + 1] = (z * scale * 2.03 % 1024.0).toFloat()
                offsets[start + 2] = (z * scale * 4.07 % 1024.0).toFloat()
            }
            octave(0, 0.58)
            octave(3, 0.63)
            octave(6, 1.0)
            offsets[9] = (z * 2.1 % 1024.0).toFloat()
            return offsets
        }
    }
}
