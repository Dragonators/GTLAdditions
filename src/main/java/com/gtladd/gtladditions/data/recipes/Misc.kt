package com.gtladd.gtladditions.data.recipes

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.data.tag.TagPrefix
import com.gregtechceu.gtceu.common.data.GTMaterials
import com.gregtechceu.gtceu.data.recipe.builder.ShapedRecipeBuilder
import com.gtladd.gtladditions.GTLAdditions
import dev.latvian.mods.kubejs.KubeJS
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Blocks
import org.gtlcore.gtlcore.common.data.GTLMaterials
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes.*
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import java.util.function.Consumer

object Misc {
    @JvmStatic
    fun init(provider : Consumer<FinishedRecipe?>) {
        DECAY_HASTENER_RECIPES.recipeBuilder(GTLAdditions.id("tiranium50"))
            .inputFluids(GTMaterials.Titanium.getFluid(144))
            .outputFluids(GTLMaterials.Titanium50.getFluid(144))
            .EUt(GTValues.VA[14].toLong()).duration(10).save(provider)
        DOOR_OF_CREATE_RECIPES.recipeBuilder(GTLAdditions.id("command_block"))
            .inputItems(TagPrefix.block, GTLMaterials.MagnetohydrodynamicallyConstrainedStarMatter)
            .outputItems(Blocks.COMMAND_BLOCK.asItem())
            .dimension(ResourceLocation("overworld"))
            .EUt(GTValues.V[14]).duration(5).save(provider)
        DOOR_OF_CREATE_RECIPES.recipeBuilder(GTLAdditions.id("magmatter_block"))
            .inputItems(TagPrefix.ingot, GTLMaterials.Magmatter, 64)
            .outputItems(TagPrefix.block, GTLMaterials.Magmatter)
            .dimension(ResourceLocation("overworld"))
            .EUt(GTValues.V[14]).duration(5).save(provider)
        CREATE_AGGREGATION_RECIPES.recipeBuilder(GTLAdditions.id("chain_command_block"))
            .inputItems(getItemStack("kubejs:chain_command_block_core"))
            .inputItems(getItemStack("kubejs:command_block_broken"))
            .outputItems(Blocks.CHAIN_COMMAND_BLOCK.asItem())
            .dimension(KubeJS.id("create")).CWUt(Int.Companion.MAX_VALUE / 2)
            .EUt(GTValues.V[14]).duration(20)
            .save(provider)
        CREATE_AGGREGATION_RECIPES.recipeBuilder(GTLAdditions.id("repeating_command_block"))
            .inputItems(getItemStack("kubejs:repeating_command_block_core"))
            .inputItems(getItemStack("kubejs:chain_command_block_broken"))
            .outputItems(Blocks.REPEATING_COMMAND_BLOCK.asItem())
            .dimension(KubeJS.id("create")).CWUt(Int.Companion.MAX_VALUE / 2)
            .EUt(GTValues.V[14]).duration(20)
            .save(provider)
        ShapedRecipeBuilder(GTLAdditions.id("ultimate_input_dual_hatch"))
            .output(getItemStack("gtladditions:ultimate_input_dual_hatch"))
            .pattern(" S ")
            .pattern("SDS")
            .pattern(" S ")
            .define('S', getItemStack("gtladditions:super_input_dual_hatch"))
            .define('D', getItemStack("gtlcore:max_storage"))
            .save(provider);
    }
}
