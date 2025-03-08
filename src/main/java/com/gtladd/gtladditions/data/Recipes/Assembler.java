package com.gtladd.gtladditions.data.Recipes;

import org.gtlcore.gtlcore.common.data.GTLMaterials;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.api.Registry.GTLAddRecipeBuilder;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLER_RECIPES;

public class Assembler {

    public Assembler() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        new GTLAddRecipeBuilder("naquadria_charge_more", ASSEMBLER_RECIPES)
                .inputItems("gtceu:quantum_star")
                .inputItems("gtceu:industrial_tnt")
                .inputItems("gtceu:naquadria_dust")
                .inputItems("gtceu:tiny_hexanitrohexaaxaisowurtzitane_dust", 4)
                .inputItems("gtceu:double_thorium_plate")
                .inputFluids(GTLMaterials.Antimatter.getFluid(1))
                .outputItems("kubejs:naquadria_charge", 64)
                .TierEUtVA(13)
                .duration(200)
                .save(provider);

        new GTLAddRecipeBuilder("leptonic_charge", ASSEMBLER_RECIPES)
                .inputItems("gtceu:gravi_star")
                .inputItems("gtceu:industrial_tnt")
                .inputItems("gtceu:degenerate_rhenium_dust")
                .inputItems("gtceu:small_hexanitrohexaaxaisowurtzitane_dust", 2)
                .inputItems("gtceu:double_enderium_plate")
                .inputFluids(GTLMaterials.Antimatter.getFluid(10))
                .outputItems("kubejs:leptonic_charge", 64)
                .TierEUtVA(14)
                .duration(200)
                .save(provider);

        new GTLAddRecipeBuilder("quantum_chromodynamic_charge", ASSEMBLER_RECIPES)
                .notConsumable("gtceu:eternity_nanoswarm")
                .inputItems("kubejs:unstable_star")
                .inputItems("kubejs:leptonic_charge")
                .inputItems("kubejs:quantumchromodynamic_protective_plating")
                .inputFluids(GTLMaterials.Antimatter.getFluid(100))
                .outputItems("kubejs:quantum_chromodynamic_charge", 64)
                .duration(200)
                .EUt(4L * GTValues.VA[GTValues.MAX])
                .save(provider);

        for (int i = 1; i < 9; i++) {
            String tierName = GTValues.VN[i].toLowerCase();
            new GTLAddRecipeBuilder(tierName + "_world_accelerator", ASSEMBLER_RECIPES)
                    .circuitMeta(24)
                    .inputItems("gtceu:" + tierName + "_field_generator", 4)
                    .inputItems("gtceu:" + tierName + "_emitter", 2)
                    .inputItems("gtceu:" + tierName + "_sensor", 2)
                    .inputItems("gtceu:" + tierName + "_machine_hull")
                    .outputItems("gtceu:" + tierName + "_world_accelerator")
                    .duration(200).EUt(480).save(provider);
        }

