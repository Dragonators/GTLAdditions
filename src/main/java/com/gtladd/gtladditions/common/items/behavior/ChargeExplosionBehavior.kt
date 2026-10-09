package com.gtladd.gtladditions.common.items.behavior

import com.gtladd.gtladditions.config.ConfigHolder
import com.gtladd.gtladditions.utils.CommonUtils.createLanguageRainbowComponent
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.SynchronousSphereExplosion
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraftforge.registries.ForgeRegistries

object ChargeExplosionBehavior {
    private data class Charge(val activationItem: ResourceLocation, val radius: Int)

    private val charges = mapOf(
        ResourceLocation("kubejs", "naquadria_charge") to Charge(ResourceLocation("gtceu", "quantum_star"), 125),
        ResourceLocation("kubejs", "leptonic_charge") to Charge(ResourceLocation("gtceu", "gravi_star"), 250),
        ResourceLocation("kubejs", "quantum_chromodynamic_charge") to Charge(ResourceLocation("kubejs", "unstable_star"), 500)
    )

    fun radiusFor(blockId: ResourceLocation, itemId: ResourceLocation): Int =
        charges[blockId]?.takeIf { it.activationItem == itemId }?.radius ?: 0

    fun radiusForCharge(blockId: ResourceLocation): Int = charges[blockId]?.radius ?: 0

    fun appendHoverText(blockId: ResourceLocation, tooltipComponents: MutableList<Component>) {
        val charge = charges[blockId] ?: return
        val activationName = ForgeRegistries.ITEMS.getValue(charge.activationItem)?.description
            ?: Component.literal(charge.activationItem.toString())
        tooltipComponents.add("tooltip.gtladditions.item.explosion".toComponent(activationName, charge.radius))
        tooltipComponents.add(createLanguageRainbowComponent("gui.gtladditions.modify".toComponent))
    }

    fun onUse(level: Level, pos: BlockPos, player: Player, hand: InteractionHand): InteractionResult {
        if (!ConfigHolder.INSTANCE.enableChargeExplosions) return InteractionResult.PASS
        if (player.isSpectator || player.isSecondaryUseActive) return InteractionResult.PASS

        val stack = player.getItemInHand(hand)
        if (stack.isEmpty) return InteractionResult.PASS
        val radius = radiusFor(
            ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).block) ?: return InteractionResult.PASS,
            ForgeRegistries.ITEMS.getKey(stack.item) ?: return InteractionResult.PASS
        )
        if (radius == 0) return InteractionResult.PASS

        if (level.isClientSide) return InteractionResult.SUCCESS
        if (level !is ServerLevel || !level.removeBlock(pos, false)) return InteractionResult.PASS

        // Remove the source before a synchronous blast so another hand or repeated use cannot trigger it twice.
        if (!player.isCreative) stack.shrink(1)
        SynchronousSphereExplosion.explode(level, pos, radius)
        return InteractionResult.SUCCESS
    }
}