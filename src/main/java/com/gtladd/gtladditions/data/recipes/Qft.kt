package com.gtladd.gtladditions.data.recipes

import com.gregtechceu.gtceu.api.GTValues.MAX
import com.gregtechceu.gtceu.api.GTValues.VA
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.dust
import com.gregtechceu.gtceu.common.data.GTMaterials.*
import com.gtladd.gtladditions.GTLAdditions.Companion.id
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.common.data.GTLMaterials.*
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes.QFT_RECIPES
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import java.util.function.Consumer

object Qft {
    fun init(provider : Consumer<FinishedRecipe?>) {
        QFT_RECIPES.recipeBuilder(id("resonating_gem"))
            .notConsumable(getItemStack("kubejs:eternity_catalyst"))
            .inputItems(dust, Sapphire, 64)
            .outputItems(getItemStack("kubejs:resonating_gem", 64))
            .inputFluids(Mana.getFluid(10000))
            .inputFluids(Starlight.getFluid(10000))
            .inputFluids(Water.getFluid(10000000))
            .EUt(VA[14].toLong()).duration(1200).save(provider)
        QFT_RECIPES.recipeBuilder(id("gamma_rays_photoresist"))
            .notConsumable(getItemStack("kubejs:spacetime_catalyst"))
            .inputItems(dust, Flerovium, 16)
            .inputItems(dust, Lanthanum, 16)
            .inputItems(dust, UnfoldedFullerene, 16)
            .inputItems(dust, Holmium, 16)
            .inputItems(dust, Thulium, 16)
            .inputItems(dust, Copernicium, 16)
            .inputItems(dust, Astatine, 16)
            .inputItems(dust, Francium, 16)
            .inputItems(dust, Boron, 16)
            .inputItems(dust, Carbon, 16)
            .inputFluids(EuvPhotoresist.getFluid(10000))
            .inputFluids(Chlorine.getFluid(16000))
            .inputFluids(Nitrogen.getFluid(32000))
            .outputFluids(GammaRaysPhotoresist.getFluid(16000))
            .EUt(VA[14].toLong()).duration(2560).save(provider)
        QFT_RECIPES.recipeBuilder(id("radox_easy"))
            .inputItems(dust, Chromium, 4)
            .inputItems(dust, Boron, 4)
            .inputItems(dust, Silver, 4)
            .inputItems(dust, Cobalt, 4)
            .inputItems(dust, Silicon, 4)
            .inputItems(dust, Molybdenum, 4)
            .inputItems(dust, Zirconium, 4)
            .inputItems(dust, Copper, 4)
            .inputItems(dust, Arsenic, 4)
            .inputItems(dust, Antimony, 4)
            .inputItems(dust, Phosphorus, 4)
            .inputItems(dust, Zinc, 4)
            .inputItems(dust, Sodium, 4)
            .inputItems(dust, Magnesium, 4)
            .inputItems(dust, Lead, 4)
            .inputItems(dust, Potassium, 4)
            .inputItems(dust, Germanium, 4)
            .inputItems(dust, RareEarth, 4)
            .inputFluids(RadoxGas.getFluid(21600))
            .inputFluids(TemporalFluid.getFluid(1000))
            .inputFluids(Titanium50.getFluid(576))
            .outputFluids(Radox.getFluid(10800))
            .EUt(VA[MAX] * 256L).duration(2560).save(provider)
        QFT_RECIPES.recipeBuilder(id("super_mutated_living_solder"))
            .notConsumable(getItemStack("kubejs:spacetime_catalyst"))
            .inputItems(getItemStack("kubejs:essence_seed", 256))
            .inputItems(getItemStack("kubejs:draconium_dust", 256))
            .inputItems(getItemStack("ae2:sky_dust", 256))
            .inputItems(dust, NetherStar, 4)
            .inputFluids(MutatedLivingSolder.getFluid(100000))
            .inputFluids(Biomass.getFluid(100000))
            .inputFluids(SterileGrowthMedium.getFluid(100000))
            .outputFluids(SuperMutatedLivingSolder.getFluid(100000))
            .EUt(VA[MAX] * 3072L).duration(7200).save(provider)
    }
}
