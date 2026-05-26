package com.gtladd.gtladditions.data.recipes

import com.gregtechceu.gtceu.api.data.tag.TagPrefix.block
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.bolt
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.dust
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.foil
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.frameGt
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.gear
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.ingot
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.nugget
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.plate
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.plateDouble
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.rod
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.rodLong
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.screw
import com.gregtechceu.gtceu.api.data.tag.TagPrefix.wireFine
import com.gregtechceu.gtceu.common.data.GCyMRecipeTypes.ALLOY_BLAST_RECIPES
import com.gregtechceu.gtceu.common.data.GTItems
import com.gregtechceu.gtceu.common.data.GTMaterials
import com.gregtechceu.gtceu.common.data.GTMaterials.Iridium
import com.gregtechceu.gtceu.common.data.GTMaterials.Ruridit
import com.gregtechceu.gtceu.common.data.GTMaterials.Ruthenium
import com.gregtechceu.gtceu.common.data.GTRecipeTypes
import com.gregtechceu.gtceu.common.data.GTRecipeTypes.EXTRACTOR_RECIPES
import com.gregtechceu.gtceu.common.data.GTRecipeTypes.FLUID_SOLIDFICATION_RECIPES
import com.gtladd.gtladditions.GTLAdditions.Companion.id
import com.gtladd.gtladditions.common.material.GTLAddMaterial
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import net.minecraft.data.recipes.FinishedRecipe
import java.util.function.Consumer

