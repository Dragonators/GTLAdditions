package com.gtladd.gtladditions.data.Recipes.NewMachineRecipe;

import org.gtlcore.gtlcore.common.data.GTLMaterials;
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.api.Recipe.GTLAddRecipesTypes;
import com.gtladd.gtladditions.api.Registry.GTLAddRecipeBuilder;

import java.util.Objects;
import java.util.function.Consumer;

public class BiologicalSimulation {

    public BiologicalSimulation() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        Sword[] swords = {
                new Sword("minecraft:diamond_sword", 15, 1),
                new Sword("minecraft:netherite_sword", 5, 5),
                new Sword("avaritia:infinity_sword", 0, 20)
        };

        ItemStacks[] itemStacks = {
                new ItemStacks("blaze", "nether", GTMaterials.Biomass, GTMaterials.SterileGrowthMedium, "minecraft:blaze_rod", 5, 7),
                new ItemStacks("chicken", "overworld", GTMaterials.Biomass, "minecraft:chicken", 75, "minecraft:feather", 40, 2),
                new ItemStacks("cow", "overworld", GTMaterials.Biomass, "minecraft:beef", 75, "minecraft:leather", 25, 2),
                new ItemStacks("drowned", "overworld", GTMaterials.Biomass, "minecraft:rotten_flesh", 75, "minecraft:copper_ingot", 6, 3),
                new ItemStacks("enderman", "end", GTMaterials.Biomass, GTMaterials.SterileGrowthMedium, "minecraft:ender_pearl", 5, 6),
                new ItemStacks("ghast", "nether", GTMaterials.Biomass, GTMaterials.SterileGrowthMedium, "minecraft:gunpowder", 60, "minecraft:ghast_tear", 6, 7),
                new ItemStacks("creeper", "overworld", GTMaterials.Biomass, "minecraft:gunpowder", 80, 2),
                new ItemStacks("zombie", "overworld", GTMaterials.Biomass, "minecraft:rotten_flesh", 75, "minecraft:iron_ingot", 6, "minecraft:carrot", 15, "minecraft:potato", 15, 2),
                new ItemStacks("zombie_villager", "overworld", GTMaterials.Biomass, "minecraft:rotten_flesh", 75, "minecraft:iron_ingot", 6, "minecraft:carrot", 15, "minecraft:potato", 15, 2),
                new ItemStacks("husk", "overworld", GTMaterials.Biomass, "minecraft:rotten_flesh", 75, "minecraft:iron_ingot", 6, "minecraft:carrot", 15, "minecraft:potato", 15, 2),
                new ItemStacks("zombified_piglin", "nether", GTMaterials.Biomass, "minecraft:rotten_flesh", 75, "minecraft:gold_ingot", 6, "minecraft:gold_nugget", 10, 3),
                new ItemStacks("pig", "overworld", GTMaterials.Biomass, "minecraft:porkchop", 80, 2),
                new ItemStacks("sheep", "overworld", GTMaterials.Biomass, "minecraft:mutton", 80, "minecraft:white_wool", 50, 2),
                new ItemStacks("skeleton", "overworld", GTMaterials.Biomass, "minecraft:bone", 75, "minecraft:arrow", 65, 2),
                new ItemStacks("slime", "overworld", GTMaterials.Biomass, "minecraft:slime_ball", 50, 3),
                new ItemStacks("spider", "overworld", GTMaterials.Biomass, "minecraft:string", 70, "minecraft:spider_eye", 20, 2),
                new ItemStacks("vindicator", "overworld", GTMaterials.Biomass, "minecraft:emerald", 10, 3),
                new ItemStacks("witch", "overworld", GTMaterials.Biomass, "minecraft:stick", 50, "minecraft:gunpowder", 35, "minecraft:sugar", 35, "minecraft:glass_bottle", 35, "minecraft:redstone", 6, "minecraft:glowstone_dust", 6, "minecraft:spider_eye", 6, 3),
                new ItemStacks("wither_skeleton", "nether", GTMaterials.Biomass, GTLMaterials.BiohmediumSterilized, "minecraft:bone", 75, "minecraft:coal", 65, "minecraft:wither_skeleton_skull", 5, 8),
                new ItemStacks("rabbit", "overworld", GTMaterials.Biomass, "minecraft:rabbit", 70, "minecraft:rabbit_hide", 10, "minecraft:rabbit_foot", 5, 3),
                new ItemStacks("donkey", "overworld", GTMaterials.Biomass, "minecraft:leather", 50, 2),
                new ItemStacks("llama", "overworld", GTMaterials.Biomass, "minecraft:leather", 50, 2),
                new ItemStacks("cat", "overworld", GTMaterials.Biomass, "minecraft:string", 50, 2),
                new ItemStacks("panda", "overworld", GTMaterials.Biomass, "minecraft:bamboo", 50, 3),
                new ItemStacks("polar_bear", "overworld", GTMaterials.Biomass, "minecraft:cod", 50, "minecraft:salmon", 50, 3)

        };

