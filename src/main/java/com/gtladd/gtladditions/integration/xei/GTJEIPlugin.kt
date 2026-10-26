package com.gtladd.gtladditions.integration.xei

import com.gregtechceu.gtceu.integration.jei.recipe.GTRecipeTypeCategory
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.APOCALYPTIC_TORSION_QUANTUM_MATRIX
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.ATOMIC_TRANSMUTATIOON_CORE
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.HELIOFLARE_POWER_FORGE
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.SUBATOMIC_TRANSMUTATIOON_CORE
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.EM_RESONANCE_CONVERSION_FIELD
import com.lowdragmc.lowdraglib.LDLib
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.constants.RecipeTypes
import mezz.jei.api.constants.VanillaTypes
import mezz.jei.api.registration.IRecipeCatalystRegistration
import mezz.jei.api.runtime.IJeiRuntime
import net.minecraft.resources.ResourceLocation

@Suppress("unused")
@JeiPlugin
class GTJEIPlugin : IModPlugin {
    override fun getPluginUid(): ResourceLocation = GTLAdditions.id("jei_plugin")

    override fun onRuntimeAvailable(jeiRuntime: IJeiRuntime) {
        val ingredients = jeiRuntime.ingredientManager
        val hiddenItems = ingredients.getAllIngredients(VanillaTypes.ITEM_STACK).filter {
            it.`is`(ATOMIC_TRANSMUTATIOON_CORE.item) || it.`is`(APOCALYPTIC_TORSION_QUANTUM_MATRIX.item)
        }

        if (hiddenItems.isNotEmpty()) ingredients.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hiddenItems)
    }

    override fun registerRecipeCatalysts(registration: IRecipeCatalystRegistration) {
        if (LDLib.isReiLoaded() || LDLib.isEmiLoaded()) return
        registration.addRecipeCatalyst(HELIOFLARE_POWER_FORGE.asStack(), RecipeTypes.SMELTING)
        // Conversion runs on a dummy recipe type; expose its actual block conversion recipes here.
        registration.addRecipeCatalyst(
            SUBATOMIC_TRANSMUTATIOON_CORE.asStack(),
            GTRecipeTypeCategory.TYPES.apply(EM_RESONANCE_CONVERSION_FIELD)
        )
    }
}