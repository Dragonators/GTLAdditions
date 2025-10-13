package com.gtladd.gtladditions.common.recipe;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.GTSoundEntries;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture.FillDirection;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.utils.CycleItemStackHandler;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import com.gtladd.gtladditions.common.data.GTLAddSoundEntries;
import com.gtladd.gtladditions.common.data.RecipesModify;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static com.lowdragmc.lowdraglib.gui.texture.ProgressTexture.FillDirection.LEFT_TO_RIGHT;

public class GTLAddRecipesTypes {

    public static final Set<GTRecipeType> MULTIPLE_TYPE_RECIPES;
    public static final GTRecipeType PHOTON_MATRIX_ETCH;
    public static final GTRecipeType EM_RESONANCE_CONVERSION_FIELD;
    public static final GTRecipeType TECTONIC_FAULT_GENERATOR;
    public static final GTRecipeType BIOLOGICAL_SIMULATION;
    public static final GTRecipeType VOIDFLUX_REACTION;
    public static final GTRecipeType STELLAR_LGNITION;
    public static final GTRecipeType CHAOTIC_ALCHEMY;
    public static final GTRecipeType ANTIENTROPY_CONDENSATION;
    public static final GTRecipeType MOLECULAR_DECONSTRUCTION;
    public static final GTRecipeType UNIVERSE_SANDBOX;
    public static final GTRecipeType CHAOS_WEAVE;
    public static final GTRecipeType GENESIS_ENGINE;
    public static final GTRecipeType STAR_CORE_STRIPPER;

    public static final GTRecipeType FORGE_OF_THE_ANTICHRIST;
    public static final GTRecipeType QUANTUM_OSCILLATION;

    public GTLAddRecipesTypes() {}

    public static void init() {
        RecipesModify.init();
    }

