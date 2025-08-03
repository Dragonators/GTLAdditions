package com.gtladd.gtladditions.data.recipes.newmachinerecipe

import com.gregtechceu.gtceu.api.GTValues.*
import com.gregtechceu.gtceu.common.data.GTMaterials.*
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gtladd.gtladditions.GTLAdditions.id
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.common.data.GTLMaterials.*
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes.*
import org.gtlcore.gtlcore.utils.Registries.*
import java.util.function.Consumer

object BiologicalSimulation {
    @JvmStatic
    fun init(provider : Consumer<FinishedRecipe?>) {
        val swords = arrayOf<Sword?>(
            Sword("minecraft:diamond_sword", 1500, 1),
            Sword("minecraft:netherite_sword", 500, 5),
            Sword("avaritia:infinity_sword", 0, 20)
        )
        val biologicals = arrayOf<Biological?>(
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
        for (i in biologicals) {
            for (s in swords) generateRecipe(i !!, s !!, provider)
            setspawneggreicpes(i !!, provider)
        }
        generateSpecialRecipes(provider)
    }

    private fun generateRecipe(item : Biological, sword : Sword, provider : Consumer<FinishedRecipe?>) {
        val builder = GTLAddRecipesTypes.BIOLOGICAL_SIMULATION.recipeBuilder(
            id(item.name + (if (sword.damage > 10) "_1" else (if (sword.damage > 0) "_2" else "_3")))
        ).notConsumable(getItemStack("minecraft:" + item.name + "_spawn_egg"))
            .notConsumable(getItemStack("kubejs:" + item.data + "_data"))
        if (sword.name == "avaritia:infinity_sword") builder.notConsumable(getItemStack(sword.name))
        else builder.chancedInput(getItemStack(sword.name), sword.damage, 0)
        builder.inputFluids(Biomass.getFluid((1000 / sword.factor).toLong()))
        addOutputItems(builder, item, sword)
        builder.EUt(VA[item.EUt].toLong()).duration(400 / sword.factor).save(provider)
    }

    private fun setspawneggreicpes(item : Biological, provider : Consumer<FinishedRecipe?>) {
        if (item.name == "cow") return
        val builder = INCUBATOR_RECIPES.recipeBuilder(id(item.name + "_spawn_egg"))
            .inputItems(getItemStack("minecraft:bone", 4))
            .inputFluids(Biomass.getFluid(1000))
            .inputFluids(Milk.getFluid(1000))
        addInputItems(builder, item)
        builder.outputItems(getItemStack("minecraft:" + item.name + "_spawn_egg"))
            .EUt(VA[3].toLong()).duration(1200).save(provider)
    }

    private fun addOutputItems(builder : GTRecipeBuilder, i : Biological, sword : Sword) {
        builder.chancedOutput(getItemStack(i.O1, sword.factor * 2), i.O1f, 0)
        if (i.O2 != null) builder.chancedOutput(getItemStack(i.O2, sword.factor * 2), i.O2f, 0)
        if (i.O3 != null) builder.chancedOutput(getItemStack(i.O3, sword.factor * 2), i.O3f, 0)
        if (i.O4 != null) builder.chancedOutput(getItemStack(i.O4, sword.factor * 2), i.O4f, 0)
        if (i.O5 != null) builder.chancedOutput(getItemStack(i.O5, sword.factor * 2), i.O5f, 0)
        if (i.O6 != null) builder.chancedOutput(getItemStack(i.O6, sword.factor * 2), i.O6f, 0)
        if (i.O7 != null) builder.chancedOutput(getItemStack(i.O7, sword.factor * 2), i.O7f, 0)
    }

    private fun addInputItems(builder : GTRecipeBuilder, item : Biological) {
        if(item.name == "witch") {
            builder.inputItems(getItemStack("minecraft:redstone", 4))
                .inputItems(getItemStack("minecraft:glowstone_dust", 4))
                .inputItems(getItemStack("minecraft:sugar", 4))
                .inputItems(getItemStack("minecraft:glass_bottle", 4))
            return
        }
        if (item.O1 != "minecraft:bone") builder.inputItems(getItemStack(item.O1, 4))
        if (item.O2 != null) builder.inputItems(getItemStack(item.O2!!, 4))
        if (item.O3 != null) builder.inputItems(getItemStack(item.O3!!, 4))
        if (item.O4 != null) builder.inputItems(getItemStack(item.O4!!, 4))
        when (item.name) {
            "cat" -> builder.circuitMeta(1)
            "zombie" -> builder.circuitMeta(1)
            "zombie_villager" -> builder.circuitMeta(2)
            "husk" -> builder.circuitMeta(3)
            "donkey" -> builder.circuitMeta(1)
            "llama" -> builder.circuitMeta(2)
            "creeper" -> builder.circuitMeta(1)
        }
    }

    private fun generateSpecialRecipes(provider : Consumer<FinishedRecipe?>) {
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

    internal class Biological {
        var name : String
        var data : String?
        var O1 : String
        var O1f : Int
        var O2 : String? = null
        var O2f : Int = 0
        var O3 : String? = null
        var O3f : Int = 0
        var O4 : String? = null
        var O4f : Int = 0
        var O5 : String? = null
        var O5f : Int = 0
        var O6 : String? = null
        var O6f : Int = 0
        var O7 : String? = null
        var O7f : Int = 0
        var EUt : Int

        constructor(name : String, data : String?, o1 : String, o1f : Int, o2 : String?, o2f : Int, o3 : String?, o3f : Int, o4 : String?, o4f : Int, o5 : String?, o5f : Int, o6 : String?, o6f : Int, o7 : String?, o7f : Int, EUt : Int) {
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

        constructor(name : String, data : String?, o1 : String, o1f : Int, EUt : Int) {
            this.name = name
            this.data = data
            O1 = o1
            O1f = o1f
            this.EUt = EUt
        }

        constructor(name : String, data : String?, o1 : String, o1f : Int, o2 : String?, o2f : Int, EUt : Int) {
            this.name = name
            this.data = data
            O1 = o1
            O1f = o1f
            O2 = o2
            O2f = o2f
            this.EUt = EUt
        }

        constructor(name : String, data : String?, o1 : String, o1f : Int, o2 : String?, o2f : Int, o3 : String?, o3f : Int, EUt : Int) {
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

        constructor(name : String, data : String?, o1 : String, o1f : Int, o2 : String?, o2f : Int, o3 : String?, o3f : Int, o4 : String?, o4f : Int, EUt : Int) {
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

    internal class Sword(var name : String, var damage : Int, var factor : Int)
}
