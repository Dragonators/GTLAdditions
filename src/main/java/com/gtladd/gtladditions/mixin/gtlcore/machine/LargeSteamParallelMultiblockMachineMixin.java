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
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import com.gtladd.gtladditions.common.machine.GTLAddMachine;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mixin(LargeSteamParallelMultiblockMachine.class)
public class LargeSteamParallelMultiblockMachineMixin extends WorkableMultiblockMachine {

    public LargeSteamParallelMultiblockMachineMixin(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    @Unique
    private static boolean gtladditions$isLarge;
    @Unique
    private static boolean gtladditions$isHuge;
    @Unique
    @Persisted
    private static int gtladditions$amountOC;
    @Unique
    private static int gtladditions$parallel;
    @Shadow(remap = false)
    private boolean isOC;
    @Shadow(remap = false)
    private int amountOC;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    public void LargeSteamParallelMultiblockMachine(IMachineBlockEntity holder, int maxParallels, Object[] args, CallbackInfo ci) {
        gtladditions$parallel = maxParallels;
    }

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public static GTRecipe recipeModifier(MetaMachine machine, @NotNull GTRecipe recipe, double reductionDuration) {
        if (machine instanceof LargeSteamParallelMultiblockMachine) {
            if (RecipeHelper.getInputEUt(recipe) > (long) (gtladditions$isHuge ? 512 : (gtladditions$isLarge ? 128 : 32))) return null;
            GTRecipe result = GTRecipeModifiers.accurateParallel(machine, recipe, gtladditions$parallel, false).getFirst();
            recipe = result == recipe ? result.copy() : result;
            recipe.duration = gtladditions$isHuge ? 1 : (int) Math.max(1.0, (double) recipe.duration * reductionDuration / (gtladditions$isLarge ? Math.pow(2.0, gtladditions$amountOC) : 1.0));
        }
        return recipe;
    }

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public void onStructureFormed() {
        super.onStructureFormed();
        List<IRecipeHandler<?>> handlers = this.capabilitiesProxy.get(IO.IN, FluidRecipeCapability.CAP);
        if (handlers != null) {
            Iterator<IRecipeHandler<?>> itr = handlers.iterator();
            while (itr.hasNext()) {
                IRecipeHandler<?> handler = itr.next();
                if (handler instanceof NotifiableFluidTank) {
                    NotifiableFluidTank tank = (NotifiableFluidTank) handler;
                    if (tank.getFluidInTank(0).isFluidEqual(GTMaterials.Steam.getFluid(1L))) {
                        gtladditions$isLarge = tank.getMachine().getDefinition() == GTLMachines.LARGE_STEAM_HATCH;
                        gtladditions$isHuge = tank.getMachine().getDefinition() == GTLAddMachine.HUGE_STEAM_HATCH;
                        this.isOC = gtladditions$isLarge || gtladditions$isHuge;
                        Object2IntMap<RecipeCapability<?>> recipeOutputLimits = new Object2IntOpenHashMap<>();
                        recipeOutputLimits.put(ItemRecipeCapability.CAP, gtladditions$isHuge ? 3 : 1);
                        MultiBlockMachineA.LARGE_STEAM_MACERATOR.setRecipeOutputLimits(recipeOutputLimits);
                        itr.remove();
                        if (!this.capabilitiesProxy.contains(IO.IN, EURecipeCapability.CAP)) this.capabilitiesProxy.put(IO.IN, EURecipeCapability.CAP, new ArrayList());
                        (this.capabilitiesProxy.get(IO.IN, EURecipeCapability.CAP))
                                .add(new SteamEnergyRecipeHandler(tank, 0.5 * (gtladditions$isHuge ? 250.0 : (gtladditions$isLarge ? Math.pow(3.0, gtladditions$amountOC) : 1.0))));
                        return;
                    }
                }
            }
        }
    }

    @Inject(method = "addDisplayText", at = @At(value = "INVOKE", target = "Lcom/gregtechceu/gtceu/api/machine/trait/RecipeLogic;isWaiting()Z", shift = At.Shift.BEFORE), remap = false, cancellable = true)
    public void addDisplayText(List<Component> textList, CallbackInfo ci) {
        if (this.recipeLogic.isWaiting()) textList.add(Component.translatable("gtceu.multiblock.steam.low_steam").setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
        if (gtladditions$isHuge) {
            textList.add(Component.translatable("gtceu.multiblock.steam_duration_modify"));
        } else if (this.isOC) {
            textList.add(Component.translatable("gtceu.multiblock.oc_amount", gtladditions$amountOC).withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("gtceu.multiblock.steam_parallel_machine.oc")))));
            textList.add(Component.translatable("gtceu.multiblock.steam_parallel_machine.modification_oc")
                    .append(ComponentPanelWidget.withButton(Component.literal("[-] "), "ocSub"))
                    .append(ComponentPanelWidget.withButton(Component.literal("[+]"), "ocAdd")));
        }
        ci.cancel();
    }

    @Inject(method = "handleDisplayClick", at = @At("RETURN"), remap = false)
    public void handleDisplayClick(String componentData, ClickData clickData, CallbackInfo ci) {
        gtladditions$amountOC = this.amountOC;
    }
}
