package com.gtladd.gtladditions

import com.gregtechceu.gtceu.api.GTCEuAPI
import com.gregtechceu.gtceu.api.data.chemical.material.event.MaterialEvent
import com.gregtechceu.gtceu.api.data.chemical.material.event.MaterialRegistryEvent
import com.gregtechceu.gtceu.api.machine.MachineDefinition
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gtladd.gtladditions.api.registry.GTLAddRegistration
import com.gtladd.gtladditions.common.machine.GTLAddMachines
import com.gtladd.gtladditions.common.material.GTLAddMaterial
import com.gtladd.gtladditions.common.material.MaterialModify
import com.gtladd.gtladditions.common.modify.GTLAddCreativeModeTabs
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import com.gtladd.gtladditions.config.ConfigHolder
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Mod(GTLAdditions.MOD_ID)
class GTLAdditions {
    init {
        GTLAddCreativeModeTabs.init()
        ConfigHolder.init()

        val modEventBus = FMLJavaModLoadingContext.get().modEventBus
        modEventBus.addListener { _: MaterialRegistryEvent ->
            GTCEuAPI.materialManager.createRegistry(MOD_ID)
        }
        modEventBus.addListener { _: MaterialEvent ->
            MaterialModify.init()
            GTLAddMaterial.init()
        }
        GTLAddRegistration.REGISTRATE.registerEventListeners(modEventBus)

        modEventBus.addGenericListener(GTRecipeType::class.java) { _: GTCEuAPI.RegisterEvent<ResourceLocation, GTRecipeType> ->
            GTLAddRecipesTypes.init()
        }
        modEventBus.addGenericListener(MachineDefinition::class.java) { _: GTCEuAPI.RegisterEvent<ResourceLocation, MachineDefinition> ->
            GTLAddMachines.init()
        }
    }

    companion object {
        const val MOD_ID = "gtladditions"
        const val NAME = "GTLAdditions"
        val LOGGER: Logger = LogManager.getLogger(NAME)

        @JvmStatic
        fun id(name: String) = ResourceLocation(MOD_ID, name)
    }
}