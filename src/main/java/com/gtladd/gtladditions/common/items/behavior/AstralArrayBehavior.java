package com.gtladd.gtladditions.common.items.behavior;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;

import com.gtladd.gtladditions.api.machine.IAstralArrayInteractionMachine;
import org.jetbrains.annotations.NotNull;

public class AstralArrayBehavior implements IInteractionItem {

    public static final AstralArrayBehavior INSTANCE = new AstralArrayBehavior();

    private AstralArrayBehavior() {}

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof MetaMachineBlockEntity machineBlock) {
            if (machineBlock.getMetaMachine() instanceof IAstralArrayInteractionMachine machine) {
                if (context.getPlayer() instanceof ServerPlayer serverPlayer && serverPlayer.isShiftKeyDown()) {
                    int used = machine.increaseAstralArrayCount(context.getItemInHand().getCount());

                    if (used > 0) {
                        if (!serverPlayer.isCreative()) {
                            context.getItemInHand().shrink(used);
                        }

                        serverPlayer.sendSystemMessage(
                                Component.translatable("gtladditions.message.astral_array.installed_multiple",
                                        used, machine.getAstralArrayCount()));
                    } else {
                        serverPlayer.sendSystemMessage(
                                Component.translatable("gtladditions.message.astral_array.max_reached"));
                    }

                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }
}
