package com.gtladd.gtladditions.mixin.stargatejourney.stargate;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.povstalec.sgjourney.common.block_entities.StructureGenEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import net.povstalec.sgjourney.common.blocks.stargate.AbstractStargateBaseBlock;
import net.povstalec.sgjourney.common.blocks.stargate.AbstractStargateBlock;
import net.povstalec.sgjourney.common.config.CommonStargateConfig;
import net.povstalec.sgjourney.common.misc.InventoryUtil;
import net.povstalec.sgjourney.common.sgjourney.Address;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

@Mixin(AbstractStargateBaseBlock.class)
public abstract class AbstractStargateBaseBlockMixin extends AbstractStargateBlock {

    public AbstractStargateBaseBlockMixin(Properties properties, double width, double horizontalOffset) {
        super(properties, width, horizontalOffset);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable BlockGetter getter, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag isAdvanced) {
        String id;

        CompoundTag blockEntityTag = InventoryUtil.getBlockEntityTag(stack);

        if (blockEntityTag != null) {
            if (blockEntityTag.contains(AbstractStargateEntity.VARIANT)) {
                String variant = blockEntityTag.getString(AbstractStargateEntity.VARIANT);

                if (!variant.equals(AbstractStargateBaseBlock.EMPTY))
                    tooltipComponents.add(Component.translatable("tooltip.sgjourney.variant").append(Component.literal(": " + variant)).withStyle(ChatFormatting.GREEN));
            }
        }

        if (blockEntityTag != null) {
            if ((blockEntityTag.contains(AbstractStargateEntity.DISPLAY_ID) && blockEntityTag.getBoolean(AbstractStargateEntity.DISPLAY_ID)) || CommonStargateConfig.always_display_stargate_id.get()) {
                if (blockEntityTag.contains(AbstractStargateEntity.ID)) {
                    id = blockEntityTag.getString(AbstractStargateEntity.ID);
                    tooltipComponents.add(Component.translatable("tooltip.sgjourney.9_chevron_address").append(Component.literal(": " + id)).withStyle(ChatFormatting.AQUA));
                } else if (blockEntityTag.contains(AbstractStargateEntity.ID_9_CHEVRON_ADDRESS)) {
                    id = Address.addressIntArrayToString(blockEntityTag.getIntArray(AbstractStargateEntity.ID_9_CHEVRON_ADDRESS));
                    tooltipComponents.add(Component.translatable("tooltip.sgjourney.9_chevron_address").append(Component.literal(": " + id)).withStyle(ChatFormatting.AQUA));
                }
            }

            if ((blockEntityTag.contains(AbstractStargateEntity.UPGRADED) && blockEntityTag.getBoolean(AbstractStargateEntity.UPGRADED)))
                tooltipComponents.add(Component.translatable("tooltip.sgjourney.upgraded").withStyle(ChatFormatting.DARK_GREEN));

            if ((blockEntityTag.contains(AbstractStargateBaseBlock.LOCAL_POINT_OF_ORIGIN)))
                tooltipComponents.add(Component.translatable("tooltip.sgjourney.local_point_of_origin").withStyle(ChatFormatting.GREEN));

            if (blockEntityTag.contains(AbstractStargateEntity.GENERATION_STEP, CompoundTag.TAG_BYTE) && StructureGenEntity.Step.SETUP == StructureGenEntity.Step.fromByte(Objects.requireNonNull(stack.getTag())
                    .getCompound("BlockEntityTag").getByte(AbstractStargateEntity.GENERATION_STEP)))
                tooltipComponents.add(Component.translatable("tooltip.sgjourney.generates_inside_structure").withStyle(ChatFormatting.YELLOW));

            if (blockEntityTag.getBoolean(AbstractStargateEntity.PRIMARY))
                tooltipComponents.add(Component.translatable("tooltip.sgjourney.is_primary").withStyle(ChatFormatting.DARK_GREEN));
        }

        super.appendHoverText(stack, getter, tooltipComponents, isAdvanced);
    }
}
