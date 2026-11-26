package com.gtladd.gtladditions.integration.xei.structureupgrade

import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory
import com.lowdragmc.lowdraglib.jei.ModularWrapper
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.helpers.IJeiHelpers
import mezz.jei.api.recipe.RecipeType
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.registration.IRecipeRegistration
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

class StructureUpgradeJeiCategory private constructor(helpers: IJeiHelpers, private val layout: StructureUpgradeWidget.Layout) : ModularUIRecipeCategory<StructureUpgradeJeiCategory.Wrapper>() {

    private val icon = helpers.guiHelper.drawableBuilder(StructureUpgradeEntries.ICON, 0, 0, 16, 16).setTextureSize(16, 16).build()

    override fun getRecipeType(): RecipeType<Wrapper> = RECIPE_TYPE

    override fun getTitle(): Component = "gtladditions.structure_tier.title".toComponent

    override fun getIcon(): IDrawable = icon

    override fun getWidth(): Int = StructureUpgradeWidget.WIDTH

    override fun getHeight(): Int = layout.height

    override fun getRegistryName(recipe: Wrapper): ResourceLocation = recipe.entry.id

    class Wrapper(val entry: StructureUpgradeEntry, layout: StructureUpgradeWidget.Layout) : ModularWrapper<WidgetGroup>(StructureUpgradeWidget(entry, layout))

    companion object {
        private val RECIPE_TYPE = RecipeType(StructureUpgradeEntries.ID, Wrapper::class.java)

        fun registerCategories(registration: IRecipeCategoryRegistration) {
            val layout = StructureUpgradeWidget.Layout.of(StructureUpgradeEntries.entries())
            registration.addRecipeCategories(StructureUpgradeJeiCategory(registration.jeiHelpers, layout))
        }

        fun registerRecipes(registration: IRecipeRegistration) {
            val entries = StructureUpgradeEntries.entries()
            val layout = StructureUpgradeWidget.Layout.of(entries)
            registration.addRecipes(RECIPE_TYPE, entries.map { Wrapper(it, layout) })
        }
    }
}
