package com.gtladd.gtladditions.data.recipes.newmachinerecipe

import com.gregtechceu.gtceu.api.GTValues.UV
import com.gregtechceu.gtceu.api.GTValues.VA
import com.gregtechceu.gtceu.common.data.GTMaterials.Biomass
import com.gregtechceu.gtceu.common.data.GTMaterials.Milk
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gtladd.gtladditions.GTLAdditions.Companion.id
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.common.data.GTLMaterials.BiohmediumSterilized
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes.INCUBATOR_RECIPES
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import java.util.function.Consumer

object BiologicalSimulation {
    fun init(provider: Consumer<FinishedRecipe?>) {
        val swords = listOf(
            Sword("minecraft:diamond_sword", 15, 1),
            Sword("minecraft:netherite_sword", 5, 5),
            Sword("avaritia:infinity_sword", 0, 20)
        )
        val biologicals = listOf(
            Biological("blaze", "nether", "minecraft:blaze_rod", 500, 7),
            Biological("chicken", "overworld", "minecraft:chicken", 7500, "minecraft:feather", 4000, "minecraft:egg", 1000, 2),
            Biological("cow", "overworld", "minecraft:beef", 7500, "minecraft:leather", 2500, 2),
            Biological("drowned", "overworld", "minecraft:rotten_flesh", 7500, "minecraft:copper_ingot", 600, 3),
            Biological("enderman", "end", "minecraft:ender_pearl", 500, 6),
            Biological("ghast", "nether", "minecraft:gunpowder", 6000, "minecraft:ghast_tear", 600, 7),
            Biological("creeper", "overworld", "minecraft:gunpowder", 8000, 2),
            Biological("zombie", "overworld", "minecraft:rotten_flesh", 7500, "minecraft:iron_ingot", 600, "minecraft:carrot", 1500, "minecraft:potato", 1500, 2),
            Biological("zombie_villager", "overworld", "minecraft:rotten_flesh", 7500, "minecraft:iron_ingot", 600, "minecraft:carrot", 1500, "minecraft:potato", 1500, 2),
            Biological("husk", "overworld", "minecraft:rotten_flesh", 7500, "minecraft:iron_ingot", 600, "minecraft:carrot", 1500, "minecraft:potato", 1500, 2),
            Biological("zombified_piglin", "nether", "minecraft:rotten_flesh", 7500, "minecraft:gold_ingot", 600, "minecraft:gold_nugget", 1000, 3),
            Biological("pig", "overworld", "minecraft:porkchop", 8000, 2),
            Biological("sheep", "overworld", "minecraft:mutton", 8000, "minecraft:white_wool", 5000, 2),
            Biological("skeleton", "overworld", "minecraft:bone", 7500, "minecraft:arrow", 6500, 2),
            Biological("slime", "overworld", "minecraft:slime_ball", 5000, 3),
            Biological("spider", "overworld", "minecraft:string", 7000, "minecraft:spider_eye", 2000, 2),
            Biological("vindicator", "overworld", "minecraft:emerald", 1000, 3),
            Biological("witch", "overworld", "minecraft:stick", 5000, "minecraft:gunpowder", 3500, "minecraft:sugar", 3500, "minecraft:glass_bottle", 3500, "minecraft:redstone", 600, "minecraft:glowstone_dust", 600, "minecraft:spider_eye", 600, 3),
            Biological("wither_skeleton", "nether", "minecraft:bone", 7500, "minecraft:coal", 6500, "minecraft:wither_skeleton_skull", 500, 8),
            Biological("rabbit", "overworld", "minecraft:rabbit", 7000, "minecraft:rabbit_hide", 1000, "minecraft:rabbit_foot", 500, 3),
            Biological("donkey", "overworld", "minecraft:leather", 5000, 2),
            Biological("llama", "overworld", "minecraft:leather", 5000, 2),
            Biological("cat", "overworld", "minecraft:string", 5000, 2),
            Biological("panda", "overworld", "minecraft:bamboo", 5000, 3),
            Biological("polar_bear", "overworld", "minecraft:cod", 5000, "minecraft:salmon", 5000, 3)
        )
        for (biological in biologicals) {
            for (sword in swords) {
                generateRecipe(biological, sword, provider)
            }
            setSpawnEggRecipes(biological, provider)
        }
        generateSpecialRecipes(provider)
    }

    private fun generateRecipe(item: Biological, sword: Sword, provider: Consumer<FinishedRecipe?>) {
        val builder = GTLAddRecipesTypes.BIOLOGICAL_SIMULATION.recipeBuilder(
            id(item.name + (if (sword.damage > 10) "_1" else (if (sword.damage > 0) "_2" else "_3")))
        ).notConsumable(getItemStack("minecraft:" + item.name + "_spawn_egg"))
            .notConsumable(getItemStack("kubejs:" + item.data + "_data"))
        if (sword.name == "avaritia:infinity_sword") builder.notConsumable(getItemStack(sword.name))
        else builder.chancedInput(getItemStack(sword.name), sword.damage, 0)
        builder.inputFluids(Biomass.getFluid((1000 / sword.factor).toLong()))
        addOutputItems(builder, item, sword)
        builder.EUt(VA[item.eut].toLong()).duration(400 / sword.factor).save(provider)
    }

