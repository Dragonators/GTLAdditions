package com.gtladd.gtladditions.client.render.machine.antichrist

import io.github.douira.glsl_transformer.ast.print.ASTPrinter
import io.github.douira.glsl_transformer.ast.query.match.Matcher
import io.github.douira.glsl_transformer.ast.transform.ASTInjectionPoint
import io.github.douira.glsl_transformer.ast.transform.ASTTransformer
import io.github.douira.glsl_transformer.parser.ParseShape

object RingBatchShaderSources {
    const val PREFIX = "gtladditions_foa_batch_"
    const val ATTRIBUTE = "gtladd_foa_RingGroup"
    const val BLOCK = "GtladdFoaPalette"
    const val BYTES_PER_RING = 224
    private const val VARYING = "gtladd_foa_RingGroupV"
    private const val RING = "gtladd_foa_Rings[gtladd_foa_RingIndex()]"
    private const val NO_MODEL_VIEW = "FOA batching could not identify the terrain model-view uniform"
    private const val DRAW_LOCAL_IDS = "FOA batching cannot preserve draw-local vertex/primitive IDs for this program"
    private val main = Regex("""\bvoid\s+main\s*\(\s*(?:void\s*)?\)\s*\{""")
    private val comments = Regex("""(?s)/\*.*?\*/|//[^\r\n]*""")
    private val drawLocalIds = Regex("""\bgl_(?:VertexID|PrimitiveID)\b""")

    private class Parameter(val name: String, val type: String, val definitions: List<String>) {
        val call = "gtladd_foa_$name()"
    }

    private val parameters = listOf(
        matrix("iris_ModelViewMat", "mat4", "modelView"),
        matrix("ModelViewMat", "mat4", "modelView"),
        matrix("iris_ModelViewMatInverse", "mat4", "inverseModelView"),
        matrix("iris_NormalMat", "mat3", "normalMatrix"),
        matrix("NormalMat", "mat3", "normalMatrix"),
        modulator("iris_ColorModulator"),
        modulator("ColorModulator")
    )

    data class Sources(val vertex: String, val fragment: String)

    @JvmStatic
    fun isVariant(name: String): Boolean = name.substringAfter(':').startsWith(PREFIX)

    /** Iris output is preprocessed GLSL, edited as a syntax tree by the transformer that printed it. */
    @JvmStatic
    fun patchIris(transformer: ASTTransformer<*, *>, vertex: String, fragment: String, capacity: Int): Sources {
        require(capacity >= 3)
        return Sources(patchTree(transformer, vertex, capacity, true), patchTree(transformer, fragment, capacity, false))
    }

    /** Core shader text still carries imports and macros, which no syntax tree can hold. */
    @JvmStatic
    fun patchVanilla(vertex: String, fragment: String, capacity: Int): Sources {
        require(capacity >= 3)
        require(main.findAll(vertex).count() == 1 && main.findAll(fragment).count() == 1)
        require(!drawLocalIds.containsMatchIn(comments.replace("$vertex\n$fragment", ""))) { DRAW_LOCAL_IDS }
        return Sources(patchText(vertex, capacity, true), patchText(fragment, capacity, false))
    }

    private fun patchTree(transformer: ASTTransformer<*, *>, source: String, capacity: Int, vertex: Boolean): String {
        require(ATTRIBUTE !in source && BLOCK !in source)
        val tree = transformer.parseSeparateTranslationUnit(source)
        val root = tree.root
        require(!root.identifierIndex.has("gl_VertexID") && !root.identifierIndex.has("gl_PrimitiveID")) { DRAW_LOCAL_IDS }
        val replaced = parameters.filter { parameter ->
            val uniform = Matcher("uniform ${parameter.type} ${parameter.name};", ParseShape.EXTERNAL_DECLARATION)
            val declarations = root.externalDeclarationIndex.getStream(parameter.name)
                .map { it.declaration() }.filter(uniform::matches).toList()
            declarations.forEach { it.detachAndDelete() }
            if (declarations.isNotEmpty()) root.replaceReferenceExpressions(transformer, parameter.name, parameter.call)
            declarations.isNotEmpty()
        }

        if (vertex) {
            require(replaced.any { it.name.endsWith("ModelViewMat") }) { NO_MODEL_VIEW }
            tree.prependMainFunctionBody(transformer, "$VARYING = $ATTRIBUTE;")
        }

        tree.parseAndInjectNodes(
            transformer,
            ASTInjectionPoint.BEFORE_DECLARATIONS,
            *declarations(capacity, vertex, replaced).toTypedArray()
        )
        return ASTPrinter.print(transformer.printType, tree)
    }

    private fun patchText(source: String, capacity: Int, vertex: Boolean): String {
        require(ATTRIBUTE !in source && BLOCK !in source)
        var result = source
        val replaced = parameters.filter { parameter ->
            val uniform = Regex("""\buniform\s+${parameter.type}\s+${parameter.name}\s*;""")
            if (!uniform.containsMatchIn(result)) return@filter false
            result = result.replace(uniform, "").replace(Regex("""\b${parameter.name}\b"""), parameter.call)
            true
        }

        if (vertex) require(replaced.any { it.name.endsWith("ModelViewMat") }) { NO_MODEL_VIEW }
        val header = headerEnd(result)
        result = result.substring(0, header) +
            declarations(capacity, vertex, replaced).joinToString("\n", "\n", "\n\n") +
            result.substring(header)
        if (vertex) {
            val entry = checkNotNull(main.find(result))
            result = result.replaceRange(entry.range, "${entry.value}\n$VARYING = $ATTRIBUTE;\n")
        }

        return result
    }

    private fun matrix(name: String, type: String, member: String) =
        Parameter(name, type, listOf("$type gtladd_foa_$name() { return $type($RING.$member); }"))

    private fun modulator(name: String) = Parameter(
        name,
        "vec4",
        listOf("uniform vec4 $name;", "vec4 gtladd_foa_$name() { return $name * vec4(vec3(gtladd_foa_Shade()), 1.0); }")
    )

    /** One top-level declaration per entry, which is how the syntax tree takes them. */
    private fun declarations(capacity: Int, vertex: Boolean, replaced: List<Parameter>): List<String> {
        val input = if (vertex) ATTRIBUTE else VARYING
        return buildList {
            add("struct GtladdFoaRing { mat4 modelView; mat4 inverseModelView; mat4 normalMatrix; vec4 shadesLow; vec4 shadesHigh; };")
            add("layout(std140) uniform $BLOCK { GtladdFoaRing gtladd_foa_Rings[$capacity]; };")
            if (vertex) {
                add("in vec2 $ATTRIBUTE;")
                add("flat out vec2 $VARYING;")
            } else {
                add("flat in vec2 $VARYING;")
            }

            add("int gtladd_foa_RingIndex() { return int($input.x); }")
            add(
                "float gtladd_foa_Shade() { int group = int($input.y); return group < 4 ? $RING.shadesLow[group] : $RING.shadesHigh[group - 4]; }"
            )
            replaced.forEach { addAll(it.definitions) }
        }
    }

    /** Keep #version, extension directives and vanilla #moj_import headers ahead of declarations. */
    private fun headerEnd(source: String): Int {
        var index = 0

        while (index < source.length) {
            when {
                source[index].isWhitespace() -> index++
                source.startsWith("//", index) || source[index] == '#' -> {
                    index = source.indexOf('\n', index).let { if (it < 0) source.length else it + 1 }
                }
                source.startsWith("/*", index) -> {
                    index = source.indexOf("*/", index + 2).also { require(it >= 0) } + 2
                }
                else -> return index
            }
        }

        return index
    }
}
