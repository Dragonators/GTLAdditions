package com.gtladd.gtladditions.common.items

import com.gregtechceu.gtceu.api.item.component.IInteractionItem
import com.gtladd.gtladditions.GTLAdditions
import guideme.GuidesCommon
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class GuideBook : IInteractionItem {
    override fun use(
        item: Item,
        level: Level,
        player: Player,
        usedHand: InteractionHand
    ): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(usedHand)
        if (level.isClientSide) {
            GuidesCommon.openGuide(player, GTLAdditions.id("guide"))
        }
        return InteractionResultHolder(InteractionResult.FAIL, stack)
    }
}