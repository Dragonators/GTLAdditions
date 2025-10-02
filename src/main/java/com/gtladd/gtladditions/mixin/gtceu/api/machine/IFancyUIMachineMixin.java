package com.gtladd.gtladditions.mixin.gtceu.api.machine;

import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CombinedDirectionalFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.MachineModeFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineFeature;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;

import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(IFancyUIMachine.class)
public interface IFancyUIMachineMixin extends IMachineFeature {

    /**
     * @author Dragons
     * @reason 跨配方种类机器不显示MachineMode Configurator
     */
    @Overwrite(remap = false)
    default void attachSideTabs(TabsWidget sideTabs) {
        sideTabs.setMainTab((IFancyUIMachine) this);

        if (this instanceof IRecipeLogicMachine rLMachine && rLMachine.getRecipeTypes().length > 1 && !(GTLAddRecipesTypes.MULTIPLE_TYPE_RECIPES.contains(rLMachine.getRecipeType()))) {
            sideTabs.attachSubTab(new MachineModeFancyConfigurator(rLMachine));
        }

        var directionalConfigurator = CombinedDirectionalFancyConfigurator.of(self(), self());
        if (directionalConfigurator != null)
            sideTabs.attachSubTab(directionalConfigurator);
    }
}
