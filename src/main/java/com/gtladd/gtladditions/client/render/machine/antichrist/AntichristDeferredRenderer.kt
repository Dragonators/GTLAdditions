package com.gtladd.gtladditions.client.render.machine.antichrist

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.utils.antichrist.RingStructureVertexBuffer
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexSorting
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
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
    private val entries = Long2ObjectLinkedOpenHashMap<Entry>()
    private val ringEntries = Long2ObjectLinkedOpenHashMap<Entry>()
    private var postFinalContext: PostFinalContext? = null

    fun enqueue(blockEntity: BlockEntity, profile: AntichristRenderProfile) {
        entries.put(blockEntity.blockPos.asLong(), Entry(blockEntity, profile))
    }

    fun enqueueRing(blockEntity: BlockEntity, profile: AntichristRenderProfile) {
        ringEntries.put(blockEntity.blockPos.asLong(), Entry(blockEntity, profile))
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
                        RingStructureVertexBuffer.renderTerrainBatched(entry.profile, this)
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
                    renderEntryRelativeToCamera(entry, cameraPosition, event.poseStack)
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
                renderEntryRelativeToCamera(entry, context.cameraPosition, poseStack)
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

    private fun renderEntryRelativeToCamera(
        entry: Entry,
        cameraPosition: Vec3,
        poseStack: PoseStack
    ) {
        val blockEntity = entry.blockEntity
        if (blockEntity.isRemoved) return

        poseStack.withPose {
            translate(
                blockEntity.blockPos.x - cameraPosition.x,
                blockEntity.blockPos.y - cameraPosition.y,
                blockEntity.blockPos.z - cameraPosition.z
            )
            render(entry.profile, this, blockEntity)
        }
    }

    private fun render(
        profile: AntichristRenderProfile,
        poseStack: PoseStack,
        blockEntity: BlockEntity
    ) {
        AntichristStarRenderer.renderOpaque(profile, poseStack)
        AntichristStarRenderer.renderTransparent(profile, poseStack)
        AntichristBeamRenderer.render(profile, poseStack, blockEntity)
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