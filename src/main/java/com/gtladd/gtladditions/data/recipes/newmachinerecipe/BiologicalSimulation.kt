package com.gtladd.gtladditions.data.recipes.newmachinerecipe

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.common.data.GTMaterials
import com.gtladd.gtladditions.api.registry.GTLAddRecipeBuilder
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.common.data.GTLMaterials
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes
import java.util.function.Consumer

object BiologicalSimulation {
    @JvmStatic
    fun init(provider : Consumer<FinishedRecipe?>) {
        val swords = arrayOf<Sword?>(
            Sword("minecraft:diamond_sword", 15.0, 1),
            Sword("minecraft:netherite_sword", 5.0, 5),
            Sword("avaritia:infinity_sword", 0.0, 20)
        )
        val itemStacks = arrayOf<ItemStacks?>(
            ItemStacks("blaze", "nether", "minecraft:blaze_rod", 5.0, 7),
            ItemStacks(
                "chicken",
                "overworld",
                "minecraft:chicken",
                75.0,
                "minecraft:feather",
                40.0,
                "minecraft:egg",
                10.0,
                2
            ),
            ItemStacks("cow", "overworld", "minecraft:beef", 75.0, "minecraft:leather", 25.0, 2),
            ItemStacks("drowned", "overworld", "minecraft:rotten_flesh", 75.0, "minecraft:copper_ingot", 6.0, 3),
            ItemStacks("enderman", "end", "minecraft:ender_pearl", 5.0, 6),
            ItemStacks("ghast", "nether", "minecraft:gunpowder", 60.0, "minecraft:ghast_tear", 6.0, 7),
            ItemStacks("creeper", "overworld", "minecraft:gunpowder", 80.0, 2),
            ItemStacks(
                "zombie",
                "overworld",
                "minecraft:rotten_flesh",
                75.0,
                "minecraft:iron_ingot",
                6.0,
                "minecraft:carrot",
                15.0,
                "minecraft:potato",
                15.0,
                2
            ),
            ItemStacks(
                "zombie_villager",
                "overworld",
                "minecraft:rotten_flesh",
                75.0,
                "minecraft:iron_ingot",
                6.0,
                "minecraft:carrot",
                15.0,
                "minecraft:potato",
                15.0,
                2
            ),
            ItemStacks(
                "husk",
                "overworld",
                "minecraft:rotten_flesh",
                75.0,
                "minecraft:iron_ingot",
                6.0,
                "minecraft:carrot",
                15.0,
                "minecraft:potato",
                15.0,
                2
            ),
            ItemStacks(
                "zombified_piglin",
                "nether",
                "minecraft:rotten_flesh",
                75.0,
                "minecraft:gold_ingot",
                6.0,
                "minecraft:gold_nugget",
                10.0,
                3
            ),
            ItemStacks("pig", "overworld", "minecraft:porkchop", 80.0, 2),
            ItemStacks("sheep", "overworld", "minecraft:mutton", 80.0, "minecraft:white_wool", 50.0, 2),
            ItemStacks("skeleton", "overworld", "minecraft:bone", 75.0, "minecraft:arrow", 65.0, 2),
            ItemStacks("slime", "overworld", "minecraft:slime_ball", 50.0, 3),
            ItemStacks("spider", "overworld", "minecraft:string", 70.0, "minecraft:spider_eye", 20.0, 2),
            ItemStacks("vindicator", "overworld", "minecraft:emerald", 10.0, 3),
            ItemStacks(
                "witch",
                "overworld",
                "minecraft:stick",
                50.0,
                "minecraft:gunpowder",
                35.0,
                "minecraft:sugar",
                35.0,
                "minecraft:glass_bottle",
                35.0,
                "minecraft:redstone",
                6.0,
                "minecraft:glowstone_dust",
                6.0,
                "minecraft:spider_eye",
                6.0,
                3
            ),
            ItemStacks(
                "wither_skeleton",
                "nether",
                "minecraft:bone",
                75.0,
                "minecraft:coal",
                65.0,
                "minecraft:wither_skeleton_skull",
                5.0,
                8
            ),
            ItemStacks(
                "rabbit",
                "overworld",
                "minecraft:rabbit",
                70.0,
                "minecraft:rabbit_hide",
                10.0,
                "minecraft:rabbit_foot",
                5.0,
                3
            ),
            ItemStacks("donkey", "overworld", "minecraft:leather", 50.0, 2),
            ItemStacks("llama", "overworld", "minecraft:leather", 50.0, 2),
            ItemStacks("cat", "overworld", "minecraft:string", 50.0, 2),
            ItemStacks("panda", "overworld", "minecraft:bamboo", 50.0, 3),
            ItemStacks("polar_bear", "overworld", "minecraft:cod", 50.0, "minecraft:salmon", 50.0, 3)
        )
        for (i in itemStacks) {
            for (s in swords) generateRecipe(i !!, s !!, provider)
            setspawneggreicpes(i !!, provider)
        }
        generateSpecialRecipes(provider)
    }

