package com.gtladd.gtladditions.events

import com.gregtechceu.gtceu.common.data.GTMachines
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.utils.CommonUtils.createLanguageRainbowComponent
import committee.nova.mods.avaritia.init.registry.ModItems
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.povstalec.sgjourney.common.init.BlockInit

@Suppress("unused")
@Mod.EventBusSubscriber(modid = GTLAdditions.Companion.MOD_ID, value = [Dist.CLIENT])
object TooltipHandler {
    @SubscribeEvent
    @JvmStatic
    fun onItemTooltip(event: ItemTooltipEvent) {
        val stack = event.itemStack

        when {
            stack.`is`(ModItems.neutron_ring.get()) -> {
                event.toolTip[0] = Component.translatable("item.avaritia.neutron_ring_re")
            }

            stack.`is`(ModItems.infinity_ring.get()) -> {
                event.toolTip[0] = Component.translatable("item.avaritia.infinity_ring_re")
            }

            stack.`is`(ModItems.infinity_umbrella.get()) -> {
                event.toolTip[0] = Component.translatable("item.avaritia.infinity_umbrella_re")
            }

            stack.`is`(BlockInit.CLASSIC_STARGATE_RING_BLOCK.get().asItem()) -> {
                event.toolTip.add(
                    event.toolTip.size - 1,
                    Component.translatable("tooltip.gtladditions.classic_stargate_ring_block")
                        .withStyle(ChatFormatting.BLUE)
                )
            }

            stack.`is`(BlockInit.CLASSIC_STARGATE_CHEVRON_BLOCK.get().asItem()) -> {
                event.toolTip.add(
                    event.toolTip.size - 1,
                    Component.translatable("tooltip.gtladditions.classic_stargate_chevron_block")
                        .withStyle(ChatFormatting.GOLD)
                )
            }

            stack.`is`(GTMachines.CREATIVE_ITEM.item) -> {
                event.toolTip.addAll(
                    event.toolTip.size - 1,
                    listOf(
                        createLanguageRainbowComponent(
                            Component.translatable("tooltip.gtladditions.create_chest.0")
                        ),
                        createLanguageRainbowComponent(
                            Component.translatable("tooltip.gtladditions.create_chest.1")
                        ),
                        createLanguageRainbowComponent(
                            Component.translatable("tooltip.gtladditions.create_chest.2")
                        )
                    )
                )
            }
        }
    }
}