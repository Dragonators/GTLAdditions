package com.gtladd.gtladditions.api.guide

import guideme.PageAnchor
import guideme.color.SymbolicColor
import guideme.compiler.PageCompiler
import guideme.compiler.tags.FlowTagCompiler
import guideme.compiler.tags.MdxAttrs
import guideme.document.flow.LytFlowLink
import guideme.document.flow.LytFlowParent
import guideme.document.flow.LytTooltipSpan
import guideme.document.interaction.ItemTooltip
import guideme.indices.ItemIndex
import guideme.libs.mdast.mdx.model.MdxJsxElementFields
import net.minecraft.ChatFormatting
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

class CleanItemLinkCompiler : FlowTagCompiler() {
    override fun getTagNames() = setOf("ItemLink")

    override fun compile(compiler: PageCompiler, parent: LytFlowParent, el: MdxJsxElementFields) {
        val itemStackAndId = MdxAttrs.getRequiredItemStackAndId(compiler, parent, el) ?: return
        val itemId = itemStackAndId.left
        val itemStack = itemStackAndId.right
        val linkEnabled = MdxAttrs.getBoolean(compiler, parent, el, "link", true)
        val linkColor = MdxAttrs.getBoolean(compiler, parent, el, "linkColor", false)
        val anchor = compiler.getIndex(ItemIndex::class.java)[itemId]

        if (anchor == null && itemId.namespace == compiler.pageId.namespace) {
            parent.append(compiler.createErrorFlowContent("Item $itemId is not indexed by any page", el))
            return
        }

        if (!linkEnabled || anchor == null || anchor.isCurrentPage(compiler.pageId)) {
            val span = LytTooltipSpan()
            span.modifyStyle {
                it.italic(false)
                if (linkColor) {
                    it.color(SymbolicColor.LINK)
                }
            }
            span.appendText(itemStack.cleanHoverName())
            span.setTooltip(ItemTooltip(itemStack))
            parent.append(span)
        } else {
            val link = LytFlowLink()
            link.setPageLink(anchor)
            link.appendText(itemStack.cleanHoverName())
            link.setTooltip(ItemTooltip(itemStack))
            parent.append(link)
        }
    }

    private fun PageAnchor.isCurrentPage(pageId: ResourceLocation) = anchor() == null && pageId == pageId()

    private fun ItemStack.cleanHoverName(): String {
        val hoverName = hoverName.string
        return ChatFormatting.stripFormatting(hoverName) ?: hoverName
    }
}