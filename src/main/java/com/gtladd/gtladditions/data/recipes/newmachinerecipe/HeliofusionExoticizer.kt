package com.gtladd.gtladditions.data.recipes.newmachinerecipe

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.block
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.dust
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.rod
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.api.data.tag.GTLTagPrefix.nanoswarm
import org.gtlcore.gtlcore.common.data.GTLMaterials
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import java.util.function.Consumer

object HeliofusionExoticizer {
    @JvmStatic
    fun init(provider : Consumer<FinishedRecipe?>) {
        GTLAddRecipesTypes.MATTER_EXOTIC.recipeBuilder(GTLAdditions.id("high_energy_quark_gluon_matter_exotic"))
            .inputFluids(GTLMaterials.HeavyQuarkDegenerateMatter.getFluid(5760))
            .inputFluids(GTLMaterials.CosmicSuperconductor.getFluid(2880))
            .inputFluids(GTLMaterials.Antimatter.getFluid(4000))
            .inputFluids(GTLMaterials.Periodicium.getFluid(1440))
            .outputFluids(GTLMaterials.HighEnergyQuarkGluon.getFluid(60000))
            .duration(1200)
            .EUt(GTValues.VA[GTValues.MAX].toLong())
            .save(provider)
        GTLAddRecipesTypes.MATTER_EXOTIC.recipeBuilder(GTLAdditions.id("hypercube_matter_exotic"))
            .inputItems(rod, GTLMaterials.Periodicium, 16)
            .inputItems(getItemStack("kubejs:quantum_anomaly"))
            .inputFluids(GTLMaterials.ExcitedDtec.getFluid(1000))
            .inputFluids(GTLMaterials.SpaceTime.getFluid(1000))
            .outputItems(getItemStack("kubejs:hypercube", 288))
            .duration(3200)
            .EUt(GTValues.VA[GTValues.MAX].toLong())
            .save(provider)
        GTLAddRecipesTypes.MATTER_EXOTIC.recipeBuilder(GTLAdditions.id("magmatter_matter_exotic"))
            .notConsumable(getItemStack("kubejs:eternity_catalyst"))
            .inputItems(block, GTLMaterials.AttunedTengam, 64)
            .inputFluids(GTLMaterials.Chaos.getFluid(1000))
            .inputFluids(GTLMaterials.SpatialFluid.getFluid(1000))
            .inputFluids(GTLMaterials.ExcitedDtsc.getFluid(1000))
            .inputFluids(GTLMaterials.Periodicium.getFluid(1000))
            .outputFluids(GTLMaterials.Magmatter.getFluid(40000))
            .duration(1000)
            .EUt(GTValues.VA[GTValues.MAX].toLong() * 15)
            .save(provider)
        GTLAddRecipesTypes.MATTER_EXOTIC.recipeBuilder(GTLAdditions.id("eternal_singularity_matter_exotic"))
            .notConsumable(nanoswarm, GTLMaterials.Eternity, 64)
            .inputItems(dust, GTLMaterials.SpaceTime)
            .inputFluids(GTLMaterials.Periodicium.getFluid(14400))
            .inputFluids(GTLMaterials.SpatialFluid.getFluid(1000))
            .inputFluids(GTLMaterials.Chaos.getFluid(1000))
            .outputItems(getItemStack("avaritia:eternal_singularity", 64))
            .duration(1600)
            .EUt(GTValues.VA[GTValues.MAX].toLong())
            .save(provider)
        GTLAddRecipesTypes.MATTER_EXOTIC.recipeBuilder(GTLAdditions.id("excited_dtsc_matter_exotic"))
            .inputFluids(GTLMaterials.DimensionallyTranscendentStellarCatalyst.getFluid(15000))
            .inputFluids(GTLMaterials.ConcentrationMixingHyperFuel2.getFluid(15000))
            .inputFluids(GTLMaterials.HighEnergyQuarkGluon.getFluid(5000))
            .inputFluids(GTLMaterials.Periodicium.getFluid(5760))
            .outputFluids(GTLMaterials.ExcitedDtsc.getFluid(50000))
            .duration(12000)
            .EUt(GTValues.VA[GTValues.OpV].toLong())
            .save(provider)
    }
}