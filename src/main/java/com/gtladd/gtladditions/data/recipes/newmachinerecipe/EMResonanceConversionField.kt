package com.gtladd.gtladditions.data.recipes.newmachinerecipe

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper
import com.gregtechceu.gtceu.api.data.tag.TagPrefix
import com.gregtechceu.gtceu.common.data.GTMaterials
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import org.gtlcore.gtlcore.utils.Registries
import java.util.function.Consumer

object EMResonanceConversionField {
    fun init(provider : Consumer<FinishedRecipe?>) {
        addRecipe(Blocks.BONE_BLOCK, Registries.getBlock("kubejs:essence_block"), 4, provider)
        addRecipe(Blocks.OAK_LOG, Blocks.CRIMSON_STEM, 1, provider)
        addRecipe(Blocks.BIRCH_LOG, Blocks.WARPED_STEM, 1, provider)
        addRecipe(ChemicalHelper.getBlock(TagPrefix.block, GTMaterials.Calcium), Blocks.BONE_BLOCK, 2, provider)
        addRecipe(Blocks.MOSS_BLOCK, Blocks.SCULK, 5, provider)
        addRecipe(Blocks.GRASS_BLOCK, Blocks.MOSS_BLOCK, 2, provider)
        addRecipe(
            Registries.getBlock("kubejs:infused_obsidian"),
            Registries.getBlock("kubejs:draconium_block_charged"),
            8,
            provider
        )
    }

    private fun addRecipe(input : Block, output : Block, euT : Int, provider : Consumer<FinishedRecipe?>) {
        GTLAddRecipesTypes.EM_RESONANCE_CONVERSION_FIELD.recipeBuilder(GTLAdditions.id(output.descriptionId))
            .inputItems(input.asItem())
            .circuitMeta(1)
            .outputItems(output.asItem())
            .EUt(GTValues.VA[euT].toLong()).save(provider)
    }
}
