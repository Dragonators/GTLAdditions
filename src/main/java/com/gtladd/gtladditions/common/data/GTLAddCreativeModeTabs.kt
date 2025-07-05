package com.gtladd.gtladditions.common.data;

import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachine;

import com.gregtechceu.gtceu.common.data.GTCreativeModeTabs;

import net.minecraft.world.item.CreativeModeTab;

import com.gtladd.gtladditions.GTLAdditions;
import com.gtladd.gtladditions.common.items.GTLAddItems;
import com.tterrag.registrate.util.entry.RegistryEntry;

import static com.gtladd.gtladditions.api.registry.GTLAddRegistration.REGISTRATE;

public class GTLAddCreativeModeTabs {

    public static RegistryEntry<CreativeModeTab> GTLADD_ITEMS = REGISTRATE.defaultCreativeTab("item",
            builder -> builder.displayItems(new GTCreativeModeTabs.RegistrateDisplayItemsGenerator("item", REGISTRATE))
                    .title(REGISTRATE.addLang("itemGroup", GTLAdditions.id("item"), "GTLAdditions"))
                    .icon(GTLAddItems.STARMETAL_BOULE::asStack)
                    .build())
            .register();
    public static RegistryEntry<CreativeModeTab> GTLADD_MACHINE = REGISTRATE.defaultCreativeTab("machine",
            builder -> builder.displayItems(new GTCreativeModeTabs.RegistrateDisplayItemsGenerator("machine", REGISTRATE))
                    .title(REGISTRATE.addLang("itemGroup", GTLAdditions.id("machine"), "GTLAdditions"))
                    .icon(AdvancedMultiBlockMachine.EYE_OF_HARMONY::asStack)
                    .build())
            .register();

    public static void init() {}
}
