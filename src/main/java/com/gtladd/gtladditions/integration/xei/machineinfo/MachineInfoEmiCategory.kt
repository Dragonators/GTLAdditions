package com.gtladd.gtladditions.integration.xei.machineinfo

import com.lowdragmc.lowdraglib.emi.ModularEmiRecipe
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import dev.emi.emi.api.EmiRegistry
import dev.emi.emi.api.recipe.EmiRecipeCategory
import dev.emi.emi.api.stack.EmiStack
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

class MachineInfoEmiCategory private constructor(
    private val page: MachineInfoPage
) : EmiRecipeCategory(page.id, EmiStack.of(page.machine.asStack())) {

    override fun getName(): Component = Component.translatable(page.machine.descriptionId)

    private class Recipe(private val category: MachineInfoEmiCategory) : ModularEmiRecipe<WidgetGroup>({ category.page.createWidget() }) {

        override fun getCategory(): EmiRecipeCategory = category

        override fun getId(): ResourceLocation = category.page.id.withPrefix("/")
    }

    companion object {
        fun register(registry: EmiRegistry) {
            for (page in MachineInfoPages.pages()) {
                val category = MachineInfoEmiCategory(page)
                registry.addCategory(category)
                registry.addWorkstation(category, EmiStack.of(page.machine.asStack()))
                registry.addRecipe(Recipe(category))
            }
        }
    }
}