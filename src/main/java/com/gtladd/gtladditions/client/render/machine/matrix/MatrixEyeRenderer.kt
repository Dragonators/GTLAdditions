package com.gtladd.gtladditions.client.render.machine.matrix

import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.server.packs.resources.ResourceProvider
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.gtlcore.gtlcore.config.ConfigHolder
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL13C
import java.util.WeakHashMap
import kotlin.math.cos
import kotlin.math.sin

@OnlyIn(Dist.CLIENT)
object MatrixEyeRenderer {
    const val RADIUS = 7.55f
    const val HEIGHT = 2.8f
    const val SEAT = 1.49f
    private val animations = WeakHashMap<BlockEntity, MatrixEyeAnimation>()
    private val textures = MatrixEyeTextures()
    private val scene = MatrixEyeScene()
    private var texturesReady = false
    private var dome: VertexBuffer? = null
    private var screen: VertexBuffer? = null

    val tint = Vector3f(1f, 0.68f, 0.24f)
    private val sclera = Vector3f(0.020f, 0.012f, 0.004f)
    private val abyss = Vector3f(0.55f, 0.28f, 0.05f)
    private val skin = Vector3f(0.012f, 0.0075f, 0.004f)

    fun sample(blockEntity: BlockEntity, seconds: Double, running: Boolean): MatrixEyeAnimation.Frame =
        animations.getOrPut(blockEntity) { MatrixEyeAnimation() }
            .sample(seconds, running, ConfigHolder.INSTANCE.graphics.enableSmoothAnimations)

    fun forget(blockEntity: BlockEntity) {
        animations.remove(blockEntity)
    }

    fun clearAnimations() = animations.clear()

    /** Approved model coordinates (x,y,z), Z-up, to controller-relative Minecraft coordinates. */
    fun applyTransform(pose: PoseStack, yaw: Float, flipped: Boolean) {
        MatrixFrameTransforms.applyStructure(pose, yaw, flipped)
        pose.translate(0.5, -0.5, 0.5)
        pose.mulPose(Axis.XP.rotationDegrees(-90f))
    }

    fun prepareTextures(resources: ResourceProvider): Boolean = withTextureBindings {
        textures.prepare(resources).also { texturesReady = it }
    }

    private fun ensureTextures(): Boolean = texturesReady || prepareTextures(Minecraft.getInstance().resourceManager)

    /** Called in the shared opaque phase so every mist pass sees all eyes in its depth snapshot. */
    fun renderSurfaces(pose: PoseStack, frame: MatrixEyeAnimation.Frame) {
        val eye = MatrixShaders.eye ?: return
        val lid = MatrixShaders.eyeLid ?: return
        withTextureBindings {
            if (!ensureTextures()) return@withTextureBindings
            RenderUtils.withSavedRenderState {
                RenderSystem.enableDepthTest()
                RenderSystem.depthFunc(GL11C.GL_LEQUAL)
                RenderSystem.depthMask(true)
                RenderSystem.enableCull()
                RenderSystem.disableBlend()
                val view = pose.last().pose()
                val inverse = Matrix4f(view).invert()
                val camera = inverse.transformPosition(Vector3f())
                val toCamera = Vector3f(camera).sub(0f, 0f, SEAT + HEIGHT * 0.5f).normalize()
                val right = inverse.transformDirection(Vector3f(1f, 0f, 0f)).normalize()
                val light = Vector3f(-toCamera.x, -toCamera.y, toCamera.z).fma(-0.12f, right).normalize()
                fun setup(shader: ShaderInstance) {
                    shader.setSampler("uBase", textures["gold_base"])
                    shader.getUniform("cameraPosition")?.set(camera)
                    shader.getUniform("uCamLight")?.set(light)
                    shader.getUniform("uTint")?.set(tint)
                    shader.getUniform("uGaze")?.set(frame.gazeX, frame.gazeY)
                    shader.getUniform("uTime")?.set(frame.time.toFloat())
                    shader.getUniform("uDilate")?.set(frame.dilate)
                    shader.getUniform("uLidU")?.set(frame.upperLid)
                    shader.getUniform("uLidL")?.set(frame.lowerLid)
                    shader.getUniform("uGlow")?.set(frame.light)
                    shader.getUniform("uEnvGlow")?.set(0.12f * frame.light)
                }

                setup(eye)
                eye.getUniform("uScale")?.set(RADIUS, RADIUS, HEIGHT)
                eye.getUniform("uSclera")?.set(sclera)
                eye.getUniform("uAbyss")?.set(abyss)
                eye.setSampler("uData", textures["data"])
                eye.setSampler("uNormal", textures["normal"])
                draw(domeBuffer(), view, eye)
                if (frame.open < 1f) {
                    setup(lid)
                    lid.getUniform("uScale")?.set(RADIUS * 1.03f, RADIUS * 1.03f, HEIGHT * 1.06f)
                    lid.getUniform("uSkin")?.set(skin)
                    lid.getUniform("uSeam")?.set(frame.seam)
                    // One-sided derivative coverage works on Minecraft's single-sample target.
                    // The covered membrane still writes depth and is opaque away from its free edge.
                    RenderSystem.enableBlend()
                    RenderSystem.defaultBlendFunc()
                    draw(domeBuffer(), view, lid)
                }
            }
        }
    }

