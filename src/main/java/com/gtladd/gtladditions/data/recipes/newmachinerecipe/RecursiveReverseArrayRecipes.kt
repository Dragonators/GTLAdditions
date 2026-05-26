package com.gtladd.gtladditions.data.recipes.newmachinerecipe

import com.gregtechceu.gtceu.api.GTValues.OpV
import com.gregtechceu.gtceu.api.GTValues.UXV
import com.gregtechceu.gtceu.api.GTValues.VA
import com.gtladd.gtladditions.GTLAdditions.Companion.id
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.SPACETIME_STASIS
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.SUPRATEMPORAL_BOOSTING
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.TIME_SPACE_DISTORTION
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.common.data.GTLMaterials.SpaceTime
import java.util.function.Consumer

object RecursiveReverseArrayRecipes {
    fun init(provider: Consumer<FinishedRecipe?>) {
        SPACETIME_STASIS.recipeBuilder(id("spacetime_stasis"))
            .inputFluids(SpaceTime.getFluid(70000))
            .EUt(VA[OpV].toLong())
            .duration(20)
            .save(provider)

        SUPRATEMPORAL_BOOSTING.recipeBuilder(id("supratemporal_boosting"))
            .circuitMeta(1)
            .EUt(VA[UXV].toLong())
            .duration(200)
            .save(provider)

        TIME_SPACE_DISTORTION.recipeBuilder(id("time_space_distortion"))
            .circuitMeta(1)
            .EUt(VA[OpV].toLong())
            .duration(600)
            .save(provider)
    }
}