package com.gtladd.gtladditions.data.recipes.newmachinerecipe

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.data.tag.TagPrefix
import com.gregtechceu.gtceu.common.data.GTMaterials
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.common.data.GTLMaterials
import java.util.function.Consumer

object ChaoticAlchemy {
    @JvmStatic
    fun init(provider : Consumer<FinishedRecipe?>) {
        GTLAddRecipesTypes.CHAOTIC_ALCHEMY.recipeBuilder(GTLAdditions.id("carbon_disulfide"))
            .circuitMeta(8)
            .inputItems(TagPrefix.dust, GTMaterials.Carbon)
            .inputItems(TagPrefix.dust, GTMaterials.Sulfur, 2)
            .outputFluids(GTLMaterials.CarbonDisulfide.getFluid(1000))
            .EUt(GTValues.VA[GTValues.LV].toLong()).duration(350)
            .blastFurnaceTemp(1200).save(provider)
    }
}
