package com.gtladd.gtladditions.client.render.machine.dimensionfocus

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.render.machine.DimensionFocusInfinityCraftingArrayRenderer.Companion.ORBIT_PERIOD_SECONDS
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredMachineRenderer
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredOculusCompat
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderContext
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderEntry
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.common.machine.multiblock.controller.DimensionFocusInfinityCraftingArray
import com.gtladd.gtladditions.common.machine.multiblock.structure.layout.DimensionFocusBlockLayout
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.math.Axis
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.core.Direction
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RegisterShadersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.joml.Vector3f
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL12C
import org.lwjgl.opengl.GL13C
import org.lwjgl.opengl.GL32C
import org.lwjgl.opengl.GL33C
import java.io.IOException
import java.util.WeakHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

@OnlyIn(Dist.CLIENT)
internal object DimensionFocusPocketRenderer {
    private const val RADIUS = 20.8f
    private const val SPHERE_SEGMENTS = 96
    private const val SPHERE_RINGS = 48
    private const val IDLE_LEVEL = 0.6f
    private const val LEVEL_RISE_SECONDS = 0.3
    private const val LEVEL_FALL_SECONDS = 1.2
    private const val SPIN_RISE_SECONDS = 1.6
    private const val SPIN_FALL_SECONDS = 3.0
    private const val NEEDLE_SYMBOLS = "KLM"
    private const val NOISE_UNIT = 2
    private const val GAS_NEAR_UNIT = 3
    private const val GAS_DEEP_UNIT = 4

    var pocketShader: ShaderInstance? = null
    var needleShader: ShaderInstance? = null
    private val states = WeakHashMap<BlockEntity, State>()
    private val sphere by lazy(::buildSphere)
    private val needles by lazy(::buildNeedles)
    private val cameraLocal = Vector3f()

    fun enqueue(blockEntity: BlockEntity, machine: DimensionFocusInfinityCraftingArray, ticks: Double) {
        DeferredMachineRenderer.enqueue(Entry(blockEntity, machine, ticks))
    }

    private fun draw(context: DeferredRenderContext, entry: Entry) {
        val pocket = pocketShader ?: return
        val needle = needleShader ?: return
        val textures = DimensionFocusPocketTextures.current ?: return
        val state = update(entry)
        val ticks = entry.ticks
        val pos = entry.blockEntity.blockPos
        val facing = Axis.YP.rotationDegrees(DimensionFocusOrbitGeometry.yaw(entry.machine.frontFacing))
        val offset = DimensionFocusBlockLayout.centerOffset
        // Same transform chain as DimensionFocusOrbitRenderer: block centre, facing, structure centre.
        val origin = facing.transform(Vector3f(offset.x.toFloat(), offset.y.toFloat(), offset.z.toFloat())).add(0.5f, 0.5f, 0.5f)
        cameraLocal.set(
            (context.cameraPosition.x - pos.x).toFloat() - origin.x,
            (context.cameraPosition.y - pos.y).toFloat() - origin.y,
            (context.cameraPosition.z - pos.z).toFloat() - origin.z
        )
        facing.transformInverse(cameraLocal)

        for (shader in arrayOf(pocket, needle)) {
            shader.getUniform("Time")?.set((ticks / 20.0).toFloat())
            shader.getUniform("Flow")?.set(state.flow.toFloat())
            shader.getUniform("Spin")?.set(state.spin)
            shader.getUniform("Level")?.set(state.level)
            shader.getUniform("Align")?.set(DimensionFocusOrbitGeometry.alignment(ticks))
            shader.getUniform("CameraLocal")?.set(cameraLocal.x, cameraLocal.y, cameraLocal.z)
        }

        RenderUtils.withSavedRenderState {
            textures.bind {
                RenderSystem.enableBlend()
                RenderSystem.blendFuncSeparate(
                    GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ZERO,
                    GlStateManager.DestFactor.ONE
                )
                RenderSystem.enableDepthTest()
                RenderSystem.depthFunc(GL11C.GL_LEQUAL)
                RenderSystem.depthMask(false)
                context.renderRelativeToCamera(entry.blockEntity) {
                    translate(0.5, 0.5, 0.5)
                    mulPose(facing)
                    translate(offset.x.toDouble(), offset.y.toDouble(), offset.z.toDouble())
                    DeferredOculusCompat.withDeferredShaderPass {
                        // Inward-wound triangles: back-face culling keeps the far half of the sphere, so every ray enters the pocket once.
                        RenderSystem.enableCull()
                        RenderUtils.withBoundBuffer(sphere) { sphere.drawWithShader(last().pose(), RenderSystem.getProjectionMatrix(), pocket) }
                        // The needle faces lie exactly on the terrain's; hidden ones fail the depth test, so culling can stay off.
                        RenderSystem.disableCull()
                        RenderSystem.enablePolygonOffset()
                        RenderSystem.polygonOffset(-1f, -1f)
                        try {
                            needles.forEachIndexed { layer, buffer ->
                                val angle = DimensionFocusOrbitGeometry.angleDegrees(layer, ticks)
                                needle.getUniform("LayerAngle")?.set(Math.toRadians(angle.toDouble()).toFloat())
                                withPose {
                                    mulPose(Axis.YP.rotationDegrees(angle))
                                    RenderUtils.withBoundBuffer(buffer) { buffer.drawWithShader(last().pose(), RenderSystem.getProjectionMatrix(), needle) }
                                }
                            }
                        } finally {
                            RenderSystem.polygonOffset(0f, 0f)
                            RenderSystem.disablePolygonOffset()
                        }
                    }
                }
            }
        }
    }

