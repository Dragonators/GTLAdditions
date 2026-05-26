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
import com.gregtechceu.gtceu.common.data.GTCompassSections
import com.gregtechceu.gtceu.common.data.GTMachines
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.machine.GTLAddPartAbility
import com.gtladd.gtladditions.api.registry.GTLAddRegistration.Companion.REGISTRATE
import com.gtladd.gtladditions.api.registry.MachineBuilderExtensions.overlayHullRenderer
import com.gtladd.gtladditions.common.machine.hatch.HugeSteamHatchPartMachine
import com.gtladd.gtladditions.common.machine.hatch.InfinityDualHatchPartMachine
import com.gtladd.gtladditions.common.machine.hatch.MEBlockConversationHatch
import com.gtladd.gtladditions.common.machine.hatch.OreProcessorHatch
import com.gtladd.gtladditions.common.machine.hatch.SuperDualHatchPartMachine
import com.gtladd.gtladditions.common.machine.hatch.VientianeTranscriptionNode
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine
import com.gtladd.gtladditions.common.machine.multiblock.part.*
import com.gtladd.gtladditions.common.modify.GTLAddCreativeModeTabs
import com.gtladd.gtladditions.common.modify.MultiBlockModify
import com.gtladd.gtladditions.common.modify.MutableMultiBlockModify
import com.gtladd.gtladditions.config.ConfigHolder
import com.gtladd.gtladditions.utils.CommonUtils.createRainbowComponent
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.ComponentExtensions.translatable
import com.hepdd.gtmthings.common.block.machine.multiblock.part.HugeDualHatchPartMachine
import com.hepdd.gtmthings.common.registry.GTMTRegistration
import com.hepdd.gtmthings.data.CreativeModeTabs
import com.hepdd.gtmthings.data.CustomMachines
import com.hepdd.gtmthings.data.WirelessMachines
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.GTLCore
import java.util.function.BiConsumer

object GTLAddMachines {
    val WIRELL_ENERGY_HIGH_TIERS: IntArray = GTValues.tiersBetween(5, 14)

    @JvmField
    val HUGE_STEAM_HATCH: MachineDefinition
    val SUPER_INPUT_DUAL_HATCH: MachineDefinition
    val ME_BLOCK_CONVERSATION: MachineDefinition
    val INFINITY_INPUT_DUAL_HATCH: MachineDefinition
    val ORE_PROCESSOR_HATCH: MachineDefinition
    val VIENTIANE_TRANSCRIPTION_NODE: MachineDefinition
    val ME_SUPER_PATTERN_BUFFER: MachineDefinition
    val ME_SUPER_PATTERN_BUFFER_PROXY: MachineDefinition
    val SUPER_PARALLEL_HATCH: MachineDefinition
    val Wireless_Energy_Network_OUTPUT_Terminal: MachineDefinition
    val Wireless_Energy_Network_INPUT_Terminal: MachineDefinition
    val THREAD_MODIFIER_HATCH: MachineDefinition
    val HUGE_OUTPUT_DUAL_HATCH: Array<MachineDefinition?>
    val LASER_INPUT_HATCH_16777216A: Array<MachineDefinition?>
    val LASER_OUTPUT_HATCH_16777216A: Array<MachineDefinition?>
    val LASER_INPUT_HATCH_67108864A: Array<MachineDefinition?>
    val LASER_OUTPUT_HATCH_67108863A: Array<MachineDefinition?>
    val WIRELESS_LASER_INPUT_HATCH_16777216A: Array<MachineDefinition?>
    val WIRELESS_LASER_OUTPUT_HATCH_16777216A: Array<MachineDefinition?>
    val WIRELESS_LASER_INPUT_HATCH_67108864A: Array<MachineDefinition?>
    val WIRELESS_LASER_OUTPUT_HATCH_67108863A: Array<MachineDefinition?>

    fun init() {
        MultiBlockMachine.init()
        MultiBlockModify.init()
        MutableMultiBlockModify.init()
    }

    val GTLAdd_ADD: BiConsumer<ItemStack?, MutableList<Component?>?> =
        BiConsumer { stack: ItemStack?, components: MutableList<Component?>? ->
            components!!.add(createRainbowComponent("gui.gtladditions.add".translatable))
        }

    val GTLAdd_MODIFY: Component = createRainbowComponent("gui.gtladditions.modify".translatable)

