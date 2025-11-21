package com.gtladd.gtladditions.common.items

import com.gregtechceu.gtceu.api.item.ComponentItem
import com.gregtechceu.gtceu.common.data.GTItems
import com.gregtechceu.gtceu.common.item.TooltipBehavior
import com.gtladd.gtladditions.api.registry.GTLAddRegistration.Companion.REGISTRATE
import com.gtladd.gtladditions.common.modify.GTLAddCreativeModeTabs
import com.gtladd.gtladditions.common.items.behavior.AstralArrayBehavior
import com.gtladd.gtladditions.common.items.behavior.ModuleConnectionBehavior
import com.tterrag.registrate.util.entry.ItemEntry
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item

object GTLAddItems {
    val ECHO_SHARD_BOULE: ItemEntry<Item?>
    val ECHO_SHARD_WAFER: ItemEntry<Item?>
    val BIOWARE_ECHO_SHARD_BOULE: ItemEntry<Item?>
    val OUTSTANDING_SOC_WAFER: ItemEntry<Item?>
    val OUTSTANDING_SOC: ItemEntry<Item?>
    val HASSIUM_BOULE: ItemEntry<Item?>
    val HASSIUM_WAFER: ItemEntry<Item?>
    val PREPARE_EXTRAORDINARY_SOC_WAFER: ItemEntry<Item?>
    val EXTRAORDINARY_SOC_WAFER: ItemEntry<Item?>
    val EXTRAORDINARY_SOC: ItemEntry<Item?>
    val STARMETAL_BOULE: ItemEntry<Item?>
    val STARMETAL_WAFER: ItemEntry<Item?>
    val DRAGON_ELEMENT_STARMETAL_WAFER: ItemEntry<Item?>
    val CHAOS_SOC_WAFER: ItemEntry<Item?>
    val CHAOS_SOC: ItemEntry<Item?>
    val PERIODICIUM_BOULE: ItemEntry<Item?>
    val PERIODICIUM_WAFER: ItemEntry<Item?>
    val PREPARE_SPACETIME_SOC_WAFER: ItemEntry<Item?>
    val SPACETIME_SOC_WAFER: ItemEntry<Item?>
    val SPACETIME_SOC: ItemEntry<Item?>
    val INFINITY_BOULE: ItemEntry<Item?>
    val INFINITY_WAFER: ItemEntry<Item?>
    val PREPARE_PRIMARY_SOC_WAFER: ItemEntry<Item?>
    val PRIMARY_SOC_WAFER: ItemEntry<Item?>
    val PRIMARY_SOC: ItemEntry<Item?>
    val SPACETIME_LENS: ItemEntry<Item?>
    val PHONONIC_SEED_CRYSTAL: ItemEntry<Item?>
    val THERMAL_SUPERCONDUCTOR: ItemEntry<Item?>
    val RELATIVISTIC_HEAT_CAPACITOR: ItemEntry<Item?>
    val DEBUG_MODULE_CONNECTOR: ItemEntry<ComponentItem>
    val STRANGE_ANNIHILATION_FUEL_ROD: ItemEntry<ComponentItem>
    val BLACK_HOLE_SEED: ItemEntry<ComponentItem>
    val ASTRAL_ARRAY: ItemEntry<ComponentItem>

    fun init() {}

    private fun register(id: String, name: String): ItemEntry<Item?> {
        return REGISTRATE.item(id) { properties: Item.Properties -> Item(properties) }.lang(name).register()
    }

