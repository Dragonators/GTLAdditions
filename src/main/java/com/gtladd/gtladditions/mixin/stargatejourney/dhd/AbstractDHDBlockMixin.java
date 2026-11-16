package com.gtladd.gtladditions.mixin.stargatejourney.dhd;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.povstalec.sgjourney.common.blocks.dhd.AbstractDHDBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AbstractDHDBlock.class)
public abstract class AbstractDHDBlockMixin {

    @Inject(method = "appendHoverText", at = @At("TAIL"))
    private void gtladditions$updateTooltip(ItemStack stack, BlockGetter getter, List<Component> tooltipComponents, TooltipFlag isAdvanced, CallbackInfo ci) {
        tooltipComponents.removeIf(component -> component.getContents() instanceof TranslatableContents text &&
                (text.getKey().equals("tooltip.sgjourney.energy_buffer") || text.getKey().equals("tooltip.sgjourney.dhd.crystal_menu")));
        tooltipComponents.addAll(List.of(
                Component.translatable("tooltip.gtladditions.dhd").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                Component.translatable("tooltip.gtladditions.dialog.0"),
                Component.translatable("tooltip.gtladditions.dialog.1"),
                Component.translatable("tooltip.gtladditions.dialog.2"),
                Component.translatable("tooltip.gtladditions.dialog.3")));
    }
}