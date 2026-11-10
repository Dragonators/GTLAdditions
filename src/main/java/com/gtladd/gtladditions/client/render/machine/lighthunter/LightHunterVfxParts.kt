package com.gtladd.gtladditions.client.render.machine.lighthunter

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.client.scene.PartBlend
import com.gtladd.gtladditions.api.client.scene.PartBounds
import com.gtladd.gtladditions.api.client.scene.PartDrawContext
import com.gtladd.gtladditions.api.client.scene.PartFeature
import com.gtladd.gtladditions.api.client.scene.PartKey
import com.gtladd.gtladditions.api.client.scene.PartOutcome
import com.gtladd.gtladditions.api.client.scene.SceneFrame
import com.gtladd.gtladditions.api.client.scene.ScenePart
import com.gtladd.gtladditions.api.client.scene.SceneRendering
import com.gtladd.gtladditions.client.render.machine.heart.HeartHandDepthMask
import com.gtladd.gtladditions.client.render.machine.heart.HeartLayerImages
import com.gtladd.gtladditions.client.render.machine.lighthunter.LightHunterVfxGeometry.Section
import com.gtladd.gtladditions.client.render.scene.CompositePartRenderer
import com.gtladd.gtladditions.client.render.scene.LayeredPartRenderer
import com.gtladd.gtladditions.client.render.withPose
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Matrix4f

@OnlyIn(Dist.CLIENT)
internal object LightHunterVfxParts : LayeredPartRenderer<LightHunterVfx>, CompositePartRenderer<LightHunterVfx> {
    private val provider = SceneRendering.register(GTLAdditions.id("light_hunter"))
    private val fullView = IntArray(4)

    fun submit(frame: SceneFrame, entry: LightHunterVfxRenderer.Entry) {
        val pos = entry.blockEntity.blockPos

        for (section in Section.entries) {
            frame.submit(
                ScenePart.builder(PartKey.block(provider, pos, section.ordinal), this, LightHunterVfx(entry, section))
                    .blend(if (section == Section.EXHAUST) PartBlend.ADDITIVE else PartBlend.STRAIGHT)
                    .bounds(PartBounds.box(LightHunterVfxGeometry.bounds(entry.machine.frontFacing, section).move(pos)))
                    .features(PartFeature.SCENE_DEPTH, PartFeature.OWN_HAND_MASK)
                    .build()
            )
        }
    }

    // Films and emissive marks blend differently at the front and on the shields; the exhaust only adds light.
    override fun composite(part: ScenePart<LightHunterVfx>): Boolean = part.payload.section != Section.EXHAUST

    override fun layered(part: ScenePart<LightHunterVfx>): Boolean = layersCover(part.payload)

    override fun draw(context: PartDrawContext, parts: List<ScenePart<LightHunterVfx>>) {
        for (part in parts) {
            if (layersCover(part.payload)) {
                drawLayers(context, part.payload, 0, 0, 0, fullView, false)
            } else if (!drawMaterial(context, part.payload)) {
                context.report(part, PartOutcome.RESOURCE_UNAVAILABLE)
            }
        }
    }

    override fun drawLayered(
        context: PartDrawContext,
        part: ScenePart<LightHunterVfx>,
        depth: Int,
        count: Int,
        base: Int,
        rect: IntArray,
        local: Boolean
    ) =
        drawLayers(context, part.payload, depth, count, base, rect, local)

    /** The layered programs cover everything the part draws this frame; shaders fail to load one by one. */
    private fun layersCover(vfx: LightHunterVfx): Boolean {
        if (LightHunterShaders.layers == null) return false
        val seconds = vfx.entry.paradoxSeconds
        if (vfx.section != Section.FRONT || seconds == null) return true
        return (
            !LightHunterFusionRenderer.drawn(seconds) ||
                LightHunterShaders.fusion == null ||
                LightHunterShaders.fusionLayers != null
            ) &&
            (
                !LightHunterParadoxRenderer.windowDrawn(seconds) ||
                    LightHunterShaders.paradox == null ||
                    LightHunterShaders.paradoxModel == null ||
                    LightHunterShaders.paradoxLayers != null
                )
    }

