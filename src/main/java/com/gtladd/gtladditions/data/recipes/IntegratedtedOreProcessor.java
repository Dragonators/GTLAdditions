package com.gtladd.gtladditions.data.recipes;

import org.gtlcore.gtlcore.common.data.GTLMaterials;
import org.gtlcore.gtlcore.config.ConfigHolder;

import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.api.registry.GTLAddRecipeBuilder;

import java.util.Objects;
import java.util.function.Consumer;

import static org.gtlcore.gtlcore.common.data.GTLRecipeTypes.INTEGRATED_ORE_PROCESSOR;

public class IntegratedtedOreProcessor {

    public static final int orenumber = ConfigHolder.INSTANCE.oreMultiplier;
    public static final int orefluid = 100 * orenumber;

    public IntegratedtedOreProcessor() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        String[][] platinum_group_sludge_dust_list = {
                { "cooperite", "nickel", "palladium", "mercury" },
                { "bornite", "pyrite", "gold", "mercury" },
                { "tetrahedrite", "antimony", "cadmium", "sodium_persulfate" },
                { "chalcocite", "sulfur" }
        };
        for (String[] pure : platinum_group_sludge_dust_list) {
            new GTLAddRecipeBuilder("purified_" + pure[0] + "_ore_8", INTEGRATED_ORE_PROCESSOR)
                    .circuitMeta(8).inputItemsTag("ores/" + pure[0])
                    .inputFluids(GTMaterials.DistilledWater.getFluid(2L * orefluid))
                    .outputItems("gtceu:purified_" + pure[0] + "_ore", 2 * orenumber)
                    .chancedOutputItems("gtceu:" + pure[1] + "_dust", 14, 8.5)
                    .chancedOutputItems("gtceu:" + pure[1] + "_dust", 2 * orenumber, 33, 0)
                    .outputItems(TagPrefix.dust, GTMaterials.Stone, 2 * orenumber)
                    .EUt(30).duration(26 + 200 * 2 * orenumber).save(provider);
            if (Objects.equals(pure[0], "chalcocite")) return;
            new GTLAddRecipeBuilder("purified_" + pure[0] + "_ore_9", INTEGRATED_ORE_PROCESSOR)
                    .circuitMeta(9).inputItemsTag("ores/" + pure[0])
                    .inputFluids("gtceu:" + pure[3], 2 * orefluid)
                    .outputItems("gtceu:purified_" + pure[0] + "_ore", 2 * orenumber)
                    .chancedOutputItems("gtceu:" + pure[1] + "_dust", 14, 8.5)
                    .chancedOutputItems("gtceu:" + pure[1] + "_dust", 2 * orenumber, 33, 0)
                    .outputItems(TagPrefix.dust, GTMaterials.Stone, 2 * orenumber)
                    .EUt(30).duration(26 + 200 * 2 * orenumber).save(provider);

        }

        new GTLAddRecipeBuilder("jasper_processed", INTEGRATED_ORE_PROCESSOR)
                .circuitMeta(8)
                .inputItemsTag("ores/jasper")
                .inputFluids(GTMaterials.DistilledWater.getFluid(2L * orefluid))
                .outputItems(TagPrefix.dust, GTLMaterials.Jasper, 2 * orenumber)
                .chancedOutput(TagPrefix.dust, GTMaterials.Talc, 1400, 850)
                .chancedOutput(TagPrefix.dust, GTMaterials.Talc, 2 * orenumber, 3300, 0)
                .outputItems(TagPrefix.dust, GTMaterials.Stone, 2 * orenumber)
                .chancedOutput(TagPrefix.dust, GTMaterials.Boron, 2 * orenumber, 1400, 850)
                .chancedOutput(TagPrefix.dust, GTLMaterials.RawTengam, 2 * orenumber, 1000, 0)
                .chancedOutput(TagPrefix.dust, GTLMaterials.RawTengam, 2 * orenumber, 500, 0)
                .EUt(30)
                .duration(26 + 800 * 2 * orenumber)
                .save(provider);
    }
}
