package com.gtladd.gtladditions.data.recipes;

import org.gtlcore.gtlcore.api.machine.multiblock.GTLCleanroomType;
import org.gtlcore.gtlcore.common.data.GTLMaterials;
import org.gtlcore.gtlcore.common.recipe.condition.GravityCondition;
import org.gtlcore.gtlcore.utils.Registries;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;

import dev.latvian.mods.kubejs.KubeJS;

import java.util.function.Consumer;

import static org.gtlcore.gtlcore.common.data.GTLRecipeTypes.*;

public class Misc {

    public Misc() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        DECAY_HASTENER_RECIPES.recipeBuilder("tiranium50")
                .inputFluids(GTMaterials.Titanium.getFluid(144)).outputFluids(GTLMaterials.Titanium50.getFluid(144))
                .cleanroom(GTLCleanroomType.LAW_CLEANROOM).addCondition(new GravityCondition(true)).EUt(GTValues.VA[8]).duration(100).save(provider);
        DOOR_OF_CREATE_RECIPES.recipeBuilder("command_block")
                .inputItems(Registries.getItemStack("gtceu:magnetohydrodynamicallyconstrainedstarmatter_block"))
                .outputItems(Registries.getItemStack("minecraft:command_block"))
                .dimension(new ResourceLocation("overworld")).EUt(GTValues.V[14]).duration(1200).save(provider);
        DOOR_OF_CREATE_RECIPES.recipeBuilder("magmatter_block")
                .inputItems(Registries.getItemStack("gtceu:magmatter_ingot", 64))
                .outputItems(Registries.getItemStack("gtceu:magmatter_block"))
                .dimension(new ResourceLocation("overworld")).EUt(GTValues.V[14]).duration(1200).save(provider);
        CREATE_AGGREGATION_RECIPES.recipeBuilder("chain_command_block")
                .inputItems(Registries.getItemStack("kubejs:chain_command_block_core"))
                .inputItems(Registries.getItemStack("kubejs:command_block_broken"))
                .outputItems(Registries.getItemStack("minecraft:chain_command_block"))
                .dimension(KubeJS.id("create")).CWUt(Integer.MAX_VALUE - 1).EUt(GTValues.V[14]).duration(1200).save(provider);
        CREATE_AGGREGATION_RECIPES.recipeBuilder("repeating_command_block")
                .inputItems(Registries.getItemStack("kubejs:repeating_command_block_core"))
                .inputItems(Registries.getItemStack("kubejs:chain_command_block_broken"))
                .outputItems(Registries.getItemStack("minecraft:repeating_command_block"))
                .dimension(KubeJS.id("create")).CWUt(Integer.MAX_VALUE - 1).EUt(GTValues.V[14]).duration(1200).save(provider);
    }
}
