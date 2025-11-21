package com.gtladd.gtladditions.data.recipes.newmachinerecipe

import com.gregtechceu.gtceu.api.GTValues
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.NIGHTMARE_CRAFTING
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.common.data.GTLItems.COMPRESSED_PUFFERFISH
import org.gtlcore.gtlcore.common.data.GTLItems.SUPER_GLUE
import org.gtlcore.gtlcore.utils.Registries
import java.util.function.Consumer

object NightmareCrafting {
    @JvmStatic
    fun init(provider : Consumer<FinishedRecipe?>) {
        NIGHTMARE_CRAFTING.recipeBuilder(GTLAdditions.id("fish_big"))
            .inputItems(COMPRESSED_PUFFERFISH, 32)
            .inputItems(SUPER_GLUE, 24)
            .inputItems(Registries.getItemStack("kubejs:fishbig_fabric", 15))
            .inputItems(Registries.getItemStack("kubejs:fishbig_hair", 1))
            .inputItems(Registries.getItemStack("kubejs:fishbig_frame", 3))
            .inputItems(Registries.getItemStack("kubejs:fishbig_hade", 1))
            .inputItems(Registries.getItemStack("kubejs:fishbig_lhand", 1))
            .inputItems(Registries.getItemStack("kubejs:fishbig_body", 1))
            .inputItems(Registries.getItemStack("kubejs:fishbig_rhand", 1))
            .inputItems(Registries.getItemStack("kubejs:fishbig_lleg", 1))
            .inputItems(Registries.getItemStack("kubejs:fishbig_rleg", 1))
            .outputItems(Registries.getItemStack("expatternprovider:fishbig"))
            .duration(288000)
            .EUt(GTValues.VEX[22])
            .save(provider)
        NIGHTMARE_CRAFTING.recipeBuilder(GTLAdditions.id("ultimate_stew"))
            .inputItems(Registries.getItemStack("minecraft:apple"))
            .inputItems(Registries.getItemStack("minecraft:golden_apple"))
            .inputItems(Registries.getItemStack("minecraft:bread"))
            .inputItems(Registries.getItemStack("minecraft:kelp"))
            .inputItems(Registries.getItemStack("minecraft:cocoa_beans"))
            .inputItems(Registries.getItemStack("minecraft:cake"))
            .inputItems(Registries.getItemStack("minecraft:glistering_melon_slice"))
            .inputItems(Registries.getItemStack("minecraft:carrot"))
            .inputItems(Registries.getItemStack("minecraft:poisonous_potato"))
            .inputItems(Registries.getItemStack("minecraft:chorus_fruit"))
            .inputItems(Registries.getItemStack("minecraft:beetroot"))
            .inputItems(Registries.getItemStack("minecraft:mushroom_stew"))
            .inputItems(Registries.getItemStack("minecraft:honey_bottle"))
            .inputItems(Registries.getItemStack("minecraft:sweet_berries"))
            .inputItems(Registries.getItemStack("avaritia:neutron_nugget"))
            .outputItems(Registries.getItemStack("avaritia:ultimate_stew"))
            .duration(100)
            .EUt(GTValues.VA[GTValues.MAX].toLong())
            .save(provider)
        NIGHTMARE_CRAFTING.recipeBuilder(GTLAdditions.id("cosmic_meatballs"))
            .inputItems(Registries.getItemStack("minecraft:porkchop", 512))
            .inputItems(Registries.getItemStack("minecraft:beef", 512))
            .inputItems(Registries.getItemStack("minecraft:mutton", 512))
            .inputItems(Registries.getItemStack("minecraft:cod", 512))
            .inputItems(Registries.getItemStack("minecraft:salmon", 512))
            .inputItems(Registries.getItemStack("minecraft:tropical_fish", 512))
            .inputItems(Registries.getItemStack("minecraft:pufferfish", 512))
            .inputItems(Registries.getItemStack("minecraft:rabbit", 512))
            .inputItems(Registries.getItemStack("minecraft:chicken", 512))
            .inputItems(Registries.getItemStack("minecraft:rotten_flesh", 512))
            .inputItems(Registries.getItemStack("minecraft:spider_eye", 512))
            .inputItems(Registries.getItemStack("minecraft:egg", 512))
            .inputItems(Registries.getItemStack("avaritia:neutron_nugget", 512))
            .outputItems(Registries.getItemStack("avaritia:cosmic_meatballs"))
            .duration(16000)
            .EUt(GTValues.VA[GTValues.MAX].toLong())
            .save(provider)
        NIGHTMARE_CRAFTING.recipeBuilder(GTLAdditions.id("endest_pearl"))
            .inputItems(Registries.getItemStack("minecraft:end_stone", 2400))
            .inputItems(Registries.getItemStack("minecraft:ender_pearl", 3200))
            .inputItems(Registries.getItemStack("avaritia:neutron_ingot", 400))
            .inputItems(Registries.getItemStack("minecraft:nether_star", 1024))
            .outputItems(Registries.getItemStack("avaritia:endest_pearl"))
            .duration(16000)
            .EUt(GTValues.VA[GTValues.MAX].toLong())
            .save(provider)
    }
}