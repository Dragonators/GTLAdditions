package com.gtladd.gtladditions.mixin.gtceu.common.item;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.item.DataItemBehavior;
import com.gregtechceu.gtceu.utils.ResearchManager;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Collection;
import java.util.List;

@Mixin(DataItemBehavior.class)
public abstract class DataItemBehaviorMixin {

    /**
     * @author Dragons
     * @reason 增加超级样板总成的提示
     */
    @Overwrite(remap = false)
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        Pair<GTRecipeType, String> researchData = ResearchManager.readResearchId(stack);
        if (researchData == null) {
            if (!stack.hasTag()) return;
            if (stack.getOrCreateTag().contains("pos", Tag.TAG_INT_ARRAY)) {
                int[] posArray = stack.getOrCreateTag().getIntArray("pos");
                tooltipComponents.add(Component.translatable(
                        "gtceu.tooltip.proxy_bind",
                        Component.literal("" + posArray[0]).withStyle(ChatFormatting.LIGHT_PURPLE),
                        Component.literal("" + posArray[1]).withStyle(ChatFormatting.LIGHT_PURPLE),
                        Component.literal("" + posArray[2]).withStyle(ChatFormatting.LIGHT_PURPLE)));
            } else if (stack.getOrCreateTag().contains("superPos", Tag.TAG_INT_ARRAY)) {
                int[] posArray = stack.getOrCreateTag().getIntArray("superPos");
                tooltipComponents.add(Component.translatable(
                        "gtceu.tooltip.super_proxy_bind",
                        Component.literal("" + posArray[0]).withStyle(ChatFormatting.LIGHT_PURPLE),
                        Component.literal("" + posArray[1]).withStyle(ChatFormatting.LIGHT_PURPLE),
                        Component.literal("" + posArray[2]).withStyle(ChatFormatting.LIGHT_PURPLE)));
            }
        } else {
            Collection<GTRecipe> recipes = researchData.getFirst().getDataStickEntry(researchData.getSecond());
            if (recipes != null && !recipes.isEmpty()) {
                tooltipComponents.add(Component.translatable("behavior.data_item.assemblyline.title"));
                Collection<ItemStack> added = new ObjectOpenHashSet<>();
                outer:
                for (GTRecipe recipe : recipes) {
                    ItemStack output = ItemRecipeCapability.CAP
                            .of(recipe.getOutputContents(ItemRecipeCapability.CAP).get(0).content).getItems()[0];
                    for (var item : added) {
                        if (output.is(item.getItem())) continue outer;
                    }
                    if (added.add(output)) {
                        tooltipComponents.add(
                                Component.translatable("behavior.data_item.assemblyline.data",
                                        output.getDisplayName()));
                    }
                }
            }
        }
    }
}
