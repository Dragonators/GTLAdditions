package com.gtladd.gtladditions.client.render.frame

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.GTLAddRenderTypes
import com.gtladd.gtladditions.client.render.StructureBlockLighting
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil
import net.irisshaders.iris.api.v0.IrisApi
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.chunk.ChunkStatus
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.client.model.data.ModelData
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.level.LevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.common.Mod
import java.util.Collections
import java.util.IdentityHashMap
import java.util.PriorityQueue
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.floor

@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, value = [Dist.CLIENT])
object FrameOverlayRenderer {
    @Volatile
    var enabled = false
        private set

    @Volatile
    private var resetRequested = true

    private val dirty = ConcurrentHashMap.newKeySet<Long>()
    private val unloaded = ConcurrentHashMap.newKeySet<Long>()
    private val meshes = Long2ObjectOpenHashMap<Mesh>()
    private val queued = LongOpenHashSet()
    private val pending = PriorityQueue<Pending>(compareBy { it.distance })
    private var level: ClientLevel? = null
    private var job: Job? = null
    private var workBuffer: BufferBuilder? = null
    private var baker: StructureBlockLighting.Baker? = null
    private var lastQueueCamera = Vec3.ZERO
    private val renderType by lazy(GTLAddRenderTypes::createFrameOverlayLayer)
    private val embeddium = ModList.get().isLoaded("embeddium")

    @JvmStatic
    fun requestReset() {
        resetRequested = true
    }

    @JvmStatic
    fun dirtySection(x: Int, y: Int, z: Int) {
        if (enabled && !embeddium) dirty.add(SectionPos.asLong(x, y, z))
    }

    @JvmStatic
    fun dirtyArea(x0: Int, y0: Int, z0: Int, x1: Int, y1: Int, z1: Int) {
        if (!enabled || embeddium) return
        for (x in (x0 shr 4)..(x1 shr 4)) for (y in (y0 shr 4)..(y1 shr 4)) for (z in (z0 shr 4)..(z1 shr 4)) dirtySection(x, y, z)
    }

    @JvmStatic
    fun chunkLoaded(chunk: LevelChunk) {
        if (!embeddium && chunk.level === Minecraft.getInstance().level) queueChunk(chunk)
    }

    @JvmStatic
    fun chunkUnloaded(x: Int, z: Int) {
        if (enabled && !embeddium) unloaded.add(ChunkPos.asLong(x, z))
    }

    @SubscribeEvent
    @JvmStatic
    fun onLevelUnload(event: LevelEvent.Unload) {
        if (event.level.isClientSide) requestReset()
    }

    @SubscribeEvent
    @JvmStatic
    fun onRenderTick(event: TickEvent.RenderTickEvent) {
        if (event.phase != TickEvent.Phase.START) return

        val mc = Minecraft.getInstance()
        val current = mc.level
        val native = current != null && !IrisApi.getInstance().isShaderPackInUse
        val changed = native != enabled
        enabled = native

        if (current !== level || changed || resetRequested) {
            resetRequested = false
            clear()
            level = current
            if (enabled && !embeddium) discoverLoaded(mc)
            if (changed && current != null) {
                // Mesh routing changes too; a stale chunk must not retain or lose its decal.
                mc.levelRenderer.allChanged()
                resetRequested = false
            }
        }
    }

    private fun clear() {
        cancelJob()
        meshes.values.forEach { it.buffer.close() }
        meshes.clear()
        pending.clear()
        queued.clear()
        dirty.clear()
        unloaded.clear()
        baker = null
    }

    private fun discoverLoaded(mc: Minecraft) {
        val current = level ?: return
        val center = mc.player?.blockPosition() ?: return

        val radius = mc.options.effectiveRenderDistance + 2
        for (x in (center.x shr 4) - radius..(center.x shr 4) + radius) {
            for (z in (center.z shr 4) - radius..(center.z shr 4) + radius) {
                current.chunkSource.getChunk(x, z, ChunkStatus.FULL, false)?.let(::queueChunk)
            }
        }
    }

    private fun queueChunk(chunk: LevelChunk) {
        if (!enabled || embeddium) return
        for (index in chunk.sections.indices) {
            if (!chunk.sections[index].hasOnlyAir()) dirtySection(chunk.pos.x, chunk.minSection + index, chunk.pos.z)
        }
    }

    private fun processInvalidations(camera: Vec3) {
        val current = level ?: return

        val chunks = unloaded.iterator()
        while (chunks.hasNext()) {
            val key = chunks.next()
            chunks.remove()
            val x = ChunkPos.getX(key)
            val z = ChunkPos.getZ(key)

            for (y in current.minSection until current.maxSection) {
                val section = SectionPos.asLong(x, y, z)
                meshes.remove(section)?.buffer?.close()
                queued.remove(section)
                if (job?.key == section) cancelJob()
            }
        }

        // View-center changes can make a chunk unavailable before a drop callback arrives.
        val resident = meshes.values.iterator()
        while (resident.hasNext()) {
            val mesh = resident.next()
            if (!current.chunkSource.hasChunk(mesh.x, mesh.z)) {
                mesh.buffer.close()
                resident.remove()
            }
        }

        job?.let {
            if (!current.chunkSource.hasChunk(SectionPos.x(it.key), SectionPos.z(it.key))) cancelJob()
        }

        val changes = dirty.iterator()
        while (changes.hasNext()) {
            val key = changes.next()
            changes.remove()
            meshes.remove(key)?.buffer?.close()
            if (job?.key == key) cancelJob()
            if (queued.add(key)) pending.add(Pending(key, distance(key, camera)))
        }

        if (pending.isNotEmpty() && camera.distanceToSqr(lastQueueCamera) >= 256.0) {
            val keys = queued.toLongArray()
            pending.clear()
            keys.forEach { pending.add(Pending(it, distance(it, camera))) }
            lastQueueCamera = camera
        }
    }

