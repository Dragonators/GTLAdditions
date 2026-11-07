package com.gtladd.gtladditions.mixin.embeddium;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer", remap = false)
public interface FrameOverlayWorldRendererAccessor {

    @Accessor("renderSectionManager")
    RenderSectionManager getRenderSectionManager();
}