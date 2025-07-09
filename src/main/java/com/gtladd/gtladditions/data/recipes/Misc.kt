package com.gtladd.gtladditions.data.recipes

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.common.data.GTMaterials
import com.gtladd.gtladditions.GTLAdditions
import dev.latvian.mods.kubejs.KubeJS
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.resources.ResourceLocation
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
            .inputItems(getItemStack("gtceu:magnetohydrodynamicallyconstrainedstarmatter_block"))
            .outputItems(getItemStack("minecraft:command_block"))
            .dimension(ResourceLocation("overworld"))
            .EUt(GTValues.V[14]).duration(20).save(provider)
        DOOR_OF_CREATE_RECIPES.recipeBuilder(GTLAdditions.id("magmatter_block"))
            .inputItems(getItemStack("gtceu:magmatter_ingot", 64))
            .outputItems(getItemStack("gtceu:magmatter_block"))
            .dimension(ResourceLocation("overworld"))
            .EUt(GTValues.V[14]).duration(20).save(provider)
        CREATE_AGGREGATION_RECIPES.recipeBuilder(GTLAdditions.id("chain_command_block"))
            .inputItems(getItemStack("kubejs:chain_command_block_core"))
            .inputItems(getItemStack("kubejs:command_block_broken"))
            .outputItems(getItemStack("minecraft:chain_command_block"))
            .dimension(KubeJS.id("create")).CWUt(Int.Companion.MAX_VALUE - 1)
            .EUt(GTValues.V[14]).duration(600)
            .save(provider)
        CREATE_AGGREGATION_RECIPES.recipeBuilder(GTLAdditions.id("repeating_command_block"))
            .inputItems(getItemStack("kubejs:repeating_command_block_core"))
            .inputItems(getItemStack("kubejs:chain_command_block_broken"))
            .outputItems(getItemStack("minecraft:repeating_command_block"))
            .dimension(KubeJS.id("create")).CWUt(Int.Companion.MAX_VALUE - 1)
            .EUt(GTValues.V[14]).duration(600)
            .save(provider)
    }
}
