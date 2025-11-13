package com.gtladd.gtladditions.mixin.gtceu.xei;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredientTypeWithSubtypes;
import mezz.jei.common.platform.IPlatformFluidHelperInternal;
import mezz.jei.library.render.FluidTankRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FluidTankRenderer.class)
public abstract class FluidTankRendererMixin<T> implements IIngredientRenderer<T> {

    @Shadow(remap = false)
    @Final
    private IPlatformFluidHelperInternal<T> fluidHelper;

    @Shadow(remap = false)
    private static void drawTiledSprite(GuiGraphics guiGraphics, final int tiledWidth, final int tiledHeight, int color, long scaledAmount, TextureAtlasSprite sprite, int posX, int posY) {
        throw new AssertionError();
    }

    /**
     * @author Dragons
     * @reason fix icon
     */
    @Overwrite(remap = false)
    private void drawFluid(GuiGraphics guiGraphics, final int width, final int height, T fluidStack, int posX, int posY) {
        IIngredientTypeWithSubtypes<Fluid, T> type = fluidHelper.getFluidIngredientType();
        Fluid fluid = type.getBase(fluidStack);
        if (fluid.isSame(Fluids.EMPTY)) {
            return;
        }

        fluidHelper.getStillFluidSprite(fluidStack)
                .ifPresent(fluidStillSprite -> {
                    int fluidColor = fluidHelper.getColorTint(fluidStack);
                    drawTiledSprite(guiGraphics, width, height, fluidColor, height, fluidStillSprite, posX, posY);
                });
    }
}
