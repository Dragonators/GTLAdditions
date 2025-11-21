package com.gtladd.gtladditions.data.recipes.newmachinerecipe

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.data.tag.TagPrefix
import com.gregtechceu.gtceu.common.data.GTMaterials
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.utils.Registries
import java.util.function.Consumer

object ChaosWeave {
    fun init(provider : Consumer<FinishedRecipe?>) {
        GTLAddRecipesTypes.CHAOS_WEAVE.recipeBuilder(GTLAdditions.id("chaos_weave"))
            .inputItems(TagPrefix.dust, GTMaterials.Stone, 64)
            .outputItems(Registries.getItemStack("kubejs:scrap_box", 24))
            .duration(100).EUt(GTValues.V[10]).save(provider)
    }
}
