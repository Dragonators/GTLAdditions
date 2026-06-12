package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.client.render.machine.deferred.DeferredOculusCompat
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexBuffer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Matrix4f
import org.lwjgl.opengl.GL30C

@OnlyIn(Dist.CLIENT)
internal interface HeartBlackHoleRenderEntry {
    val blockEntity: BlockEntity
    val profile: HeartBlackHoleRenderProfile
}

@OnlyIn(Dist.CLIENT)
object HeartBlackHoleRenderer {
    private val IDENTITY_MATRIX = Matrix4f()
    private val EMPTY_PROFILE = HeartBlackHoleRenderProfile(0.0f, Vec3.ZERO, 0.0f, 0.0f, 1.0f)
    private const val VOLUME_PASS_EFFECT = 0.0f
    private const val VOLUME_PASS_MASK = 1.0f
    private val drawCommandDistanceComparator = Comparator<HeartDrawCommand> { first, second ->
        second.distanceToCameraSqr.compareTo(first.distanceToCameraSqr)
    }
    private val drawCommandPool = ArrayList<HeartDrawCommand>()
    private val drawCommands = ArrayList<HeartDrawCommand>()
    private var pendingComposite: PendingHeartComposite? = null

    fun render(
        profile: HeartBlackHoleRenderProfile,
        poseStack: PoseStack,
        blockEntity: BlockEntity,
        cameraPosition: Vec3
    ) {
        render(listOf(SingleHeartBlackHoleRenderEntry(blockEntity, profile)), poseStack, cameraPosition)
    }

    internal fun render(
        entries: List<HeartBlackHoleRenderEntry>,
        poseStack: PoseStack,
        cameraPosition: Vec3
    ) {
        try {
            if (!prepareDrawCommands(entries, cameraPosition)) return

            val volumeShader = HeartBlackHoleShaders.volumeShader ?: return
            val blurShader = HeartBlackHoleShaders.blurShader ?: return
            HeartBlackHoleShaders.compositeShader ?: return

            DeferredOculusCompat.withDeferredShaderPass {
                renderPostProcessed(
                    drawCommands,
                    poseStack,
                    volumeShader,
                    blurShader
                )
            }
        } finally {
            releaseDrawCommands()
        }
    }

    private fun prepareDrawCommands(entries: List<HeartBlackHoleRenderEntry>, cameraPosition: Vec3): Boolean {
        releaseDrawCommands()
        entries.forEach { entry ->
            val blockEntity = entry.blockEntity
            if (blockEntity.isRemoved) return@forEach

            val blockPos = blockEntity.blockPos
            val profile = entry.profile
            val center = profile.center
            val centerX = blockPos.x + center.x
            val centerY = blockPos.y + center.y
            val centerZ = blockPos.z + center.z
            val distanceX = centerX - cameraPosition.x
            val distanceY = centerY - cameraPosition.y
            val distanceZ = centerZ - cameraPosition.z
            val command = obtainDrawCommand(drawCommands.size)

            command.set(
                profile,
                blockPos.x - cameraPosition.x,
                blockPos.y - cameraPosition.y,
                blockPos.z - cameraPosition.z,
                cameraPosition.x - blockPos.x,
                cameraPosition.y - blockPos.y,
                cameraPosition.z - blockPos.z,
                distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ
            )
            drawCommands.add(command)
        }
        if (drawCommands.isEmpty()) return false

        drawCommands.sortWith(drawCommandDistanceComparator)
        return true
    }

    private fun obtainDrawCommand(index: Int): HeartDrawCommand {
        while (index >= drawCommandPool.size) {
            drawCommandPool.add(HeartDrawCommand())
        }
        return drawCommandPool[index]
    }

    private fun releaseDrawCommands() {
        drawCommands.forEach { command ->
            command.clearReferences()
        }
        drawCommands.clear()
    }

