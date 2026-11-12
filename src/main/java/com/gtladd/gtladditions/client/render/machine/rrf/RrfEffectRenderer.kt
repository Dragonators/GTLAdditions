package com.gtladd.gtladditions.client.render.machine.rrf

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.client.scene.PartBlend
import com.gtladd.gtladditions.api.client.scene.PartBounds
import com.gtladd.gtladditions.api.client.scene.PartDrawContext
import com.gtladd.gtladditions.api.client.scene.PartKey
import com.gtladd.gtladditions.api.client.scene.PartOutcome
import com.gtladd.gtladditions.api.client.scene.PartRenderer
import com.gtladd.gtladditions.api.client.scene.ScenePart
import com.gtladd.gtladditions.api.client.scene.SceneRendering
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.IndexedEffectVertexBuffer
import com.gtladd.gtladditions.common.machine.multiblock.controller.rrf.RRFModuleMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.rrf.RRFWorkableModuleMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.rrf.RecursiveReverseArray
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
import java.util.EnumMap

@OnlyIn(Dist.CLIENT)
internal object RrfEffectRenderer {
    private val builder = BufferBuilder(1 shl 20)
    private val vertexWriter = RrfVertexWriter()
    private val moduleMeshes = EnumMap<RrfEffectKind, RrfEffectMesh>(RrfEffectKind::class.java)

    private val moduleMeshTicks = EnumMap<RrfEffectKind, Double>(RrfEffectKind::class.java)
    private val arrayMesh = RrfArrayMesh()
    private var arrayMeshTicks = Double.NaN
    private var vertexBuffer: VertexBuffer? = null
    private var arrayBuffer: IndexedEffectVertexBuffer? = null
    private val arrayIndices = it.unimi.dsi.fastutil.ints.IntArrayList()
    private var arrayVertices = 0
    private var shader: ShaderInstance? = null
    private var arrayShader: ShaderInstance? = null
    private val provider = SceneRendering.register(GTLAdditions.id("rrf"))

    fun isActive(machine: MultiblockControllerMachine): Boolean = machine.isFormed &&
        when (machine) {
            is RecursiveReverseArray -> machine.effectsActive
            is RRFModuleMachine -> machine.effectsActive
            is RRFWorkableModuleMachine -> machine.effectsActive
            else -> false
        }

    fun submit(blockEntity: BlockEntity, machine: MultiblockControllerMachine, kind: RrfEffectKind, partialTicks: Float) {
        val frame = SceneRendering.frame() ?: return
        val array = kind == RrfEffectKind.ARRAY
        val ticks = if (ConfigHolder.INSTANCE.graphics.enableSmoothAnimations) {
            frame.renderTicks
        } else {
            RenderUtils.getSmoothTickDouble(machine, partialTicks)
        }

        val placement = RrfEffectPlacement(machine.frontFacing, machine.upwardsFacing, machine.isFlipped, array)
        val renderer = if (array) ArrayParts else ModuleParts
        frame.submit(
            ScenePart.builder(
                PartKey.block(provider, blockEntity.blockPos, 0),
                renderer,
                Rrf(Vec3.atCenterOf(blockEntity.blockPos), placement, kind, ticks)
            )
                .blend(if (array) PartBlend.SCREEN else PartBlend.ADDITIVE)
                .bounds(PartBounds.box(placement.bounds(blockEntity.blockPos, array)))
                .batchKey(renderer)
                .build()
        )
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

    /** The blend is the program's own. */
    private fun drawBatch(context: PartDrawContext, parts: List<ScenePart<Rrf>>, array: Boolean) {
        val currentShader = (if (array) arrayShader else shader)
            ?: return parts.forEach { context.report(it, PartOutcome.RESOURCE_UNAVAILABLE) }
        val frame = context.frame
        arrayIndices.clear()
        arrayVertices = 0
        builder.begin(
            VertexFormat.Mode.TRIANGLES,
            if (array) DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL else DefaultVertexFormat.POSITION_TEX_COLOR
        )

        for (part in parts) {
            val rrf = part.payload
            val relative = rrf.center.subtract(frame.cameraX, frame.cameraY, frame.cameraZ)
            if (array) {
                if (arrayMeshTicks != rrf.ticks) {
                    RrfArrayGeometry.build(rrf.ticks / 20.0, arrayMesh)
                    arrayMeshTicks = rrf.ticks
                }
                appendArray(arrayMesh, relative)
            } else {
                val mesh = moduleMeshes.getOrPut(rrf.kind) { RrfEffectMesh() }
                if (moduleMeshTicks[rrf.kind] != rrf.ticks) {
                    RrfEffectGeometry.module(rrf.kind, rrf.ticks / 20.0, mesh)
                    moduleMeshTicks[rrf.kind] = rrf.ticks
                }

                vertexWriter.append(builder, mesh, rrf.placement, relative)
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

                // The array tubes show their front faces only; the flat module glyphs face either way.
                if (array) RenderSystem.enableCull() else RenderSystem.disableCull()
                vbo.drawWithShader(context.poseStack.last().pose(), RenderSystem.getProjectionMatrix(), currentShader)
            }
        }
    }

    private fun appendArray(mesh: RrfArrayMesh, relative: Vec3) {
        vertexWriter.appendArray(builder, mesh, relative)
        for (i in 0 until mesh.indexCount) arrayIndices.add(arrayVertices + mesh.indices[i])
        arrayVertices += mesh.vertexCount
    }

    private class Rrf(val center: Vec3, val placement: RrfEffectPlacement, val kind: RrfEffectKind, val ticks: Double)

    private object ArrayParts : PartRenderer<Rrf> {
        override fun draw(context: PartDrawContext, parts: List<ScenePart<Rrf>>) = drawBatch(context, parts, true)
    }

    private object ModuleParts : PartRenderer<Rrf> {
        override fun draw(context: PartDrawContext, parts: List<ScenePart<Rrf>>) = drawBatch(context, parts, false)
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