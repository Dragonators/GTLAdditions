package com.gtladd.gtladditions.client.render.machine.lighthunter

import com.gtladd.gtladditions.GTLAdditions
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.preprocessor.GlslPreprocessor
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceProvider
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL12C
import org.lwjgl.opengl.GL13C
import org.lwjgl.opengl.GL15C
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL30C
import org.lwjgl.opengl.GL33C
import org.lwjgl.opengl.GL42C
import org.lwjgl.opengl.GL43C

class LightHunterPlasmaFieldCache : AutoCloseable {
    private var resources: ResourceProvider? = null
    private var program = 0
    private val fields = ArrayList<Field>(MAX_FIELDS)
    private var phaseUniform = -1
    private val phase = FloatArray(10)

    private var noiseUniform = -1
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

    private fun setPhases(shader: ShaderInstance, tick: Double) {
        val seconds = tick / 20.0
        fun offset(speed: Double) = (seconds * speed % 64.0).toFloat()
        val fiber = -31.0 * 0.090
        val wide = -31.0 * 0.065
        phase[0] = offset(-31.0 * 0.055)
        phase[1] = offset(fiber)
        phase[2] = offset(fiber * 2.07)
        phase[3] = offset(fiber * 4.19)
        phase[4] = offset(wide)
        phase[5] = offset(wide * 2.07)
        phase[6] = offset(wide * 4.19)
        phase[7] = offset(fiber * 2.25 - 1.4)
        phase[8] = offset(0.42)
        phase[9] = offset(-31.0 * 0.18)
        shader.getUniform("PlasmaPhase")?.set(phase)
    }

    fun prepare(shader: ShaderInstance, noiseTexture: Int, tick: Double): Int {
        setPhases(shader, tick)
        if (unavailable || !GL.getCapabilities().OpenGL43) return 0
        val provider = resources ?: return 0
        try {
            if (program == 0) initialize(provider)
            val bits = tick.toRawBits()
            val field = fields.firstOrNull { it.timeBits == bits && it.noiseTexture == noiseTexture } ?: nextField()
            if (field.timeBits != bits || field.noiseTexture != noiseTexture) {
                update(field.texture, noiseTexture)
                field.timeBits = bits
                field.noiseTexture = noiseTexture
            }

            field.lastFrame = frame
            field.lastUse = ++useSequence
            return field.texture
        } catch (error: Exception) {
            close()
            unavailable = true
            GTLAdditions.LOGGER.warn("Light Hunter plasma cache unavailable; using procedural sampling until resources reload.", error)
            return 0
        }
    }

    private fun nextField(): Field {
        val oldest = fields.minByOrNull { it.lastUse }
        if (oldest != null && (oldest.lastFrame != frame || fields.size == MAX_FIELDS)) return oldest
        return Field(allocateTexture()).also { fields.add(it) }
    }

    private fun initialize(provider: ResourceProvider) {
        val source = provider.openAsReader(GTLAdditions.id("shaders/compute/light_hunter_plasma.csh")).use { it.readText() }
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
            phaseUniform = GL20C.glGetUniformLocation(program, "PlasmaPhase")
            noiseUniform = GL20C.glGetUniformLocation(program, "PlasmaNoise")
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
            GL42C.glTexStorage3D(GL12C.GL_TEXTURE_3D, 1, GL30C.GL_RGBA16F, WIDTH, HEIGHT, HEIGHT)
            check(GL11C.glGetTexLevelParameteri(GL12C.GL_TEXTURE_3D, 0, GL11C.GL_TEXTURE_WIDTH) == WIDTH) {
                "Could not allocate the Light Hunter plasma field"
            }

            GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_LINEAR)
            GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_LINEAR)
            GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL11C.GL_TEXTURE_WRAP_S, GL12C.GL_CLAMP_TO_EDGE)
            GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL11C.GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE)
            GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL12C.GL_TEXTURE_WRAP_R, GL12C.GL_CLAMP_TO_EDGE)
            return texture
        } catch (error: Exception) {
            GL11C.glDeleteTextures(texture)
            throw error
        } finally {
            GL11C.glBindTexture(GL12C.GL_TEXTURE_3D, previous)
            RenderSystem.activeTexture(activeTexture)
        }
    }

    private fun update(texture: Int, noiseTexture: Int) {
        val activeTexture = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE)
        val previousProgram = GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM)
        val image = GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_NAME, 0)
        val level = GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_LEVEL, 0)
        val layered = GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_LAYERED, 0) != 0
        val layer = GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_LAYER, 0)
        val access = GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_ACCESS, 0)
        val format = GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_FORMAT, 0)
        val sampler = GL30C.glGetIntegeri(GL33C.GL_SAMPLER_BINDING, 1)
        RenderSystem.activeTexture(GL13C.GL_TEXTURE1)
        val previousNoise = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D)
        try {
            // Finish earlier reads before reusing a field for another animation phase.
            GL42C.glMemoryBarrier(GL42C.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT)
            GlStateManager._bindTexture(noiseTexture)
            GL33C.glBindSampler(1, 0)
            GlStateManager._glUseProgram(program)
            GL20C.glUniform1i(noiseUniform, 1)
            GL20C.glUniform1fv(phaseUniform, phase)
            GL20C.glUniform3i(sizeUniform, WIDTH, HEIGHT, HEIGHT)
            GL42C.glBindImageTexture(0, texture, 0, true, 0, GL15C.GL_WRITE_ONLY, GL30C.GL_RGBA16F)
            GL43C.glDispatchCompute(WIDTH / 8, HEIGHT / 4, HEIGHT / 4)
            GL42C.glMemoryBarrier(GL42C.GL_TEXTURE_FETCH_BARRIER_BIT or GL42C.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT)
        } finally {
            GL42C.glBindImageTexture(0, image, level, layered, layer, access, format)
            GlStateManager._glUseProgram(previousProgram)
            GL33C.glBindSampler(1, sampler)
            GlStateManager._bindTexture(previousNoise)
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

    private class Field(val texture: Int) {
        var timeBits: Long? = null
        var noiseTexture = 0
        var lastFrame = -1L
        var lastUse = 0L
    }

    private companion object {
        const val WIDTH = 192
        const val HEIGHT = 96
        const val MAX_FIELDS = 4
    }
}
