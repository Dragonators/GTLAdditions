package com.gtladd.gtladditions.integration.stargate

import com.google.gson.JsonElement
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.level.material.Fluid
import net.minecraftforge.registries.ForgeRegistries

object StargateContent {
    private const val NAMESPACE = "sgjourney"

    private val disabledItems = setOf(
        "raw_naquadah", "naquadah_ingot", "naquadah_nugget",
        "refined_naquadah", "pure_naquadah", "naquadah",
        "naquadah_copper_mixture", "naquadah_copper_alloy", "naquadah_copper_nugget",
        "naquadah_iron_mixture", "naquadah_iron_alloy", "naquadah_iron_nugget",
        "raw_naquadria", "raw_trinium", "trinium_ingot",
        "trinium_nugget", "naquadah_rod", "naquadah_iron_rod",
        "naquadah_copper_rod", "trinium_rod", "naquadah_ore",
        "deepslate_naquadah_ore", "nether_naquadah_ore", "naquadria_ore",
        "deepslate_naquadria_ore", "nether_naquadria_ore", "trinium_ore",
        "deepslate_trinium_ore", "nether_trinium_ore", "raw_naquadah_block",
        "pure_naquadah_block", "raw_naquadria_block", "raw_trinium_block",
        "naquadah_block", "naquadah_copper_block", "naquadah_iron_block",
        "trinium_block",
        "naquadah_graver", "naquadah_sword", "naquadah_pickaxe",
        "naquadah_axe", "naquadah_shovel", "naquadah_hoe",
        "naquadah_helmet", "naquadah_chestplate", "naquadah_leggings",
        "naquadah_boots", "trinium_graver", "trinium_sword",
        "trinium_pickaxe", "trinium_axe", "trinium_shovel",
        "trinium_hoe", "trinium_arrow", "trinium_helmet",
        "trinium_chestplate", "trinium_leggings", "trinium_boots",
        // Our recipes still need reaction_chamber and crystal_base.
        "liquid_naquadah_bucket", "heavy_liquid_naquadah_bucket", "vial",
        "naquadah_liquidizer", "heavy_naquadah_liquidizer", "crystallizer",
        "advanced_crystallizer", "naquadah_reactor", "naquadah_generator_mark_i",
        "naquadah_generator_mark_ii", "naquadah_generator_core", "fusion_core",
        "naquadah_fuel_rod", "naquadah_power_cell", "plasma_converter",
        "large_naquadah_battery", "naquadah_wire", "small_naquadah_cable",
        "medium_naquadah_cable", "large_naquadah_cable", "zero_point_module",
        "zpm_holder", "zpm_plug", "zpm_port",
        "zpm_hub"
    )

    private val disabledFluids = setOf(
        "liquid_naquadah",
        "flowing_liquid_naquadah",
        "heavy_liquid_naquadah",
        "flowing_heavy_liquid_naquadah"
    )

    private val disabledRecipeTypes = setOf(
        "crystallizing",
        "advanced_crystallizing",
        "naquadah_liquidizing",
        "naquadah_heavy_liquidizing"
    )

    fun isDisabledItem(id: ResourceLocation?): Boolean = id?.namespace == NAMESPACE && id.path in disabledItems

    fun isDisabledItem(item: Item): Boolean = isDisabledItem(ForgeRegistries.ITEMS.getKey(item))

    fun isDisabledFluid(id: ResourceLocation?): Boolean =
        id?.namespace == NAMESPACE && id.path in disabledFluids

    fun isDisabledFluid(fluid: Fluid): Boolean = isDisabledFluid(ForgeRegistries.FLUIDS.getKey(fluid))

    @JvmStatic
    fun isDisabledRecipe(id: ResourceLocation, recipe: JsonElement): Boolean {
        // The pack already replaces all upstream recipes.
        if (id.namespace == NAMESPACE) return true
        if (recipe.isJsonObject) {
            val type = recipe.asJsonObject.get("type")
            if (type != null && type.isJsonPrimitive && type.asJsonPrimitive.isString) {
                val typeId = ResourceLocation.tryParse(type.asString)
                if (typeId?.namespace == NAMESPACE && typeId.path in disabledRecipeTypes) return true
            }
        }

        return referencesDisabledContent(recipe)
    }

    private fun referencesDisabledContent(element: JsonElement): Boolean = when {
        element.isJsonObject -> element.asJsonObject.entrySet().any { referencesDisabledContent(it.value) }
        element.isJsonArray -> element.asJsonArray.any(::referencesDisabledContent)
        element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
            val value = element.asString
            if (value.startsWith("$NAMESPACE:")) {
                val id = ResourceLocation.tryParse(value)
                isDisabledItem(id) || isDisabledFluid(id)
            } else {
                false
            }
        }
        else -> false
    }
}