    init {
        REGISTRATE.creativeModeTab { GTLAddCreativeModeTabs.GTLADD_ITEMS }
        SPACETIME_LENS = register("spacetime_lens", "Spacetime Lens")
        ECHO_SHARD_BOULE = register("echo_shard_boule", "Echo Shard Boule")
        ECHO_SHARD_WAFER = register("echo_shard_wafer", "Echo Shard Wafer")
        BIOWARE_ECHO_SHARD_BOULE = register("bioware_echo_shard_wafer", "Bioware Echo Shard Wafer")
        OUTSTANDING_SOC_WAFER = register("outstanding_soc_wafer", "Outstanding Soc Wafer")
        OUTSTANDING_SOC = register("outstanding_soc", "Outstanding Soc")
        HASSIUM_BOULE = register("hassium_boule", "Hassium Boule")
        HASSIUM_WAFER = register("hassium_wafer", "Hassium Wafer")
        PREPARE_EXTRAORDINARY_SOC_WAFER = register("prepare_extraordinary_soc_wafer", "Prepare Extraordinary Soc Wafer")
        EXTRAORDINARY_SOC_WAFER = register("extraordinary_soc_wafer", "Extraordinary Soc Wafer")
        EXTRAORDINARY_SOC = register("extraordinary_soc", "Extraordinary Soc")
        STARMETAL_BOULE = register("starmetal_boule", "Starmetal Boule")
        STARMETAL_WAFER = register("starmetal_wafer", "Starmetal Wafer")
        DRAGON_ELEMENT_STARMETAL_WAFER = register("dragon_element_starmetal_wafer", "Dragon Element Starmetal Wafer")
        CHAOS_SOC_WAFER = register("chaos_soc_wafer", "Chaos Soc Wafer")
        CHAOS_SOC = register("chaos_soc", "Chaos Soc")
        PERIODICIUM_BOULE = register("periodicium_boule", "Periodicuim Boule")
        PERIODICIUM_WAFER = register("periodicium_wafer", "Periodicuim Wafer")
        PREPARE_SPACETIME_SOC_WAFER = register("prepare_spacetime_soc_wafer", "Prepare Spacetime Soc Wafer")
        SPACETIME_SOC_WAFER = register("spacetime_soc_wafer", "Spacetime Soc Wafer")
        SPACETIME_SOC = register("spacetime_soc", "Spacetime Soc")
        INFINITY_BOULE = register("infinity_boule", "Infinity Boule")
        INFINITY_WAFER = register("infinity_wafer", "Infinity Wafer")
        PREPARE_PRIMARY_SOC_WAFER = register("prepare_primary_soc_wafer", "Prepare Primary Soc Wafer")
        PRIMARY_SOC_WAFER = register("primary_soc_wafer", "Primary Soc Wafer")
        PRIMARY_SOC = register("primary_soc", "Primary Soc")
        PHONONIC_SEED_CRYSTAL = register("phononic_seed_crystal", "Phononic Seed Crystal")
        THERMAL_SUPERCONDUCTOR = register("thermal_superconductor", "Thermal Superconductor")
        RELATIVISTIC_HEAT_CAPACITOR = register("relativistic_heat_capacitor", "Relativistic Heat Capacitor")
        DEBUG_MODULE_CONNECTOR = REGISTRATE.item("debug_module_connector") { properties: Item.Properties -> ComponentItem.create(properties) }
            .onRegister(
                GTItems.attach(
                    TooltipBehavior
                    { lines: MutableList<Component> ->
                        lines.add(Component.translatable("gtladditions.item.debug_module_connector.tooltips.0"))
                        lines.add(Component.translatable("gtladditions.item.debug_module_connector.tooltips.1"))
                        lines.add(Component.translatable("gtladditions.item.debug_module_connector.tooltips.2"))
                    }, ModuleConnectionBehavior
                )
            )
            .lang("Debug Module Connector")
            .register()
        STRANGE_ANNIHILATION_FUEL_ROD = REGISTRATE.item("strange_annihilation_fuel_rod") { properties: Item.Properties -> ComponentItem.create(properties) }
            .onRegister(
                GTItems.attach(
                    TooltipBehavior
                    { lines: MutableList<Component> ->
                        lines.add(Component.translatable("gtladditions.item.strange_annihilation_fuel_rod.tooltips.0"))
                    })
            )
            .lang("Strange Annihilation Fuel Rod")
            .register()
        BLACK_HOLE_SEED = REGISTRATE.item("black_hole_seed") { properties: Item.Properties -> ComponentItem.create(properties) }
            .onRegister(
                GTItems.attach(
                    TooltipBehavior
                    { lines: MutableList<Component> ->
                        lines.add(Component.translatable("gtladditions.item.black_hole_seed.tooltips.0"))
                    })
            )
            .lang("Black Hole Seed")
            .register()
        ASTRAL_ARRAY = REGISTRATE.item("astral_array") { properties: Item.Properties -> ComponentItem.create(properties) }
            .onRegister(
                GTItems.attach(
                    TooltipBehavior
                    { lines: MutableList<Component> ->
                        lines.add(Component.translatable("gtladditions.item.astral_array.tooltips.0"))
                        lines.add(Component.translatable("gtladditions.item.astral_array.tooltips.1"))
                        lines.add(Component.translatable("gtladditions.item.astral_array.tooltips.2"))
                        lines.add(Component.translatable("gtladditions.item.astral_array.tooltips.3"))
                        lines.add(Component.translatable("gtladditions.item.astral_array.tooltips.4"))
                    }, AstralArrayBehavior)
            )
            .lang("Astral Array")
            .register()
    }
}