    fun renderMist(pose: PoseStack, frame: MatrixEyeAnimation.Frame) {
        val shader = MatrixShaders.eyeMist ?: return
        withTextureBindings {
            if (!ensureTextures()) return@withTextureBindings
            RenderUtils.withSavedRenderState {
                if (!scene.capture(shader)) return@withSavedRenderState
                val inverse = Matrix4f(pose.last().pose()).invert()
                shader.getUniform("uInvProj")?.set(Matrix4f(RenderSystem.getProjectionMatrix()).invert())
                shader.getUniform("uCamWorld")?.set(inverse)
                shader.getUniform("uCam")?.set(inverse.transformPosition(Vector3f()))
                shader.getUniform("uTint")?.set(tint)
                shader.getUniform("uShroud")?.set(frame.shroud)
                shader.getUniform("uStir")?.set(frame.stir)
                shader.getUniform("uSwirl")?.set(frame.swirl)
                shader.getUniform("uRise")?.set(frame.rise)
                shader.getUniform("uLight")?.set(frame.light * 0.9f + frame.seam * 0.12f)
                shader.setSampler("tNoise", textures["mist_noise"])
                RenderSystem.disableDepthTest()
                RenderSystem.depthMask(false)
                RenderSystem.disableCull()
                RenderSystem.disableBlend()
                draw(screenBuffer(), Matrix4f(), shader)
            }
        }
    }

    private fun draw(buffer: VertexBuffer, view: Matrix4f, shader: ShaderInstance) {
        RenderUtils.withBoundBuffer(buffer) { buffer.drawWithShader(view, RenderSystem.getProjectionMatrix(), shader) }
    }

    private fun domeBuffer(): VertexBuffer = dome ?: RenderUtils.withStagingBuilder { builder ->
        val rings = 48
        val segments = 192
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION)
        fun vertex(ring: Int, segment: Int) {
            val phi = Math.PI * 0.5 * ring / rings
            val theta = Math.PI * 2.0 * segment / segments
            builder.vertex(sin(phi) * cos(theta), sin(phi) * sin(theta), cos(phi)).endVertex()
        }

        for (i in 0 until rings) {
            for (j in 0 until segments) {
                vertex(i, j)
                vertex(i + 1, j)
                vertex(i, j + 1)
                vertex(i + 1, j)
                vertex(i + 1, j + 1)
                vertex(i, j + 1)
            }
        }

        RenderUtils.uploadStaticBuffer(builder)
    }.also { dome = it }

    private fun screenBuffer(): VertexBuffer = screen ?: RenderUtils.withStagingBuilder { builder ->
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION)
        builder.vertex(-1.0, -1.0, 0.0).endVertex()
        builder.vertex(1.0, -1.0, 0.0).endVertex()
        builder.vertex(1.0, 1.0, 0.0).endVertex()
        builder.vertex(-1.0, 1.0, 0.0).endVertex()
        RenderUtils.uploadStaticBuffer(builder)
    }.also { screen = it }

    /** Custom shader samplers use units 0..2, independently of RenderSystem's SamplerN array. */
    private inline fun <T> withTextureBindings(action: () -> T): T {
        val active = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE)
        val bound = IntArray(3) { index ->
            RenderSystem.activeTexture(GL13C.GL_TEXTURE0 + index)
            GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D)
        }

        try {
            RenderSystem.activeTexture(GL13C.GL_TEXTURE0)
            return action()
        } finally {
            bound.forEachIndexed { index, texture ->
                RenderSystem.activeTexture(GL13C.GL_TEXTURE0 + index)
                GlStateManager._bindTexture(texture)
            }
            RenderSystem.activeTexture(active)
        }
    }

    fun releaseBuffers() {
        dome?.close()
        screen?.close()
        dome = null
        screen = null
        textures.close()
        scene.close()
        texturesReady = false
    }
}