    private fun generateRecipe(item : ItemStacks, sword : Sword, provider : Consumer<FinishedRecipe?>) {
        val builder = GTLAddRecipeBuilder(
            item.name + (if (sword.damage > 10) "_1" else (if (sword.damage > 0) "_2" else "_3")),
            GTLAddRecipesTypes.BIOLOGICAL_SIMULATION
        )
            .notConsumable("minecraft:" + item.name + "_spawn_egg").notConsumable("kubejs:" + item.data + "_data")
        if (sword.name == "avaritia:infinity_sword") builder.notConsumable(sword.name)
        else builder.chancedInputItems(sword.name, sword.damage, 0.0)
        builder.inputFluids(GTMaterials.Biomass.getFluid((1000 / sword.factor).toLong()))
        addOutputItems(builder, item, sword)
        builder.TierEUtVA(item.EUt).duration(400 / sword.factor).save(provider)
    }

    private fun setspawneggreicpes(item : ItemStacks, provider : Consumer<FinishedRecipe?>) {
        val builder = GTLAddRecipeBuilder(item.name + "_spawn_egg", GTLRecipeTypes.INCUBATOR_RECIPES)
            .inputItems("minecraft:bone", 4).inputFluids(GTMaterials.Biomass.getFluid(1000))
            .inputFluids(GTMaterials.Milk.getFluid(1000))
        addInputItems(builder, item)
        builder.outputItems("minecraft:" + item.name + "_spawn_egg").TierEUtVA(3).duration(1200).save(provider)
    }

    private fun addOutputItems(builder : GTLAddRecipeBuilder, item : ItemStacks, sword : Sword) {
        builder.chancedOutputItems(item.O1, sword.factor * 2, item.O1f, 0.0)
        if (item.O2 != null) builder.chancedOutputItems(item.O2 !!, sword.factor * 2, item.O2f, 0.0)
        if (item.O3 != null) builder.chancedOutputItems(item.O3 !!, sword.factor * 2, item.O3f, 0.0)
        if (item.O4 != null) builder.chancedOutputItems(item.O4 !!, sword.factor * 2, item.O4f, 0.0)
        if (item.O5 != null) builder.chancedOutputItems(item.O5 !!, sword.factor * 2, item.O5f, 0.0)
        if (item.O6 != null) builder.chancedOutputItems(item.O6 !!, sword.factor * 2, item.O6f, 0.0)
        if (item.O7 != null) builder.chancedOutputItems(item.O7 !!, sword.factor * 2, item.O7f, 0.0)
    }

    private fun addInputItems(builder : GTLAddRecipeBuilder, item : ItemStacks) {
        if (item.name == "witch") {
            builder.inputItems("minecraft:redstone", 4).inputItems("minecraft:glowstone_dust", 4)
                .inputItems("minecraft:sugar", 4).inputItems("minecraft:glass_bottle", 4)
            return
        }
        if (item.O1 != "minecraft:bone") builder.inputItems(item.O1, 4)
        if (item.O2 != null) builder.inputItems(item.O2, 4)
        if (item.O3 != null) builder.inputItems(item.O3, 4)
        if (item.O4 != null) builder.inputItems(item.O4, 4)
    }

