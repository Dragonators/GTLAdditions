package com.gtladd.gtladditions

import com.gregtechceu.gtceu.api.GTCEuAPI
import com.gregtechceu.gtceu.api.data.chemical.material.event.MaterialEvent
import com.gregtechceu.gtceu.api.data.chemical.material.event.MaterialRegistryEvent
import com.gregtechceu.gtceu.api.machine.MachineDefinition
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gtladd.gtladditions.api.registry.GTLAddRegistration
import com.gtladd.gtladditions.common.machine.GTLAddMachines
import com.gtladd.gtladditions.common.material.GTLAddMaterial
import com.gtladd.gtladditions.common.material.MaterialAdd
import com.gtladd.gtladditions.common.modify.GTLAddCreativeModeTabs
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import com.gtladd.gtladditions.config.ConfigHolder
import com.gtladd.gtladditions.network.GTLAddNetworking
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Mod(GTLAdditions.MOD_ID)
class GTLAdditions {
    init {
        GTLAddCreativeModeTabs.init()
        ConfigHolder.init()
        GTLAddNetworking.init()

        val modEventBus = FMLJavaModLoadingContext.get().modEventBus
        MinecraftForge.EVENT_BUS.register(this)
        modEventBus.register(this)
        GTLAddRegistration.REGISTRATE.registerEventListeners(modEventBus)

        modEventBus.addGenericListener(GTRecipeType::class.java) { _: GTCEuAPI.RegisterEvent<ResourceLocation, GTRecipeType> ->
            GTLAddRecipesTypes.init()
        }
        modEventBus.addGenericListener(MachineDefinition::class.java) { _: GTCEuAPI.RegisterEvent<ResourceLocation, MachineDefinition> ->
            GTLAddMachines.init()
        }
    }

    @SubscribeEvent
    fun onMaterialRegistry(event: MaterialRegistryEvent) {
        GTCEuAPI.materialManager.createRegistry(MOD_ID)
    }

    @SubscribeEvent
    fun onMaterialRegister(event: MaterialEvent) {
        MaterialAdd.init()
        GTLAddMaterial.init()
    }

    companion object {
        const val MOD_ID = "gtladditions"
        const val NAME = "GTLAdditions"
        val LOGGER: Logger = LogManager.getLogger(NAME)

        @JvmStatic
        fun id(name: String) = ResourceLocation(MOD_ID, name)
    }
}
