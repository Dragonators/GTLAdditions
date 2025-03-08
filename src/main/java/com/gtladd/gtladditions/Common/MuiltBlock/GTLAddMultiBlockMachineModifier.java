package com.gtladd.gtladditions.Common.MuiltBlock;

import org.gtlcore.gtlcore.common.machine.multiblock.electric.StorageMachine;
import org.gtlcore.gtlcore.utils.Registries;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.gtladd.gtladditions.api.Machine.GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class GTLAddMultiBlockMachineModifier {

    public static final RecipeModifier[] TITAN_CRIP_EARTHBORE_MODIFIER = new RecipeModifier[] {
            (machine, recipe, params, result) -> {
                boolean isParallel = false;
                int p = 0;
                if (machine instanceof StorageMachine storageMachine) {
                    ItemStack item = storageMachine.getMachineStorageItem();
                    p = item.getCount() * 4;
                    isParallel = Objects.equals(item.getItem(), Registries.getItem("kubejs:bedrock_drill"));
                }
                return isParallel ? GTRecipeModifiers.accurateParallel(machine, recipe, p, false).getFirst() : recipe;
            }, GTRecipeModifiers.ELECTRIC_OVERCLOCK.apply(OverclockingLogic.PERFECT_OVERCLOCK)
    };
    public static final RecipeModifier[] DRACONIC_COLLAPSE_CORE_MODIFIER = new RecipeModifier[] { (machine, recipe, params, result) -> GTRecipeModifiers.accurateParallel(machine, recipe, (int) Math.pow(8.0, ((WorkableElectricMultiblockMachine) machine).getTier() - 10), false).getFirst(),
            GTRecipeModifiers.ELECTRIC_OVERCLOCK.apply(OverclockingLogic.PERFECT_OVERCLOCK)
    };
    public static final BiConsumer<IMultiController, List<Component>> MULTIPLERECIPES_COIL_PARALLEL = (controller, components) -> {
        if (controller instanceof GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine machine) {
            if (controller.isFormed()) {
                components.add(Component.translatable("gtceu.multiblock.parallel",
                        Component.translatable(FormattingUtil.formatNumbers(Math.min(Integer.MAX_VALUE, (int) Math.pow(2.0, (double) machine.getCoilType().getCoilTemperature() / 900.0))))
                                .withStyle(ChatFormatting.DARK_PURPLE))
                        .withStyle(ChatFormatting.GRAY));
            }
        }
    };
    public static final BiConsumer<IMultiController, List<Component>> INT_MAX_PARALLEL = (controller, components) -> {
        if (controller.isFormed()) {
            components.add(Component.translatable("gtceu.multiblock.parallel", Component.literal("2147483647")
                    .withStyle(ChatFormatting.DARK_PURPLE))
                    .withStyle(ChatFormatting.GRAY));
        }
    };
    public static final BiConsumer<IMultiController, List<Component>> DRACONIC_COLLAPSE_CORE_ADDTEXT = (controller, components) -> {
        if (controller.isFormed()) {
            components.add(Component
                    .translatable("gtceu.multiblock.parallel", Component.translatable(FormattingUtil.formatNumbers(Math.pow(8.0, ((WorkableElectricMultiblockMachine) controller).getTier() - 10)))
                            .withStyle(ChatFormatting.DARK_PURPLE))
                    .withStyle(ChatFormatting.GRAY));
        }
    };
}
