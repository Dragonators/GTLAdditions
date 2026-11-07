package com.gtladd.gtladditions.client.render.machine.rrf

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.render.IndexedEffectVertexBuffer
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredMachineRenderer
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredOculusCompat
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderContext
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderEntry
import com.gtladd.gtladditions.common.machine.multiblock.controller.rrf.RRFModuleMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.rrf.RRFWorkableModuleMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.rrf.RecursiveReverseArray
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RegisterShadersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.gtlcore.gtlcore.config.ConfigHolder
import org.lwjgl.opengl.GL11C
import java.util.EnumMap
import java.util.EnumSet

@OnlyIn(Dist.CLIENT)
internal object RrfEffectRenderer {
    private val builder = BufferBuilder(1 shl 20)
    private val vertexWriter = RrfVertexWriter()
    private val moduleMeshes = EnumMap<RrfEffectKind, RrfEffectMesh>(RrfEffectKind::class.java)
    private val preparedKinds = EnumSet.noneOf(RrfEffectKind::class.java)
    private val arrayMesh = RrfArrayMesh()
    private val arrayBatchKey = Any()
    private var vertexBuffer: VertexBuffer? = null
    private var arrayBuffer: IndexedEffectVertexBuffer? = null
    private val arrayIndices = it.unimi.dsi.fastutil.ints.IntArrayList()
    private var arrayVertices = 0
    private var shader: ShaderInstance? = null
    private var arrayShader: ShaderInstance? = null

    fun isActive(machine: MultiblockControllerMachine): Boolean = machine.isFormed &&
        when (machine) {
            is RecursiveReverseArray -> machine.effectsActive
            is RRFModuleMachine -> machine.effectsActive
            is RRFWorkableModuleMachine -> machine.effectsActive
            else -> false
        }

    fun enqueue(blockEntity: BlockEntity, machine: MultiblockControllerMachine, kind: RrfEffectKind, partialTicks: Float) {
        DeferredMachineRenderer.enqueue(Entry(blockEntity, machine, kind, partialTicks))
    }

    fun replaceShader(replacement: ShaderInstance?) {
        vertexWriter.close()
        vertexBuffer?.close()
        vertexBuffer = null
        shader = replacement
    }

    fun replaceArrayShader(replacement: ShaderInstance?) {
        arrayBuffer?.close()
        arrayBuffer = null
        arrayShader = replacement
    }

