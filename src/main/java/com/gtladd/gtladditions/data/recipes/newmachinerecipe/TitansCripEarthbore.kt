package com.gtladd.gtladditions.data.recipes.newmachinerecipe

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.data.tag.TagPrefix
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.common.data.GTLMaterials
import org.gtlcore.gtlcore.utils.Registries
import java.util.function.Consumer

object TitansCripEarthbore {
    fun init(provider : Consumer<FinishedRecipe?>) {
        GTLAddRecipesTypes.TECTONIC_FAULT_GENERATOR.recipeBuilder(GTLAdditions.id("bedrock_dust"))
            .chancedInput(Registries.getItemStack("kubejs:bedrock_drill"), 100, 0)
            .circuitMeta(1)
            .outputItems(TagPrefix.dust, GTLMaterials.Bedrock, 64)
            .EUt(GTValues.VA[11].toLong()).duration(1200).save(provider)
    }
}
