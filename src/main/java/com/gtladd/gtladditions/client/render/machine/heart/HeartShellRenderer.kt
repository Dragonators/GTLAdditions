package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.client.scene.PartBounds
import com.gtladd.gtladditions.api.client.scene.PartDrawContext
import com.gtladd.gtladditions.api.client.scene.PartFeature
import com.gtladd.gtladditions.api.client.scene.PartKey
import com.gtladd.gtladditions.api.client.scene.PartOutcome
import com.gtladd.gtladditions.api.client.scene.PartPass
import com.gtladd.gtladditions.api.client.scene.PartRenderer
import com.gtladd.gtladditions.api.client.scene.SceneFrame
import com.gtladd.gtladditions.api.client.scene.ScenePart
import com.gtladd.gtladditions.api.client.scene.SceneRendering
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.StructureBlockLighting
import com.gtladd.gtladditions.client.render.StructureRenderBuffers
import com.gtladd.gtladditions.client.render.StructureRenderBuffers.GroupedTerrainLayer
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.common.machine.multiblock.controller.HeartOfTheCosmos
import com.gtladd.gtladditions.common.machine.multiblock.structure.layout.HeartBlockLayout
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraft.util.RandomSource
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.level.BlockAndTintGetter
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.client.model.data.ModelData
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.ForgeRegistries
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL12C
import org.lwjgl.opengl.GL13C
import org.lwjgl.opengl.GL30C
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.util.WeakHashMap
import java.util.concurrent.CompletableFuture
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Baked models are drawn as terrain so shaderpacks treat them as terrain. */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, value = [Dist.CLIENT])
object HeartShellRenderer : PartRenderer<HeartShellRenderer.Overlay> {
    const val SHELLS = 4

    /** Reaches the outermost prefab corner, 115.69 from the centre. */
    const val RADIUS = 117.0

    private val provider = SceneRendering.register(GTLAdditions.id("heart_shells"))
    private val entries = Long2ObjectLinkedOpenHashMap<Entry>()
    private val ZERO_ANGLES = DoubleArray(SHELLS)
    private val motions = WeakHashMap<HeartOfTheCosmos, HeartMotion>()
    private val lightingState = StructureBlockLighting.CacheState()
    private val shades = FloatArray(GROUPS)
    private val shadeNormal = Vector3f()
    private var shaderCompatible = false

    private var data: HeartShellData.Data? = null
    private var dataFailed = false
    private var bakeFailed = false
    private var baking: CompletableFuture<List<PreparedShell>>? = null
    private var prepared: List<PreparedShell>? = null
    private val terrain = arrayOfNulls<Terrain>(SHELLS)
    private val overlays = arrayOfNulls<HeartMesh>(SHELLS)
    private var uploaded = 0
    private var plateTexture = -1

    fun motion(machine: HeartOfTheCosmos): HeartMotion = motions.getOrPut(machine, ::HeartMotion)

    fun shellData(): HeartShellData.Data? {
        data?.let { return it }
        if (dataFailed) return null
        return try {
            HeartShellData.read(Minecraft.getInstance().resourceManager).also { data = it }
        } catch (error: Exception) {
            GTLAdditions.LOGGER.error("Unable to read the Heart shells", error)
            dataFailed = true
            null
        }
    }

    fun submit(frame: SceneFrame, blockEntity: BlockEntity, machine: HeartOfTheCosmos, motion: HeartMotion) {
        val center = HeartBlockLayout.center(machine.pos, machine.frontFacing)
        val shells = HeartBlockLayout.shells(machine.structureTier)
        val angles = motion.angles.copyOf()
        entries.put(blockEntity.blockPos.asLong(), Entry(blockEntity, machine, angles, shells))
        if (!ready()) return
        val x = center.x + 0.5
        val y = center.y + 0.5
        val z = center.z + 0.5
        frame.submit(
            ScenePart.builder(
                PartKey.block(provider, blockEntity.blockPos, 0),
                this,
                Overlay(machine, angles, motion.time, motion.ease, shells)
            )
                .pass(PartPass.OPAQUE)
                .bounds(PartBounds.sphere(x, y, z, RADIUS))
                .features(PartFeature.OWN_HAND_MASK)
                .build()
        )
    }

