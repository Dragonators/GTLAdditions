package com.gtladd.gtladditions.utils.antichrist

import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristRenderProfile
import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristRingTransforms
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks
import com.gtladd.gtladditions.common.machine.multiblock.structure.RingStructure
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
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

@OnlyIn(Dist.CLIENT)
object RingStructureVertexBuffer {

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

    private var vanillaTerrainUploadedRings: Array<UploadedTerrainRing>? = null
    private var shaderTerrainUploadedRings: Array<UploadedTerrainRing>? = null
    private var currentTerrainUploadedRings: Array<UploadedTerrainRing>? = null
    private var useShaderCompatibleTerrainUpload = false

    fun renderTerrainBatched(profile: AntichristRenderProfile, poseStack: PoseStack) {
        (currentTerrainUploadedRings ?: ensureCurrentTerrainUploadedRings()).forEachIndexed { index, uploadedRing ->
            poseStack.withPose {
                AntichristRingTransforms.apply(profile, index, this)

                renderUploadedTerrainRing(uploadedRing, this)
            }
        }
    }

    private fun prepareRing(
        structure: Array<Array<String>>
    ): PreparedRing {
        val random = RandomSource.create()
        val centerX = structure.size / 2.0
        val centerY = structure[0].size / 2.0
        val centerZ = structure[0][0].length / 2.0
        val blockCenterX = structure.size / 2
        val blockCenterY = structure[0].size / 2
        val blockCenterZ = structure[0][0].length / 2
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
                    val localX = x - centerX
                    val localY = centerY - y - 1
                    val localZ = z - centerZ
                    val blockLocalX = x - blockCenterX
                    val blockLocalY = blockCenterY - y
                    val blockLocalZ = z - blockCenterZ

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
                                blockLocalX,
                                blockLocalY,
                                blockLocalZ,
                                blockState,
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

    private fun ensureCurrentTerrainUploadedRings(): Array<UploadedTerrainRing> =
        if (useShaderCompatibleTerrainUpload) {
            ensureShaderTerrainUploadedRings()
        } else {
            ensureVanillaTerrainUploadedRings()
        }

    private fun ensureVanillaTerrainUploadedRings(): Array<UploadedTerrainRing> =
        vanillaTerrainUploadedRings ?: buildTerrainUploadedRings(useBlockSensitiveBuilder = false).also {
            vanillaTerrainUploadedRings = it
            currentTerrainUploadedRings = it
        }

    private fun ensureShaderTerrainUploadedRings(): Array<UploadedTerrainRing> =
        shaderTerrainUploadedRings ?: buildTerrainUploadedRings(useBlockSensitiveBuilder = true).also {
            shaderTerrainUploadedRings = it
            currentTerrainUploadedRings = it
        }

    private fun buildTerrainUploadedRings(useBlockSensitiveBuilder: Boolean): Array<UploadedTerrainRing> {
        val preparedRings = Array(RingStructure.RINGS.size) { tier ->
            prepareRing(RingStructure.RINGS[tier])
        }

        return preparedRings.map { uploadTerrainRing(it, useBlockSensitiveBuilder) }.toTypedArray()
    }

    @JvmStatic
    fun useShaderCompatibleTerrainUploadMode() {
        useShaderCompatibleTerrainUpload = true
        currentTerrainUploadedRings = shaderTerrainUploadedRings
    }

    @JvmStatic
    fun useVanillaCompatibleTerrainUploadMode() {
        useShaderCompatibleTerrainUpload = false
        currentTerrainUploadedRings = vanillaTerrainUploadedRings
    }

    private fun uploadTerrainRing(preparedRing: PreparedRing, useBlockSensitiveBuilder: Boolean): UploadedTerrainRing {
        val poseStack = PoseStack()
        val builderPack = if (useBlockSensitiveBuilder) ChunkBufferBuilderPack() else null
        val uploadedBuffers = linkedMapOf<RenderType, VertexBuffer>()

        preparedRing.blocksByRenderType.forEach { (renderType, preparedBlocks) ->
            val consumer = if (useBlockSensitiveBuilder) {
                builderPack!!.builder(renderType)
            } else {
                BufferBuilder(renderType.bufferSize())
            }
            consumer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK)
            preparedBlocks.forEach { preparedBlock ->
                poseStack.withPose {
                    translate(
                        preparedBlock.localX,
                        preparedBlock.localY,
                        preparedBlock.localZ
                    )

                    if (consumer is BlockSensitiveBufferBuilder) {
                        consumer.beginBlock(
                            resolveBlockStateId(preparedBlock.blockState),
                            (-1).toShort(),
                            preparedBlock.blockLocalX and 15,
                            preparedBlock.blockLocalY and 15,
                            preparedBlock.blockLocalZ and 15
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
        val ids = currentBlockStateIds() ?: return (-1).toShort()
        return ids.getOrDefault(blockState, -1).toShort()
    }

    private fun currentBlockStateIds(): Object2IntMap<BlockState>? = WorldRenderingSettings.INSTANCE.blockStateIds

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
        val localX: Double,
        val localY: Double,
        val localZ: Double,
        val blockLocalX: Int,
        val blockLocalY: Int,
        val blockLocalZ: Int,
        val blockState: BlockState,
        val light: Int,
        val quads: List<BakedQuad>
    )

    private data class UploadedTerrainRing(
        val buffersByRenderType: Map<RenderType, VertexBuffer>
    )
}