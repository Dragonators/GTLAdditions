package com.gtladd.gtladditions.data.recipes.newmachinerecipe

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.data.chemical.material.MarkerMaterial
import com.gregtechceu.gtceu.api.data.chemical.material.MarkerMaterials
import com.gregtechceu.gtceu.api.data.chemical.material.Material
import com.gregtechceu.gtceu.api.data.tag.TagPrefix
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType
import com.gregtechceu.gtceu.common.data.GTItems
import com.gregtechceu.gtceu.common.data.GTRecipeTypes
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.registry.GTLAddRecipeBuilder
import com.gtladd.gtladditions.common.items.GTLAddItems
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import com.tterrag.registrate.util.entry.ItemEntry
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.world.item.Item
import org.gtlcore.gtlcore.common.data.GTLMaterials.*
import java.util.function.Consumer

object PhotonMatrixEtch {
    @JvmStatic
    fun init(provider : Consumer<FinishedRecipe?>) {
        Engraving.init(provider)
        addRecipe(
            "raw_photon_carrying_wafer",
            "kubejs:rutherfordium_neutronium_wafer",
            "kubejs:lithography_mask",
            "gtceu:yellow_glass_lens",
            240, 8, Photoresist, provider
        )
        addRecipe(
            "nm_wafer",
            "kubejs:rutherfordium_neutronium_wafer",
            "kubejs:lithography_mask",
            "gtceu:orange_glass_lens",
            400, 7, Photoresist, provider
        )
        addRecipe(
            "pm_wafer",
            "kubejs:taranium_wafer",
            "kubejs:lithography_mask",
            "gtceu:lime_glass_lens",
            800, 8, EuvPhotoresist, provider
        )
        addRecipe(
            "fm_wafer",
            "kubejs:pm_wafer",
            "kubejs:grating_lithography_mask",
            "gtceu:pink_glass_lens",
            1080, 9, GammaRaysPhotoresist, provider
        )
        addRecipe(
            "prepared_cosmic_soc_wafer",
            "kubejs:taranium_wafer",
            "kubejs:lithography_mask",
            "gtceu:light_gray_glass_lens",
            2160, 10, GammaRaysPhotoresist, provider
        )
        addRecipe(
            "high_precision_crystal_soc",
            "gtceu:crystal_soc",
            "kubejs:lithography_mask",
            "gtceu:cyan_glass_lens",
            960, 9, EuvPhotoresist, provider
        )
    }

    private fun addRecipe(
        id : String,
        input : String,
        notitem_1 : String,
        notitem_2 : String,
        duration : Int,
        EUt : Int,
        Fluid : Material,
        provider : Consumer<FinishedRecipe?>
    ) {
        GTLAddRecipeBuilder(id, GTLAddRecipesTypes.PHOTON_MATRIX_ETCH).inputItems(input).notConsumable(notitem_2)
            .notConsumable(notitem_1)
            .inputFluids(Fluid.getFluid(50))
            .outputItems("kubejs:$id")
            .TierEUtVA(EUt).duration(duration)
            .cleanroom(CleanroomType.CLEANROOM).save(provider)
    }

