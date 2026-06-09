package com.gtladd.gtladditions.client.render.machine.antichrist

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.render.withPose
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexSorting
import net.minecraft.client.Minecraft
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.joml.Matrix4f

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(
    modid = GTLAdditions.MOD_ID,
    bus = Mod.EventBusSubscriber.Bus.FORGE,
    value = [Dist.CLIENT]
)
object AntichristDeferredRenderer {
    private val entries = LinkedHashMap<Long, Entry>()
    private val ringEntries = LinkedHashMap<Long, Entry>()
    private var postFinalContext: PostFinalContext? = null

    fun enqueue(blockEntity: BlockEntity, profile: AntichristRenderProfile) {
        entries[blockEntity.blockPos.asLong()] = Entry(blockEntity, profile)
    }

    fun enqueueRing(blockEntity: BlockEntity, profile: AntichristRenderProfile) {
        ringEntries[blockEntity.blockPos.asLong()] = Entry(blockEntity, profile)
    }

    @SubscribeEvent
    @JvmStatic
    fun onRenderLevelStage(event: RenderLevelStageEvent) {
        when (event.stage) {
            RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES -> {
                if (ringEntries.isEmpty()) return

                val cameraPosition = event.camera.position
                ringEntries.values.forEach { entry ->
                    val blockEntity = entry.blockEntity
                    if (blockEntity.isRemoved) return@forEach

                    event.poseStack.withPose {
                        translate(
                            blockEntity.blockPos.x - cameraPosition.x,
                            blockEntity.blockPos.y - cameraPosition.y,
                            blockEntity.blockPos.z - cameraPosition.z
                        )
                        AntichristRingRenderer.renderTerrainBatched(entry.profile, this)
                    }
                }
                ringEntries.clear()
            }

            RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS -> {
                if (entries.isEmpty()) return

                val cameraPosition = event.camera.position
                if (AntichristOculusCompat.shouldRenderAfterShaderpackFinal()) {
                    postFinalContext = PostFinalContext(
                        entries.values.toList(),
                        cameraPosition,
                        Matrix4f(event.poseStack.last().pose()),
                        Matrix4f(event.projectionMatrix)
                    )
                    entries.clear()
                    return
                }

                val iterator = entries.values.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val blockEntity = entry.blockEntity
                    if (blockEntity.isRemoved) continue

                    event.poseStack.withPose {
                        translate(
                            blockEntity.blockPos.x - cameraPosition.x,
                            blockEntity.blockPos.y - cameraPosition.y,
                            blockEntity.blockPos.z - cameraPosition.z
                        )
                        render(entry.profile, this, blockEntity, true)
                    }
                }
                entries.clear()
            }

            else -> return
        }
    }

    @JvmStatic
    fun renderAfterShaderpackFinal() {
        val context = postFinalContext ?: return
        postFinalContext = null

        val previousProjection = Matrix4f(RenderSystem.getProjectionMatrix())
        AntichristOculusCompat.beginDirectMainTargetPass()
        try {
            Minecraft.getInstance().mainRenderTarget.bindWrite(false)
            RenderSystem.setProjectionMatrix(context.projectionMatrix, VertexSorting.DISTANCE_TO_ORIGIN)

            val poseStack = PoseStack()
            poseStack.mulPoseMatrix(context.poseMatrix)
            context.entries.forEach { entry ->
                val blockEntity = entry.blockEntity
                if (blockEntity.isRemoved) return@forEach

                poseStack.withPose {
                    translate(
                        blockEntity.blockPos.x - context.cameraPosition.x,
                        blockEntity.blockPos.y - context.cameraPosition.y,
                        blockEntity.blockPos.z - context.cameraPosition.z
                    )
                    render(entry.profile, this, blockEntity, false)
                }
            }
        } finally {
            RenderSystem.setProjectionMatrix(previousProjection, VertexSorting.ORTHOGRAPHIC_Z)
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
            RenderSystem.colorMask(true, true, true, true)
            RenderSystem.depthMask(true)
            RenderSystem.disableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.enableCull()
            AntichristOculusCompat.endDirectMainTargetPass()
        }
    }

    private fun render(
        profile: AntichristRenderProfile,
        poseStack: PoseStack,
        blockEntity: BlockEntity,
        writeDepthShell: Boolean
    ) {
        AntichristStarRenderer.renderOpaque(profile, poseStack)
        AntichristStarRenderer.renderTransparent(profile, poseStack)
        AntichristBeamRenderer.render(profile, poseStack, blockEntity)
        if (writeDepthShell) {
            AntichristStarRenderer.renderTransparentDepthForShaderpack(profile, poseStack)
        }
    }

    private data class Entry(
        val blockEntity: BlockEntity,
        val profile: AntichristRenderProfile
    )

    private data class PostFinalContext(
        val entries: List<Entry>,
        val cameraPosition: Vec3,
        val poseMatrix: Matrix4f,
        val projectionMatrix: Matrix4f
    )
}