package com.gtladd.gtladditions.common.machine.muiltblock.part

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.common.data.GTItems
import com.gregtechceu.gtceu.utils.ResearchManager
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEPatternBufferProxyPartMachine

class MESuperPatternBufferProxyPartMachine(holder: IMachineBlockEntity) : MEPatternBufferProxyPartMachine(holder) {
    override fun onUse(
        state: BlockState?,
        world: Level,
        pos: BlockPos?,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult?
    ): InteractionResult {
        val stack = player.getItemInHand(hand)
        if (stack.isEmpty) return InteractionResult.PASS

        if (stack.`is`(GTItems.TOOL_DATA_STICK.asItem())) {
            if (!world.isClientSide) {
                // Check if it's research data - if so, pass to avoid conflicts
                val researchData = ResearchManager.readResearchId(stack)
                if (researchData != null) {
                    return InteractionResult.PASS
                }

                // Read pattern buffer position from the data stick
                val tag = stack.tag
                if (tag != null && tag.contains("superPos")) {
                    val posArray = tag.getIntArray("superPos")
                    if (posArray.size == 3) {
                        val bufferPos = BlockPos(posArray[0], posArray[1], posArray[2])
                        player.sendSystemMessage(Component.translatable("gtceu.machine.me.import_paste_settings"))
                        setBuffer(bufferPos)
                    }
                }
            }
            return InteractionResult.sidedSuccess(world.isClientSide)
        }

        return InteractionResult.PASS
    }
}
