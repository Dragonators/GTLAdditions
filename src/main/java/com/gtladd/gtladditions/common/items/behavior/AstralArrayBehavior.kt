package com.gtladd.gtladditions.common.items.behavior

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity
import com.gregtechceu.gtceu.api.item.component.IInteractionItem
import com.gtladd.gtladditions.api.machine.IAstralArrayInteractionMachine
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.context.UseOnContext

object AstralArrayBehavior : IInteractionItem {

    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player as? ServerPlayer ?: return InteractionResult.PASS
        if (!player.isShiftKeyDown) return InteractionResult.PASS

        val machine = (context.level.getBlockEntity(context.clickedPos) as? MetaMachineBlockEntity)
            ?.metaMachine as? IAstralArrayInteractionMachine
            ?: return InteractionResult.PASS

        val itemCount = context.itemInHand.count
        val installedCount = machine.increaseAstralArrayCount(itemCount)

        when {
            installedCount > 0 -> {
                if (!player.isCreative) {
                    context.itemInHand.shrink(installedCount)
                }

                player.sendSystemMessage(
                    Component.translatable(
                        "gtladditions.message.astral_array.installed_multiple",
                        installedCount,
                        machine.astralArrayCount
                    )
                )
            }
            else -> {
                player.sendSystemMessage(
                    Component.translatable("gtladditions.message.astral_array.max_reached")
                )
            }
        }

        return InteractionResult.SUCCESS
    }
}