    private fun generateSpecialRecipes(provider : Consumer<FinishedRecipe?>) {
        GTLAddRecipeBuilder("nether_star", GTLAddRecipesTypes.BIOLOGICAL_SIMULATION)
            .notConsumable("gtceu:nether_star_block")
            .notConsumable("kubejs:nether_data", 64)
            .notConsumable("avaritia:infinity_sword")
            .inputFluids(GTMaterials.Biomass.getFluid(50))
            .inputFluids(GTLMaterials.BiohmediumSterilized.getFluid(50))
            .chancedOutputItems("minecraft:nether_star", 15.0, 0.0)
            .duration(100)
            .EUt(GTValues.VA[GTValues.UV].toLong())
            .save(provider)

        GTLAddRecipeBuilder("dragon_egg", GTLAddRecipesTypes.BIOLOGICAL_SIMULATION)
            .notConsumable("minecraft:dragon_head")
            .notConsumable("kubejs:end_data", 64)
            .notConsumable("avaritia:infinity_sword")
            .inputFluids(GTMaterials.Biomass.getFluid(50))
            .inputFluids(GTLMaterials.BiohmediumSterilized.getFluid(50))
            .outputItems("minecraft:dragon_egg")
            .duration(100)
            .EUt(GTValues.VA[GTValues.UV].toLong())
            .save(provider)
    }

    internal class ItemStacks {
        var name : String
        var data : String?
        var O1 : String
        var O1f : Double
        var O2 : String? = null
        var O2f : Double = 0.0
        var O3 : String? = null
        var O3f : Double = 0.0
        var O4 : String? = null
        var O4f : Double = 0.0
        var O5 : String? = null
        var O5f : Double = 0.0
        var O6 : String? = null
        var O6f : Double = 0.0
        var O7 : String? = null
        var O7f : Double = 0.0
        var EUt : Int

        constructor(
            name : String,
            data : String?,
            o1 : String,
            o1f : Double,
            o2 : String?,
            o2f : Double,
            o3 : String?,
            o3f : Double,
            o4 : String?,
            o4f : Double,
            o5 : String?,
            o5f : Double,
            o6 : String?,
            o6f : Double,
            o7 : String?,
            o7f : Double,
            EUt : Int
        ) {
            this.name = name
            this.data = data
            O1 = o1
            O1f = o1f
            O2 = o2
            O2f = o2f
            O3 = o3
            O3f = o3f
            O4 = o4
            O4f = o4f
            O5 = o5
            O5f = o5f
            O6 = o6
            O6f = o6f
            O7 = o7
            O7f = o7f
            this.EUt = EUt
        }

        constructor(name : String, data : String?, o1 : String, o1f : Double, EUt : Int) {
            this.name = name
            this.data = data
            O1 = o1
            O1f = o1f
            this.EUt = EUt
        }

        constructor(name : String, data : String?, o1 : String, o1f : Double, o2 : String?, o2f : Double, EUt : Int) {
            this.name = name
            this.data = data
            O1 = o1
            O1f = o1f
            O2 = o2
            O2f = o2f
            this.EUt = EUt
        }

        constructor(
            name : String,
            data : String?,
            o1 : String,
            o1f : Double,
            o2 : String?,
            o2f : Double,
            o3 : String?,
            o3f : Double,
            EUt : Int
        ) {
            this.name = name
            this.data = data
            O1 = o1
            O1f = o1f
            O2 = o2
            O2f = o2f
            O3 = o3
            O3f = o3f
            this.EUt = EUt
        }

        constructor(
            name : String,
            data : String?,
            o1 : String,
            o1f : Double,
            o2 : String?,
            o2f : Double,
            o3 : String?,
            o3f : Double,
            o4 : String?,
            o4f : Double,
            EUt : Int
        ) {
            this.name = name
            this.data = data
            O1 = o1
            O1f = o1f
            O2 = o2
            O2f = o2f
            O3 = o3
            O3f = o3f
            O4 = o4
            O4f = o4f
            this.EUt = EUt
        }
    }

    internal class Sword(var name : String, var damage : Double, var factor : Int)
}
