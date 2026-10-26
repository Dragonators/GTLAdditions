package com.gtladd.gtladditions.mixin.gtceu.common.item;

import com.gregtechceu.gtceu.common.item.DataItemBehavior;
import com.gregtechceu.gtceu.utils.ResearchManager;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DataItemBehavior.class)
public abstract class DataItemBehaviorMixin {

    @Inject(method = "appendHoverText", at = @At("RETURN"), remap = false, require = 1)
    private void gtladditions$appendSuperProxyBinding(ItemStack stack, @Nullable Level level,
                                                      List<Component> tooltipComponents, TooltipFlag isAdvanced,
                                                      CallbackInfo ci) {
        if (ResearchManager.readResearchId(stack) != null) return;
        var tag = stack.getTag();
        if (tag == null || tag.contains("pos", Tag.TAG_INT_ARRAY) || !tag.contains("superPos", Tag.TAG_INT_ARRAY)) return;
        int[] pos = tag.getIntArray("superPos");
        if (pos.length < 3) return;
        tooltipComponents.add(Component.translatable(
                "gtceu.tooltip.super_proxy_bind",
                Component.literal(Integer.toString(pos[0])).withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.literal(Integer.toString(pos[1])).withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.literal(Integer.toString(pos[2])).withStyle(ChatFormatting.LIGHT_PURPLE)));
    }
}