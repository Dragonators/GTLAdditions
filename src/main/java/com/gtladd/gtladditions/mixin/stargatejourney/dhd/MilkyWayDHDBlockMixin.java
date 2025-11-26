package com.gtladd.gtladditions.mixin.stargatejourney.dhd;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.povstalec.sgjourney.common.block_entities.dhd.MilkyWayDHDEntity;
import net.povstalec.sgjourney.common.blocks.dhd.MilkyWayDHDBlock;
import net.povstalec.sgjourney.common.menu.MilkyWayDHDMenu;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(MilkyWayDHDBlock.class)
public abstract class MilkyWayDHDBlockMixin {

    /**
     * @author Dragons
     * @reason Disable Holder
     */
    @Overwrite
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult trace) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity instanceof MilkyWayDHDEntity dhd) {
                dhd.setStargate();

                MenuProvider containerProvider = new MenuProvider() {

                    @Override
                    public @NotNull Component getDisplayName() {
                        return Component.translatable("screen.sgjourney.dhd");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                        return new MilkyWayDHDMenu(windowId, playerInventory, blockEntity);
                    }
                };
                NetworkHooks.openScreen((ServerPlayer) player, containerProvider, blockEntity.getBlockPos());
            } else {
                throw new IllegalStateException("Our named container provider is missing!");
            }
        }
        return InteractionResult.SUCCESS;
    }
}
