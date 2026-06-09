package com.gtladd.gtladditions.utils.antichrist

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristRenderProfile
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks
import com.gtladd.gtladditions.common.machine.multiblock.structure.RingStructure
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.math.Axis
import it.unimi.dsi.fastutil.objects.Object2IntMap
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ChunkBufferBuilderPack
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.block.model.BakedQuad
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
import org.joml.Quaternionf

@OnlyIn(Dist.CLIENT)
object RingStructureVertexBuffer {

    val vanillaRingBuffers: Array<VertexBuffer> by lazy {
        try {
            Array(preparedRings.size) { tier ->
                uploadVanillaRing(preparedRings[tier])
            }
        } catch (e: Exception) {
            GTLAdditions.LOGGER.error("Failed to build vanilla ring VertexBuffers", e)
            Array(3) { VertexBuffer(VertexBuffer.Usage.STATIC) }
        }
    }

    val ringBuffers: Array<VertexBuffer>
        get() = vanillaRingBuffers

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

    private val blockStateIds: Object2IntMap<BlockState>? by lazy {
        WorldRenderingSettings.INSTANCE.blockStateIds
    }

    private val preparedRings: Array<PreparedRing> by lazy {
        Array(RingStructure.RINGS.size) { tier ->
            prepareRing(RingStructure.RINGS[tier])
        }
    }

    private val terrainUploadedRings: Array<UploadedTerrainRing> by lazy {
        preparedRings.map(::uploadTerrainRing).toTypedArray()
    }

    fun renderTerrainBatched(profile: AntichristRenderProfile, poseStack: PoseStack) {
        terrainUploadedRings.forEachIndexed { index, uploadedRing ->
            poseStack.withPose {
                translate(profile.starPos.x, profile.starPos.y, profile.starPos.z)

                when (profile.facing) {
                    Direction.NORTH -> mulPose(Axis.YP.rotationDegrees(270f))
                    Direction.SOUTH -> mulPose(Axis.YP.rotationDegrees(90f))
                    Direction.WEST -> mulPose(Axis.YP.rotationDegrees(0f))
                    Direction.EAST -> mulPose(Axis.YP.rotationDegrees(180f))
                    else -> {}
                }

                if (profile.isWorking) {
                    val direction = if (index == 1) -1.0f else 1.0f
                    val speedMultiplier = 0.4f + index * 0.4f
                    val angleOffset = index * 120f
                    val rotationAngle = (profile.tick * speedMultiplier * 2.0f * direction + angleOffset) % 360.0f

                    mulPose(Quaternionf().fromAxisAngleDeg(1.0f, 0.0f, 0.0f, rotationAngle))
                }

                renderUploadedTerrainRing(uploadedRing, this)
            }
        }
    }

    private fun uploadVanillaRing(preparedRing: PreparedRing): VertexBuffer {
        val buffer = VertexBuffer(VertexBuffer.Usage.STATIC)
        val bufferBuilder = Tesselator.getInstance().builder
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK)
        val poseStack = PoseStack()
        preparedRing.blocksByRenderType.values.forEach { preparedBlocks ->
            preparedBlocks.forEach { preparedBlock ->
                poseStack.withPose {
                    translate(
                        preparedBlock.localX.toDouble(),
                        preparedBlock.localY.toDouble(),
                        preparedBlock.localZ.toDouble()
                    )

                    val pose = last()
                    preparedBlock.quads.forEach { quad ->
                        bufferBuilder.putBulkData(
                            pose,
                            quad,
                            1.0f,
                            1.0f,
                            1.0f,
                            1.0f,
                            preparedBlock.light,
                            OverlayTexture.NO_OVERLAY,
                            true
                        )
                    }
                }
            }
        }

