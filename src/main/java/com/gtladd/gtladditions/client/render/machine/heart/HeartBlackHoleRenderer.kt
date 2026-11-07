package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL14C
import org.lwjgl.opengl.GL30C
import kotlin.math.sin

@OnlyIn(Dist.CLIENT)
internal interface HeartBlackHoleRenderEntry {
    val blockEntity: BlockEntity
    val profile: HeartBlackHoleRenderProfile
}

@OnlyIn(Dist.CLIENT)
object HeartBlackHoleRenderer {
    private val EMPTY_PROFILE = HeartBlackHoleRenderProfile(0.0, Vec3.ZERO, 0.0f, 0.0f, 1.0f)
    private const val VOLUME_PASS_EFFECT = 0.0f
    private const val VOLUME_PASS_MATERIAL = 2.0f
    private const val SMOOTH_TICK_SECONDS = 0.05f
    private const val TAU = 6.2831855f
    private const val ROTATION_PHASE_SALT = 0x13579BDF2468ACE0L
    private const val ROTATION_SPEED_SALT = 0x2468ACE013579BDFL
    private const val ROTATION_DIRECTION_SALT = 0x5DEECE66DL
    private const val ROTATION_AXIS_SALT_STEP = 0x1F123BB5L
    private const val PITCH_AXIS = 0
    private val drawCommandDistanceComparator = Comparator<HeartDrawCommand> { first, second ->
        val distanceOrder = second.distanceToCameraSqr.compareTo(first.distanceToCameraSqr)
        if (distanceOrder != 0) distanceOrder else first.sourceKey.compareTo(second.sourceKey)
    }
    private val drawCommandPool = ArrayList<HeartDrawCommand>()
    private val drawCommands = ArrayList<HeartDrawCommand>()

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
        cameraPosition: Vec3,
        handMask: Int = 0
    ) {
        try {
            if (!prepareDrawCommands(entries, cameraPosition)) return

            val volumeShader = HeartBlackHoleShaders.volumeShader ?: return
            val downsampleShader = HeartBlackHoleShaders.downsampleShader ?: return
            val blurShader = HeartBlackHoleShaders.blurShader ?: return
            HeartBlackHoleShaders.compositeShader ?: return
            if (!HeartBlackHoleLookupTextures.setSamplers(volumeShader)) return
            if (!HeartBlackHoleAppearanceTextures.setSamplers(volumeShader)) return

            val completed = HeartBlackHoleRenderState.withSavedState {
                renderPostProcessed(drawCommands, poseStack, volumeShader, downsampleShader, blurShader)
            } ?: return
            // The same world stage owns production and consumption; no HUD callback can reuse it.
            HeartBlackHoleCompositeRenderer.renderPendingComposite(completed.width, completed.height, completed, handMask)
        } finally {
            releaseDrawCommands()
        }
    }

    internal fun renderLayers(
        entries: List<HeartBlackHoleRenderEntry>,
        poseStack: PoseStack,
        cameraPosition: Vec3,
        width: Int,
        height: Int,
        images: HeartLayerImages,
        handMask: Int
    ): Boolean {
        try {
            if (!prepareDrawCommands(entries, cameraPosition)) return false
            val volume = HeartBlackHoleShaders.volumeShader ?: return false
            val downsample = HeartBlackHoleShaders.downsampleShader ?: return false
            val blur = HeartBlackHoleShaders.blurShader ?: return false
            HeartBlackHoleShaders.compositeShader ?: return false
            HeartBlackHoleShaders.layerCopyShader ?: return false
            if (!HeartBlackHoleLookupTextures.setSamplers(volume) || !HeartBlackHoleAppearanceTextures.setSamplers(volume)) return false
            val framebuffer = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING)
            val sceneDepth = HeartBlackHoleFramebuffer.querySceneDepth(framebuffer) ?: return false
            val centers = drawCommands.map { command ->
                val center = command.profile.center
                poseStack.last().pose().transformPosition(
                    Vector3f(
                        (command.translateX + center.x).toFloat(),
                        (command.translateY + center.y).toFloat(),
                        (command.translateZ + center.z).toFloat()
                    )
                )
            }
            val rects = centers.mapIndexed { index, center ->
                HeartSceneCompositor.projectHeart(
                    RenderSystem.getProjectionMatrix(),
                    center,
                    drawCommands[index].volumeProxyRadius,
                    width,
                    height
                )
            }

            images.ensure(width, height, drawCommands.size, rects)
            val targets = HeartBlackHolePostTargets.ensure(width, height)
            if (!HeartBlackHoleFramebuffer.bindSceneDepth(volume, sceneDepth, framebuffer, width, height, targets.depthCopy)) return false
            drawCommands.forEachIndexed { index, command ->
                if (!HeartBlackHoleFramebuffer.clearColor(targets.effect)) return false
                if (!HeartVolumePass(listOf(command), poseStack, volume).renderEffect(targets.effect, targets.material)) return false
                images.capture(targets.effect, index)
                val center = centers[index]
                images.setSource(index, command.sourceKey)
                images.setCenter(index, center.x, center.y, center.z, command.volumeProxyRadius)
            }

            images.uploadCenters()
            if (!HeartBlackHoleFramebuffer.clearColor(targets.effect)) return false
            drawCommands.indices.forEach { index -> images.copyTo(targets.effect, index, blend = true) }
            if (!HeartBlackHoleBlurRenderer.renderBloom(
                    targets.effect,
                    targets.bloomLevels,
                    downsample,
                    blur,
                    filterBounds(drawCommands, poseStack, width, height)
                )
            ) {
                return false
            }
            val pending = PendingHeartComposite(targets, width, height, targets.effectBlurred.viewWidth, targets.effectBlurred.viewHeight)
            drawCommands.indices.forEach { index ->
                images.copyTo(targets.effect, index)
                if (!HeartBlackHoleBlurRenderer.render(
                        targets.effect,
                        targets.effectBlurred,
                        targets.effectBlurScratch,
                        downsample,
                        blur,
                        inputBounds = rects[index]
                    )
                ) {
                    return false
                }

                if (!HeartBlackHoleBlurRenderer.render(
                        targets.effect,
                        targets.maskBlurred,
                        targets.maskBlurScratch,
                        downsample,
                        blur,
                        inputBounds = rects[index]
                    )
                ) {
                    return false
                }

                images.bindLayer(index)
                HeartBlackHoleCompositeRenderer.renderPendingComposite(
                    width,
                    height,
                    pending,
                    handMask,
                    writeAlpha = true,
                    origin = images.rect(index)
                )
            }

            RenderSystem.disableScissor()
            return true
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
            val blockSeed = blockPos.asLong()
            command.sourceKey = blockSeed
            command.volumeProxyRadius = HeartBlackHoleVisualConfig.volumeProxyRadius(profile.tick)
            val blackHoleRotationX = getBlackHoleRotation(blockSeed, profile.tick, 0)
            val blackHoleRotationY = getBlackHoleRotation(blockSeed, profile.tick, 1)

            command.set(
                profile,
                blockPos.x - cameraPosition.x,
                blockPos.y - cameraPosition.y,
                blockPos.z - cameraPosition.z,
                cameraPosition.x - blockPos.x,
                cameraPosition.y - blockPos.y,
                cameraPosition.z - blockPos.z,
                distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ,
                blackHoleRotationX,
                blackHoleRotationY
            )
            drawCommands.add(command)
        }
        if (drawCommands.isEmpty()) return false

        drawCommands.sortWith(drawCommandDistanceComparator)
        return true
    }

    private fun maximumNearPlaneDistance(): Double {
        val projection = RenderSystem.getProjectionMatrix()
        val corner = Vector3f()
        var distance = 0.0f
        for (index in 0..3) distance = maxOf(distance, projection.frustumCorner(index, corner).length())
        return if (distance.isFinite()) distance.toDouble() else 0.0
    }

    private fun obtainDrawCommand(index: Int): HeartDrawCommand {
        while (index >= drawCommandPool.size) {
            drawCommandPool.add(HeartDrawCommand())
        }
        return drawCommandPool[index]
    }

    private fun getBlackHoleRotation(seed: Long, tick: Double, axis: Int): Float {
        val axisSalt = ROTATION_AXIS_SALT_STEP * axis.toLong()
        val phase = hashUnit(seed, ROTATION_PHASE_SALT + axisSalt) * TAU
        val speedRandom = hashUnit(seed, ROTATION_SPEED_SALT + axisSalt)
        val direction = if (hashUnit(seed, ROTATION_DIRECTION_SALT + axisSalt) < 0.5f) -1.0f else 1.0f
        val speedRange = HeartBlackHoleVisualConfig.BLACK_HOLE_ROTATION_MAX_SPEED -
            HeartBlackHoleVisualConfig.BLACK_HOLE_ROTATION_MIN_SPEED
        val speed = HeartBlackHoleVisualConfig.BLACK_HOLE_ROTATION_MIN_SPEED +
            speedRange * speedRandom
        val angle = phase + tick * SMOOTH_TICK_SECONDS * speed * direction
        return if (axis == PITCH_AXIS) {
            (sin(angle) * HeartBlackHoleVisualConfig.BLACK_HOLE_VERTICAL_ROTATION_LIMIT).toFloat()
        } else {
            Math.IEEEremainder(angle, 2 * Math.PI).toFloat()
        }
    }

    private fun hashUnit(seed: Long, salt: Long): Float {
        var value = seed xor salt
        value = value xor (value ushr 33)
        value *= -49064778989728563L
        value = value xor (value ushr 33)
        value *= -4265267296055464877L
        value = value xor (value ushr 33)
        return ((value ushr 40) and 0xFFFFFFL).toFloat() / 0x1000000.toFloat()
    }

    private fun releaseDrawCommands() {
        drawCommands.forEach { command ->
            command.clearReferences()
        }
        drawCommands.clear()
    }

    private fun renderPostProcessed(
        commands: List<HeartDrawCommand>,
        poseStack: PoseStack,
        volumeShader: ShaderInstance,
        downsampleShader: ShaderInstance,
        blurShader: ShaderInstance
    ): PendingHeartComposite? {
        val mainTarget = Minecraft.getInstance().mainRenderTarget
        val width = mainTarget.viewWidth.coerceAtLeast(1)
        val height = mainTarget.viewHeight.coerceAtLeast(1)
        val outputFramebuffer = GlStateManager._getInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING)
        val sceneDepth = HeartBlackHoleFramebuffer.querySceneDepth(outputFramebuffer) ?: return null
        val targets = HeartBlackHolePostTargets.ensure(width, height)
        if (!HeartBlackHoleFramebuffer.bindSceneDepth(volumeShader, sceneDepth, outputFramebuffer, width, height, targets.depthCopy)) return null
        if (!targets.clearVolumeTargets()) return null

        val filterBounds = filterBounds(commands, poseStack, width, height)
        val volumePass = HeartVolumePass(commands, poseStack, volumeShader)
        if (!volumePass.renderEffect(targets.effect, targets.material)) return null
        if (!HeartBlackHoleBlurRenderer.render(
                targets.effect,
                targets.effectBlurred,
                targets.effectBlurScratch,
                downsampleShader,
                blurShader,
                inputBounds = filterBounds
            )
        ) {
            return null
        }

        if (!HeartBlackHoleBlurRenderer.render(
                targets.effect,
                targets.maskBlurred,
                targets.maskBlurScratch,
                downsampleShader,
                blurShader,
                inputBounds = filterBounds
            )
        ) {
            return null
        }

        if (!HeartBlackHoleBlurRenderer.renderBloom(
                targets.effect,
                targets.bloomLevels,
                downsampleShader,
                blurShader,
                filterBounds
            )
        ) {
            return null
        }

        val blurWidth = targets.effectBlurred.viewWidth.coerceAtLeast(1)
        val blurHeight = targets.effectBlurred.viewHeight.coerceAtLeast(1)
        return PendingHeartComposite(targets, width, height, blurWidth, blurHeight)
    }

    private fun filterBounds(commands: List<HeartDrawCommand>, pose: PoseStack, width: Int, height: Int): IntArray {
        val vp = Matrix4f(RenderSystem.getProjectionMatrix()).mul(pose.last().pose())
        var x0 = width
        var y0 = height
        var x1 = 0
        var y1 = 0

        for (command in commands) {
            val center = command.profile.center
            val x = command.translateX + center.x
            val y = command.translateY + center.y
            val z = command.translateZ + center.z
            val r = command.volumeProxyRadius.toDouble()
            val rect = HeartSceneCompositor.screenBounds(
                vp,
                net.minecraft.world.phys.AABB(x - r, y - r, z - r, x + r, y + r, z + r),
                width,
                height,
                2
            )
            if (rect[2] <= 0 || rect[3] <= 0) continue

            x0 = minOf(x0, rect[0])
            y0 = minOf(y0, rect[1])
            x1 = maxOf(x1, rect[0] + rect[2])
            y1 = maxOf(y1, rect[1] + rect[3])
        }

        return if (x1 <= x0 || y1 <= y0) intArrayOf(0, 0, 0, 0) else intArrayOf(x0, y0, x1 - x0, y1 - y0)
    }

    private class HeartVolumePass(
        private val commands: List<HeartDrawCommand>,
        private val poseStack: PoseStack,
        private val shader: ShaderInstance
    ) {
        private val uniforms = HeartVolumeUniforms(shader)
        private val nearPlaneMargin = maximumNearPlaneDistance()

        fun renderEffect(target: RenderTarget, material: RenderTarget): Boolean {
            shader.getUniform("MaterialUvScale")?.set(1.0f / target.viewWidth, 1.0f / target.viewHeight)
            shader.getUniform("MaterialPixelScale")?.set(
                material.viewWidth.toFloat() / target.viewWidth,
                material.viewHeight.toFloat() / target.viewHeight
            )
            // Each Heart owns a complete material estimate before its full-resolution
            // background pass. Sharing one estimate between overlapping Hearts mixes fog.
            commands.forEach { command ->
                shader.setSampler("MaterialSampler", 0)
                if (!HeartBlackHoleFramebuffer.clearColor(material)) return false
                if (!render(material, VOLUME_PASS_MATERIAL, command)) return false
                shader.setSampler("MaterialSampler", material.colorTextureId)
                if (!render(target, VOLUME_PASS_EFFECT, command)) return false
            }

            return true
        }

        fun render(target: RenderTarget, passMode: Float, only: HeartDrawCommand? = null): Boolean {
            if (!HeartBlackHoleFramebuffer.bindForDraw(target)) return false
            shader.getUniform("PassSize")?.set(target.viewWidth.toFloat(), target.viewHeight.toFloat())
            draw(passMode, only)
            return true
        }

        private fun draw(passMode: Float, only: HeartDrawCommand?) {
            RenderSystem.colorMask(true, true, true, true)
            RenderSystem.enableBlend()
            RenderSystem.blendEquation(GL14C.GL_FUNC_ADD)
            RenderSystem.blendFuncSeparate(GL11C.GL_ONE, GL11C.GL_ONE_MINUS_SRC_ALPHA, GL11C.GL_ONE, GL11C.GL_ONE_MINUS_SRC_ALPHA)
            RenderSystem.disableDepthTest()
            RenderSystem.depthMask(false)
            RenderSystem.enableCull()
            GL11C.glFrontFace(GL11C.GL_CCW)

            try {
                RenderUtils.withBoundBuffer(HeartBlackHoleQuadBuffers.heartVolumeBuffer) {
                    uniforms.applyVolumePass(passMode)
                    commands.forEach { command ->
                        if (only != null && command !== only) return@forEach
                        // Shade one proxy surface even when the eye enters the volume.
                        // Scene occlusion clips the volume integral, never this proxy surface.
                        val insideDistance = command.volumeProxyRadius + nearPlaneMargin
                        GL11C.glCullFace(
                            if (command.distanceToCameraSqr <= insideDistance * insideDistance) GL11C.GL_FRONT else GL11C.GL_BACK
                        )
                        poseStack.pushPose()
                        try {
                            poseStack.translate(command.translateX, command.translateY, command.translateZ)
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
                }
            } finally {
                shader.clear()
            }
        }
    }

    private class HeartVolumeUniforms(shader: ShaderInstance) {
        private val passMode = shader.getUniform("PassMode")
        private val volumeProxyRadius = shader.getUniform("VolumeProxyRadius")
        private val animationPhase = shader.getUniform("AnimationPhase")
        private val center = shader.getUniform("Center")
        private val cameraPosition = shader.getUniform("CameraPosition")
        private val facingAxis = shader.getUniform("FacingAxis")
        private val blackHoleAndDiskRadius = shader.getUniform("BlackHoleAndDiskRadius")
        private val spaceSolidRadius = shader.getUniform("SpaceSolidRadius")
        private val spaceFadeRadius = shader.getUniform("SpaceFadeRadius")
        private val blackHoleRotation = shader.getUniform("BlackHoleRotation")

        fun applyVolumePass(volumePass: Float) {
            passMode?.set(volumePass)
            blackHoleAndDiskRadius?.set(HeartBlackHoleVisualConfig.BLACK_HOLE_AND_DISK_RADIUS)
            spaceSolidRadius?.set(HeartBlackHoleVisualConfig.SPACE_SOLID_RADIUS)
            spaceFadeRadius?.set(HeartBlackHoleVisualConfig.SPACE_FADE_RADIUS)
        }

        fun applyEntry(command: HeartDrawCommand) {
            val profile = command.profile
            volumeProxyRadius?.set(command.volumeProxyRadius)
            animationPhase?.set(
                Math.IEEEremainder(profile.tick / 20.0 * 0.002, 2 * Math.PI).toFloat(),
                ((profile.tick / 20.0 * (profile.rotationSpeed / 0.3f)) % 12.0).toFloat()
            )
            center?.set(
                profile.center.x.toFloat(),
                profile.center.y.toFloat(),
                profile.center.z.toFloat()
            )
            cameraPosition?.set(command.cameraX, command.cameraY, command.cameraZ)
            facingAxis?.set(profile.facingX, profile.facingY, profile.facingZ)
            blackHoleRotation?.set(command.blackHoleRotationX, command.blackHoleRotationY)
        }
    }

    private class HeartDrawCommand {
        var sourceKey = 0L
        var volumeProxyRadius = 0.0f
        var profile: HeartBlackHoleRenderProfile = EMPTY_PROFILE
        var translateX = 0.0
        var translateY = 0.0
        var translateZ = 0.0
        var cameraX = 0.0f
        var cameraY = 0.0f
        var cameraZ = 0.0f
        var distanceToCameraSqr = 0.0
        var blackHoleRotationX = 0.0f
        var blackHoleRotationY = 0.0f

        fun set(
            profile: HeartBlackHoleRenderProfile,
            translateX: Double,
            translateY: Double,
            translateZ: Double,
            cameraX: Double,
            cameraY: Double,
            cameraZ: Double,
            distanceToCameraSqr: Double,
            blackHoleRotationX: Float,
            blackHoleRotationY: Float
        ) {
            this.profile = profile
            this.translateX = translateX
            this.translateY = translateY
            this.translateZ = translateZ
            this.cameraX = cameraX.toFloat()
            this.cameraY = cameraY.toFloat()
            this.cameraZ = cameraZ.toFloat()
            this.distanceToCameraSqr = distanceToCameraSqr
            this.blackHoleRotationX = blackHoleRotationX
            this.blackHoleRotationY = blackHoleRotationY
        }

        fun clearReferences() {
            profile = EMPTY_PROFILE
            blackHoleRotationX = 0.0f
            blackHoleRotationY = 0.0f
        }
    }
}

private data class SingleHeartBlackHoleRenderEntry(
    override val blockEntity: BlockEntity,
    override val profile: HeartBlackHoleRenderProfile
) : HeartBlackHoleRenderEntry