    fun submitStatic(blockEntity: BlockEntity, machine: HeartOfTheCosmos) {
        entries.put(
            blockEntity.blockPos.asLong(),
            Entry(blockEntity, machine, ZERO_ANGLES, HeartBlockLayout.shells(machine.structureTier))
        )
    }

    fun onResourceReload() {
        releaseBuffers()
        data = null
        dataFailed = false
        bakeFailed = false
    }

    fun releaseBuffers() {
        entries.clear()
        releaseGeometry()
    }

    private fun releaseGeometry() {
        baking?.thenAccept { shells ->
            RenderSystem.recordRenderCall { shells.forEach(PreparedShell::free) }
        }

        baking = null
        prepared?.forEach(PreparedShell::free)
        prepared = null

        for (i in 0 until SHELLS) {
            terrain[i]?.close()
            terrain[i] = null
            overlays[i]?.close()
            overlays[i] = null
        }

        uploaded = 0
        if (plateTexture >= 0) GlStateManager._deleteTexture(plateTexture)
        plateTexture = -1
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

    private fun ready(): Boolean = uploaded == SHELLS

    private fun advanceBuild() {
        if (ready() || bakeFailed) return
        val pending = prepared
        if (pending == null) {
            val job = baking
            if (job == null) {
                val shells = shellData() ?: return
                val level = Minecraft.getInstance().level ?: return
                baking = CompletableFuture.supplyAsync({ bake(shells, level) }, Util.backgroundExecutor())
                return
            }

            if (!job.isDone) return
            baking = null
            prepared = try {
                job.join()
            } catch (error: Exception) {
                GTLAdditions.LOGGER.error("Unable to bake the Heart shells", error)
                bakeFailed = true
                return
            }

            return
        }

        val index = uploaded
        val shell = pending[index]
        terrain[index] = uploadTerrain(shell)

        overlays[index] = shell.overlay?.let {
            HeartMesh.quads(DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL, it, shell.overlayQuads)
        }

        shell.free()
        uploaded++
        if (uploaded == SHELLS) prepared = null
    }

    internal interface Terrain : AutoCloseable {
        fun draw(pose: PoseStack.Pose, shades: FloatArray)
    }

    private class NativeTerrain(private val layer: GroupedTerrainLayer) : Terrain {
        override fun draw(pose: PoseStack.Pose, shades: FloatArray) = layer.draw(pose, shades, batchShader = true)
        override fun close() = layer.close()
    }

    private fun uploadTerrain(shell: PreparedShell): Terrain {
        val pose = PoseStack()

        fun write(batch: StructureRenderBuffers.TerrainBatch, builder: BufferBuilder, placed: List<PlacedCell>): Int {
            var vertices = 0

            for (item in placed) {
                val cell = item.cell
                val pos = cell.position
                pose.withPose {
                    last().pose().mul(item.pose)
                    last().normal().mul(item.rotation)
                    translate(pos.x - 0.5, pos.y - 0.5, pos.z - 0.5)
                    translate(cell.offset.x, cell.offset.y, cell.offset.z)
                    batch.withBlock(builder, cell.state, pos.x, pos.y, pos.z) {
                        for (quad in item.quads) quad.write(builder, last())
                    }
                }
                vertices += item.quads.size * 4
            }

            return vertices
        }

        // Only opaque/cutout groups may move across each other. Keep translucent and custom draws in native order.
        val flat = shaderCompatible &&
            WorldRenderingSettings.INSTANCE.shouldDisableDirectionalShading() &&
            shell.groups.all { group ->
                group.keys.all { it === RenderType.solid() || it === RenderType.cutoutMipped() || it === RenderType.cutout() }
            }
        val palette = !shaderCompatible && shell.groups.all { group -> group.keys.all(HeartShellTerrain::hasShader) }

        if (flat || palette) {
            return StructureRenderBuffers.TerrainBatch(shaderCompatible).use { batch ->
                val counts = if (palette) HashMap<BufferBuilder, IntArray>() else null

                for ((group, types) in shell.groups.withIndex()) {
                    for ((type, placed) in types) {
                        val builder = batch.builder(type)
                        val vertices = write(batch, builder, placed)
                        if (counts != null) counts.getOrPut(builder) { IntArray(GROUPS) }[group] = vertices
                    }
                }

                if (counts == null) {
                    NativeTerrain(GroupedTerrainLayer(listOf(batch.uploadLayer())))
                } else {
                    HeartShellTerrain.Layer(batch.uploadLayer { HeartShellTerrain.upload(it, counts.getValue(it)) })
                }
            }
        }

        val layer = StructureRenderBuffers.groupedTerrainLayers(1, GROUPS, shaderCompatible) { _, group, batch ->
            for ((type, placed) in shell.groups[group]) write(batch, batch.builder(type), placed)
        }.single()

        return NativeTerrain(layer)
    }

    /** A master cube's baked quads by render type, then by face (null: unshaded). */
    private class BakedCell(
        val position: BlockPos,
        val state: BlockState,
        val offset: Vec3,
        val flags: Int,
        val layers: List<Pair<RenderType, Map<Direction?, List<StructureBlockLighting.LitQuad>>>>
    )

    private class PlacedCell(
        val cell: BakedCell,
        val quads: List<StructureBlockLighting.LitQuad>,
        val pose: Matrix4f,
        val rotation: Matrix3f
    )

    private class PreparedShell(
        val groups: List<Map<RenderType, List<PlacedCell>>>,
        var overlay: ByteBuffer?,
        val overlayQuads: Int
    ) {
        fun free() {
            overlay?.let(MemoryUtil::memFree)
            overlay = null
        }
    }

    /** Effect bits by pattern symbol: collector, light bar, lit opening edge, top-step line. */
    private fun flags(symbol: Char): Int = when (symbol) {
        'W', 'w' -> 1
        'L' -> 2
        'S' -> 4
        'G' -> 8
        else -> 0
    }

    private fun bake(shellData: HeartShellData.Data, level: BlockAndTintGetter): List<PreparedShell> {
        val states = HashMap<Char, BlockState>()

        for ((symbol, id) in HeartBlockLayout.BLOCK_IDS) {
            states[symbol] = checkNotNull(ForgeRegistries.BLOCKS.getValue(ResourceLocation(id))) {
                "Missing Heart block: $id"
            }.defaultBlockState()
        }

        val baker = StructureBlockLighting.Baker()
        val random = RandomSource.create(42L)
        val result = ArrayList<PreparedShell>(SHELLS)
        try {
            for (shell in shellData.shells) {
                val groups = List(GROUPS) { linkedMapOf<RenderType, MutableList<PlacedCell>>() }
                val overlay = ArrayList<OverlayQuad>()
                // Plates with the same rim sectors and node role share one master, baked in its own grid (AO, connected textures).
                val variants = shell.plates.indices.groupBy { shell.plates[it].rimMask or if (shell.plates[it].node) NODE else 0 }

                for (members in variants.values) {
                    val cells = bakeMaster(shell, shell.plates[members[0]], states, baker, random, level)

                    for (p in members) {
                        val plate = shell.plates[p]
                        val pose = plate.pose(shell.levels)
                        val rotation = Matrix3f(pose)
                        val normals = Array(6) { d -> rotation.transform(Vector3f(Direction.from3DDataValue(d).step())) }
                        val bins = IntArray(6) { 1 + bin(normals[it]) }
                        val centre = Vector3f()

                        for (cell in cells) {
                            val pos = cell.position
                            pose.transformPosition(centre.set(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat()))

                            for ((type, faces) in cell.layers) {
                                for ((face, quads) in faces) {
                                    val group = if (face == null) 0 else bins[face.get3DDataValue()]
                                    groups[group].getOrPut(type, ::mutableListOf).add(PlacedCell(cell, quads, pose, rotation))

                                    for (quad in quads) {
                                        val normal = normals[quad.quad.direction.get3DDataValue()]
                                        // Collectors, light bars and edges always; other faces only when they look toward the hole.
                                        if (cell.flags == 0 && -normal.dot(centre) / centre.length() <= -0.25f) continue
                                        overlay.add(OverlayQuad(quad.quad.vertices, pos, pose, normal, cell.flags, p, plate.angle))
                                    }
                                }
                            }
                        }
                    }
                }

                result.add(PreparedShell(groups, writeOverlay(overlay), overlay.size))
            }
            return result
        } catch (error: Throwable) {
            result.forEach(PreparedShell::free)
            throw error
        }
    }

    private fun bakeMaster(
        shell: HeartShellData.Shell,
        plate: HeartShellData.Plate,
        states: Map<Char, BlockState>,
        baker: StructureBlockLighting.Baker,
        random: RandomSource,
        level: BlockAndTintGetter
    ): List<BakedCell> {
        val minecraft = Minecraft.getInstance()
        val symbols = HashMap<BlockPos, Char>(shell.cubes.size)
        for (cube in shell.cubes) symbols[BlockPos(cube.x, cube.y, -cube.depth)] = cube.symbol(plate)
        val blocks = symbols.mapValues { (_, symbol) -> states.getValue(symbol) }
        val view = StructureBlockLighting.LocalView(blocks, level)
        val cells = ArrayList<BakedCell>(blocks.size)

        for ((pos, state) in blocks) {
            val symbol = symbols.getValue(pos)
            val model = minecraft.blockRenderer.getBlockModel(state)
            val modelData = model.getModelData(view, pos, state, ModelData.EMPTY)
            random.setSeed(42L)
            val layers = model.getRenderTypes(state, random, modelData).mapNotNull { type ->
                val baked = baker.bakeModel(model, view, pos, state, type, modelData, true)
                // Only the north face of a recessed light bar opens onto the back of the plate.
                val lit = if (symbol == 'L') baked.filter { it.quad.direction == Direction.NORTH } else baked
                if (lit.isEmpty()) null else type to lit.groupBy { if (it.quad.isShade) it.quad.direction else null }
            }
            if (layers.isNotEmpty()) cells.add(BakedCell(pos, state, state.getOffset(view, pos), flags(symbol), layers))
        }

        return cells
    }

    private class OverlayQuad(
        val vertices: IntArray,
        val cell: BlockPos,
        val pose: Matrix4f,
        val normal: Vector3f,
        val flags: Int,
        val plate: Int,
        val angle: Float
    )

    /** Colour packs (flags, plate, cap-axis angle / pi, 255); positions are relative to the centre. */
    private fun writeOverlay(quads: List<OverlayQuad>): ByteBuffer? {
        if (quads.isEmpty()) return null
        val stride = DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL.vertexSize
        val buffer = MemoryUtil.memAlloc(quads.size * 4 * stride)
        val p = Vector3f()

        for (q in quads) {
            val v = q.vertices
            val angle = (q.angle / PI * 255).toInt().coerceIn(0, 255)
            val n = q.normal

            for (k in 0 until 4) {
                val o = k * 8
                p.set(
                    q.cell.x - 0.5f + Float.fromBits(v[o]),
                    q.cell.y - 0.5f + Float.fromBits(v[o + 1]),
                    q.cell.z - 0.5f + Float.fromBits(v[o + 2])
                )
                q.pose.transformPosition(p).fma(LIFT, n)
                buffer.putFloat(p.x).putFloat(p.y).putFloat(p.z)
                buffer.putFloat(Float.fromBits(v[o + 4]))
                buffer.putFloat(Float.fromBits(v[o + 5]))
                buffer.put(q.flags.toByte()).put(q.plate.toByte()).put(angle.toByte()).put(255.toByte())
                buffer.put((n.x * 127f).roundToInt().toByte())
                buffer.put((n.y * 127f).roundToInt().toByte())
                buffer.put((n.z * 127f).roundToInt().toByte())
                buffer.put(0)
            }
        }

        return buffer.flip()
    }

    private const val LIFT = 0.002f

    @SubscribeEvent
    @JvmStatic
    fun onRenderLevelStage(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES || entries.isEmpty()) return
        try {
            if (lightingState.changed()) releaseGeometry()
            advanceBuild()
            if (!ready()) return
            val camera = event.camera.position

            for (entry in entries.values) {
                val machine = entry.machine
                if (entry.blockEntity.isRemoved || !machine.isFormed || entry.blockEntity.level !== Minecraft.getInstance().level) continue
                val center = HeartBlockLayout.center(machine.pos, machine.frontFacing)
                if (!event.frustum.isVisible(AABB(center).inflate(RADIUS))) continue
                val level = checkNotNull(entry.blockEntity.level)

                for (s in 0 until entry.shells) {
                    val model = shellTransform(machine, entry.angles[s], s, camera.x, camera.y, camera.z)
                    val normal = Matrix3f(model).normal()
                    shades[0] = level.getShade(Direction.UP, false)

                    for (b in 0 until NORMALS) {
                        normal.transform(BINS[b], shadeNormal)
                        shades[1 + b] = StructureBlockLighting.shade(level, shadeNormal.x, shadeNormal.y, shadeNormal.z)
                    }

                    event.poseStack.withPose {
                        last().pose().mul(model)
                        last().normal().mul(normal)
                        terrain[s]!!.draw(last(), shades)
                    }
                }
            }
        } finally {
            entries.clear()
        }
    }

