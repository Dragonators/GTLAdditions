package com.gtladd.gtladditions.api.Machine.Special;

import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine;
import org.gtlcore.gtlcore.common.data.GTLRecipeModifiers;
import org.gtlcore.gtlcore.common.machine.multiblock.electric.StorageMachine;
import org.gtlcore.gtlcore.utils.Registries;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.logic.OCParams;
import com.gregtechceu.gtceu.api.recipe.logic.OCResult;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.gtladd.gtladditions.api.RecipesLogic.GTLAddMultipleRecipesLogic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class BiologicalSimulationLaboratory extends StorageMachine implements ParallelMachine {

    public static double reDuctionEUt = 1.0;
    public static double reDuctionDuration = 1.0;
    public static int Max_Parallels = 64;
    public static boolean Is_MultiRecipe = false;

    public BiologicalSimulationLaboratory(IMachineBlockEntity holder) {
        super(holder, 1);
    }

    protected RecipeLogic createRecipeLogic(Object... args) {
        return new BiologicalSimulationLaboratoryLogic(this);
    }

    public static @Nullable GTRecipe recipeModifier(MetaMachine machine, @NotNull GTRecipe recipe, @NotNull OCParams params, @NotNull OCResult result) {
        if (machine instanceof BiologicalSimulationLaboratory biologicalSimulationLaboratory) {
            return GTLRecipeModifiers.reduction(machine, recipe, reDuctionEUt, reDuctionDuration);
        }
        return null;
    }

    public static boolean beforeWorking(IRecipeLogicMachine machine, @NotNull GTRecipe recipe) {
        if (machine instanceof BiologicalSimulationLaboratory biologicalSimulationLaboratory) {
            biologicalSimulationLaboratory.setparameter((MetaMachine) machine);
            List<ItemStack> input = RecipeHelper.getInputItems(recipe);
            for (ItemStack itemstack : input) {
                if (itemstack.getItem().equals(Registries.getItem("avaritia:infinity_sword")) && !Is_MultiRecipe) return false;
            }
        }
        return true;
    }

    public void addDisplayText(@NotNull List<Component> textList) {
        super.addDisplayText(textList);
        if (this.isFormed) {
            textList.add(Component.translatable("gtceu.multiblock.parallel", Component.translatable(FormattingUtil.formatNumbers(Max_Parallels)).withStyle(ChatFormatting.DARK_PURPLE)).withStyle(ChatFormatting.GRAY));
            textList.add(Component.translatable((Is_MultiRecipe ? "已" : "未") + "解锁寰宇支配之剑的配方"));
            textList.add(Component.translatable("gtceu.machine.eut_multiplier.tooltip", Component.translatable(FormattingUtil.formatNumbers(reDuctionEUt))));
            textList.add(Component.translatable("gtceu.machine.duration_multiplier.tooltip", Component.translatable(FormattingUtil.formatNumbers(reDuctionDuration))));
        }
    }

    private int getTier(MetaMachine machine) {
        int tier = 0;
        if (machine instanceof BiologicalSimulationLaboratory biologicalSimulationLaboratory) {
            ItemStack item = biologicalSimulationLaboratory.getMachineStorageItem();
            if (Objects.equals(item.getItem(), Registries.getItem("gtceu:rhenium_nanoswarm"))) {
                tier = 1;
            } else if (Objects.equals(item.getItem(), Registries.getItem("gtceu:orichalcum_nanoswarm"))) {
                tier = 2;
            } else if (Objects.equals(item.getItem(), Registries.getItem("gtceu:infuscolium_nanoswarm"))) {
                tier = 3;
            } else if (Objects.equals(item.getItem(), Registries.getItem("gtceu:nan_certificate"))) {
                tier = 4;
            } else {
                tier = 5;
            }
        }
        return tier;
    }

    private void setparameter(MetaMachine machine) {
        int tier = getTier(machine);
        switch (tier) {
            case 1 -> {
                setmachine(false, 2048, 0.9, 0.9);
            }
            case 2 -> {
                setmachine(false, 16384, 0.8, 0.6);
            }
            case 3 -> {
                setmachine(false, 262144, 0.6, 0.4);
            }
            case 4 -> {
                setmachine(true, 4194304, 0.25, 0.1);
            }
            default -> {
                setmachine(false, 64, 1.0, 1.0);
            }
        }
    }

    private void setmachine(boolean isMultiRecipe, int maxParallel, double Reductioneut, double Reductionduration) {
        Is_MultiRecipe = isMultiRecipe;
        Max_Parallels = maxParallel;
        reDuctionEUt = Reductioneut;
        reDuctionDuration = Reductionduration;
    }

    @Override
    public int getMaxParallel() {
        return Max_Parallels;
    }

    static class BiologicalSimulationLaboratoryLogic extends GTLAddMultipleRecipesLogic {

        public BiologicalSimulationLaboratoryLogic(WorkableElectricMultiblockMachine machine) {
            super((ParallelMachine) machine);
        }

        public BiologicalSimulationLaboratory getMachine() {
            return (BiologicalSimulationLaboratory) super.getMachine();
        }

        public void findAndHandleRecipe() {
            lastRecipe = null;
            GTRecipe match;
            if (isNanCertificate()) match = getRecipe();
            else match = getOneRecipe();
            if (match != null && match.matchRecipe(this.machine).isSuccess()) {
                setupRecipe(match);
            }
        }

        private boolean isNanCertificate() {
            ItemStack item = getMachine().getMachineStorageItem();
            return Objects.equals(item.getItem(), Registries.getItem("gtceu:nan_certificate"));
        }

        private GTRecipe getOneRecipe() {
            if (!machine.hasProxies()) return null;
            GTRecipe recipe = this.machine.getRecipeType().getLookup().findRecipe(machine);
            if (recipe == null || RecipeHelper.getRecipeEUtTier(recipe) > getMachine().getTier()) return null;
            recipe = parallelRecipe(recipe, getMachine().getMaxParallel());
            RecipeHelper.setInputEUt(recipe, (long) Math.max(1.0, (RecipeHelper.getInputEUt(recipe) * reDuctionEUt)));
            recipe.duration = (int) Math.max(1.0, (double) recipe.duration *
                    reDuctionDuration / Math.pow(2, (getMachine().getTier() - RecipeHelper.getRecipeEUtTier(recipe))));
            return recipe;
        }

        @Override
        public void onRecipeFinish() {
            machine.afterWorking();
            if (lastRecipe != null) {
                lastRecipe.postWorking(this.machine);
                lastRecipe.handleRecipeIO(IO.OUT, this.machine, this.chanceCaches);
            }
            GTRecipe match = isNanCertificate() ? getRecipe() : getOneRecipe();
            if (match != null) if (match.matchRecipe(this.machine).isSuccess()) {
                setupRecipe(match);
                return;
            }
            setStatus(Status.IDLE);
            progress = 0;
            duration = 0;
        }
    }
}
