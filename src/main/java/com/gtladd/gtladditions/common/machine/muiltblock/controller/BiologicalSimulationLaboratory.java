package com.gtladd.gtladditions.common.machine.muiltblock.controller;

import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine;
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
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.gtladd.gtladditions.api.recipeslogic.GTLAddMultipleRecipesLogic;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class BiologicalSimulationLaboratory extends StorageMachine implements ParallelMachine {

    private static double reDuctionEUt = 1.0;
    private static double reDuctionDuration = 1.0;
    private static int Max_Parallels = 64;
    private static boolean Is_MultiRecipe = false;
    private static final ItemStack RHENIUM_NANOSWARM = Registries.getItemStack("gtceu:rhenium_nanoswarm");
    private static final ItemStack ORICHALCUM_NANOSWARM = Registries.getItemStack("gtceu:orichalcum_nanoswarm");
    private static final ItemStack INFUSCOLIUM_NANOSWARM = Registries.getItemStack("gtceu:infuscolium_nanoswarm");
    private static final ItemStack NAN_CERTIFICATE = Registries.getItemStack("gtceu:nan_certificate");

    public BiologicalSimulationLaboratory(IMachineBlockEntity holder) {
        super(holder, 1);
    }

    protected @NotNull RecipeLogic createRecipeLogic(Object... args) {
        return new BiologicalSimulationLaboratoryLogic(this);
    }

    protected boolean filter(@NotNull ItemStack itemStack) {
        Item item = itemStack.getItem();
        if (RHENIUM_NANOSWARM.is(item)) return true;
        else if (ORICHALCUM_NANOSWARM.is(item)) return true;
        else if (INFUSCOLIUM_NANOSWARM.is(item)) return true;
        else return NAN_CERTIFICATE.is(item);
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
            if (this.holder.getOffsetTimer() % 20L == 0L) {
                this.setparameter(this);
            }
            textList.add(Component.translatable("gtceu.multiblock.parallel", Component.translatable(FormattingUtil.formatNumbers(Max_Parallels)).withStyle(ChatFormatting.DARK_PURPLE)).withStyle(ChatFormatting.GRAY));
            textList.add(Component.translatable((Is_MultiRecipe ? "已" : "未") + "解锁寰宇支配之剑的配方"));
            textList.add(Component.translatable("gtceu.machine.eut_multiplier.tooltip", Component.translatable(FormattingUtil.formatNumbers(reDuctionEUt))));
            textList.add(Component.translatable("gtceu.machine.duration_multiplier.tooltip", Component.translatable(FormattingUtil.formatNumbers(reDuctionDuration))));
        }
    }

    private int getTier(MetaMachine machine) {
        if (machine instanceof BiologicalSimulationLaboratory biologicalSimulationLaboratory) {
            Item item = biologicalSimulationLaboratory.machineStorage.storage.getStackInSlot(0).getItem();
            if (RHENIUM_NANOSWARM.is(item)) return 1;
            else if (ORICHALCUM_NANOSWARM.is(item)) return 2;
            else if (INFUSCOLIUM_NANOSWARM.is(item)) return 3;
            else if (NAN_CERTIFICATE.is(item)) return 4;
        }
        return 0;
    }

    private void setparameter(MetaMachine machine) {
        int tier = getTier(machine);
        switch (tier) {
            case 1 -> setMachine(false, 2048, 0.9, 0.9);
            case 2 -> setMachine(false, 16384, 0.8, 0.6);
            case 3 -> setMachine(false, 262144, 0.6, 0.4);
            case 4 -> setMachine(true, 4194304, 0.25, 0.1);
            default -> setMachine(false, 64, 1.0, 1.0);
        }
    }

    private void setMachine(boolean isMultiRecipe, int maxParallel, double Reductioneut, double Reductionduration) {
        Is_MultiRecipe = isMultiRecipe;
        Max_Parallels = maxParallel;
        reDuctionEUt = Reductioneut;
        reDuctionDuration = Reductionduration;
    }

    @Override
    public int getMaxParallel() {
        return Max_Parallels;
    }

    private static class BiologicalSimulationLaboratoryLogic extends GTLAddMultipleRecipesLogic {

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
                    reDuctionDuration / (1 << (getMachine().getTier() - RecipeHelper.getRecipeEUtTier(recipe))));
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
