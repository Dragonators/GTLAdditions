package com.gtladd.gtladditions.data.recipes;

import org.gtlcore.gtlcore.common.data.GTLBlocks;
import org.gtlcore.gtlcore.common.data.GTLMaterials;
import org.gtlcore.gtlcore.utils.Registries;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLY_LINE_RECIPES;

public class AssemblyLine {

    private static final Material[] wire = {
            GTMaterials.SamariumIronArsenicOxide, GTMaterials.IndiumTinBariumTitaniumCuprate, GTMaterials.UraniumRhodiumDinaquadide, GTMaterials.EnrichedNaquadahTriniumEuropiumDuranide,
            GTMaterials.RutheniumTriniumAmericiumNeutronate, GTLMaterials.Enderite, GTLMaterials.Echoite, GTLMaterials.Legendarium, GTLMaterials.DraconiumAwakened, GTLMaterials.Infinity
    };
    private static final Material[] cable = {
            GTMaterials.Graphene, GTMaterials.NiobiumTitanium, GTMaterials.Trinium, GTMaterials.NaquadahAlloy, GTMaterials.Mendelevium, GTLMaterials.Mithril,
            GTLMaterials.Adamantine, GTLMaterials.NaquadriaticTaranium, GTLMaterials.Starmetal, GTLMaterials.CosmicNeutronium
    };

