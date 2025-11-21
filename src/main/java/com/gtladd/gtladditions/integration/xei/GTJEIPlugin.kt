package com.gtladd.gtladditions.integration.xei

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.HELIOFLARE_POWER_FORGE
import com.lowdragmc.lowdraglib.LDLib
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.constants.RecipeTypes
import mezz.jei.api.registration.IRecipeCatalystRegistration
import net.minecraft.resources.ResourceLocation

@Suppress("unused")
@JeiPlugin
class GTJEIPlugin : IModPlugin {
    override fun getPluginUid(): ResourceLocation {
        return GTLAdditions.id("jei_plugin")
    }

    override fun registerRecipeCatalysts(registration: IRecipeCatalystRegistration) {
        if (LDLib.isReiLoaded() || LDLib.isEmiLoaded()) return
        registration.addRecipeCatalyst(HELIOFLARE_POWER_FORGE.asStack(), RecipeTypes.SMELTING)
    }
}
