package com.gtladd.gtladditions.api.guide

import guideme.compiler.PageCompiler
import guideme.compiler.tags.BlockTagCompiler
import guideme.compiler.tags.MdxAttrs
import guideme.document.block.LytBlockContainer
import guideme.libs.mdast.mdx.model.MdxJsxElementFields
import guideme.libs.mdast.model.MdAstNode

class LatexCompiler : BlockTagCompiler() {

    override fun getTagNames() = mutableSetOf("Latex")

    override fun compile(compiler: PageCompiler, parent: LytBlockContainer, el: MdxJsxElementFields) {
        val string = el.getAttributeString("math", "")
        if (string.isEmpty()) {
            parent.appendError(compiler, "Latex tag requires 'math' attribute", el)
            return
        }
        val size = MdxAttrs.getFloat(compiler, parent, el, "size", LytLatex.DEFAULT_FONT_SIZE)
        val latex = LytLatex(string, size)
        latex.sourceNode = el as MdAstNode
        parent.append(latex)
    }
}