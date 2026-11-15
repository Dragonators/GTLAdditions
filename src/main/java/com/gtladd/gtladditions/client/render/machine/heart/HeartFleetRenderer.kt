package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.client.scene.PartBlend
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
import com.gtladd.gtladditions.common.machine.multiblock.controller.HeartOfTheCosmos
import com.gtladd.gtladditions.common.machine.multiblock.structure.layout.HeartBlockLayout
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.client.renderer.ShaderInstance
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL13C
import org.lwjgl.opengl.GL15C
import org.lwjgl.system.MemoryUtil
import java.util.BitSet
import kotlin.math.sqrt

@OnlyIn(Dist.CLIENT)
object HeartFleetRenderer {
    /** Station diameters, in blocks. */
    const val DEATH_STAR = 14.0
    const val STARKILLER = 22.5

    private val provider = SceneRendering.register(GTLAdditions.id("heart_fleet"))
    private var plan: HeartFleetPlan? = null
    private var lines: HeartFleetLines? = null
    private var instanceBuffer = 0
    private var instanceOffsets: Map<HeartFleetAssets.Model, Int> = emptyMap()
    private var instanceCapacity = 0

    /** [HeartFleetPlan]'s frame to the game frame of a controller facing south. */
    private val STUDY = Matrix4f(1f, 0f, 0f, 0f, 0f, 0f, -1f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f)
    private val DS_POLE = Vector3f(-0.3f, 0.1f, 1f).normalize()
    private val SK_POLE = Vector3f(0.25f, -0.2f, 1f).normalize()
    private const val DS_SPIN = 0.14
    private const val SK_SPIN = 0.09

    class Frame(
        val machine: HeartOfTheCosmos,
        val orbitTime: Double,
        val time: Double,
        val ease: Double,
        val heart: Vector3d,
        val poses: Array<HeartFleetPlan.Pose>,
        val study: Matrix4f
    )

    private class Sprite(val x: Float, val y: Float, val z: Float, val halfSize: Float, val r: Int, val g: Int, val b: Int)

    private class Light(val frame: Frame, val sprites: List<Sprite> = emptyList(), val arc: Int = -1)

    fun release() {
        plan = null
        lines?.close()
        lines = null
        if (instanceBuffer != 0) GlStateManager._glDeleteBuffers(instanceBuffer)
        instanceBuffer = 0
        instanceOffsets = emptyMap()
        instanceCapacity = 0
        Lights.release()
    }

    private fun plan(assets: HeartFleetAssets.Loaded, shells: HeartShellData.Data): HeartFleetPlan {
        plan?.let { return it }
        val sizes = assets.models
            .filterKeys { it != "deathstar1" }
            .mapValues { (_, m) -> m.length.toDouble() to m.box.map(Float::toDouble).toDoubleArray() }
        val a = shells.shells[0].axis
        // The opening is opposite the innermost shell's cap axis; game (x, y, z) is study (x, -z, y).
        val opening = Vector3d(-a.x.toDouble(), a.z.toDouble(), -a.y.toDouble())
        return HeartFleetPlan(sizes, opening, DEATH_STAR, STARKILLER).also {
            plan = it
            lines = HeartFleetLines(it)
            val capacity = linkedMapOf<HeartFleetAssets.Model, Int>()

            for (i in 0 until it.count) {
                val model = assets.models.getValue(it.type(i))
                capacity[model] = (capacity[model] ?: 0) + 1
            }

            assets.models["deathstar1"]?.let { model -> capacity[model] = 1 }
            var offset = 0
            instanceOffsets = capacity.mapValues { (_, count) ->
                val first = offset
                offset += count
                first
            }

            instanceCapacity = offset
        }
    }

