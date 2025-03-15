package com.gtladd.gtladditions.common.items;

import net.minecraft.world.item.Item;

import com.gtladd.gtladditions.common.data.GTLAddCreativeModeTabs;
import com.tterrag.registrate.util.entry.ItemEntry;

import static com.gtladd.gtladditions.api.registry.GTLAddRegistration.REGISTRATE;

public class GTLAddItems {

    public static ItemEntry<Item> ECHO_SHARD_BOULE;
    public static ItemEntry<Item> ECHO_SHARD_WAFER;
    public static ItemEntry<Item> BIOWARE_ECHO_SHARD_BOULE;
    public static ItemEntry<Item> OUTSTANDING_SOC_WAFER;
    public static ItemEntry<Item> OUTSTANDING_SOC;
    public static ItemEntry<Item> HASSIUM_BOULE;
    public static ItemEntry<Item> HASSIUM_WAFER;
    public static ItemEntry<Item> PREPARE_EXTRAORDINARY_SOC_WAFER;
    public static ItemEntry<Item> EXTRAORDINARY_SOC_WAFER;
    public static ItemEntry<Item> EXTRAORDINARY_SOC;
    public static ItemEntry<Item> STARMETAL_BOULE;
    public static ItemEntry<Item> STARMETAL_WAFER;
    public static ItemEntry<Item> DRAGON_ELEMENT_STARMETAL_WAFER;
    public static ItemEntry<Item> CHAOS_SOC_WAFER;
    public static ItemEntry<Item> CHAOS_SOC;
    public static ItemEntry<Item> PERIODICIUM_BOULE;
    public static ItemEntry<Item> PERIODICIUM_WAFER;
    public static ItemEntry<Item> PREPARE_SPACETIME_SOC_WAFER;
    public static ItemEntry<Item> SPACETIME_SOC_WAFER;
    public static ItemEntry<Item> SPACETIME_SOC;
    public static ItemEntry<Item> INFINITY_BOULE;
    public static ItemEntry<Item> INFINITY_WAFER;
    public static ItemEntry<Item> PREPARE_PRIMARY_SOC_WAFER;
    public static ItemEntry<Item> PRIMARY_SOC_WAFER;
    public static ItemEntry<Item> PRIMARY_SOC;
    public static ItemEntry<Item> SPACETIME_LENS;

    public static void init() {}

    private static ItemEntry<Item> register(String id, String name) {
        return REGISTRATE.item(id, Item::new).lang(name).register();
    }

    static {
        REGISTRATE.creativeModeTab(() -> GTLAddCreativeModeTabs.GTLADD_ITEMS);
        SPACETIME_LENS = register("spacetime_lens", "Spacetime Lens");
        ECHO_SHARD_BOULE = register("echo_shard_boule", "Echo Shard Boule");
        ECHO_SHARD_WAFER = register("echo_shard_wafer", "Echo Shard Wafer");
        BIOWARE_ECHO_SHARD_BOULE = register("bioware_echo_shard_wafer", "Bioware Echo Shard Wafer");
        OUTSTANDING_SOC_WAFER = register("outstanding_soc_wafer", "Outstanding Soc Wafer");
        OUTSTANDING_SOC = register("outstanding_soc", "Outstanding Soc");
        HASSIUM_BOULE = register("hassium_boule", "Hassium Boule");
        HASSIUM_WAFER = register("hassium_wafer", "Hassium Wafer");
        PREPARE_EXTRAORDINARY_SOC_WAFER = register("prepare_extraordinary_soc_wafer", "Prepare Extraordinary Soc Wafer");
        EXTRAORDINARY_SOC_WAFER = register("extraordinary_soc_wafer", "Extraordinary Soc Wafer");
        EXTRAORDINARY_SOC = register("extraordinary_soc", "Extraordinary Soc");
        STARMETAL_BOULE = register("starmetal_boule", "Starmetal Boule");
        STARMETAL_WAFER = register("starmetal_wafer", "Starmetal Wafer");
        DRAGON_ELEMENT_STARMETAL_WAFER = register("dragon_element_starmetal_wafer", "Dragon Element Starmetal Wafer");
        CHAOS_SOC_WAFER = register("chaos_soc_wafer", "Chaos Soc Wafer");
        CHAOS_SOC = register("chaos_soc", "Chaos Soc");
        PERIODICIUM_BOULE = register("periodicium_boule", "Periodicuim Boule");
        PERIODICIUM_WAFER = register("periodicium_wafer", "Periodicuim Wafer");
        PREPARE_SPACETIME_SOC_WAFER = register("prepare_spacetime_soc_wafer", "Prepare Spacetime Soc Wafer");
        SPACETIME_SOC_WAFER = register("spacetime_soc_wafer", "Spacetime Soc Wafer");
        SPACETIME_SOC = register("spacetime_soc", "Spacetime Soc");
        INFINITY_BOULE = register("infinity_boule", "Infinity Boule");
        INFINITY_WAFER = register("infinity_wafer", "Infinity Wafer");
        PREPARE_PRIMARY_SOC_WAFER = register("prepare_primary_soc_wafer", "Prepare Primary Soc Wafer");
        PRIMARY_SOC_WAFER = register("primary_soc_wafer", "Primary Soc Wafer");
        PRIMARY_SOC = register("primary_soc", "Primary Soc");
    }
}
