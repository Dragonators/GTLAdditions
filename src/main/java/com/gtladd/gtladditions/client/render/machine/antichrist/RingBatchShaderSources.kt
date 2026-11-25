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
    private val conditionalOpen = Regex("""#\s*if(?:n?def)?\b""")
    private val conditionalBranch = Regex("""#\s*(?:else|elif)\b""")
    private val conditionalClose = Regex("""#\s*endif\b""")
    private val conditionalDirective = Regex("""#\s*(?:if(?:n?def)?|elif)\b""")
    private val headerOnlyDirective = Regex("""#\s*(?:version|extension)\b""")
    private val defineHeader = Regex("""#\s*define\s+[A-Za-z_]\w*(?:\([^)]*\))?""")
    private val lineContinuation = Regex("""\\\r?\n""")
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
        val maskedVertex = maskComments(vertex)
        val maskedFragment = maskComments(fragment)
        require(main.findAll(maskedVertex).count() == 1 && main.findAll(maskedFragment).count() == 1)
        require(!drawLocalIds.containsMatchIn(maskedVertex) && !drawLocalIds.containsMatchIn(maskedFragment)) { DRAW_LOCAL_IDS }
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
            val beforeMasked = maskComments(result)
            val splicedBefore = beforeMasked.replace(lineContinuation, "")
            val declarations = uniform.findAll(beforeMasked).toList()
            // Reject protected uniforms split across GLSL §3.1 line continuations before skipping absent parameters.
            require(declarations.size == uniform.findAll(splicedBefore).count())
            if (declarations.isEmpty()) {
                val word = Regex("""\b${Regex.escape(parameter.name)}\b""")
                require(word.findAll(splicedBefore).count() == word.findAll(beforeMasked).count())
                return@filter false
            }
            result = replaceMatches(result, declarations, "")
            val masked = maskComments(result)
            require(!hasNameCollision(result, masked, parameter.name))
            result = replaceMatches(result, Regex("""\b${parameter.name}\b""").findAll(masked).toList(), parameter.call)
            true
        }

        if (vertex) require(replaced.any { it.name.endsWith("ModelViewMat") }) { NO_MODEL_VIEW }
        val maskedResult = maskComments(result)
        val header = headerEnd(result, maskedResult)
        require(headerOnlyDirective.find(maskedResult.substring(header).replace(lineContinuation, "")) == null)
        result = result.substring(0, header) +
            declarations(capacity, vertex, replaced).joinToString("\n", "\n", "\n\n") +
            result.substring(header)
        if (vertex) {
            val entry = checkNotNull(main.find(maskComments(result)))
            result = result.replaceRange(entry.range, "${result.substring(entry.range)}\n$VARYING = $ATTRIBUTE;\n")
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

    private fun replaceMatches(source: String, matches: List<MatchResult>, replacement: String): String {
        if (matches.isEmpty()) return source
        val builder = StringBuilder(source.length)
        var cursor = 0

        for (match in matches) {
            builder.append(source, cursor, match.range.first)
            builder.append(replacement)
            cursor = match.range.last + 1
        }

        builder.append(source, cursor, source.length)
        return builder.toString()
    }

    /** Reject shaders where a replaced uniform name is redeclared, redefined, mutated, called, or accessed as a member. */
    private fun hasNameCollision(source: String, masked: String, name: String): Boolean {
        val escaped = Regex.escape(name)
        val word = Regex("""\b$escaped\b""")
        val spliced = masked.replace(lineContinuation, "")
        val splicedCount = word.findAll(spliced).count()
        if (splicedCount == 0) return false
        // Reject identifiers split across GLSL §3.1 line continuations (e.g. Model\<newline>ViewMat) that regex rewrite would miss.
        if (splicedCount != word.findAll(masked).count()) return true
        val macroCollision = Regex(
            """#\s*(?:define|undef)\s+$escaped\b|#\s*define\s+[A-Za-z_]\w*\([^)]*\b$escaped\b""" +
                """|\bdefined\s*\(\s*$escaped\s*\)|\bdefined\s+$escaped\b"""
        )
        val codeOnly = StringBuilder(masked.length)
        val macroBodies = StringBuilder()
        var index = 0

        while (index < masked.length) {
            if (masked[index] == '#') {
                val end = directiveEnd(source, index)
                // Splice GLSL §3.1 line continuations before directive matching without altering original replacement offsets.
                val directive = masked.substring(index, end).replace(lineContinuation, "")
                if (macroCollision.containsMatchIn(directive)) return true
                if (conditionalDirective.matchAt(directive, 0) != null && word.containsMatchIn(directive)) return true
                defineHeader.matchAt(directive, 0)?.let { header ->
                    macroBodies.append("; ")
                    macroBodies.append(directive.substring(header.range.last + 1))
                    macroBodies.append(" ;\n")
                }
                if (conditionalBranch.matchAt(directive, 0) != null) {
                    codeOnly.append(";\n")
                } else {
                    codeOnly.append('\n')
                }
                index = end
            } else {
                codeOnly.append(masked[index++])
            }
        }

        val combined = (if (macroBodies.isEmpty()) codeOnly.toString() else "$codeOnly\n;\n$macroBodies")
            .replace(lineContinuation, "")
        val declarationCollision = Regex(
            """\b(?!return\b|else\b|do\b|case\b)[A-Za-z_]\w*(?:\s*\[[^\]]*\])*\s+$escaped\b"""
        )
        val accessOrMutation = Regex(
            """(?:\.|(?:\+\+|--))\s*$escaped\b|\b$escaped\s*\(""" +
                """|\b$escaped(?:\s*(?:\.\s*[A-Za-z_]\w*|\[[^\]]*\]))*\s*(?:[+\-*/%&|^]?=(?!=)|<<=|>>=|\+\+|--)"""
        )
        return declarationCollision.containsMatchIn(combined) ||
            accessOrMutation.containsMatchIn(combined) ||
            hasCommaDeclarator(combined, name)
    }

    private fun hasCommaDeclarator(code: String, name: String): Boolean {
        val commaName = Regex(""",\s*${Regex.escape(name)}(?:\s*\[[^\]]*\])*\s*(?:[,;]|=(?!=))""")
        var parenDepth = 0
        var forInitDepth = -1
        var index = 0

        while (index < code.length) {
            when (code[index]) {
                '(' -> {
                    parenDepth++
                    if (forInitDepth < 0 && precededByFor(code, index)) forInitDepth = parenDepth
                }
                ')' -> {
                    if (parenDepth == forInitDepth) forInitDepth = -1
                    if (parenDepth > 0) parenDepth--
                }
                ';' -> if (parenDepth == forInitDepth) forInitDepth = -1
                ',' -> if ((parenDepth == 0 || parenDepth == forInitDepth) && commaName.matchAt(code, index) != null) {
                    return true
                }
            }

            index++
        }

        return false
    }

    private fun precededByFor(code: String, openParenIndex: Int): Boolean {
        var end = openParenIndex
        while (end > 0 && code[end - 1].isWhitespace()) end--
        return end >= 3 &&
            code.regionMatches(end - 3, "for", 0, 3) &&
            (end == 3 || !(code[end - 4].isLetterOrDigit() || code[end - 4] == '_'))
    }

    /** Mask comment contents with spaces while keeping line breaks so regexes match only code at identical offsets. */
    private fun maskComments(source: String): String {
        val chars = source.toCharArray()
        var index = 0

        while (index < chars.size) {
            when {
                chars[index] == '/' && index + 1 < chars.size && chars[index + 1] == '/' -> {
                    val end = lineEnd(source, index)
                    while (index < end) {
                        if (chars[index] != '\n' && chars[index] != '\r') chars[index] = ' '
                        index++
                    }
                }
                chars[index] == '/' && index + 1 < chars.size && chars[index + 1] == '*' -> {
                    chars[index++] = ' '
                    chars[index++] = ' '
                    while (index < chars.size && !(chars[index] == '*' && index + 1 < chars.size && chars[index + 1] == '/')) {
                        if (chars[index] != '\n' && chars[index] != '\r') chars[index] = ' '
                        index++
                    }

                    require(index + 1 < chars.size)
                    chars[index++] = ' '
                    chars[index++] = ' '
                }
                else -> index++
            }
        }

        return String(chars)
    }

    /** Keep leading preprocessor headers and balanced directive-only #if blocks ahead of injected declarations. */
    private fun headerEnd(source: String, masked: String): Int {
        var index = 0
        var depth = 0
        var safeEnd = 0

        while (index < source.length) {
            when {
                source[index].isWhitespace() -> {
                    index++
                    if (depth == 0) safeEnd = index
                }
                source.startsWith("//", index) -> {
                    index = lineEnd(source, index)
                    if (depth == 0) safeEnd = index
                }
                source.startsWith("/*", index) -> {
                    index = source.indexOf("*/", index + 2).also { require(it >= 0) } + 2
                    if (depth == 0) safeEnd = index
                }
                source[index] == '#' -> {
                    val end = directiveEnd(source, index)
                    val directive = masked.substring(index, end).replace(lineContinuation, "")
                    when {
                        conditionalOpen.matchAt(directive, 0) != null -> depth++
                        conditionalBranch.matchAt(directive, 0) != null -> require(depth > 0)
                        conditionalClose.matchAt(directive, 0) != null -> {
                            require(depth > 0)
                            depth--
                        }
                    }
                    index = end
                    if (depth == 0) safeEnd = index
                }
                else -> return safeEnd
            }
        }

        require(depth == 0)
        return safeEnd
    }

    private fun directiveEnd(source: String, start: Int): Int {
        var index = start

        while (index < source.length) {
            when {
                source.startsWith("//", index) -> return lineEnd(source, index)
                source.startsWith("/*", index) -> {
                    index = source.indexOf("*/", index + 2).also { require(it >= 0) } + 2
                }
                source[index] == '\\' && index + 1 < source.length && source[index + 1] == '\n' -> index += 2
                source[index] == '\\' && index + 2 < source.length && source[index + 1] == '\r' && source[index + 2] == '\n' -> {
                    index += 3
                }
                source[index] == '\n' -> return index + 1
                else -> index++
            }
        }

        return source.length
    }

    private fun lineEnd(source: String, start: Int): Int {
        var index = start

        while (index < source.length) {
            val lineStart = index
            val newline = source.indexOf('\n', index)
            if (newline < 0) return source.length
            val contentEnd = if (newline > lineStart && source[newline - 1] == '\r') newline - 1 else newline
            index = newline + 1
            if (contentEnd <= lineStart || source[contentEnd - 1] != '\\') return index
        }

        return source.length
    }
}
