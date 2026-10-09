package com.gtladd.gtladditions.events

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.utils.SynchronousSphereExplosion
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.Event
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.ForgeRegistries

@Suppress("unused")
@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object ChargeExplosionHandler {
    private val NAQUADRIA = ResourceLocation("kubejs", "naquadria_charge")
    private val LEPTONIC = ResourceLocation("kubejs", "leptonic_charge")
    private val QUANTUM_CHROMODYNAMIC = ResourceLocation("kubejs", "quantum_chromodynamic_charge")

    private val UNSTABLE_STAR = ResourceLocation("kubejs", "unstable_star")
    private val QUANTUM_STAR = ResourceLocation("gtceu", "quantum_star")
    private val GRAVI_STAR = ResourceLocation("gtceu", "gravi_star")

    fun radiusFor(blockId: ResourceLocation, itemId: ResourceLocation): Int = when {
        blockId == NAQUADRIA && itemId == QUANTUM_STAR -> 125
        blockId == LEPTONIC && itemId == GRAVI_STAR -> 250
        blockId == QUANTUM_CHROMODYNAMIC && itemId == UNSTABLE_STAR -> 500
        else -> 0
    }

    fun radiusForCharge(blockId: ResourceLocation): Int = when (blockId) {
        NAQUADRIA -> 125
        LEPTONIC -> 250
        QUANTUM_CHROMODYNAMIC -> 500
        else -> 0
    }

    fun activationItemForCharge(blockId: ResourceLocation): ResourceLocation? = when (blockId) {
        NAQUADRIA -> QUANTUM_STAR
        LEPTONIC -> GRAVI_STAR
        QUANTUM_CHROMODYNAMIC -> UNSTABLE_STAR
        else -> null
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    @JvmStatic
    fun onRightClickBlock(event: PlayerInteractEvent.RightClickBlock) {
        if (event.isCanceled ||
            event.entity.isSpectator ||
            event.useBlock == Event.Result.DENY ||
            event.useItem == Event.Result.DENY
        ) {
            return
        }

        val level = event.level
        val stack = event.itemStack
        if (stack.isEmpty) return

        val radius = radiusFor(
            ForgeRegistries.BLOCKS.getKey(level.getBlockState(event.pos).block) ?: return,
            ForgeRegistries.ITEMS.getKey(stack.item) ?: return
        )
        if (radius == 0) return

        if (level is ServerLevel) {
            // Remove the source before a synchronous blast so another hand or repeated event cannot trigger it twice.
            if (!level.removeBlock(event.pos, false)) return
            if (!event.entity.isCreative) stack.shrink(1)

            event.isCanceled = true
            event.cancellationResult = InteractionResult.SUCCESS

            SynchronousSphereExplosion.explode(level, event.pos, radius)
        } else if (level.isClientSide) {
            event.isCanceled = true
            event.cancellationResult = InteractionResult.SUCCESS
        }
    }
}