    private fun renderBatch(context: DeferredRenderContext, entries: List<DeferredRenderEntry>) {
        val first = entries.firstOrNull() as? Entry ?: return
        val array = first.kind == RrfEffectKind.ARRAY
        val currentShader = (if (array) arrayShader else shader)
            ?: return
        val smoothAnimations = ConfigHolder.INSTANCE.graphics.enableSmoothAnimations
        val sharedTicks = if (smoothAnimations) RenderUtils.getSmoothTickDouble(first.machine, first.partialTicks) else 0.0
        preparedKinds.clear()
        arrayIndices.clear()
        arrayVertices = 0
        builder.begin(
            VertexFormat.Mode.TRIANGLES,
            if (array) DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL else DefaultVertexFormat.POSITION_TEX_COLOR
        )

        for (candidate in entries) {
            val entry = candidate as Entry
            // A deferred pass can outlive the state that queued it. Disabled machines draw nothing.
            if (!isActive(entry.machine) || entry.blockEntity.isRemoved) continue
            val relative = entry.center.subtract(context.cameraPosition)
            val seconds = (if (smoothAnimations) sharedTicks else RenderUtils.getSmoothTickDouble(entry.machine, entry.partialTicks))
                .toDouble() / 20.0
            if (array) {
                if (!smoothAnimations || preparedKinds.add(RrfEffectKind.ARRAY)) RrfArrayGeometry.build(seconds, arrayMesh)
                appendArray(arrayMesh, relative)
            } else {
                val cached = moduleMeshes.getOrPut(entry.kind) { RrfEffectMesh() }
                if (!smoothAnimations || preparedKinds.add(entry.kind)) RrfEffectGeometry.module(entry.kind, seconds, cached)
                append(cached, entry.placement, relative)
            }
        }

        val vbo = if (array) {
            arrayBuffer ?: IndexedEffectVertexBuffer().also { arrayBuffer = it }
        } else {
            vertexBuffer ?: VertexBuffer(VertexBuffer.Usage.DYNAMIC).also { vertexBuffer = it }
        }

        RenderUtils.withSavedRenderState {
            RenderUtils.withBoundBuffer(vbo) {
                if (vbo is IndexedEffectVertexBuffer) {
                    vbo.uploadIndexed(builder.end(), arrayIndices.elements(), arrayIndices.size)
                } else {
                    vbo.upload(builder.end())
                }
                RenderSystem.enableBlend()
                if (array) {
                    RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR)
                    RenderSystem.enableCull()
                } else {
                    RenderSystem.blendFuncSeparate(
                        GlStateManager.SourceFactor.ONE,
                        GlStateManager.DestFactor.ONE,
                        GlStateManager.SourceFactor.ONE,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
                    )
                    RenderSystem.disableCull()
                }
                RenderSystem.enableDepthTest()
                RenderSystem.depthFunc(GL11C.GL_LEQUAL)
                RenderSystem.depthMask(false)
                DeferredOculusCompat.withDeferredShaderPass {
                    vbo.drawWithShader(context.poseStack.last().pose(), RenderSystem.getProjectionMatrix(), currentShader)
                }
            }
        }
    }

    private fun append(mesh: RrfEffectMesh, placement: RrfEffectPlacement, relative: Vec3) {
        vertexWriter.append(builder, mesh, placement, relative)
    }

    private fun appendArray(mesh: RrfArrayMesh, relative: Vec3) {
        vertexWriter.appendArray(builder, mesh, relative)
        for (i in 0 until mesh.indexCount) arrayIndices.add(arrayVertices + mesh.indices[i])
        arrayVertices += mesh.vertexCount
    }

    private class Entry(
        override val blockEntity: BlockEntity,
        val machine: MultiblockControllerMachine,
        val kind: RrfEffectKind,
        val partialTicks: Float
    ) : DeferredRenderEntry {
        val center: Vec3 = Vec3.atCenterOf(blockEntity.blockPos)
        val placement = RrfEffectPlacement(machine.frontFacing, machine.upwardsFacing, machine.isFlipped, kind == RrfEffectKind.ARRAY)
        private val bounds = placement.bounds(blockEntity.blockPos, kind == RrfEffectKind.ARRAY)
        override val batchKey: Any = if (kind == RrfEffectKind.ARRAY) arrayBatchKey else RrfEffectRenderer
        override val renderOrder: Int = 25

        override fun isVisible(context: DeferredRenderContext): Boolean =
            !blockEntity.isRemoved &&
                isActive(machine) &&
                center.distanceToSqr(context.cameraPosition) <= 128.0 * 128.0 &&
                context.frustum?.isVisible(bounds) != false

        override fun render(context: DeferredRenderContext) = renderBatch(context, listOf(this))
        override fun renderBatch(context: DeferredRenderContext, entries: List<DeferredRenderEntry>) = RrfEffectRenderer.renderBatch(context, entries)
    }
}

@Suppress("unused")
@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
internal object RrfEffectRenderEvents {
    @SubscribeEvent
    @JvmStatic
    fun onRegisterShaders(event: RegisterShadersEvent) {
        RenderUtils.registerShader(
            event,
            GTLAdditions.id("gtladditions_recursive_reverse_effect"),
            DefaultVertexFormat.POSITION_TEX_COLOR,
            RrfEffectRenderer::replaceShader
        )
        RenderUtils.registerShader(
            event,
            GTLAdditions.id("gtladditions_rrf_array_glow"),
            DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL,
            RrfEffectRenderer::replaceArrayShader
        )
    }
}