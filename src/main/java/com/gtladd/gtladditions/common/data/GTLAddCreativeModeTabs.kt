package com.gtladd.gtladditions.common.data

import com.gregtechceu.gtceu.common.data.GTCreativeModeTabs.RegistrateDisplayItemsGenerator
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.registry.GTLAddRegistration.REGISTRATE
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks
import com.gtladd.gtladditions.common.items.GTLAddItems
import com.tterrag.registrate.util.entry.RegistryEntry
import net.minecraft.world.item.CreativeModeTab
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachine

object GTLAddCreativeModeTabs {
    @JvmField
    val GTLADD_ITEMS: RegistryEntry<CreativeModeTab?> = REGISTRATE.defaultCreativeTab("item")
    { builder : CreativeModeTab.Builder? ->
        builder !!.displayItems(RegistrateDisplayItemsGenerator("item", REGISTRATE))
            .title(
                REGISTRATE.addLang("itemGroup", GTLAdditions.id("item"), "GTLAdditions")
            )
            .icon { GTLAddItems.STARMETAL_BOULE.asStack() }
            .build()
    }
        .register()
    @JvmField
    val GTLADD_MACHINE: RegistryEntry<CreativeModeTab?> = REGISTRATE.defaultCreativeTab("machine")
    { builder : CreativeModeTab.Builder? ->
        builder !!.displayItems(RegistrateDisplayItemsGenerator("machine", REGISTRATE))
            .title(
                REGISTRATE.addLang("itemGroup", GTLAdditions.id("machine"), "GTLAdditions")
            )
            .icon { AdvancedMultiBlockMachine.EYE_OF_HARMONY.asStack() }
            .build()
    }
        .register()

    @JvmField
    val GTLADD_BLOCKS: RegistryEntry<CreativeModeTab?> = REGISTRATE.defaultCreativeTab("block")
    { builder : CreativeModeTab.Builder? ->
        builder !!.displayItems(RegistrateDisplayItemsGenerator("block", REGISTRATE))
            .title(
                REGISTRATE.addLang("itemGroup", GTLAdditions.id("block"), "GTLAdditions")
            )
            .icon { GTLAddBlocks.QUANTUM_GLASS.asStack() }
            .build()
    }
        .register()

    @JvmStatic
    fun init() {}
}