    private fun distance(key: Long, camera: Vec3): Double {
        val dx = SectionPos.x(key) * 16 + 8 - camera.x
        val dy = SectionPos.y(key) * 16 + 8 - camera.y
        val dz = SectionPos.z(key) * 16 + 8 - camera.z
        return dx * dx + dy * dy + dz * dz
    }

    private fun cancelJob() {
        if (job != null) {
            workBuffer?.takeIf { it.building() }?.end()?.release()
            job = null
        }
    }

    private fun buildSome() {
        val current = level ?: return
        val deadline = System.nanoTime() + 2_000_000L

        while (System.nanoTime() < deadline) {
            if (job == null) {
                val next = pending.poll() ?: return
                if (!queued.remove(next.key)) continue
                val y = SectionPos.y(next.key)
                if (y !in current.minSection until current.maxSection) continue

                val chunk = current.chunkSource.getChunk(SectionPos.x(next.key), SectionPos.z(next.key), ChunkStatus.FULL, false)
                    ?: continue
                val section = chunk.getSection(y - current.minSection)
                if (section.hasOnlyAir() || !section.maybeHas(FrameOverlayModels::candidate)) continue

                val buffer = workBuffer ?: BufferBuilder(32768).also { workBuffer = it }
                buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK)
                job = Job(next.key, section, buffer, baker ?: StructureBlockLighting.Baker().also { baker = it })
            }

            val active = checkNotNull(job)
            try {
                active.step(current)
                if (active.cursor == 4096) {
                    if (active.quads > 0) {
                        meshes.put(
                            active.key,
                            Mesh(active.key, RenderUtils.uploadStaticBuffer(active.buffer), active.sprites.toTypedArray())
                        )
                    } else {
                        active.buffer.end().release()
                    }
                    job = null
                }
            } catch (error: RuntimeException) {
                cancelJob()
                GTLAdditions.LOGGER.error("Could not rebuild GT frame overlay section {}", active.key, error)
            }
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun onRenderLevelStage(event: RenderLevelStageEvent) {
        if (!enabled || event.stage != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES || level !== Minecraft.getInstance().level) return
        if (embeddium) {
            FrameOverlayTerrainPass.draw(event)
            return
        }

        val camera = event.camera.position
        processInvalidations(camera)
        buildSome()
        if (meshes.isEmpty()) return

        val radius = Minecraft.getInstance().options.effectiveRenderDistance
        val cx = floor(camera.x).toInt() shr 4
        val cz = floor(camera.z).toInt() shr 4
        val color = RenderSystem.getShaderColor().copyOf()
        RenderUtils.withSavedRenderState {
            try {
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                RenderUtils.withRenderType(renderType) {
                    val shader = RenderSystem.getShader() ?: return@withRenderType
                    shader.getUniform("ChunkOffset")?.set(0f, 0f, 0f)
                    RenderUtils.withShaderBatch(shader, true) {
                        for (mesh in meshes.values) {
                            if (abs(mesh.x - cx) > radius || abs(mesh.z - cz) > radius || !event.frustum.isVisible(mesh.bounds)) continue

                            // These sprites no longer belong to Embeddium's terrain meshes.
                            if (embeddium) mesh.sprites.forEach(SpriteUtil::markSpriteActive)
                            event.poseStack.withPose {
                                translate(mesh.x * 16.0 - camera.x, mesh.y * 16.0 - camera.y, mesh.z * 16.0 - camera.z)
                                RenderUtils.withBoundBuffer(mesh.buffer) {
                                    mesh.buffer.drawWithShader(last().pose(), event.projectionMatrix, shader)
                                }
                            }
                        }
                    }
                }
            } finally {
                RenderSystem.setShaderColor(color[0], color[1], color[2], color[3])
            }
        }
    }

    private data class Pending(val key: Long, val distance: Double)

    private class Mesh(key: Long, val buffer: VertexBuffer, val sprites: Array<TextureAtlasSprite>) {
        val x = SectionPos.x(key)
        val y = SectionPos.y(key)
        val z = SectionPos.z(key)
        val bounds = AABB(x * 16.0, y * 16.0, z * 16.0, x * 16.0 + 16, y * 16.0 + 16, z * 16.0 + 16)
    }

    private class Job(val key: Long, val section: LevelChunkSection, val buffer: BufferBuilder, val baker: StructureBlockLighting.Baker) {
        var cursor = 0
        var quads = 0
        val sprites: MutableSet<TextureAtlasSprite> = Collections.newSetFromMap(IdentityHashMap())

        private val origin = BlockPos(SectionPos.x(key) * 16, SectionPos.y(key) * 16, SectionPos.z(key) * 16)
        private val position = BlockPos.MutableBlockPos()
        private val pose = PoseStack()

        fun step(level: ClientLevel) {
            val index = cursor++
            val x = index and 15
            val z = index shr 4 and 15
            val y = index shr 8
            val state = section.getBlockState(x, y, z)
            if (!FrameOverlayModels.candidate(state)) return

            val model = Minecraft.getInstance().blockRenderer.getBlockModel(state)
            if (!FrameOverlayModels.eligible(state, model)) return

            position.set(origin.x + x, origin.y + y, origin.z + z)
            val data = model.getModelData(level, position, state, ModelData.EMPTY)
            val lit = baker.bakeModel(model, level, position, state, RenderType.translucent(), data, true)
            val offset = state.getOffset(level, position)
            pose.withPose {
                translate(x + offset.x, y + offset.y, z + offset.z)
                lit.forEach {
                    it.write(buffer, last())
                    sprites.add(it.quad.sprite)
                }
            }

            quads += lit.size
        }
    }
}