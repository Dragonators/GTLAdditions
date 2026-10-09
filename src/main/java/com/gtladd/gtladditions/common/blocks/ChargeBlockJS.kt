package com.gtladd.gtladditions.common.blocks

import com.gtladd.gtladditions.common.items.behavior.ChargeExplosionBehavior
import dev.latvian.mods.kubejs.block.BlockBuilder
import dev.latvian.mods.kubejs.block.custom.BasicBlockJS
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

class ChargeBlockJS(builder: BlockBuilder) : BasicBlockJS(builder) {
    override fun use(state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult): InteractionResult {
        val result = ChargeExplosionBehavior.onUse(level, pos, player, hand)
        return if (result == InteractionResult.PASS) super.use(state, level, pos, player, hand, hit) else result
    }

    override fun appendHoverText(stack: ItemStack, level: BlockGetter?, tooltipComponents: MutableList<Component>, isAdvanced: TooltipFlag) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced)
        ChargeExplosionBehavior.appendHoverText(blockBuilder.id, tooltipComponents)
    }
}