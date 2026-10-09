package com.gtladd.gtladditions.integration.xei

import appeng.client.gui.AEBaseScreen
import mezz.jei.api.constants.VanillaTypes
import mezz.jei.api.forge.ForgeTypes
import mezz.jei.api.gui.handlers.IGhostIngredientHandler
import mezz.jei.api.ingredients.ITypedIngredient
import net.minecraft.client.renderer.Rect2i
import net.minecraft.world.item.ItemStack
import net.minecraftforge.fluids.FluidStack

object TerminalSearchBoxJeiHandler : IGhostIngredientHandler<AEBaseScreen<*>> {
    override fun <I : Any> getTargetsTyped(
        gui: AEBaseScreen<*>,
        ingredient: ITypedIngredient<I>,
        doStart: Boolean
    ): List<IGhostIngredientHandler.Target<I>> {
        val name = ingredientName(ingredient) ?: return emptyList()

        return SearchBoxDrop.getTextFieldAreas(gui).map { field ->
            object : IGhostIngredientHandler.Target<I> {
                override fun getArea(): Rect2i = SearchBoxDrop.getArea(field)

                override fun accept(ingredient: I) {
                    SearchBoxDrop.fillTextField(gui, field, name)
                }
            }
        }
    }

    override fun onComplete() = Unit

    private fun ingredientName(ingredient: ITypedIngredient<*>): String? {
        ingredient.getIngredient(VanillaTypes.ITEM_STACK).orElse(null)?.takeUnless { it.isEmpty }?.let {
            return it.hoverName.string
        }

        ingredient.getIngredient(ForgeTypes.FLUID_STACK).orElse(null)?.takeUnless { it.isEmpty }?.let {
            return it.displayName.string
        }

        return when (val value = ingredient.ingredient) {
            is ItemStack -> value.takeUnless { it.isEmpty }?.hoverName?.string
            is FluidStack -> value.takeUnless { it.isEmpty }?.displayName?.string
            else -> null
        }
    }
}