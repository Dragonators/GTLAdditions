package com.gtladd.gtladditions.client.render.frame

import com.gtladd.gtladditions.client.GTLAddRenderTypes
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.mixin.embeddium.FrameOverlayWorldRendererAccessor
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.parameters.AlphaCutoffParameter
import me.jellysquid.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType
import net.minecraftforge.client.event.RenderLevelStageEvent
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

object FrameOverlayTerrainPass {
    @JvmField
    val PASS = TerrainRenderPass(GTLAddRenderTypes.createFrameOverlayLayer(), false, false)

    @JvmField
    val MATERIAL = Material(PASS, AlphaCutoffParameter.ZERO, true)

    private val defaults = DefaultTerrainRenderPasses.ALL
    private val extended = defaults + PASS

    @JvmStatic
    fun include(original: Array<TerrainRenderPass>): Array<TerrainRenderPass> =
        if (original === defaults) {
            extended
        } else if (original.any { it === PASS }) {
            original
        } else {
            original + PASS
        }

    @JvmStatic
    fun createBuffer(type: ChunkVertexType): ChunkMeshBufferBuilder = FrameBuffer(type)

    fun draw(event: RenderLevelStageEvent) {
        val renderer = SodiumWorldRenderer.instanceNullable() ?: return
        val manager = (renderer as FrameOverlayWorldRendererAccessor).renderSectionManager ?: return

        val camera = event.camera.position
        RenderUtils.withSavedRenderState {
            RenderDevice.enterManagedCode()
            try {
                manager.renderLayer(
                    ChunkRenderMatrices(event.projectionMatrix, event.poseStack.last().pose()),
                    PASS,
                    camera.x,
                    camera.y,
                    camera.z
                )
            } finally {
                RenderDevice.exitManagedCode()
            }
        }
    }

    /** Growth stays in vertex units: Embeddium 0.3.x's grow path mixes units. */
    private class FrameBuffer(type: ChunkVertexType) : ChunkMeshBufferBuilder(type, 0, false) {
        private val encoder = type.encoder
        private val stride = type.vertexFormat.stride
        private var data: ByteBuffer? = null
        private var capacity = 0
        private var vertices = 0
        private var section = 0

        override fun start(sectionIndex: Int) {
            vertices = 0
            section = sectionIndex
        }

        override fun push(values: Array<ChunkVertexEncoder.Vertex>, material: Material) {
            val required = vertices + values.size
            if (required > capacity) {
                capacity = maxOf(256, capacity * 2, required)
                data = MemoryUtil.memRealloc(data, capacity * stride)
            }

            var pointer = MemoryUtil.memAddress(checkNotNull(data), vertices * stride)
            for (vertex in values) pointer = encoder.write(pointer, material, vertex, section)
            vertices = required
        }

        override fun count(): Int = vertices

        override fun isEmpty(): Boolean = vertices == 0

        override fun slice(): ByteBuffer {
            check(vertices > 0)
            return MemoryUtil.memSlice(checkNotNull(data), 0, vertices * stride)
        }

        override fun destroy() {
            data?.let(MemoryUtil::memFree)
            data = null
            capacity = 0
            vertices = 0
        }
    }
}