    private fun setSpawnEggRecipes(item: Biological, provider: Consumer<FinishedRecipe?>) {
        if (item.name == "cow") return
        val builder = INCUBATOR_RECIPES.recipeBuilder(id(item.name + "_spawn_egg"))
            .inputItems(getItemStack("minecraft:bone", 4))
            .inputFluids(Biomass.getFluid(1000))
            .inputFluids(Milk.getFluid(1000))
        addInputItems(builder, item)
        builder.outputItems(getItemStack("minecraft:" + item.name + "_spawn_egg"))
            .EUt(VA[3].toLong()).duration(1200).save(provider)
    }

    private fun addOutputItems(builder: GTRecipeBuilder, item: Biological, sword: Sword) {
        val outputs = listOf(
            Triple(item.o1, item.o1f, true),
            Triple(item.o2, item.o2f, item.o2 != null),
            Triple(item.o3, item.o3f, item.o3 != null),
            Triple(item.o4, item.o4f, item.o4 != null),
            Triple(item.o5, item.o5f, item.o5 != null),
            Triple(item.o6, item.o6f, item.o6 != null),
            Triple(item.o7, item.o7f, item.o7 != null)
        )

        for ((outputItem, chance, shouldAdd) in outputs) {
            if (shouldAdd && outputItem != null) {
                builder.chancedOutput(getItemStack(outputItem, sword.factor * 2), chance, 0)
            }
        }
    }

    private fun addInputItems(builder: GTRecipeBuilder, item: Biological) {
        if (item.name == "witch") {
            builder.inputItems(getItemStack("minecraft:redstone", 4))
                .inputItems(getItemStack("minecraft:glowstone_dust", 4))
                .inputItems(getItemStack("minecraft:sugar", 4))
                .inputItems(getItemStack("minecraft:glass_bottle", 4))
            return
        }

        val inputs = listOfNotNull(
            item.o1.takeIf { it != "minecraft:bone" },
            item.o2,
            item.o3,
            item.o4
        )

        for (input in inputs) {
            builder.inputItems(getItemStack(input, 4))
        }

        when (item.name) {
            "cat", "zombie", "donkey", "creeper" -> builder.circuitMeta(1)
            "zombie_villager", "llama" -> builder.circuitMeta(2)
            "husk" -> builder.circuitMeta(3)
        }
    }

    private fun generateSpecialRecipes(provider: Consumer<FinishedRecipe?>) {
        GTLAddRecipesTypes.BIOLOGICAL_SIMULATION.recipeBuilder(id("nether_star"))
            .notConsumable(getItemStack("gtceu:nether_star_block"))
            .notConsumable(getItemStack("kubejs:nether_data", 64))
            .notConsumable(getItemStack("avaritia:infinity_sword"))
            .inputFluids(Biomass.getFluid(50))
            .inputFluids(BiohmediumSterilized.getFluid(50))
            .chancedOutput(getItemStack("minecraft:nether_star"), 1500, 0)
            .duration(100)
            .EUt(VA[UV].toLong())
            .save(provider)

        GTLAddRecipesTypes.BIOLOGICAL_SIMULATION.recipeBuilder(id("dragon_egg"))
            .notConsumable(getItemStack("minecraft:dragon_head"))
            .notConsumable(getItemStack("kubejs:end_data", 64))
            .notConsumable(getItemStack("avaritia:infinity_sword"))
            .inputFluids(Biomass.getFluid(50))
            .inputFluids(BiohmediumSterilized.getFluid(50))
            .outputItems(getItemStack("minecraft:dragon_egg"))
            .duration(100)
            .EUt(VA[UV].toLong())
            .save(provider)
    }

    internal data class Biological(
        val name: String,
        val data: String?,
        val o1: String,
        val o1f: Int,
        val o2: String?,
        val o2f: Int,
        val o3: String?,
        val o3f: Int,
        val o4: String?,
        val o4f: Int,
        val o5: String?,
        val o5f: Int,
        val o6: String?,
        val o6f: Int,
        val o7: String?,
        val o7f: Int,
        val eut: Int
    ) {
        constructor(name: String, data: String?, o1: String, o1f: Int, eut: Int) :
            this(name, data, o1, o1f, null, 0, null, 0, null, 0, null, 0, null, 0, null, 0, eut)

        constructor(name: String, data: String?, o1: String, o1f: Int, o2: String?, o2f: Int, eut: Int) :
            this(name, data, o1, o1f, o2, o2f, null, 0, null, 0, null, 0, null, 0, null, 0, eut)

        constructor(name: String, data: String?, o1: String, o1f: Int, o2: String?, o2f: Int, o3: String?, o3f: Int, eut: Int) :
            this(name, data, o1, o1f, o2, o2f, o3, o3f, null, 0, null, 0, null, 0, null, 0, eut)

        constructor(name: String, data: String?, o1: String, o1f: Int, o2: String?, o2f: Int, o3: String?, o3f: Int, o4: String?, o4f: Int, eut: Int) :
            this(name, data, o1, o1f, o2, o2f, o3, o3f, o4, o4f, null, 0, null, 0, null, 0, eut)
    }

    internal data class Sword(
        val name: String,
        val damage: Int,
        val factor: Int
    )
}
