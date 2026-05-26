package com.gtladd.gtladditions.api.gui

import appeng.client.gui.me.common.StackSizeRenderer
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient
import com.gregtechceu.gtceu.client.TooltipsHandler
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper
import com.lowdragmc.lowdraglib.side.fluid.FluidHelper
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import com.lowdragmc.lowdraglib.utils.LocalizationUtils
import guideme.document.LytRect
import guideme.document.block.LytBlock
import guideme.document.block.LytSlotGrid
import guideme.document.interaction.GuideTooltip
import guideme.document.interaction.InteractiveElement
import guideme.document.interaction.ItemTooltip
import guideme.internal.GuideME
import guideme.layout.LayoutContext
import guideme.render.GuiAssets
import guideme.render.GuiSprite
import guideme.render.RenderContext
import guideme.siteexport.ResourceExporter
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient
import org.gtlcore.gtlcore.utils.NumberUtils
import java.util.Optional
import java.util.concurrent.TimeUnit
import kotlin.math.min

class GTLytSlotGrid(private val width: Int, private val itemHeight: Int, fluidHeight: Int) : LytSlotGrid(0, 0) {
    private val height: Int = itemHeight + fluidHeight
    private val slots: Array<GTLytSlot?> = arrayOfNulls(width * height)

    private fun addSlot(x: Int, y: Int, content: Content) {
        if (x !in 0..width) throw IndexOutOfBoundsException("x: $x")
        if (y !in 0..height) throw IndexOutOfBoundsException("y: $y")
        val index = getSlotIndex(x, y)
        slots[index]?.let {
            it.removeChild(it)
            slots[index] = null
        }
        slots[index] = GTLytSlot(content)
        append(slots[index])
    }

    override fun computeBoxLayout(context: LayoutContext, x: Int, y: Int, availableWidth: Int): LytRect {
        for (row in 0..<height) {
            for (col in 0..<width) {
                val index = getSlotIndex(col, row)
                if (index < slots.size) {
                    slots[index]?.layout(context, x + col * 18, y + row * 18, availableWidth)
                }
            }
        }
        return LytRect(x, y, 18 * width, 18 * height)
    }

    override fun render(context: RenderContext) {
        for (y in 0..<height) {
            for (x in 0..<width) {
                val index = getSlotIndex(x, y)
                if (slots[index] == null) {
                    val background = if (y < itemHeight) GuiAssets.SLOT_BACKGROUND else FLUID_SLOT
                    context.drawIcon(bounds.x() + 18 * x, bounds.y() + 18 * y, background)
                }
            }
        }
        super.render(context)
    }

    private fun getSlotIndex(col: Int, row: Int) = row * width + col

    class Builder(private val recipe: GTRecipe) {
        val recipeInput: GTLytSlotGrid
            get() {
                var width = 0
                var itemHeight = 0
                var fluidHeight = 0
                for ((capability, size) in recipe.recipeType.maxInputs) {
                    if (capability.doRenderSlot) {
                        if (size > width) width = min(size, 3)
                        if (capability === ItemRecipeCapability.CAP) {
                            itemHeight = (size + 2) / 3
                        } else if (capability === FluidRecipeCapability.CAP) {
                            fluidHeight = (size + 2) / 3
                        }
                    }
                }

                var itemIndex = 0
                var fluidIndex = 0
                val slot = GTLytSlotGrid(width, itemHeight, fluidHeight)
                for ((capability, contents) in recipe.inputs) {
                    if (capability === ItemRecipeCapability.CAP) {
                        for (content in contents) {
                            slot.addSlot(itemIndex % 3, itemIndex / 3, content)
                            itemIndex++
                        }
                    } else if (capability === FluidRecipeCapability.CAP) {
                        for (content in contents) {
                            slot.addSlot(fluidIndex % 3, fluidIndex / 3 + itemHeight, content)
                            fluidIndex++
                        }
                    }
                }
                return slot
            }

        val recipeOutput: GTLytSlotGrid
            get() {
                var width = 0
                var itemHeight = 0
                var fluidHeight = 0
                for ((capability, size) in recipe.recipeType.maxOutputs) {
                    if (capability.doRenderSlot) {
                        if (size > width) width = min(size, 3)
                        if (capability === ItemRecipeCapability.CAP) {
                            itemHeight = (size + 2) / 3
                        } else if (capability === FluidRecipeCapability.CAP) {
                            fluidHeight = (size + 2) / 3
                        }
                    }
                }

                var itemIndex = 0
                var fluidIndex = 0
                val slot = GTLytSlotGrid(width, itemHeight, fluidHeight)
                for ((capability, contents) in recipe.outputs) {
                    if (capability === ItemRecipeCapability.CAP) {
                        for (content in contents) {
                            slot.addSlot(itemIndex % 3, itemIndex / 3, content)
                            itemIndex++
                        }
                    } else if (capability === FluidRecipeCapability.CAP) {
                        for (content in contents) {
                            slot.addSlot(fluidIndex % 3, fluidIndex / 3 + itemHeight, content)
                            fluidIndex++
                        }
                    }
                }
                return slot
            }
    }

