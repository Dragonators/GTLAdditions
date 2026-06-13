package com.gtladd.gtladditions.client.render.machine.antichrist

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.RenderMode
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredOculusCompat
import com.gtladd.gtladditions.client.render.withPose
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OnlyIn(Dist.CLIENT)
object AntichristStarRenderer {
    private const val SPHERE_SLICES = 128
    private const val SPHERE_STACKS = 128

    private val STAR_LAYER_0 = GTLAdditions.id("textures/block/multiblock/forge_of_antichrist/star_layer_0.png")
    private val STAR_LAYER_1 = GTLAdditions.id("textures/block/multiblock/forge_of_antichrist/star_layer_1.png")
    private val STAR_LAYER_2 = GTLAdditions.id("textures/block/multiblock/forge_of_antichrist/star_layer_2.png")

    private val LAYER_0_AXIS = Vector3f(0.0f, 1.0f, 1.0f).normalize()
    private val LAYER_1_AXIS = Vector3f(1.0f, 1.0f, 0.0f).normalize()
    private val LAYER_2_AXIS = Vector3f(1.0f, 0.0f, 1.0f).normalize()

    private val sphereBuffer: VertexBuffer by lazy {
        buildSphereBuffer(SPHERE_SLICES, SPHERE_STACKS)
    }

    fun renderOpaque(profile: AntichristRenderProfile, poseStack: PoseStack) {
        renderOpaque(profile, poseStack, STAR_LAYER_0)
    }

    fun renderOpaque(profile: AntichristRenderProfile, poseStack: PoseStack, texture: ResourceLocation) {
        RenderSystem.enableDepthTest()
        RenderSystem.depthMask(true)
        renderLayer(profile, poseStack, texture, profile.starRadius, LAYER_0_AXIS, 130f, 1.0f)
    }

    fun renderOpaqueOriginalColor(profile: AntichristRenderProfile, poseStack: PoseStack, texture: ResourceLocation) {
        RenderSystem.enableDepthTest()
        RenderSystem.depthMask(true)
        renderLayerOriginalColor(profile, poseStack, texture, profile.starRadius, LAYER_0_AXIS, 130f)
    }

    fun renderTransparent(profile: AntichristRenderProfile, poseStack: PoseStack) {
        RenderSystem.enableBlend()
        RenderSystem.blendFunc(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        )
        RenderSystem.enableDepthTest()
        RenderSystem.depthMask(false)
        renderLayer(profile, poseStack, STAR_LAYER_1, profile.starRadius * 1.02f, LAYER_1_AXIS, -49f, 0.4f)
        renderLayer(profile, poseStack, STAR_LAYER_2, profile.starRadius * 1.04f, LAYER_2_AXIS, 67f, 0.2f)
        RenderSystem.depthMask(true)
        RenderSystem.disableBlend()
    }

    private fun renderLayer(
        profile: AntichristRenderProfile,
        poseStack: PoseStack,
        texture: ResourceLocation,
        radius: Float,
        axis: Vector3f,
        baseRotation: Float,
        alpha: Float
    ) {
        val shader = AntichristShaders.starShader ?: return
        val rotationSpeedMultiplier = when (profile.renderMode) {
            RenderMode.RAINBOW -> 1.7f
            RenderMode.COLLAPSING -> 2.6f
            else -> 1.0f
        }
        val rotationDegrees = baseRotation + (profile.tick * rotationSpeedMultiplier) % 360000f

        poseStack.withPose {
            translate(profile.starPos.x, profile.starPos.y, profile.starPos.z)
            mulPose(Quaternionf().fromAxisAngleDeg(axis.x, axis.y, axis.z, rotationDegrees))
            scale(radius, radius, radius)

            RenderSystem.disableCull()
            RenderSystem.setShaderTexture(0, texture)
            shader.getUniform("Color")?.set(profile.colorR, profile.colorG, profile.colorB, alpha)
            shader.getUniform("Gamma")?.set(AntichristRenderProfile.STAR_GAMMA)

            sphereBuffer.bind()
            DeferredOculusCompat.withDeferredShaderPass {
                sphereBuffer.drawWithShader(last().pose(), RenderSystem.getProjectionMatrix(), shader)
            }
            VertexBuffer.unbind()
            RenderSystem.enableCull()
        }
    }