    private object Engraving {
        fun init(provider : Consumer<FinishedRecipe?>) {
            addRecipe(
                "engrave_ilc_periodicium",
                GTLAddItems.PERIODICIUM_WAFER !!,
                MarkerMaterials.Color.Red,
                GTItems.INTEGRATED_LOGIC_CIRCUIT_WAFER,
                256, 6, GTValues.VA[GTValues.UHV], provider
            )
            addRecipe(
                "engrave_ram_periodicium",
                GTLAddItems.PERIODICIUM_WAFER !!,
                MarkerMaterials.Color.Green,
                GTItems.RANDOM_ACCESS_MEMORY_WAFER,
                256, 6, GTValues.VA[GTValues.UHV], provider
            )
            addRecipe(
                "engrave_cpu_periodicium",
                GTLAddItems.PERIODICIUM_WAFER !!,
                MarkerMaterials.Color.LightBlue,
                GTItems.CENTRAL_PROCESSING_UNIT_WAFER,
                256, 6, GTValues.VA[GTValues.UHV], provider
            )
            addRecipe(
                "engrave_ulpic_periodicium",
                GTLAddItems.PERIODICIUM_WAFER !!,
                MarkerMaterials.Color.Blue,
                GTItems.ULTRA_LOW_POWER_INTEGRATED_CIRCUIT_WAFER,
                256, 6, GTValues.VA[GTValues.UHV], provider
            )
            addRecipe(
                "engrave_lpic_periodicium",
                GTLAddItems.PERIODICIUM_WAFER !!,
                MarkerMaterials.Color.Orange,
                GTItems.LOW_POWER_INTEGRATED_CIRCUIT_WAFER,
                256, 6, GTValues.VA[GTValues.UHV], provider
            )
            addRecipe(
                "engrave_ssoc_periodicium",
                GTLAddItems.PERIODICIUM_WAFER !!,
                MarkerMaterials.Color.Cyan,
                GTItems.SIMPLE_SYSTEM_ON_CHIP_WAFER,
                256, 6, GTValues.VA[GTValues.UHV], provider
            )
            addRecipe(
                "engrave_nand_periodicium",
                GTLAddItems.PERIODICIUM_WAFER !!,
                MarkerMaterials.Color.Gray,
                GTItems.NAND_MEMORY_CHIP_WAFER,
                128, 13, GTValues.VA[GTValues.UHV], provider
            )
            addRecipe(
                "engrave_nor_periodicium",
                GTLAddItems.PERIODICIUM_WAFER !!,
                MarkerMaterials.Color.Pink,
                GTItems.NOR_MEMORY_CHIP_WAFER,
                128, 13, GTValues.VA[GTValues.UHV], provider
            )
            addRecipe(
                "engrave_pic_periodicium",
                GTLAddItems.PERIODICIUM_WAFER !!,
                MarkerMaterials.Color.Brown,
                GTItems.POWER_INTEGRATED_CIRCUIT_WAFER,
                128, 13, GTValues.VA[GTValues.UHV], provider
            )
            addRecipe(
                "engrave_soc_periodicium",
                GTLAddItems.PERIODICIUM_WAFER !!,
                MarkerMaterials.Color.Yellow,
                GTItems.SYSTEM_ON_CHIP_WAFER,
                128, 13, GTValues.VA[GTValues.UHV], provider
            )
            addRecipe(
                "engrave_asoc_periodicium",
                GTLAddItems.PERIODICIUM_WAFER !!,
                MarkerMaterials.Color.Purple,
                GTItems.ADVANCED_SYSTEM_ON_CHIP_WAFER,
                32, 50, GTValues.VA[GTValues.UHV], provider
            )
            addRecipe(
                "engrave_hasoc_periodicium",
                GTLAddItems.PERIODICIUM_WAFER !!,
                MarkerMaterials.Color.Black,
                GTItems.HIGHLY_ADVANCED_SOC_WAFER,
                16, 80, GTValues.VA[GTValues.UHV], provider
            )
        }

        fun addRecipe(
            id : String?,
            input : ItemEntry<Item?>,
            color : MarkerMaterial,
            output : ItemEntry<Item?>,
            count : Int,
            duration : Int,
            EUt : Int,
            provider : Consumer<FinishedRecipe?>
        ) {
            GTRecipeTypes.LASER_ENGRAVER_RECIPES.recipeBuilder(GTLAdditions.id(id)).inputItems(input)
                .notConsumable(TagPrefix.lens, color)
                .outputItems(output, count)
                .duration(duration).EUt(EUt.toLong())
                .cleanroom(CleanroomType.CLEANROOM).save(provider)
        }
    }
}
