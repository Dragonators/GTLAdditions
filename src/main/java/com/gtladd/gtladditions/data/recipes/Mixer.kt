package com.gtladd.gtladditions.data.recipes

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.common.data.GTRecipeTypes
import com.gtladd.gtladditions.GTLAdditions
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import java.util.function.Consumer

object Mixer {
    @JvmStatic
    fun init(provider : Consumer<FinishedRecipe?>) {
        GTRecipeTypes.MIXER_RECIPES.recipeBuilder(GTLAdditions.id("warped_ender_pearl"))
            .inputItems(getItemStack("minecraft:bone_meal", 4))
            .inputItems(getItemStack("minecraft:blaze_powder", 4))
            .inputItems(getItemStack("minecraft:ender_pearl"))
            .outputItems(getItemStack("kubejs:warped_ender_pearl", 4))
            .EUt(GTValues.VA[4].toLong()).duration(200).save(provider)
    }
}
