package com.gtladd.gtladditions.integration.xei;

import com.lowdragmc.lowdraglib.LDLib;

import net.minecraft.resources.ResourceLocation;

import com.gtladd.gtladditions.GTLAdditions;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import org.jetbrains.annotations.NotNull;

import static com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.HELIOFLARE_POWER_FORGE;

@JeiPlugin
public class GTJEIPlugin implements IModPlugin {

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return GTLAdditions.id("jei_plugin");
    }

    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        if (LDLib.isReiLoaded() || LDLib.isEmiLoaded()) return;
        registration.addRecipeCatalyst(HELIOFLARE_POWER_FORGE.asStack(), RecipeTypes.SMELTING);
    }
}
