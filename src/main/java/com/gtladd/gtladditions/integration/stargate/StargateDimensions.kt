package com.gtladd.gtladditions.integration.stargate

import committee.nova.mods.avaritia.init.registry.ModItems
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.povstalec.sgjourney.StargateJourney
import net.povstalec.sgjourney.common.init.BlockInit

object StargateDimensions {
    const val VALID_TAG = "gtladditions:stargate_transfer"

    private val FORBIDDEN_DIMENSIONS by lazy {
        mapOf(
            ResourceLocation(StargateJourney.MODID, "lantea") to ItemStack(BlockInit.UNIVERSE_STARGATE.get().asItem()),
            ResourceLocation(StargateJourney.MODID, "abydos") to ItemStack(ModItems.neutron_ring.get()),
            ResourceLocation(StargateJourney.MODID, "chulak") to ItemStack(ModItems.infinity_umbrella.get()),
            ResourceLocation(StargateJourney.MODID, "cavum_tenebrae") to ItemStack(ModItems.infinity_ring.get())
        )
    }

    @JvmStatic
    fun selectDisplayItem(dimensionLocation: ResourceLocation): ItemStack? = FORBIDDEN_DIMENSIONS[dimensionLocation]

    @JvmStatic
    fun isTargetDimension(dimensionLocation: ResourceLocation): Boolean = FORBIDDEN_DIMENSIONS.containsKey(dimensionLocation)
}