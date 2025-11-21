package com.gtladd.gtladditions.api.recipe

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.common.data.GTRecipeTypes
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import net.minecraft.resources.ResourceLocation
import java.math.BigInteger

class WirelessGTRecipeBuilder : GTRecipeBuilder {

    var wirelessEut: BigInteger? = null

    constructor(id: ResourceLocation, recipeType: GTRecipeType) : super(id, recipeType)

    constructor(toCopy: GTRecipe, recipeType: GTRecipeType) : super(toCopy, recipeType)

    fun setWirelessEut(wirelessEut: BigInteger?): WirelessGTRecipeBuilder {
        this.wirelessEut = wirelessEut
        return this
    }

    fun output(cap: RecipeCapability<*>, contents: List<Content>): WirelessGTRecipeBuilder {
        output[cap] = contents
        return this
    }

    override fun duration(duration: Int): WirelessGTRecipeBuilder {
        this.duration = duration
        return this
    }

    override fun buildRawRecipe(): WirelessGTRecipe {
        return WirelessGTRecipe(
            recipeType,
            id.withPrefix("${recipeType.registryName.path}/"),
            input,
            output,
            tickInput,
            tickOutput,
            inputChanceLogic,
            outputChanceLogic,
            tickInputChanceLogic,
            tickOutputChanceLogic,
            conditions,
            listOf<Any>(),
            data,
            duration,
            isFuel,
            wirelessEut
        )
    }

    companion object {
        fun ofRaw(): WirelessGTRecipeBuilder {
            return ofRaw(GTRecipeTypes.DUMMY_RECIPES)
        }

        fun ofRaw(recipeType: GTRecipeType): WirelessGTRecipeBuilder {
            return WirelessGTRecipeBuilder(GTCEu.id("raw"), recipeType)
        }
    }
}