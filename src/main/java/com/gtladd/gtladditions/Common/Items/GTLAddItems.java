package com.gtladd.gtladditions.Common.Items;

import net.minecraft.world.item.Item;

import com.gtladd.gtladditions.Common.Data.GTLAddCreativeModeTabs;
import com.tterrag.registrate.util.entry.ItemEntry;

import static com.gtladd.gtladditions.api.Registry.GTLAddRegistration.REGISTRATE;

public class GTLAddItems {

    public static ItemEntry<Item> ECHO_SHARD_BOULE;
    public static ItemEntry<Item> BIOWARE_ECHO_SHARD_BOULE;
    public static ItemEntry<Item> ECHO_SHARD_WAFER;
    public static ItemEntry<Item> HASSIUM_BOULE;
    public static ItemEntry<Item> HASSIUM_WAFER;
    public static ItemEntry<Item> STARMETAL_BOULE;
    public static ItemEntry<Item> STARMETAL_WAFER;
    public static ItemEntry<Item> DRAGON_ELEMENT_STARMETAL_WAFER;
    public static ItemEntry<Item> CHAOS_SOC;
    public static ItemEntry<Item> CHAOS_SOC_WAFER;
    public static ItemEntry<Item> EXTRAORDINARY_SOC;
    public static ItemEntry<Item> EXTRAORDINARY_SOC_WAFER;
    public static ItemEntry<Item> PREPARE_EXTRAORDINARY_SOC_WAFER;
    public static ItemEntry<Item> OUTSTANDING_SOC;
    public static ItemEntry<Item> OUTSTANDING_SOC_WAFER;

    public static void init() {}

    private static ItemEntry<Item> register(String id, String name) {
        return REGISTRATE.item(id, Item::new).lang(name).register();
    }

    static {
        REGISTRATE.creativeModeTab(() -> GTLAddCreativeModeTabs.GTLADD_ITEMS);
        ECHO_SHARD_BOULE = register("echo_shard_boule", "Echo Shard Boule");
        ECHO_SHARD_WAFER = register("echo_shard_wafer", "Echo Shard Wafer");
        HASSIUM_BOULE = register("hassium_boule", "Hassium Boule");
        HASSIUM_WAFER = register("hassium_wafer", "Hassium Wafer");
        STARMETAL_BOULE = register("starmetal_boule", "Starmetal Boule");
        STARMETAL_WAFER = register("starmetal_wafer", "Starmetal Wafer");
        BIOWARE_ECHO_SHARD_BOULE = register("bioware_echo_shard_wafer", "Bioware Echo Shard Wafer");
        DRAGON_ELEMENT_STARMETAL_WAFER = register("dragon_element_starmetal_wafer", "Dragon Element Starmetal Wafer");
        CHAOS_SOC = register("chaos_soc", "Chaos Soc");
        CHAOS_SOC_WAFER = register("chaos_soc_wafer", "Chaos Soc Wafer");
        EXTRAORDINARY_SOC = register("extraordinary_soc", "Extraordinary Soc");
        EXTRAORDINARY_SOC_WAFER = register("extraordinary_soc_wafer", "Extraordinary Soc Wafer");
        PREPARE_EXTRAORDINARY_SOC_WAFER = register("prepare_extraordinary_soc_wafer", "Prepare Extraordinary Soc Wafer");
        OUTSTANDING_SOC = register("outstanding_soc", "Outstanding Soc");
        OUTSTANDING_SOC_WAFER = register("outstanding_soc_wafer", "Outstanding Soc Wafer");
    }
}