    /**
     * Working machines glow fully and their pocket flows; idle ones dim and the flow nearly stops. Eased over game time, so a pause freezes it with the blocks
     * and one-tick recipes do not flicker. The flow is integrated rather than derived from the tick, so a state change bends the motion instead of jumping it.
     */
    private fun update(entry: Entry): State {
        val working = entry.machine.isActive && entry.machine.isWorkingEnabled
        val level = if (working) 1f else IDLE_LEVEL
        val spin = if (working) 1f else 0f
        val state = states.getOrPut(entry.blockEntity) { State(entry.ticks, level, spin) }
        val seconds = ((entry.ticks - state.ticks) / 20.0).coerceIn(0.0, 1.0)
        state.ticks = entry.ticks
        state.level = ease(state.level, level, seconds, LEVEL_RISE_SECONDS, LEVEL_FALL_SECONDS)
        state.spin = ease(state.spin, spin, seconds, SPIN_RISE_SECONDS, SPIN_FALL_SECONDS)
        state.flow += seconds * (0.22 + 1.38 * state.spin) * 2.0 * PI / ORBIT_PERIOD_SECONDS

        return state
    }

    private fun ease(current: Float, target: Float, seconds: Double, rise: Double, fall: Double): Float =
        current + (target - current) * (1.0 - exp(-seconds / if (target > current) rise else fall)).toFloat()

    private fun buildSphere(): VertexBuffer {
        val builder = BufferBuilder(SPHERE_SEGMENTS * SPHERE_RINGS * 6 * 12)
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION)
        fun point(i: Int, j: Int): Vector3f {
            val theta = PI * j / SPHERE_RINGS
            val phi = 2.0 * PI * i / SPHERE_SEGMENTS
            return Vector3f((sin(theta) * sin(phi)).toFloat(), cos(theta).toFloat(), (sin(theta) * cos(phi)).toFloat()).mul(RADIUS)
        }

        fun triangle(a: Vector3f, b: Vector3f, c: Vector3f) {
            val normal = Vector3f(b).sub(a).cross(Vector3f(c).sub(a))
            if (normal.lengthSquared() < 1e-6f) return
            val (second, third) = if (normal.dot(Vector3f(a).add(b).add(c)) > 0f) c to b else b to c

            for (vertex in arrayOf(a, second, third)) {
                builder.vertex(vertex.x.toDouble(), vertex.y.toDouble(), vertex.z.toDouble()).endVertex()
            }
        }

