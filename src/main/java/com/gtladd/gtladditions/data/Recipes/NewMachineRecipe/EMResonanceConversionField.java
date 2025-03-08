package com.gtladd.gtladditions.data.Recipes.NewMachineRecipe;

import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.api.Recipe.GTLAddRecipesTypes;
import com.gtladd.gtladditions.api.Registry.GTLAddRecipeBuilder;

import java.util.function.Consumer;

public class EMResonanceConversionField {

    public EMResonanceConversionField() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        Material[] materials = {
                new Material("minecraft:grass_block", "minecraft:moss_block", 2, "moss_block"),
                new Material("minecraft:moss_block", "minecraft:sculk", 5, "sculk"),
                new Material("gtceu:calcium_block", "minecraft:bone_block", 2, "bone_block"),
                new Material("minecraft:oak_log", "minecraft:crimson_stem", 1, "crimson_stem"),
                new Material("minecraft:birch_log", "minecraft:warped_stem", 1, "warped_stem"),
                new Material("minecraft:bone_block", "kubejs:essence_block", 4, "essence_block"),
                new Material("kubejs:infused_obsidian", "kubejs:draconium_block_charged", 8, "draconium_block_charged")
        };

        for (Material m : materials) {
            new GTLAddRecipeBuilder(m.id, GTLAddRecipesTypes.EM_RESONANCE_CONVERSION_FIELD)
                    .inputItems(m.input)
                    .circuitMeta(1)
                    .outputItems(m.output)
                    .TierEUtVA(m.EUt)
                    .duration(20)
                    .save(provider);
        }
    }

    static class Material {

        String id;
        String input;
        String output;
        int EUt;

        public Material(String input, String output, int EUt, String id) {
            this.input = input;
            this.output = output;
            this.EUt = EUt;
            this.id = id;
        }
    }
}
