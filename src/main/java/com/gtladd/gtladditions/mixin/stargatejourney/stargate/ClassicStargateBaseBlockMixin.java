package com.gtladd.gtladditions.mixin.stargatejourney.stargate;

import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.povstalec.sgjourney.common.block_entities.stargate.PegasusStargateEntity;
import net.povstalec.sgjourney.common.blocks.stargate.AbstractStargateRingBlock;
import net.povstalec.sgjourney.common.blocks.stargate.ClassicStargateBaseBlock;
import net.povstalec.sgjourney.common.blocks.stargate.PegasusStargateBlock;
import net.povstalec.sgjourney.common.blockstates.Orientation;
import net.povstalec.sgjourney.common.blockstates.StargatePart;
import net.povstalec.sgjourney.common.config.CommonStargateConfig;
import net.povstalec.sgjourney.common.data.BlockEntityList;
import net.povstalec.sgjourney.common.init.BlockInit;
import net.povstalec.sgjourney.common.init.ItemInit;
import net.povstalec.sgjourney.common.sgjourney.Address;
import net.povstalec.sgjourney.common.sgjourney.PointOfOrigin;
import net.povstalec.sgjourney.common.sgjourney.Symbols;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

import static com.gtladd.gtladditions.utils.CommonUtils.createRainbowComponent;

@Mixin(ClassicStargateBaseBlock.class)
public abstract class ClassicStargateBaseBlockMixin extends HorizontalDirectionalBlock {

    @Shadow(remap = false)
    private static Orientation getPlacementOrientation(Level level, BlockPos pos, Direction direction) {
        throw new AssertionError();
    }

    protected ClassicStargateBaseBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable BlockGetter level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(createRainbowComponent(
                Component.translatable("tooltip.gtladditions.classic_stargate_base_block").getString()));
        if (GTUtil.isShiftDown()) {
            tooltip.addAll(
                    List.of(
                            Component.translatable("tooltip.gtladditions.stargate_structure.1"),
                            Component.translatable("tooltip.gtladditions.stargate_structure.2"),
                            Component.translatable("tooltip.gtladditions.stargate_structure.3"),
                            Component.translatable("tooltip.gtladditions.stargate_structure.4"),
                            Component.translatable("tooltip.gtladditions.stargate_structure.5"),
                            Component.translatable("tooltip.gtladditions.stargate_structure.6"),
                            Component.translatable("tooltip.gtladditions.stargate_structure.7"),
                            Component.translatable("tooltip.gtladditions.stargate_structure.8"),
                            Component.translatable("tooltip.gtladditions.stargate_structure.9"),
                            Component.translatable("tooltip.gtladditions.stargate_structure.10")));
        } else {
            tooltip.add(Component.translatable("tooltip.gtladditions.stargate_structure.0")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult result) {
        if (!level.isClientSide()) {
            ItemStack stack = player.getItemInHand(hand);
            Address.Mutable address = new Address.Mutable();

            if (CommonStargateConfig.enable_address_choice.get() && stack.is(ItemInit.CONTROL_CRYSTAL.get())) {
                String name = stack.getHoverName().getString();
                if (address.fromString(name).getType() != Address.Type.ADDRESS_9_CHEVRON) {
                    player.displayClientMessage(Component.translatable("block.sgjourney.stargate.classic.invalid_address"), true);
                    return InteractionResult.FAIL;
                }

                if (BlockEntityList.get(level).containsStargate(address)) {
                    player.displayClientMessage(Component.translatable("block.sgjourney.stargate.classic.address_exists"), true);
                    return InteractionResult.FAIL;
                }
            }

            Direction direction = level.getBlockState(pos).getValue(FACING);
            Orientation orientation = getPlacementOrientation(level, pos, direction);

            if (orientation == null) {
                player.displayClientMessage(Component.translatable("block.sgjourney.stargate.classic.incorrect_setup"), true);
                return InteractionResult.FAIL;
            }

            PegasusStargateBlock block = BlockInit.PEGASUS_STARGATE.get();
            level.setBlock(pos, block.defaultBlockState()
                    .setValue(PegasusStargateBlock.FACING, direction)
                    .setValue(AbstractStargateRingBlock.ORIENTATION, orientation), 3);

            for (StargatePart part : block.getParts()) {
                if (!part.equals(StargatePart.BASE)) {
                    level.setBlock(part.getRingPos(pos, direction, orientation),
                            BlockInit.PEGASUS_RING.get().defaultBlockState()
                                    .setValue(AbstractStargateRingBlock.PART, part)
                                    .setValue(AbstractStargateRingBlock.FACING, direction)
                                    .setValue(AbstractStargateRingBlock.ORIENTATION, orientation),
                            3);
                }
            }

            if (level.getBlockEntity(pos) instanceof PegasusStargateEntity stargate) {
                if (address.getType() == Address.Type.ADDRESS_9_CHEVRON) {
                    stargate.set9ChevronAddress(new Address.Immutable(address));

                    if (!player.isCreative())
                        stack.shrink(1);
                }

                stargate.symbolInfo().setPointOfOrigin(PointOfOrigin.randomPointOfOrigin(level.getServer(), level.dimension()));
                stargate.symbolInfo().setSymbols(Symbols.fromDimension(level.getServer(), level.dimension()));
                stargate.displayID();
                stargate.addStargateToNetwork();
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.SUCCESS;
    }
}