    private fun renderLayerOriginalColor(
        profile: AntichristRenderProfile,
        poseStack: PoseStack,
        texture: ResourceLocation,
        radius: Float,
        axis: Vector3f,
        baseRotation: Float
    ) {
        val rotationSpeedMultiplier = when (profile.renderMode) {
            RenderMode.RAINBOW -> 1.7f
            RenderMode.COLLAPSING -> 2.6f
            else -> 1.0f
        }
        val rotationDegrees = baseRotation + (profile.tick * rotationSpeedMultiplier) % 360000f

        poseStack.withPose {
            translate(profile.starPos.x, profile.starPos.y, profile.starPos.z)
            mulPose(Quaternionf().fromAxisAngleDeg(axis.x, axis.y, axis.z, rotationDegrees))
            scale(radius, radius, radius)

            RenderSystem.disableCull()
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
            RenderSystem.setShaderTexture(0, texture)

            sphereBuffer.bind()
            DeferredOculusCompat.withDeferredShaderPass {
                RenderSystem.setShader(GameRenderer::getPositionTexShader)
                val shader = GameRenderer.getPositionTexShader() ?: return@withDeferredShaderPass
                sphereBuffer.drawWithShader(last().pose(), RenderSystem.getProjectionMatrix(), shader)
            }
            VertexBuffer.unbind()
            RenderSystem.enableCull()
        }
    }

    private fun buildSphereBuffer(slices: Int, stacks: Int): VertexBuffer {
        val buffer = VertexBuffer(VertexBuffer.Usage.STATIC)
        val builder = Tesselator.getInstance().builder
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX)

        for (stack in 0 until stacks) {
            val v0 = stack.toDouble() / stacks.toDouble()
            val v1 = (stack + 1).toDouble() / stacks.toDouble()
            val phi0 = PI / 2.0 - stack * PI / stacks
            val phi1 = PI / 2.0 - (stack + 1) * PI / stacks

            val y0 = sin(phi0)
            val y1 = sin(phi1)
            val r0 = cos(phi0)
            val r1 = cos(phi1)

            for (slice in 0 until slices) {
                val u0 = slice.toDouble() / slices.toDouble()
                val u1 = (slice + 1).toDouble() / slices.toDouble()
                val uu0 = 1.0 - u0
                val uu1 = 1.0 - u1
                val theta0 = slice * 2.0 * PI / slices
                val theta1 = (slice + 1) * 2.0 * PI / slices

                val x00 = r0 * cos(theta0)
                val z00 = r0 * sin(theta0)
                val x10 = r1 * cos(theta0)
                val z10 = r1 * sin(theta0)
                val x11 = r1 * cos(theta1)
                val z11 = r1 * sin(theta1)
                val x01 = r0 * cos(theta1)
                val z01 = r0 * sin(theta1)

                addVertex(builder, x00, y0, z00, uu0, v0)
                addVertex(builder, x10, y1, z10, uu0, v1)
                addVertex(builder, x11, y1, z11, uu1, v1)
                addVertex(builder, x00, y0, z00, uu0, v0)
                addVertex(builder, x11, y1, z11, uu1, v1)
                addVertex(builder, x01, y0, z01, uu1, v0)
            }
        }

        buffer.bind()
        buffer.upload(builder.end())
        VertexBuffer.unbind()
        return buffer
    }

    private fun addVertex(builder: BufferBuilder, x: Double, y: Double, z: Double, u: Double, v: Double) {
        builder.vertex(x, y, z).uv(u.toFloat(), v.toFloat()).endVertex()
    }
}