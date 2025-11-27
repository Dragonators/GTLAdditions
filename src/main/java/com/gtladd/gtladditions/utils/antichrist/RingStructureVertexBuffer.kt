package com.gtladd.gtladditions.utils.antichrist

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks
import com.gtladd.gtladditions.common.machine.muiltblock.structure.RingStructure
import com.mojang.blaze3d.vertex.*
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.EmptyBlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.model.data.ModelData
import org.gtlcore.gtlcore.client.ClientUtil

@OnlyIn(Dist.CLIENT)
object RingStructureVertexBuffer {

    val ringBuffers: Array<VertexBuffer> by lazy {
        try {
            Array(3) { tier ->
                buildRingBuffer(RingStructure.RINGS[tier])
            }
        } catch (e: Exception) {
            GTLAdditions.Companion.LOGGER.error("Failed to build ring VertexBuffers", e)
            Array(3) { VertexBuffer(VertexBuffer.Usage.STATIC) }
        }
    }

    val BLOCK_MAPPER: Map<Char, Block> by lazy {
        mapOf(
            'C' to GTLAddBlocks.GOD_FORGE_INNER_CASING.get(),
            'D' to GTLAddBlocks.GOD_FORGE_SUPPORT_CASING.get(),
            'E' to GTLAddBlocks.SUPRACHRONAL_MAGNETIC_CONFINEMENT_CASING.get(),
            'F' to GTLAddBlocks.GOD_FORGE_ENERGY_CASING.get(),
            'G' to GTLAddBlocks.REMOTE_GRAVITON_FLOW_REGULATOR.get(),
            'H' to GTLAddBlocks.SPATIALLY_TRANSCENDENT_GRAVITATIONAL_LENS.get(),
            'I' to GTLAddBlocks.CENTRAL_GRAVITON_FLOW_REGULATOR.get(),
            'J' to GTLAddBlocks.GOD_FORGE_TRIM_CASING.get(),
            'K' to GTLAddBlocks.MEDIARY_GRAVITON_FLOW_REGULATOR.get()
        )
    }

    private fun buildRingBuffer(structure: Array<Array<String>>): VertexBuffer {
        val buffer = VertexBuffer(VertexBuffer.Usage.STATIC)
        val bufferBuilder = Tesselator.getInstance().builder
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK)

        val poseStack = PoseStack()
        val random = RandomSource.create()

        val centerX = structure.size / 2.0
        val centerY = structure[0].size / 2.0
        val centerZ = structure[0][0].length / 2.0

        for (x in structure.indices) {
            val plane = structure[x]
            for (y in plane.indices) {
                val row = plane[y]
                for (z in row.indices) {
                    val letter = row[z]
                    if (letter == ' ') continue

                    val block = BLOCK_MAPPER[letter] ?: continue
                    if (block == Blocks.AIR) continue

                    val visibleFaces = getVisibleFaces(structure, x, y, z)
                    if (visibleFaces.isEmpty()) continue

                    val blockState = block.defaultBlockState()
                    val model = ClientUtil.blockRenderer().getBlockModel(blockState)

                    poseStack.pushPose()
                    poseStack.translate(
                        (x - centerX),
                        (centerY - y - 1),
                        (z - centerZ)
                    )

                    val light = LightTexture.pack(
                        block.getLightEmission(blockState, EmptyBlockGetter.INSTANCE, BlockPos.ZERO),
                        13
                    )

                    renderBlockModelFaces(
                        model, blockState, visibleFaces,
                        light, poseStack, bufferBuilder, random
                    )

                    poseStack.popPose()
                }
            }
        }

        buffer.bind()
        buffer.upload(bufferBuilder.end())
        VertexBuffer.unbind()

        return buffer
    }

    private fun renderBlockModelFaces(
        model: BakedModel,
        blockState: BlockState,
        visibleFaces: List<Direction>,
        light: Int,
        poseStack: PoseStack,
        bufferBuilder: BufferBuilder,
        random: RandomSource
    ) {
        val pose = poseStack.last()
        val modelData = ModelData.EMPTY
        val renderType: RenderType? = null
        val overlay = OverlayTexture.NO_OVERLAY

        random.setSeed(42L)
        for (quad in model.getQuads(blockState, null, random, modelData, renderType)) {
            bufferBuilder.putBulkData(
                pose, quad,
                1.0f, 1.0f, 1.0f, 1.0f,
                light, overlay, true
            )
        }

        for (direction in visibleFaces) {
            random.setSeed(42L)
            for (quad in model.getQuads(blockState, direction, random, modelData, renderType)) {
                bufferBuilder.putBulkData(
                    pose, quad,
                    1.0f, 1.0f, 1.0f, 1.0f,
                    light, overlay, true
                )
            }
        }
    }

    private fun getVisibleFaces(
        struct: Array<Array<String>>,
        x: Int,
        y: Int,
        z: Int
    ): List<Direction> {
        val visibleFaces = mutableListOf<Direction>()
        val currentChar = struct[x][y][z]

        val checks = arrayOf(
            DirectionCheck(-1, 0, 0, x > 0, Direction.WEST),
            DirectionCheck(1, 0, 0, x < struct.size - 1, Direction.EAST),
            DirectionCheck(0, -1, 0, y > 0, Direction.UP),
            DirectionCheck(0, 1, 0, y < struct[0].size - 1, Direction.DOWN),
            DirectionCheck(0, 0, -1, z > 0, Direction.NORTH),
            DirectionCheck(0, 0, 1, z < struct[0][0].length - 1, Direction.SOUTH)
        )

        for (check in checks) {
            if (check.isInBounds) {
                val neighborChar = struct[x + check.dx][y + check.dy][z + check.dz]
                if (!shouldCullFace(currentChar, neighborChar)) {
                    visibleFaces.add(check.direction)
                }
            } else {
                visibleFaces.add(check.direction)
            }
        }

        return visibleFaces
    }

    private fun shouldCullFace(current: Char, neighbor: Char): Boolean {
        if (neighbor == ' ') return false

        val neighborBlock = BLOCK_MAPPER[neighbor] ?: return false
        if (neighborBlock == Blocks.AIR) return false
        if (current != neighbor) return false

        val state = neighborBlock.defaultBlockState()
        return state.isSolidRender(Minecraft.getInstance().level!!, BlockPos.ZERO)
    }

    private data class DirectionCheck(
        val dx: Int,
        val dy: Int,
        val dz: Int,
        val isInBounds: Boolean,
        val direction: Direction
    )
}