        for (j in 0 until SPHERE_RINGS) {
            for (i in 0 until SPHERE_SEGMENTS) {
                val a = point(i, j)
                val b = point(i + 1, j)
                val c = point(i + 1, j + 1)
                val d = point(i, j + 1)
                triangle(a, b, c)
                triangle(a, c, d)
            }
        }
        return RenderUtils.uploadStaticBuffer(builder)
    }

    /** In each rotating layer's own frame. */
    private fun buildNeedles(): List<VertexBuffer> = DimensionFocusBlockLayout.layers.map { cells ->
        val occupied = cells.mapTo(HashSet()) { it.position }
        val builder = BufferBuilder(16384)
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_NORMAL)
        for (cell in cells) {
            if (cell.symbol !in NEEDLE_SYMBOLS) continue
            for (face in Direction.entries) {
                if (cell.position.relative(face) in occupied) continue
                val normal = face.normal
                val (u, v) = when (face.axis) {
                    Direction.Axis.X -> Vector3f(0f, 0.5f, 0f) to Vector3f(0f, 0f, 0.5f)
                    Direction.Axis.Y -> Vector3f(0.5f, 0f, 0f) to Vector3f(0f, 0f, 0.5f)
                    else -> Vector3f(0.5f, 0f, 0f) to Vector3f(0f, 0.5f, 0f)
                }
                val centre = Vector3f(
                        cell.position.x + normal.x * 0.5f,
                        cell.position.y + normal.y * 0.5f,
                        cell.position.z + normal.z * 0.5f
                    )
                for ((a, b) in CORNERS) {
                    builder.vertex(
                            (centre.x + u.x * a + v.x * b).toDouble(),
                            (centre.y + u.y * a + v.y * b).toDouble(),
                            (centre.z + u.z * a + v.z * b).toDouble()
                        )
                        .color(1f, 1f, 1f, 1f)
                        .normal(normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat())
                        .endVertex()
                }
            }
        }
        RenderUtils.uploadStaticBuffer(builder)
    }

    private val CORNERS = arrayOf(-1f to -1f, 1f to -1f, 1f to 1f, -1f to 1f)

    private class State(var ticks: Double, var level: Float, var spin: Float, var flow: Double = 0.0)

    private class Entry(
        override val blockEntity: BlockEntity,
        val machine: DimensionFocusInfinityCraftingArray,
        val ticks: Double
    ) : DeferredRenderEntry {
        override val renderOrder: Int = 20

        override fun isVisible(context: DeferredRenderContext): Boolean =
            !blockEntity.isRemoved &&
                machine.isFormed &&
                (context.frustum?.isVisible(DimensionFocusOrbitGeometry.bounds(machine.pos, machine.frontFacing)) != false)

        override fun render(context: DeferredRenderContext) {
            draw(context, this)
        }
    }

    private fun DimensionFocusPocketTextures.Loaded.bind(draw: () -> Unit) {
        val active = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE)
        val seamless = GL11C.glIsEnabled(GL32C.GL_TEXTURE_CUBE_MAP_SEAMLESS)
        val bindings = arrayOf(
            Triple(NOISE_UNIT, GL12C.GL_TEXTURE_3D, noise),
            Triple(GAS_NEAR_UNIT, GL13C.GL_TEXTURE_CUBE_MAP, gasNear),
            Triple(GAS_DEEP_UNIT, GL13C.GL_TEXTURE_CUBE_MAP, gasDeep)
        )
        val previous = bindings.map { (unit, target, _) ->
            RenderSystem.activeTexture(GL13C.GL_TEXTURE0 + unit)
            val texture = GL11C.glGetInteger(
                if (target == GL12C.GL_TEXTURE_3D) GL12C.GL_TEXTURE_BINDING_3D else GL13C.GL_TEXTURE_BINDING_CUBE_MAP
            )
            texture to GL11C.glGetInteger(GL33C.GL_SAMPLER_BINDING)
        }

        try {
            for ((unit, target, texture) in bindings) {
                RenderSystem.activeTexture(GL13C.GL_TEXTURE0 + unit)
                GL11C.glBindTexture(target, texture)
                GL33C.glBindSampler(unit, 0)
            }

            GL11C.glEnable(GL32C.GL_TEXTURE_CUBE_MAP_SEAMLESS)
            RenderSystem.activeTexture(active)
            draw()
        } finally {
            if (!seamless) GL11C.glDisable(GL32C.GL_TEXTURE_CUBE_MAP_SEAMLESS)
            bindings.forEachIndexed { i, (unit, target, _) ->
                RenderSystem.activeTexture(GL13C.GL_TEXTURE0 + unit)
                GL11C.glBindTexture(target, previous[i].first)
                GL33C.glBindSampler(unit, previous[i].second)
            }

            RenderSystem.activeTexture(active)
        }
    }
}

@Suppress("unused")
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
internal object DimensionFocusPocketShaderEvents {
    @SubscribeEvent
    @JvmStatic
    fun onRegisterShaders(event: RegisterShadersEvent) {
        val textures = try {
            DimensionFocusPocketTextures.read(event.resourceProvider)
        } catch (exception: IOException) {
            // Without its maps the pocket is skipped; the blocks and their rotation are unaffected.
            GTLAdditions.LOGGER.warn("Unable to read Dimension Focus pocket textures", exception)
            null
        }
        event.registerShader(
            ShaderInstance(event.resourceProvider,
            GTLAdditions.id("gtladditions_dimension_focus_pocket"),
            DefaultVertexFormat.POSITION
        )
        ) { shader ->
            if (textures != null) DimensionFocusPocketTextures.upload(textures)
            DimensionFocusPocketRenderer.pocketShader = shader
        }
        event.registerShader(
            ShaderInstance(event.resourceProvider,
            GTLAdditions.id("gtladditions_dimension_focus_pocket_needles"),
            DefaultVertexFormat.POSITION_COLOR_NORMAL
        )
        ) { DimensionFocusPocketRenderer.needleShader = it }
    }
}