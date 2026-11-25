package com.gtladd.gtladditions.client.render.machine.dimensionfocus

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.render.StructureBlockLighting
import com.gtladd.gtladditions.client.render.StructureRenderBuffers
import com.gtladd.gtladditions.client.render.StructureRenderBuffers.GroupedTerrainLayer
import com.gtladd.gtladditions.client.render.machine.dimensionfocus.DimensionFocusOrbitLighting.FaceGroup
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.common.machine.multiblock.controller.DimensionFocusInfinityCraftingArray
import com.gtladd.gtladditions.common.machine.multiblock.structure.layout.DimensionFocusBlockLayout
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.client.model.data.ModelData
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.ForgeRegistries

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, value = [Dist.CLIENT])
object DimensionFocusOrbitRenderer {
    private val entries = Long2ObjectLinkedOpenHashMap<Entry>()
    private var buffers: List<GroupedTerrainLayer>? = null
    private var shaderCompatible = false
    private val lightingState = StructureBlockLighting.CacheState()
    private val shades = FloatArray(FaceGroup.entries.size)

    fun enqueue(blockEntity: BlockEntity, machine: DimensionFocusInfinityCraftingArray, ticks: Double) {
        entries.put(blockEntity.blockPos.asLong(), Entry(blockEntity, machine, ticks))
    }

    fun releaseBuffers() {
        releaseGeometry()
        entries.clear()
    }

    private fun releaseGeometry() {
        buffers?.forEach(GroupedTerrainLayer::close)
        buffers = null
    }

    @JvmStatic
    fun useShaderCompatibleTerrainUploadMode() {
        releaseBuffers()
        shaderCompatible = true
    }

    @JvmStatic
    fun useVanillaCompatibleTerrainUploadMode() {
        releaseBuffers()
        shaderCompatible = false
    }

    @SubscribeEvent
    @JvmStatic
    fun onRenderLevelStage(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES || entries.isEmpty()) return
        try {
            val camera = event.camera.position

            for (entry in entries.values) {
                val machine = entry.machine
                if (entry.blockEntity.isRemoved || !machine.isFormed || entry.blockEntity.level !== Minecraft.getInstance().level) continue
                if (!event.frustum.isVisible(DimensionFocusOrbitGeometry.bounds(machine.pos, machine.frontFacing))) continue
                event.poseStack.withPose {
                    translate(machine.pos.x - camera.x, machine.pos.y - camera.y, machine.pos.z - camera.z)
                    render(entry, this)
                }
            }
        } finally {
            entries.clear()
        }
    }

    private fun render(entry: Entry, pose: PoseStack) {
        if (lightingState.changed()) releaseGeometry()
        val uploaded = buffers ?: buildBuffers().also { buffers = it }
        val center = DimensionFocusBlockLayout.centerOffset
        val level = checkNotNull(entry.blockEntity.level)
        val facingYaw = DimensionFocusOrbitGeometry.yaw(entry.machine.frontFacing)
        pose.withPose {
            translate(0.5, 0.5, 0.5)
            mulPose(Axis.YP.rotationDegrees(facingYaw))
            translate(center.x.toDouble(), center.y.toDouble(), center.z.toDouble())

            repeat(DimensionFocusBlockLayout.layers.size) { layer ->
                val orbitYaw = DimensionFocusOrbitGeometry.angleDegrees(layer, entry.ticks)
                for (group in FaceGroup.entries) shades[group.ordinal] = group.shade(level, facingYaw + orbitYaw)
                withPose {
                    mulPose(Axis.YP.rotationDegrees(orbitYaw))
                    uploaded[layer].draw(last(), shades, batchShader = true)
                }
            }
        }
    }

    private fun buildBuffers(): List<GroupedTerrainLayer> {
        val materials = mapOf(
            'A' to "gtlcore:dimensionally_transcendent_casing",
            'D' to "gtladditions:temporal_anchor_field_casing",
            'K' to "kubejs:containment_field_generator",
            'L' to "gtlcore:graviton_field_constraint_casing",
            'M' to "kubejs:annihilate_core"
        ).mapValues { (_, id) ->
            checkNotNull(ForgeRegistries.BLOCKS.getValue(ResourceLocation(id))) { "Missing orbit block: $id" }.defaultBlockState()
        }

        val minecraft = Minecraft.getInstance()
        val models = materials.mapValues { (_, state) -> minecraft.blockRenderer.getBlockModel(state) }
        val baker = StructureBlockLighting.Baker()
        val random = RandomSource.create(42L)
        val prepared = DimensionFocusBlockLayout.layers.map { cells ->
            val states = cells.associate { it.position to materials.getValue(it.symbol) }
            val view = StructureBlockLighting.LocalView(states, checkNotNull(minecraft.level))
            val groups = List(FaceGroup.entries.size) { linkedMapOf<RenderType, MutableList<PreparedCell>>() }
            val renderTypes = linkedSetOf<RenderType>()

            for (cell in cells) {
                val state = states.getValue(cell.position)
                val model = models.getValue(cell.symbol)
                // Preserve position-specific CTM connections within each rigidly moving layer.
                val data = model.getModelData(view, cell.position, state, ModelData.EMPTY)
                val offset = state.getOffset(view, cell.position)
                random.setSeed(42L)

                for (type in model.getRenderTypes(state, random, data)) {
                    renderTypes.add(type)
                    val lit = baker.bakeModel(model, view, cell.position, state, type, data, true)

                    for ((group, quads) in lit.groupBy { FaceGroup.of(it.quad) }) {
                        groups[group.ordinal].getOrPut(type, ::mutableListOf).add(PreparedCell(cell.position, state, offset, quads))
                    }
                }
            }

            groups.map { blocks ->
                renderTypes.filter { it in blocks }.associateWith(blocks::getValue)
            }
        }

        val pose = PoseStack()
        return StructureRenderBuffers.groupedTerrainLayers(prepared.size, FaceGroup.entries.size, shaderCompatible) { index, group, batch ->
            for ((type, cells) in prepared[index][group]) {
                val builder = batch.builder(type)

                for (cell in cells) {
                    val pos = cell.position
                    pose.withPose {
                        translate(pos.x - 0.5, pos.y - 0.5, pos.z - 0.5)
                        // Preserve tesselateBlock's model offset when replaying cached quads.
                        translate(cell.offset.x, cell.offset.y, cell.offset.z)
                        batch.withBlock(builder, cell.state, pos.x, pos.y, pos.z) {
                            for (quad in cell.quads) quad.write(builder, last())
                        }
                    }
                }
            }
        }
    }

    private data class PreparedCell(
        val position: BlockPos,
        val state: BlockState,
        val offset: Vec3,
        val quads: List<StructureBlockLighting.LitQuad>
    )

    private data class Entry(val blockEntity: BlockEntity, val machine: DimensionFocusInfinityCraftingArray, val ticks: Double)
}

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, value = [Dist.CLIENT], bus = Mod.EventBusSubscriber.Bus.MOD)
object DimensionFocusOrbitReloadListener {
    @SubscribeEvent
    @JvmStatic
    fun onRegisterReloadListeners(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(
            ResourceManagerReloadListener {
                if (RenderSystem.isOnRenderThread()) {
                    DimensionFocusOrbitRenderer.releaseBuffers()
                } else {
                    RenderSystem.recordRenderCall { DimensionFocusOrbitRenderer.releaseBuffers() }
                }
            }
        )
    }
}
