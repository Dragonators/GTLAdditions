package com.gtladd.gtladditions.common.items.behavior

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity
import com.gregtechceu.gtceu.api.item.component.IInteractionItem
import com.gtladd.gtladditions.api.machine.IAstralArrayInteractionMachine
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.LevelReader

object AstralArrayBehavior : IInteractionItem {

    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player ?: return InteractionResult.PASS
        if (!player.isShiftKeyDown) return InteractionResult.PASS

        val machine = (context.level.getBlockEntity(context.clickedPos) as? MetaMachineBlockEntity)
            ?.metaMachine as? IAstralArrayInteractionMachine
            ?: return InteractionResult.PASS

        if (context.level.isClientSide) return InteractionResult.SUCCESS

        val serverPlayer = player as ServerPlayer
        val itemCount = context.itemInHand.count
        val installedCount = machine.increaseAstralArrayCount(itemCount)

        when {
            installedCount > 0 -> {
                if (!serverPlayer.isCreative) {
                    context.itemInHand.shrink(installedCount)
                }

                serverPlayer.sendSystemMessage(
                    Component.translatable(
                        "gtladditions.message.astral_array.installed_multiple",
                        installedCount,
                        machine.astralArrayCount
                    )
                )
            }
            else -> {
                serverPlayer.sendSystemMessage(
                    Component.translatable("gtladditions.message.astral_array.max_reached")
                )
            }
        }

        return InteractionResult.SUCCESS
    }
}
