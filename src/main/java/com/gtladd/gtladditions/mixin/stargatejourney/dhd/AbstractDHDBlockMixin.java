package com.gtladd.gtladditions.mixin.stargatejourney.dhd;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.povstalec.sgjourney.common.block_entities.StructureGenEntity;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.blocks.dhd.AbstractDHDBlock;
import net.povstalec.sgjourney.common.misc.ComponentHelper;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

import javax.annotation.Nullable;

@Mixin(AbstractDHDBlock.class)
public abstract class AbstractDHDBlockMixin extends HorizontalDirectionalBlock {

    protected AbstractDHDBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter getter, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {
        if (stack.hasTag()) {
            CompoundTag blockEntityTag = BlockItem.getBlockEntityData(stack);
            if (blockEntityTag != null && blockEntityTag.contains(AbstractDHDEntity.GENERATION_STEP, CompoundTag.TAG_BYTE) && StructureGenEntity.Step.GENERATED != StructureGenEntity.Step.fromByte(blockEntityTag.getByte(AbstractDHDEntity.GENERATION_STEP)))
                tooltipComponents.add(Component.translatable("tooltip.sgjourney.generates_inside_structure").withStyle(ChatFormatting.YELLOW));
        }

        tooltipComponents.add(ComponentHelper.description("tooltip.sgjourney.dhd.description"));
        tooltipComponents.add(ComponentHelper.usage("tooltip.sgjourney.dhd.dialing_menu"));
        tooltipComponents.addAll(List.of(
                Component.translatable("tooltip.gtladditions.dhd")
                        .withStyle(ChatFormatting.GRAY)
                        .withStyle(ChatFormatting.ITALIC),
                Component.translatable("tooltip.gtladditions.dialog.0"),
                Component.translatable("tooltip.gtladditions.dialog.1"),
                Component.translatable("tooltip.gtladditions.dialog.2"),
                Component.translatable("tooltip.gtladditions.dialog.3")));

        super.appendHoverText(stack, getter, tooltipComponents, isAdvanced);
    }
}
