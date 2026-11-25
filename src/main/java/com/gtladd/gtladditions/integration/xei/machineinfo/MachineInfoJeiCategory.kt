package com.gtladd.gtladditions.integration.xei.machineinfo

import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory
import com.lowdragmc.lowdraglib.jei.ModularWrapper
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.helpers.IJeiHelpers
import mezz.jei.api.recipe.RecipeType
import mezz.jei.api.registration.IRecipeCatalystRegistration
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.registration.IRecipeRegistration
import net.minecraft.network.chat.Component

class MachineInfoJeiCategory private constructor(
    private val page: MachineInfoPage,
    helpers: IJeiHelpers
) : ModularUIRecipeCategory<MachineInfoJeiCategory.Wrapper>() {

    private val recipeType = typeOf(page)
    private val size = page.createWidget().size
    private val icon = helpers.guiHelper.createDrawableItemStack(page.machine.asStack())

    override fun getRecipeType(): RecipeType<Wrapper> = recipeType

    override fun getTitle(): Component = Component.translatable(page.machine.descriptionId)

    override fun getIcon(): IDrawable = icon

    override fun getWidth(): Int = size.width

    override fun getHeight(): Int = size.height

    class Wrapper(page: MachineInfoPage) : ModularWrapper<WidgetGroup>(page.createWidget())

    companion object {
        private fun typeOf(page: MachineInfoPage) = RecipeType(page.id, Wrapper::class.java)

        fun registerCategories(registration: IRecipeCategoryRegistration) {
            for (page in MachineInfoPages.pages()) {
                registration.addRecipeCategories(MachineInfoJeiCategory(page, registration.jeiHelpers))
            }
        }

        fun registerRecipes(registration: IRecipeRegistration) {
            for (page in MachineInfoPages.pages()) {
                registration.addRecipes(typeOf(page), listOf(Wrapper(page)))
            }
        }

        fun registerRecipeCatalysts(registration: IRecipeCatalystRegistration) {
            for (page in MachineInfoPages.pages()) {
                registration.addRecipeCatalyst(page.machine.asStack(), typeOf(page))
            }
        }
    }
}
