package com.gtladd.gtladditions.mixin.gtceu.api.recipe;

import com.gregtechceu.gtceu.api.recipe.content.Content;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(Content.class)
public class ContentMixin {

    // @Shadow(remap = false)
    // public int chance;
    // @Shadow(remap = false)
    // public int maxChance;
    //
    // @Inject(method = "drawChance", at = @At("HEAD"), remap = false, cancellable = true)
    // public void drawChance(GuiGraphics graphics, float x, float y, int width, int height, CallbackInfo ci) {
    // if (this.chance != ChanceLogic.getMaxChancedValue()) {
    // graphics.pose().pushPose();
    // graphics.pose().translate(0.0F, 0.0F, 400.0F);
    // graphics.pose().scale(0.5F, 0.5F, 1.0F);
    // float chance = 100.0F * (float) this.chance / (float) this.maxChance;
    // String percent = FormattingUtil.formatPercent(chance);
    // String s = chance == 0.0F ? LocalizationUtils.format("gtceu.gui.content.chance_0_short") : percent + "%";
    // int color = chance == 0.0F ? 16711680 : 16776960;
    // Font fontRenderer = Minecraft.getInstance().font;
    // int xDraw = (int) ((x + (float) width / 3.0F) * 2.0F - (float) fontRenderer.width(s) + 23.0F) - (chance == 0.0F ?
    // 10 : 0);
    // int yDraw = (int) ((y + (float) height / 3.0F + 6.0F) * 2.0F - (float) height) - (chance == 0.0F ? 3 : 0);
    // graphics.drawString(fontRenderer, s, xDraw, yDraw, color, true);
    // graphics.pose().popPose();
    // }
    // ci.cancel();
    // }
}