        String[][] hermetic_casing = {
                { "lv", "", "super", "lv", "gtceu", "steel", "polyethylene_large_fluid_pipe" },
                { "mv", "", "super", "mv", "gtceu", "aluminium", "polyvinyl_chloride_large_item_pipe" },
                { "hv", "lv", "super", "hv", "gtceu", "stainless_steel", "polytetrafluoroethylene_large_fluid_pipe" },
                { "ev", "mv", "super", "ev", "gtceu", "titanium", "stainless_steel_large_fluid_pipe" },
                { "iv", "hv", "quantum", "iv", "gtceu", "dense_tungsten_steel", "titanium_large_fluid_pipe" },
                { "luv", "ev", "quantum", "luv", "gtceu", "dense_rhodium_plated_palladium", "tungsten_steel_large_fluid_pipe" },
                { "zpm", "iv", "quantum", "zpm", "gtceu", "dense_naquadah_alloy", "niobium_titanium_large_fluid_pipe" },
                { "uv", "luv", "quantum", "uv", "gtceu", "dense_darmstadtium", "naquadah_large_fluid_pipe" },
                { "uv", "zpm", "quantum", "uhv", "gtceu", "neutronium", "duranium_large_fluid_pipe" },
                { "uhv", "uv", "quantum", "uev", "gtlcore", "quantanium", "neutronium_large_fluid_pipe" },
                { "uev", "uhv", "quantum", "uiv", "gtlcore", "adamantium", "neutronium_large_fluid_pipe" },
                { "uiv", "uev", "quantum", "uxv", "gtlcore", "vibranium", "enderium_large_fluid_pipe" },
                { "uxv", "uiv", "quantum", "opv", "gtlcore", "draconium", "heavy_quark_degenerate_matter_large_fluid_pipe" }
        };
        for (String[] val : hermetic_casing) {
            new GTLAddRecipeBuilder(val[3] + "_hermetic_casing", ASSEMBLER_RECIPES)
                    .circuitMeta(3)
                    .inputItems("gtceu:" + val[5] + "_plate", 8)
                    .inputItems("gtceu:" + val[6])
                    .outputItems(val[4] + ":" + val[3] + "_hermetic_casing")
                    .TierEUtVA(3)
                    .duration(400)
                    .save(provider);
            for (String s : new String[] { "_tank", "_chest" }) {
                GTLAddRecipeBuilder builder = new GTLAddRecipeBuilder(val[3] + "_" + val[2] + s, ASSEMBLER_RECIPES)
                        .circuitMeta(s.equals("_tank") ? 9 : 10)
                        .inputItemsModTag("circuits/" + val[3], 4);
                if (s.equals("_tank")) builder.inputItems("gtceu:" + val[5] + "_plate", val[1].equals("") ? 3 : 2)
                        .inputItems(val[4] + ":" + val[3] + "_hermetic_casing")
                        .inputItems("gtceu:" + val[0] + "_electric_pump");
                if (s.equals("_chest")) builder.inputItems("gtceu:" + val[5] + "_plate", val[1].equals("") ? 4 : 3)
                        .inputItems(val[2].equals("super") ? "gtceu:" + val[5] + "_crate" : "gtceu:" + val[3] + "_machine_hull");
                if (!val[1].equals("")) builder.inputItems("gtceu:" + val[1] + "_field_generator");
                builder.outputItems("gtceu:" + val[3] + "_" + val[2] + s).TierEUtVA(4).duration(1200).save(provider);
            }
        }

        String[][] diode = {
                { "lv", "diodes", "steel", "tin_quadruple" },
                { "mv", "diodes", "aluminium", "copper_quadruple" },
                { "hv", "gtceu:smd_diode", "stainless_steel", "gold_quadruple" },
                { "ev", "gtceu:smd_diode", "titanium", "aluminium_quadruple" },
                { "iv", "gtceu:smd_diode", "tungsten_steel", "platinum_quadruple" },
                { "luv", "gtceu:advanced_smd_diode", "rhodium_plated_palladium", "niobium_titanium_quadruple" },
                { "zpm", "gtceu:advanced_smd_diode", "naquadah_alloy", "vanadium_gallium_quadruple" },
                { "uv", "gtceu:advanced_smd_diode", "darmstadtium", "yttrium_barium_cuprate_quadruple" },
                { "uhv", "gtceu:advanced_smd_diode", "neutronium", "europium_quadruple" },
                { "uev", "gtceu:advanced_smd_diode", "quantanium", "mithril_double" },
                { "uiv", "gtceu:advanced_smd_diode", "adamantium", "neutronium_double" },
                { "uxv", "gtceu:advanced_smd_diode", "vibranium", "taranium_double" },
                { "opv", "gtceu:advanced_smd_diode", "draconium", "crystalmatrix_double" }
        };
        for (String[] val : diode) {
            GTLAddRecipeBuilder builder = new GTLAddRecipeBuilder(val[0] + "_diode", ASSEMBLER_RECIPES)
                    .circuitMeta(9);
            if (val[0].equals("lv") || val[0].equals("mv")) {
                builder.inputItemsModTag(val[1], 4);
            } else {
                builder.inputItems(val[1], 4);
            }
            builder.inputItems("gtceu:" + val[2] + "_plate", 2)
                    .inputItems("gtceu:" + val[3] + "_cable", 2)
                    .inputItems("gtceu:" + val[0] + "_machine_hull")
                    .outputItems("gtceu:" + val[0] + "_diode")
                    .TierEUtVA(4)
                    .duration(1200)
                    .save(provider);
        }
    }
}