    public AssemblyLine() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        addLaserHatchRecipe(provider);
    }

    private static void addLaserHatchRecipe(Consumer<FinishedRecipe> provider) {
        for (int i = GTValues.IV; i < 15; i++) {
            String tier = GTValues.VN[i].toLowerCase();
            ASSEMBLY_LINE_RECIPES.recipeBuilder(tier + "_16777216a_laser_source_hatch").EUt(GTValues.VA[i]).duration(200).inputItems(Registries.getItemStack("gtceu:" + tier + "_4194304a_laser_source_hatch")).inputItems(TagPrefix.lens, GTMaterials.NetherStar, 64).inputItems(GTLBlocks.IMPROVED_SUPERCONDUCTOR_COIL.asStack(64)).inputItems(GTMachines.POWER_SUBSTATION.asStack(64)).inputItems(TagPrefix.wireGtQuadruple, wire[i - 5], 16).inputItems(TagPrefix.cableGtQuadruple, cable[i - 5], 16).circuitMeta(1).inputFluids(GTMaterials.SodiumPotassium.getFluid(10000)).inputFluids(GTMaterials.SolderingAlloy.getFluid(1296)).outputItems(Registries.getItemStack("gtceu:" + tier + "_16777216a_laser_source_hatch")).save(provider);
            ASSEMBLY_LINE_RECIPES.recipeBuilder(tier + "_67108863a_laser_source_hatch").EUt(GTValues.VA[i]).duration(200).inputItems(Registries.getItemStack("gtceu:" + tier + "_4194304a_laser_source_hatch")).inputItems(TagPrefix.lens, GTMaterials.NetherStar, 256).inputItems(GTLBlocks.IMPROVED_SUPERCONDUCTOR_COIL.asStack(256)).inputItems(GTMachines.POWER_SUBSTATION.asStack(256)).inputItems(TagPrefix.wireGtQuadruple, wire[i - 5], 32).inputItems(TagPrefix.cableGtQuadruple, cable[i - 5], 32).circuitMeta(2).inputFluids(GTMaterials.SodiumPotassium.getFluid(10000)).inputFluids(GTMaterials.SolderingAlloy.getFluid(1296)).outputItems(Registries.getItemStack("gtceu:" + tier + "_67108863a_laser_source_hatch")).save(provider);
            ASSEMBLY_LINE_RECIPES.recipeBuilder(tier + "_16777216a_laser_target_hatch").EUt(GTValues.VA[i]).duration(200).inputItems(Registries.getItemStack("gtceu:" + tier + "_4194304a_laser_source_hatch")).inputItems(TagPrefix.lens, GTMaterials.NetherStar, 64).inputItems(GTLBlocks.IMPROVED_SUPERCONDUCTOR_COIL.asStack(64)).inputItems(GTMachines.POWER_SUBSTATION.asStack(64)).inputItems(TagPrefix.wireGtQuadruple, wire[i - 5], 16).inputItems(TagPrefix.cableGtQuadruple, cable[i - 5], 16).circuitMeta(4).inputFluids(GTMaterials.SodiumPotassium.getFluid(10000)).inputFluids(GTMaterials.SolderingAlloy.getFluid(1296)).outputItems(Registries.getItemStack("gtceu:" + tier + "_16777216a_laser_target_hatch")).save(provider);
            ASSEMBLY_LINE_RECIPES.recipeBuilder(tier + "_67108864a_laser_target_hatch").EUt(GTValues.VA[i]).duration(200).inputItems(Registries.getItemStack("gtceu:" + tier + "_4194304a_laser_source_hatch")).inputItems(TagPrefix.lens, GTMaterials.NetherStar, 256).inputItems(GTLBlocks.IMPROVED_SUPERCONDUCTOR_COIL.asStack(256)).inputItems(GTMachines.POWER_SUBSTATION.asStack(256)).inputItems(TagPrefix.wireGtQuadruple, wire[i - 5], 32).inputItems(TagPrefix.cableGtQuadruple, cable[i - 5], 32).circuitMeta(5).inputFluids(GTMaterials.SodiumPotassium.getFluid(10000)).inputFluids(GTMaterials.SolderingAlloy.getFluid(1296)).outputItems(Registries.getItemStack("gtceu:" + tier + "_67108864a_laser_target_hatch")).save(provider);
            ASSEMBLY_LINE_RECIPES.recipeBuilder(tier + "_268435455a_laser_target_hatch").EUt(GTValues.VA[i]).duration(200).inputItems(Registries.getItemStack("gtceu:" + tier + "_4194304a_laser_source_hatch")).inputItems(TagPrefix.lens, GTMaterials.NetherStar, 1024).inputItems(GTLBlocks.IMPROVED_SUPERCONDUCTOR_COIL.asStack(1024)).inputItems(GTMachines.POWER_SUBSTATION.asStack(1024)).inputItems(TagPrefix.wireGtQuadruple, wire[i - 5], 64).inputItems(TagPrefix.cableGtQuadruple, cable[i - 5], 64).circuitMeta(6).inputFluids(GTMaterials.SodiumPotassium.getFluid(10000)).inputFluids(GTMaterials.SolderingAlloy.getFluid(1296)).outputItems(Registries.getItemStack("gtceu:" + tier + "_268435455a_laser_target_hatch")).save(provider);
            ASSEMBLY_LINE_RECIPES.recipeBuilder(tier + "_16777216a_wireless_laser_source_hatch").EUt(GTValues.VA[i]).duration(200).inputItems(Registries.getItemStack("gtmthings:" + tier + "_4194304a_wireless_laser_source_hatch")).inputItems(TagPrefix.lens, GTMaterials.NetherStar, 64).inputItems(GTLBlocks.IMPROVED_SUPERCONDUCTOR_COIL.asStack(64)).inputItems(GTMachines.POWER_SUBSTATION.asStack(64)).inputItems(TagPrefix.wireGtQuadruple, wire[i - 5], 16).inputItems(TagPrefix.cableGtQuadruple, cable[i - 5], 16).circuitMeta(1).inputFluids(GTMaterials.SodiumPotassium.getFluid(10000)).inputFluids(GTMaterials.SolderingAlloy.getFluid(1296)).outputItems(Registries.getItemStack("gtmthings:" + tier + "_16777216a_wireless_laser_source_hatch")).save(provider);
            ASSEMBLY_LINE_RECIPES.recipeBuilder(tier + "_67108863a_wireless_laser_source_hatch").EUt(GTValues.VA[i]).duration(200).inputItems(Registries.getItemStack("gtmthings:" + tier + "_4194304a_wireless_laser_source_hatch")).inputItems(TagPrefix.lens, GTMaterials.NetherStar, 256).inputItems(GTLBlocks.IMPROVED_SUPERCONDUCTOR_COIL.asStack(256)).inputItems(GTMachines.POWER_SUBSTATION.asStack(256)).inputItems(TagPrefix.wireGtQuadruple, wire[i - 5], 32).inputItems(TagPrefix.cableGtQuadruple, cable[i - 5], 32).circuitMeta(2).inputFluids(GTMaterials.SodiumPotassium.getFluid(10000)).inputFluids(GTMaterials.SolderingAlloy.getFluid(1296)).outputItems(Registries.getItemStack("gtmthings:" + tier + "_67108863a_wireless_laser_source_hatch")).save(provider);
            ASSEMBLY_LINE_RECIPES.recipeBuilder(tier + "_16777216a_wireless_laser_target_hatch").EUt(GTValues.VA[i]).duration(200).inputItems(Registries.getItemStack("gtmthings:" + tier + "_4194304a_wireless_laser_target_hatch")).inputItems(TagPrefix.lens, GTMaterials.NetherStar, 64).inputItems(GTLBlocks.IMPROVED_SUPERCONDUCTOR_COIL.asStack(64)).inputItems(GTMachines.POWER_SUBSTATION.asStack(64)).inputItems(TagPrefix.wireGtQuadruple, wire[i - 5], 16).inputItems(TagPrefix.cableGtQuadruple, cable[i - 5], 16).circuitMeta(4).inputFluids(GTMaterials.SodiumPotassium.getFluid(10000)).inputFluids(GTMaterials.SolderingAlloy.getFluid(1296)).outputItems(Registries.getItemStack("gtmthings:" + tier + "_16777216a_wireless_laser_target_hatch")).save(provider);
            ASSEMBLY_LINE_RECIPES.recipeBuilder(tier + "_67108864a_wireless_laser_target_hatch").EUt(GTValues.VA[i]).duration(200).inputItems(Registries.getItemStack("gtmthings:" + tier + "_4194304a_wireless_laser_target_hatch")).inputItems(TagPrefix.lens, GTMaterials.NetherStar, 256).inputItems(GTLBlocks.IMPROVED_SUPERCONDUCTOR_COIL.asStack(256)).inputItems(GTMachines.POWER_SUBSTATION.asStack(256)).inputItems(TagPrefix.wireGtQuadruple, wire[i - 5], 32).inputItems(TagPrefix.cableGtQuadruple, cable[i - 5], 32).circuitMeta(5).inputFluids(GTMaterials.SodiumPotassium.getFluid(10000)).inputFluids(GTMaterials.SolderingAlloy.getFluid(1296)).outputItems(Registries.getItemStack("gtmthings:" + tier + "_67108864a_wireless_laser_target_hatch")).save(provider);
            ASSEMBLY_LINE_RECIPES.recipeBuilder(tier + "_268435455a_wireless_laser_target_hatch").EUt(GTValues.VA[i]).duration(200).inputItems(Registries.getItemStack("gtmthings:" + tier + "_4194304a_wireless_laser_target_hatch")).inputItems(TagPrefix.lens, GTMaterials.NetherStar, 1024).inputItems(GTLBlocks.IMPROVED_SUPERCONDUCTOR_COIL.asStack(1024)).inputItems(GTMachines.POWER_SUBSTATION.asStack(1024)).inputItems(TagPrefix.wireGtQuadruple, wire[i - 5], 64).inputItems(TagPrefix.cableGtQuadruple, cable[i - 5], 64).circuitMeta(6).inputFluids(GTMaterials.SodiumPotassium.getFluid(10000)).inputFluids(GTMaterials.SolderingAlloy.getFluid(1296)).outputItems(Registries.getItemStack("gtmthings:" + tier + "_268435455a_wireless_laser_target_hatch")).save(provider);
        }
    }
}
