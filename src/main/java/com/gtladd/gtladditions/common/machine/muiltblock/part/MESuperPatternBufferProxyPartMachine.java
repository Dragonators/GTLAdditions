package com.gtladd.gtladditions.common.machine.muiltblock.part;

import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEPatternBufferProxyPartMachine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.utils.ResearchManager;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.mojang.datafixers.util.Pair;
import org.jetbrains.annotations.NotNull;

public class MESuperPatternBufferProxyPartMachine extends MEPatternBufferProxyPartMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MESuperPatternBufferProxyPartMachine.class, MEPatternBufferProxyPartMachine.MANAGED_FIELD_HOLDER);

    @Override
    @NotNull
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public MESuperPatternBufferProxyPartMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return InteractionResult.PASS;

        if (stack.is(GTItems.TOOL_DATA_STICK.asItem())) {
            if (!world.isClientSide) {
                // Check if it's research data - if so, pass to avoid conflicts
                Pair<GTRecipeType, String> researchData = ResearchManager.readResearchId(stack);
                if (researchData != null) {
                    return InteractionResult.PASS;
                }

                // Read pattern buffer position from the data stick
                var tag = stack.getTag();
                if (tag != null && tag.contains("superPos")) {
                    int[] posArray = tag.getIntArray("superPos");
                    if (posArray.length == 3) {
                        BlockPos bufferPos = new BlockPos(posArray[0], posArray[1], posArray[2]);
                        player.sendSystemMessage(Component.translatable("gtceu.machine.me.import_paste_settings"));
                        setBuffer(bufferPos);
                    }
                }
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        return InteractionResult.PASS;
    }
}