    @Suppress("SameParameterValue")
    private fun renderPostProcessed(
        commands: List<HeartDrawCommand>,
        poseStack: PoseStack,
        volumeShader: ShaderInstance,
        blurShader: ShaderInstance
    ) {
        val mainTarget = Minecraft.getInstance().mainRenderTarget
        val width = mainTarget.viewWidth.coerceAtLeast(1)
        val height = mainTarget.viewHeight.coerceAtLeast(1)
        val outputFramebuffer = GlStateManager._getInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING)
        val targets = HeartBlackHolePostTargets.ensure(width, height)
        val sharedDepth = HeartBlackHoleFramebuffer.queryDepthAttachments(outputFramebuffer)

        targets.clearVolumeTargets()
        if (sharedDepth == null) {
            HeartBlackHoleFramebuffer.prepareFramebufferWrite(true)
            HeartBlackHoleFramebuffer.copyDepthFromFramebuffer(outputFramebuffer, width, height, targets.effect)
            HeartBlackHoleFramebuffer.copyDepthFromFramebuffer(outputFramebuffer, width, height, targets.mask)
        }

        val volumeUniforms = HeartVolumeUniforms(volumeShader)
        renderHeartTarget(
            targets.effect,
            commands,
            poseStack,
            volumeShader,
            volumeUniforms,
            VOLUME_PASS_EFFECT,
            sharedDepth
        )
        renderHeartTarget(
            targets.mask,
            commands,
            poseStack,
            volumeShader,
            volumeUniforms,
            VOLUME_PASS_MASK,
            sharedDepth
        )

        val blurWidth = targets.effectBlurA.viewWidth.coerceAtLeast(1)
        val blurHeight = targets.effectBlurA.viewHeight.coerceAtLeast(1)
        renderBlur(targets.effect, targets.effectBlurA, blurShader, 1.0f / width.toFloat(), 0.0f)
        renderBlur(targets.effectBlurA, targets.effectBlurB, blurShader, 0.0f, 1.0f / blurHeight.toFloat())
        renderBlur(targets.mask, targets.maskBlurA, blurShader, 1.0f / width.toFloat(), 0.0f)
        renderBlur(targets.maskBlurA, targets.maskBlurB, blurShader, 0.0f, 1.0f / blurHeight.toFloat())

