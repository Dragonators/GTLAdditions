package com.gtladd.gtladditions.api.recipe;

import org.gtlcore.gtlcore.api.recipe.IGTRecipe;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

public interface IWirelessGTRecipe extends IGTRecipe {

    @Nullable
    BigInteger getEuTickInputs();

    void setEuTickInputs(@NotNull BigInteger euTickInputs);
}
