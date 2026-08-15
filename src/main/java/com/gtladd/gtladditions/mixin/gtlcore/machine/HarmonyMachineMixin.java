package com.gtladd.gtladditions.mixin.gtlcore.machine;

import org.gtlcore.gtlcore.api.machine.multiblock.NoEnergyMultiblockMachine;
import org.gtlcore.gtlcore.api.recipe.RecipeExtensionCopier;
import org.gtlcore.gtlcore.common.data.GTLMaterials;
import org.gtlcore.gtlcore.common.machine.multiblock.electric.HarmonyMachine;
import org.gtlcore.gtlcore.utils.MachineIO;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.gtladd.gtladditions.api.machine.IHarmonyMachineAccessor;
import com.gtladd.gtladditions.utils.CommonUtils;
import com.hepdd.gtmthings.api.misc.WirelessEnergyManager;
import com.hepdd.gtmthings.utils.TeamUtil;
import com.llamalad7.mixinextras.sugar.Local;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.UUID;

@Mixin(HarmonyMachine.class)
@Implements(@Interface(
                       iface = IHarmonyMachineAccessor.class,
                       prefix = "gtladditions$"))
public abstract class HarmonyMachineMixin extends NoEnergyMultiblockMachine {

    @Shadow(remap = false)
    private int oc = 0;
    @Shadow(remap = false)
    private long hydrogen = 0L;
    @Shadow(remap = false)
    private long helium = 0L;
    @Shadow(remap = false)
    private UUID userid;

    @Unique
    @Persisted
    private final NotifiableItemStackHandler gtladditions$machineStorage = new NotifiableItemStackHandler(
            this,
            1,
            IO.NONE,
            IO.BOTH,
            slots -> new ItemStackTransfer(1) {

                @Override
                public int getSlotLimit(int slot) {
                    return 1;
                }
            });

    @Shadow(remap = false)
    private long getStartupEnergy() {
        throw new AssertionError();
    }

    public HarmonyMachineMixin(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    @Unique
    public boolean gtladditions$consumeCosmosStartup() {
        if (userid == null || hydrogen < 1024000000L || helium < 1024000000L || oc <= 0) return false;
        if (!WirelessEnergyManager.addEUToGlobalEnergyMap(userid, -getStartupEnergy(), this)) return false;

        hydrogen -= 1024000000L;
        helium -= 1024000000L;
        return true;
    }

    @Unique
    public boolean gtladditions$consumeAstralStartup() {
        if (userid == null || hydrogen < 1024000000L || helium < 1024000000L) return false;
        if (!WirelessEnergyManager.addEUToGlobalEnergyMap(userid, -Long.MAX_VALUE, this)) return false;

        hydrogen -= 1024000000L;
        helium -= 1024000000L;
        return true;
    }

    @Unique
    public int gtladditions$getHarmonyDuration() {
        return (int) (4800 / Math.pow(2, oc));
    }

    @Unique
    public @NotNull GTRecipe gtladditions$applyCreateDataOutput(GTRecipe instance) {
        GTRecipe modified = instance.copy();
        RecipeExtensionCopier.copy(instance, modified);

        ItemStack stack = gtladditions$machineStorage.storage.getStackInSlot(0);
        if (stack.isEmpty() ||
                !"gtladditions:create_data".equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
            return modified;
        }

        modified.outputs.clear();
        RecipeCapability<FluidIngredient> fluidCapability = FluidRecipeCapability.CAP;
        Content fluidContent = new Content(
                FluidIngredient.of(GTLMaterials.RawStarMatter.getFluid(FluidStorageKeys.PLASMA, 15_728_640L)),
                10000,
                10000,
                0,
                null,
                null);
        modified.outputs.put(fluidCapability, List.of(fluidContent));
        return modified;
    }

    @Redirect(
              method = "recipeModifier",
              at = @At(
                       value = "INVOKE",
                       target = "Lcom/gregtechceu/gtceu/api/recipe/GTRecipe;copy()Lcom/gregtechceu/gtceu/api/recipe/GTRecipe;"),
              remap = false)
    private static @NotNull GTRecipe redirectCreateDataOutput(
                                                              GTRecipe instance,
                                                              @Local(argsOnly = true, ordinal = 0) MetaMachine machine) {
        return ((IHarmonyMachineAccessor) machine).applyCreateDataOutput(instance);
    }

    @Override
    public @NotNull Widget createUIWidget() {
        WidgetGroup group = (WidgetGroup) super.createUIWidget();
        SlotWidget slot = new SlotWidget(
                gtladditions$machineStorage,
                0,
                group.getSizeWidth() - 30,
                group.getSizeHeight() - 30,
                true,
                true);
        slot.setBackground(GuiTextures.SLOT);
        group.addWidget(slot);
        return group;
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    protected void StartupUpdate() {
        if (this.getOffsetTimer() % 20L == 0L) {
            this.oc = 0;
            if (this.hydrogen < 10000000000L && MachineIO.inputFluid(this, GTMaterials.Hydrogen.getFluid(100000000L))) this.hydrogen += 100000000L;
            if (this.helium < 10000000000L && MachineIO.inputFluid(this, GTMaterials.Helium.getFluid(100000000L))) this.helium += 100000000L;
            if (MachineIO.notConsumableCircuit(this, 4)) this.oc = 4;
            if (MachineIO.notConsumableCircuit(this, 3)) this.oc = 3;
            if (MachineIO.notConsumableCircuit(this, 2)) this.oc = 2;
            if (MachineIO.notConsumableCircuit(this, 1)) this.oc = 1;
        }
    }

    @Override
    public void addDisplayText(@NotNull List<Component> textList) {
        super.addDisplayText(textList);
        if (this.isFormed()) {
            if (userid != null) {
                var totalEu = WirelessEnergyManager.getUserEU(userid);
                textList.add(Component.translatable("gtmthings.machine.wireless_energy_monitor.tooltip.0",
                        TeamUtil.GetName(getLevel(), userid)));
                textList.add(Component.translatable("gtmthings.machine.wireless_energy_monitor.tooltip.1",
                        Component.literal(CommonUtils.formatBigIntegerFixed(totalEu)).withStyle(ChatFormatting.RED)));
            }
            textList.add(Component.translatable("tooltip.gtlcore.startup_energy_cost", FormattingUtil.formatNumbers(getStartupEnergy())));
            textList.add(Component.translatable("tooltip.gtlcore.hydrogen_storage", FormattingUtil.formatNumbers(hydrogen)));
            textList.add(Component.translatable("tooltip.gtlcore.helium_storage", FormattingUtil.formatNumbers(helium)));
        }
    }
}