        pendingComposite = PendingHeartComposite(targets, width, height, blurWidth, blurHeight)
        HeartBlackHoleFramebuffer.restoreFramebuffer(outputFramebuffer, width, height)
    }

    @JvmStatic
    fun renderPendingComposite(width: Int, height: Int) {
        val pending = pendingComposite ?: return
        pendingComposite = null
        HeartBlackHoleCompositeRenderer.renderPendingComposite(width, height, pending)
    }

    private fun renderHeartTarget(
        target: RenderTarget,
        commands: List<HeartDrawCommand>,
        poseStack: PoseStack,
        shader: ShaderInstance,
        uniforms: HeartVolumeUniforms,
        volumePass: Float,
        sharedDepth: HeartBlackHoleFramebuffer.DepthAttachments?
    ) {
        target.bindWrite(true)
        val originalDepth = if (sharedDepth != null) {
            HeartBlackHoleFramebuffer.replaceDepthAttachments(target.frameBufferId, sharedDepth)
        } else {
            null
        }
        HeartBlackHoleFramebuffer.prepareFramebufferWrite(true)
        RenderSystem.disableBlend()
        RenderSystem.enableDepthTest()
        RenderSystem.depthMask(false)
        RenderSystem.disableCull()

        HeartBlackHoleQuadBuffers.heartVolumeBuffer.bind()
        try {
            uniforms.applyVolumePass(volumePass)
            commands.forEach { command ->
                poseStack.pushPose()
                try {
                    poseStack.translate(
                        command.translateX,
                        command.translateY,
                        command.translateZ
                    )
                    uniforms.applyEntry(command)

                    HeartBlackHoleQuadBuffers.heartVolumeBuffer.drawWithShader(
                        poseStack.last().pose(),
                        RenderSystem.getProjectionMatrix(),
                        shader
                    )
                } finally {
                    poseStack.popPose()
                }
            }
        } finally {
            VertexBuffer.unbind()
            shader.clear()
        }

        if (originalDepth != null) {
            HeartBlackHoleFramebuffer.restoreDepthAttachments(target.frameBufferId, originalDepth)
        }
        RenderSystem.enableCull()
        RenderSystem.depthMask(true)
    }

    private fun renderBlur(
        input: RenderTarget,
        output: RenderTarget,
        shader: ShaderInstance,
        texelStepX: Float,
        texelStepY: Float
    ) {
        output.bindWrite(true)
        HeartBlackHoleFramebuffer.prepareFramebufferWrite(false)
        RenderSystem.disableBlend()
        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)
        RenderSystem.disableCull()

        RenderSystem.setShaderTexture(0, input.colorTextureId)
        shader.getUniform("TexelStep")?.set(texelStepX, texelStepY)

        HeartBlackHoleQuadBuffers.screenQuadBuffer.bind()
        HeartBlackHoleQuadBuffers.screenQuadBuffer.drawWithShader(IDENTITY_MATRIX, IDENTITY_MATRIX, shader)
        VertexBuffer.unbind()
        shader.clear()
    }

    private class HeartVolumeUniforms(shader: ShaderInstance) {
        private val passMode = shader.getUniform("PassMode")
        private val time = shader.getUniform("Time")
        private val center = shader.getUniform("Center")
        private val cameraPosition = shader.getUniform("CameraPosition")
        private val facingAxis = shader.getUniform("FacingAxis")
        private val blackHoleAndDiskRadius = shader.getUniform("BlackHoleAndDiskRadius")
        private val spaceSolidRadius = shader.getUniform("SpaceSolidRadius")
        private val spaceFadeRadius = shader.getUniform("SpaceFadeRadius")
        private val rotationSpeed = shader.getUniform("RotationSpeed")

        fun applyVolumePass(volumePass: Float) {
            passMode?.set(volumePass)
            blackHoleAndDiskRadius?.set(HeartBlackHoleVisualConfig.BLACK_HOLE_AND_DISK_RADIUS)
            spaceSolidRadius?.set(HeartBlackHoleVisualConfig.SPACE_SOLID_RADIUS)
            spaceFadeRadius?.set(HeartBlackHoleVisualConfig.SPACE_FADE_RADIUS)
        }

        fun applyEntry(command: HeartDrawCommand) {
            val profile = command.profile
            time?.set(profile.tick)
            center?.set(
                profile.center.x.toFloat(),
                profile.center.y.toFloat(),
                profile.center.z.toFloat()
            )
            cameraPosition?.set(command.cameraX, command.cameraY, command.cameraZ)
            facingAxis?.set(profile.facingX, profile.facingY, profile.facingZ)
            rotationSpeed?.set(profile.rotationSpeed)
        }
    }

    private class HeartDrawCommand {
        var profile: HeartBlackHoleRenderProfile = EMPTY_PROFILE
        var translateX = 0.0
        var translateY = 0.0
        var translateZ = 0.0
        var cameraX = 0.0f
        var cameraY = 0.0f
        var cameraZ = 0.0f
        var distanceToCameraSqr = 0.0

        fun set(
            profile: HeartBlackHoleRenderProfile,
            translateX: Double,
            translateY: Double,
            translateZ: Double,
            cameraX: Double,
            cameraY: Double,
            cameraZ: Double,
            distanceToCameraSqr: Double
        ) {
            this.profile = profile
            this.translateX = translateX
            this.translateY = translateY
            this.translateZ = translateZ
            this.cameraX = cameraX.toFloat()
            this.cameraY = cameraY.toFloat()
            this.cameraZ = cameraZ.toFloat()
            this.distanceToCameraSqr = distanceToCameraSqr
        }

        fun clearReferences() {
            profile = EMPTY_PROFILE
        }
    }
}

private data class SingleHeartBlackHoleRenderEntry(
    override val blockEntity: BlockEntity,
    override val profile: HeartBlackHoleRenderProfile
) : HeartBlackHoleRenderEntry