    private class GTLytSlot(private val content: Content) :
        LytBlock(),
        InteractiveElement {
        override fun computeLayout(layoutContext: LayoutContext, x: Int, y: Int, availableWidth: Int) = LytRect(x, y, 18, 18)

        override fun onLayoutMoved(x: Int, y: Int) = Unit

        override fun renderBatch(renderContext: RenderContext, multiBufferSource: MultiBufferSource) = Unit

        override fun render(context: RenderContext) {
            val graphics = context.guiGraphics()
            val x = bounds.x()
            val y = bounds.y()
            val width = bounds.width()
            val height = bounds.height()
            val font = Minecraft.getInstance().font

            when (val ingredient = content.content) {
                is Ingredient -> {
                    val item = getDisplayedItemStack(ingredient.items)
                    context.fillIcon(bounds, GuiAssets.SLOT)
                    if (!item.isEmpty) {
                        val pose = context.poseStack()
                        pose.pushPose()
                        pose.translate(x + 1f, y + 1f, 2f)
                        graphics.renderItem(item, 0, 0)
                        pose.popPose()
                        if (ingredient.guideAmount > 1) {
                            StackSizeRenderer.renderSizeLabel(graphics, font, x + 1f, y + 1f, ingredient.guideAmount.toString(), true)
                        }
                    }
                }
                is FluidIngredient -> {
                    val fluid = getDisplayedFluidStack(ingredient.getStacks())
                    context.fillIcon(bounds, FLUID_SLOT)
                    if (!fluid.isEmpty) {
                        DrawerHelper.drawFluidForGui(graphics, fluid, ingredient.amount, x + 1, y + 1, width - 2, height - 2)
                        graphics.pose().pushPose()
                        graphics.pose().translate(0f, 0f, 400f)
                        graphics.pose().scale(.5f, .5f, 1f)
                        var amount = fluid.amount
                        val amountText = if (amount >= 1000) {
                            amount /= 1000
                            NumberUtils.formatLong(amount) + "B"
                        } else {
                            "${amount}mB"
                        }
                        graphics.drawString(font, amountText, ((x + (width / 3f)) * 2 - font.width(amountText) + 21).toInt(), ((y + (height / 3f) + 6) * 2).toInt(), 0xFFFFFF, true)
                        graphics.pose().popPose()
                    }
                }
            }

            if (content.chance != 10000) {
                graphics.pose().pushPose()
                graphics.pose().translate(.0f, .0f, 400f)
                graphics.pose().scale(.5f, .5f, 1f)
                val chance = 100f * content.chance.toFloat() / content.maxChance.toFloat()
                val percent = FormattingUtil.formatPercent(chance.toDouble())
                val chanceText = if (chance == 0f) LocalizationUtils.format("gtceu.gui.content.chance_0_short") else "$percent%"
                val xDraw = ((x + width / 3f) * 2f - font.width(chanceText) + 23f).toInt() - if (chance == 0f) 10 else 0
                val yDraw = ((y + height / 3f + 6f) * 2.0f - height).toInt() - if (chance == 0f) 3 else 0
                graphics.drawString(font, chanceText, xDraw, yDraw, if (chance == 0f) 0xFF0000 else 0xFFFF00, true)
                graphics.pose().popPose()
            }
        }

        override fun getTooltip(x: Float, y: Float): Optional<GuideTooltip> = when (val ingredient = content.content) {
            is Ingredient -> {
                val stack = ingredient.items.firstOrNull() ?: ItemStack.EMPTY
                if (stack.isEmpty) Optional.empty() else Optional.of(ItemTooltip(stack))
            }
            is FluidIngredient -> {
                val stack = ingredient.firstStack
                if (stack.isEmpty) Optional.empty() else Optional.of(FluidTooltip(stack))
            }
            else -> Optional.empty()
        }

        private fun getDisplayedItemStack(stacks: Array<ItemStack>) = if (stacks.isEmpty()) {
            ItemStack.EMPTY
        } else {
            stacks[(System.nanoTime() / TimeUnit.MILLISECONDS.toNanos(2000L) % stacks.size.toLong()).toInt()]
        }

        private fun getDisplayedFluidStack(stacks: Array<FluidStack>) = if (stacks.isEmpty()) {
            FluidStack.empty()
        } else {
            stacks[(System.nanoTime() / TimeUnit.MILLISECONDS.toNanos(2000L) % stacks.size.toLong()).toInt()]
        }
    }

    data class FluidTooltip(val fluidStack: FluidStack) : GuideTooltip {
        override fun getLines(): MutableList<ClientTooltipComponent> {
            val list = ObjectArrayList<Component>()
            list.add(FluidHelper.getDisplayName(fluidStack))
            TooltipsHandler.appendFluidTooltips(fluidStack.fluid, fluidStack.amount, list::add, null)
            return list.stream().map { it.visualOrderText }.map { ClientTooltipComponent.create(it) }.toList()
        }

        override fun getIcon(): ItemStack = fluidStack.fluid.bucket.defaultInstance

        override fun exportResources(resourceExporter: ResourceExporter) = resourceExporter.referenceFluid(fluidStack.fluid)
    }

    companion object {
        private val FLUID_SLOT: GuiSprite = GuiAssets.sprite(GuideME.makeId("fluid_slot"))

        @JvmStatic
        fun builder(recipe: GTRecipe): Builder = Builder(recipe)
    }
}

private val Ingredient.guideAmount: Long
    get() = when (this) {
        is LongIngredient -> actualAmount
        is SizedIngredient -> amount.toLong()
        else -> 1L
    }

private val FluidIngredient.firstStack: FluidStack
    get() {
        getStacks().forEach { if (it != null && !it.isEmpty) return it }
        return FluidStack.empty()
    }