    fun submit(frame: SceneFrame, machine: HeartOfTheCosmos, motion: HeartMotion) {
        val assets = HeartFleetAssets.current() ?: return
        val shells = HeartShellRenderer.shellData() ?: return
        val plan = plan(assets, shells)
        val center = HeartBlockLayout.center(machine.pos, machine.frontFacing)
        val heart = Vector3d(center.x + 0.5, center.y + 0.5, center.z + 0.5)
        val poses = Array(plan.count) { i -> plan.craft(i, motion.orbitTime, HeartFleetPlan.Pose()) }
        val payload = Frame(machine, motion.orbitTime, motion.time, motion.ease, heart, poses, studyToCamera(frame, machine, heart))
        val bounds = PartBounds.sphere(heart.x, heart.y, heart.z, plan.reach)
        frame.submit(
            ScenePart.builder(PartKey.block(provider, machine.pos, 0), Bodies, payload)
                .pass(PartPass.OPAQUE)
                .bounds(bounds)
                .features(PartFeature.OWN_HAND_MASK)
                .build()
        )
        submitLights(frame, payload, plan, assets)
    }

    private fun submitLights(frame: SceneFrame, payload: Frame, plan: HeartFleetPlan, assets: HeartFleetAssets.Loaded) {
        val scale = frame.viewportHeight / 2.0 * frame.projection.m11()
        val study = payload.study
        val point = Vector3f()
        fun sprite(at: Vector3f, worldSize: Double, maxPx: Double, r: Float, g: Float, b: Float): Sprite {
            val distance = maxOf(at.length().toDouble(), 1.0)
            val pixels = (scale * worldSize / distance).coerceIn(3.0, maxPx)
            return Sprite(
                at.x,
                at.y,
                at.z,
                (pixels / 2 * distance / scale).toFloat(),
                (r * 255).toInt().coerceIn(0, 255),
                (g * 255).toInt().coerceIn(0, 255),
                (b * 255).toInt().coerceIn(0, 255)
            )
        }

        fun submit(id: Int, sprites: List<Sprite>) {
            if (sprites.isEmpty()) return
            val bounds = sprites.map { s ->
                PartBounds.sphere(frame.cameraX + s.x, frame.cameraY + s.y, frame.cameraZ + s.z, s.halfSize * sqrt(2.0))
            }

            frame.submit(
                ScenePart.builder(PartKey.block(provider, payload.machine.pos, id), Lights, Light(payload, sprites))
                    .blend(PartBlend.ADDITIVE)
                    .bounds(PartBounds.union(*bounds.toTypedArray()))
                    .batchKey(payload)
                    .build()
            )
        }

        val gain = (0.35 + 0.65 * payload.ease).toFloat()
        for (i in 0 until plan.count) {
            val pose = payload.poses[i]
            if (pose.k <= 0.05) continue
            val tail = plan.tail(i) * pose.k + 0.4 * plan.k
            study.transformPosition(
                (pose.p.x - pose.x.x * tail).toFloat(),
                (pose.p.y - pose.x.y * tail).toFloat(),
                (pose.p.z - pose.x.z * tail).toFloat(),
                point
            )
            submit(1 + i, listOf(sprite(point, minOf(5 * plan.lengthOf(i) / 9, 16.0), 40.0, 0.45f * gain, 0.90f * gain, gain)))
        }

        assets.models["deathstar1"]?.let { model ->
            val at = Vector3d()
            plan.station("ds", payload.orbitTime, at)
            val local = Matrix4f(study)
                .translate(at.x.toFloat(), at.y.toFloat(), at.z.toFloat())
                .rotate(stationRotation(DS_POLE, DS_SPIN, payload.orbitTime))
                .scale((plan.deathStar / 18.0).toFloat())
            val dotGain = (0.4 + 0.6 * payload.ease).toFloat()
            val color = model.pointColor
            val sprites = model.points.map { p ->
                local.transformPosition(p.x, p.y, p.z, point)
                sprite(point, 0.9 * plan.deathStar / 18.0, 28.0, color.x * dotGain, color.y * dotGain, color.z * dotGain)
            }

            submit(1 + plan.count, sprites)
        }

        val geometry = checkNotNull(lines)
        // A full raster pixel at the farthest possible point also covers the one-pixel line's corners.
        val padding = (payload.heart.distance(frame.cameraX, frame.cameraY, frame.cameraZ) + plan.reach) / scale + 0.001

        for (i in geometry.arcs.indices) {
            frame.submit(
                ScenePart.builder(PartKey.block(provider, payload.machine.pos, 2 + plan.count + i), Lights, Light(payload, arc = i))
                    .blend(PartBlend.ADDITIVE)
                    .bounds(geometry.bounds(i, payload.machine, study, payload.heart, padding))
                    .batchKey(payload)
                    .build()
            )
        }
    }

