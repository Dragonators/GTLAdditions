package com.gtladd.gtladditions.client.render.machine.antichrist

import com.gtladd.gtladditions.client.render.StructureBlockLighting
import com.gtladd.gtladditions.client.render.StructureBlockLighting.FaceGroup
import com.gtladd.gtladditions.client.render.StructureRenderBuffers
import com.gtladd.gtladditions.client.render.StructureRenderBuffers.GroupedTerrainLayer
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks
import com.gtladd.gtladditions.common.machine.multiblock.structure.layout.AntichristBlockLayout
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.model.data.ModelData
import org.gtlcore.gtlcore.client.ClientUtil
import org.joml.Matrix4f
import kotlin.collections.iterator

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

    private var uploadedRings: List<GroupedTerrainLayer>? = null
    private var uploadedTranslucentRings: List<Map<Int, RingTransparency.Buffer>> = emptyList()
    private var useShaderCompatibleTerrainUpload = false
    private val ringCells by lazy { AntichristBlockLayout.RINGS.map(RingStructureGeometry::collect) }
    private val lightingState = StructureBlockLighting.CacheState()
    private val shades = FloatArray(FaceGroup.entries.size)

    /** Atlas UVs and baked models are no longer valid after a resource reload. */
    fun releaseBuffers() {
        RingTransparency.release()
        uploadedRings?.forEach(GroupedTerrainLayer::close)
        uploadedRings = null
        uploadedTranslucentRings.forEach { groups -> groups.values.forEach(RingTransparency.Buffer::close) }
        uploadedTranslucentRings = emptyList()
    }

    fun renderTerrainBatched(profile: AntichristRenderProfile, poseStack: PoseStack) {
        if (lightingState.changed()) releaseBuffers()
        val level = Minecraft.getInstance().level ?: return
        val worldPose = PoseStack()
        val rings = uploadedRings ?: buildTerrainUploadedRings().also { uploadedRings = it }
        rings.forEachIndexed { index, uploadedRing ->
            worldPose.withPose {
                // Reuse the exact ring transform without the camera's view rotation.
                AntichristRingTransforms.apply(profile, index, this)
                val worldNormal = last().normal()
                for (group in FaceGroup.entries) shades[group.ordinal] = group.shade(level, worldNormal)
                poseStack.withPose {
                    AntichristRingTransforms.apply(profile, index, this)
                    uploadedRing.draw(last(), shades)
                }
            }
        }
    }

    fun collectTranslucent(profile: AntichristRenderProfile, poseStack: PoseStack, draws: MutableList<RingTransparency.Instance>) {
        val level = Minecraft.getInstance().level ?: return
        if (uploadedTranslucentRings.isEmpty()) return
        val worldPose = PoseStack()
        val transforms = Array(3) { Matrix4f() }
        val ringShades = Array(3) { FloatArray(FaceGroup.entries.size) }

        repeat(3) { index ->
            worldPose.withPose {
                AntichristRingTransforms.apply(profile, index, this)
                val worldNormal = last().normal()
                poseStack.withPose {
                    AntichristRingTransforms.apply(profile, index, this)
                    transforms[index].set(last().pose())
                    for (group in FaceGroup.entries) ringShades[index][group.ordinal] = group.shade(level, worldNormal)
                }
            }
        }

        draws.add(RingTransparency.Instance(transforms, ringShades))
    }

    private fun prepareRing(
        structure: Array<Array<String>>,
        cells: List<RingStructureGeometry.Cell>,
        materials: MutableMap<Char, PreparedMaterial>,
        baker: StructureBlockLighting.Baker
    ): PreparedRing {
        // Match AntichristBlockLayout's placement of the even-width third ring.
        val axialOffset = if (structure.size == 94) 0.5 else 0.0
        val centerX = structure.size / 2.0 - axialOffset
        val centerY = structure[0].size / 2.0
        val centerZ = structure[0][0].length / 2.0
        val blockCenterX = structure.size / 2
        val blockCenterY = structure[0].size / 2
        val blockCenterZ = structure[0][0].length / 2
        val preparedBlocksByGroup = List(FaceGroup.entries.size) { linkedMapOf<RenderType, MutableList<PreparedBlock>>() }
        val renderTypes = linkedSetOf<RenderType>()
        val states = cells.mapNotNull { cell ->
            val block = BLOCK_MAPPER[cell.letter] ?: return@mapNotNull null
            if (block == Blocks.AIR) return@mapNotNull null
            val material = materials.getOrPut(cell.letter) { prepareMaterial(block) }
            BlockPos(cell.x - blockCenterX, blockCenterY - cell.y, cell.z - blockCenterZ) to material.state
        }.toMap()

        val view = StructureBlockLighting.LocalView(states, checkNotNull(Minecraft.getInstance().level))

        for (cell in cells) {
            val block = BLOCK_MAPPER[cell.letter] ?: continue
            if (block == Blocks.AIR) continue
            val material = materials.getOrPut(cell.letter) { prepareMaterial(block) }
            val visibleFaces = cell.visibleFaces(material.solid)
            if (visibleFaces == 0) continue

            val localX = cell.x - centerX
            val localY = centerY - cell.y - 1
            val localZ = cell.z - centerZ
            val blockLocalX = cell.x - blockCenterX
            val blockLocalY = blockCenterY - cell.y
            val blockLocalZ = cell.z - blockCenterZ

            for ((renderType, modelQuads) in material.quadsByRenderType) {
                val quads = modelQuads.get(visibleFaces)
                if (quads.isEmpty()) continue
                renderTypes.add(renderType)
                val lit = baker.bake(
                    material.model,
                    view,
                    BlockPos(blockLocalX, blockLocalY, blockLocalZ),
                    material.state,
                    renderType,
                    quads
                )

                for ((group, grouped) in lit.groupBy { it.group }) {
                    preparedBlocksByGroup[group.ordinal].getOrPut(renderType, ::mutableListOf).add(
                        PreparedBlock(localX, localY, localZ, blockLocalX, blockLocalY, blockLocalZ, material.state, grouped)
                    )
                }
            }
        }

        return PreparedRing(
            preparedBlocksByGroup.map { blocks ->
                renderTypes.filter { it in blocks }.associateWith(blocks::getValue)
            }
        )
    }

    private fun prepareMaterial(block: Block): PreparedMaterial {
        val state = block.defaultBlockState()
        val model = ClientUtil.blockRenderer().getBlockModel(state)
        val types = model.getRenderTypes(state, RandomSource.create(42L), ModelData.EMPTY)
        return PreparedMaterial(
            state,
            model,
            state.isSolidRender(Minecraft.getInstance().level!!, BlockPos.ZERO),
            types.associateWith { RingModelQuads(model, state, it) }
        )
    }

    private fun buildTerrainUploadedRings(): List<GroupedTerrainLayer> {
        val materials = HashMap<Char, PreparedMaterial>()
        val baker = StructureBlockLighting.Baker()
        val preparedRings = AntichristBlockLayout.RINGS.mapIndexed { tier, structure ->
            prepareRing(structure, ringCells[tier], materials, baker)
        }

        val opaque = StructureRenderBuffers.groupedTerrainLayers(
            preparedRings.size,
            FaceGroup.entries.size,
            useShaderCompatibleTerrainUpload
        ) { index, group, batch ->
            writeTerrainRing(preparedRings[index], batch, group, false)
        }

        val translucent = ArrayList<Map<Int, RingTransparency.Buffer>>()
        try {
            val batch = StructureRenderBuffers.TerrainBatch(useShaderCompatibleTerrainUpload)
            for ((index, ring) in preparedRings.withIndex()) {
                val groups = linkedMapOf<Int, RingTransparency.Buffer>()
                translucent.add(groups)
                for (group in FaceGroup.entries.indices) {
                    writeTerrainRing(ring, batch, group, true)
                    val uploaded = batch.uploadLayer { builder -> RingTransparency.Buffer.upload(builder, index, group) }
                    uploaded[RenderType.translucent()]?.let { groups[group] = it as RingTransparency.Buffer }
                }
            }

            uploadedTranslucentRings = translucent
            RingTransparency.install(translucent.flatMap { it.values })
            return opaque
        } catch (error: Throwable) {
            opaque.forEach(GroupedTerrainLayer::close)
            translucent.forEach { groups -> groups.values.forEach(RingTransparency.Buffer::close) }
            throw error
        }
    }

    @JvmStatic
    fun useShaderCompatibleTerrainUploadMode() {
        // A new pipeline can assign different material IDs even without reloading block models.
        releaseBuffers()
        useShaderCompatibleTerrainUpload = true
    }

    @JvmStatic
    fun useVanillaCompatibleTerrainUploadMode() {
        releaseBuffers()
        useShaderCompatibleTerrainUpload = false
    }

    private fun writeTerrainRing(preparedRing: PreparedRing, batch: StructureRenderBuffers.TerrainBatch, group: Int, translucent: Boolean) {
        val poseStack = PoseStack()

        for ((renderType, preparedBlocks) in preparedRing.blocksByGroup[group]) {
            if ((renderType === RenderType.translucent()) != translucent) continue
            for ((localX, localY, localZ, blockLocalX, blockLocalY, blockLocalZ, blockState, quads) in preparedBlocks) {
                val consumer = batch.builder(renderType)
                poseStack.withPose {
                    translate(
                        localX,
                        localY,
                        localZ
                    )

                    batch.withBlock(consumer, blockState, blockLocalX, blockLocalY, blockLocalZ) {
                        val pose = last()
                        for (quad in quads) quad.write(consumer, pose)
                    }
                }
            }
        }
    }

    private data class PreparedMaterial(
        val state: BlockState,
        val model: BakedModel,
        val solid: Boolean,
        val quadsByRenderType: Map<RenderType, RingModelQuads>
    )

    private data class PreparedRing(
        val blocksByGroup: List<Map<RenderType, List<PreparedBlock>>>
    )

    private data class PreparedBlock(
        val localX: Double,
        val localY: Double,
        val localZ: Double,
        val blockLocalX: Int,
        val blockLocalY: Int,
        val blockLocalZ: Int,
        val blockState: BlockState,
        val quads: List<StructureBlockLighting.LitQuad>
    )
}

@OnlyIn(Dist.CLIENT)
internal class RingModelQuads(private val model: BakedModel, private val state: BlockState, private val type: RenderType) {
    private val random = RandomSource.create(42L)
    private val faces = arrayOfNulls<List<BakedQuad>>(7)
    private val masks = arrayOfNulls<List<BakedQuad>>(64)

    fun get(visibleFaces: Int): List<BakedQuad> = masks[visibleFaces] ?: buildList {
        addAll(face(0))

        for (index in DIRECTIONS.indices) {
            if (visibleFaces and (1 shl index) != 0) addAll(face(index + 1))
        }
    }.also { masks[visibleFaces] = it }

    private fun face(index: Int): List<BakedQuad> = faces[index] ?: run {
        random.setSeed(42L)
        model.getQuads(state, if (index == 0) null else DIRECTIONS[index - 1], random, ModelData.EMPTY, type).toList()
    }.also { faces[index] = it }

    companion object {
        private val DIRECTIONS = arrayOf(Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH)
    }
}