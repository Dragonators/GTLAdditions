package com.gtladd.gtladditions.data.guide

import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.common.data.GCyMRecipeTypes
import com.gregtechceu.gtceu.common.data.GTMachines
import com.gregtechceu.gtceu.common.data.GTRecipeTypes
import com.gtladd.gtladditions.api.gui.GTLytSlotGrid
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import com.gtladd.gtladditions.utils.ComponentExtensions.translatable
import guideme.compiler.tags.RecipeTypeMappingSupplier
import guideme.document.block.recipes.LytStandardRecipeBox
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes
import org.gtlcore.gtlcore.common.data.machines.GCyMMachines
import org.gtlcore.gtlcore.utils.Registries.getItemStack

class RecipeTypeContributions : RecipeTypeMappingSupplier {
    override fun collect(mappings: RecipeTypeMappingSupplier.RecipeTypeMappings) {
        for ((recipeType, icon) in TYPE_MAP) {
            mappings.add(recipeType) { recipe -> getGTRecipe(recipe, recipeType, icon) }
        }
    }

    private fun getGTRecipe(recipe: GTRecipe, recipeType: GTRecipeType, icon: ItemStack): LytStandardRecipeBox<GTRecipe> {
        val builder = LytStandardRecipeBox.builder()
            .icon(icon)
            .title(recipeType.registryName.toLanguageKey().translatable)
        val slotGrid = GTLytSlotGrid.builder(recipe)
        builder.input(slotGrid.recipeInput)
        builder.output(slotGrid.recipeOutput)
        return builder.build(recipe)
    }

    companion object {
        private val TYPE_MAP = Object2ObjectOpenHashMap<GTRecipeType, ItemStack>().also {
            it[GTLAddRecipesTypes.EM_RESONANCE_CONVERSION_FIELD] = MultiBlockMachine.SUBATOMIC_TRANSMUTATIOON_CORE.asStack()
            it[GTLAddRecipesTypes.TECTONIC_FAULT_GENERATOR] = MultiBlockMachine.TITAN_CRIP_EARTHBORE.asStack()
            it[GTLAddRecipesTypes.MATTER_EXOTIC] = MultiBlockMachine.HELIOFUSION_EXOTICIZER.asStack()
            it[GTLAddRecipesTypes.NIGHTMARE_CRAFTING] = MultiBlockMachine.DIMENSION_FOCUS_INFINITY_CRAFTING_ARRAY.asStack()
            it[GTLAddRecipesTypes.LEYLINE_CRYSTALLIZE] = MultiBlockMachine.HELIOPHASE_LEYLINE_CRYSTALLIZER.asStack()
            it[GTLAddRecipesTypes.CHAOTIC_ALCHEMY] = MultiBlockMachine.FUXI_BAGUA_HEAVEN_FORGING_FURNACE.asStack()
            it[GTLAddRecipesTypes.MOLECULAR_DECONSTRUCTION] = MultiBlockMachine.FUXI_BAGUA_HEAVEN_FORGING_FURNACE.asStack()
            it[GTLAddRecipesTypes.COMPRESSED_ASTRAL_ARRAY] = MultiBlockMachine.ARCANIC_ASTROGRAPH.asStack()
            it[GTLAddRecipesTypes.TIME_SPACE_DISTORTION] = MultiBlockMachine.TIME_SPACE_DISTORTER.asStack()
            it[GCyMRecipeTypes.ALLOY_BLAST_RECIPES] = GCyMMachines.BLAST_ALLOY_SMELTER.asStack()
            it[GTRecipeTypes.BLAST_RECIPES] = GTMachines.ELECTRIC_BLAST_FURNACE.asStack()
            it[GTRecipeTypes.ASSEMBLER_RECIPES] = GTMachines.ASSEMBLER[10].asStack()
            it[GTRecipeTypes.MIXER_RECIPES] = GTMachines.MIXER[2].asStack()
            it[GTLRecipeTypes.MATTER_FABRICATOR_RECIPES] = getItemStack("gtceu:matter_fabricator")
            it[GTLRecipeTypes.DISTORT_RECIPES] = getItemStack("gtceu:chemical_distort")
            it[GTLRecipeTypes.QFT_RECIPES] = getItemStack("gtceu:qft")
            it[GTLRecipeTypes.DECAY_HASTENER_RECIPES] = MultiBlockMachine.SKELETON_SHIFT_RIFT_ENGINE.asStack()
            it[GTLRecipeTypes.DOOR_OF_CREATE_RECIPES] = getItemStack("gtceu:door_of_create")
            it[GTLRecipeTypes.CREATE_AGGREGATION_RECIPES] = getItemStack("gtceu:create_aggregation")
            it[GTLRecipeTypes.DIMENSIONALLY_TRANSCENDENT_PLASMA_FORGE_RECIPES] =
                getItemStack("gtceu:dimensionally_transcendent_plasma_forge")
            it[GTLRecipeTypes.STELLAR_FORGE_RECIPES] = getItemStack("gtceu:stellar_forge")
        }
    }
}