    private fun studyToCamera(frame: SceneFrame, machine: HeartOfTheCosmos, heart: Vector3d): Matrix4f = Matrix4f()
        .translate((heart.x - frame.cameraX).toFloat(), (heart.y - frame.cameraY).toFloat(), (heart.z - frame.cameraZ).toFloat())
        .rotateY(Math.toRadians(HeartShellRenderer.yaw(machine.frontFacing).toDouble()).toFloat())
        .mul(STUDY)

    private fun stationRotation(pole: Vector3f, spin: Double, t: Double): Quaternionf =
        Quaternionf()
            .rotationTo(0f, 0f, 1f, pole.x, pole.y, pole.z)
            .mul(Quaternionf().rotationZ(RenderUtils.rotationRadians(t * spin)))

    private object Bodies : PartRenderer<Frame> {
        override fun draw(context: PartDrawContext, parts: List<ScenePart<Frame>>) {
            val craft = HeartFxShaders.craft
            val starkiller = HeartFxShaders.starkiller
            val assets = HeartFleetAssets.current()
            val plan = plan
            if (craft == null || starkiller == null || assets == null || plan == null) {
                return parts.forEach { context.report(it, PartOutcome.RESOURCE_UNAVAILABLE) }
            }

            RenderUtils.withSavedRenderState {
                val arrayBuffer = GL11C.glGetInteger(GL15C.GL_ARRAY_BUFFER_BINDING)
                RenderSystem.disableCull()
                HeartHandDepthMask.bind(craft, context.handMaskTexture)
                HeartHandDepthMask.bind(starkiller, context.handMaskTexture)
                try {
                    for (part in parts) drawFrame(context, part.payload, plan, assets, craft, starkiller)
                } finally {
                    GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, arrayBuffer)
                    craft.getUniform("SceneHandMaskEnabled")?.set(0)
                    starkiller.getUniform("SceneHandMaskEnabled")?.set(0)
                    RenderSystem.enableCull()
                    RenderSystem.activeTexture(GL13C.GL_TEXTURE0)
                }
            }
        }

