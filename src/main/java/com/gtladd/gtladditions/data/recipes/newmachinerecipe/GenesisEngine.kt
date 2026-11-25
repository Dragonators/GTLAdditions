package com.gtladd.gtladditions.data.recipes.newmachinerecipe

import com.gregtechceu.gtceu.api.GTValues
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.machine.feature.IStructureTierMachine
import com.gtladd.gtladditions.common.items.GTLAddItems
import com.gtladd.gtladditions.common.machine.multiblock.controller.HeartOfTheUniverse
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import java.util.function.Consumer

object GenesisEngine {
    fun init(provider: Consumer<FinishedRecipe?>) {
        // 64 creative energy hatches per plane: 1280 ticks squeezed into the Heart's 20.
        GTLAddRecipesTypes.GENESIS_ENGINE.recipeBuilder(GTLAdditions.id("eternity_dark_matter"))
            .inputItems(GTLAddItems.ETERNITY_DARK_MATTER_FUEL_ROD)
            .chancedInput(GTLAddItems.BLACK_HOLE_SEED.asStack(), 500, 0)
            .chancedOutput(getItemStack("kubejs:annihilation_constrainer"), 9000, 0)
            .duration(1280)
            .EUt(-GTValues.VEX[GTValues.MAX_TRUE])
            .addData(IStructureTierMachine.RECIPE_TIER, 1)
            .save(provider)
        GTLAddRecipesTypes.GENESIS_ENGINE.recipeBuilder(GTLAdditions.id(HeartOfTheUniverse.ANNIHILATION_RECIPE_NAME))
            .inputItems(GTLAddItems.STRANGE_ANNIHILATION_FUEL_ROD)
            .inputItems(GTLAddItems.BLACK_HOLE_SEED)
            .chancedOutput(getItemStack("kubejs:annihilation_constrainer", 64), 6000, 0)
            .duration(81920)
            .EUt(-GTValues.VEX[GTValues.MAX_TRUE])
            .addData(IStructureTierMachine.RECIPE_TIER, 2)
            .save(provider)
    }
}
