package com.gtladd.gtladditions.mixin.gtlcore.machine.part;

import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEPatternBufferPartMachine;

import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.utils.ResearchManager;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.mojang.datafixers.util.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(MEPatternBufferPartMachine.class)
public abstract class MEPatternBufferPartMachineMixin {

    /**
     * @author Dragons
     * @reason 防止镜像数据混淆
     */
    @Overwrite(remap = false)
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player,
                                   InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return InteractionResult.PASS;

        if (stack.is(GTItems.TOOL_DATA_STICK.asItem())) {
            if (!world.isClientSide) {
                // Check if it's research data - if so, pass to avoid conflicts
                Pair<GTRecipeType, String> researchData = ResearchManager.readResearchId(stack);
                if (researchData != null) {
                    return InteractionResult.PASS;
                }

                if (stack.getOrCreateTag().contains("superPos", Tag.TAG_INT_ARRAY)) {
                    stack.getOrCreateTag().remove("superPos");
                }

                // Store this pattern buffer's position in the data stick
                stack.getOrCreateTag().putIntArray("pos", new int[] { pos.getX(), pos.getY(), pos.getZ() });
                player.sendSystemMessage(Component.translatable("gtceu.machine.me.import_copy_settings"));
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        return InteractionResult.PASS;
    }
}