        private fun drawFrame(
            context: PartDrawContext,
            payload: Frame,
            plan: HeartFleetPlan,
            assets: HeartFleetAssets.Loaded,
            craft: ShaderInstance,
            starkiller: ShaderInstance
        ) {
            val frame = context.frame
            val view = Matrix4f(context.poseStack.last().pose())
            val projection = RenderSystem.getProjectionMatrix()
            val study = payload.study
            val center = Vector3f(
                (payload.heart.x - frame.cameraX).toFloat(),
                (payload.heart.y - frame.cameraY).toFloat(),
                (payload.heart.z - frame.cameraZ).toFloat()
            )

            for (shader in listOf(craft, starkiller)) {
                shader.getUniform("ProjMat")?.set(projection)
                shader.getUniform("Center")?.set(center)
                shader.getUniform("Power")?.set(payload.ease.toFloat())
                shader.getUniform("Time")?.set((payload.time % 3600.0).toFloat())
            }

            val t = payload.orbitTime
            val instances = linkedMapOf<HeartFleetAssets.Model, MutableList<Matrix4f>>()

            for (i in 0 until plan.count) {
                val pose = payload.poses[i]
                if (pose.k <= 0.05) continue
                val model = assets.models[plan.type(i)] ?: continue
                val s = (plan.k * pose.k).toFloat()
                val local = Matrix4f(
                    pose.x.x.toFloat() * s, pose.x.y.toFloat() * s, pose.x.z.toFloat() * s, 0f,
                    pose.y.x.toFloat() * s, pose.y.y.toFloat() * s, pose.y.z.toFloat() * s, 0f,
                    pose.up.x.toFloat() * s, pose.up.y.toFloat() * s, pose.up.z.toFloat() * s, 0f,
                    pose.p.x.toFloat(), pose.p.y.toFloat(), pose.p.z.toFloat(), 1f
                )
                instances.getOrPut(model, ::mutableListOf).add(Matrix4f(study).mul(local))
            }

            val at = Vector3d()
            assets.models["deathstar1"]?.let { model ->
                plan.station("ds", t, at)
                val scale = (plan.deathStar / 18.0).toFloat()
                val local = Matrix4f()
                    .translation(at.x.toFloat(), at.y.toFloat(), at.z.toFloat())
                    .rotate(stationRotation(DS_POLE, DS_SPIN, t))
                    .scale(scale)
                instances.getOrPut(model, ::mutableListOf).add(Matrix4f(study).mul(local))
            }

            val instanced = HeartFxShaders.craftInstanceAttributes != null &&
                (GL.getCapabilities().OpenGL33 || GL.getCapabilities().GL_ARB_instanced_arrays)
            if (instanced) {
                val data = MemoryUtil.memCallocFloat(instanceCapacity * 32)
                try {
                    for ((model, matrices) in instances) {
                        data.position(instanceOffsets.getValue(model) * 32)

                        for (matrix in matrices) {
                            matrix.get(data.position(), data)
                            data.position(data.position() + 16)
                            Matrix4f(view).mul(matrix).get(data.position(), data)
                            data.position(data.position() + 16)
                        }
                    }

                    data.position(0)
                    data.limit(instanceCapacity * 32)
                    if (instanceBuffer == 0) instanceBuffer = GlStateManager._glGenBuffers()
                    GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, instanceBuffer)
                    GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, data, GL15C.GL_STREAM_DRAW)
                    GlStateManager._glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0)
                    craft.getUniform("Instanced")?.set(1)

