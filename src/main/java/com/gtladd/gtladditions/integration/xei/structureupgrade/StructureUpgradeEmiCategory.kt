package com.gtladd.gtladditions.integration.xei.structureupgrade

import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.lowdragmc.lowdraglib.emi.ModularEmiRecipe
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import dev.emi.emi.api.EmiRegistry
import dev.emi.emi.api.recipe.EmiRecipeCategory
import dev.emi.emi.api.render.EmiTexture
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

class StructureUpgradeEmiCategory private constructor() : EmiRecipeCategory(StructureUpgradeEntries.ID, EmiTexture(StructureUpgradeEntries.ICON, 0, 0, 16, 16, 16, 16, 16, 16)) {

    override fun getName(): Component = "gtladditions.structure_tier.title".toComponent

    class Recipe(
        private val category: StructureUpgradeEmiCategory,
        val entry: StructureUpgradeEntry,
        layout: StructureUpgradeWidget.Layout
    ) : ModularEmiRecipe<WidgetGroup>({ StructureUpgradeWidget(entry, layout) }) {

        override fun getCategory(): EmiRecipeCategory = category

        override fun getId(): ResourceLocation = entry.id.withPrefix("/")
    }

    companion object {
        fun register(registry: EmiRegistry) {
            val category = StructureUpgradeEmiCategory()
            val entries = StructureUpgradeEntries.entries()
            val layout = StructureUpgradeWidget.Layout.of(entries)
            registry.addCategory(category)
            for (entry in entries) {
                registry.addRecipe(Recipe(category, entry, layout))
            }
        }
    }
}
