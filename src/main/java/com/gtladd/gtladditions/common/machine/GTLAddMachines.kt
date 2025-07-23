package com.gtladd.gtladditions.common.machine

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.data.RotationState
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MachineDefinition
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import com.gregtechceu.gtceu.api.registry.registrate.MachineBuilder
import com.gregtechceu.gtceu.client.renderer.machine.OverlaySteamMachineRenderer
import com.gregtechceu.gtceu.client.util.TooltipHelper
import com.gregtechceu.gtceu.common.data.GTCompassSections
import com.gregtechceu.gtceu.common.data.GTMachines
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.registry.GTLAddRegistration.REGISTRATE
import com.gtladd.gtladditions.common.data.GTLAddCreativeModeTabs
import com.gtladd.gtladditions.common.data.MultiBlockModify
import com.gtladd.gtladditions.common.machine.hatch.HugeSteamHatchPartMachine
import com.gtladd.gtladditions.common.machine.hatch.SuperDualHatchPartMachine
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine
import com.hepdd.gtmthings.common.block.machine.multiblock.part.HugeDualHatchPartMachine
import com.hepdd.gtmthings.common.registry.GTMTRegistration
import com.hepdd.gtmthings.data.CreativeModeTabs
import com.hepdd.gtmthings.data.CustomMachines
import com.hepdd.gtmthings.data.WirelessMachines
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.utils.TextUtil
import java.util.function.BiConsumer

object GTLAddMachines {
    val WIRELL_ENERGY_HIGH_TIERS: IntArray = GTValues.tiersBetween(5, 14)
    @JvmField
    val HUGE_STEAM_HATCH: MachineDefinition
    val SUPER_INPUT_DUAL_HATCH: MachineDefinition
    @JvmField
    val HUGE_OUTPUT_DUAL_HATCH: Array<MachineDefinition?>
    val LASER_INPUT_HATCH_16777216A: Array<MachineDefinition?>
    val LASER_OUTPUT_HATCH_16777216A: Array<MachineDefinition?>
    val LASER_INPUT_HATCH_67108864A: Array<MachineDefinition?>
    val LASER_OUTPUT_HATCH_67108863A: Array<MachineDefinition?>
    val WIRELESS_LASER_INPUT_HATCH_16777216A: Array<MachineDefinition?>
    val WIRELESS_LASER_OUTPUT_HATCH_16777216A: Array<MachineDefinition?>
    val WIRELESS_LASER_INPUT_HATCH_67108864A: Array<MachineDefinition?>
    val WIRELESS_LASER_OUTPUT_HATCH_67108863A: Array<MachineDefinition?>

    @JvmStatic
    fun init() {
        MultiBlockMachine.init()
        MultiBlockModify.init()
    }

    val GTLAdd_TOOLTIP: BiConsumer<ItemStack?, MutableList<Component?>?> =
        BiConsumer { stack: ItemStack?, components: MutableList<Component?>? ->
            components!!.add(
                Component.literal(TextUtil.full_color("由GTLAdditions添加"))
                    .withStyle { style: Style? -> style!!.withColor(TooltipHelper.RAINBOW.current) }
            )
        }

