package com.gtladd.gtladditions.common.machine

import com.gregtechceu.gtceu.GTCEu
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
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.registry.GTLAddRegistration.REGISTRATE
import com.gtladd.gtladditions.api.registry.MachineBuilderExtensions.overlayHullRenderer
import com.gtladd.gtladditions.common.data.GTLAddCreativeModeTabs
import com.gtladd.gtladditions.common.data.MultiBlockModify
import com.gtladd.gtladditions.common.machine.hatch.HugeSteamHatchPartMachine
import com.gtladd.gtladditions.common.machine.hatch.SuperDualHatchPartMachine
import com.gtladd.gtladditions.common.machine.hatch.SuperParallelHatchPartMachine
import com.gtladd.gtladditions.common.machine.hatch.UltimateDualHatchPartMachine
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine
import com.gtladd.gtladditions.common.machine.muiltblock.part.MESuperPatternBufferPartMachine
import com.gtladd.gtladditions.common.machine.muiltblock.part.MESuperPatternBufferProxyPartMachine
import com.gtladd.gtladditions.config.ConfigHolder
import com.hepdd.gtmthings.common.block.machine.multiblock.part.HugeDualHatchPartMachine
import com.hepdd.gtmthings.common.registry.GTMTRegistration
import com.hepdd.gtmthings.data.CreativeModeTabs
import com.hepdd.gtmthings.data.CustomMachines
import com.hepdd.gtmthings.data.WirelessMachines
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.GTLCore
import org.gtlcore.gtlcore.utils.TextUtil
import java.util.function.BiConsumer

object GTLAddMachines {
    val WIRELL_ENERGY_HIGH_TIERS: IntArray = GTValues.tiersBetween(5, 14)
    @JvmField
    val HUGE_STEAM_HATCH: MachineDefinition
    val SUPER_INPUT_DUAL_HATCH: MachineDefinition
    val Ultimate_INPUT_DUAL_HATCH: MachineDefinition
    @JvmField
    val ME_SUPER_PATTERN_BUFFER: MachineDefinition
    val ME_SUPER_PATTERN_BUFFER_PROXY: MachineDefinition
    val SUPER_PARALLEL_HATCH: MachineDefinition
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

    val GTLAdd_ADD: BiConsumer<ItemStack?, MutableList<Component?>?> =
        BiConsumer { stack: ItemStack?, components: MutableList<Component?>? ->
            components!!.add(createRainbowComponent(Component.translatable("gui.gtladditions.add").string))
        }

    val GTLAdd_MODIFY: Component = createRainbowComponent(Component.translatable("gui.gtladditions.modify").string)

    fun createRainbowComponent(string: String): Component {
        return Component.literal(TextUtil.full_color(string))
            .withStyle { style: Style? -> style!!.withColor(TooltipHelper.RAINBOW.current) }
    }

    fun createObfuscatedRainbowComponent(text: String): Component {
        val component = Component.empty()

        text.forEachIndexed { index, char ->
            component.append(
                Component.literal(char.toString())
                    .withStyle(ChatFormatting.OBFUSCATED)
                    .withStyle { style: Style? -> style!!.withColor(TooltipHelper.RAINBOW.current)}
            )
        }

        return component;
    }

