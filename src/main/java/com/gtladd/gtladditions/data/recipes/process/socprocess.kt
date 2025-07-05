package com.gtladd.gtladditions.data.recipes.process;

import org.gtlcore.gtlcore.api.machine.multiblock.GTLCleanroomType;
import org.gtlcore.gtlcore.common.data.GTLMaterials;
import org.gtlcore.gtlcore.common.recipe.condition.GravityCondition;
import org.gtlcore.gtlcore.utils.Registries;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.Item;

import com.gtladd.gtladditions.GTLAdditions;
import com.gtladd.gtladditions.api.registry.GTLAddRecipeBuilder;
import com.gtladd.gtladditions.common.items.GTLAddItems;
import com.gtladd.gtladditions.common.material.GTLAddMaterial;
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes;
import com.tterrag.registrate.util.entry.ItemEntry;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.*;
import static org.gtlcore.gtlcore.common.data.GTLRecipeTypes.*;

public class socprocess {

    public socprocess() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        electricblastfurnace.init(provider);
        cutter.init(provider);
        engravingarray.init(provider);
        circuitassembler.init(provider);
        generatedRecipe(provider);
    }

    private static void generatedRecipe(Consumer<FinishedRecipe> provider) {
        CHEMICAL_BATH_RECIPES.recipeBuilder(GTLAdditions.id("bioware_echo_shard_wafer"))
                .inputItems(GTLAddItems.ECHO_SHARD_WAFER.asStack()).inputFluids(GTLMaterials.BiohmediumSterilized.getFluid(250))
                .outputItems(GTLAddItems.BIOWARE_ECHO_SHARD_BOULE.asStack())
                .EUt(GTValues.VA[GTValues.UHV]).duration(200).cleanroom(CleanroomType.STERILE_CLEANROOM).addCondition(new GravityCondition()).save(provider);
        LARGE_CHEMICAL_RECIPES.recipeBuilder(GTLAdditions.id("prepare_extraordinary_soc_wafer"))
                .inputItems(GTLAddItems.HASSIUM_WAFER.asStack()).inputItems(TagPrefix.dust, GTLMaterials.FranciumCaesiumCadmiumBromide, 2)
                .inputFluids(GTLMaterials.SeaborgiumDopedNanotubes.getFluid(144)).inputFluids(GTLMaterials.CarbonNanotubes.getFluid(144))
                .outputItems(GTLAddItems.PREPARE_EXTRAORDINARY_SOC_WAFER.asStack())
                .EUt(GTValues.VA[GTValues.UEV]).duration(200).cleanroom(GTLCleanroomType.LAW_CLEANROOM).save(provider);
        SPS_CRAFTING_RECIPES.recipeBuilder(GTLAdditions.id("dragon_element_starmetal_wafer"))
                .inputItems(GTLAddItems.STARMETAL_WAFER, 4)
                .inputItems(Registries.getItemStack("kubejs:kinetic_matter")).inputItems(Registries.getItemStack("kubejs:unstable_star", 2))
                .inputFluids(GTLMaterials.Mana.getFluid(10000)).inputFluids(GTLMaterials.DragonElement.getFluid(1000))
                .inputFluids(GTLMaterials.FreeAlphaGas.getFluid(500)).outputItems(GTLAddItems.DRAGON_ELEMENT_STARMETAL_WAFER, 4)
                .EUt(GTValues.VA[GTValues.UIV]).duration(200).cleanroom(GTLCleanroomType.LAW_CLEANROOM).addCondition(new GravityCondition()).save(provider);
        QFT_RECIPES.recipeBuilder(GTLAdditions.id("prepare_spacetime_soc_wafer"))
                .inputItems(GTLAddItems.PERIODICIUM_WAFER, 4)
                .inputItems(Registries.getItemStack("kubejs:charged_lepton_trap_crystal"))
                .inputItems(Registries.getItemStack("kubejs:nuclear_star", 4))
                .inputFluids(GTLMaterials.CosmicMesh.getFluid(FluidStorageKeys.PLASMA, 1000))
                .inputFluids(GTLMaterials.CosmicElement.getFluid(10000))
                .inputFluids(GTLMaterials.SpaceTime.getFluid(500))
                .outputItems(GTLAddItems.PREPARE_SPACETIME_SOC_WAFER, 4).EUt(GTValues.VA[GTValues.OpV]).duration(200).save(provider);
        DIMENSIONALLY_TRANSCENDENT_MIXER_RECIPES.recipeBuilder(GTLAdditions.id("prepare_primary_soc_wafer"))
                .notConsumable(Registries.getItemStack("kubejs:eternity_catalyst")).inputItems(GTLAddItems.INFINITY_WAFER, 4)
                .inputItems(Registries.getItemStack("kubejs:quantum_anomaly")).inputItems(Registries.getItemStack("kubejs:two_way_foil"))
                .inputItems(Registries.getItemStack("kubejs:void_matter")).inputItems(TagPrefix.dust, GTLMaterials.TranscendentMetal, 16)
                .inputFluids(GTLMaterials.RawStarMatter.getFluid(FluidStorageKeys.PLASMA, 10000))
                .inputFluids(GTLMaterials.SpaceTime.getFluid(1000)).inputFluids(GTLMaterials.PrimordialMatter.getFluid(1000))
                .outputItems(GTLAddItems.PREPARE_PRIMARY_SOC_WAFER, 4).EUt(4L * GTValues.VA[GTValues.MAX]).duration(200)
                .cleanroom(GTLCleanroomType.LAW_CLEANROOM).addCondition(new GravityCondition()).save(provider);
        new GTLAddRecipeBuilder("spacetime_lens", PRECISION_ASSEMBLER_RECIPES)
                .inputItems("kubejs:grating_lithography_mask").inputItems("kubejs:topological_manipulator_unit")
                .inputItems("kubejs:ctc_computational_unit").inputFluids(GTLMaterials.QuantumDots.getFluid(1000))
                .inputFluids(GTLMaterials.CosmicComputingMixture.getFluid(1000)).inputFluids(GTMaterials.Krypton.getFluid(10000))
                .outputItems(GTLAddItems.SPACETIME_LENS).EUt(GTValues.VA[GTValues.OpV]).duration(2000).cleanroom(GTLCleanroomType.LAW_CLEANROOM).save(provider);
    }

    static class electricblastfurnace {

        public static void init(Consumer<FinishedRecipe> provider) {
            BLAST_RECIPES.recipeBuilder(GTLAdditions.id("echo_shard_boule"))
                    .inputItems(GTItems.SILICON_BOULE, 64).inputItems(TagPrefix.dust, GTLAddMaterial.GALLIUM_OXIDE, 16).inputItems(Registries.getItemStack("gtceu:echo_shard_dust", 16))
                    .inputFluids(GTMaterials.Krypton.getFluid(16000))
                    .outputItems(GTLAddItems.ECHO_SHARD_BOULE).EUt(GTValues.VA[GTValues.UV]).duration(21000).blastFurnaceTemp(14400).save(provider);
            addBlastRecipe(GTMaterials.Hassium, GTLAddItems.HASSIUM_BOULE, GTValues.VA[GTValues.UHV], 24000, 18000, provider);
            addBlastRecipe(GTLMaterials.Starmetal, GTLAddItems.STARMETAL_BOULE, GTValues.VA[GTValues.UEV], 27000, 21000, provider);
            addBlastRecipe(GTLMaterials.Periodicium, GTLAddItems.PERIODICIUM_BOULE, GTValues.VA[GTValues.UXV], 30000, 36000, provider);
            addBlastRecipe(GTLMaterials.Infinity, GTLAddItems.INFINITY_BOULE, GTValues.VA[GTValues.OpV], 33000, 62000, provider);
            CHEMICAL_RECIPES.recipeBuilder(GTLAdditions.id("ammonium_gallium_sulfate"))
                    .inputItems(TagPrefix.dust, GTMaterials.Gallium).inputFluids(GTMaterials.SulfuricAcid.getFluid(2000)).inputFluids(GTMaterials.Ammonia.getFluid(1000))
                    .outputItems(TagPrefix.dust, GTLAddMaterial.AMMONIUM_GALIUM_SULFATE).EUt(GTValues.VA[GTValues.EV]).duration(200).save(provider);
            LARGE_CHEMICAL_RECIPES.recipeBuilder(GTLAdditions.id("ammonium_gallium_sulfate"))
                    .inputItems(TagPrefix.dust, GTMaterials.Gallium).inputFluids(GTMaterials.SulfuricAcid.getFluid(2000)).inputFluids(GTMaterials.Ammonia.getFluid(1000))
                    .outputItems(TagPrefix.dust, GTLAddMaterial.AMMONIUM_GALIUM_SULFATE).EUt(GTValues.VA[GTValues.EV]).duration(200).save(provider);
            LARGE_CHEMICAL_RECIPES.recipeBuilder(GTLAdditions.id("gallium_oxide"))
                    .inputItems(TagPrefix.dust, GTLAddMaterial.AMMONIUM_GALIUM_SULFATE, 4).inputFluids(GTMaterials.Oxygen.getFluid(15000))
                    .outputItems(TagPrefix.dust, GTLAddMaterial.GALLIUM_OXIDE, 2).outputFluids(GTMaterials.Nitrogen.getFluid(2000)).outputFluids(GTMaterials.SulfurTrioxide.getFluid(8000))
                    .outputFluids(GTMaterials.Water.getFluid(8000)).EUt(GTValues.VA[GTValues.IV]).duration(200).save(provider);
        }

        public static void addBlastRecipe(Material input, ItemEntry<Item> output, int EUt, int duration, int temperature, Consumer<FinishedRecipe> provider) {
            BLAST_RECIPES.recipeBuilder(GTLAdditions.id(output.asItem().toString()))
                    .inputItems(GTItems.SILICON_BOULE, 64).inputItems(TagPrefix.dust, GTLAddMaterial.GALLIUM_OXIDE, 16).inputItems(TagPrefix.dust, input, 16)
                    .inputFluids(GTMaterials.Krypton.getFluid(16000)).outputItems(output).EUt(EUt).duration(duration).blastFurnaceTemp(temperature).save(provider);
        }
    }

    static class cutter {

        public static void init(Consumer<FinishedRecipe> provider) {
            addCutterRecipe("echo_shard_wafer", GTLAddItems.ECHO_SHARD_BOULE, 16, GTLAddItems.ECHO_SHARD_WAFER, GTValues.VA[GTValues.UV], CleanroomType.STERILE_CLEANROOM, provider);
            addCutterRecipe("outstanding_soc", GTLAddItems.OUTSTANDING_SOC_WAFER, 6, GTLAddItems.OUTSTANDING_SOC, GTValues.VA[GTValues.UV], CleanroomType.STERILE_CLEANROOM, provider);
            addCutterRecipe("hassium_wafer", GTLAddItems.HASSIUM_BOULE, 16, GTLAddItems.HASSIUM_WAFER, GTValues.VA[GTValues.UHV], GTLCleanroomType.LAW_CLEANROOM, provider);
            addCutterRecipe("extraordinary_soc_wafer", GTLAddItems.EXTRAORDINARY_SOC_WAFER, 6, GTLAddItems.EXTRAORDINARY_SOC, GTValues.VA[GTValues.UHV], GTLCleanroomType.LAW_CLEANROOM, provider);
            addCutterRecipe("starmetal_wafer", GTLAddItems.STARMETAL_BOULE, 16, GTLAddItems.STARMETAL_WAFER, GTValues.VA[GTValues.UEV], GTLCleanroomType.LAW_CLEANROOM, provider);
            addCutterRecipe("chaos_soc", GTLAddItems.CHAOS_SOC_WAFER, 6, GTLAddItems.CHAOS_SOC, GTValues.VA[GTValues.UEV], GTLCleanroomType.LAW_CLEANROOM, provider);
            addCutterRecipe("periodicium_wafer", GTLAddItems.PERIODICIUM_BOULE, 256, GTLAddItems.PERIODICIUM_WAFER, GTValues.VA[GTValues.UXV], GTLCleanroomType.LAW_CLEANROOM, provider);
            addCutterRecipe("spacetime_soc", GTLAddItems.SPACETIME_SOC_WAFER, 6, GTLAddItems.SPACETIME_SOC, GTValues.VA[GTValues.UXV], GTLCleanroomType.LAW_CLEANROOM, provider);
            addCutterRecipe("infinity_wafer", GTLAddItems.INFINITY_BOULE, 16, GTLAddItems.INFINITY_WAFER, GTValues.VA[GTValues.OpV], GTLCleanroomType.LAW_CLEANROOM, provider);
            addCutterRecipe("primary_soc", GTLAddItems.PRIMARY_SOC_WAFER, 6, GTLAddItems.PRIMARY_SOC, GTValues.VA[GTValues.OpV], GTLCleanroomType.LAW_CLEANROOM, provider);
        }

        private static void addCutterRecipe(String id, ItemEntry<Item> input, int output, ItemEntry<Item> outputitem, int EUt, CleanroomType cleanroomType, Consumer<FinishedRecipe> provider) {
            GTRecipeBuilder builder = CUTTER_RECIPES.recipeBuilder(GTLAdditions.id(id + "_0"))
                    .inputItems(input).outputItems(outputitem, output).EUt(EUt).cleanroom(cleanroomType);
            if (EUt > GTValues.VA[GTValues.UEV]) {
                builder.inputFluids(GTLMaterials.GradePurifiedWater16.getFluid(EUt > GTValues.VA[GTValues.UXV] ? 1000 : 500)).duration(450).save(provider);
                return;
            }
            GTRecipeBuilder recipe = builder.copy(GTLAdditions.id(id + "_1"));
            builder.inputFluids(GTLMaterials.GradePurifiedWater8.getFluid(500)).duration(900).save(provider);
            recipe.inputFluids(GTLMaterials.GradePurifiedWater16.getFluid(250)).duration(450).save(provider);
        }
    }

    static class engravingarray {

        public static void init(Consumer<FinishedRecipe> provider) {
            addEngravingRecipe(GTLAddItems.BIOWARE_ECHO_SHARD_BOULE, GTLMaterials.Photoresist, "kubejs:grating_lithography_mask", GTLAddItems.OUTSTANDING_SOC_WAFER, GTValues.VA[GTValues.UHV], 300, CleanroomType.STERILE_CLEANROOM, provider);
            addEngravingRecipe(GTLAddItems.PREPARE_EXTRAORDINARY_SOC_WAFER, GTLMaterials.Photoresist, "kubejs:grating_lithography_mask", GTLAddItems.EXTRAORDINARY_SOC_WAFER, GTValues.VA[GTValues.UEV], 400, GTLCleanroomType.LAW_CLEANROOM, provider);
            addEngravingRecipe(GTLAddItems.DRAGON_ELEMENT_STARMETAL_WAFER, GTLMaterials.EuvPhotoresist, "kubejs:grating_lithography_mask", GTLAddItems.CHAOS_SOC_WAFER, GTValues.VA[GTValues.UIV], 500, GTLCleanroomType.LAW_CLEANROOM, provider);
            addEngravingRecipe(GTLAddItems.PREPARE_SPACETIME_SOC_WAFER, GTLMaterials.GammaRaysPhotoresist, "gtladditions:spacetime_lens", GTLAddItems.SPACETIME_SOC_WAFER, GTValues.VA[GTValues.UXV], 600, GTLCleanroomType.LAW_CLEANROOM, provider);
            addEngravingRecipe(GTLAddItems.PREPARE_PRIMARY_SOC_WAFER, GTLMaterials.GammaRaysPhotoresist, "gtladditions:spacetime_lens", GTLAddItems.PRIMARY_SOC_WAFER, GTValues.VA[GTValues.OpV], 800, GTLCleanroomType.LAW_CLEANROOM, provider);
        }

        private static void addEngravingRecipe(ItemEntry<Item> input, Material fluid, String noinput, ItemEntry<Item> output, int EUt, int duration, CleanroomType cleanroomType, Consumer<FinishedRecipe> provider) {
            DIMENSIONAL_FOCUS_ENGRAVING_ARRAY_RECIPES.recipeBuilder(GTLAdditions.id(output.asItem().toString()))
                    .inputItems(input).notConsumable(Registries.getItemStack(noinput)).inputFluids(fluid.getFluid(100))
                    .outputItems(output).EUt(EUt).duration(duration).cleanroom(cleanroomType).save(provider);
            GTLAddRecipesTypes.PHOTON_MATRIX_ETCH.recipeBuilder(GTLAdditions.id(output.asItem().toString()))
                    .inputItems(input).notConsumable(Registries.getItemStack(noinput)).inputFluids(fluid.getFluid(75))
                    .outputItems(output).EUt(EUt / 4).duration((int) (duration * 0.75)).cleanroom(cleanroomType).save(provider);
        }
    }

    static class circuitassembler {

        public static void init(Consumer<FinishedRecipe> provider) {
            addCircuitRecipe("bioware_processor", "kubejs:bioware_printed_circuit_board", GTLAddItems.OUTSTANDING_SOC, GTMaterials.Naquadah, GTLMaterials.Quantanium, "kubejs:bioware_processor", GTValues.VA[GTValues.UHV], CleanroomType.STERILE_CLEANROOM, provider);
            addCircuitRecipe("optical_processor", "kubejs:optical_printed_circuit_board", GTLAddItems.EXTRAORDINARY_SOC, GTMaterials.Dubnium, GTLMaterials.Vibranium, "kubejs:optical_processor", GTValues.VA[GTValues.UEV], GTLCleanroomType.LAW_CLEANROOM, provider);
            addCircuitRecipe("exotic_processor", "kubejs:exotic_printed_circuit_board", GTLAddItems.CHAOS_SOC, GTLMaterials.Cinobite, GTLMaterials.HastelloyX78, "kubejs:exotic_processor", GTValues.VA[GTValues.UIV], CleanroomType.STERILE_CLEANROOM, provider);
            generateCircuitRecipes(provider);
        }

        private static void addCircuitRecipe(String id, String inputs, ItemEntry<Item> input, Material material1, Material material2, String output, int EUt, CleanroomType cleanroomType, Consumer<FinishedRecipe> provider) {
            GTLAddRecipeBuilder builder = (GTLAddRecipeBuilder) new GTLAddRecipeBuilder(id + "_0", CIRCUIT_ASSEMBLER_RECIPES)
                    .inputItems(inputs).inputItems(input).inputItems(TagPrefix.wireFine, material1, 8).inputItems(TagPrefix.bolt, material2, 8)
                    .outputItems(Registries.getItemStack(output, 4)).EUt(EUt).cleanroom(cleanroomType);
            if (EUt > GTValues.VA[GTValues.UEV]) {
                builder.inputFluids(GTLMaterials.SuperMutatedLivingSolder.getFluid(144)).duration(EUt <= GTValues.VA[GTValues.UEV] ? 200 : 150).save(provider);
                return;
            }
            GTRecipeBuilder recipe = builder.copy(GTLAdditions.id(id + "_1"));
            builder.inputFluids(GTLMaterials.MutatedLivingSolder.getFluid(144)).duration(EUt <= GTValues.VA[GTValues.UEV] ? 200 : 150).save(provider);
            recipe.inputFluids(GTLMaterials.SuperMutatedLivingSolder.getFluid(72)).duration(EUt <= GTValues.VA[GTValues.UEV] ? 200 : 150).save(provider);
        }

        private static void generateCircuitRecipes(Consumer<FinishedRecipe> provider) {
            new GTLAddRecipeBuilder("cosmic_processor", CIRCUIT_ASSEMBLER_RECIPES)
                    .inputItems("kubejs:cosmic_printed_circuit_board").inputItems(GTLAddItems.SPACETIME_SOC)
                    .inputItems(TagPrefix.wireFine, GTLMaterials.HastelloyX78, 8).inputItems(TagPrefix.plate, GTLMaterials.Crystalmatrix)
                    .inputFluids(GTLMaterials.SuperMutatedLivingSolder.getFluid(288)).EUt(GTValues.VA[GTValues.UXV]).cleanroom(GTLCleanroomType.LAW_CLEANROOM)
                    .outputItems(Registries.getItemStack("kubejs:cosmic_processor", 4)).duration(150).save(provider);
            new GTLAddRecipeBuilder("supracausal_processor", CIRCUIT_ASSEMBLER_RECIPES)
                    .inputItems("kubejs:supracausal_printed_circuit_board").inputItems(GTLAddItems.PRIMARY_SOC)
                    .inputItems(TagPrefix.wireGtDouble, GTLMaterials.Hypogen, 4).inputItems(TagPrefix.plate, GTLMaterials.DraconiumAwakened)
                    .inputFluids(GTLMaterials.SuperMutatedLivingSolder.getFluid(360)).EUt(GTValues.VA[GTValues.OpV]).cleanroom(GTLCleanroomType.LAW_CLEANROOM)
                    .outputItems(Registries.getItemStack("kubejs:supracausal_processor", 4)).duration(150).save(provider);
        }
    }
}
