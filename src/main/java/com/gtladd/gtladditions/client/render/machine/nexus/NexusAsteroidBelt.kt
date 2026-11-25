package com.gtladd.gtladditions.client.render.machine.nexus

import com.gtladd.gtladditions.client.RenderUtils.rotationRadians
import com.gtladd.gtladditions.client.render.scene.SceneScheduler
import net.minecraft.client.Minecraft
import org.joml.FrustumIntersection
import org.joml.Matrix4f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

internal class NexusAsteroidBelt(nodes: Map<String, NexusGpu.Node>) {
    private val solids = nodes.getValue("dir.beltRocks")
    private val sprites = nodes.getValue("dir.beltDust")
    private val pageSize = (solids.data.getAsJsonObject("geometry").get("indexPageTriangles")?.asInt ?: 80) * 3

    private class Rock(val x: Float, val y: Float, val z: Float, val diameter: Float, val firstPage: Int, val pages: Int) {
        var submitted = false
    }

    private val rocks = NexusGpu.belt().getAsJsonArray("rocks").map {
        val row = it.asJsonArray
        require(pageSize > 0 && row[4].asInt % pageSize == 0 && row[5].asInt % pageSize == 0)
        Rock(row[0].asFloat, row[1].asFloat, row[2].asFloat, row[3].asFloat, row[4].asInt / pageSize, row[5].asInt / pageSize)
    }

    private val solidIndices: NexusIndexPages
    private val spriteIndices = NexusIndexPages(IntArray(rocks.size) { it }, 1)
    private val solidChoice = BooleanArray(rocks.size)
    private val drawNodes = arrayOf(solids, sprites)

    private val lastView = Matrix4f()
    private val lastProjection = Matrix4f()
    private var lastSelectionTime = Double.NaN
    private var lastHeight = 0
    private var lastFalling = false
    private val modelView = Matrix4f()
    private val clip = Matrix4f()
    private val frustum = FrustumIntersection()

    private val spin = FloatArray(65 * 2)
    private var lastSpin = Double.NaN
    private var initialized = false

    init {
        solidIndices = NexusIndexPages(NexusGpu.beltIndices(), pageSize)
        solids.indices = solidIndices
        sprites.indices = spriteIndices
    }

    fun update(frame: NexusFrame, view: Matrix4f, projection: Matrix4f) {
        if (frame.vanish >= 1 || (frame.inverse <= 0.5 && 1 - frame["orbitFade"] <= 0.01)) return

        solids.matrix.identity().rotateZ(rotationRadians(2 * PI * frame.beltTime / 100.531))
        sprites.matrix.set(solids.matrix)

        val gain = 0.3 + 0.7 * minOf(1.0, frame.rim)
        for (node in drawNodes) {
            node.value("uNeg", frame.inverse * 0.85)
            node.value("uGold", gain, gain * 0.77, gain * 0.42)
            node.value("uFall", frame.vanish)
            node.value("uStarR", frame.starRadius)
        }

        solids.value("uAmbK", 0.05 + 0.3 * frame.ambient)
        solids.value("uFillC", 0.68 + 0.15 * frame.ambient, 0.70 + 0.15 * frame.ambient, 0.78 + 0.15 * frame.ambient)

        if (lastSpin != frame.beltTime) {
            phases(frame.beltTime, spin)
            lastSpin = frame.beltTime
        }

        solids.value("uSpinPhase", spin)

        val elapsed = frame.beltTime - lastSelectionTime
        val falling = frame.vanish > 0
        if (!initialized ||
            falling != lastFalling ||
            elapsed < 0.0 ||
            elapsed >= 0.1 ||
            lastHeight != Minecraft.getInstance().window.height ||
            !view.equals(lastView, 1e-5f) ||
            !projection.equals(lastProjection, 1e-6f)
        ) {
            select(frame.beltTime, view, projection, falling)
        }

        solids.drawCount = solidIndices.indexCount
        sprites.drawCount = spriteIndices.indexCount
        solids.visible = solids.drawCount > 0
        sprites.visible = sprites.drawCount > 0
        if (SceneScheduler.frameId % 256L == 0L) NexusIndexPages.collect()
    }

    private fun select(time: Double, view: Matrix4f, projection: Matrix4f, falling: Boolean) {
        lastSelectionTime = time
        lastFalling = falling
        lastView.set(view)
        lastProjection.set(projection)
        modelView.set(view).mul(solids.matrix)
        clip.set(projection).mul(modelView)
        frustum.set(clip)
        lastHeight = Minecraft.getInstance().window.height
        val scale = projection.m11() * lastHeight * 0.5f

        for ((i, rock) in rocks.withIndex()) {
            val depth = -(modelView.m02() * rock.x + modelView.m12() * rock.y + modelView.m22() * rock.z + modelView.m32())
            val pixels = rock.diameter * scale / max(0.1f, depth)

            // A half-pixel hysteresis band prevents repeated toggling at the 4px boundary.
            val threshold = if (!initialized) {
                4f
            } else if (solidChoice[i]) {
                3.75f
            } else {
                4.25f
            }

            val solid = pixels >= threshold
            solidChoice[i] = solid

            // Include the minimum-size soft sprite in the conservative culling sphere. At r<=52.4 and one turn/100.531s,
            // the centre moves <0.33 blocks between stationary-camera refreshes. Retain that swept margin.
            val radius = max(rock.diameter, max(0f, depth) * 1.5f / scale) + 0.35f
            // Falling rocks leave their slots for the star, which the static spheres do not follow.
            val inView = falling || frustum.testSphere(rock.x, rock.y, rock.z, radius)
            val submitted = solid && inView
            if (rock.submitted != submitted) {
                solidIndices.setVisible(rock.firstPage, rock.pages, submitted)
                rock.submitted = submitted
            }

            spriteIndices.setVisible(i, 1, !solid && inView)
        }

        initialized = true
    }

    companion object {
        /** 65 rates from 15 through 45 degrees/s; never convert unbounded seconds to float. */
        internal fun phases(beltTime: Double, output: FloatArray) {
            for (i in 0..64) {
                val period = 768.0 / (i + 32)
                val halfAngle = (beltTime % period) / period * PI
                output[i * 2] = sin(halfAngle).toFloat()
                output[i * 2 + 1] = cos(halfAngle).toFloat()
            }
        }
    }
}
