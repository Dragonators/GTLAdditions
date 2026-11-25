package com.gtladd.gtladditions.client.render.machine.elevator

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.StructureRenderBuffers
import com.gtladd.gtladditions.client.render.machine.elevator.SpaceElevatorCableGeometry.Facing
import com.gtladd.gtladditions.client.render.withPose
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexBuffer
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
import net.irisshaders.iris.shadows.ShadowRenderingState
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.Direction
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import kotlin.math.max

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, value = [Dist.CLIENT])
object SpaceElevatorCableRenderer {
    private val cableSprite = GTLAdditions.id("block/space_elevator/cable")
    private val glowType = RenderType.eyes(GTLAdditions.id("textures/effect/space_elevator/cable_glow.png"))
    private val cableLight = LightTexture.pack(0, 15)

    // GTNH's glow colour is (0, 0.65, 1), added where the light front passes.
    private const val GLOW_GREEN = 0.65f

    // The additive glow sits just outside the ribbons so it never depth-fights them.
    private const val GLOW_LIFT = 1.004f

    private val entries = Long2ObjectLinkedOpenHashMap<Entry>()
    private val meshes = HashMap<Int, List<VertexBuffer>>()
    private var shaderCompatible = false
    private val corners = FloatArray(SpaceElevatorCableGeometry.QUAD_FLOATS)
    private val shades = FloatArray(Facing.entries.size)

    fun enqueue(blockEntity: BlockEntity, axisX: Double, bottomY: Double, axisZ: Double, length: Int, ticks: Double) {
        entries.put(blockEntity.blockPos.asLong(), Entry(blockEntity, axisX, bottomY, axisZ, length, ticks))
    }

