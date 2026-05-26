package com.gtladd.gtladditions.api.guide

import com.gregtechceu.gtceu.client.TooltipsHandler
import com.lowdragmc.lowdraglib.side.fluid.FluidHelper
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import guideme.compiler.PageCompiler
import guideme.compiler.tags.FlowTagCompiler
import guideme.document.flow.LytFlowParent
import guideme.document.flow.LytTooltipSpan
import guideme.document.interaction.GuideTooltip
import guideme.libs.mdast.mdx.model.MdxJsxElementFields
import guideme.siteexport.ResourceExporter
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.utils.Registries

class FluidLinkCompiler : FlowTagCompiler() {
    override fun getTagNames() = mutableSetOf("FluidLink")

    override fun compile(compiler: PageCompiler, parent: LytFlowParent, el: MdxJsxElementFields) {
        val id = el.getAttributeString("id", "")
        if (id.isEmpty()) {
            parent.appendError(compiler, "No fluid id provided for FluidLink", el)
            return
        }

        val fluidStack = FluidStack.create(Registries.getFluid(id), 1000)
        val span = LytTooltipSpan()
        span.modifyStyle { it.bold(true) }
        span.appendComponent(FluidHelper.getDisplayName(fluidStack))
        span.setTooltip(FluidTooltip(fluidStack))
        parent.append(span)
    }

    private data class FluidTooltip(private val fluidStack: FluidStack) : GuideTooltip {
        override fun getLines(): MutableList<ClientTooltipComponent> {
            val list = mutableListOf<Component>()
            list.add(FluidHelper.getDisplayName(fluidStack))
            TooltipsHandler.appendFluidTooltips(fluidStack.fluid, fluidStack.amount, list::add, null)
            return list.map { ClientTooltipComponent.create(it.visualOrderText) }.toMutableList()
        }

        override fun getIcon(): ItemStack = fluidStack.fluid.bucket.defaultInstance

        override fun exportResources(resourceExporter: ResourceExporter) {
            resourceExporter.referenceFluid(fluidStack.fluid)
        }
    }
}