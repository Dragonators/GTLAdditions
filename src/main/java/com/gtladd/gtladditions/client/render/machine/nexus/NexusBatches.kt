package com.gtladd.gtladditions.client.render.machine.nexus

import com.gtladd.gtladditions.client.render.machine.nexus.NexusFrame.Companion.fract
import org.joml.Vector3d

internal class NexusBatches(nodes: Map<String, NexusGpu.Node>) {
    private val ringNode = nodes.getValue("batches.rings.mesh")
    private val ribbonNode = nodes.getValue("batches.ribbons.mesh")
    private val pointNode = nodes.getValue("batches.points.mesh")
    private val ring = ringNode.attributeStream("rings", listOf("iC" to 3, "iN" to 3, "iP" to 4, "iCol" to 3, "iS" to 4, "iArc" to 2), true)
    private val ribbon = ribbonNode.attributeStream("ribbons", listOf("iA" to 3, "iB" to 3, "iW" to 4, "iCol" to 3, "iAl" to 2), true)
    private val points = pointNode.attributeStream("points", listOf("position" to 3, "aSize" to 1, "aCol" to 4), false)

    fun ring(
        center: Vector3d,
        radius: Double,
        width: Double,
        color: Vector3d,
        alpha: Double,
        normal: Vector3d = Vector3d(0.0, 0.0, 1.0),
        segments: Double = 0.0,
        gap: Double = 0.0,
        rotation: Double = 0.0,
        arcStart: Double = 0.0,
        arc: Double = 1.01,
        dashes: Double = 0.0,
        duty: Double = 0.5
    ) {
        if (alpha <= 0.002) return
        ring.put(center)
        ring.put(normal)
        ring.put(radius, width, alpha, fract(rotation))
        ring.put(color)
        ring.put(segments, gap, dashes, duty)
        ring.put(arcStart, arc)
    }

    fun point(position: Vector3d, size: Double, color: Vector3d, alpha: Double) {
        if (alpha <= 0.002) return
        points.put(position)
        points.put(size)
        points.put(color.x, color.y, color.z, alpha)
    }

    fun line(
        a: Vector3d,
        b: Vector3d,
        width: Double,
        color: Vector3d,
        alpha: Double,
        endWidth: Double = width,
        endAlpha: Double = alpha,
        flow: Double = 0.0,
        frequency: Double = 0.0
    ) {
        if (maxOf(alpha, endAlpha) <= 0.002) return
        ribbon.put(a)
        ribbon.put(b)
        ribbon.put(width, endWidth, flow, frequency)
        ribbon.put(color)
        ribbon.put(alpha, endAlpha)
    }

    fun upload() {
        ringNode.instances = ring.size / ring.components
        ringNode.visible = ringNode.instances > 0
        ribbonNode.instances = ribbon.size / ribbon.components
        ribbonNode.visible = ribbonNode.instances > 0
        pointNode.drawCount = points.size / points.components
        pointNode.visible = pointNode.drawCount > 0
    }
}