    init {
        LASER_INPUT_HATCH_16777216A = GTMachines.registerLaserHatch(IO.IN, 16777216, PartAbility.INPUT_LASER)
        LASER_OUTPUT_HATCH_16777216A = GTMachines.registerLaserHatch(IO.OUT, 16777216, PartAbility.OUTPUT_LASER)
        LASER_INPUT_HATCH_67108864A = GTMachines.registerLaserHatch(IO.IN, 67108864, PartAbility.INPUT_LASER)
        LASER_OUTPUT_HATCH_67108863A = GTMachines.registerLaserHatch(IO.OUT, 67108863, PartAbility.OUTPUT_LASER)
        HUGE_OUTPUT_DUAL_HATCH = CustomMachines.registerTieredMachines(
            "huge_output_dual_hatch",
            { holder: IMachineBlockEntity?, tier: Int? ->
                HugeDualHatchPartMachine(holder!!, tier!!, IO.OUT)
            },
            { tier: Int?, builder: MachineBuilder<MachineDefinition?>? ->
                val vnf = GTValues.VNF
                builder!!.langValue(vnf[tier!!] + " Huge Output Dual Hatch").rotationState(RotationState.ALL)
                    .overlayTieredHullRenderer("huge_dual_hatch.import")
                    .abilities(*GTMachines.DUAL_OUTPUT_HATCH_ABILITIES).compassNode("huge_dual_hatch")
                    .tooltips("gtceu.machine.dual_hatch.export.tooltip".toComponent)
                    .tooltips("gtceu.universal.tooltip.item_storage_capacity".toComponent((1 + tier) * 2 - 1))
                    .tooltips("gtceu.universal.tooltip.fluid_storage_capacity_mult".toComponent(tier, FormattingUtil.formatNumbers(Int.Companion.MAX_VALUE)))
                builder.register()
            },
            *GTValues.tiersBetween(1, 13)
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
        HUGE_STEAM_HATCH = REGISTRATE.machine("huge_steam_input_hatch") { HugeSteamHatchPartMachine(it!!) }
            .rotationState(RotationState.ALL).abilities(PartAbility.STEAM)
            .tooltips(
                "gtceu.multiblock.steam_oc_hv".toComponent,
                "gtceu.multiblock.steam_duraiton".toComponent,
                "gtceu.universal.tooltip.fluid_storage_capacity".toComponent(Int.Companion.MAX_VALUE),
                "gtceu.machine.steam.steam_hatch.tooltip".toComponent
            )
            .tooltipBuilder(GTLAdd_ADD).compassSections(GTCompassSections.STEAM).compassNode("steam_hatch")
            .renderer {
                OverlaySteamMachineRenderer(ResourceLocation("gtceu", "block/machine/part/steam_hatch"))
            }.register()
        SUPER_INPUT_DUAL_HATCH = REGISTRATE.machine("super_input_dual_hatch") { SuperDualHatchPartMachine(it!!, 18) }
            .rotationState(RotationState.ALL)
            .abilities(*GTMachines.DUAL_INPUT_HATCH_ABILITIES)
            .langValue("Super Input Dual Hatch").overlayTieredHullRenderer("super_input_dual_hatch.import")
            .tooltips("gtceu.universal.tooltip.item_storage_capacity".toComponent(37))
            .tooltips("gtceu.universal.tooltip.fluid_storage_capacity_mult".toComponent(24, FormattingUtil.formatNumbers(Long.Companion.MAX_VALUE shr 12)))
            .tooltipBuilder(GTLAdd_ADD).tier(14).register()
        ORE_PROCESSOR_HATCH = REGISTRATE.machine("spectral_analysis_hatch") { OreProcessorHatch(it!!) }
            .rotationState(RotationState.ALL)
            .langValue("Ore Processor Hatch")
            .overlayTieredHullRenderer("op_hatch")
            .tooltips(
                "gtceu.universal.disabled".toComponent,
                "gtceu.machine.hold_g.tooltip.1".toComponent
            )
            .tooltipBuilder(GTLAdd_ADD)
            .tier(8)
            .register()
        VIENTIANE_TRANSCRIPTION_NODE = REGISTRATE.machine("vientiane_transcription_node") { VientianeTranscriptionNode(it!!) }
            .rotationState(RotationState.ALL)
            .overlayTieredHullRenderer("vientiane_transcription_node")
            .langValue("Vientiane Transcription Node")
            .tooltipBuilder(GTLAdd_ADD)
            .tier(14)
            .register()
        ME_BLOCK_CONVERSATION = REGISTRATE.machine("me_block_conservation") { MEBlockConversationHatch(it!!) }
            .rotationState(RotationState.ALL)
            .overlayTieredHullRenderer("me_block_conservation")
            .tooltips(
                "gtceu.universal.disabled".toComponent,
                "gtceu.machine.me.item_import.tooltip".toComponent,
                "gtceu.machine.block_conversation.tooltip.0".toComponent,
                "gtceu.machine.block_conversation.tooltip.1".toComponent,
                "gtceu.machine.hold_g.tooltip.1".toComponent
            )
            .langValue("Transmutation Bus Hatch")
            .tooltipBuilder(GTLAdd_ADD)
            .tier(11)
            .register()
        INFINITY_INPUT_DUAL_HATCH = REGISTRATE.machine("infinity_input_dual_hatch") { InfinityDualHatchPartMachine(it!!) }
            .rotationState(RotationState.ALL)
            .abilities(*GTMachines.DUAL_INPUT_HATCH_ABILITIES)
            .overlayHullRenderer(ResourceLocation(GTLAdditions.MOD_ID, "block/casings/infinity_input_dual_hatch_casing"), ResourceLocation(GTLAdditions.MOD_ID, "block/machine/part/ultimate_input_dual_hatch.import"))
            .langValue("Infinity Input Dual Hatch")
            .tooltips(
                "gtceu.universal.tooltip.item_storage_capacity".toComponent(
                    "gtladditions.multiblock.forge_of_the_antichrist.parallel".toComponent
                        .withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.BOLD)
                )
            )
            .tooltips(
                "gtceu.universal.tooltip.fluid_storage_capacity_mult".toComponent(
                    "gtladditions.multiblock.forge_of_the_antichrist.parallel".toComponent
                        .withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.BOLD),
                    FormattingUtil.formatNumbers(Long.Companion.MAX_VALUE)
                )
            )
            .tooltips("gtladditions.machine.infinity_input_dual_hatch.tooltip.0".toComponent)
            .tooltipBuilder(GTLAdd_ADD).tier(14).register()
        ME_SUPER_PATTERN_BUFFER = REGISTRATE.machine("me_super_pattern_buffer") { MESuperPatternBufferPartMachine(it!!, ConfigHolder.INSTANCE.superPatternBuffer.patternsPerRow, ConfigHolder.INSTANCE.superPatternBuffer.rowsPerPage, ConfigHolder.INSTANCE.superPatternBuffer.maxPages) }
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.IMPORT_ITEMS, PartAbility.IMPORT_FLUIDS)
            .overlayHullRenderer(ResourceLocation(GTLAdditions.MOD_ID, "block/casings/infinity_input_dual_hatch_casing"), GTCEu.id("block/machine/part/me_pattern_buffer"))
            .langValue("Me Super Pattern Buffer")
            .tooltips(
                "tooltip.gtlcore.bigger_stronger".toComponent.withStyle(ChatFormatting.GOLD),
                "block.gtceu.pattern_buffer.desc.0".toComponent,
                "gtceu.machine.me_pattern_buffer.desc.0".toComponent,
                "gtceu.machine.me_pattern_buffer.desc.1".toComponent,
                "gtceu.machine.me_pattern_buffer.desc.2".toComponent,
                "gtceu.machine.me_pattern_buffer.desc.3".toComponent,
                "gtceu.machine.me_pattern_buffer.desc.4".toComponent,
                "gtceu.machine.me_pattern_buffer.desc.5".toComponent,
                "gtladditions.machine.me_super_pattern_buffer.desc.0".toComponent,
                "block.gtceu.pattern_buffer.desc.2".toComponent,
                "gtceu.universal.enabled".toComponent
            )
            .tooltipBuilder(GTLAdd_ADD)
            .register()
        ME_SUPER_PATTERN_BUFFER_PROXY = REGISTRATE.machine("me_super_pattern_buffer_proxy") { MESuperPatternBufferProxyPartMachine(it!!) }
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.IMPORT_ITEMS, PartAbility.IMPORT_FLUIDS)
            .overlayHullRenderer(ResourceLocation(GTLAdditions.MOD_ID, "block/casings/infinity_input_dual_hatch_casing"), GTCEu.id("block/machine/part/me_pattern_buffer_proxy"))
            .langValue("Me Super Pattern Buffer Proxy")
            .tooltips(
                "block.gtceu.pattern_buffer_proxy.desc.0".toComponent,
                "block.gtceu.pattern_buffer_proxy.desc.1".toComponent,
                "block.gtceu.pattern_buffer_proxy.desc.2".toComponent,
                "gtceu.machine.me_pattern_buffer_proxy.desc.0".toComponent,
                "gtceu.universal.enabled".toComponent
            )
            .tooltipBuilder(GTLAdd_ADD)
            .register()
        SUPER_PARALLEL_HATCH = REGISTRATE.machine("super_parallel_hatch") { SuperParallelHatchPartMachine(it!!) }
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.PARALLEL_HATCH)
            .workableCasingRenderer(GTLCore.id("block/create_casing"), GTCEu.id("block/machines/parallel_hatch_mk10"))
            .tooltips(
                "gtceu.universal.enabled".toComponent,
                "gtceu.machine.super_parallel_hatch.tooltip".toComponent
            )
            .tooltipBuilder(GTLAdd_ADD)
            .compassNode("parallel_hatch")
            .register()
        Wireless_Energy_Network_OUTPUT_Terminal = REGISTRATE.machine("wireless_energy_network_output_terminal") { WirelessEnergyNetworkTerminalPartMachine(it!!, IO.OUT) }
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.OUTPUT_LASER, PartAbility.OUTPUT_ENERGY)
            .tier(GTValues.MAX)
            .overlayHullRenderer(ResourceLocation(GTLAdditions.MOD_ID, "block/casings/wireless_terminate_casing"), GTLAdditions.id("block/machine/part/wireless_energy_network_terminal"))
            .langValue("Wireless Energy Network Output Terminal")
            .tooltips(
                "gtceu.universal.disabled".toComponent,
                "gtladditions.machine.wireless_energy_network_terminal.tooltips.0".toComponent,
                "gtladditions.machine.wireless_energy_network_output_terminal.tooltips.0".toComponent,
                "gtladditions.machine.wireless_energy_network_output_terminal.tooltips.1".toComponent
            )
            .tooltipBuilder(GTLAdd_ADD)
            .register()
        Wireless_Energy_Network_INPUT_Terminal = REGISTRATE.machine("wireless_energy_network_input_terminal") { WirelessEnergyNetworkTerminalPartMachine(it!!, IO.IN) }
            .rotationState(RotationState.ALL)
            .abilities(PartAbility.INPUT_LASER, PartAbility.INPUT_ENERGY)
            .tier(GTValues.MAX)
            .overlayHullRenderer(ResourceLocation(GTLAdditions.MOD_ID, "block/casings/wireless_terminate_casing"), GTLAdditions.id("block/machine/part/wireless_energy_network_terminal"))
            .langValue("Wireless Energy Network Input Terminal")
            .tooltips(
                "gtceu.universal.disabled".toComponent,
                "gtladditions.machine.wireless_energy_network_terminal.tooltips.0".toComponent,
                "gtladditions.machine.wireless_energy_network_input_terminal.tooltips.0".toComponent,
                "gtladditions.machine.wireless_energy_network_input_terminal.tooltips.1".toComponent
            )
            .tooltipBuilder(GTLAdd_ADD)
            .register()
        THREAD_MODIFIER_HATCH = REGISTRATE.machine("thread_modifier_hatch") { ThreadPartMachine(it!!) }
            .rotationState(RotationState.ALL)
            .abilities(GTLAddPartAbility.THREAD_MODIFIER)
            .overlayHullRenderer(GTLCore.id("block/create_casing"), GTLAdditions.id("block/machine/part/thread_modifier_hatch"))
            .langValue("Thread Modifier Hatch")
            .tooltips(
                "gtceu.universal.disabled".toComponent,
                "gtladditions.machine.thread_modifier_hatch.tooltips.0".toComponent,
                "gtladditions.machine.thread_modifier_hatch.tooltips.1".toComponent,
                "gtladditions.machine.thread_modifier_hatch.tooltips.2".toComponent,
                "gtladditions.machine.thread_modifier_hatch.tooltips.3".toComponent
            )
            .tooltipBuilder(GTLAdd_ADD)
            .register()
    }
}