    fun createObfuscatedDeleteComponent(text: String): Component {
        val rainbowColors = arrayOf(
            ChatFormatting.RED,
            ChatFormatting.GOLD,
            ChatFormatting.YELLOW,
            ChatFormatting.GREEN,
            ChatFormatting.AQUA,
            ChatFormatting.BLUE,
            ChatFormatting.LIGHT_PURPLE
        )
        val component = Component.empty()

        text.forEachIndexed { index, char ->
            component.append(
                Component.literal(char.toString())
                    .withStyle(ChatFormatting.OBFUSCATED)
                    .withStyle(ChatFormatting.STRIKETHROUGH)
                    .withStyle(rainbowColors[index % 7])
            )
        }

        return component;
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
            .tooltipBuilder(GTLAdd_ADD).compassSections(GTCompassSections.STEAM).compassNode("steam_hatch")
            .renderer {
                OverlaySteamMachineRenderer(ResourceLocation("gtceu", "block/machine/part/steam_hatch"))
            }.register()
        SUPER_INPUT_DUAL_HATCH = REGISTRATE.machine("super_input_dual_hatch")
        { SuperDualHatchPartMachine(it!!, 18) }
            .rotationState(RotationState.ALL)
            .abilities(*GTMachines.DUAL_INPUT_HATCH_ABILITIES)
            .langValue("Super Input Dual Hatch").overlayTieredHullRenderer("super_input_dual_hatch.import")
            .tooltips(Component.translatable("gtceu.universal.tooltip.item_storage_capacity", 37))
            .tooltips(Component.translatable("gtceu.universal.tooltip.fluid_storage_capacity_mult", 24,
                    FormattingUtil.formatNumbers(Long.Companion.MAX_VALUE shr 12)))
            .tooltipBuilder(GTLAdd_ADD).tier(14).register()
        Ultimate_INPUT_DUAL_HATCH = REGISTRATE.machine("ultimate_input_dual_hatch")
        { UltimateDualHatchPartMachine(it!!, 64) }
            .rotationState(RotationState.ALL)
            .abilities(*GTMachines.DUAL_INPUT_HATCH_ABILITIES)
            .overlayHullRenderer(ResourceLocation(GTLAdditions.MOD_ID, "block/casings/ultimate_dual_hatch_casing"), ResourceLocation(GTLAdditions.MOD_ID, "block/machine/part/ultimate_input_dual_hatch.import"))
            .langValue("Ultimate Input Dual Hatch")
            .tooltips(Component.translatable("gtceu.universal.tooltip.item_storage_capacity", 129))
            .tooltips(Component.translatable("gtceu.universal.tooltip.fluid_storage_capacity_mult", 64,
                FormattingUtil.formatNumbers(Long.Companion.MAX_VALUE)))
            .tooltipBuilder(GTLAdd_ADD).register()
        ME_SUPER_PATTERN_BUFFER = REGISTRATE.machine("me_super_pattern_buffer")
        { MESuperPatternBufferPartMachine(it!!, ConfigHolder.INSTANCE.superPatternBuffer.patternsPerRow, ConfigHolder.INSTANCE.superPatternBuffer.rowsPerPage, ConfigHolder.INSTANCE.superPatternBuffer.maxPages) }
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.IMPORT_ITEMS, PartAbility.IMPORT_FLUIDS)
            .overlayHullRenderer(ResourceLocation(GTLAdditions.MOD_ID, "block/casings/ultimate_dual_hatch_casing"), GTCEu.id("block/machine/part/me_pattern_buffer"))
            .langValue("Me Super Pattern Buffer")
            .tooltips(Component.translatable("tooltip.gtlcore.bigger_stronger").withStyle(ChatFormatting.GOLD),
                Component.translatable("block.gtceu.pattern_buffer.desc.0"),
                Component.translatable("gtceu.machine.me_pattern_buffer.desc.0"),
                Component.translatable("gtceu.machine.me_pattern_buffer.desc.1"),
                Component.translatable("gtceu.machine.me_pattern_buffer.desc.2"),
                Component.translatable("gtceu.machine.me_pattern_buffer.desc.3"),
                Component.translatable("gtceu.machine.me_pattern_buffer.desc.4"),
                Component.translatable("gtceu.machine.me_pattern_buffer.desc.5"),
                Component.translatable("gtladditions.machine.me_super_pattern_buffer.desc.0"),
                Component.translatable("block.gtceu.pattern_buffer.desc.2"),
                Component.translatable("gtceu.universal.enabled"))
            .tooltipBuilder(GTLAdd_ADD)
            .register()
        ME_SUPER_PATTERN_BUFFER_PROXY = REGISTRATE.machine("me_super_pattern_buffer_proxy")
        { MESuperPatternBufferProxyPartMachine(it!!) }
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.IMPORT_ITEMS, PartAbility.IMPORT_FLUIDS)
            .overlayHullRenderer(ResourceLocation(GTLAdditions.MOD_ID, "block/casings/ultimate_dual_hatch_casing"), GTCEu.id("block/machine/part/me_pattern_buffer_proxy"))
            .langValue("Me Super Pattern Buffer Proxy")
            .tooltips(Component.translatable("block.gtceu.pattern_buffer_proxy.desc.0"),
                Component.translatable("block.gtceu.pattern_buffer_proxy.desc.1"),
                Component.translatable("block.gtceu.pattern_buffer_proxy.desc.2"),
                Component.translatable("gtceu.machine.me_pattern_buffer_proxy.desc.0"),
                Component.translatable("gtceu.universal.enabled"))
            .tooltipBuilder(GTLAdd_ADD)
            .register()
        SUPER_PARALLEL_HATCH = REGISTRATE.machine("super_parallel_hatch")
        { SuperParallelHatchPartMachine(it!!) }
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.PARALLEL_HATCH)
            .workableCasingRenderer(GTLCore.id("block/create_casing"), GTCEu.id("block/machines/parallel_hatch_mk10"))
            .tooltips(Component.translatable("gtceu.machine.super_parallel_hatch.tooltip"))
            .tooltipBuilder(GTLAdd_ADD)
            .compassNode("parallel_hatch")
            .register()

    }
}
