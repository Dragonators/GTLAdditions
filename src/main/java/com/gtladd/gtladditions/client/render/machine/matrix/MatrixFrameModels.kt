package com.gtladd.gtladditions.client.render.machine.matrix

import com.gtladd.gtladditions.client.render.StructureBlockLighting
import com.gtladd.gtladditions.client.render.withPose
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.RandomSource
import net.minecraft.world.level.BlockAndTintGetter
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.model.data.ModelData
import net.minecraftforge.registries.ForgeRegistries
import java.util.IdentityHashMap

@OnlyIn(Dist.CLIENT)
object MatrixFrameModels {
    data class PreparedBlock(
        val position: BlockPos,
        val state: BlockState,
        val quads: Map<RenderType, List<BakedQuad>>
    )

    data class LitBlock(val position: BlockPos, val state: BlockState, val quads: List<StructureBlockLighting.LitQuad>)

    class FrameView(blocks: Map<BlockPos, BlockState>, lighting: BlockAndTintGetter) : StructureBlockLighting.LocalView(blocks, lighting)

    private val palette = HashMap<ResourceLocation, BlockState>()
    private val models = IdentityHashMap<BlockState, BakedModel>()

    private data class FrameCell(val position: BlockPos, val material: ResourceLocation, val visible: List<Direction>)

    private val geometry by lazy {
        MatrixEffectGeometry.layers.map { layer ->
            MatrixEffectGeometry.unitBlocks(layer).map { cell ->
                FrameCell(
                    BlockPos(cell.x, cell.y, cell.z),
                    ResourceLocation(cell.material),
                    cell.faces.map { face ->
                        Direction.fromAxisAndDirection(
                            Direction.Axis.values()[face.axis],
                            if (face.sign > 0) Direction.AxisDirection.POSITIVE else Direction.AxisDirection.NEGATIVE
                        )
                    }
                )
            }
        }
    }

    fun clear() {
        palette.clear()
        models.clear()
    }

    fun prepareLayer(index: Int): List<Map<RenderType, List<LitBlock>>> {
        val cells = geometry[index]
        val states = cells.associate { cell ->
            cell.position to palette.getOrPut(cell.material) {
                (ForgeRegistries.BLOCKS.getValue(cell.material) ?: Blocks.AIR).defaultBlockState()
            }
        }

        val view = FrameView(states, checkNotNull(Minecraft.getInstance().level))
        val baker = StructureBlockLighting.Baker()
        val groups = List(StructureBlockLighting.FaceGroup.entries.size) { linkedMapOf<RenderType, MutableList<LitBlock>>() }

        for (cell in cells) {
            val pos = cell.position
            val state = states.getValue(pos)
            val model = models.getOrPut(state) { Minecraft.getInstance().blockRenderer.getBlockModel(state) }
            // Keep model data and quads per position: equal block states can have different CTM connections.
            val block = prepareBlock(model, view, pos, state, cell.visible)

            for ((type, quads) in block.quads) {
                val lit = baker.bake(model, view, pos, state, type, quads)

                for ((group, grouped) in lit.groupBy { it.group }) {
                    groups[group.ordinal].getOrPut(type, ::mutableListOf).add(LitBlock(pos, state, grouped))
                }
            }
        }

        return groups
    }

    fun prepareBlock(
        model: BakedModel,
        view: BlockAndTintGetter,
        pos: BlockPos,
        state: BlockState,
        visible: List<Direction>
    ): PreparedBlock {
        // LDLib's CTM wrapper stores WORLD and POS in this data. ModelData.EMPTY loses all connections.
        val data = model.getModelData(view, pos, state, ModelData.EMPTY)
        val random = RandomSource.create(42L)
        val byType = model.getRenderTypes(state, random, data).associateWith { type ->
            val quads = ArrayList<BakedQuad>()
            random.setSeed(42L)
            quads.addAll(model.getQuads(state, null, random, data, type).filter { it.direction in visible })

            for (face in visible) {
                random.setSeed(42L)
                quads.addAll(model.getQuads(state, face, random, data, type))
            }

            quads.toList()
        }

        return PreparedBlock(pos, state, byType)
    }

    fun write(consumer: VertexConsumer, pose: PoseStack, index: Int, block: LitBlock) {
        val layer = MatrixEffectGeometry.layers[index]
        val half = MatrixEffectGeometry.unitSide(layer) / 2.0
        pose.withPose {
            translate(
                (block.position.x - half) * layer.thickness,
                (block.position.y - half) * layer.thickness,
                (block.position.z - half) * layer.thickness
            )
            scale(layer.thickness, layer.thickness, layer.thickness)
            for (quad in block.quads) quad.write(consumer, last())
        }
    }
}