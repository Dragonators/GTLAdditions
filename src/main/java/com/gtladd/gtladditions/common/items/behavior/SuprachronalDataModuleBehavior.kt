package com.gtladd.gtladditions.common.items.behavior

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity
import com.gregtechceu.gtceu.api.item.component.IAddInformation
import com.gregtechceu.gtceu.api.item.component.IInteractionItem
import com.gtladd.gtladditions.api.machine.DataModuleBindingResult
import com.gtladd.gtladditions.api.machine.IWirelessBindableSource
import com.gtladd.gtladditions.api.machine.IWirelessBindableTarget
import com.gtladd.gtladditions.common.data.DataModuleBindingRecord
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level

object SuprachronalDataModuleBehavior : IInteractionItem, IAddInformation {

    override fun onItemUseFirst(itemStack: ItemStack, context: UseOnContext): InteractionResult = handleMachineUse(context, itemStack)

    override fun useOn(context: UseOnContext): InteractionResult = handleMachineUse(context, context.itemInHand)

    private fun handleMachineUse(context: UseOnContext, stack: ItemStack): InteractionResult {
        val player = context.player ?: return InteractionResult.PASS
        val level = context.level
        when (val machine = (level.getBlockEntity(context.clickedPos) as? MetaMachineBlockEntity)?.metaMachine) {
            is IWirelessBindableSource<*> -> {
                if (level.isClientSide) return InteractionResult.SUCCESS

                val serverPlayer = player as? ServerPlayer ?: return InteractionResult.PASS
                when (val result = machine.tryRecording()) {
                    DataModuleBindingResult.Success -> {
                        DataModuleBindingRecord.write(stack, machine, level)
                        serverPlayer.sendSystemMessage(machine.getRecordedMessage(context.clickedPos))
                        return InteractionResult.SUCCESS
                    }
                    is DataModuleBindingResult.BoundSuccess -> {
                        DataModuleBindingRecord.write(stack, machine, level)
                        result.message.let(serverPlayer::sendSystemMessage)
                        return InteractionResult.SUCCESS
                    }
                    is DataModuleBindingResult.Failure -> {
                        serverPlayer.sendSystemMessage(result.message)
                        return InteractionResult.FAIL
                    }
                }
            }
            is IWirelessBindableTarget -> {
                if (level.isClientSide) return InteractionResult.SUCCESS

                val serverPlayer = player as? ServerPlayer ?: return InteractionResult.PASS
                return when (val result = machine.tryBindSource(level, stack)) {
                    DataModuleBindingResult.Success -> {
                        consumeAppliedRecord(stack, serverPlayer)
                        result.message?.let(serverPlayer::sendSystemMessage)
                        InteractionResult.SUCCESS
                    }
                    is DataModuleBindingResult.BoundSuccess -> {
                        consumeAppliedRecord(stack, serverPlayer)
                        result.message.let(serverPlayer::sendSystemMessage)
                        InteractionResult.SUCCESS
                    }
                    is DataModuleBindingResult.Failure -> {
                        serverPlayer.sendSystemMessage(result.message)
                        InteractionResult.FAIL
                    }
                }
            }
            else -> {
                if (level.isClientSide && DataModuleBindingRecord.hasRecord(stack)) {
                    return InteractionResult.SUCCESS
                }
                if (DataModuleBindingRecord.hasRecord(stack)) {
                    (player as? ServerPlayer)?.sendSystemMessage("gtladditions.message.suprachronal_data_module.target_mismatch".toComponent)
                    return InteractionResult.FAIL
                }
            }
        }

        return InteractionResult.PASS
    }

    private fun consumeAppliedRecord(stack: ItemStack, player: ServerPlayer) {
        if (player.isCreative) {
            DataModuleBindingRecord.clear(stack)
        } else {
            stack.shrink(1)
        }
    }

    override fun use(item: Item, level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (!player.isShiftKeyDown) return InteractionResultHolder.pass(stack)
        if (!level.isClientSide && DataModuleBindingRecord.hasRecord(stack)) {
            DataModuleBindingRecord.clear(stack)
            (player as? ServerPlayer)?.sendSystemMessage("gtladditions.message.suprachronal_data_module.cleared".toComponent)
            return InteractionResultHolder.success(stack)
        }
        return InteractionResultHolder.pass(stack)
    }

    override fun appendHoverText(stack: ItemStack, level: Level?, tooltipComponents: MutableList<Component>, isAdvanced: TooltipFlag) {
        tooltipComponents.add("gtladditions.item.suprachronal_data_module.tooltips.0".toComponent)
        tooltipComponents.add("gtladditions.item.suprachronal_data_module.tooltips.1".toComponent)

        val record = DataModuleBindingRecord.read(stack) ?: return
        tooltipComponents.add(
            "gtladditions.tooltip.suprachronal_data_module.bound_source".toComponent(
                record.getSourceName(),
                Component.literal(record.sourcePos.x.toString()).withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.literal(record.sourcePos.y.toString()).withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.literal(record.sourcePos.z.toString()).withStyle(ChatFormatting.LIGHT_PURPLE)
            )
        )
    }
}