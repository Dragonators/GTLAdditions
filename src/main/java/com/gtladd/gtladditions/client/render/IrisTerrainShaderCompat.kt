package com.gtladd.gtladditions.client.render

import io.github.douira.glsl_transformer.ast.node.expression.Expression
import io.github.douira.glsl_transformer.ast.print.ASTPrinter
import io.github.douira.glsl_transformer.ast.query.match.AutoHintedMatcher
import io.github.douira.glsl_transformer.ast.transform.ASTInjectionPoint
import io.github.douira.glsl_transformer.ast.transform.ASTTransformer
import io.github.douira.glsl_transformer.parser.ParseShape

/** The classifier is Complementary/Euphoria's automatic cross-model foliage detection. */
object IrisTerrainShaderCompat {
    private const val MODEL_NORMAL = "gtladditions_ModelGeoNormal"
    private val crossModel = AutoHintedMatcher(classifier("worldGeoNormal"), ParseShape.EXPRESSION)

    data class Sources(val vertex: String, val fragment: String)

    private fun classifier(normal: String) = "length(abs($normal.xz) - vec2(sqrt(0.5))) < 0.01"

    /**
     * Iris calls this after its source transformation, with the transformer that printed it. A rotating cube passes the
     * shaderpack's world-space 45-degree test four times per revolution, so only that classification uses the original
     * model normal; lighting keeps the transformed normal. Both stages are returned together so a source never gets
     * half a varying interface.
     */
    fun patch(transformer: ASTTransformer<*, *>, vertex: String?, fragment: String?): Sources? {
        if (vertex == null || fragment == null || MODEL_NORMAL in vertex || MODEL_NORMAL in fragment) return null
        // Only the classifier's own programs carry this text.
        if ("worldGeoNormal.xz" !in fragment || "subsurfaceMode" !in fragment || "noDirectionalShading" !in fragment) return null
        val vertexTree = transformer.parseSeparateTranslationUnit(vertex)
        if (!vertexTree.root.identifierIndex.has("iris_Normal")) return null
        val fragmentTree = transformer.parseSeparateTranslationUnit(fragment)
        val classifiers = ArrayList<Expression>()
        fragmentTree.root.processMatches(transformer, crossModel) { classifiers.add(it) }
        if (classifiers.size != 1) return null
        fragmentTree.root.replaceExpressions(transformer, classifiers.stream(), classifier(MODEL_NORMAL))
        fragmentTree.parseAndInjectNode(transformer, ASTInjectionPoint.BEFORE_DECLARATIONS, "flat in vec3 $MODEL_NORMAL;")
        vertexTree.parseAndInjectNode(transformer, ASTInjectionPoint.BEFORE_DECLARATIONS, "flat out vec3 $MODEL_NORMAL;")
        vertexTree.prependMainFunctionBody(
            transformer,
            "$MODEL_NORMAL = dot(iris_Normal, iris_Normal) > 1e-12 ? normalize(iris_Normal) : vec3(0.0);"
        )

        return Sources(ASTPrinter.print(transformer.printType, vertexTree), ASTPrinter.print(transformer.printType, fragmentTree))
    }
}