    init {
        LASER_INPUT_HATCH_16777216A = GTMachines.registerLaserHatch(IO.IN, 16777216, PartAbility.INPUT_LASER)
        LASER_OUTPUT_HATCH_16777216A = GTMachines.registerLaserHatch(IO.OUT, 16777216, PartAbility.OUTPUT_LASER)
        LASER_INPUT_HATCH_67108864A = GTMachines.registerLaserHatch(IO.IN, 67108864, PartAbility.INPUT_LASER)
        LASER_OUTPUT_HATCH_67108863A = GTMachines.registerLaserHatch(IO.OUT, 67108863, PartAbility.OUTPUT_LASER)
        HUGE_OUTPUT_DUAL_HATCH = CustomMachines.registerTieredMachines("huge_output_dual_hatch",
            { holder: IMachineBlockEntity?, tier: Int? ->
                HugeDualHatchPartMachine(holder!!, tier!!, IO.OUT) },
            { tier: Int?, builder: MachineBuilder<MachineDefinition?>? ->
                val vnf = GTValues.VNF
                builder!!.langValue(vnf[tier!!] + " Huge Output Dual Hatch").rotationState(RotationState.ALL)
                    .overlayTieredHullRenderer("huge_dual_hatch.import")
                    .abilities(*GTMachines.DUAL_OUTPUT_HATCH_ABILITIES).compassNode("huge_dual_hatch")
                    .tooltips(Component.translatable("gtceu.machine.dual_hatch.export.tooltip"))
                    .tooltips(Component.translatable(
                        "gtceu.universal.tooltip.item_storage_capacity", (1 + tier) * 2 - 1))
                    .tooltips(Component.translatable(
                            "gtceu.universal.tooltip.fluid_storage_capacity_mult", tier,
                            FormattingUtil.formatNumbers(Int.Companion.MAX_VALUE)))
                builder.register()
            }, *GTValues.tiersBetween(1, 13)
        )
        GTMTRegistration.GTMTHINGS_REGISTRATE.creativeModeTab { CreativeModeTabs.WIRELESS_TAB }
        WIRELESS_LASER_INPUT_HATCH_16777216A = WirelessMachines.registerWirelessLaserHatch(
            IO.IN,
            16777216,
            PartAbility.INPUT_LASER,
            WIRELL_ENERGY_HIGH_TIERS
        )
        WIRELESS_LASER_OUTPUT_HATCH_16777216A = WirelessMachines.registerWirelessLaserHatch(
            IO.OUT,
            16777216,
            PartAbility.OUTPUT_LASER,
            WIRELL_ENERGY_HIGH_TIERS
        )
        WIRELESS_LASER_INPUT_HATCH_67108864A = WirelessMachines.registerWirelessLaserHatch(
            IO.IN,
            67108864,
            PartAbility.INPUT_LASER,
            WIRELL_ENERGY_HIGH_TIERS
        )
        WIRELESS_LASER_OUTPUT_HATCH_67108863A = WirelessMachines.registerWirelessLaserHatch(
            IO.OUT,
            67108863,
            PartAbility.OUTPUT_LASER,
            WIRELL_ENERGY_HIGH_TIERS
        )
        REGISTRATE.creativeModeTab { GTLAddCreativeModeTabs.GTLADD_MACHINE }
        HUGE_STEAM_HATCH = REGISTRATE.machine("huge_steam_input_hatch")
        { HugeSteamHatchPartMachine(it !!) }
            .rotationState(RotationState.ALL).abilities(PartAbility.STEAM)
            .tooltips(Component.translatable("gtceu.multiblock.steam_oc_hv"),
                Component.translatable("gtceu.multiblock.steam_duraiton"),
                Component.translatable("gtceu.universal.tooltip.fluid_storage_capacity", Int.Companion.MAX_VALUE),
                Component.translatable("gtceu.machine.steam.steam_hatch.tooltip")
            )
            .tooltipBuilder(GTLAdd_TOOLTIP).compassSections(GTCompassSections.STEAM).compassNode("steam_hatch")
            .renderer {
                OverlaySteamMachineRenderer(ResourceLocation("gtceu", "block/machine/part/steam_hatch"))
            }.register()
        SUPER_INPUT_DUAL_HATCH = REGISTRATE.machine("super_input_dual_hatch")
        { SuperDualHatchPartMachine(it!!) }
            .rotationState(RotationState.ALL)
            .abilities(*GTMachines.DUAL_INPUT_HATCH_ABILITIES)
            .langValue("Super Input Dual Hatch").overlayTieredHullRenderer("super_input_dual_hatch.import")
            .tooltips(Component.translatable("gtceu.universal.tooltip.item_storage_capacity", 37))
            .tooltips(Component.translatable("gtceu.universal.tooltip.fluid_storage_capacity_mult", 24,
                    FormattingUtil.formatNumbers(Long.Companion.MAX_VALUE shr 12)))
            .tooltipBuilder(GTLAdd_TOOLTIP).tier(14).register()
    }
}
