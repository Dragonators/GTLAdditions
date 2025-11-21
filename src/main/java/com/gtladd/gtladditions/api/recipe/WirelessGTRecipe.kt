package com.gtladd.gtladditions.api.recipe

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.api.recipe.RecipeCondition
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic
import com.gregtechceu.gtceu.api.recipe.content.Content
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import java.math.BigInteger

open class WirelessGTRecipe : GTRecipe, IWirelessGTRecipe {

    @JvmField
    protected val wirelessEuTickInputs: BigInteger?

    constructor(
        recipeType: GTRecipeType,
        inputs: Map<RecipeCapability<*>, List<Content>>,
        outputs: Map<RecipeCapability<*>, List<Content>>,
        tickInputs: Map<RecipeCapability<*>, List<Content>>,
        tickOutputs: Map<RecipeCapability<*>, List<Content>>,
        inputChanceLogics: Map<RecipeCapability<*>, ChanceLogic>,
        outputChanceLogics: Map<RecipeCapability<*>, ChanceLogic>,
        tickInputChanceLogics: Map<RecipeCapability<*>, ChanceLogic>,
        tickOutputChanceLogics: Map<RecipeCapability<*>, ChanceLogic>,
        conditions: List<RecipeCondition>,
        ingredientActions: List<*>,
        data: CompoundTag,
        duration: Int,
        isFuel: Boolean
    ) : this(
        recipeType,
        null,
        inputs,
        outputs,
        tickInputs,
        tickOutputs,
        inputChanceLogics,
        outputChanceLogics,
        tickInputChanceLogics,
        tickOutputChanceLogics,
        conditions,
        ingredientActions,
        data,
        duration,
        isFuel,
        null
    )

    constructor(
        recipeType: GTRecipeType,
        id: ResourceLocation?,
        inputs: Map<RecipeCapability<*>, List<Content>>,
        outputs: Map<RecipeCapability<*>, List<Content>>,
        tickInputs: Map<RecipeCapability<*>, List<Content>>,
        tickOutputs: Map<RecipeCapability<*>, List<Content>>,
        inputChanceLogics: Map<RecipeCapability<*>, ChanceLogic>,
        outputChanceLogics: Map<RecipeCapability<*>, ChanceLogic>,
        tickInputChanceLogics: Map<RecipeCapability<*>, ChanceLogic>,
        tickOutputChanceLogics: Map<RecipeCapability<*>, ChanceLogic>,
        conditions: List<RecipeCondition>,
        ingredientActions: List<*>,
        data: CompoundTag,
        duration: Int,
        isFuel: Boolean
    ) : this(
        recipeType,
        id,
        inputs,
        outputs,
        tickInputs,
        tickOutputs,
        inputChanceLogics,
        outputChanceLogics,
        tickInputChanceLogics,
        tickOutputChanceLogics,
        conditions,
        ingredientActions,
        data,
        duration,
        isFuel,
        null
    )

    constructor(
        recipeType: GTRecipeType,
        id: ResourceLocation?,
        inputs: Map<RecipeCapability<*>, List<Content>>,
        outputs: Map<RecipeCapability<*>, List<Content>>,
        tickInputs: Map<RecipeCapability<*>, List<Content>>,
        tickOutputs: Map<RecipeCapability<*>, List<Content>>,
        inputChanceLogics: Map<RecipeCapability<*>, ChanceLogic>,
        outputChanceLogics: Map<RecipeCapability<*>, ChanceLogic>,
        tickInputChanceLogics: Map<RecipeCapability<*>, ChanceLogic>,
        tickOutputChanceLogics: Map<RecipeCapability<*>, ChanceLogic>,
        conditions: List<RecipeCondition>,
        ingredientActions: List<*>,
        data: CompoundTag,
        duration: Int,
        isFuel: Boolean,
        wirelessEuTickInputs: BigInteger?
    ) : super(
        recipeType,
        id,
        inputs,
        outputs,
        tickInputs,
        tickOutputs,
        inputChanceLogics,
        outputChanceLogics,
        tickInputChanceLogics,
        tickOutputChanceLogics,
        conditions,
        ingredientActions,
        data,
        duration,
        isFuel
    ) {
        this.wirelessEuTickInputs = wirelessEuTickInputs
    }

    override fun getWirelessEuTickInputs(): BigInteger? {
        return wirelessEuTickInputs
    }
}