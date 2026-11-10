package com.gtladd.gtladditions.client.render.machine.lighthunter

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.client.scene.PartBounds
import com.gtladd.gtladditions.api.client.scene.PartDrawContext
import com.gtladd.gtladditions.api.client.scene.PartKey
import com.gtladd.gtladditions.api.client.scene.PartOutcome
import com.gtladd.gtladditions.api.client.scene.PartPass
import com.gtladd.gtladditions.api.client.scene.PartRenderer
import com.gtladd.gtladditions.api.client.scene.SceneFrame
import com.gtladd.gtladditions.api.client.scene.ScenePart
import com.gtladd.gtladditions.api.client.scene.SceneRendering
import com.gtladd.gtladditions.client.render.withPose
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.world.phys.AABB
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Matrix4f

@OnlyIn(Dist.CLIENT)
internal object LightHunterSolidParts : PartRenderer<LightHunterSolidParts.Solid> {
    private const val CABIN = 0
    private const val WINDOW = 1
    private val provider = SceneRendering.register(GTLAdditions.id("light_hunter_solid"))

    fun submit(frame: SceneFrame, entry: LightHunterVfxRenderer.Entry) {
        val machine = entry.machine
        val pos = entry.blockEntity.blockPos
        if (machine.isFormed) frame.submit(part(entry, CABIN, LightHunterInteriorGeometry.bounds(machine.frontFacing).move(pos)))
        val seconds = entry.paradoxSeconds ?: return
        if (machine.recipeLogic.isWorking &&
            LightHunterParadoxRenderer.windowDrawn(seconds) &&
            LightHunterParadoxTiming.window(seconds).reveal >= 1f
        ) {
            frame.submit(part(entry, WINDOW, LightHunterParadoxGeometry.bounds(machine.frontFacing).move(pos)))
        }
    }

    private fun part(entry: LightHunterVfxRenderer.Entry, kind: Int, box: AABB) =
        ScenePart.builder(PartKey.block(provider, entry.blockEntity.blockPos, kind), this, Solid(entry, kind))
            .pass(PartPass.OPAQUE)
            .bounds(PartBounds.box(box))
            .build()

    override fun draw(context: PartDrawContext, parts: List<ScenePart<Solid>>) {
        val frame = context.frame

        for (part in parts) {
            val entry = part.payload.entry
            val pos = entry.blockEntity.blockPos
            context.poseStack.withPose {
                translate(pos.x - frame.cameraX, pos.y - frame.cameraY, pos.z - frame.cameraZ)
                val modelView = Matrix4f(last().pose()).mul(LightHunterVfxGeometry.transform(entry.machine.frontFacing))
                val projection = RenderSystem.getProjectionMatrix()
                val drawn = if (part.payload.kind == CABIN) LightHunterInteriorRenderer.draw(modelView, projection, entry.tick) else drawWindow(entry, modelView, projection)
                if (!drawn) context.report(part, PartOutcome.RESOURCE_UNAVAILABLE)
            }
        }
    }

    private fun drawWindow(entry: LightHunterVfxRenderer.Entry, modelView: Matrix4f, projection: Matrix4f): Boolean {
        val shader = LightHunterShaders.paradoxOpaque ?: return false
        val model = LightHunterShaders.paradoxModel ?: return false
        val meshes = LightHunterParadoxRenderer.meshes
        entry.opaqueWindowDrawn = LightHunterParadoxRenderer.drawOpaque(
            meshes,
            shader,
            model,
            modelView,
            projection, entry.paradoxSeconds!!,
            entry.tick,
            entry.blockEntity
        )

        return entry.opaqueWindowDrawn
    }

    internal class Solid(val entry: LightHunterVfxRenderer.Entry, val kind: Int)
}