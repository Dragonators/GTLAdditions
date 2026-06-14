package com.gtladd.gtladditions.common.modify

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.GTValues.VA
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKey
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys.GAS
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys.LIQUID
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient
import com.gregtechceu.gtceu.common.data.GCyMRecipeTypes.ALLOY_BLAST_RECIPES
import com.gregtechceu.gtceu.common.data.GTMaterials.Helium
import com.gregtechceu.gtceu.common.data.GTRecipeTypes.LASER_ENGRAVER_RECIPES
import com.gregtechceu.gtceu.common.data.GTRecipeTypes.VACUUM_RECIPES
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gtladd.gtladditions.GTLAdditions.Companion.id
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.CHAOTIC_ALCHEMY
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.LEYLINE_CRYSTALLIZE
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.PHOTON_MATRIX_ETCH
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.SPACE_ORE_PROCESSOR
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import com.gtladd.gtladditions.utils.TempChemicalHelper
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.common.data.GTLMaterials.EuvPhotoresist
import org.gtlcore.gtlcore.common.data.GTLMaterials.Photoresist
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes.*
import org.gtlcore.gtlcore.config.ConfigHolder
import java.util.function.Consumer
import kotlin.math.log10

object RecipesModify {
    fun init() {
        DOOR_OF_CREATE_RECIPES.setMaxIOSize(1, 1, 0, 0)
        CREATE_AGGREGATION_RECIPES.setMaxIOSize(2, 1, 0, 0)
        DIMENSIONALLY_TRANSCENDENT_MIXER_RECIPES.setMaxIOSize(9, 1, 16, 1)

        initPhotonMatrixEtch()
        initAntientropyCondensation()
        initChaoticAlchemy()
        initSpaceOreProcessor()
        initLeylineCrystallize()
        initForgeOfTheAntichristSlotNames()
        if (ConfigHolder.INSTANCE.enableSkyBlokeMode) SkyTearsAndGregHeart.init()
    }

    private fun initAntientropyCondensation() {
        val liquidHeliumPredicate = createFluidPredicate(LIQUID)
        val gasHeliumPredicate = createFluidPredicate(GAS)

        VACUUM_RECIPES.onRecipeBuild { recipeBuilder: GTRecipeBuilder, provider: Consumer<FinishedRecipe> ->
            val antientropyBuilder = ANTIENTROPY_CONDENSATION
                .copyFrom(recipeBuilder)
                .duration((recipeBuilder.duration / 1.5).toInt().coerceAtLeast(1))
            antientropyBuilder.input[FluidRecipeCapability.CAP]?.removeIf(liquidHeliumPredicate)
            antientropyBuilder.output[FluidRecipeCapability.CAP]?.removeIf(gasHeliumPredicate)
            antientropyBuilder.save(provider)
        }

        PLASMA_CONDENSER_RECIPES.onRecipeBuild { recipeBuilder: GTRecipeBuilder, provider: Consumer<FinishedRecipe> ->
            val antientropyBuilder = ANTIENTROPY_CONDENSATION.copyFrom(recipeBuilder)
            antientropyBuilder.input[FluidRecipeCapability.CAP]?.removeIf(liquidHeliumPredicate)
            if (antientropyBuilder.id.path != "helium_condenser") {
                antientropyBuilder.output[FluidRecipeCapability.CAP]?.removeIf(gasHeliumPredicate)
            }
            antientropyBuilder.save(provider)
        }
    }

    private fun createFluidPredicate(storageKey: FluidStorageKey): (Content) -> Boolean = { content ->
        FluidRecipeCapability.CAP.of(content.content)?.values?.firstOrNull()?.let { value ->
            when (value) {
                is FluidIngredient.TagValue -> {
                    val expectedTag = if (storageKey == LIQUID) "forge.liquid_helium" else "forge.gas_helium"
                    value.tag.location.toLanguageKey() == expectedTag
                }

                else -> {
                    value.fluids?.any { it.isSame(Helium.getFluid(storageKey)) } == true
                }
            }
        } ?: false
    }

