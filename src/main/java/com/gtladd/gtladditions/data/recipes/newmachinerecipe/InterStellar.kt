package com.gtladd.gtladditions.data.recipes.newmachinerecipe

import com.gregtechceu.gtceu.api.GTValues
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import net.minecraft.data.recipes.FinishedRecipe
import java.util.function.Consumer

object InterStellar {
    fun init(provider: Consumer<FinishedRecipe?>) {
        GTLAddRecipesTypes.INTER_STELLAR.recipeBuilder(GTLAdditions.id("inter_stellar"))
            .circuitMeta(1)
            .duration(800)
            .EUt(GTValues.V[GTValues.UXV])
            .save(provider)
    }
}