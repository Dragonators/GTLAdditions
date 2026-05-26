package com.gtladd.gtladditions.mixin.guide;

import com.gtladd.gtladditions.api.guide.CleanItemLinkCompiler;
import com.gtladd.gtladditions.api.guide.FluidLinkCompiler;
import com.gtladd.gtladditions.api.guide.LatexCompiler;
import guideme.compiler.TagCompiler;
import guideme.compiler.tags.ATagCompiler;
import guideme.compiler.tags.BoxFlowDirection;
import guideme.compiler.tags.BoxTagCompiler;
import guideme.compiler.tags.BreakCompiler;
import guideme.compiler.tags.CategoryIndexCompiler;
import guideme.compiler.tags.ColorTagCompiler;
import guideme.compiler.tags.CommandLinkCompiler;
import guideme.compiler.tags.DivTagCompiler;
import guideme.compiler.tags.FloatingImageCompiler;
import guideme.compiler.tags.ItemGridCompiler;
import guideme.compiler.tags.KeyBindTagCompiler;
import guideme.compiler.tags.PlayerNameTagCompiler;
import guideme.compiler.tags.RecipeCompiler;
import guideme.compiler.tags.SubPagesCompiler;
import guideme.internal.extensions.DefaultExtensions;
import guideme.scene.BlockImageTagCompiler;
import guideme.scene.ItemImageTagCompiler;
import guideme.scene.SceneTagCompiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DefaultExtensions.class)
public class DefaultExtensionsMixin {

    @Inject(method = "tagCompilers", at = @At("HEAD"), remap = false, cancellable = true)
    private static void tagCompilers(CallbackInfoReturnable<List<TagCompiler>> cir) {
        cir.setReturnValue(List.of(
                new DivTagCompiler(),
                new ATagCompiler(),
                new ColorTagCompiler(),
                new CleanItemLinkCompiler(),
                new FloatingImageCompiler(),
                new BreakCompiler(),
                new RecipeCompiler(),
                new ItemGridCompiler(),
                new CategoryIndexCompiler(),
                new BlockImageTagCompiler(),
                new ItemImageTagCompiler(),
                new BoxTagCompiler(BoxFlowDirection.ROW),
                new FluidLinkCompiler(),
                new BoxTagCompiler(BoxFlowDirection.COLUMN),
                new SceneTagCompiler(),
                new SubPagesCompiler(),
                new CommandLinkCompiler(),
                new PlayerNameTagCompiler(),
                new KeyBindTagCompiler(),
                new LatexCompiler()));
    }
}