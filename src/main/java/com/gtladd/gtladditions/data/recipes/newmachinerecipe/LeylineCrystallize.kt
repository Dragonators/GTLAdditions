package com.gtladd.gtladditions.data.recipes.newmachinerecipe

import com.gregtechceu.gtceu.api.GTValues.MAX
import com.gregtechceu.gtceu.api.GTValues.VEX
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.block
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.dust
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.ingot
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.world.level.block.Blocks
import org.gtlcore.gtlcore.api.data.tag.GTLTagPrefix.nanoswarm
import org.gtlcore.gtlcore.common.data.GTLMaterials.*
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import java.util.function.Consumer

object LeylineCrystallize {
    fun init(provider: Consumer<FinishedRecipe?>) {
        GTLAddRecipesTypes.LEYLINE_CRYSTALLIZE.recipeBuilder(GTLAdditions.id("magmatter_block"))
            .notConsumable(getItemStack("kubejs:dragon_stabilizer_core"))
            .inputItems(ingot, Magmatter, 64)
            .inputItems(getItemStack("kubejs:hypercube"))
            .inputItems(getItemStack("kubejs:quantumchromodynamic_protective_plating"))
            .inputItems(block, AttunedTengam)
            .inputItems(ingot, Magmatter, 64)
            .inputItems(getItemStack("kubejs:recursively_folded_negative_space"))
            .inputItems(getItemStack("kubejs:quantum_anomaly"))
            .inputItems(block, Chaos)
            .outputItems(block, Magmatter, 2)
            .EUt(VEX[MAX + 1])
            .duration(400)
            .save(provider)
        GTLAddRecipesTypes.LEYLINE_CRYSTALLIZE.recipeBuilder(GTLAdditions.id("command_block"))
            .notConsumable(getItemStack("kubejs:dragon_stabilizer_core"))
            .inputItems(nanoswarm, CosmicNeutronium)
            .inputItems(getItemStack("kubejs:hypercube"))
            .inputItems(getItemStack("kubejs:quantumchromodynamic_protective_plating"))
            .inputItems(block, MagnetohydrodynamicallyConstrainedStarMatter)
            .inputItems(dust, Infinity)
            .inputItems(getItemStack("kubejs:recursively_folded_negative_space"))
            .inputItems(getItemStack("kubejs:pellet_antimatter"))
            .inputItems(block, Chaos)
            .outputItems(Blocks.COMMAND_BLOCK.asItem(), 2)
            .EUt(VEX[MAX + 1])
            .duration(400)
            .save(provider)
        GTLAddRecipesTypes.LEYLINE_CRYSTALLIZE.recipeBuilder(GTLAdditions.id("chain_command_block"))
            .notConsumable(getItemStack("kubejs:dragon_stabilizer_core"))
            .inputItems(getItemStack("kubejs:chain_command_block_core"))
            .inputItems(getItemStack("kubejs:timepiece"))
            .inputItems(getItemStack("kubejs:cosmic_fabric"))
            .inputItems(getItemStack("kubejs:command_block_broken"))
            .inputItems(dust, SpaceTime)
            .inputItems(getItemStack("kubejs:eigenfolded_kerr_manifold"))
            .inputItems(getItemStack("kubejs:omni_matter"))
            .inputItems(block, SpaceTime)
            .outputItems(Blocks.CHAIN_COMMAND_BLOCK.asItem(), 2)
            .EUt(VEX[MAX + 2])
            .duration(400)
            .save(provider)
        GTLAddRecipesTypes.LEYLINE_CRYSTALLIZE.recipeBuilder(GTLAdditions.id("repeating_command_block"))
            .notConsumable(getItemStack("kubejs:dragon_stabilizer_core"))
            .inputItems(getItemStack("kubejs:repeating_command_block_core"))
            .inputItems(getItemStack("kubejs:black_body_naquadria_supersolid"))
            .inputItems(getItemStack("kubejs:two_way_foil"))
            .inputItems(getItemStack("kubejs:chain_command_block_broken"))
            .inputItems(dust, Eternity)
            .inputItems(getItemStack("kubejs:ctc_computational_unit"))
            .inputItems(getItemStack("kubejs:temporal_matter"))
            .inputItems(block, Eternity)
            .outputItems(Blocks.REPEATING_COMMAND_BLOCK.asItem(), 2)
            .EUt(VEX[MAX + 2])
            .duration(400)
            .save(provider)
    }
}