    static {
        PHOTON_MATRIX_ETCH = GTRecipeTypes.register("photon_matrix_etch", GTRecipeTypes.MULTIBLOCK)
                .setEUIO(IO.IN).setMaxIOSize(3, 1, 1, 0).setMaxTooltips(4)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT).setSound(GTSoundEntries.ARC);
        EM_RESONANCE_CONVERSION_FIELD = GTRecipeTypes.register("em_resonance_conversion_field", GTRecipeTypes.MULTIBLOCK)
                .setEUIO(IO.IN).setMaxIOSize(2, 1, 0, 0)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT).setSound(GTSoundEntries.ARC);
        TECTONIC_FAULT_GENERATOR = GTRecipeTypes.register("tectonic_fault_generator", GTRecipeTypes.MULTIBLOCK)
                .setEUIO(IO.IN).setMaxIOSize(2, 1, 0, 0)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT).setSound(GTSoundEntries.ARC);
        BIOLOGICAL_SIMULATION = GTRecipeTypes.register("biological_simulation", GTRecipeTypes.MULTIBLOCK)
                .setEUIO(IO.IN).setMaxIOSize(3, 7, 2, 0)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT).setSound(GTSoundEntries.ARC);
        VOIDFLUX_REACTION = GTRecipeTypes.register("voidflux_reaction", GTRecipeTypes.MULTIBLOCK)
                .setEUIO(IO.IN).setMaxIOSize(3, 0, 0, 1)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT).setSound(GTSoundEntries.ARC);
        STELLAR_LGNITION = GTRecipeTypes.register("stellar_lgnition", GTRecipeTypes.MULTIBLOCK)
                .setEUIO(IO.IN)
                .setMaxIOSize(1, 0, 1, 1)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARC_FURNACE, FillDirection.LEFT_TO_RIGHT)
                .addDataInfo(data -> {
                    int temp = data.getInt("ebf_temp");
                    return LocalizationUtils.format("gtceu.recipe.temperature", FormattingUtil.formatNumbers(temp));
                }).addDataInfo(data -> {
                    int temp = data.getInt("ebf_temp");
                    ICoilType requiredCoil = ICoilType.getMinRequiredType(temp);
                    if (requiredCoil != null && requiredCoil.getMaterial() != null) {
                        return LocalizationUtils.format("gtceu.recipe.coil.tier",
                                I18n.get(requiredCoil.getMaterial().getUnlocalizedName()));
                    }
                    return "";
                }).setUiBuilder((recipe, widgetGroup) -> {
                    int temp = recipe.data.getInt("ebf_temp");
                    List<List<ItemStack>> items = new ArrayList<>();
                    items.add(GTCEuAPI.HEATING_COILS.entrySet().stream()
                            .filter(coil -> coil.getKey().getCoilTemperature() >= temp)
                            .map(coil -> new ItemStack(coil.getValue().get())).toList());
                    widgetGroup.addWidget(new SlotWidget(new CycleItemStackHandler(items), 0,
                            widgetGroup.getSize().width - 50, widgetGroup.getSize().height - 40, false, false));
                }).setSound(GTSoundEntries.ARC);
        CHAOTIC_ALCHEMY = GTRecipeTypes.register("chaotic_alchemy", GTRecipeTypes.MULTIBLOCK)
                .setMaxIOSize(9, 0, 3, 1)
                .setEUIO(IO.IN).setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT)
                .setSlotOverlay(false, false, false, GuiTextures.FURNACE_OVERLAY_1)
                .setSlotOverlay(false, false, true, GuiTextures.FURNACE_OVERLAY_1)
                .setSlotOverlay(false, true, false, GuiTextures.FURNACE_OVERLAY_2)
                .setSlotOverlay(false, true, true, GuiTextures.FURNACE_OVERLAY_2)
                .setSlotOverlay(true, true, false, GuiTextures.FURNACE_OVERLAY_2)
                .setSlotOverlay(true, true, true, GuiTextures.FURNACE_OVERLAY_2)
                .addDataInfo((data) -> {
                    int temp = data.getInt("ebf_temp");
                    return LocalizationUtils.format("gtceu.recipe.temperature", FormattingUtil.formatNumbers(temp));
                }).addDataInfo((data) -> {
                    int temp = data.getInt("ebf_temp");
                    ICoilType requiredCoil = ICoilType.getMinRequiredType(temp);
                    return requiredCoil != null && requiredCoil.getMaterial() != null ? LocalizationUtils.format("gtceu.recipe.coil.tier", I18n.get(requiredCoil.getMaterial().getUnlocalizedName())) : "";
                }).setMaxTooltips(4).setUiBuilder((recipe, widgetGroup) -> {
                    int temp = recipe.data.getInt("ebf_temp");
                    List<List<ItemStack>> items = new ArrayList<>();
                    items.add(GTCEuAPI.HEATING_COILS.entrySet().stream()
                            .filter((coil) -> coil.getKey().getCoilTemperature() >= temp)
                            .map((coil) -> new ItemStack((ItemLike) ((Supplier<?>) coil.getValue()).get()))
                            .toList());
                    widgetGroup.addWidget(new SlotWidget(new CycleItemStackHandler(items), 0, widgetGroup.getSize().width - 25, widgetGroup.getSize().height - 40, false, false));
                }).setSound(GTSoundEntries.ARC);
        ANTIENTROPY_CONDENSATION = GTRecipeTypes.register("antientropy_condensation", GTRecipeTypes.MULTIBLOCK)
                .setEUIO(IO.IN).setMaxIOSize(2, 2, 2, 1)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT).setSound(GTSoundEntries.ARC);
        MOLECULAR_DECONSTRUCTION = GTRecipeTypes.register("molecular_deconstruction", GTRecipeTypes.MULTIBLOCK)
                .setEUIO(IO.IN).setMaxIOSize(1, 0, 0, 1)
                .setProgressBar(GuiTextures.PROGRESS_BAR_EXTRACT, FillDirection.LEFT_TO_RIGHT).setSound(GTSoundEntries.ARC);
        UNIVERSE_SANDBOX = GTRecipeTypes.register("universe_sandbox", GTRecipeTypes.MULTIBLOCK)
                .setEUIO(IO.IN).setMaxIOSize(1, 120, 1, 18)
                .setProgressBar(GuiTextures.PROGRESS_BAR_EXTRACT, FillDirection.LEFT_TO_RIGHT).setSound(GTSoundEntries.ARC);
        CHAOS_WEAVE = GTRecipeTypes.register("chaos_weave", GTRecipeTypes.MULTIBLOCK)
                .setEUIO(IO.IN).setMaxIOSize(1, 1, 0, 0)
                .setProgressBar(GuiTextures.PROGRESS_BAR_EXTRACT, FillDirection.LEFT_TO_RIGHT).setSound(GTSoundEntries.ARC);
        GENESIS_ENGINE = GTRecipeTypes.register("genesis_engine", GTRecipeTypes.MULTIBLOCK)
                .setEUIO(IO.OUT).setMaxIOSize(2, 1, 0, 0)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT).setSound(GTLAddSoundEntries.GENESIS_ENGINE);
        STAR_CORE_STRIPPER = GTRecipeTypes.register("star_core_stripper", GTRecipeTypes.MULTIBLOCK)
                .setEUIO(IO.IN).setMaxIOSize(3, 12, 1, 1)
                .setMaxTooltips(1)
                .setProgressBar(GuiTextures.PROGRESS_BAR_MACERATE, LEFT_TO_RIGHT).setSound(GTSoundEntries.MINER);
        FORGE_OF_THE_ANTICHRIST = GTRecipeTypes.register("forge_of_the_antichrist", GTRecipeTypes.DUMMY)
                .setXEIVisible(false)
                .setSound(GTLAddSoundEntries.FORGE_OF_THE_ANTICHRIST);
        QUANTUM_OSCILLATION = GTRecipeTypes.register("quantum_oscillation", GTRecipeTypes.DUMMY)
                .setXEIVisible(false)
                .setSound(GTLAddSoundEntries.QUANTUM_OSCILLATION);

        MULTIPLE_TYPE_RECIPES = Set.of(
                FORGE_OF_THE_ANTICHRIST,
                QUANTUM_OSCILLATION);
    }
}