    fun releaseBuffers() {
        for (mesh in meshes.values) RenderUtils.releaseBuffers(mesh)
        meshes.clear()
        entries.clear()
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
            val level = Minecraft.getInstance().level ?: return
            val visible = entries.values.filter {
                !it.blockEntity.isRemoved && it.blockEntity.level === level && event.frustum.isVisible(it.bounds())
            }
            if (visible.isEmpty()) return
            val camera = event.camera.position
            for (entry in visible) meshes.getOrPut(entry.length) { buildMesh(entry.length) }
            drawCables(event.poseStack, camera, visible, level)
            if (!ShadowRenderingState.areShadowsCurrentlyBeingRendered()) drawGlow(event.poseStack, camera, visible)
        } finally {
            entries.clear()
        }
    }

    private fun drawCables(poseStack: PoseStack, camera: Vec3, visible: List<Entry>, level: ClientLevel) {
        val x = level.getShade(Direction.EAST, true)
        val z = level.getShade(Direction.SOUTH, true)
        // The brightest side keeps the texture's own brightness, as on GTNH's unlit cable; the dark texture needs it.
        val brightest = max(x, z)
        shades[Facing.X.ordinal] = x / brightest
        shades[Facing.Z.ordinal] = z / brightest
        shades[Facing.DIAGONAL.ordinal] = (x + z) * 0.5f / brightest
        val color = RenderSystem.getShaderColor()
        val red = color[0]
        val green = color[1]
        val blue = color[2]
        val alpha = color[3]
        try {
            RenderUtils.withRenderType(RenderType.cutoutMipped()) {
                val shader = RenderSystem.getShader() ?: return@withRenderType
                // Like GTNH, the ribbons are double-sided: the inner wall shows through the slits.
                RenderSystem.disableCull()
                RenderUtils.withShaderBatch(shader, true) {
                    for (entry in visible) {
                        val mesh = meshes.getValue(entry.length)
                        poseStack.withPose {
                            translate(entry.axisX - camera.x, entry.bottomY - camera.y, entry.axisZ - camera.z)
                            for ((facing, buffer) in mesh.withIndex()) {
                                val shade = shades[facing]
                                RenderSystem.setShaderColor(red * shade, green * shade, blue * shade, alpha)
                                RenderUtils.withBoundBuffer(buffer) {
                                    buffer.drawWithShader(last().pose(), RenderSystem.getProjectionMatrix(), shader)
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            RenderSystem.setShaderColor(red, green, blue, alpha)
        }
    }

    // Vanilla draws this batch at once, after the cables; under Iris the batch waits for the block entity flush.
    private fun drawGlow(poseStack: PoseStack, camera: Vec3, visible: List<Entry>) {
        val lit = visible.filter { !SpaceElevatorCableGeometry.pulseSections(it.ticks, SpaceElevatorCableGeometry.sections(it.length)).isEmpty() }
        if (lit.isEmpty()) return
        val source = Minecraft.getInstance().renderBuffers().bufferSource()
        val consumer = source.getBuffer(glowType)
        for (entry in lit) {
            poseStack.withPose {
                translate(entry.axisX - camera.x, entry.bottomY - camera.y, entry.axisZ - camera.z)
                val pose = last()
                for (section in SpaceElevatorCableGeometry.pulseSections(entry.ticks, SpaceElevatorCableGeometry.sections(entry.length))) {
                    val glow = SpaceElevatorCableGeometry.pulse(section, entry.ticks)
                    if (glow <= 0f) continue
                    for (strand in 0 until SpaceElevatorCableGeometry.STRANDS) {
                        for (part in 0 until SpaceElevatorCableGeometry.PARTS) {
                            SpaceElevatorCableGeometry.quad(section, strand, part, GLOW_LIFT, corners)
                            for (corner in 0 until 4) {
                                val offset = corner * 5
                                consumer.vertex(pose.pose(), corners[offset], corners[offset + 1], corners[offset + 2])
                                    .color(0f, GLOW_GREEN * glow, glow, 1f)
                                    .uv(corners[offset + 3], corners[offset + 4])
                                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                                    .uv2(LightTexture.FULL_BRIGHT)
                                    .normal(pose.normal(), corners[20], 0f, corners[21])
                                    .endVertex()
                            }
                        }
                    }
                }
            }
        }
        source.endBatch(glowType)
    }

    private fun buildMesh(length: Int): List<VertexBuffer> {
        val sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(cableSprite)
        val sections = SpaceElevatorCableGeometry.sections(length)
        val type = RenderType.cutoutMipped()
        val layers = StructureRenderBuffers.terrainLayers(Facing.entries.size, shaderCompatible) { facing, batch ->
            val builder = batch.builder(type)
            for (section in 0 until sections) {
                for (strand in 0 until SpaceElevatorCableGeometry.STRANDS) {
                    for (part in 0 until SpaceElevatorCableGeometry.PARTS) {
                        if (SpaceElevatorCableGeometry.facing(strand, part).ordinal != facing) continue
                        SpaceElevatorCableGeometry.quad(section, strand, part, 1f, corners)
                        batch.withBlock(builder, null, 0, corners[1].toInt(), 0) {
                            for (corner in 0 until 4) {
                                val offset = corner * 5
                                builder.vertex(corners[offset].toDouble(), corners[offset + 1].toDouble(), corners[offset + 2].toDouble())
                                    .color(255, 255, 255, 255)
                                    .uv(sprite.getU(corners[offset + 3] * 16.0), sprite.getV(corners[offset + 4] * 16.0))
                                    .uv2(cableLight)
                                    .normal(corners[20], 0f, corners[21])
                                    .endVertex()
                            }
                        }
                    }
                }
            }
        }
        return layers.map { it.getValue(type) }
    }

    private data class Entry(
        val blockEntity: BlockEntity,
        val axisX: Double,
        val bottomY: Double,
        val axisZ: Double,
        val length: Int,
        val ticks: Double
    ) {
        fun bounds() = AABB(axisX - 0.5, bottomY, axisZ - 0.5, axisX + 0.5, bottomY + length + 1.0, axisZ + 0.5)
    }
}

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, value = [Dist.CLIENT], bus = Mod.EventBusSubscriber.Bus.MOD)
object SpaceElevatorCableReloadListener {
    @SubscribeEvent
    @JvmStatic
    fun onRegisterReloadListeners(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(
            ResourceManagerReloadListener {
                if (RenderSystem.isOnRenderThread()) {
                    SpaceElevatorCableRenderer.releaseBuffers()
                } else {
                    RenderSystem.recordRenderCall { SpaceElevatorCableRenderer.releaseBuffers() }
                }
            }
        )
    }
}
