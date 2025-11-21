package com.gtladd.gtladditions.data.recipes.newmachinerecipe

import com.gregtechceu.gtceu.api.GTValues
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.items.GTLAddItems
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import java.util.function.Consumer

object GenesisEngine {
    fun init(provider : Consumer<FinishedRecipe?>) {
        GTLAddRecipesTypes.GENESIS_ENGINE.recipeBuilder(GTLAdditions.id("carbon_disulfide"))
            .inputItems(GTLAddItems.STRANGE_ANNIHILATION_FUEL_ROD)
            .inputItems(GTLAddItems.BLACK_HOLE_SEED)
            .chancedOutput(getItemStack("kubejs:annihilation_constrainer", 64), 6000, 0)
            .duration(81920)
            .EUt(-GTValues.VEX[GTValues.MAX_TRUE])
            .save(provider)
    }
}