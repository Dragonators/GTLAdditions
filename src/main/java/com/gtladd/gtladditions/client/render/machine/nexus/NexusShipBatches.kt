package com.gtladd.gtladditions.client.render.machine.nexus

import com.google.gson.JsonObject
import java.util.IdentityHashMap

internal class NexusShipBatches(nodes: Map<String, NexusGpu.Node>) {
    private data class Key(val geometry: String, val material: JsonObject, val glow: Boolean)

    private class Group(val members: List<NexusGpu.Node>, val glow: Boolean) {
        val node = NexusGpu.shipBatch(members.first().data)
        val stream = node.attributeStream("ship-instances", fields, true)

        init {
            node.instanced = true
        }

        fun update(frame: NexusFrame) {
            node.reset()

            for (member in members) {
                if (!member.visible) continue
                stream.put(member.matrix)
                stream.put(member.scalarValue("uI").toDouble())
                node.instances++
            }

            node.visible = node.instances > 0
            node.value("uNeg", frame.inverse)
            node.value("uNegGlow", if (glow) 0.75 else 0.0)
        }
    }

    private val groups: List<Group>
    val drawables: List<NexusGpu.Node>

    init {
        val grouped = LinkedHashMap<Key, MutableList<NexusGpu.Node>>()

        for (node in nodes.values) {
            if (!node.label.startsWith("ship.")) continue
            val material = node.data.getAsJsonObject("uniforms").deepCopy()
            for (name in listOf("uI", "uNeg", "uNegGlow", "uCam", "uTime", "cameraPosition")) material.remove(name)
            val glow = node.label.substringAfterLast('.').toInt() in 1..5
            grouped.getOrPut(Key(node.data.getAsJsonObject("geometry").toString(), material, glow)) { ArrayList() }.add(node)
        }

        val leaders = IdentityHashMap<NexusGpu.Node, NexusGpu.Node>()
        groups = grouped.map { (key, members) -> Group(members, key.glow).also { leaders[members.first()] = it.node } }
        drawables = nodes.values.mapNotNull { if (it.label.startsWith("ship.")) leaders[it] else it }
    }

    fun update(frame: NexusFrame) {
        for (group in groups) group.update(frame)
    }

    companion object {
        private val fields = listOf("nexusM0" to 4, "nexusM1" to 4, "nexusM2" to 4, "nexusM3" to 4, "nexusIntensity" to 1)
    }
}
