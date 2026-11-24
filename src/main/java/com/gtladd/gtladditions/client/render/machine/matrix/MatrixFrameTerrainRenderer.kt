package com.gtladd.gtladditions.client.render.machine.matrix

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.render.StructureBlockLighting
import com.gtladd.gtladditions.client.render.StructureBlockLighting.FaceGroup
import com.gtladd.gtladditions.client.render.StructureRenderBuffers
import com.gtladd.gtladditions.client.render.StructureRenderBuffers.GroupedTerrainLayer
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.common.machine.multiblock.controller.ApocalypticWarpQuantumMatrix
import com.mojang.blaze3d.vertex.PoseStack
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.EnumMap
import kotlin.math.floor

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, value = [Dist.CLIENT])
object MatrixFrameTerrainRenderer {
    private val entries = Long2ObjectLinkedOpenHashMap<Entry>()
    private val staticEntries = Long2ObjectLinkedOpenHashMap<Entry>()
    private var buffers: List<GroupedTerrainLayer>? = null
    private val staticBlocks = EnumMap<Direction, GroupedTerrainLayer>(Direction::class.java)
    private var shaderTerrainActive = false
    private val lightingState = StructureBlockLighting.CacheState()
    private val shades = FloatArray(FaceGroup.entries.size)

    @JvmStatic
    fun useShaderCompatibleTerrainUploadMode() {
        // mc_Entity IDs belong to the current shaderpack; do not reuse another pack's VBOs.
        releaseBuffers()
        shaderTerrainActive = true
    }

    @JvmStatic
    fun useVanillaCompatibleTerrainUploadMode() {
        shaderTerrainActive = false
        releaseBuffers()
    }

    fun releaseBuffers() {
        releaseGeometry()
        entries.clear()
        staticEntries.clear()
    }

    private fun releaseGeometry() {
        buffers?.forEach(GroupedTerrainLayer::close)
        buffers = null
        staticBlocks.values.forEach(GroupedTerrainLayer::close)
        staticBlocks.clear()
        MatrixFrameModels.clear()
    }

    fun enqueue(blockEntity: BlockEntity, machine: ApocalypticWarpQuantumMatrix, ticks: Double) {
        entries.put(blockEntity.blockPos.asLong(), Entry(blockEntity, machine, ticks))
    }

    fun enqueueStatic(blockEntity: BlockEntity, machine: ApocalypticWarpQuantumMatrix) {
        staticEntries.put(blockEntity.blockPos.asLong(), Entry(blockEntity, machine, 0.0))
    }

