package com.gtladd.gtladditions.client.render

import com.gtladd.gtladditions.client.RenderUtils
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder
import net.minecraft.client.renderer.ChunkBufferBuilderPack
import net.minecraft.client.renderer.RenderType
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

/** Shared terrain builders; callers retain their own geometry, vertex order and transforms. */
@OnlyIn(Dist.CLIENT)
object StructureRenderBuffers {
    // 1.20.1 never frees BufferBuilder memory; builder sets live for the process and serve one batch at a time.
    private val idleShaderBuilders = ArrayDeque<TerrainBuilders>()
    private val idleVanillaBuilders = ArrayDeque<TerrainBuilders>()

    fun terrainLayers(
        count: Int,
        shaderCompatible: Boolean,
        write: (Int, TerrainBatch) -> Unit
    ): List<Map<RenderType, VertexBuffer>> = TerrainBatch(shaderCompatible).use { batch ->
        val layers = ArrayList<Map<RenderType, VertexBuffer>>(count)
        try {
            repeat(count) { index ->
                write(index, batch)
                layers.add(batch.uploadLayer())
            }
            return layers
        } catch (error: Throwable) {
            layers.forEach { RenderUtils.releaseBuffers(it.values) }
            throw error
        }
    }

    fun groupedTerrainLayers(
        count: Int,
        groupCount: Int,
        shaderCompatible: Boolean,
        write: (Int, Int, TerrainBatch) -> Unit
    ): List<GroupedTerrainLayer> {
        val uploaded = terrainLayers(count * groupCount, shaderCompatible) { index, batch ->
            write(index / groupCount, index % groupCount, batch)
        }

        return try {
            uploaded.chunked(groupCount).map(::GroupedTerrainLayer)
        } catch (error: Throwable) {
            uploaded.forEach { RenderUtils.releaseBuffers(it.values) }
            throw error
        }
    }

    class GroupedTerrainLayer internal constructor(groups: List<Map<RenderType, VertexBuffer>>) {
        private val batches = buildList {
            val opaque = linkedMapOf<RenderType, MutableList<GroupBuffer>>()
            fun flushOpaque() {
                for ((type, buffers) in opaque) add(DrawBatch(type, buffers))
                opaque.clear()
            }

            groups.forEachIndexed { group, types ->
                for ((type, buffer) in types) {
                    val grouped = GroupBuffer(group, buffer)
                    if (type === RenderType.solid() || type === RenderType.cutout() || type === RenderType.cutoutMipped()) {
                        opaque.getOrPut(type, ::mutableListOf).add(grouped)
                    } else {
                        // Do not move any draw across translucent or unknown render types.
                        flushOpaque()
                        add(DrawBatch(type, listOf(grouped)))
                    }
                }
            }

            flushOpaque()
        }

        fun draw(pose: PoseStack.Pose, shades: FloatArray, batchShader: Boolean = false) {
            val color = RenderSystem.getShaderColor()
            val red = color[0]
            val green = color[1]
            val blue = color[2]
            val alpha = color[3]
            try {
                for ((type, buffers) in batches) {
                    RenderUtils.withRenderType(type) {
                        val shader = RenderSystem.getShader() ?: return@withRenderType
                        RenderUtils.withShaderBatch(shader, batchShader && buffers.size > 1) {
                            for ((group, buffer) in buffers) {
                                val shade = shades[group]
                                RenderSystem.setShaderColor(red * shade, green * shade, blue * shade, alpha)
                                RenderUtils.withBoundBuffer(buffer) {
                                    buffer.drawWithShader(pose.pose(), RenderSystem.getProjectionMatrix(), shader)
                                }
                            }
                        }
                    }
                }
            } finally {
                RenderSystem.setShaderColor(red, green, blue, alpha)
            }
        }

        fun close() {
            for ((_, buffers) in batches) for ((_, buffer) in buffers) buffer.close()
        }

        private data class GroupBuffer(val group: Int, val buffer: VertexBuffer)

        private data class DrawBatch(val type: RenderType, val buffers: List<GroupBuffer>)
    }

    class TerrainBatch internal constructor(private val shaderCompatible: Boolean) : AutoCloseable {
        private val builders = idleBuilders(shaderCompatible).removeLastOrNull() ?: TerrainBuilders(shaderCompatible)
        private val started = linkedSetOf<RenderType>()
        private val materialIds = HashMap<BlockState, Short>()

        fun builder(type: RenderType): BufferBuilder {
            val builder = builders[type]
            if (started.add(type)) builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK)
            return builder
        }

        // This cache lives only for one build, never across a shaderpack/material-map change.
        fun materialId(state: BlockState): Short = materialIds.getOrPut(state) {
            WorldRenderingSettings.INSTANCE.blockStateIds?.getOrDefault(state, -1)?.toShort() ?: (-1).toShort()
        }

        inline fun withBlock(builder: BufferBuilder, state: BlockState?, x: Int, y: Int, z: Int, write: () -> Unit) {
            if (builder is BlockSensitiveBufferBuilder) {
                builder.beginBlock(state?.let(::materialId) ?: (-1).toShort(), (-1).toShort(), x and 15, y and 15, z and 15)
            }

            try {
                write()
            } finally {
                if (builder is BlockSensitiveBufferBuilder) builder.endBlock()
            }
        }

        internal fun uploadLayer(): Map<RenderType, VertexBuffer> = uploadLayer(RenderUtils::uploadStaticBuffer)

        internal fun uploadLayer(upload: (BufferBuilder) -> VertexBuffer): Map<RenderType, VertexBuffer> {
            val uploaded = linkedMapOf<RenderType, VertexBuffer>()
            try {
                for (type in started) uploaded[type] = upload(builders[type])
                return uploaded
            } catch (error: Throwable) {
                RenderUtils.releaseBuffers(uploaded.values)
                throw error
            } finally {
                started.clear()
            }
        }

        override fun close() {
            builders.abandonUnfinished()
            idleBuilders(shaderCompatible).addLast(builders)
        }
    }

    private fun idleBuilders(shaderCompatible: Boolean) = if (shaderCompatible) idleShaderBuilders else idleVanillaBuilders

    private class TerrainBuilders(shaderCompatible: Boolean) {
        // Iris extends builders created by this pack. Keep that construction path intact.
        private val pack = if (shaderCompatible) ChunkBufferBuilderPack() else null
        private val builders = HashMap<RenderType, BufferBuilder>()

        operator fun get(type: RenderType): BufferBuilder =
            builders.getOrPut(type) { pack?.builder(type) ?: BufferBuilder(type.bufferSize()) }

        fun abandonUnfinished() {
            for (builder in builders.values) if (builder.building()) builder.end().release()
        }
    }
}