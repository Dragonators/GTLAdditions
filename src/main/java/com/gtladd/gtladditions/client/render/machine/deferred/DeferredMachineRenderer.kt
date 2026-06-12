package com.gtladd.gtladditions.client.render.machine.deferred

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.render.withPose
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexSorting
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.joml.Matrix4f

@OnlyIn(Dist.CLIENT)
internal interface DeferredRenderEntry {
    val blockEntity: BlockEntity
    val renderOrder: Int
        get() = 0
    val batchKey: Any?
        get() = null

    fun isVisible(context: DeferredRenderContext): Boolean = !blockEntity.isRemoved

    fun render(context: DeferredRenderContext)

    fun renderBatch(context: DeferredRenderContext, entries: List<DeferredRenderEntry>) {
        entries.forEach { entry -> entry.render(context) }
    }
}

@OnlyIn(Dist.CLIENT)
internal class DeferredRenderContext(
    val cameraPosition: Vec3,
    val poseStack: PoseStack,
    val frustum: Frustum?
) {
    fun renderRelativeToCamera(
        blockEntity: BlockEntity,
        render: PoseStack.() -> Unit
    ) {
        if (blockEntity.isRemoved) return

        poseStack.withPose {
            translate(
                blockEntity.blockPos.x - cameraPosition.x,
                blockEntity.blockPos.y - cameraPosition.y,
                blockEntity.blockPos.z - cameraPosition.z
            )
            render()
        }
    }
}

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(
    modid = GTLAdditions.MOD_ID,
    bus = Mod.EventBusSubscriber.Bus.FORGE,
    value = [Dist.CLIENT]
)
object DeferredMachineRenderer {
    private val entries = Long2ObjectLinkedOpenHashMap<DeferredRenderEntry>()
    private val visibleEntries = ArrayList<DeferredRenderEntry>()
    private val batchedEntries = ArrayList<DeferredRenderEntry>()
    private val renderedBatchKeys = HashSet<Any>()
    private var postFinalContext: PostFinalContext? = null

    internal fun enqueue(entry: DeferredRenderEntry) {
        entries.put(entry.blockEntity.blockPos.asLong(), entry)
    }

    @SubscribeEvent
    @JvmStatic
    fun onRenderLevelStage(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || entries.isEmpty()) return

        val context = DeferredRenderContext(
            event.camera.position,
            event.poseStack,
            event.frustum
        )

        try {
            collectVisibleEntries(context)
            if (DeferredOculusCompat.shouldRenderAfterShaderpackFinal()) {
                if (visibleEntries.isNotEmpty()) {
                    postFinalContext = PostFinalContext(
                        visibleEntries.toList(),
                        context.cameraPosition,
                        Matrix4f(event.poseStack.last().pose()),
                        Matrix4f(event.projectionMatrix)
                    )
                }
                return
            }

            renderEntries(context, visibleEntries)
        } finally {
            entries.clear()
            visibleEntries.clear()
            batchedEntries.clear()
            renderedBatchKeys.clear()
        }
    }

    @JvmStatic
    fun renderAfterShaderpackFinal() {
        val context = postFinalContext ?: return
        postFinalContext = null

        val previousProjection = Matrix4f(RenderSystem.getProjectionMatrix())
        DeferredOculusCompat.beginDirectMainTargetPass()
        try {
            Minecraft.getInstance().mainRenderTarget.bindWrite(false)
            RenderSystem.setProjectionMatrix(context.projectionMatrix, VertexSorting.DISTANCE_TO_ORIGIN)

            val poseStack = PoseStack()
            poseStack.mulPoseMatrix(context.poseMatrix)
            renderEntries(
                DeferredRenderContext(
                    context.cameraPosition,
                    poseStack,
                    null
                ),
                context.entries
            )
        } finally {
            RenderSystem.setProjectionMatrix(previousProjection, VertexSorting.ORTHOGRAPHIC_Z)
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
            RenderSystem.colorMask(true, true, true, true)
            RenderSystem.depthMask(true)
            RenderSystem.disableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.enableCull()
            DeferredOculusCompat.endDirectMainTargetPass()
        }
    }

    private fun collectVisibleEntries(context: DeferredRenderContext) {
        visibleEntries.clear()
        entries.values.forEach { entry ->
            if (entry.isVisible(context)) {
                visibleEntries.add(entry)
            }
        }
        visibleEntries.sortBy { entry -> entry.renderOrder }
    }

    private fun renderEntries(context: DeferredRenderContext, frameEntries: List<DeferredRenderEntry>) {
        frameEntries.forEach { entry ->
            val batchKey = entry.batchKey
            if (batchKey == null) {
                entry.render(context)
            } else if (renderedBatchKeys.add(batchKey)) {
                batchedEntries.clear()
                frameEntries.forEach { candidate ->
                    if (candidate.batchKey == batchKey) {
                        batchedEntries.add(candidate)
                    }
                }
                entry.renderBatch(context, batchedEntries)
            }
        }
        batchedEntries.clear()
        renderedBatchKeys.clear()
    }

    private data class PostFinalContext(
        val entries: List<DeferredRenderEntry>,
        val cameraPosition: Vec3,
        val poseMatrix: Matrix4f,
        val projectionMatrix: Matrix4f
    )
}