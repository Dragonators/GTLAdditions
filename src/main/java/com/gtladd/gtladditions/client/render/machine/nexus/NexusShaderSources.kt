package com.gtladd.gtladditions.client.render.machine.nexus

internal object NexusShaderSources {
    fun vertex(material: String, common: String): String = header(common, true) + material

    fun fragment(material: String, common: String, handMask: String): String =
        header(common, false) + material + "\n" + handMask

    private fun header(common: String, vertex: Boolean): String = buildString {
        append("#version 330 core\n")
        if (vertex) append("#define NEXUS_VERTEX_SHADER\n")
        append(common).append('\n')
    }
}
