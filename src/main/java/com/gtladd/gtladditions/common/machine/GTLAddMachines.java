package com.gtladd.gtladditions.common.machine;

import org.gtlcore.gtlcore.utils.TextUtil;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.client.renderer.machine.OverlaySteamMachineRenderer;
import com.gregtechceu.gtceu.client.util.TooltipHelper;
import com.gregtechceu.gtceu.common.data.GTCompassSections;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import com.gtladd.gtladditions.common.data.GTLAddCreativeModeTabs;
import com.gtladd.gtladditions.common.data.MultiBlockModify;
import com.gtladd.gtladditions.common.machine.hatch.HugeSteamHatchPartMachine;
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine;
import com.hepdd.gtmthings.common.block.machine.multiblock.part.HugeDualHatchPartMachine;
import com.hepdd.gtmthings.common.registry.GTMTRegistration;
import com.hepdd.gtmthings.data.CreativeModeTabs;

import java.util.List;
import java.util.function.BiConsumer;

import static com.gtladd.gtladditions.api.registry.GTLAddRegistration.REGISTRATE;
import static com.hepdd.gtmthings.data.CustomMachines.registerTieredMachines;
import static com.hepdd.gtmthings.data.WirelessMachines.registerWirelessLaserHatch;

public class GTLAddMachines {

    public static final int[] WIRELL_ENERGY_HIGH_TIERS = GTValues.tiersBetween(5, 14);
    public static final MachineDefinition HUGE_STEAM_HATCH;
    public static final MachineDefinition[] HUGE_OUTPUT_DUAL_HATCH;
    public static final MachineDefinition[] LASER_INPUT_HATCH_16777216A;
    public static final MachineDefinition[] LASER_OUTPUT_HATCH_16777216A;
    public static final MachineDefinition[] LASER_INPUT_HATCH_67108864A;
    public static final MachineDefinition[] LASER_OUTPUT_HATCH_67108863A;
    public static final MachineDefinition[] LASER_INPUT_HATCH_268435455A;
    public static final MachineDefinition[] WIRELESS_LASER_INPUT_HATCH_16777216A;
    public static final MachineDefinition[] WIRELESS_LASER_OUTPUT_HATCH_16777216A;
    public static final MachineDefinition[] WIRELESS_LASER_INPUT_HATCH_67108864A;
    public static final MachineDefinition[] WIRELESS_LASER_OUTPUT_HATCH_67108863A;
    public static final MachineDefinition[] WIRELESS_LASER_INPUT_HATCH_268435455A;

    public static void init() {
        MultiBlockMachine.init();
        MultiBlockModify.init();
    }

    public static final BiConsumer<ItemStack, List<Component>> GTLAdd_TOOLTIP = (stack, components) -> components.add(Component.literal(TextUtil.full_color("由GTLAdditions添加"))
            .withStyle((style) -> style.withColor(TooltipHelper.RAINBOW.getCurrent())));

    static {
        LASER_INPUT_HATCH_16777216A = GTMachines.registerLaserHatch(IO.IN, 16777216, PartAbility.INPUT_LASER);
        LASER_OUTPUT_HATCH_16777216A = GTMachines.registerLaserHatch(IO.OUT, 16777216, PartAbility.OUTPUT_LASER);
        LASER_INPUT_HATCH_67108864A = GTMachines.registerLaserHatch(IO.IN, 67108864, PartAbility.INPUT_LASER);
        LASER_OUTPUT_HATCH_67108863A = GTMachines.registerLaserHatch(IO.OUT, 67108863, PartAbility.OUTPUT_LASER);
        LASER_INPUT_HATCH_268435455A = GTMachines.registerLaserHatch(IO.IN, 268435455, PartAbility.INPUT_LASER);
        HUGE_OUTPUT_DUAL_HATCH = registerTieredMachines("huge_output_dual_hatch", (holder, tier) -> new HugeDualHatchPartMachine(holder, tier, IO.OUT),
                (tier, builder) -> {
                    String[] vnf = GTValues.VNF;
                    builder.langValue(vnf[tier] + " Huge Output Dual Hatch").rotationState(RotationState.ALL).overlayTieredHullRenderer("huge_dual_hatch.import")
                            .abilities(GTMachines.DUAL_OUTPUT_HATCH_ABILITIES).compassNode("huge_dual_hatch")
                            .tooltips(Component.translatable("gtceu.machine.dual_hatch.export.tooltip")).tooltips(Component.translatable("gtceu.universal.tooltip.item_storage_capacity", (1 + tier) * 2 - 1))
                            .tooltips(Component.translatable("gtceu.universal.tooltip.fluid_storage_capacity_mult", tier, FormattingUtil.formatNumbers(Integer.MAX_VALUE)));
                    return builder.register();
                }, GTValues.tiersBetween(1, 13));
        GTMTRegistration.GTMTHINGS_REGISTRATE.creativeModeTab(() -> CreativeModeTabs.WIRELESS_TAB);
        WIRELESS_LASER_INPUT_HATCH_16777216A = registerWirelessLaserHatch(IO.IN, 16777216, PartAbility.INPUT_LASER, WIRELL_ENERGY_HIGH_TIERS);
        WIRELESS_LASER_OUTPUT_HATCH_16777216A = registerWirelessLaserHatch(IO.OUT, 16777216, PartAbility.OUTPUT_LASER, WIRELL_ENERGY_HIGH_TIERS);
        WIRELESS_LASER_INPUT_HATCH_67108864A = registerWirelessLaserHatch(IO.IN, 67108864, PartAbility.INPUT_LASER, WIRELL_ENERGY_HIGH_TIERS);
        WIRELESS_LASER_OUTPUT_HATCH_67108863A = registerWirelessLaserHatch(IO.OUT, 67108863, PartAbility.OUTPUT_LASER, WIRELL_ENERGY_HIGH_TIERS);
        WIRELESS_LASER_INPUT_HATCH_268435455A = registerWirelessLaserHatch(IO.IN, 268435455, PartAbility.INPUT_LASER, WIRELL_ENERGY_HIGH_TIERS);
        REGISTRATE.creativeModeTab(() -> GTLAddCreativeModeTabs.GTLADD_MACHINE);
        HUGE_STEAM_HATCH = REGISTRATE.machine("huge_steam_input_hatch", holder -> new HugeSteamHatchPartMachine(holder, IO.IN))
                .rotationState(RotationState.ALL).abilities(PartAbility.STEAM)
                .tooltips(Component.translatable("gtceu.multiblock.steam_oc_hv"), Component.translatable("gtceu.multiblock.steam_duraiton"),
                        Component.translatable("gtceu.universal.tooltip.fluid_storage_capacity", Integer.MAX_VALUE), Component.translatable("gtceu.machine.steam.steam_hatch.tooltip"))
                .tooltipBuilder(GTLAdd_TOOLTIP).compassSections(GTCompassSections.STEAM).compassNode("steam_hatch")
                .renderer(() -> new OverlaySteamMachineRenderer(new ResourceLocation("gtceu", "block/machine/part/steam_hatch"))).register();
    }
}