        for (ItemStacks i : itemStacks) {
            for (Sword s : swords) {
                generateRecipe(i, s, provider);
            }
            setspawneggreicpes(i, provider);
        }
        generateSpecialRecipes(provider);
    }

    private static void generateRecipe(ItemStacks item, Sword sword, Consumer<FinishedRecipe> provider) {
        GTLAddRecipeBuilder builder = new GTLAddRecipeBuilder(item.name + (sword.damage > 10 ? "_1" : (sword.damage > 0 ? "_2" : "_3")), GTLAddRecipesTypes.BIOLOGICAL_SIMULATION)
                .notConsumable("minecraft:" + item.name + "_spawn_egg")
                .notConsumable("kubejs:" + item.data + "_data");

        if (Objects.equals(sword.name, "avaritia:infinity_sword")) {
            builder.notConsumable(sword.name);
        } else {
            builder.chancedInputItems(sword.name, sword.damage, 0);
        }

        builder.inputFluids(item.F1.getFluid(1000 / sword.factor));

        if (item.F2 != null) {
            builder.inputFluids(item.F2.getFluid(500 / sword.factor));
        }

        addOutputItems(builder, item);
        builder.TierEUtVA(item.EUt).duration(400 / sword.factor).save(provider);
    }

    private static void setspawneggreicpes(ItemStacks item, Consumer<FinishedRecipe> provider) {
        GTLAddRecipeBuilder builder = new GTLAddRecipeBuilder(item.name + "_spawn_egg", GTLRecipeTypes.INCUBATOR_RECIPES)
                .inputItems("minecraft:bone", 4)
                .inputFluids(GTMaterials.Biomass.getFluid(1000))
                .inputFluids(GTMaterials.Milk.getFluid(1000));
        addInputItems(builder, item);
        builder.outputItems("minecraft:" + item.name + "_spawn_egg").TierEUtVA(3).duration(1200).save(provider);
    }

    private static void addOutputItems(GTLAddRecipeBuilder builder, ItemStacks item) {
        builder.chancedOutputItems(item.O1, item.O1f, 0);
        if (item.O2 != null) builder.chancedOutputItems(item.O2, item.O2f, 0);
        if (item.O3 != null) builder.chancedOutputItems(item.O3, item.O3f, 0);
        if (item.O4 != null) builder.chancedOutputItems(item.O4, item.O4f, 0);
        if (item.O5 != null) builder.chancedOutputItems(item.O5, item.O5f, 0);
        if (item.O6 != null) builder.chancedOutputItems(item.O6, item.O6f, 0);
        if (item.O7 != null) builder.chancedOutputItems(item.O7, item.O7f, 0);
    }

    private static void addInputItems(GTLAddRecipeBuilder builder, ItemStacks item) {
        if (item.name.equals("witch")) {
            builder.inputItems("minecraft:redstone", 4).inputItems("minecraft:glowstone_dust", 4)
                    .inputItems("minecraft:sugar", 4).inputItems("minecraft:glass_bottle", 4);
            return;
        }
        if (!Objects.equals(item.O1, "minecraft:bone")) builder.inputItems(item.O1, 4);
        if (item.O2 != null) builder.inputItems(item.O2, 4);
        if (item.O3 != null) builder.inputItems(item.O3, 4);
        if (item.O4 != null) builder.inputItems(item.O4, 4);
    }

    private static void generateSpecialRecipes(Consumer<FinishedRecipe> provider) {
        new GTLAddRecipeBuilder("nether_star", GTLAddRecipesTypes.BIOLOGICAL_SIMULATION)
                .notConsumable("gtceu:nether_star_block")
                .notConsumable("kubejs:nether_data", 64)
                .notConsumable("avaritia:infinity_sword")
                .inputFluids(GTMaterials.Biomass.getFluid(50))
                .inputFluids(GTLMaterials.BiohmediumSterilized.getFluid(50))
                .chancedOutputItems("minecraft:nether_star", 15, 0)
                .duration(100)
                .EUt(GTValues.VA[GTValues.UV])
                .save(provider);

        new GTLAddRecipeBuilder("dragon_egg", GTLAddRecipesTypes.BIOLOGICAL_SIMULATION)
                .notConsumable("minecraft:dragon_head")
                .notConsumable("kubejs:end_data", 64)
                .notConsumable("avaritia:infinity_sword")
                .inputFluids(GTMaterials.Biomass.getFluid(50))
                .inputFluids(GTLMaterials.BiohmediumSterilized.getFluid(50))
                .outputItems("minecraft:dragon_egg")
                .duration(100)
                .EUt(GTValues.VA[GTValues.UV])
                .save(provider);
    }

    static class ItemStacks {

        String name;
        String data;
        Material F1;
        Material F2;
        String O1;
        double O1f;
        String O2;
        double O2f;
        String O3;
        double O3f;
        String O4;
        double O4f;
        String O5;
        double O5f;
        String O6;
        double O6f;
        String O7;
        double O7f;
        int EUt;

        public ItemStacks(String name, String data, Material f1, String o1, double o1f, String o2, double o2f, String o3, double o3f, String o4, double o4f, String o5, double o5f, String o6, double o6f, String o7, double o7f, int EUt) {
            this.name = name;
            this.data = data;
            F1 = f1;
            O1 = o1;
            O1f = o1f;
            O2 = o2;
            O2f = o2f;
            O3 = o3;
            O3f = o3f;
            O4 = o4;
            O4f = o4f;
            O5 = o5;
            O5f = o5f;
            O6 = o6;
            O6f = o6f;
            O7 = o7;
            O7f = o7f;
            this.EUt = EUt;
        }

        public ItemStacks(String name, String data, Material f1, String o1, double o1f, int EUt) {
            this.name = name;
            this.data = data;
            F1 = f1;
            O1 = o1;
            O1f = o1f;
            this.EUt = EUt;
        }

        public ItemStacks(String name, String data, Material f1, Material f2, String o1, double o1f, int EUt) {
            this.name = name;
            this.data = data;
            F1 = f1;
            F2 = f2;
            O1 = o1;
            O1f = o1f;
            this.EUt = EUt;
        }

        public ItemStacks(String name, String data, Material f1, String o1, double o1f, String o2, double o2f, int EUt) {
            this.name = name;
            this.data = data;
            F1 = f1;
            O1 = o1;
            O1f = o1f;
            O2 = o2;
            O2f = o2f;
            this.EUt = EUt;
        }

        public ItemStacks(String name, String data, Material f1, String o1, double o1f, String o2, double o2f, String o3, double o3f, int EUt) {
            this.name = name;
            this.data = data;
            F1 = f1;
            O1 = o1;
            O1f = o1f;
            O2 = o2;
            O2f = o2f;
            O3 = o3;
            O3f = o3f;
            this.EUt = EUt;
        }

        public ItemStacks(String name, String data, Material f1, String o1, double o1f, String o2, double o2f, String o3, double o3f, String o4, double o4f, int EUt) {
            this.name = name;
            this.data = data;
            F1 = f1;
            O1 = o1;
            O1f = o1f;
            O2 = o2;
            O2f = o2f;
            O3 = o3;
            O3f = o3f;
            O4 = o4;
            O4f = o4f;
            this.EUt = EUt;
        }

        public ItemStacks(String name, String data, Material f1, Material f2, String o1, double o1f, String o2, double o2f, int EUt) {
            this.name = name;
            this.data = data;
            F1 = f1;
            F2 = f2;
            O1 = o1;
            O1f = o1f;
            O2 = o2;
            O2f = o2f;
            this.EUt = EUt;
        }

        public ItemStacks(String name, String data, Material f1, Material f2, String o1, double o1f, String o2, double o2f, String o3, double o3f, int EUt) {
            this.name = name;
            this.data = data;
            F1 = f1;
            F2 = f2;
            O1 = o1;
            O1f = o1f;
            O2 = o2;
            O2f = o2f;
            O3 = o3;
            O3f = o3f;
            this.EUt = EUt;
        }
    }

    static class Sword {

        String name;
        double damage;
        int factor;

        public Sword(String name, double damage, int factor) {
            this.name = name;
            this.damage = damage;
            this.factor = factor;
        }
    }
}
