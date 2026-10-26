package com.gtladd.gtladditions.mixin.guide;

import com.gtladd.gtladditions.api.guide.CleanItemLinkCompiler;
import com.gtladd.gtladditions.api.guide.FluidLinkCompiler;
import com.gtladd.gtladditions.api.guide.LatexCompiler;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import guideme.compiler.TagCompiler;
import guideme.compiler.tags.ItemLinkCompiler;
import guideme.internal.extensions.DefaultExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(DefaultExtensions.class)
public class DefaultExtensionsMixin {

    @ModifyReturnValue(method = "tagCompilers", at = @At("RETURN"), remap = false, require = 1)
    private static List<TagCompiler> gtladditions$extendTagCompilers(List<TagCompiler> original) {
        List<TagCompiler> compilers = new ArrayList<>(original.size() + 2);

        for (TagCompiler compiler : original) {
            compilers.add(compiler.getClass() == ItemLinkCompiler.class ? new CleanItemLinkCompiler() : compiler);
        }

        compilers.add(new FluidLinkCompiler());
        compilers.add(new LatexCompiler());
        return List.copyOf(compilers);
    }
}