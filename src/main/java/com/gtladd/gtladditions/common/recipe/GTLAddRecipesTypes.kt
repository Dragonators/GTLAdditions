package com.gtladd.gtladditions.common.recipe

import com.gregtechceu.gtceu.api.GTCEuAPI
import com.gregtechceu.gtceu.api.block.ICoilType
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.common.block.CoilBlock
import com.gregtechceu.gtceu.common.data.GTRecipeTypes
import com.gregtechceu.gtceu.common.data.GTSoundEntries
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.common.modify.GTLAddSoundEntries
import com.gtladd.gtladditions.common.modify.RecipesModify
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture.FillDirection
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.utils.CycleItemStackHandler
import com.lowdragmc.lowdraglib.utils.LocalizationUtils
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.client.resources.language.I18n
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.common.data.GTLSoundEntries
import java.util.function.Supplier

@Suppress("JoinDeclarationAndAssignment")
object GTLAddRecipesTypes {
    @JvmField
    val MULTIPLE_TYPE_RECIPES: Set<GTRecipeType>
    val PHOTON_MATRIX_ETCH: GTRecipeType
    val EM_RESONANCE_CONVERSION_FIELD: GTRecipeType
    val TECTONIC_FAULT_GENERATOR: GTRecipeType
    val BIOLOGICAL_SIMULATION: GTRecipeType
    val VOIDFLUX_REACTION: GTRecipeType
    val STELLAR_LGNITION: GTRecipeType
    val CHAOTIC_ALCHEMY: GTRecipeType
    val ANTIENTROPY_CONDENSATION: GTRecipeType
    @JvmField
    val MOLECULAR_DECONSTRUCTION: GTRecipeType
    val UNIVERSE_SANDBOX: GTRecipeType
    val CHAOS_WEAVE: GTRecipeType
    @JvmField
    val GENESIS_ENGINE: GTRecipeType
    val STAR_CORE_STRIPPER: GTRecipeType
    val MATTER_EXOTIC: GTRecipeType
    val INTER_STELLAR: GTRecipeType
    val NIGHTMARE_CRAFTING: GTRecipeType

    val FORGE_OF_THE_ANTICHRIST: GTRecipeType
    val QUANTUM_OSCILLATION: GTRecipeType

    fun init() {
        RecipesModify.init()
    }

