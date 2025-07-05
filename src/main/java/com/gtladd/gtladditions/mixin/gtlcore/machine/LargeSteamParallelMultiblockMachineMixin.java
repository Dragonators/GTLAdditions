package com.gtladd.gtladditions.mixin.gtlcore.machine;

import org.gtlcore.gtlcore.common.data.GTLMachines;
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineA;
import org.gtlcore.gtlcore.common.machine.multiblock.steam.LargeSteamParallelMultiblockMachine;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.steam.SteamEnergyRecipeHandler;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import com.gtladd.gtladditions.common.machine.GTLAddMachines;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Mixin(LargeSteamParallelMultiblockMachine.class)
public class LargeSteamParallelMultiblockMachineMixin extends WorkableMultiblockMachine {

    public LargeSteamParallelMultiblockMachineMixin(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    @Unique
    private boolean gtladditions$isLarge;
    @Unique
    private boolean gtladditions$isHuge;
    @Shadow(remap = false)
    private boolean isOC;
    @Shadow(remap = false)
    private int amountOC;
    @Shadow(remap = false)
    @Final
    private int max_parallels;

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public static GTRecipe recipeModifier(MetaMachine machine, @NotNull GTRecipe recipe, double reductionDuration) {
        if (machine instanceof LargeSteamParallelMultiblockMachine machine1) {
            LargeSteamParallelMultiblockMachineMixin mixin = (LargeSteamParallelMultiblockMachineMixin) (Object) machine1;
            boolean isHuge = mixin.gtladditions$isHuge;
            boolean isLarge = mixin.gtladditions$isLarge;
            if (RecipeHelper.getInputEUt(recipe) > (long) (isHuge ? 512 : (isLarge ? 128 : 32))) return null;
            GTRecipe result = GTRecipeModifiers.accurateParallel(machine, recipe, mixin.max_parallels, false).getFirst();
            recipe = result == recipe ? result.copy() : result;
            if (isHuge) reductionDuration = 0.0;
            if (machine1.getDefinition() == MultiBlockMachineA.DIMENSIONALLY_TRANSCENDENT_STEAM_OVEN) recipe.tickInputs.put(EURecipeCapability.CAP, List.of(new Content((long) Math.max(1.0, (double) RecipeHelper.getInputEUt(recipe) * 0.01), ChanceLogic.getMaxChancedValue(), ChanceLogic.getMaxChancedValue(), 0, null, null)));
            recipe.duration = (int) Math.max(1.0, (double) recipe.duration * reductionDuration / (isLarge ? Math.pow(2.0, mixin.amountOC) : 1.0));
        }
        return recipe;
    }

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public void onStructureFormed() {
        super.onStructureFormed();
        List<IRecipeHandler<?>> handlers = this.capabilitiesProxy.get(IO.IN, FluidRecipeCapability.CAP);
        if (handlers != null) {
            Iterator<IRecipeHandler<?>> itr = handlers.iterator();
            while (itr.hasNext()) {
                IRecipeHandler<?> handler = itr.next();
                if (handler instanceof NotifiableFluidTank tank) {
                    if (tank.getFluidInTank(0).isFluidEqual(GTMaterials.Steam.getFluid(1L))) {
                        gtladditions$isLarge = tank.getMachine().getDefinition() == GTLMachines.LARGE_STEAM_HATCH;
                        gtladditions$isHuge = tank.getMachine().getDefinition() == GTLAddMachines.HUGE_STEAM_HATCH;
                        this.isOC = gtladditions$isLarge || gtladditions$isHuge;
                        Object2IntMap<RecipeCapability<?>> recipeOutputLimits = new Object2IntOpenHashMap<>();
                        recipeOutputLimits.put(ItemRecipeCapability.CAP, gtladditions$isHuge ? 3 : 1);
                        MultiBlockMachineA.LARGE_STEAM_MACERATOR.setRecipeOutputLimits(recipeOutputLimits);
                        itr.remove();
                        if (!this.capabilitiesProxy.contains(IO.IN, EURecipeCapability.CAP)) this.capabilitiesProxy.put(IO.IN, EURecipeCapability.CAP, new ArrayList<>());
                        (Objects.requireNonNull(this.capabilitiesProxy.get(IO.IN, EURecipeCapability.CAP)))
                                .add(new SteamEnergyRecipeHandler(tank, 0.5 * (gtladditions$isHuge ? 250.0 : (gtladditions$isLarge ? Math.pow(3.0, amountOC) : 1.0))));
                        return;
                    }
                }
            }
        }
    }

    @Inject(method = "addDisplayText", at = @At(value = "INVOKE", target = "Lcom/gregtechceu/gtceu/api/machine/trait/RecipeLogic;isWaiting()Z", shift = At.Shift.BEFORE), remap = false, cancellable = true)
    public void addDisplayText(List<Component> textList, CallbackInfo ci) {
        if (this.recipeLogic.isWaiting()) textList.add(Component.translatable("gtceu.multiblock.steam.low_steam").setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
        if (gtladditions$isHuge) textList.add(Component.translatable("gtceu.multiblock.steam_duration_modify"));
        else if (gtladditions$isLarge) {
            textList.add(Component.translatable("gtceu.multiblock.oc_amount", amountOC)
                    .withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("gtceu.multiblock.steam_parallel_machine.oc")))));
            textList.add(Component.translatable("gtceu.multiblock.steam_parallel_machine.modification_oc")
                    .append(ComponentPanelWidget.withButton(Component.literal("[-] "), "ocSub"))
                    .append(ComponentPanelWidget.withButton(Component.literal("[+]"), "ocAdd")));
        }
        ci.cancel();
    }
}