    fun shellTransform(
        machine: HeartOfTheCosmos,
        angle: Double,
        index: Int,
        cameraX: Double,
        cameraY: Double,
        cameraZ: Double
    ): Matrix4f {
        val pos = machine.pos
        val offset = HeartBlockLayout.centerOffset
        val axis = shellData()!!.shells[index].axis
        return Matrix4f()
            .translate((pos.x - cameraX).toFloat(), (pos.y - cameraY).toFloat(), (pos.z - cameraZ).toFloat())
            .translate(0.5f, 0.5f, 0.5f)
            .rotateY(Math.toRadians(yaw(machine.frontFacing).toDouble()).toFloat())
            .translate(offset.x.toFloat(), offset.y.toFloat(), offset.z.toFloat())
            .rotate(Quaternionf().rotateAxis(RenderUtils.rotationRadians(angle), axis.x, axis.y, axis.z))
    }

    fun yaw(facing: Direction): Float = when (facing) {
        Direction.WEST -> -90f
        Direction.NORTH -> 180f
        Direction.EAST -> 90f
        else -> 0f
    }

    /** Circuit light timing, in seconds. */
    private const val GAP = 16.0
    private const val LENGTH = 4.5
    private const val CROSS = 18.0
    private const val CELL = 0.5f

    private const val CIRCUIT_SPEED = 6.0

