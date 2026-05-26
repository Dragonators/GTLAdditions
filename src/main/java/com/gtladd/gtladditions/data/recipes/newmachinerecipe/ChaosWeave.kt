package com.gtladd.gtladditions.data.recipes.newmachinerecipe

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.data.tag.TagUtil
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.utils.Registries
import java.util.function.Consumer

object ChaosWeave {
    fun init(provider: Consumer<FinishedRecipe?>) {
        generateTagRecipes(
            provider,
            TagRecipeConfig("dusts"),
            TagRecipeConfig("ingots"),
            TagRecipeConfig("small_dusts", 256),
            TagRecipeConfig("tiny_dusts", 576),
            TagRecipeConfig("storage_blocks", 7),
            TagRecipeConfig("nuggets", 576),
            TagRecipeConfig("gems")
        )
    }

    private fun generateTagRecipes(provider: Consumer<FinishedRecipe?>, vararg configs: TagRecipeConfig) {
        for (config in configs) {
            val tagKey = TagUtil.createItemTag(config.tag)
            GTLAddRecipesTypes.CHAOS_WEAVE.recipeBuilder(GTLAdditions.id("chaos_weave_${tagKey.location.path}"))
                .inputItems(tagKey, config.inputCount)
                .outputItems(Registries.getItemStack("kubejs:scrap_box", 24))
                .duration(100).EUt(GTValues.V[10]).save(provider)
        }
    }

    private data class TagRecipeConfig(
        val tag: String,
        val inputCount: Int = 64
    )
}