package com.gtladd.gtladditions.mixin.gtceu.api.recipe;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import com.gtladd.gtladditions.api.recipe.IWirelessGTRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.math.BigInteger;

@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(GTRecipe.class)
public abstract class GTRecipeMixin implements IWirelessGTRecipe {

    @Unique
    @Nullable
    private BigInteger euTickInputs;

    @Override
    @Nullable
    public BigInteger getEuTickInputs() {
        return euTickInputs;
    }

    @Override
    public void setEuTickInputs(@NotNull BigInteger euTickInputs) {
        this.euTickInputs = euTickInputs;
    }
}
