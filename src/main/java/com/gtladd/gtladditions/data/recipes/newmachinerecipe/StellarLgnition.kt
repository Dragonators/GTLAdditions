package com.gtladd.gtladditions.data.recipes.newmachinerecipe

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.data.chemical.material.Material
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys
import com.gregtechceu.gtceu.common.data.GTMaterials.*
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.common.data.GTLMaterials.*
import java.util.function.Consumer

object StellarLgnition {
    @JvmStatic
    fun init(provider : Consumer<FinishedRecipe?>) {
        addRecipe(Argon, 36000, provider)
        addRecipe(Helium, 36000, provider)
        addRecipe(Iron, 52000, provider)
        addRecipe(Nickel, 52000, provider)
        addRecipe(Nitrogen, 21600, provider)
        addRecipe(Oxygen, 32000, provider)
        addRecipe(Silver, 56000, provider)
        addRecipe(Vibranium, 72000, provider)
        addRecipe(Mithril, 64000, provider)
        addRecipe(Starmetal, 72000, provider)
        addRecipe(Orichalcum, 56000, provider)
        addRecipe(Infuscolium, 48000, provider)
        addRecipe(Enderium, 81000, provider)
        addRecipe(Plutonium241, 48000, provider)
    }

    private fun addRecipe(material : Material, temperature : Int, provider : Consumer<FinishedRecipe?>) {
        GTLAddRecipesTypes.STELLAR_LGNITION.recipeBuilder(GTLAdditions.id(material.name))
            .circuitMeta(1)
            .inputFluids(material.getFluid(10000))
            .outputFluids(material.getFluid(FluidStorageKeys.PLASMA, 10000))
            .blastFurnaceTemp(temperature)
            .EUt(GTValues.VA[GTValues.UEV].toLong()).duration(100).save(provider)
    }
}
