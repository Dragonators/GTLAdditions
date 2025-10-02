package com.gtladd.gtladditions.mixin.gtceu.integration;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.integration.jade.provider.MachineModeProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;

import java.util.Objects;

@Mixin(MachineModeProvider.class)
public abstract class MachineModeProviderMixin implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlockEntity() instanceof MetaMachineBlockEntity blockEntity) {
            @Nullable
            GTRecipeType[] recipeTypes = blockEntity.getMetaMachine().getDefinition().getRecipeTypes();
            if (recipeTypes != null && recipeTypes.length > 1) {
                if (blockEntity.getMetaMachine() instanceof IRecipeLogicMachine recipeLogicMachine) {
                    ListTag recipeTypesTagList = new ListTag();
                    GTRecipeType currentRecipeType = recipeLogicMachine.getRecipeType();
                    int currentRecipeTypeIndex = -1;

                    if (GTLAddRecipesTypes.MULTIPLE_TYPE_RECIPES.contains(currentRecipeType)) {
                        currentRecipeTypeIndex = 0;
                        recipeTypesTagList.add(StringTag.valueOf(currentRecipeType.registryName.toString()));
                    } else {
                        for (int i = 0; i < recipeTypes.length; i++) {
                            if (recipeTypes[i] == currentRecipeType) {
                                currentRecipeTypeIndex = i;
                            }
                            recipeTypesTagList.add(StringTag.valueOf(Objects.requireNonNull(recipeTypes[i]).registryName.toString()));
                        }
                    }

                    compoundTag.put("RecipeTypes", recipeTypesTagList);
                    compoundTag.putInt("CurrentRecipeType", currentRecipeTypeIndex);
                }
            }
        }
    }
}