    init {
        PHOTON_MATRIX_ETCH = GTRecipeTypes.register("photon_matrix_etch", GTRecipeTypes.MULTIBLOCK)
            .setEUIO(IO.IN).setMaxIOSize(3, 1, 1, 0).setMaxTooltips(4)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT).setSound(GTSoundEntries.ARC)
        EM_RESONANCE_CONVERSION_FIELD =
            GTRecipeTypes.register("em_resonance_conversion_field", GTRecipeTypes.MULTIBLOCK)
                .setEUIO(IO.IN).setMaxIOSize(2, 1, 0, 0)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.ARC)
        TECTONIC_FAULT_GENERATOR = GTRecipeTypes.register("tectonic_fault_generator", GTRecipeTypes.MULTIBLOCK)
            .setEUIO(IO.IN).setMaxIOSize(2, 1, 0, 0)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT).setSound(GTSoundEntries.ARC)
        BIOLOGICAL_SIMULATION = GTRecipeTypes.register("biological_simulation", GTRecipeTypes.MULTIBLOCK)
            .setEUIO(IO.IN).setMaxIOSize(3, 7, 2, 0)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT).setSound(GTSoundEntries.ARC)
        VOIDFLUX_REACTION = GTRecipeTypes.register("voidflux_reaction", GTRecipeTypes.MULTIBLOCK)
            .setEUIO(IO.IN).setMaxIOSize(3, 0, 0, 1)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT).setSound(GTSoundEntries.ARC)
        STELLAR_LGNITION = GTRecipeTypes.register("stellar_lgnition", GTRecipeTypes.MULTIBLOCK)
            .setEUIO(IO.IN)
            .setMaxIOSize(1, 0, 1, 1)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARC_FURNACE, FillDirection.LEFT_TO_RIGHT)
            .addDataInfo { data: CompoundTag? ->
                val temp = data!!.getInt("ebf_temp")
                LocalizationUtils.format("gtceu.recipe.temperature", FormattingUtil.formatNumbers(temp))
            }.addDataInfo { data: CompoundTag? ->
                val temp = data!!.getInt("ebf_temp")
                val requiredCoil = ICoilType.getMinRequiredType(temp)
                if (requiredCoil != null && requiredCoil.material != null) {
                    return@addDataInfo LocalizationUtils.format(
                        "gtceu.recipe.coil.tier",
                        I18n.get(requiredCoil.material!!.unlocalizedName)
                    )
                }
                ""
            }.setUiBuilder { recipe: GTRecipe?, widgetGroup: WidgetGroup? ->
                val temp = recipe!!.data.getInt("ebf_temp")
                val items: MutableList<MutableList<ItemStack>> = ObjectArrayList()
                items.add(
                    GTCEuAPI.HEATING_COILS.entries.stream()
                        .filter { coil -> coil.key.coilTemperature >= temp }
                        .map { coil ->
                            ItemStack(coil.value.get())
                        }.toList()
                )
                widgetGroup!!.addWidget(
                    SlotWidget(
                        CycleItemStackHandler(items), 0,
                        widgetGroup.size.width - 50, widgetGroup.size.height - 40, false, false
                    )
                )
            }.setSound(GTSoundEntries.ARC)
        CHAOTIC_ALCHEMY = GTRecipeTypes.register("chaotic_alchemy", GTRecipeTypes.MULTIBLOCK)
            .setMaxIOSize(9, 0, 3, 1)
            .setEUIO(IO.IN).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
            .setSlotOverlay(false, false, false, GuiTextures.FURNACE_OVERLAY_1)
            .setSlotOverlay(false, false, true, GuiTextures.FURNACE_OVERLAY_1)
            .setSlotOverlay(false, true, false, GuiTextures.FURNACE_OVERLAY_2)
            .setSlotOverlay(false, true, true, GuiTextures.FURNACE_OVERLAY_2)
            .setSlotOverlay(true, true, false, GuiTextures.FURNACE_OVERLAY_2)
            .setSlotOverlay(true, true, true, GuiTextures.FURNACE_OVERLAY_2)
            .addDataInfo { data: CompoundTag? ->
                val temp = data!!.getInt("ebf_temp")
                LocalizationUtils.format("gtceu.recipe.temperature", FormattingUtil.formatNumbers(temp))
            }.addDataInfo { data: CompoundTag? ->
                val temp = data!!.getInt("ebf_temp")
                val requiredCoil = ICoilType.getMinRequiredType(temp)
                if (requiredCoil != null && requiredCoil.material != null) LocalizationUtils.format(
                    "gtceu.recipe.coil.tier", I18n.get(
                        requiredCoil.material!!.unlocalizedName
                    )
                ) else ""
            }.setMaxTooltips(4).setUiBuilder { recipe: GTRecipe?, widgetGroup: WidgetGroup? ->
                val temp = recipe!!.data.getInt("ebf_temp")
                val items: MutableList<MutableList<ItemStack?>?> = ObjectArrayList()
                items.add(
                    GTCEuAPI.HEATING_COILS.entries.stream()
                        .filter { coil: MutableMap.MutableEntry<ICoilType, Supplier<CoilBlock>> -> coil.key.coilTemperature >= temp }
                        .map { coil: MutableMap.MutableEntry<ICoilType, Supplier<CoilBlock>> ->
                            ItemStack(coil.value.get())
                        }
                        .toList())
                widgetGroup!!.addWidget(
                    SlotWidget(
                        CycleItemStackHandler(items),
                        0,
                        widgetGroup.size.width - 25,
                        widgetGroup.size.height - 40,
                        false,
                        false
                    )
                )
            }.setSound(GTSoundEntries.ARC)
        ANTIENTROPY_CONDENSATION = GTRecipeTypes.register("antientropy_condensation", GTRecipeTypes.MULTIBLOCK)
            .setEUIO(IO.IN).setMaxIOSize(2, 2, 2, 1)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT).setSound(GTSoundEntries.ARC)
        MOLECULAR_DECONSTRUCTION = GTRecipeTypes.register("molecular_deconstruction", GTRecipeTypes.MULTIBLOCK)
            .setEUIO(IO.IN).setMaxIOSize(1, 0, 0, 1)
            .setProgressBar(GuiTextures.PROGRESS_BAR_EXTRACT, FillDirection.LEFT_TO_RIGHT).setSound(GTSoundEntries.ARC)
        UNIVERSE_SANDBOX = GTRecipeTypes.register("universe_sandbox", GTRecipeTypes.MULTIBLOCK)
            .setEUIO(IO.IN).setMaxIOSize(1, 120, 1, 18)
            .setProgressBar(GuiTextures.PROGRESS_BAR_EXTRACT, FillDirection.LEFT_TO_RIGHT).setSound(GTSoundEntries.ARC)
        CHAOS_WEAVE = GTRecipeTypes.register("chaos_weave", GTRecipeTypes.MULTIBLOCK)
            .setEUIO(IO.IN).setMaxIOSize(1, 1, 0, 0)
            .setProgressBar(GuiTextures.PROGRESS_BAR_EXTRACT, FillDirection.LEFT_TO_RIGHT).setSound(GTSoundEntries.ARC)
        GENESIS_ENGINE = GTRecipeTypes.register("genesis_engine", GTRecipeTypes.MULTIBLOCK)
            .setEUIO(IO.OUT).setMaxIOSize(2, 1, 0, 0)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
            .setSound(GTLAddSoundEntries.GENESIS_ENGINE)
        STAR_CORE_STRIPPER = GTRecipeTypes.register("star_core_stripper", GTRecipeTypes.MULTIBLOCK)
            .setEUIO(IO.IN).setMaxIOSize(3, 12, 1, 1)
            .setMaxTooltips(1)
            .setProgressBar(GuiTextures.PROGRESS_BAR_MACERATE, FillDirection.LEFT_TO_RIGHT)
            .setSound(GTSoundEntries.MINER)
        MATTER_EXOTIC = GTRecipeTypes.register("matter_exotic", GTRecipeTypes.MULTIBLOCK)
            .setEUIO(IO.IN).setMaxIOSize(2, 1, 4, 1)
            .setMaxTooltips(1)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
            .setSound(GTLSoundEntries.FUSIONLOOP)
        INTER_STELLAR = GTRecipeTypes.register("inter_stellar", GTRecipeTypes.MULTIBLOCK)
            .setEUIO(IO.IN).setMaxIOSize(1, 0, 0, 0)
            .setMaxTooltips(1)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
            .setSound(GTLAddSoundEntries.INTER_STELLAR)
        NIGHTMARE_CRAFTING = GTRecipeTypes.register("nightmare_crafting", GTRecipeTypes.MULTIBLOCK)
            .setEUIO(IO.IN).setMaxIOSize(16, 1, 1, 0)
            .setMaxTooltips(1)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
            .setSound(GTSoundEntries.SCIENCE)
        FORGE_OF_THE_ANTICHRIST = GTRecipeTypes.register("forge_of_the_antichrist", GTRecipeTypes.DUMMY)
            .setXEIVisible(false)
            .setSound(GTLAddSoundEntries.FORGE_OF_THE_ANTICHRIST)
        QUANTUM_OSCILLATION = GTRecipeTypes.register("quantum_oscillation", GTRecipeTypes.DUMMY)
            .setXEIVisible(false)
            .setSound(GTLAddSoundEntries.QUANTUM_OSCILLATION)

        MULTIPLE_TYPE_RECIPES = setOf(
            FORGE_OF_THE_ANTICHRIST,
            QUANTUM_OSCILLATION
        )
    }
}
