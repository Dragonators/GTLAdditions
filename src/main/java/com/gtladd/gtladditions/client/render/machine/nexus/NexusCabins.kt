package com.gtladd.gtladditions.client.render.machine.nexus

import com.google.gson.JsonParser
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.render.StructureBlockLighting
import com.gtladd.gtladditions.client.render.StructureRenderBuffers
import com.gtladd.gtladditions.client.render.machine.matrix.MatrixFrameModels
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.common.machine.multiblock.controller.SubspaceCorridorNexusIndustrialArray
import com.gtladd.gtladditions.utils.SubspaceNexusAnimationClock
import com.gtladd.gtladditions.utils.SubspaceNexusTimeline
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.irisshaders.iris.Iris
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.ForgeRegistries
import org.lwjgl.opengl.GL11.*
import java.util.zip.GZIPInputStream
import kotlin.math.PI
import kotlin.math.sin

@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, value = [Dist.CLIENT])
object NexusCabins {
    private data class Entry(
        val blockEntity: BlockEntity,
        val machine: SubspaceCorridorNexusIndustrialArray,
        val animation: SubspaceNexusAnimationClock.Frame
    )

    private val entries = linkedMapOf<BlockPos, Entry>()
    private val buffers = mutableMapOf<Boolean, StructureRenderBuffers.GroupedTerrainLayer>()
    private val lighting = StructureBlockLighting.CacheState()
    private var shaderMode = false
    private var shaderPipeline: Any? = null

    fun enqueue(blockEntity: BlockEntity, machine: SubspaceCorridorNexusIndustrialArray, animation: SubspaceNexusAnimationClock.Frame) {
        entries[blockEntity.blockPos] = Entry(blockEntity, machine, animation)
    }

    fun release() {
        buffers.values.forEach { it.close() }
        buffers.clear()
        entries.clear()
    }

    private fun build(active: Boolean): StructureRenderBuffers.GroupedTerrainLayer {
        val data = GZIPInputStream(NexusGpu.bytes("cabins.json.gz").inputStream()).use { JsonParser.parseReader(it.reader()).asJsonObject }
        val palette = data.getAsJsonObject("palette").entrySet().associate { (code, material) ->
            val block = checkNotNull(ForgeRegistries.BLOCKS.getValue(ResourceLocation(material.asString)))
            var state = block.defaultBlockState()
            val property = block.stateDefinition.getProperty("active") as? BooleanProperty
            if (property != null) state = state.setValue(property, active)
            code.toInt() to state
        }

        val cells = data.getAsJsonArray("cells").map { it.asJsonArray.map { value -> value.asInt }.toIntArray() }
        val scale = data.get("scale").asFloat
        val anchors = data.getAsJsonArray("anchors").map { it.asJsonArray.map { value -> value.asDouble }.toDoubleArray() }
        val sectors = cells.associate { c -> BlockPos(c[0], c[1], c[2]) to c[5] }
        val states = cells.associate { c -> BlockPos(c[0], c[1], c[2]) to palette.getValue(c[3]) }

        val view = MatrixFrameModels.FrameView(states, checkNotNull(Minecraft.getInstance().level))
        val baker = StructureBlockLighting.Baker()
        val groups = List(StructureBlockLighting.FaceGroup.entries.size) {
            linkedMapOf<RenderType, MutableList<MatrixFrameModels.LitBlock>>()
        }

        val directions = arrayOf(Direction.WEST, Direction.EAST, Direction.SOUTH, Direction.NORTH, Direction.DOWN, Direction.UP)

        for (cell in cells) {
            if (cell[4] == 0) continue

            val pos = BlockPos(cell[0], cell[1], cell[2])
            val state = states.getValue(pos)
            val model = Minecraft.getInstance().blockRenderer.getBlockModel(state)
            val block = MatrixFrameModels.prepareBlock(
                model,
                view,
                pos,
                state,
                directions.filterIndexed { index, _ -> cell[4] and (1 shl index) != 0 }
            )

            for ((type, quads) in block.quads) {
                val lit = baker.bake(model, view, pos, state, type, quads)

                for ((group, grouped) in lit.groupBy { it.group }) {
                    groups[group.ordinal].getOrPut(type, ::mutableListOf).add(MatrixFrameModels.LitBlock(pos, state, grouped))
                }
            }
        }

        val pose = PoseStack()
        return StructureRenderBuffers.groupedTerrainLayers(1, groups.size, shaderMode) { _, group, batch ->
            for ((type, blocks) in groups[group]) {
                val builder = batch.builder(type)

                for (block in blocks) {
                    batch.withBlock(builder, block.state, block.position.x, block.position.y, block.position.z) {
                        pose.withPose {
                            val anchor = anchors[sectors.getValue(block.position)]
                            translate(anchor[0], anchor[1], anchor[2])
                            scale(scale, scale, scale)
                            translate(block.position.x - anchor[0], block.position.y - anchor[1], block.position.z - anchor[2])
                            block.quads.forEach { it.write(builder, last()) }
                        }
                    }
                }
            }
        }.single()
    }

    @SubscribeEvent
    @JvmStatic
    fun render(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES || entries.isEmpty()) return

        val pipeline = Iris.getPipelineManager().pipelineNullable
        val useShader = (pipeline as? ShaderRenderingPipeline)?.shouldOverrideShaders() == true
        if (pipeline !== shaderPipeline || useShader != shaderMode || lighting.changed()) {
            buffers.values.forEach { it.close() }
            buffers.clear()
            shaderMode = useShader
            shaderPipeline = pipeline
        }

        try {
            for (entry in entries.values) {
                if (entry.blockEntity.isRemoved ||
                    !entry.machine.isFormed ||
                    entry.blockEntity.level !== Minecraft.getInstance().level
                ) {
                    continue
                }

                val clock = SubspaceNexusTimeline.chartClock(entry.animation.animationState, entry.animation.tick)
                val angle = (32 * sin(PI * clock / 10)).toFloat()
                val buffer = buffers.getOrPut(entry.machine.recipeLogic.isWorking) { build(entry.machine.recipeLogic.isWorking) }

                val world = PoseStack()
                NexusScene.applyController(world, entry.machine)
                world.mulPose(Axis.YP.rotationDegrees(angle))
                val shades = FloatArray(StructureBlockLighting.FaceGroup.entries.size) { index ->
                    StructureBlockLighting.FaceGroup.entries[index].shade(checkNotNull(entry.blockEntity.level), world.last().normal())
                }

                val pos = entry.blockEntity.blockPos
                val camera = event.camera.position
                val winding = glGetInteger(GL_FRONT_FACE)
                try {
                    if (entry.machine.isFlipped) glFrontFace(GL_CW)
                    event.poseStack.withPose {
                        translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z)
                        NexusScene.applyController(this, entry.machine)
                        mulPose(Axis.YP.rotationDegrees(angle))
                        buffer.draw(last(), shades, batchShader = true)
                    }
                } finally {
                    glFrontFace(winding)
                }
            }
        } finally {
            entries.clear()
        }
    }
}
