package com.gtladd.gtladditions.client.render.machine.nexus

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.client.scene.PartBlend
import com.gtladd.gtladditions.api.client.scene.PartDrawContext
import com.gtladd.gtladditions.api.client.scene.PartFeature
import com.gtladd.gtladditions.api.client.scene.PartKey
import com.gtladd.gtladditions.api.client.scene.PartPass
import com.gtladd.gtladditions.api.client.scene.SceneFrame
import com.gtladd.gtladditions.api.client.scene.ScenePart
import com.gtladd.gtladditions.api.client.scene.SceneRendering
import com.gtladd.gtladditions.client.render.scene.CompositePartRenderer
import com.gtladd.gtladditions.common.machine.multiblock.controller.SubspaceCorridorNexusIndustrialArray
import com.gtladd.gtladditions.utils.SubspaceNexusAnimationClock
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.core.Direction
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f
import org.lwjgl.opengl.GL11.*

@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object NexusScene {
    // Values contain no holder/machine references, allowing unloaded hosts to be reclaimed.
    private val scenes = java.util.WeakHashMap<BlockEntity, Scene>()

    private var unavailable = false

    private fun scene(blockEntity: BlockEntity): Scene? = scenes[blockEntity] ?: if (unavailable || NexusGpu.current == null) {
        null
    } else {
        try {
            Scene().also { scenes[blockEntity] = it }
        } catch (error: Exception) {
            unavailable = true
            GTLAdditions.LOGGER.error("Subspace Nexus scene is unavailable until the next resource reload", error)
            null
        }
    }

    internal class Scene {
        val nodes = NexusGpu.nodes().associate { it.label to NexusGpu.Node(it.data, it.program, it.geometry) }
        val batches = NexusBatches(nodes)
        val ships = NexusShipBatches(nodes)
        val fleet = NexusFleet.State(nodes)
        val screens = NexusScreens.State(nodes)
        val center = NexusCenter.State(nodes)
        val planets = NexusPlanets.State(nodes)
        val belt = NexusAsteroidBelt(nodes)

        val opaque = ArrayList<NexusGpu.Node>()
        val transparent = ArrayList<NexusGpu.Node>()

        fun update(frame: NexusFrame, view: Matrix4f, projection: Matrix4f) {
            for (node in nodes.values) node.reset()
            val logistics = NexusFleet.update(fleet, frame, batches)
            NexusCenter.update(center, frame, logistics, batches)
            NexusPlanets.update(planets, frame, batches)
            belt.update(frame, view, projection)
            val order = NexusScreens.update(screens, frame, logistics, batches)
            batches.upload()
            ships.update(frame)

            opaque.clear()
            transparent.clear()

            for (node in ships.drawables) {
                if (node.visible) (if (node.opaque) opaque else transparent).add(node)
            }

            val comparator = compareBy<NexusGpu.Node> { order[it] ?: it.order }
            opaque.sortWith(comparator)
            transparent.sortWith(comparator)
        }
    }

    fun enqueue(blockEntity: BlockEntity, machine: SubspaceCorridorNexusIndustrialArray, partialTicks: Float) {
        val animation = machine.nexusEffects.animationFrame(partialTicks) ?: return
        NexusCabins.enqueue(blockEntity, machine, animation)
        Parts.submit(SceneRendering.frame() ?: return, blockEntity, machine, animation)
    }

    @SubscribeEvent
    @JvmStatic
    fun reload(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(Reload)
    }

    internal object Reload : SimplePreparableReloadListener<() -> NexusGpu.Prepared>() {
        override fun prepare(resources: ResourceManager, profiler: ProfilerFiller): () -> NexusGpu.Prepared {
            val prepared = try {
                NexusGpu.read(resources)
            } catch (error: Exception) {
                return { throw error }
            }
            return { prepared }
        }

        override fun apply(prepared: () -> NexusGpu.Prepared, resources: ResourceManager, profiler: ProfilerFiller) =
            NexusGpu.replace(prepared) {
                scenes.clear()
                unavailable = false
                NexusIndexPages.release()
                NexusCabins.release()
            }
    }

    internal fun applyController(pose: PoseStack, machine: SubspaceCorridorNexusIndustrialArray) {
        pose.translate(0.5, 0.5, 0.5)
        pose.mulPose(
            Axis.YP.rotationDegrees(
                when (machine.frontFacing) {
                    Direction.EAST -> 90f
                    Direction.NORTH -> 180f
                    Direction.WEST -> -90f
                    else -> 0f
                }
            )
        )
        if (machine.isFlipped) pose.scale(-1f, 1f, 1f)
        pose.translate(-21.5, -31.5, -130.5)
    }

    internal class Draw(
        val blockEntity: BlockEntity,
        val machine: SubspaceCorridorNexusIndustrialArray,
        val scene: Scene,
        val frame: NexusFrame,
        val view: Matrix4f,
        val camera: FloatArray
    )

    internal object Parts : CompositePartRenderer<Draw> {
        private val provider = SceneRendering.register(GTLAdditions.id("nexus"))

        fun submit(
            frame: SceneFrame,
            blockEntity: BlockEntity,
            machine: SubspaceCorridorNexusIndustrialArray,
            animation: SubspaceNexusAnimationClock.Frame
        ) {
            val prepared = prepare(frame, blockEntity, machine, animation) ?: return
            val wide = prepared.frame["flash"] > 0.004 ||
                prepared.frame.eventAge(0.05, 1.6) != null ||
                prepared.frame.eventAge(16.0, 1.6) != null
            val (opaque, transparent) = NexusBounds.parts(prepared, frame.projection, wide)
            val pos = blockEntity.blockPos
            frame.submit(
                ScenePart.builder(PartKey.block(provider, pos, 0), this, prepared)
                    .pass(PartPass.OPAQUE)
                    .bounds(opaque)
                    .features(PartFeature.OWN_HAND_MASK)
                    .build()
            )
            frame.submit(
                ScenePart.builder(PartKey.block(provider, pos, 1), this, prepared)
                    .blend(PartBlend.STRAIGHT)
                    .bounds(transparent)
                    .features(PartFeature.OWN_HAND_MASK)
                    .build()
            )
        }

        private fun prepare(
            frame: SceneFrame,
            blockEntity: BlockEntity,
            machine: SubspaceCorridorNexusIndustrialArray,
            animation: SubspaceNexusAnimationClock.Frame
        ): Draw? {
            val scene = scene(blockEntity) ?: return null
            val pos = blockEntity.blockPos
            val pose = PoseStack()
            pose.mulPoseMatrix(Matrix4f(frame.modelView))
            pose.translate(pos.x - frame.cameraX, pos.y - frame.cameraY, pos.z - frame.cameraZ)
            applyController(pose, machine)
            pose.mulPose(Axis.XP.rotationDegrees(-90f))
            val view = Matrix4f(pose.last().pose())
            val camera = Vector3d(Matrix4f(view).invert().transformPosition(Vector3f()))
            val billboard = NexusBillboardView(camera, NexusFrame.STAR)
            val nexusFrame = NexusFrame(machine, animation, camera, billboard.right, billboard.up)
            scene.update(nexusFrame, view, Matrix4f(frame.projection))

            return Draw(
                blockEntity,
                machine,
                scene,
                nexusFrame,
                view,
                floatArrayOf(camera.x.toFloat(), camera.y.toFloat(), camera.z.toFloat())
            )
        }

        // Materials mix blends, so the transparent part never commutes; it orders as a whole and never joins depth bins.
        override fun composite(part: ScenePart<Draw>): Boolean = true

        override fun draw(context: PartDrawContext, parts: List<ScenePart<Draw>>) {
            for (part in parts) render(part.payload, part.pass == PartPass.OPAQUE, context.handMaskTexture)
        }

        private fun render(draw: Draw, opaque: Boolean, handMask: Int) {
            NexusGpu.preserve {
                val winding = glGetInteger(GL_FRONT_FACE)
                try {
                    if (draw.machine.isFlipped) glFrontFace(GL_CW)
                    val projection = RenderSystem.getProjectionMatrix()
                    val sky = (draw.blockEntity.level?.skyDarken ?: 0) / 11f
                    val ambient = 0.2f + 0.8f * (1 - sky.coerceIn(0f, 1f))

                    for (node in if (opaque) draw.scene.opaque else draw.scene.transparent) {
                        node.draw(draw.view, projection, draw.camera, draw.frame.time, draw.frame.glow.toFloat(), ambient, handMask)
                    }
                } finally {
                    glFrontFace(winding)
                }
            }
        }
    }
}