    /** [shells] counts the active shells from the innermost. */
    class Overlay(val machine: HeartOfTheCosmos, val angles: DoubleArray, val time: Double, val ease: Double, val shells: Int)

    override fun draw(context: PartDrawContext, parts: List<ScenePart<Overlay>>) {
        val shader = HeartFxShaders.shellFx ?: return parts.forEach { context.report(it, PartOutcome.RESOURCE_UNAVAILABLE) }
        val shells = shellData() ?: return parts.forEach { context.report(it, PartOutcome.RESOURCE_UNAVAILABLE) }
        if (!ready()) return parts.forEach { context.report(it, PartOutcome.PENDING) }
        val frame = context.frame
        val kappa = ln(2.0) / sin(PI / 2 * minOf(LENGTH / GAP, 1.0)).pow(2.0)
        RenderUtils.withSavedRenderState {
            premultiplied()
            RenderSystem.depthMask(false)
            RenderSystem.enableDepthTest()
            RenderSystem.depthFunc(GL11C.GL_LEQUAL)
            RenderSystem.enableCull()
            RenderSystem.enablePolygonOffset()
            // A slope bias pulls nearly edge-on decals through the walls of the light-bar recesses.
            RenderSystem.polygonOffset(0f, -2f)
            shader.setSampler("Sampler0", Minecraft.getInstance().textureManager.getTexture(InventoryMenu.BLOCK_ATLAS).id)
            shader.setSampler("Sampler1", plateTexture(shells))
            HeartHandDepthMask.bind(shader, context.handMaskTexture)
            try {
                for (part in parts) {
                    val overlay = part.payload
                    val time = overlay.time
                    shader.getUniform("SweepPhase")?.set(((time / 5.0) % 1.0).toFloat())
                    shader.getUniform("WarmPhase")?.set(RenderUtils.rotationRadians(time * 2.3))
                    shader.getUniform("CircuitTime")?.set((time * CIRCUIT_SPEED % GAP).toFloat())
                    shader.getUniform("Power")?.set(overlay.ease.toFloat())
                    shader.getUniform("Gap")?.set(GAP.toFloat())
                    shader.getUniform("Kappa")?.set(kappa.toFloat())
                    shader.getUniform("Reach")?.set((CROSS / PI).toFloat())
                    shader.getUniform("Cell")?.set(CELL)

                    for (s in 0 until overlay.shells) {
                        val mesh = overlays[s] ?: continue
                        val shell = shells.shells[s]
                        val model = shellTransform(overlay.machine, overlay.angles[s], s, frame.cameraX, frame.cameraY, frame.cameraZ)
                        shader.getUniform("ModelViewMat")?.set(Matrix4f(context.poseStack.last().pose()).mul(model))
                        shader.getUniform("ProjMat")?.set(RenderSystem.getProjectionMatrix())
                        shader.getUniform("Sweep")?.set(shell.sweep)
                        shader.getUniform("MaxAngle")?.set(shell.maxAngle)
                        shader.getUniform("Layer")?.set(shell.id.toFloat())
                        shader.apply()
                        // ShaderInstance.apply() skips its blend mode when it was the last one applied.
                        premultiplied()
                        mesh.drawRange()
                        shader.clear()
                    }

                    shader.getUniform("SceneHandMaskEnabled")?.set(0)
                }
            } finally {
                shader.getUniform("SceneHandMaskEnabled")?.set(0)
                RenderSystem.polygonOffset(0f, 0f)
                RenderSystem.disablePolygonOffset()
                RenderSystem.depthMask(true)
                RenderSystem.defaultBlendFunc()
                RenderSystem.disableBlend()
                RenderSystem.activeTexture(GL13C.GL_TEXTURE0)
            }
        }
    }