    private fun initChaoticAlchemy() {
        ALLOY_BLAST_RECIPES.onRecipeBuild { recipeBuilder: GTRecipeBuilder, provider: Consumer<FinishedRecipe?> ->
            val chaoticRecipeBuilder = CHAOTIC_ALCHEMY
                .copyFrom(recipeBuilder)
                .duration((recipeBuilder.duration * 0.75).toInt().coerceAtLeast(1))
                .EUt(if (recipeBuilder.EUt() > 16) recipeBuilder.EUt() / 2 else recipeBuilder.EUt())

            val fluidOutputs = chaoticRecipeBuilder.output[FluidRecipeCapability.CAP] ?: return@onRecipeBuild
            val content = fluidOutputs.firstOrNull() ?: return@onRecipeBuild
            val fluidStack = FluidRecipeCapability.CAP.of(content.content).getStacks().firstOrNull() ?: return@onRecipeBuild
            val material = TempChemicalHelper.getMaterialFromFluid(fluidStack.fluid) ?: return@onRecipeBuild

            if (!material.hasFluid() || material.getFluid(LIQUID) == null) return@onRecipeBuild
            val liquidFluid = material.getFluid(LIQUID, fluidStack.amount)

            fluidOutputs.clear()
            chaoticRecipeBuilder.outputFluids(liquidFluid)
            chaoticRecipeBuilder.save(provider)
        }
    }

    private fun initPhotonMatrixEtch() {
        LASER_ENGRAVER_RECIPES.onRecipeBuild { recipeBuilder: GTRecipeBuilder, provider: Consumer<FinishedRecipe> ->
            if (recipeBuilder.output.containsKey(FluidRecipeCapability.CAP)) return@onRecipeBuild
            val photonBuilder = PHOTON_MATRIX_ETCH.copyFrom(recipeBuilder)
                .duration((recipeBuilder.duration * 0.2).toInt()).EUt(recipeBuilder.EUt())
            photonBuilder.save(provider)
            val dimensionalBuilder = DIMENSIONAL_FOCUS_ENGRAVING_ARRAY_RECIPES.copyFrom(recipeBuilder)
                .duration((recipeBuilder.duration * 0.2).toInt()).EUt(recipeBuilder.EUt() * 4L)
            val value = log10(recipeBuilder.EUt().toDouble()) / log10(4.0)
            if (value > 10.0) {
                dimensionalBuilder.inputFluids(EuvPhotoresist.getFluid((value / 2.0).toLong()))
            } else {
                dimensionalBuilder.inputFluids(Photoresist.getFluid(value.toLong()))
            }
            dimensionalBuilder.save(provider)
        }
    }

    private fun initSpaceOreProcessor() {
        INTEGRATED_ORE_PROCESSOR.onRecipeBuild { recipeBuilder: GTRecipeBuilder, provider: Consumer<FinishedRecipe> ->
            val spaceBuilder = SPACE_ORE_PROCESSOR
                .copyFrom(recipeBuilder)
                .EUt(VA[GTValues.HV].toLong())
            spaceBuilder.input[FluidRecipeCapability.CAP]?.clear()
            spaceBuilder.save(provider)
        }
    }

    private fun initLeylineCrystallize() {
        AGGREGATION_DEVICE_RECIPES.onRecipeBuild { recipeBuilder: GTRecipeBuilder, provider: Consumer<FinishedRecipe> ->
            val leylineBuilder = LEYLINE_CRYSTALLIZE
                .copyFrom(recipeBuilder)
                .EUt(recipeBuilder.EUt() * 4)

            if (!recipeBuilder.id.equals(id("reaction_chamber"))) {
                val itemOutputs = leylineBuilder.output[ItemRecipeCapability.CAP] ?: return@onRecipeBuild
                val content = itemOutputs.firstOrNull() ?: return@onRecipeBuild
                itemOutputs.clear()
                itemOutputs.add(content.copy(ItemRecipeCapability.CAP, ContentModifier.multiplier(1.5)))
            }

            leylineBuilder.save(provider)
        }
    }

    private fun initForgeOfTheAntichristSlotNames() {
        DIMENSIONALLY_TRANSCENDENT_PLASMA_FORGE_RECIPES.onRecipeBuild { recipeBuilder: GTRecipeBuilder, _: Consumer<FinishedRecipe> ->
            markCycleItemContents(recipeBuilder)
        }
        STELLAR_FORGE_RECIPES.onRecipeBuild { recipeBuilder: GTRecipeBuilder, _: Consumer<FinishedRecipe> ->
            markCycleItemContents(recipeBuilder)
        }
    }

    private fun markCycleItemContents(recipeBuilder: GTRecipeBuilder) {
        markCycleItemContents(recipeBuilder.input[ItemRecipeCapability.CAP])
        markCycleItemContents(recipeBuilder.output[ItemRecipeCapability.CAP])
    }

    private fun markCycleItemContents(contents: MutableList<Content>?) {
        contents?.forEach { content ->
            val item = ItemRecipeCapability.CAP.of(content.content).items.firstOrNull()?.item ?: return@forEach
            if (RecipeCalculationHelper.isRecipeCycleContainerItem(item)) content.slotName = RecipeCalculationHelper.RECIPE_CYCLE_CONTAINER
        }
    }
}