                    for ((model, matrices) in instances) {
                        drawModel(craft, assets, model, null, view, matrices.size, instanceOffsets.getValue(model) * 128)
                    }
                } finally {
                    craft.getUniform("Instanced")?.set(0)
                    MemoryUtil.memFree(data)
                }
            } else {
                craft.getUniform("Instanced")?.set(0)

                for ((model, matrices) in instances) {
                    for (matrix in matrices) {
                        drawModel(craft, assets, model, matrix, view)
                    }
                }
            }

            plan.station("sk", t, at)
            val radius = (plan.starkiller / 2).toFloat()
            val local = Matrix4f()
                .translation(at.x.toFloat(), at.y.toFloat(), at.z.toFloat())
                .rotate(stationRotation(SK_POLE, SK_SPIN, t))
                .scale(radius)
            val modelMat = Matrix4f(study).mul(local)
            starkiller.getUniform("ModelMat")?.set(modelMat)
            starkiller.getUniform("ModelViewMat")?.set(Matrix4f(view).mul(modelMat))
            starkiller.setSampler("Sampler0", assets.ice)
            starkiller.setSampler("Sampler1", assets.iceColor)
            starkiller.setSampler("Sampler2", assets.wear)
            starkiller.setSampler("Sampler3", assets.wearNormal)

            for (p in assets.starkiller) {
                if (p.name in DOUBLE_SIDED) RenderSystem.disableCull() else RenderSystem.enableCull()
                starkiller.getUniform("Role")?.set(STARKILLER_ROLES[p.name] ?: 0f)
                starkiller.getUniform("Metal")?.set(p.metal)
                starkiller.getUniform("Rough")?.set(p.rough)
                starkiller.getUniform("Emit")?.set(p.emission)
                p.mesh.draw(starkiller)
            }

            RenderSystem.disableCull()
        }

        private fun drawModel(
            shader: ShaderInstance,
            assets: HeartFleetAssets.Loaded,
            model: HeartFleetAssets.Model,
            modelMat: Matrix4f?,
            view: Matrix4f,
            count: Int = 1,
            offset: Int = 0
        ) {
            if (modelMat != null) {
                shader.getUniform("ModelMat")?.set(modelMat)
                shader.getUniform("ModelViewMat")?.set(Matrix4f(view).mul(modelMat))
            }

            for (group in model.groups) {
                val m = model.materials[group.material]
                // A worn material has no emissive or metal picture: the worn-metal pictures take those two slots.
                shader.setSampler("Sampler0", if (m.map >= 0) assets.textures[m.map] else assets.white)
                shader.setSampler(
                    "Sampler1",
                    if (m.emap >= 0) {
                        assets.textures[m.emap]
                    } else if (m.worn) {
                        assets.wear
                    } else {
                        assets.white
                    }
                )
                shader.setSampler("Sampler2", if (m.normalMap >= 0) assets.textures[m.normalMap] else assets.white)
                shader.setSampler(
                    "Sampler3",
                    if (m.metalMap >= 0) {
                        assets.textures[m.metalMap]
                    } else if (m.worn) {
                        assets.wearNormal
                    } else {
                        assets.white
                    }
                )
                shader.getUniform("BaseColor")?.set(m.color)
                shader.getUniform("Emissive")?.set(m.emissive)
                shader.getUniform("Cut")?.set(m.cut)
                shader.getUniform("NormalScale")?.set(if (m.normalMap >= 0) m.normalScale else 0f)
                shader.getUniform("Metal")?.set(m.metal)
                shader.getUniform("Rough")?.set(m.rough)
                shader.getUniform("Wear")?.set(if (m.worn) 1f else 0f)
                if (modelMat == null) {
                    group.mesh.drawInstanced(shader, instanceBuffer, offset, count, checkNotNull(HeartFxShaders.craftInstanceAttributes))
                } else {
                    group.mesh.draw(shader)
                }
            }
        }
    }

    /** The heat ring is seen from behind; the rest is culled so back-to-back faces never z-fight. */
    private val DOUBLE_SIDED = setOf("TS_Ice", "TS_Core", "TS_Heat")
    private val STARKILLER_ROLES = mapOf(
        "TS_Ice" to 1f,
        "TS_Wall_Edges" to 2f,
        "TS_Dark_Metal" to 3f,
        "TS_Core" to 4f,
        "TS_Heat" to 5f,
        "TS_Service_Windows" to 6f
    )

    private object Lights : PartRenderer<Light> {
        private class SpriteRange(val buffer: HeartFleetSprites, val first: Int, val count: Int)
        private val spriteBuffers = ArrayList<HeartFleetSprites>()

        // Keys hold only IDs, so this cache never retains a machine or level between frames.
        private val spriteRanges = HashMap<PartKey, SpriteRange>()

        fun release() {
            spriteRanges.clear()
            spriteBuffers.forEach(HeartFleetSprites::close)
            spriteBuffers.clear()
        }

        override fun prepare(context: PartDrawContext, parts: List<ScenePart<Light>>) {
            spriteRanges.clear()
            if (HeartFxShaders.glow == null) return
            val view = context.poseStack.last().pose()
            val right = Vector3f(view.m00(), view.m10(), view.m20())
            val up = Vector3f(view.m01(), view.m11(), view.m21())
            val groups = parts.filter { it.payload.sprites.isNotEmpty() }.groupBy { it.payload.frame }

            for ((index, group) in groups.values.withIndex()) {
                if (index == spriteBuffers.size) spriteBuffers.add(HeartFleetSprites())
                val buffer = spriteBuffers[index]
                val quads = group.sumOf { it.payload.sprites.size }
                val vertices = MemoryUtil.memAlloc(quads * 4 * DefaultVertexFormat.POSITION_TEX_COLOR.vertexSize)
                try {
                    var first = 0

                    for (part in group) {
                        for (s in part.payload.sprites) {
                            fun vertex(x: Float, y: Float) {
                                vertices.putFloat(s.x + (right.x * x + up.x * y) * s.halfSize)
                                vertices.putFloat(s.y + (right.y * x + up.y * y) * s.halfSize)
                                vertices.putFloat(s.z + (right.z * x + up.z * y) * s.halfSize)
                                vertices.putFloat((x + 1) / 2).putFloat((y + 1) / 2)
                                vertices.put(s.r.toByte()).put(s.g.toByte()).put(s.b.toByte()).put(255.toByte())
                            }

                            vertex(-1f, -1f)
                            vertex(1f, -1f)
                            vertex(1f, 1f)
                            vertex(-1f, 1f)
                        }

                        spriteRanges[part.key] = SpriteRange(buffer, first, part.payload.sprites.size)
                        first += part.payload.sprites.size
                    }

                    vertices.flip()
                    buffer.upload(vertices, quads)
                } finally {
                    MemoryUtil.memFree(vertices)
                }
            }
        }

        override fun draw(context: PartDrawContext, parts: List<ScenePart<Light>>) {
            if (parts.isEmpty()) return
            val payload = parts.first().payload.frame
            val view = Matrix4f(context.poseStack.last().pose())
            RenderUtils.withSavedRenderState {
                RenderSystem.enableBlend()
                RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE)
                RenderSystem.depthMask(false)
                RenderSystem.enableDepthTest()
                RenderSystem.depthFunc(GL11C.GL_LEQUAL)
                RenderSystem.disableCull()
                drawFrame(context, parts, payload, view)
            }
        }

        private fun drawFrame(context: PartDrawContext, parts: List<ScenePart<Light>>, payload: Frame, view: Matrix4f) {
            val glow = HeartFxShaders.glow
            if (parts.any { it.payload.sprites.isNotEmpty() }) {
                if (glow == null) {
                    parts.filter { it.payload.sprites.isNotEmpty() }.forEach { context.report(it, PartOutcome.RESOURCE_UNAVAILABLE) }
                } else {
                    val selected = BitSet()
                    var buffer: HeartFleetSprites? = null

                    for (part in parts) {
                        if (part.payload.sprites.isEmpty()) continue
                        val range = spriteRanges[part.key]
                        if (range == null) {
                            context.report(part, PartOutcome.RESOURCE_UNAVAILABLE)
                            continue
                        }

                        buffer = range.buffer
                        selected.set(range.first, range.first + range.count)
                    }

                    if (!selected.isEmpty) {
                        glow.getUniform("ModelViewMat")?.set(view)
                        glow.getUniform("ProjMat")?.set(RenderSystem.getProjectionMatrix())
                        glow.apply()
                        try {
                            checkNotNull(buffer).draw(selected)
                        } finally {
                            glow.clear()
                        }
                    }
                }
            }

            val arcs = parts.filter { it.payload.arc >= 0 }
            if (arcs.isEmpty()) return
            val shader = HeartFxShaders.line
            val geometry = lines
            if (shader == null || geometry == null) {
                return arcs.forEach { context.report(it, PartOutcome.RESOURCE_UNAVAILABLE) }
            }

            val opacity = (0.14 + 0.28 * payload.ease).toFloat()
            shader.getUniform("Tint")?.set(
                (0.608f * opacity * 255).toInt() / 255f,
                (0.863f * opacity * 255).toInt() / 255f,
                (opacity * 255).toInt() / 255f,
                1f
            )
            shader.getUniform("ModelMat")?.set(payload.study)
            shader.getUniform("ModelViewMat")?.set(view)
            shader.getUniform("ProjMat")?.set(RenderSystem.getProjectionMatrix())
            shader.apply()
            try {
                // 36 arcs fit a Long mask; additive blending makes their order irrelevant.
                var selected = 0L
                for (part in arcs) selected = selected or (1L shl part.payload.arc)
                geometry.draw(selected)
            } finally {
                shader.clear()
            }
        }
    }
}