    private fun premultiplied() {
        RenderSystem.enableBlend()
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ZERO,
            GlStateManager.DestFactor.ONE
        )
    }

    /**
     * Faces are shaded per frame by direction. Prefab plates are turned freely, so faces are grouped by the nearest of
     * [NORMALS] fixed shell-frame directions; group 0 holds unshaded faces.
     */
    private const val NORMALS = 64
    internal const val GROUPS = 1 + NORMALS

    /** Variant key bit for structural node plates, above the six rim sector bits. */
    private const val NODE = 1 shl 6

    /** A Fibonacci sphere of [NORMALS] directions. */
    private val BINS = Array(NORMALS) { i ->
        val y = 1f - (2f * i + 1f) / NORMALS
        val r = sqrt(1f - y * y)
        val a = i * PI.toFloat() * (3f - sqrt(5f))
        Vector3f(r * cos(a), y, r * sin(a))
    }

    private fun bin(normal: Vector3f): Int {
        var best = 0
        for (b in 1 until NORMALS) if (BINS[b].dot(normal) > BINS[best].dot(normal)) best = b
        return best
    }

    /**
     * Sampler1 of the overlay shader, RGBA32F, [TEXTURE_WIDTH] texels wide:
     * - rows shell * 256 + plate: the plate's tangent, bitangent, normal and centre
     * - rows 1024..1055: the long-run circuit tile, red = links of node (x, row)
     * - rows 1056..1059: each shell's collector rows y = -16..15, (x0, x1)
     */
    private const val MAX_PLATES = 256
    private const val TEXTURE_WIDTH = 32
    private const val TEXTURE_HEIGHT = SHELLS * MAX_PLATES + HeartShellData.TILE + SHELLS

    private fun plateTexture(shells: HeartShellData.Data): Int {
        if (plateTexture >= 0) return plateTexture
        val previousActive = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE)
        GlStateManager._activeTexture(GL13C.GL_TEXTURE0)
        val previousTexture = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D)
        val texture = GL11C.glGenTextures()
        val data = MemoryUtil.memCallocFloat(TEXTURE_WIDTH * TEXTURE_HEIGHT * 4)
        try {
            fillPlateTexture(shells, data)
            GlStateManager._bindTexture(texture)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_NEAREST)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_NEAREST)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL12C.GL_CLAMP_TO_EDGE)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL12C.GL_TEXTURE_BASE_LEVEL, 0)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL12C.GL_TEXTURE_MAX_LEVEL, 0)
            RenderUtils.withTightlyPackedPixels {
                GL11C.glTexImage2D(
                    GL11C.GL_TEXTURE_2D,
                    0,
                    GL30C.GL_RGBA32F,
                    TEXTURE_WIDTH,
                    TEXTURE_HEIGHT,
                    0,
                    GL11C.GL_RGBA,
                    GL11C.GL_FLOAT,
                    data
                )
            }
        } catch (exception: Throwable) {
            GlStateManager._deleteTexture(texture)
            throw exception
        } finally {
            MemoryUtil.memFree(data)
            GlStateManager._bindTexture(previousTexture)
            GlStateManager._activeTexture(previousActive)
        }

        plateTexture = texture
        return texture
    }

    private fun fillPlateTexture(shells: HeartShellData.Data, out: FloatBuffer) {
        fun put(row: Int, texel: Int, x: Float, y: Float, z: Float) {
            val o = (row * TEXTURE_WIDTH + texel) * 4
            out.put(o, x).put(o + 1, y).put(o + 2, z)
        }

        shells.shells.forEachIndexed { s, shell ->
            shell.plates.forEachIndexed { p, plate ->
                val row = s * MAX_PLATES + p
                for ((texel, v) in listOf(plate.tangent, plate.bitangent, plate.normal, plate.center).withIndex()) {
                    put(row, texel, v.x, v.y, v.z)
                }
            }

            val row = SHELLS * MAX_PLATES + HeartShellData.TILE + s
            for (i in 0 until HeartShellData.ROWS) put(row, i, shell.rows[i * 2].toFloat(), shell.rows[i * 2 + 1].toFloat(), 0f)
        }

        for (y in 0 until HeartShellData.TILE) {
            for (x in 0 until HeartShellData.TILE) {
                put(SHELLS * MAX_PLATES + y, x, (shells.tile[y * HeartShellData.TILE + x].toInt() and 0xff).toFloat(), 0f, 0f)
            }
        }
    }

    private class Entry(val blockEntity: BlockEntity, val machine: HeartOfTheCosmos, val angles: DoubleArray, val shells: Int)
}

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, value = [Dist.CLIENT], bus = Mod.EventBusSubscriber.Bus.MOD)
object HeartShellReloadListener {
    @SubscribeEvent
    @JvmStatic
    fun onRegisterReloadListeners(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(
            ResourceManagerReloadListener {
                val release = {
                    HeartShellRenderer.onResourceReload()
                    HeartFleetAssets.release()
                    HeartFleetRenderer.release()
                }

                if (RenderSystem.isOnRenderThread()) {
                    release()
                } else {
                    RenderSystem.recordRenderCall(release)
                }
            }
        )
    }
}