    /** An ordered draw over the full view, or one pass of a Heart bin. Every call sets the layer uniforms of the shared programs. */
    private fun drawLayers(
        context: PartDrawContext,
        vfx: LightHunterVfx,
        depth: Int,
        count: Int,
        base: Int,
        rect: IntArray,
        local: Boolean
    ) {
        val shader = LightHunterShaders.layers ?: return
        val scene = context.sceneDepth!!
        val handMask = context.handMaskTexture
        val entry = vfx.entry
        scene.bind(shader)
        HeartHandDepthMask.bind(shader, handMask)
        withModelView(context, entry) { modelView, projection ->
            LightHunterVfxRenderer.drawLayered(
                LightHunterVfxRenderer.meshes,
                shader,
                modelView,
                projection,
                entry.tick,
                depth,
                count,
                base,
                rect,
                local,
                vfx.section.layers,
                entry.paradoxSeconds ?: -1f
            )
            val seconds = entry.paradoxSeconds
            if (vfx.section == Section.FRONT && seconds != null) {
                LightHunterShaders.fusionLayers?.let { fusion ->
                    scene.bind(fusion)
                    HeartHandDepthMask.bind(fusion, handMask)
                    LightHunterFusionRenderer.drawLayered(
                        LightHunterFusionRenderer.meshes,
                        fusion,
                        modelView,
                        projection,
                        seconds,
                        entry.tick,
                        depth,
                        count,
                        base,
                        rect,
                        local
                    )
                }

                val paradox = LightHunterShaders.paradoxLayers
                val model = LightHunterShaders.paradoxModel
                if (paradox != null && model != null) {
                    scene.bind(paradox)
                    HeartHandDepthMask.bind(paradox, handMask)
                    HeartLayerImages.bindArray(3, depth)
                    paradox.getUniform("LayerDepths")?.set(3)
                    paradox.getUniform("LayerCount")?.set(count)
                    paradox.getUniform("LayerBase")?.set(base)
                    paradox.getUniform("LayerMode")?.set(if (local) 1 else 0)
                    paradox.getUniform("LayerOrigin")?.set(if (local) rect[0].toFloat() else 0f, if (local) rect[1].toFloat() else 0f)
                    paradox.getUniform("LayerRect")?.set(rect[0].toFloat(), rect[1].toFloat(), rect[2].toFloat(), rect[3].toFloat())
                    LightHunterParadoxRenderer.draw(
                        LightHunterParadoxRenderer.meshes,
                        paradox,
                        model,
                        modelView,
                        projection,
                        seconds,
                        entry.tick,
                        entry.blockEntity,
                        true,
                        local,
                        entry.windowPass
                    )
                }
            }
        }
    }

    /** The material programs at the ordered position, colour only; false when a program the part needs is missing. */
    private fun drawMaterial(context: PartDrawContext, vfx: LightHunterVfx): Boolean {
        val material = LightHunterShaders.material ?: return false
        // The exhaust has no film.
        val field = if (vfx.section == Section.EXHAUST) null else LightHunterShaders.chartField ?: return false
        val scene = context.sceneDepth!!
        val entry = vfx.entry
        scene.bind(material)
        withModelView(context, entry) { modelView, projection ->
            LightHunterVfxRenderer.draw(
                LightHunterVfxRenderer.meshes,
                material,
                field,
                modelView,
                projection,
                entry.tick,
                vfx.section.layers,
                entry.paradoxSeconds ?: -1f
            )
            val seconds = entry.paradoxSeconds
            if (vfx.section == Section.FRONT && seconds != null) {
                LightHunterShaders.fusion?.let { fusion ->
                    scene.bind(fusion)
                    LightHunterFusionRenderer.draw(LightHunterFusionRenderer.meshes, fusion, modelView, projection, seconds, entry.tick)
                }

                val paradox = LightHunterShaders.paradox
                val model = LightHunterShaders.paradoxModel
                if (paradox != null && model != null) {
                    LightHunterParadoxRenderer.draw(
                        LightHunterParadoxRenderer.meshes,
                        paradox,
                        model,
                        modelView,
                        projection,
                        seconds,
                        entry.tick,
                        entry.blockEntity,
                        false,
                        false,
                        entry.windowPass
                    )
                }
            }
        }

        return true
    }

    /** The ship's model-view relative to the frame camera, and the frame projection. */
    private inline fun withModelView(context: PartDrawContext, entry: LightHunterVfxRenderer.Entry, draw: (Matrix4f, Matrix4f) -> Unit) {
        val pos = entry.blockEntity.blockPos
        val frame = context.frame
        context.poseStack.withPose {
            translate(pos.x - frame.cameraX, pos.y - frame.cameraY, pos.z - frame.cameraZ)
            draw(
                Matrix4f(last().pose()).mul(LightHunterVfxGeometry.transform(entry.machine.frontFacing)),
                RenderSystem.getProjectionMatrix()
            )
        }
    }
}

internal class LightHunterVfx(val entry: LightHunterVfxRenderer.Entry, val section: Section)