        buffer.bind()
        buffer.upload(bufferBuilder.end())
        VertexBuffer.unbind()
        return buffer
    }

    private fun prepareRing(
        structure: Array<Array<String>>
    ): PreparedRing {
        val random = RandomSource.create()
        val centerX = structure.size / 2.0
        val centerY = structure[0].size / 2.0
        val centerZ = structure[0][0].length / 2.0
        val preparedBlocksByRenderType = linkedMapOf<RenderType, MutableList<PreparedBlock>>()

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
                    val light = LightTexture.pack(
                        block.getLightEmission(blockState, EmptyBlockGetter.INSTANCE, BlockPos.ZERO),
                        13
                    )
                    val localX = (x - centerX).toInt()
                    val localY = (centerY - y - 1).toInt()
                    val localZ = (z - centerZ).toInt()

                    val modelData = ModelData.EMPTY
                    random.setSeed(42L)
                    for (renderType in model.getRenderTypes(blockState, random, modelData)) {
                        val quads = collectModelQuads(
                            model,
                            blockState,
                            visibleFaces,
                            random,
                            renderType,
                            modelData
                        )
                        if (quads.isEmpty()) continue

                        preparedBlocksByRenderType.getOrPut(renderType, ::mutableListOf).add(
                            PreparedBlock(
                                localX,
                                localY,
                                localZ,
                                resolveBlockStateId(blockState),
                                light,
                                quads
                            )
                        )
                    }
                }
            }
        }

        return PreparedRing(
            preparedBlocksByRenderType.mapValues { (_, blocks) -> blocks.toList() }
        )
    }

    private fun uploadTerrainRing(preparedRing: PreparedRing): UploadedTerrainRing {
        val poseStack = PoseStack()
        val builderPack = ChunkBufferBuilderPack()
        val uploadedBuffers = linkedMapOf<RenderType, VertexBuffer>()

        preparedRing.blocksByRenderType.forEach { (renderType, preparedBlocks) ->
            val consumer = builderPack.builder(renderType)
            consumer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK)
            preparedBlocks.forEach { preparedBlock ->
                poseStack.withPose {
                    translate(
                        preparedBlock.localX.toDouble(),
                        preparedBlock.localY.toDouble(),
                        preparedBlock.localZ.toDouble()
                    )

                    if (consumer is BlockSensitiveBufferBuilder) {
                        consumer.beginBlock(
                            preparedBlock.blockStateId,
                            (-1).toShort(),
                            preparedBlock.localX and 15,
                            preparedBlock.localY and 15,
                            preparedBlock.localZ and 15
                        )
                    }

                    try {
                        val pose = last()
                        preparedBlock.quads.forEach { quad ->
                            consumer.putBulkData(
                                pose,
                                quad,
                                1.0f,
                                1.0f,
                                1.0f,
                                1.0f,
                                preparedBlock.light,
                                OverlayTexture.NO_OVERLAY,
                                true
                            )
                        }
                    } finally {
                        if (consumer is BlockSensitiveBufferBuilder) {
                            consumer.endBlock()
                        }
                    }
                }
            }

            val vertexBuffer = VertexBuffer(VertexBuffer.Usage.STATIC)
            vertexBuffer.bind()
            vertexBuffer.upload(consumer.end())
            VertexBuffer.unbind()
            uploadedBuffers[renderType] = vertexBuffer
        }

        return UploadedTerrainRing(uploadedBuffers)
    }

    private fun renderUploadedTerrainRing(
        uploadedRing: UploadedTerrainRing,
        poseStack: PoseStack
    ) {
        uploadedRing.buffersByRenderType.forEach { (renderType, vertexBuffer) ->
            renderType.setupRenderState()
            val shader = RenderSystem.getShader()
            if (shader != null) {
                vertexBuffer.bind()
                vertexBuffer.drawWithShader(
                    poseStack.last().pose(),
                    RenderSystem.getProjectionMatrix(),
                    shader
                )
                VertexBuffer.unbind()
            }
            renderType.clearRenderState()
        }
    }

    private fun collectModelQuads(
        model: BakedModel,
        blockState: BlockState,
        visibleFaces: List<Direction>,
        random: RandomSource,
        renderType: RenderType,
        modelData: ModelData
    ): List<BakedQuad> {
        val quads = ArrayList<BakedQuad>()

        random.setSeed(42L)
        quads.addAll(model.getQuads(blockState, null, random, modelData, renderType))

        for (direction in visibleFaces) {
            random.setSeed(42L)
            quads.addAll(model.getQuads(blockState, direction, random, modelData, renderType))
        }

        return quads
    }

    private fun resolveBlockStateId(blockState: BlockState): Short {
        val ids = blockStateIds ?: return (-1).toShort()
        return ids.getOrDefault(blockState, -1).toShort()
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

    private data class PreparedRing(
        val blocksByRenderType: Map<RenderType, List<PreparedBlock>>
    )

    private data class PreparedBlock(
        val localX: Int,
        val localY: Int,
        val localZ: Int,
        val blockStateId: Short,
        val light: Int,
        val quads: List<BakedQuad>
    )

    private data class UploadedTerrainRing(
        val buffersByRenderType: Map<RenderType, VertexBuffer>
    )
}