    @SubscribeEvent
    @JvmStatic
    fun onRenderLevelStage(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return
        if (entries.isEmpty() && staticEntries.isEmpty()) return

        try {
            if (lightingState.changed()) releaseGeometry()
            val camera = event.camera.position

            for (entry in entries.values) {
                if (entry.blockEntity.isRemoved ||
                    !entry.machine.isFormed ||
                    entry.blockEntity.level !== Minecraft.getInstance().level
                ) {
                    continue
                }

                val pos = entry.blockEntity.blockPos
                event.poseStack.withPose {
                    translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z)
                    render(entry, this)
                }
            }

            if (staticEntries.isNotEmpty()) {
                for (entry in staticEntries.values) {
                    val machine = entry.machine
                    if (entry.blockEntity.isRemoved ||
                        !machine.isFormed ||
                        entry.blockEntity.level !== Minecraft.getInstance().level
                    ) {
                        continue
                    }

                    val pos = entry.blockEntity.blockPos
                    event.poseStack.withPose {
                        translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z)
                        renderStatic(entry, this)
                    }
                }
            }
        } finally {
            entries.clear()
            staticEntries.clear()
        }
    }

    private fun buildBuffers(): List<GroupedTerrainLayer> {
        val count = MatrixEffectGeometry.ENERGY_CORE_LAYER_INDEX
        val blocks = List(count, MatrixFrameModels::prepareLayer)
        val panels = List(count) { index ->
            MatrixFrameDetails.layer(index).panels.groupBy { panel ->
                val normal = panel.normal
                FaceGroup.of(Direction.getNearest(normal[0], normal[1], normal[2])).ordinal
            }
        }

        val sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation("forge", "white"))
        val pose = PoseStack()
        return StructureRenderBuffers.groupedTerrainLayers(count, FaceGroup.entries.size, shaderTerrainActive) { index, group, batch ->
            for ((type, groupedBlocks) in blocks[index][group]) {
                val builder = batch.builder(type)

                for (block in groupedBlocks) {
                    batch.withBlock(builder, block.state, block.position.x, block.position.y, block.position.z) {
                        MatrixFrameModels.write(builder, pose, index, block)
                    }
                }
            }

            // The neutral atlas sprite keeps the fittings in the pack's opaque terrain pass.
            for (panel in panels[index][group].orEmpty()) {
                val vertices = panel.vertices
                val normal = panel.normal
                val color = panel.color
                val builder = batch.builder(RenderType.solid())
                val blockX = floor((vertices[0] + vertices[6]) * 0.5).toInt()
                val blockY = floor((vertices[1] + vertices[7]) * 0.5).toInt()
                val blockZ = floor((vertices[2] + vertices[8]) * 0.5).toInt()
                // These are plain, non-emissive surfaces, not the original casing's material.
                batch.withBlock(builder, null, blockX, blockY, blockZ) {
                    for (i in 0..3) {
                        // Non-degenerate UVs keep Iris's generated tangent basis finite.
                        val u = if (i == 1 || i == 2) 12.0 else 4.0
                        val v = if (i >= 2) 12.0 else 4.0
                        builder.vertex(vertices[i * 3].toDouble(), vertices[i * 3 + 1].toDouble(), vertices[i * 3 + 2].toDouble())
                            .color(color[0], color[1], color[2], 1f)
                            .uv(sprite.getU(u), sprite.getV(v))
                            .uv2(LightTexture.pack(0, 13))
                            .normal(normal[0], normal[1], normal[2])
                            .endVertex()
                    }
                }
            }
        }
    }

    private fun render(entry: Entry, poseStack: PoseStack) {
        val frameBuffers = buffers ?: buildBuffers().also { buffers = it }
        val level = checkNotNull(entry.blockEntity.level)
        val yaw = when (entry.machine.frontFacing) {
            Direction.WEST -> -90f
            Direction.NORTH -> 180f
            Direction.EAST -> 90f
            else -> 0f
        }

        val worldPose = PoseStack()
        repeat(MatrixEffectGeometry.ENERGY_CORE_LAYER_INDEX) { index ->
            worldPose.withPose {
                // The same multi-axis transform, with no camera rotation mixed into the lighting.
                MatrixFrameTransforms.apply(this, index, entry.ticks, yaw)
                val worldNormal = last().normal()
                for (group in FaceGroup.entries) shades[group.ordinal] = group.shade(level, worldNormal)
                poseStack.withPose {
                    MatrixFrameTransforms.apply(this, index, entry.ticks, yaw)
                    frameBuffers[index].draw(last(), shades, batchShader = true)
                }
            }
        }
    }

    private fun renderStatic(entry: Entry, pose: PoseStack) {
        val level = checkNotNull(entry.blockEntity.level)
        val facing = entry.machine.frontFacing
        val blocks = staticBlocks.getOrPut(facing) { MatrixFrameModels.buildStaticBlocks(level, facing, shaderTerrainActive) }
        for (group in FaceGroup.entries) {
            val face = group.direction
            shades[group.ordinal] = level.getShade(face ?: Direction.UP, face != null)
        }
        blocks.draw(pose.last(), shades, batchShader = true)
    }

    private data class Entry(val blockEntity: BlockEntity, val machine: ApocalypticWarpQuantumMatrix, val ticks: Double)
}