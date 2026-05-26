package com.gtladd.gtladditions.common.items.behavior

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity
import com.gregtechceu.gtceu.api.item.component.IInteractionItem
import com.gtladd.gtladditions.api.machine.IAstralArrayInteractionMachine
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.context.UseOnContext

object AstralArrayBehavior : IInteractionItem {

    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player ?: return InteractionResult.PASS
        if (!player.isShiftKeyDown) return InteractionResult.PASS

        val machine = (context.level.getBlockEntity(context.clickedPos) as? MetaMachineBlockEntity)
            ?.metaMachine as? IAstralArrayInteractionMachine
            ?: return InteractionResult.PASS

        if (context.level.isClientSide) return InteractionResult.SUCCESS

        val serverPlayer = player as ServerPlayer
        val equivalent = IAstralArrayInteractionMachine.getAstralArrayEquivalent(context.itemInHand)
        if (equivalent <= 0) return InteractionResult.PASS
        val itemCount = context.itemInHand.count
        val installedCount = machine.increaseAstralArrayCount(itemCount * equivalent)

        when {
            installedCount > 0 -> {
                if (!serverPlayer.isCreative) {
                    context.itemInHand.shrink((installedCount + equivalent - 1) / equivalent)
                }

                serverPlayer.sendSystemMessage(
                    "gtladditions.message.astral_array.installed_multiple".toComponent(installedCount, machine.astralArrayCount)
                )
            }
            else -> {
                serverPlayer.sendSystemMessage(
                    "gtladditions.message.astral_array.max_reached".toComponent
                )
            }
        }

        return InteractionResult.SUCCESS
    }
}