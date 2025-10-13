package com.gtladd.gtladditions.mixin.gtceu.recipe;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;

import net.minecraft.util.ExtraCodecs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Content.class)
public abstract class ContentMixin {

    /**
     * @author Dragons
     * @reason enable negative boost
     */
    @Overwrite(remap = false)
    public static <T> Codec<Content> codec(RecipeCapability<T> capability) {
        return RecordCodecBuilder.create(instance -> instance.group(
                capability.serializer.codec().fieldOf("content").forGetter(val -> capability.of(val.content)),
                ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("chance", ChanceLogic.getMaxChancedValue())
                        .forGetter(val -> val.chance),
                ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("maxChance", ChanceLogic.getMaxChancedValue())
                        .forGetter(val -> val.maxChance),
                Codec.INT.optionalFieldOf("tierChanceBoost", 0)
                        .forGetter(val -> val.tierChanceBoost),
                Codec.STRING.optionalFieldOf("slotName", "").forGetter(val -> val.slotName != null ? val.slotName : ""),
                Codec.STRING.optionalFieldOf("uiName", "").forGetter(val -> val.uiName != null ? val.uiName : ""))
                .apply(instance, Content::new));
    }
}
