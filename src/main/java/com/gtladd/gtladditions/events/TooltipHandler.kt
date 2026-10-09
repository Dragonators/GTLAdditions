package com.gtladd.gtladditions.events

import com.gregtechceu.gtceu.common.data.GTMachines
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.utils.CommonUtils.createLanguageRainbowComponent
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import committee.nova.mods.avaritia.init.registry.ModItems
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.ForgeRegistries
import net.povstalec.sgjourney.common.init.BlockInit

@Suppress("unused")
@Mod.EventBusSubscriber(modid = GTLAdditions.Companion.MOD_ID, value = [Dist.CLIENT])
object TooltipHandler {
    @SubscribeEvent
    @JvmStatic
    fun onItemTooltip(event: ItemTooltipEvent) {
        val stack = event.itemStack

        ForgeRegistries.ITEMS.getKey(stack.item)?.let { id ->
            val radius = ChargeExplosionHandler.radiusForCharge(id)
            if (radius > 0) {
                event.toolTip.add(createLanguageRainbowComponent("gui.gtladditions.modify".toComponent))

                val activationId = ChargeExplosionHandler.activationItemForCharge(id) ?: return@let
                val activationName = ForgeRegistries.ITEMS.getValue(activationId)?.description
                    ?: Component.literal(activationId.toString())

                event.toolTip.add("tooltip.gtladditions.item.explosion".toComponent(activationName, radius))
            }
        }

        when {
            stack.`is`(ModItems.neutron_ring.get()) -> {
                event.toolTip[0] = "item.avaritia.neutron_ring_re".toComponent
            }

            stack.`is`(ModItems.infinity_ring.get()) -> {
                event.toolTip[0] = "item.avaritia.infinity_ring_re".toComponent
            }

            stack.`is`(ModItems.infinity_umbrella.get()) -> {
                event.toolTip[0] = "item.avaritia.infinity_umbrella_re".toComponent
            }

            stack.`is`(BlockInit.CLASSIC_STARGATE_RING_BLOCK.get().asItem()) -> {
                event.toolTip.add(
                    event.toolTip.size - 1,
                    "tooltip.gtladditions.classic_stargate_ring_block".toComponent
                        .withStyle(ChatFormatting.BLUE)
                )
            }

            stack.`is`(BlockInit.CLASSIC_STARGATE_CHEVRON_BLOCK.get().asItem()) -> {
                event.toolTip.add(
                    event.toolTip.size - 1,
                    "tooltip.gtladditions.classic_stargate_chevron_block".toComponent
                        .withStyle(ChatFormatting.GOLD)
                )
            }

            stack.`is`(GTMachines.CREATIVE_ITEM.item) -> {
                event.toolTip.addAll(
                    event.toolTip.size - 1,
                    listOf(
                        createLanguageRainbowComponent(
                            "tooltip.gtladditions.create_chest.0".toComponent
                        ),
                        createLanguageRainbowComponent(
                            "tooltip.gtladditions.create_chest.1".toComponent
                        ),
                        createLanguageRainbowComponent(
                            "tooltip.gtladditions.create_chest.2".toComponent
                        )
                    )
                )
            }
        }
    }
}