package com.gtladd.gtladditions.mixin.stargatejourney.stargate;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.povstalec.sgjourney.common.blocks.stargate.AbstractStargateBaseBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AbstractStargateBaseBlock.class)
public abstract class AbstractStargateBaseBlockMixin {

    @Inject(method = "appendHoverText", at = @At("TAIL"))
    private void gtladditions$hideEnergyTooltip(ItemStack stack, BlockGetter level, List<Component> tooltip, TooltipFlag flag, CallbackInfo ci) {
        tooltip.removeIf(component -> component.getContents() instanceof TranslatableContents text &&
                text.getKey().equals("tooltip.sgjourney.energy"));
    }
}