object RuriditExtend {
    fun init(provider: Consumer<FinishedRecipe?>) {
        ALLOY_BLAST_RECIPES.recipeBuilder(id("ruridit"))
            .inputItems(dust, Ruthenium, 2)
            .inputItems(dust, Iridium)
            .circuitMeta(3)
            .outputFluids(GTLAddMaterial.MOLTEN_RURIDIT.getFluid(432))
            .blastFurnaceTemp(2300)
            .EUt(3840).duration(1085).save(provider)

        ALLOY_BLAST_RECIPES.recipeBuilder(id("ruridit_gas"))
            .inputItems(dust, Ruthenium, 2)
            .inputItems(dust, Iridium)
            .circuitMeta(13)
            .inputFluids(GTMaterials.Argon.getFluid(150))
            .outputFluids(GTLAddMaterial.MOLTEN_RURIDIT.getFluid(432))
            .blastFurnaceTemp(2300)
            .EUt(3840).duration(716).save(provider)

        GTRecipeTypes.VACUUM_RECIPES.recipeBuilder(id("ruridit"))
            .notConsumable(GTItems.SHAPE_MOLD_INGOT)
            .inputFluids(GTLAddMaterial.MOLTEN_RURIDIT.getFluid(144))
            .outputItems(ingot, Ruridit)
            .EUt(120).duration(393).save(provider)

        EXTRACTOR_RECIPES.recipeBuilder(id("extract_ruridit_ingot"))
            .inputItems(ingot, Ruridit)
            .outputFluids(GTLAddMaterial.LIQUID_RURIDIT.getFluid(144))
            .EUt(480).duration(105).save(provider)

        EXTRACTOR_RECIPES.recipeBuilder(id("extract_ruridit_plate"))
            .inputItems(plate, Ruridit)
            .outputFluids(GTLAddMaterial.LIQUID_RURIDIT.getFluid(144))
            .EUt(480).duration(105).save(provider)

        EXTRACTOR_RECIPES.recipeBuilder(id("extract_long_ruridit_rod"))
            .inputItems(rodLong, Ruridit)
            .outputFluids(GTLAddMaterial.LIQUID_RURIDIT.getFluid(144))
            .EUt(480).duration(105).save(provider)

        EXTRACTOR_RECIPES.recipeBuilder(id("extract_ruridit_rod"))
            .inputItems(rod, Ruridit)
            .outputFluids(GTLAddMaterial.LIQUID_RURIDIT.getFluid(72))
            .EUt(480).duration(52).save(provider)

        EXTRACTOR_RECIPES.recipeBuilder(id("extract_ruridit_gear"))
            .inputItems(gear, Ruridit)
            .outputFluids(GTLAddMaterial.LIQUID_RURIDIT.getFluid(576))
            .EUt(480).duration(420).save(provider)

        EXTRACTOR_RECIPES.recipeBuilder(id("extract_ruridit_block"))
            .inputItems(block, Ruridit)
            .outputFluids(GTLAddMaterial.LIQUID_RURIDIT.getFluid(1296))
            .EUt(480).duration(945).save(provider)

        EXTRACTOR_RECIPES.recipeBuilder(id("extract_ruridit_nugget"))
            .inputItems(nugget, Ruridit)
            .outputFluids(GTLAddMaterial.LIQUID_RURIDIT.getFluid(16))
            .EUt(480).duration(12).save(provider)

        EXTRACTOR_RECIPES.recipeBuilder(id("extract_ruridit_bolt"))
            .inputItems(bolt, Ruridit)
            .outputFluids(GTLAddMaterial.LIQUID_RURIDIT.getFluid(18))
            .EUt(480).duration(13).save(provider)

        EXTRACTOR_RECIPES.recipeBuilder(id("extract_fine_ruridit_wire"))
            .inputItems(wireFine, Ruridit)
            .outputFluids(GTLAddMaterial.LIQUID_RURIDIT.getFluid(18))
            .EUt(480).duration(13).save(provider)

        EXTRACTOR_RECIPES.recipeBuilder(id("extract_double_ruridit_plate"))
            .inputItems(plateDouble, Ruridit)
            .outputFluids(GTLAddMaterial.LIQUID_RURIDIT.getFluid(288))
            .EUt(480).duration(210).save(provider)

        EXTRACTOR_RECIPES.recipeBuilder(id("extract_ruridit_foil"))
            .inputItems(foil, Ruridit)
            .outputFluids(GTLAddMaterial.LIQUID_RURIDIT.getFluid(36))
            .EUt(480).duration(26).save(provider)

        EXTRACTOR_RECIPES.recipeBuilder(id("extract_ruridit_frame"))
            .inputItems(frameGt, Ruridit)
            .outputFluids(GTLAddMaterial.LIQUID_RURIDIT.getFluid(288))
            .EUt(480).duration(210).save(provider)

        EXTRACTOR_RECIPES.recipeBuilder(id("extract_ruridit_screw"))
            .inputItems(screw, Ruridit)
            .outputFluids(GTLAddMaterial.LIQUID_RURIDIT.getFluid(16))
            .EUt(480).duration(12).save(provider)

        FLUID_SOLIDFICATION_RECIPES.recipeBuilder(id("ruridit_to_ingot"))
            .notConsumable(GTItems.SHAPE_MOLD_INGOT)
            .inputFluids(GTLAddMaterial.LIQUID_RURIDIT.getFluid(144))
            .outputItems(ingot, Ruridit)
            .EUt(7).duration(20).save(provider)

        FLUID_SOLIDFICATION_RECIPES.recipeBuilder(id("ruridit_to_plate"))
            .notConsumable(GTItems.SHAPE_MOLD_PLATE)
            .inputFluids(GTLAddMaterial.LIQUID_RURIDIT.getFluid(144))
            .outputItems(plate, Ruridit)
            .EUt(7).duration(40).save(provider)

        FLUID_SOLIDFICATION_RECIPES.recipeBuilder(id("ruridit_to_nugget"))
            .notConsumable(GTItems.SHAPE_MOLD_NUGGET)
            .inputFluids(GTLAddMaterial.LIQUID_RURIDIT.getFluid(144))
            .outputItems(nugget, Ruridit, 9)
            .EUt(7).duration(105).save(provider)

        FLUID_SOLIDFICATION_RECIPES.recipeBuilder(id("ruridit_to_block"))
            .notConsumable(GTItems.SHAPE_MOLD_BLOCK)
            .inputFluids(GTLAddMaterial.LIQUID_RURIDIT.getFluid(1296))
            .outputItems(block, Ruridit)
            .EUt(7).duration(105).save(provider)

        FLUID_SOLIDFICATION_RECIPES.recipeBuilder(id("ruridit_to_gear"))
            .notConsumable(GTItems.SHAPE_MOLD_GEAR)
            .inputFluids(GTLAddMaterial.LIQUID_RURIDIT.getFluid(576))
            .outputItems(gear, Ruridit)
            .EUt(7).duration(100).save(provider)

        GTLAddRecipesTypes.MOLECULAR_DECONSTRUCTION.recipeBuilder(id("molecular_deconstruction_ruridit_dust"))
            .inputItems(dust, Ruridit)
            .outputFluids(GTLAddMaterial.LIQUID_RURIDIT.getFluid(144))
            .EUt(120).duration(80).save(provider)

        GTLAddRecipesTypes.CHAOTIC_ALCHEMY.recipeBuilder(id("ruridit"))
            .inputItems(dust, Ruthenium, 2)
            .inputItems(dust, Iridium)
            .circuitMeta(3)
            .outputFluids(GTLAddMaterial.LIQUID_RURIDIT.getFluid(432))
            .blastFurnaceTemp(2300)
            .EUt(3840).duration(814).save(provider)

        GTLAddRecipesTypes.CHAOTIC_ALCHEMY.recipeBuilder(id("ruridit_gas"))
            .inputItems(dust, Ruthenium, 2)
            .inputItems(dust, Iridium)
            .circuitMeta(13)
            .inputFluids(GTMaterials.Argon.getFluid(150))
            .outputFluids(GTLAddMaterial.LIQUID_RURIDIT.getFluid(432))
            .blastFurnaceTemp(2300)
            .EUt(3840).duration(537).save(provider)
    }
}